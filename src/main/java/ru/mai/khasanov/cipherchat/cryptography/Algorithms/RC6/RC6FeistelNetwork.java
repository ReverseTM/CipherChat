package ru.mai.khasanov.cipherchat.cryptography.Algorithms.RC6;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IKeyExpand;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

public class RC6FeistelNetwork implements IEncryptor {
    private final IKeyExpand keyExpand;

    private final int rounds;

    private int[] roundKeys;

    public RC6FeistelNetwork(IKeyExpand keyExpand, int rounds) {
        this.keyExpand = keyExpand;
        this.rounds = rounds;
    }

    @Override
    public byte[] encode(byte[] in) {
        int A = Util.toInt(in, 0);
        int B = Util.toInt(in, 4) + roundKeys[0];
        int C = Util.toInt(in, 8);
        int D = Util.toInt(in, 12) + roundKeys[1];

        int t, u;

        for (int i = 1; i <= rounds; i++) {
            t = Util.leftRotate(B * (2 * B + 1), 5);
            u = Util.leftRotate(D * (2 * D + 1), 5);
            A = Util.leftRotate(A ^ t, u) + roundKeys[2 * i];
            C = Util.leftRotate(C ^ u, t) + roundKeys[2 * i + 1];

            int tmp = A;
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        A += roundKeys[2 * rounds + 2];
        C += roundKeys[2 * rounds + 3];

        int[] data = new int[4];
        data[0] = A;
        data[1] = B;
        data[2] = C;
        data[3] = D;

        byte[] encoded = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            encoded[i] = (byte) (data[i / 4] >>> ((i % 4) * 8) & 0xff);
        }

        return encoded;
    }

    @Override
    public byte[] decode(byte[] in) {
        int A = Util.toInt(in, 0) - roundKeys[2 * rounds + 2];
        int B = Util.toInt(in, 4);
        int C = Util.toInt(in, 8) - roundKeys[2 * rounds + 3];
        int D = Util.toInt(in, 12);

        int t, u;

        for (int i = rounds; i >= 1; i--) {
            int tmp = D;
            D = C;
            C = B;
            B = A;
            A = tmp;

            u = Util.leftRotate(D * (2 * D + 1), 5);
            t = Util.leftRotate(B * (2 * B + 1), 5);
            C = Util.rightRotate(C - roundKeys[2 * i + 1], t) ^ u;
            A = Util.rightRotate(A - roundKeys[2 * i], u) ^ t;
        }

        D -= roundKeys[1];
        B -= roundKeys[0];

        int[] data = new int[4];
        data[0] = A;
        data[1] = B;
        data[2] = C;
        data[3] = D;

        byte[] decoded = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            decoded[i] = (byte) (data[i / 4] >>> ((i % 4) * 8) & 0xff);
        }

        return decoded;
    }

    @Override
    public void setKeys(byte[] key) {
        byte[][] keys = keyExpand.genKeys(key);
        this.roundKeys = new int[keys.length];

        for (int i = 0; i < keys.length; i++) {
            this.roundKeys[i] = Util.toInt(keys[i], 0);
        }
    }

    @Override
    public int getBlockLength() {
        return 16;
    }
}