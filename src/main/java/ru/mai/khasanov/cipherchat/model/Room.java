package ru.mai.khasanov.cipherchat.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rooms")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name")
    @NotBlank(message = "Room Name cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String name;

    @Column(name = "encryption_algorithm")
    @NotBlank(message = "Encryption algorithm cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String encryptionAlgorithm;

    @Column(name = "cipher_mode")
    @NotBlank(message = "Cipher mode cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String cipherMode;

    @Column(name = "padding_mode")
    @NotBlank(message = "Padding name cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String paddingMode;

    @Column(name = "primitive_root")
    private byte[] g;

    @Column(name = "modulo")
    private byte[] p;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "room_user",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();
}
