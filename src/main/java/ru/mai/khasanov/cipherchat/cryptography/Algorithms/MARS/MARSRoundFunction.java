package ru.mai.khasanov.cipherchat.cryptography.Algorithms.MARS;

import ru.mai.khasanov.cipherchat.cryptography.Algorithms.Constant.MARSConstants;
import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IRoundFunction;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

public class MARSRoundFunction implements IRoundFunction {
    @Override
    public byte[] eFunction(byte[] block, byte[] roundKey) {
        int firstKey = Util.toInt(roundKey, 0);
        int secondKey = Util.toInt(roundKey, 4);

        int in = Util.toInt(block, 0);

        int L, M, R;

        R = Util.leftRotate((Util.leftRotate(in, 13) * secondKey), 10);
        M = Util.leftRotate(in + firstKey, Util.rightRotate(R, 5) & 0x1f);
        L = Util.leftRotate(MARSConstants.S[M & 0x1ff] ^ Util.rightRotate(R, 5) ^ R, R & 0x1f);

        return Util.toByteArray(new int[]{L, M, R});
    }
}
