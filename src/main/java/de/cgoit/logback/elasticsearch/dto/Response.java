package de.cgoit.logback.elasticsearch.dto;

import org.apache.http.HttpStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Response {

    private static final String TOOK = "took";
    private static final String ERRORS = "errors";
    private static final String ITEMS = "items";
    private static final String INDEX = "index";
    private static final String STATUS = "status";

    private final Map<String, Object> response;

    public Response(Map<String, Object> response) {
        this.response = response;
    }

    public int getTook() {
        return (int) response.get(TOOK);
    }

    public boolean hasErrors() {
        return (boolean) response.get(ERRORS);
    }

    public List<Map<String, Object>> getItems() {
        return (List<Map<String, Object>>)response.get(ITEMS);
    }

    public Map<Integer, Map<String, Object>> getFailedItems() {
        if (!hasErrors()) {
            return Collections.emptyMap();
        }

        Map<Integer, Map<String, Object>> result = new HashMap<>();

        List<Map<String, Object>> items = getItems();
        int itemSize = items.size();
        for (int i = 0; i < itemSize; i++) {
            Map<String, Object> item = items.get(i);
            Map<String, Object> entry = (Map<String, Object>) item.get(INDEX);
            if ((int)entry.get(STATUS) != HttpStatus.SC_CREATED) {
                result.put(i, entry);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Response{" +
                "took=" + getTook() +
                ", errors=" + hasErrors() +
                ", items=" + getItems() +
                ", failedItems=" + getFailedItems() +
                '}';
    }
}
