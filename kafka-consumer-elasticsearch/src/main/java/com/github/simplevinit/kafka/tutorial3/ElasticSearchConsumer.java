package com.github.simplevinit.kafka.tutorial3;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchConsumer {

    final static Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class.getName());

    public static RestHighLevelClient createRestClient() {

        // credentials from bonsai cloud
        // https://a9zwi2usvk:qopmw4nf6k@twitter-kafka-poc-8270524124.eu-west-1.bonsaisearch.net:443

        final String hostname = "twitter-kafka-poc-8270524124.eu-west-1.bonsaisearch.net";
        final String username = "a9zwi2usvk";
        final String password = "qopmw4nf6k";

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        final RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(final HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        final RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    public static void main(final String[] args) throws IOException {
        final RestHighLevelClient client = createRestClient();
        final KafkaConsumer<String, String> consumer = createKafkaConsumer();
        IndexRequest indexRequest;
        IndexResponse indexResponse;
        final String jsonTweet = null;

        // Poll messages
        while (true) {
            final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));

            for (final ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                //jsonTweet = "{ \"" + consumerRecord.key() + "\"" + " : " + "\"" + consumerRecord.value() + "\" }";
                logger.info(consumerRecord.value());
                logger.info("Pushing tweets to elastic search");

                indexRequest = new IndexRequest("twitter")
                        .source(consumerRecord.value(), XContentType.JSON);

                indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
                logger.info(indexResponse.getId());
                logger.info(Integer.toString(indexResponse.status().getStatus()));

                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // close the client
            //client.close();
        }

    }

    public static KafkaConsumer<String, String> createKafkaConsumer() {

        // Consumer properties
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "elastic-search-application");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Create Consumer
        final KafkaConsumer<String, String> consumer = new KafkaConsumer(properties);

        // Subscribe Consumer
        consumer.subscribe(Collections.singleton("first_opic"));

        return consumer;
    }

}
