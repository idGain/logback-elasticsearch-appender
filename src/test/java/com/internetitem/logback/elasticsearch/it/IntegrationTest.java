package com.internetitem.logback.elasticsearch.it;

import com.internetitem.logback.elasticsearch.ElasticsearchAppender;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.Assert.assertTrue;

public abstract class IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

    private static final String INDEX = "log_entries";
    private static final int WAIT_FOR_INDEX_MAX_RETRIES = 10;
    private static final int WAIT_FOR_INDEX_SLEEP_INTERVAL = 100;
    protected static final String ELASTICSEARCH_LOGGER_NAME = "ES_LOGGER";
    private static final String ELASTICSEARCH_APPENDER_NAME = "ES_APPENDER";

    protected static RestHighLevelClient client;
    protected static ElasticsearchContainer container;

    @BeforeClass
    public static void setupElasticSearchContainer() throws IOException, InterruptedException {
        // Create the elasticsearch container.
        IntegrationTest.container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.7.1");

        // Start the container. This step might take some time...
        container.start();

        // Do whatever you want with the rest client ...
        IntegrationTest.client = new RestHighLevelClient(RestClient.builder(HttpHost.create(container.getHttpHostAddress())));
        configureElasticSearchAppender();

        deleteAll();
    }

    private static void configureElasticSearchAppender() throws MalformedURLException {
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ELASTICSEARCH_LOGGER_NAME);
        ElasticsearchAppender appender = (ElasticsearchAppender)logbackLogger.getAppender(ELASTICSEARCH_APPENDER_NAME);

        String containerUrl = HttpHost.create(container.getHttpHostAddress()).toURI() + "/_bulk";
        LOG.info("Configure appender {} to use {} as container address.", ELASTICSEARCH_APPENDER_NAME, containerUrl);
        appender.setUrl(containerUrl);
    }

    @AfterClass
    public static void tearDownElasticSearchContainer() {
        // Stop the container.
        IntegrationTest.container.stop();
    }

    @After
    public void clearFromElasticSearch() throws IOException {
        deleteAll();
    }

    protected SearchHits searchAll() throws IOException {
        SearchRequest request = new SearchRequest();
        request.searchType(SearchType.DEFAULT).source(SearchSourceBuilder.searchSource().query(QueryBuilders.matchAllQuery()));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return response.getHits();
    }

    protected static void deleteAll() throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest("_all");
        request.setQuery(QueryBuilders.matchAllQuery());
        BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
        long deleted = response.getDeleted();
        LOG.info("Deleted {} documents from elasticsearch.", deleted);
    }

    private static void createIndex() throws IOException, InterruptedException {
        CreateIndexRequest request = new CreateIndexRequest(INDEX);
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);

        assertTrue("Could not create index. Shutdown", response.isAcknowledged());
        waitForIndex();
    }

    /**
     * Waits for the specified index to exist
     */
    private static void waitForIndex() throws InterruptedException, IOException {
        int retries = WAIT_FOR_INDEX_MAX_RETRIES;
        while (!getIndex() && retries-- > 0) {
            LOG.debug("Index {} not found, sleeping for {}...", INDEX, WAIT_FOR_INDEX_SLEEP_INTERVAL);
            Thread.sleep(WAIT_FOR_INDEX_SLEEP_INTERVAL);
        }
    }

    private static boolean getIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest(INDEX);
        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
        return response.getSettings() != null && !response.getSettings().isEmpty();
    }
}
