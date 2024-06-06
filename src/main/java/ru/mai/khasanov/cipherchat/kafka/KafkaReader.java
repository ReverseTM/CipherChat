package ru.mai.khasanov.cipherchat.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class KafkaReader {
    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final ExecutorService executorService;

    private Consumer<ConsumerRecord<byte[], byte[]>> listener;
    private volatile boolean isRunning;

    private static final String BOOTSTRAP_SERVERS = "localhost:9093";
    private static final String GROUP_ID = "chat_group";
    private static final String AUTO_OFFSET_RESET = "earliest";

    public KafkaReader() {
        this.kafkaConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
                ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET
        ), new ByteArrayDeserializer(), new ByteArrayDeserializer());

        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startKafkaConsumer() {
        this.isRunning = true;

        executorService.execute(() -> {
            try {
                while (isRunning) {
                    ConsumerRecords<byte[], byte[]> consumerRecords = this.kafkaConsumer.poll(Duration.ofMillis(3000));

                    for (ConsumerRecord<byte[], byte[]> consumerRecord : consumerRecords) {
                        handleKafkaMessage(consumerRecord);
                    }
                }
            } catch (WakeupException e) {
                if (isRunning) {
                    throw e;
                }
            }
            finally {
                this.kafkaConsumer.close();
            }

        });
    }

    private void handleKafkaMessage(ConsumerRecord<byte[], byte[]> consumerRecord) {
        if (this.listener != null) {
            this.listener.accept(consumerRecord);
        }
    }

    public void subscribe(String topic) {
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    public void addListener(Consumer<ConsumerRecord<byte[], byte[]>> listener) {
        this.listener = listener;
    }

    public void stop() {
        if (kafkaConsumer != null) {
            this.isRunning = false;
            kafkaConsumer.wakeup();
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
