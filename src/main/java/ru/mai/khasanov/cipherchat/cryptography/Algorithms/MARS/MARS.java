package ru.mai.khasanov.cipherchat.cryptography.Algorithms.MARS;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;

public class MARS implements IEncryptor {
    private final IEncryptor feistelNetwork;

    public MARS() {
        this.feistelNetwork = new MARSFeistelNetwork(new MARSKeyExpand(), new MARSRoundFunction());
    }

    @Override
    public byte[] encode(byte[] in) {
        return feistelNetwork.encode(in);
    }

    @Override
    public byte[] decode(byte[] in) {
        return feistelNetwork.decode(in);
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
