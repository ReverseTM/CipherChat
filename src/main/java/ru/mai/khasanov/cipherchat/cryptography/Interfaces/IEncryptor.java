package ru.mai.khasanov.cipherchat.cryptography.Interfaces;

public interface IEncryptor {
    byte[] encode(byte[] data);

    byte[] decode(byte[] data);

    void setKeys(byte[] key);

    int getBlockLength();
}
