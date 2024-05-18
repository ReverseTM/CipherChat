package ru.mai.khasanov.cipherchat.cryptography.CipherMode;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;

import java.util.concurrent.ExecutorService;

public abstract class ACipherMode {
    protected final IEncryptor encryptor;
    protected final byte[] IV;

    protected ExecutorService executorService;
    protected final int blockLength;

    protected ACipherMode(IEncryptor encryptor, byte[] IV, int blockLength, ExecutorService executor) {
        this.encryptor = encryptor;
        this.IV = IV;
        this.blockLength = blockLength;
        this.executorService = executor;
    }

    public abstract byte[] encrypt(byte[] data);

    public abstract byte[] decrypt(byte[] data);
}
