package ru.mai.khasanov.cipherchat.cryptography;

import ru.mai.khasanov.cipherchat.cryptography.Algorithms.MARS.MARS;
import ru.mai.khasanov.cipherchat.cryptography.Algorithms.RC6.RC6;
import ru.mai.khasanov.cipherchat.cryptography.CipherMode.CipherMode;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Padding.PaddingMode;
import ru.mai.khasanov.cipherchat.model.RoomCipherInfo;

public class CipherFactory {
    public static CipherService createCipherService(RoomCipherInfo cipherInfo, byte[] key) {
        return new CipherService(
                key,
                getAlgorithm(cipherInfo.getEncryptionAlgorithm()),
                getCipherMode(cipherInfo.getCipherMode()),
                getPadding(cipherInfo.getPaddingMode()),
                cipherInfo.getIV()
        );
    }

    private static IEncryptor getAlgorithm(String algorithm) {
        return switch (algorithm) {
            case "MARS" -> new MARS();
            case "RC6" -> new RC6();
            default -> throw new IllegalStateException("Unexpected algorithm name");
        };
    }

    private static CipherMode.Mode getCipherMode(String cipherMode) {
        return switch (cipherMode) {
            case "ECB" -> CipherMode.Mode.ECB;
            case "CBC" -> CipherMode.Mode.CBC;
            case "CFB" -> CipherMode.Mode.CFB;
            case "PCBC" -> CipherMode.Mode.PCBC;
            case "OFB" -> CipherMode.Mode.OFB;
            case "CTR" -> CipherMode.Mode.CTR;
            case "RD" -> CipherMode.Mode.RD;
            default -> throw new IllegalStateException("Unexpected cipher mode");
        };
    }

    private static PaddingMode.Mode getPadding(String padding) {
        return switch (padding) {
            case "Zeros" -> PaddingMode.Mode.Zeros;
            case "PKCS7" -> PaddingMode.Mode.PKCS7;
            case "ANSI_X_923" -> PaddingMode.Mode.ANSI_X_923;
            case "ISO_10126" -> PaddingMode.Mode.ISO_10126;
            default -> throw new IllegalStateException("Unexpected padding");
        };
    }
}
