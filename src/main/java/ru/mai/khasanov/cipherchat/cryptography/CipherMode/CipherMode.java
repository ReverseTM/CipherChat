package ru.mai.khasanov.cipherchat.cryptography.CipherMode;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;

import java.util.concurrent.ExecutorService;

public class CipherMode {
    public enum Mode {
        ECB,
        CBC,
        PCBC,
        CFB,
        OFB,
        CTR,
        RD
    }

    public static ACipherMode getInstance(
            Mode mode,
            IEncryptor encryptor,
            byte[] IV,
            ExecutorService executor) {
        return switch (mode) {
            case ECB -> new ECB(encryptor, IV, executor);
            case CBC -> new CBC(encryptor, IV, executor);
            case PCBC -> new PCBC(encryptor, IV);
            case CFB -> new CFB(encryptor, IV, executor);
            case OFB -> new OFB(encryptor, IV);
            case CTR -> new CTR(encryptor, IV, executor);
            case RD -> new RD(encryptor, IV, executor);
        };
    }
}
