package ru.mai.khasanov.cipherchat.cryptography.Algorithms.MARS;

import ru.mai.khasanov.cipherchat.cryptography.Algorithms.Constant.MARSConstants;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IKeyExpand;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IRoundFunction;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

public class MARSFeistelNetwork implements IEncryptor {
    private final IKeyExpand keyExpand;

    private final IRoundFunction feistelFunction;

    private int[] roundKeys;

    public MARSFeistelNetwork(IKeyExpand keyExpand, IRoundFunction feistelFunction) {
        this.keyExpand = keyExpand;
        this.feistelFunction = feistelFunction;
    }

    @Override
    public byte[] encode(byte[] in) {
        int A = Util.toInt(in, 0) + roundKeys[0];
        int B = Util.toInt(in, 4) + roundKeys[1];
        int C = Util.toInt(in, 8) + roundKeys[2];
        int D = Util.toInt(in, 12) + roundKeys[3];

        // Forward mixing
        for (int i = 0; i < 8; i++) {
            B = (B ^ MARSConstants.S[A & 0xff]) + MARSConstants.S[(Util.rightRotate(A, 8) & 0xff) + 256];
            C = C + MARSConstants.S[Util.rightRotate(A, 16) & 0xff];
            D = D ^ MARSConstants.S[(Util.rightRotate(A, 24) & 0xff) + 256];

            A = Util.rightRotate(A, 24);
            if (i == 1 || i == 5) {
                A += B;
            } else if (i == 0 || i == 4) {
                A += D;
            }

            int tmp = A;
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        // Cryptographic core
        int L, M, R;
        int[] block = new int[1];
        int[] roundKey = new int[2];

        for (int i = 0; i < 16; i++) {
            block[0] = A;
            roundKey[0] = roundKeys[2 * i + 4];
            roundKey[1] = roundKeys[2 * i + 5];

            byte[] data = feistelFunction.eFunction(Util.toByteArray(block), Util.toByteArray(roundKey));
            L = Util.toInt(data, 0);
            M = Util.toInt(data, 4);
            R = Util.toInt(data, 8);

            C = C + M;
            if (i < 8) {
                B += L;
                D ^= R;
            } else {
                B ^= R;
                D += L;
            }

            int tmp = Util.leftRotate(A, 13);
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        // Backwards mixing
        for (int i = 0; i < 8; i++) {
            if (i == 3 || i == 7) {
                A -= B;
            } else if (i == 2 || i == 6) {
                A -= D;
            }

            B = B ^ MARSConstants.S[(A & 0xff) + 256];
            C = C - MARSConstants.S[Util.leftRotate(A, 8) & 0xff];
            D = (D - MARSConstants.S[(Util.leftRotate(A, 16) & 0xff) + 256]) ^ (MARSConstants.S[Util.leftRotate(A, 24) & 0xff]);

            int tmp = Util.leftRotate(A, 24);
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        int[] data = new int[4];
        data[0] = A - roundKeys[36];
        data[1] = B - roundKeys[37];
        data[2] = C - roundKeys[38];
        data[3] = D - roundKeys[39];

        byte[] encoded = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            encoded[i] = (byte) (data[i / 4] >>> ((i % 4) * 8) & 0xff);
        }

        return encoded;
    }

    @Override
    public byte[] decode(byte[] in) {
        int A = Util.toInt(in, 0) + roundKeys[36];
        int B = Util.toInt(in, 4) + roundKeys[37];
        int C = Util.toInt(in, 8) + roundKeys[38];
        int D = Util.toInt(in, 12) + roundKeys[39];

        // Forward mixing
        for (int i = 7; i >= 0; i--) {
            int tmp = Util.rightRotate(D, 24);
            D = C;
            C = B;
            B = A;
            A = tmp;

            D = (D ^ MARSConstants.S[Util.leftRotate(A, 24) & 0xff]) + (MARSConstants.S[(Util.leftRotate(A, 16) & 0xff) + 256]);
            C = C + MARSConstants.S[Util.leftRotate(A, 8) & 0xff];
            B = B ^ MARSConstants.S[(A & 0xff) + 256];

            if (i == 3 || i == 7) {
                A += B;
            } else if (i == 2 || i == 6) {
                A += D;
            }
        }

        // Cryptographic core
        int L, M, R;
        int[] block = new int[1];
        int[] roundKey = new int[2];

        for (int i = 15; i >= 0; i--) {
            int tmp = Util.rightRotate(D, 13);
            D = C;
            C = B;
            B = A;
            A = tmp;

            block[0] = A;
            roundKey[0] = roundKeys[2 * i + 4];
            roundKey[1] = roundKeys[2 * i + 5];

            byte[] data = feistelFunction.eFunction(Util.toByteArray(block), Util.toByteArray(roundKey));
            L = Util.toInt(data, 0);
            M = Util.toInt(data, 4);
            R = Util.toInt(data, 8);

            C = C - M;
            if (i < 8) {
                B -= L;
                D ^= R;
            } else {
                B ^= R;
                D -= L;
            }
        }

        // Backwards mixing
        for (int i = 7; i >= 0; i--) {
            int tmp = D;
            D = C;
            C = B;
            B = A;
            A = tmp;

            if (i == 1 || i == 5) {
                A -= B;
            } else if (i == 0 || i == 4) {
                A -= D;
            }

            A = Util.leftRotate(A, 24);
            D = D ^ MARSConstants.S[(Util.rightRotate(A, 24) & 0xff) + 256];
            C = C - MARSConstants.S[Util.rightRotate(A, 16) & 0xff];
            B = (B - MARSConstants.S[(Util.rightRotate(A, 8) & 0xff) + 256]) ^ (MARSConstants.S[A & 0xff]);
        }

        int[] data = new int[4];
        data[0] = A - roundKeys[0];
        data[1] = B - roundKeys[1];
        data[2] = C - roundKeys[2];
        data[3] = D - roundKeys[3];

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
