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
public class CryptoContext implements AutoCloseable {
    private static final int BLOCK_SIZE = 2_097_152; // 2Mb

    private final ExecutorService executorService;
    private final int blockLength;
    private final ACipherMode cipherMode;
    private final IPadding padding;

    public CryptoContext(
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

    private byte[] encrypt(byte[] text, boolean applyPadding) {
        return applyPadding
                ? cipherMode.encrypt(padding.applyPadding(text, blockLength))
                : cipherMode.encrypt(text);
    }

    private byte[] decrypt(byte[] cipherText, boolean removePadding) {
        return removePadding
                ? padding.removePadding(cipherMode.decrypt(cipherText))
                : cipherMode.decrypt(cipherText);
    }

    public CompletableFuture<byte[]> encrypt(byte[] text) {
        log.info("Starting encryption");
        return CompletableFuture.supplyAsync(() -> encrypt(text, true));
    }

    public CompletableFuture<byte[]> decrypt(byte[] cipherText) {
        log.info("Starting decryption");
        return CompletableFuture.supplyAsync(() -> decrypt(cipherText, true));
    }

    public CompletableFuture<Void> encrypt(String inputFile, String outputFile) {
        log.info("Starting file encryption");
        return asyncProcess(inputFile, outputFile, true);
    }

    public CompletableFuture<Void> decrypt(String inputFile, String outputFile) {
        log.info("Starting file decryption");
        return asyncProcess(inputFile, outputFile, false);
    }

    private CompletableFuture<Void> asyncProcess(String inputFile, String outputFile, boolean encrypt) {
        if (inputFile == null || outputFile == null) {
            throw new RuntimeException("Input and output files cannot be null");
        }

        try {
            File file = new File(inputFile);
            if (!file.exists()) {
                throw new FileNotFoundException(inputFile);
            }
            long fileLength = file.length();

            return CompletableFuture
                    .supplyAsync(() -> processFile(inputFile, outputFile, fileLength, encrypt));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Void processFile(String inputFile, String outputFile, long fileLength, boolean encrypt) {
        log.info("Start processing file");

        if (inputFile == null || outputFile == null) {
            throw new RuntimeException("Input and output files cannot be null");
        }

        List<Future<?>> futures = new ArrayList<>();


        for (var readBytes = 0L; readBytes < fileLength; readBytes += BLOCK_SIZE) {
            final long finalReadBytes = readBytes;

            futures.add(executorService.submit(() -> {
                byte[] block = readBlock(inputFile, finalReadBytes, fileLength);

                if (encrypt) {
                    block = finalReadBytes + block.length == fileLength
                            ? encrypt(block, true)
                            : encrypt(block, false);
                } else {
                    block = finalReadBytes + block.length == fileLength
                            ? decrypt(block, true)
                            : decrypt(block, false);
                }

                writeFile(outputFile, block, finalReadBytes);
            }));
        }

        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("Finished processing file");

        return null;
    }

    private byte[] readBlock(String inputFile, long offset, long fileLength) {
        try (RandomAccessFile file = new RandomAccessFile(inputFile, "r")) {
            file.seek(offset);
            int bytesRead = 0;

            long unreadBytes = fileLength - offset;
            int arrayLength = (int) (unreadBytes < BLOCK_SIZE ? unreadBytes : BLOCK_SIZE);

            byte[] bytes = new byte[arrayLength];

            while (bytesRead < BLOCK_SIZE && file.getFilePointer() < fileLength) {
                bytes[bytesRead++] = file.readByte();
            }

            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFile(String outputFile, byte[] bytes, long offset) {
        try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
            output.seek(offset);
            for (var value : bytes) {
                output.write(value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
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
