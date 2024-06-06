package ru.mai.khasanov.cipherchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mai.khasanov.cipherchat.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByName(String roomName);

    List<Room> findAllByUsersId(Long userId);

    boolean existsByName(String roomName);
}
