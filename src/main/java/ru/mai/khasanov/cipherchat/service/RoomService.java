package ru.mai.khasanov.cipherchat.service;

import org.springframework.stereotype.Service;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom(
            String name,
            String encryptionAlgorithm,
            String cipherMode,
            String paddingMode,
            byte[] g,
            byte[] p) {

        Room room = Room.builder()
                .name(name)
                .encryptionAlgorithm(encryptionAlgorithm)
                .cipherMode(cipherMode)
                .paddingMode(paddingMode)
                .g(g)
                .p(p)
                .build();

        return roomRepository.save(room);
    }

    public boolean existsByName(String name) {
        return roomRepository.existsByName(name);
    }

    public Optional<Room> getRoomById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    public Optional<Room> getRoomByName(String roomName) {
        return roomRepository.findByName(roomName);
    }

    public List<Room> getRoomsByUserId(Long userId) {
        return roomRepository.findAllByUsersId(userId);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public void addUserToRoom(User user, Room room) {
        room.getUsers().add(user);
        roomRepository.save(room);
    }

    public boolean removeUserFromRoom(User user, Room room) {
        if (room.getUsers().contains(user)) {
            room.getUsers().remove(user);
            roomRepository.save(room);
            return true;
        }
        return false;
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}
