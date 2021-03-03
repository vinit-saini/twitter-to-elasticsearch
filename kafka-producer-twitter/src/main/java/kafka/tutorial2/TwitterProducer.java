package kafka.tutorial2;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterProducer {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    TwitterProducer() {
    }

    public static void main(final String[] args) {
        //final TwitterProducer twitterProducer = new TwitterProducer();
        new TwitterProducer().run();
    }

    public void run() {

        logger.info("Setup");
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        final BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(1000);

        // create twitter client
        final Client client = createTwitterClient(msgQueue);
        // attempt to establish the connection
        logger.info("client connect");
        client.connect();

        // create a kafka producer
        final KafkaProducer<String, String> kafkaProducer = createKafkaProducer();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook called");
            logger.info("Shutting down client from twitter");
            client.stop();
            logger.info("Closing the producer");
            kafkaProducer.close();
            logger.info("done!");
        }));

        // loop to send tweets to kafka
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                logger.error("Interrupted", e);
                client.stop();
            }
            if (msg != null) {
                logger.info("Sending Tweet to kafka");
                final ProducerRecord<String, String> record = new ProducerRecord<>("first_opic", msg);
                kafkaProducer.send(record, (recordMetadata, e) -> {
                    if (e == null) {
                        logger.info("Received new metadata. \n" +
                                "Topic: " + recordMetadata.topic() + "\n" +
                                "Partition: " + recordMetadata.partition() + "\n" +
                                "Offset: " + recordMetadata.offset() + "\n" +
                                "Timestamp: " + recordMetadata.timestamp());
                    } else {
                        logger.error("Error while producing ", e);
                    }
                });
            }
            //kafkaProducer.flush();
        }
        logger.info("End of application");
    }

    public KafkaProducer createKafkaProducer() {

        logger.info("Create kafka producer");
        final Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // for high throughput (at the expanse of some CPU cycles and bit of latency)
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, Integer.toString(20)); // 20 ms latency
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); // 32KB batch size

        return new KafkaProducer<String, String>(properties);
    }

    public Client createTwitterClient(final BlockingQueue<String> msgQueue) {

        final String consumerKey = "tiIIdsCFTr0CM1lFF6sND0Eff";
        final String consumerSecret = "hDZniPbjOxSmRzPREwSbmB3sZ0pEBgbufQ7nxTf3p6bnEsO2HV";
        final String accessToken = "361675837-jefwM1cgUnJKb5QI9TUGnmjjSaRlFc49YFTBSknN";
        final String accessTokenSecret = "fkyy48Y18C4akA3LZm6tQhQT26uSoMAis6tRUoizOWgBL";

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        final Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        final StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms
        //List<Long> followings = Lists.newArrayList(1234L, 566788L);
        final List<String> terms = Lists.newArrayList("india");
        //hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        final Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, accessToken, accessTokenSecret);

        // Creating a client:

        final ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01") // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));
        //.eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

        final Client hosebirdClient = builder.build();

        return hosebirdClient;
    }
}
