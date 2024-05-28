package ru.mai.khasanov.cipherchat.model.message;

public record ExchangeKeyMessage(DestinationMessage destinationMessage, byte[] publicKey) {
}
