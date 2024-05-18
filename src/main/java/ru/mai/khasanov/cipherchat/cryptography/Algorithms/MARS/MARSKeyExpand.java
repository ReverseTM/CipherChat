package ru.mai.khasanov.cipherchat.cryptography.Algorithms.MARS;

import ru.mai.khasanov.cipherchat.cryptography.Algorithms.Constant.MARSConstants;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IKeyExpand;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

public class MARSKeyExpand implements IKeyExpand {
    @Override
    public byte[][] genKeys(byte[] key) {
        int n = key.length >> 2;

        if (n < 4 || n > 14) {
            throw new IllegalArgumentException("Key length must be between 16 and 56 bytes");
        }

        // Initialize T[] With the Original Key Data
        int[] T = new int[15];

        for (int i = 0; i < n; i++) {
            T[i] = key[i];
        }
        T[n] = n;

        int[] K = new int[40];

        for (int j = 0; j < 4; j++) {
            // Linear Key-Word Expansion
            for (int i = 0; i < 15; i++) {
                T[i] ^= (Util.leftRotate(T[(i + 8) % 15] ^ T[(i + 13) % 15], 3)) ^ (4 * i + j);
            }

            // S-box Based Stirring of Key-Words
            for (int k = 0; k < 4; k++) {
                for (int i = 0; i < 15; i++) {
                    T[i] = Util.leftRotate(T[i] + MARSConstants.S[T[(i + 14) % 15] & 0x1ff], 9);
                }
            }

            // Store Next 10 Key-Words into K[]
            for (int i = 0; i < 10; i++) {
                K[10 * j + i] = T[(4 * i) % 15];
            }
        }

        // Modifying Multiplication Key-Words
        int M, p, r, w, j;
        for (int i = 5; i < 36; i += 2) {
            j = K[i] & 0x3;
            w = K[i] | 0x3;

            M = generateMask(w);
            r = K[i - 1] & 0x1f;
            p = Util.leftRotate(MARSConstants.B[j], r);

            K[i] = w ^ (p & M);
        }

        return Util.to2DimByteArray(K);
    }

    private int generateMask(int w) {
        int M;

        M = (~w ^ (w >>> 1)) & 0x7fffffff;
        M &= (M >> 1) & (M >> 2);
        M &= (M >> 3) & (M >> 6);

        M <<= 1;
        M |= (M << 1);
        M |= (M << 2);
        M |= (M << 4);

        return M & 0xfffffffc;
    }
}
