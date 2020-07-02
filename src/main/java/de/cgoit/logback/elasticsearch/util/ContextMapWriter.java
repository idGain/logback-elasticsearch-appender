package de.cgoit.logback.elasticsearch.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class ContextMapWriter {

    static void traverseObject(Map<String, Object> accumulator, String context, Object object) {
        if (object == null) {
            return;
        }
        if (object instanceof Map) {
            traverseMap(accumulator, context, (Map<Object, Object>) object);
        } else if (object instanceof Collection) {
            traverseCollection(accumulator, context, (Collection) object);
        } else if (object instanceof Number) {
            accumulator.put(context, object);
        } else {
            accumulator.put(context, Objects.toString(object));
        }
    }

    static void traverseCollection(Map<String, Object> accumulator, String context, Collection<Object> object) {
        Iterator<Object> iterator = object.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Object v = iterator.next();
            traverseObject(accumulator, context + "." + i, v);
            i++;
        }
    }

    static Map<String, Object> traverseMap(Map<String, Object> accumulator, String context, Map<Object, Object> object) {
        for (Map.Entry<Object, Object> entry : object.entrySet()) {
            traverseObject(accumulator, context + "." + entry.getKey(), entry.getValue());
        }
        return accumulator;
    }

    public void writeContextMap(JsonGenerator gen, ILoggingEvent event) throws IOException {
        Object[] arguments = event.getArgumentArray();
        if (arguments == null || arguments.length == 0) {
            return;
        }
        Object lastElement = arguments[arguments.length - 1];
        if (lastElement instanceof Map) {
            Map<String, Object> indexes = traverseMap(new HashMap<>(), "context", (Map<Object, Object>) lastElement);
            for (Map.Entry<String, Object> entry : indexes.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                gen.writeObjectField(key, value);
            }
        }
    }
}