package kafka.tutorial1;

import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerDemoWithCallback {

    public static void main(final String[] args) {

        final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);

        // create producer properties
        final Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //properties.setProperty(ProducerConfig.PARTITIONER_CLASS_CONFIG, RoundRobinPartitioner.class.getName());

        // create producer
        final KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        String _key;
        for (int i = 1; i <= 15; i++) {
            _key = "key_" + Integer.toString(i);
            // create record
            final ProducerRecord<String, String> record = new ProducerRecord<>("first_opic", _key,
                    "test-message-" + i);

            // send data
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(final RecordMetadata recordMetadata, final Exception e) {
                    // executes every time record is successfully sent or exception is thrown;
                    if (e == null) {
                        logger.info("Received new metadata. \n" +
                                "Topic: " + recordMetadata.topic() + "\n" +
                                "Partition: " + recordMetadata.partition() + "\n" +
                                "Offset: " + recordMetadata.offset() + "\n" +
                                "Timestamp: " + recordMetadata.timestamp());
                    } else {
                        logger.error("Error while producing ", e);
                    }

                }
            });

        }
        producer.flush();
        producer.close();
    }

}
