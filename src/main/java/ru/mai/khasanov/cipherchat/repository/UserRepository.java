package ru.mai.khasanov.cipherchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mai.khasanov.cipherchat.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
