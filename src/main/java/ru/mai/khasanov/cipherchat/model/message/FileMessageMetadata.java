package ru.mai.khasanov.cipherchat.model.message;

import com.google.gson.Gson;

public record FileMessageMetadata(String messageId, Type type, String filename, long length) {
    public enum Type {
        IMAGE,
        FILE
    }

    private static final Gson gson = new Gson();

    public byte[] toBytes() {
        return gson.toJson(this).getBytes();
    }

    public static FileMessageMetadata toFileMessageMetadata(String json) {
        return gson.fromJson(json, FileMessageMetadata.class);
    }
}
