package ru.mai.khasanov.cipherchat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class RoomCipherInfo {
    @Column(name = "encryption_algorithm")
    @NotBlank(message = "Encryption algorithm cannot be blank")
    @NotNull
    private String encryptionAlgorithm;

    @Column(name = "cipher_mode")
    @NotBlank(message = "Cipher mode cannot be blank")
    @NotNull
    private String cipherMode;

    @Column(name = "padding_mode")
    @NotBlank(message = "Padding name cannot be blank")
    @NotNull
    private String paddingMode;

    @Column(name = "iv")
    private byte[] IV;

    @Column(name = "primitive_root")
    private byte[] g;

    @Column(name = "modulo")
    private byte[] p;
}
