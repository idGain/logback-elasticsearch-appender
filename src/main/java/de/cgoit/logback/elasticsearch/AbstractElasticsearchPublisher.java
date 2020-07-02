package de.cgoit.logback.elasticsearch;

import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders;
import de.cgoit.logback.elasticsearch.config.Property;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.AbstractPropertyAndEncoder;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;
import de.cgoit.logback.elasticsearch.writer.ElasticsearchWriter;
import de.cgoit.logback.elasticsearch.writer.FailedEventsWriter;
import de.cgoit.logback.elasticsearch.writer.LoggerWriter;
import de.cgoit.logback.elasticsearch.writer.StdErrWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractElasticsearchPublisher<T> implements Runnable {

    public static final String THREAD_NAME_PREFIX = "es-writer-";
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    );
    private final Object lock;
    private final PropertySerializer<T> propertySerializer;
    private final ElasticsearchOutputAggregator outputAggregator;
    private final List<AbstractPropertyAndEncoder<T>> propertyList;
    private final AbstractPropertyAndEncoder<T> indexPattern;
    private final JsonFactory jf;
    private final JsonGenerator jsonGenerator;
    private final JsonGenerator failedEventsJsonGenerator;
    private final FailedEventsWriter failedEventsWriter;
    private final ErrorReporter errorReporter;
    protected Settings settings;
    private volatile List<T> events;
    private volatile boolean working;

    public AbstractElasticsearchPublisher(Context context, ErrorReporter errorReporter, Settings settings, ElasticsearchProperties properties, HttpRequestHeaders headers) throws IOException {
        this.errorReporter = errorReporter;
        this.events = new LinkedList<>();
        this.lock = new Object();
        this.settings = settings;

        this.outputAggregator = configureOutputAggregator(settings, errorReporter, headers);

        this.jf = new JsonFactory();
        this.jf.setRootValueSeparator(null);
        this.jsonGenerator = jf.createGenerator(outputAggregator);
        if (settings.getFailedEventsLoggerName() != null) {
            this.failedEventsWriter = new FailedEventsWriter(settings.getFailedEventsLoggerName());
            this.failedEventsJsonGenerator = jf.createGenerator(failedEventsWriter);
        } else {
            this.failedEventsWriter = null;
            this.failedEventsJsonGenerator = null;
        }


        this.indexPattern = buildPropertyAndEncoder(context, new Property("<index>", settings.getIndex(), false));
        this.propertyList = generatePropertyList(context, properties);

        this.propertySerializer = new PropertySerializer<>();
    }

    private static ElasticsearchOutputAggregator configureOutputAggregator(Settings settings, ErrorReporter errorReporter, HttpRequestHeaders httpRequestHeaders) {
        ElasticsearchOutputAggregator spigot = new ElasticsearchOutputAggregator(settings, errorReporter);

        if (settings.isLogsToStderr()) {
            spigot.addWriter(new StdErrWriter());
        }

        if (settings.getLoggerName() != null) {
            spigot.addWriter(new LoggerWriter(settings.getLoggerName()));
        }

        if (settings.getUrl() != null) {
            spigot.addWriter(new ElasticsearchWriter(errorReporter, settings, httpRequestHeaders));
        }

        return spigot;
    }

    protected static String getTimestamp(long timestamp) {
        return DATE_FORMAT.get().format(new Date(timestamp));
    }

    private List<AbstractPropertyAndEncoder<T>> generatePropertyList(Context context, ElasticsearchProperties properties) {
        List<AbstractPropertyAndEncoder<T>> list = new ArrayList<>();
        if (properties != null) {
            for (Property property : properties.getProperties()) {
                list.add(buildPropertyAndEncoder(context, property));
            }
        }
        return list;
    }

    protected abstract AbstractPropertyAndEncoder<T> buildPropertyAndEncoder(Context context, Property property);

    public void addEvent(T event) {
        if (!outputAggregator.hasOutputs()) {
            return;
        }

        int max = settings.getMaxEvents();

        synchronized (lock) {
            events.add(event);
            if (max > 0 && events.size() > max) {
                errorReporter.logWarning("Max events in queue reached - log messages will be lost until the queue is processed");
                ((LinkedList<T>) events).removeFirst();
            }
            if (!working) {
                working = true;
                Thread thread = new Thread(this, THREAD_NAME_PREFIX + THREAD_COUNTER.getAndIncrement());
                thread.start();
            }
        }
    }

    public void run() {
        int currentTry = 1;
        int maxRetries = settings.getMaxRetries();
        while (true) {
            try {
                Thread.sleep(settings.getSleepTime());

                List<T> eventsCopy = null;
                synchronized (lock) {
                    if (!events.isEmpty()) {
                        eventsCopy = events;
                        events = new LinkedList<>();
                        currentTry = 1;
                    }

                    if (eventsCopy == null) {
                        if (!outputAggregator.hasPendingData()) {
                            // all done
                            working = false;
                            return;
                        } else {
                            // Nothing new, must be a retry
                            if (currentTry > maxRetries) {
                                // Oh well, better luck next time
                                working = false;
                                DATE_FORMAT.remove();
                                return;
                            }
                        }
                    }
                }

                if (eventsCopy != null) {
                    serializeEvents(jsonGenerator, eventsCopy, propertyList);
                }

                try {
                    Set<Integer> failedIndices = outputAggregator.sendData();
                    if (!failedIndices.isEmpty() && eventsCopy != null && failedEventsJsonGenerator != null) {
                        for (Integer idx : failedIndices) {
                            if (idx < eventsCopy.size() - 1) {
                                T event = eventsCopy.get(idx);
                                serializeIndexString(failedEventsJsonGenerator, event);
                                failedEventsJsonGenerator.writeRaw('\n');
                                serializeEvent(failedEventsJsonGenerator, event, propertyList);
                                failedEventsJsonGenerator.writeRaw('\n');
                                failedEventsJsonGenerator.flush();
                            }
                        }
                    }
                } catch (IOException e) {
                    // Fatal error in sendData
                    currentTry++;
                }
            } catch (Exception e) {
                errorReporter.logError("Internal error handling log data: " + e.getMessage(), e);
                currentTry++;
            }
        }
    }

    private void serializeEvents(JsonGenerator gen, List<T> eventsCopy, List<AbstractPropertyAndEncoder<T>> propertyList) throws IOException {
        for (T event : eventsCopy) {
            serializeIndexString(gen, event);
            gen.writeRaw('\n');
            serializeEvent(gen, event, propertyList);
            gen.writeRaw('\n');
        }
        gen.flush();
    }

    private void serializeIndexString(JsonGenerator gen, T event) throws IOException {
        gen.writeStartObject();
        gen.writeObjectFieldStart("index");
        gen.writeObjectField("_index", indexPattern.encode(event));
        gen.writeEndObject();
        gen.writeEndObject();
    }

    private void serializeEvent(JsonGenerator gen, T event, List<AbstractPropertyAndEncoder<T>> propertyList) throws IOException {
        gen.writeStartObject();

        serializeCommonFields(gen, event);

        for (AbstractPropertyAndEncoder<T> pae : propertyList) {
            propertySerializer.serializeProperty(gen, event, pae);
        }

        gen.writeEndObject();
    }

    protected abstract void serializeCommonFields(JsonGenerator gen, T event) throws IOException;

    public List<T> getEvents() {
        return this.events;
    }
}
