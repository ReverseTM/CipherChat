package ru.mai.khasanov.cipherchat.cryptography.CipherMode;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class RD extends ACipherMode {
    private final BigInteger delta;

    public RD(IEncryptor encryptor, byte[] IV, ExecutorService executor) {
        super(encryptor, IV, encryptor.getBlockLength(), executor);
        delta = new BigInteger(Arrays.copyOf(IV, blockLength / 2));
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
        BigInteger initialStart = new BigInteger(IV);

        int numBlocks = data.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(numBlocks);

        for (int i = 0; i < numBlocks; ++i) {
            final int index = i;
            futures.add(executorService.submit(() -> {
                BigInteger initial = initialStart.add(delta.multiply(BigInteger.valueOf(index)));
                int startIndex = index * blockLength;
                byte[] block = new byte[blockLength];
                System.arraycopy(data, startIndex, block, 0, blockLength);
                byte[] processedBlock = encrypt
                        ? encryptor.encode(Util.xor(initial.toByteArray(), block))
                        : Util.xor(encryptor.decode(block), initial.toByteArray());
                System.arraycopy(processedBlock, 0, result, startIndex, processedBlock.length);
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
