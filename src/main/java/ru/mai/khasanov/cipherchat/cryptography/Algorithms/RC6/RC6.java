package ru.mai.khasanov.cipherchat.cryptography.Algorithms.RC6;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;

public class RC6 implements IEncryptor {
    private static final int ROUNDS = 20;

    private final IEncryptor feistelNetwork;

    public RC6() {
        this.feistelNetwork = new RC6FeistelNetwork(new RC6KeyExpand(ROUNDS), ROUNDS);
    }

    @Override
    public byte[] encode(byte[] data) {
        return feistelNetwork.encode(data);
    }

    @Override
    public byte[] decode(byte[] data) {
        return feistelNetwork.decode(data);
    }

    @Override
    public void setKeys(byte[] key) {
        feistelNetwork.setKeys(key);
    }

    @Override
    public int getBlockLength() {
        return feistelNetwork.getBlockLength();
    }
}
