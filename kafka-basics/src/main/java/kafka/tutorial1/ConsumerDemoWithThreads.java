package kafka.tutorial1;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoWithThreads {

    public static void main(final String[] args) {
        new ConsumerDemoWithThreads().startConsumer();
    }

    private ConsumerDemoWithThreads() {

    }

    private void startConsumer() {
        final Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThreads.class.getName());
        final String bootstrapServer = "127.0.0.1:9092";
        final String topic = "first-opic";
        final String groupId = "my-java-application-22";

        // latch for dealing with multiple threads
        final CountDownLatch latch = new CountDownLatch(1);

        // Creating the consumer runnable
        logger.info("Creating the consumer thread");
        final Runnable consumerRunnableTask = new ConsumerRunnableTask(bootstrapServer, topic, groupId, latch);

        // Create Thread
        final Thread myApplicationThread = new Thread(consumerRunnableTask);
        myApplicationThread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnableTask) consumerRunnableTask).shutdown();
            try {
                latch.await();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited");
        }));

        try {
            latch.await();
        } catch (final InterruptedException e) {
            logger.error("Application got interrupted", e);
        } finally {
            logger.info("Application is closing");
        }
    }

    class ConsumerRunnableTask implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(ConsumerRunnableTask.class.getName());
        private final String bootStrapServer;
        private final String topic;
        private final String groupId;
        private final CountDownLatch latch;

        private KafkaConsumer<String, String> consumer;
        private Properties properties;

        ConsumerRunnableTask(final String bootstrapServer, final String topic, final String groupId, final CountDownLatch latch) {
            bootStrapServer = bootstrapServer;
            this.topic = topic;
            this.groupId = groupId;
            this.latch = latch;
        }

        @Override
        public void run() {

            // Consumer properties
            properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            // Create Consumer
            consumer = new KafkaConsumer(properties);

            // Subscribe Consumer
            consumer.subscribe(Collections.singleton(topic));

            // Poll messages
            try {
                while (true) {
                    final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
                    for (final ConsumerRecord consumerRecord : consumerRecords) {
                        logger.info("Key: " + consumerRecord.key() + ", Value: " + consumerRecord.value());
                        logger.info("Partition: " + consumerRecord.partition() + ", Offset: " + consumerRecord.offset() + "\n");
                    }
                }
            } catch (final WakeupException ex) {
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();

                //tell the main code we're done with the consumer
                latch.countDown();
            }
        }

        public void shutdown() {
            // the wakeup() method is a special method to interrupt consumer.poll()
            // it will throw the exception WakeupException
            consumer.wakeup();
        }
    }

}
