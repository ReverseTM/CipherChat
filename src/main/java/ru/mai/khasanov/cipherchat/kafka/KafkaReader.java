package ru.mai.khasanov.cipherchat.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class KafkaReader {
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;

    private static final String BOOTSTRAP_SERVERS = "localhost:9093";
    private static final String AUTO_OFFSET_RESET = "earliest";

    public KafkaReader(String topic, long roomId) {
        this.kafkaConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
                ConsumerConfig.GROUP_ID_CONFIG, "group_" + roomId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET
        ), new ByteArrayDeserializer(), new ByteArrayDeserializer());

        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    public void read() {
        boolean isRun = true;

        while (isRun) {
            ConsumerRecords<byte[], byte[]> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                // TODO
            }
        }
    }
}
