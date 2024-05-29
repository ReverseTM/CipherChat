package ru.mai.khasanov.cipherchat.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserById(long id) {
        return userRepository.findById(id);
    }

    public void addRoomToUser(User user, Room room) {
        user.getRooms().add(room);
        userRepository.save(user);
    }

    public boolean removeRoomFromUser(User user, Room room) {
        if (user.getRooms().contains(room)) {
            user.getRooms().remove(room);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
