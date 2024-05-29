package ru.mai.khasanov.cipherchat.cryptography;

import lombok.extern.slf4j.Slf4j;
import ru.mai.khasanov.cipherchat.cryptography.CipherMode.ACipherMode;
import ru.mai.khasanov.cipherchat.cryptography.CipherMode.CipherMode;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IPadding;
import ru.mai.khasanov.cipherchat.cryptography.Padding.PaddingMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class CipherService {
    private final ExecutorService executorService;
    private final int blockLength;
    private final ACipherMode cipherMode;
    private final IPadding padding;

    public CipherService(
            byte[] key,
            IEncryptor encryptor,
            CipherMode.Mode cypherMode,
            PaddingMode.Mode paddingMode,
            byte[] IV) {
        blockLength = encryptor.getBlockLength();
        encryptor.setKeys(key);

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        padding = PaddingMode.getInstance(paddingMode);
        cipherMode = CipherMode.getInstance(cypherMode, encryptor, IV, executorService);

        log.info("CryptoContext build successfully");
    }

    public CompletableFuture<byte[]> encrypt(byte[] text) {
        log.info("Starting encryption");
        return CompletableFuture.supplyAsync(() -> cipherMode.encrypt(padding.applyPadding(text, blockLength)));
    }

    public CompletableFuture<byte[]> decrypt(byte[] cipherText) {
        log.info("Starting decryption");
        return CompletableFuture.supplyAsync(() -> padding.removePadding(cipherMode.decrypt(cipherText)));
    }

    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
