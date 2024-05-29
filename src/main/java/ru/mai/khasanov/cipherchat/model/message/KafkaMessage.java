package ru.mai.khasanov.cipherchat.model.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public record KafkaMessage(Action action, Object content) {
    public enum Action {
        SETUP_CONNECTION,
        EXCHANGE_PUBLIC_KEY,
        CLEAR_MESSAGES,
        MESSAGE
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(KafkaMessage.class, new KafkaMessageAdapter())
            .create();

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    public static KafkaMessage toKafkaMessage(String json) {
        return gson.fromJson(json, KafkaMessage.class);
    }
}
