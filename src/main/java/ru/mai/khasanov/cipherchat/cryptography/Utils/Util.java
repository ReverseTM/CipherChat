package ru.mai.khasanov.cipherchat.cryptography.Utils;

public class Util {
    public static byte[] xor(byte[] x, byte[] y) {
        var size = Math.min(x.length, y.length);

        var result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) (x[i] ^ y[i]);
        }
        return result;
    }

    public static int leftRotate(int val, int pas) {
        return (val << pas) | (val >>> (32 - pas));
    }

    public static int rightRotate(int val, int pas) {
        return (val >>> pas) | (val << (32 - pas));
    }

    public static int toInt(byte[] data, int offset) {
        return ((data[offset++] & 0xff) | ((data[offset++] & 0xff) << 8) | ((data[offset++] & 0xff) << 16) | ((data[offset++] & 0xff) << 24));
    }

    public static int myToInt(byte[] data, int offset) {
        return (((data[offset++] & 0xff) << 24) | ((data[offset++] & 0xff) << 16) | ((data[offset++] & 0xff) << 8) | (data[offset++] & 0xff));
    }

    public static byte[] toByteArray(int[] data) {
        byte[] array = new byte[data.length * 4];

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < 4; j++) {
                array[i * 4 + j] = (byte) ((data[i] >>> (j * 8)) & 0xff);
            }
        }

        return array;
    }

    public static byte[][] to2DimByteArray(int[] data) {
        int dataLength = data.length;
        byte[][] array = new byte[dataLength][4];

        for (int i = 0; i < dataLength; i++) {
            array[i][0] = (byte) (data[i] & 0xff);
            array[i][1] = (byte) ((data[i] >>> 8) & 0xff);
            array[i][2] = (byte) ((data[i] >>> 16) & 0xff);
            array[i][3] = (byte) ((data[i] >>> 24) & 0xff);
        }

        return array;
    }
}
