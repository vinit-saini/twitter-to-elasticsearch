package kafka.tutorial1;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerDemo {

    public static void main(final String[] args) {
        // create producer properties
        final Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create producer
        final KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // create record
        final ProducerRecord<String, String> record = new ProducerRecord<>("first_opic", "hello world!");

        // send data
        producer.send(record);

        producer.flush();
        producer.close();
    }

}
