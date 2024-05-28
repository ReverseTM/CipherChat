package ru.mai.khasanov.cipherchat.cryptography.Utils;

import java.security.SecureRandom;
import java.util.Random;

public class IVGenerator {
    public static byte[] generate(int sizeInBytes) {
        byte[] IV = new byte[sizeInBytes];
        (new SecureRandom()).nextBytes(IV);

        return IV;
    }
}
