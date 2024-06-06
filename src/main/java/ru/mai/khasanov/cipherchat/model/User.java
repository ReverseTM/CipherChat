package ru.mai.khasanov.cipherchat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "username", unique = true)
    @NotBlank(message = "Username cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String username;

    @Column(name = "password")
    @NotBlank(message = "Password cannot be blank")
    @NotNull
    @EqualsAndHashCode.Include
    private String password;

    @JsonIgnore
    @ManyToMany(mappedBy = "users", fetch = FetchType.EAGER)
    private Set<Room> rooms = new HashSet<>();

//    @JsonIgnore
//    @OneToMany(mappedBy = "producer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
//    private List<Message> sentMessages = new ArrayList<>();
//
//    @JsonIgnore
//    @OneToMany(mappedBy = "consumer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
//    private List<Message> receivedMessages = new ArrayList<>();
}
