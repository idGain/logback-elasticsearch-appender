package com.internetitem.logback.elasticsearch.config;

import java.util.LinkedList;
import java.util.List;

/**
 * A container for the headers which will be sent to elasticsearch.
 */
public class HttpRequestHeaders {

    private final List<HttpRequestHeader> headers = new LinkedList<>();

    public List<HttpRequestHeader> getHeaders() {
        return headers;
    }

    public void addHeader(HttpRequestHeader header) {
        this.headers.add(header);
    }
}
