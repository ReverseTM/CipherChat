package ru.mai.khasanov.cipherchat.cryptography.CipherMode;

import ru.mai.khasanov.cipherchat.cryptography.CipherMode.ACipherMode;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ECB extends ACipherMode {
    public ECB(IEncryptor encryptor, byte[] IV, ExecutorService executor) {
        super(encryptor, IV, encryptor.getBlockLength(), executor);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return processData(data, true);
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return processData(data, false);
    }

    private byte[] processData(byte[] data, boolean encrypt) {
        byte[] result = new byte[data.length];

        int numBlocks = data.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(numBlocks);

        for (int i = 0; i < numBlocks; ++i) {
            final int index = i;
            futures.add(executorService.submit(() -> {
                int startIndex = index * blockLength;
                byte[] block = new byte[blockLength];
                System.arraycopy(data, startIndex, block, 0, blockLength);
                block = encrypt ? encryptor.encode(block) : encryptor.decode(block);
                System.arraycopy(block, 0, result, startIndex, block.length);
            }));
        }

        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }
}