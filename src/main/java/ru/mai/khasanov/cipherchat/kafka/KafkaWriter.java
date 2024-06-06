package ru.mai.khasanov.cipherchat.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KafkaWriter {
    private final KafkaProducer<byte[], byte[]> kafkaProducer;

    private static final String BOOTSTRAP_SERVERS = "localhost:9093";
    private static final String CLIENT_ID = "kafkaProducer";

    public KafkaWriter() {
        this.kafkaProducer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
                ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID,
                "auto.create.topics.enable", "true"
        ), new ByteArraySerializer(), new ByteArraySerializer());
    }

    public void write(byte[] message, String topic) {
        kafkaProducer.send(new ProducerRecord<>(topic, message));
        kafkaProducer.flush();
    }

    @PreDestroy
    public void stop() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
    }
}