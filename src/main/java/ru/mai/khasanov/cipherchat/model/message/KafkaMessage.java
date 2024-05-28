package ru.mai.khasanov.cipherchat.model.message;

import com.google.gson.Gson;

public record KafkaMessage(Action action, Object content) {
    public enum Action {
        SETUP_CONNECTION,
        EXCHANGE_PUBLIC_KEY,
        SEND_MESSAGE
    }

    private static final Gson gson = new Gson();

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    public static KafkaMessage toKafkaMessage(String json) {
        return gson.fromJson(json, KafkaMessage.class);
    }
}
