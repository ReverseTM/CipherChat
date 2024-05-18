package ru.mai.khasanov.cipherchat.cryptography.Algorithms.RC6;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IKeyExpand;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

public class RC6KeyExpand implements IKeyExpand {
    private static final int P_32 = 0xb7e15163;

    private static final int Q_32 = 0x9e3779b9;

    private static final int W = 32;

    private final int rounds;

    public RC6KeyExpand(int rounds) {
        this.rounds = rounds;
    }

    @Override
    public byte[][] genKeys(byte[] key) {
        int u = W / 8;
        int c = key.length / u;
        int t = 2 * rounds + 4;

        int[] L = new int[c];
        for (int i = 0, offset = 0; i < c; i++, offset += 4) {
            L[i] = Util.toInt(key, offset);
        }

        int[] S = new int[t];

        S[0] = P_32;
        for (int i = 1; i < t; i++) {
            S[i] = S[i - 1] + Q_32;
        }

        int A = 0, B = 0, i = 0, j = 0;
        int v = Math.max(c, t);

        for (int s = 0; s < v; s++) {
            A = S[i] = Util.leftRotate(S[i] + A + B, 3);
            B = L[j] = Util.leftRotate(L[j] + A + B, A + B);
            i = (i + 1) % t;
            j = (j + 1) % c;
        }

        return Util.to2DimByteArray(S);
    }
}