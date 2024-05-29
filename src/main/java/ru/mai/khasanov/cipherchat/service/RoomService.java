package ru.mai.khasanov.cipherchat.service;

import org.springframework.stereotype.Service;
import ru.mai.khasanov.cipherchat.cryptography.Utils.IVGenerator;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.RoomCipherInfo;
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

    public void createRoom(
            long userId,
            String name,
            String encryptionAlgorithm,
            String cipherMode,
            String paddingMode,
            byte[] g,
            byte[] p) {

        RoomCipherInfo info = RoomCipherInfo.builder()
                .encryptionAlgorithm(encryptionAlgorithm)
                .cipherMode(cipherMode)
                .paddingMode(paddingMode)
                .IV(IVGenerator.generate(16))
                .g(g)
                .p(p)
                .build();

        Room room = Room.builder()
                .ownerUserId(userId)
                .name(name)
                .roomCipherInfo(info)
                .build();

        roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        return roomRepository.existsByName(name);
    }

    public Optional<Room> getRoomById(Long roomId) {
        return roomRepository.findById(roomId);
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
}
