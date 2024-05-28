package ru.mai.khasanov.cipherchat.model.message;

public record ContentMessage (Type messageType, byte[] message, String filename) {
    public enum Type {
        TEXT,
        IMAGE,
        FILE
    }
}
