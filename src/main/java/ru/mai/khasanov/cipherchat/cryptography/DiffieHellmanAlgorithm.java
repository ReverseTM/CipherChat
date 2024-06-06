package ru.mai.khasanov.cipherchat.cryptography;

import lombok.AllArgsConstructor;
import org.atmosphere.interceptor.AtmosphereResourceStateRecovery;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@AllArgsConstructor
public class DiffieHellmanAlgorithm {
    private static final Random randomGenerator = new SecureRandom();

    public static BigInteger[] generateParams(int bitLength) {
        BigInteger p = BigInteger.probablePrime(bitLength, randomGenerator);
        BigInteger g = findPrimitiveRoot(p);

        return new BigInteger[]{g, p};
    }

    private static BigInteger findPrimitiveRoot(BigInteger p) {
        BigInteger pMinusOne = p.subtract(BigInteger.ONE);
        List<BigInteger> factors = primeFactors(pMinusOne);

        for (BigInteger g = BigInteger.TWO; g.compareTo(p) < 0; g = g.add(BigInteger.ONE)) {
            boolean isPrimitiveRoot = true;
            for (BigInteger factor : factors) {
                BigInteger exponent = pMinusOne.divide(factor);
                if (g.modPow(exponent, p).equals(BigInteger.ONE)) {
                    isPrimitiveRoot = false;
                    break;
                }
            }
            if (isPrimitiveRoot) {
                return g;
            }
        }
        return null;
    }

    private static List<BigInteger> primeFactors(BigInteger number) {
        List<BigInteger> factors = new ArrayList<>();
        BigInteger two = BigInteger.valueOf(2);
        while (number.mod(two).equals(BigInteger.ZERO)) {
            if (!factors.contains(two)) {
                factors.add(two);
            }
            number = number.divide(two);
        }
        for (BigInteger i = BigInteger.valueOf(3); i.compareTo(number.sqrt()) <= 0; i = i.add(two)) {
            while (number.mod(i).equals(BigInteger.ZERO)) {
                if (!factors.contains(i)) {
                    factors.add(i);
                }
                number = number.divide(i);
            }
        }
        if (number.compareTo(BigInteger.ONE) > 0) {
            factors.add(number);
        }
        return factors;
    }

    public static byte[] generateOwnPrivateKey() {
        return new BigInteger(32, randomGenerator).toByteArray();
    }

    public static byte[] generateOwnPublicKey(byte[] privateKey, byte[] g, byte[] p) {
        BigInteger G = new BigInteger(g);
        BigInteger P = new BigInteger(p);
        BigInteger ownPrivateKey = new BigInteger(privateKey);

        return G.modPow(ownPrivateKey, P).toByteArray();
    }

    public static byte[] calculateSharedPrivateKey(byte[] anotherUserPublicKey, byte[] ownPrivateKey, byte[] p) {
        BigInteger publicKey = new BigInteger(anotherUserPublicKey);
        BigInteger privateKey = new BigInteger(ownPrivateKey);
        BigInteger P = new BigInteger(p);
        BigInteger key = publicKey.modPow(privateKey, P);

        byte[] sharedPrivateKey = new byte[16];
        int length = Math.min(key.toByteArray().length, sharedPrivateKey.length);
        System.arraycopy(key.toByteArray(), 0, sharedPrivateKey, 0, length);

        return sharedPrivateKey;
    }

}
