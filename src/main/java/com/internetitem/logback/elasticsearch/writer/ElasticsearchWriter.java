package com.internetitem.logback.elasticsearch.writer;

import com.internetitem.logback.elasticsearch.config.HttpRequestHeader;
import com.internetitem.logback.elasticsearch.config.HttpRequestHeaders;
import com.internetitem.logback.elasticsearch.config.Settings;
import com.internetitem.logback.elasticsearch.util.ErrorReporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

public class ElasticsearchWriter implements SafeWriter {

    private final StringBuilder sendBuffer;

    private final ErrorReporter errorReporter;
    private final Settings settings;
    private final Collection<HttpRequestHeader> headerList;

    private boolean bufferExceeded;

    public ElasticsearchWriter(ErrorReporter errorReporter, Settings settings, HttpRequestHeaders headers) {
        this.errorReporter = errorReporter;
        this.settings = settings;
        this.headerList = headers != null && headers.getHeaders() != null
                ? headers.getHeaders()
                : Collections.emptyList();

        this.sendBuffer = new StringBuilder();
    }

    private static String slurpErrors(HttpURLConnection urlConnection) {
        try (InputStream stream = urlConnection.getErrorStream()) {
            if (stream == null) {
                return "<no data>";
            }

            StringBuilder builder = new StringBuilder();

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buf = new char[2048];
                int numRead;
                while ((numRead = reader.read(buf)) > 0) {
                    builder.append(buf, 0, numRead);
                }
            }
            return builder.toString();
        } catch (Exception e) {
            return "<error retrieving data: " + e.getMessage() + ">";
        }
    }

    public void write(char[] cbuf, int off, int len) {
        if (bufferExceeded) {
            return;
        }

        sendBuffer.append(cbuf, off, len);

        if (sendBuffer.length() >= settings.getMaxQueueSize()) {
            errorReporter.logWarning("Send queue maximum size exceeded - log messages will be lost until the buffer is cleared");
            bufferExceeded = true;
        }
    }

    public void sendData() throws IOException {
        if (sendBuffer.length() <= 0) {
            return;
        }

        HttpURLConnection urlConnection = (HttpURLConnection) (settings.getUrl().openConnection());
        try {
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(settings.getReadTimeout());
            urlConnection.setConnectTimeout(settings.getConnectTimeout());
            urlConnection.setRequestMethod("POST");

            String body = sendBuffer.toString();

            if (!headerList.isEmpty()) {
                for (HttpRequestHeader header : headerList) {
                    urlConnection.setRequestProperty(header.getName(), header.getValue());
                }
            }

            if (settings.getAuthentication() != null) {
                settings.getAuthentication().addAuth(urlConnection, body);
            }

            try (Writer writer = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(body);
                writer.flush();
            }

            int rc = urlConnection.getResponseCode();
            if (rc != 200) {
                String data = slurpErrors(urlConnection);
                throw new IOException("Got response code [" + rc + "] from server with data " + data);
            }
        } finally {
            urlConnection.disconnect();
        }

        sendBuffer.setLength(0);
        if (bufferExceeded) {
            errorReporter.logInfo("Send queue cleared - log messages will no longer be lost");
            bufferExceeded = false;
        }
    }

    public boolean hasPendingData() {
        return sendBuffer.length() != 0;
    }

}
