package de.cgoit.logback.elasticsearch.config;

import java.util.ArrayList;
import java.util.List;

public class ElasticsearchProperties {

    private final List<Property> properties;

    public ElasticsearchProperties() {
        this.properties = new ArrayList<>();
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void addProperty(Property property) {
        properties.add(property);
    }

}
