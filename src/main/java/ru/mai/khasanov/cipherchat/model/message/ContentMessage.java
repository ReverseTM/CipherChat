package ru.mai.khasanov.cipherchat.model.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public record ContentMessage (Type messageType, byte[] message, String filename) {
    public enum Type {
        TEXT,
        IMAGE,
        FILE
    }

    private static final Gson gson = new Gson();

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    public static ContentMessage toContentMessage(String json) {
        return gson.fromJson(json, ContentMessage.class);
    }
}
