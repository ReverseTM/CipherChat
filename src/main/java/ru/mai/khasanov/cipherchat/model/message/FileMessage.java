package ru.mai.khasanov.cipherchat.model.message;

import com.google.gson.Gson;

public record FileMessage(String messageId, byte[] data, long offset) {
    private static final Gson gson = new Gson();

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    public static FileMessage toMessage(String json) {
        return gson.fromJson(json, FileMessage.class);
    }
}
