package ru.mai.khasanov.cipherchat.cryptography.CipherMode;

import ru.mai.khasanov.cipherchat.cryptography.Interfaces.IEncryptor;
import ru.mai.khasanov.cipherchat.cryptography.Utils.Util;

public class PCBC extends ACipherMode {
    public PCBC(IEncryptor encryptor, byte[] IV) {
        super(encryptor, IV, encryptor.getBlockLength(), null);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return processData(data, true);
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return processData(data, false);
    }

    private byte[] processData(byte[] data, boolean encrypt) {
        byte[] result = new byte[data.length];
        byte[] blockForXor = IV;

        int length = data.length / blockLength;

        for (int i = 0; i < length; ++i) {
            int startIndex = i * blockLength;
            byte[] block = new byte[blockLength];
            System.arraycopy(data, startIndex, block, 0, blockLength);

            byte[] processedBlock = encrypt
                    ? encryptor.encode(Util.xor(block, blockForXor))
                    : Util.xor(blockForXor, encryptor.decode(block));
            System.arraycopy(processedBlock, 0, result, startIndex, processedBlock.length);
            blockForXor = Util.xor(processedBlock, block);
        }

        return result;
    }
}
