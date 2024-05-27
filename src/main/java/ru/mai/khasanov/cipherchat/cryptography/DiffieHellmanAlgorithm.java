package ru.mai.khasanov.cipherchat.cryptography;

import lombok.AllArgsConstructor;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@AllArgsConstructor
public class DiffieHellmanAlgorithm {
    private static final Random random = new SecureRandom();

    public static BigInteger[] generateParams(int bitLength) {
        BigInteger p = BigInteger.probablePrime(bitLength, random);
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
}
