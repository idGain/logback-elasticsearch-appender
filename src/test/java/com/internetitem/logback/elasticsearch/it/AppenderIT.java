package com.internetitem.logback.elasticsearch.it;

import org.elasticsearch.search.SearchHits;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@Category(Integration.class)
public class AppenderIT extends IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AppenderIT.class);
    private static final Logger ES_LOG = LoggerFactory.getLogger(ELASTICSEARCH_LOGGER_NAME);

    @Test
    public void testIndexLogEntry() throws InterruptedException, IOException {
        final int count = 10000;
        LOG.info("Insert {} entries to es.", count);
        for (int i = 0; i < count; i++) {
            ES_LOG.info("Log some int {} to elasticsearch", i);
        }

        LOG.info("Indexing is async. Wait for 10 seconds.");
        Thread.sleep(10000);

        SearchHits hits = searchAll();
        LOG.info("Found {} documents in elasticsearch", hits.getTotalHits().value);
        assertEquals(count, hits.getTotalHits().value);
    }
}
