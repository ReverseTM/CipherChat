package ru.mai.khasanov.cipherchat.cryptography.Interfaces;

public interface IRoundFunction {
    byte[] eFunction(byte[] block, byte[] roundKey);
}
