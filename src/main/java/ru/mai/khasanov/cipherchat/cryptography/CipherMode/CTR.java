package ru.mai.khasanov.cipherchat.cryptography.CipherMode;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class CTR extends ACipherMode {
    public CTR(IEncryptor encryptor, byte[] IV, ExecutorService executor) {
        super(encryptor, IV, encryptor.getBlockLength(), executor);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return processData(data);
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return processData(data);
    }

    private byte[] processData(byte[] data) {
        byte[] result = new byte[data.length];

        int numBlocks = data.length / blockLength;
        List<Future<?>> futures = new ArrayList<>(numBlocks);

        for (int i = 0; i < numBlocks; ++i) {
            final int index = i;
            futures.add(executorService.submit(() -> {
                int startIndex = index * blockLength;
                byte[] block = new byte[blockLength];
                System.arraycopy(data, startIndex, block, 0, blockLength);

                byte[] blockForProcess = new byte[blockLength];
                int length = blockLength - Integer.BYTES;
                System.arraycopy(IV, 0, blockForProcess, 0, length);

                byte[] counterInBytes = new byte[Integer.BYTES];
                for (int j = 0; j < counterInBytes.length; ++j) {
                    counterInBytes[j] = (byte) (index >> (3 - j) * 8);
                }
                System.arraycopy(counterInBytes, 0, blockForProcess, length, counterInBytes.length);

                byte[] processedBlock = Util.xor(block, encryptor.encode(blockForProcess));
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
