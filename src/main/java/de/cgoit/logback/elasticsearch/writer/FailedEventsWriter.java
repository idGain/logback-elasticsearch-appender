package de.cgoit.logback.elasticsearch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

public class FailedEventsWriter extends Writer {

    private final String loggerName;

    private Logger logger;

    public FailedEventsWriter(String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public void write(char[] chars, int offset, int len) throws IOException {
        if (logger == null) {
            logger = LoggerFactory.getLogger(loggerName);
        }

        logger.error(new String(chars, 0, len));
    }

    @Override
    public void flush() throws IOException {
        // no-op
    }

    @Override
    public void close() throws IOException {
        // no-op
    }
}
