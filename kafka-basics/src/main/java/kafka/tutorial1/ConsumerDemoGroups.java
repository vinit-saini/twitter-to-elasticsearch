package kafka.tutorial1;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoGroups {

    public static void main(final String[] args) {
        final Logger logger = LoggerFactory.getLogger(ConsumerDemoGroups.class.getName());

        // Consumer properties
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "my-java-application-3");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Create Consumer
        final KafkaConsumer<String, String> consumer = new KafkaConsumer(properties);

        // Subscribe Consumer
        consumer.subscribe(Collections.singleton("first_opic"));

        // Poll messages
        while (true) {
            final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));

            for (final ConsumerRecord consumerRecord : consumerRecords) {
                logger.info("Key: " + consumerRecord.key() + ", Value: " + consumerRecord.value());
                logger.info("Partition: " + consumerRecord.partition() + ", Offset: " + consumerRecord.offset() + "\n");
            }
        }
    }
}
