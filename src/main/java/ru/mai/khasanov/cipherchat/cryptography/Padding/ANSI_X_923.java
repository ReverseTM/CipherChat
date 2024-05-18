package ru.mai.khasanov.cipherchat.cryptography.Padding;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IPadding;

import java.util.Arrays;

public class ANSI_X_923 implements IPadding {
    @Override
    public byte[] applyPadding(byte[] data, int blockSize) {
        int paddingLength = blockSize - (data.length % blockSize);

        byte[] paddedInput = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, paddedInput, 0, data.length);
        paddedInput[data.length - 1] = (byte) paddingLength;

        return paddedInput;
    }

    @Override
    public byte[] removePadding(byte[] data) {
        return Arrays.copyOf(data, data.length - data[data.length - 1]);
    }
}
