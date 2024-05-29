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

    @Column(name = "owner_user_id")
    @NotNull
    @EqualsAndHashCode.Include
    private Long ownerUserId;

    @Column(name = "name", unique = true)
    @NotBlank(message = "Room Name cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String name;

    @Embedded
    @NotNull
    @EqualsAndHashCode.Include
    private RoomCipherInfo roomCipherInfo;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "room_user",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();
}
