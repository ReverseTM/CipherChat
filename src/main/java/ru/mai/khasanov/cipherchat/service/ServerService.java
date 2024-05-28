package ru.mai.khasanov.cipherchat.service;

import com.vaadin.flow.component.notification.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.khasanov.cipherchat.cryptography.DiffieHellmanAlgorithm;
import ru.mai.khasanov.cipherchat.kafka.KafkaWriter;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.model.message.KafkaMessage;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

@Service
public class ServerService {
    private final UserService userService;
    private final RoomService roomService;

    private final KafkaWriter kafkaWriter;

    @Autowired
    public ServerService(UserService userService, RoomService roomService, KafkaWriter kafkaWriter) {
        this.userService = userService;
        this.roomService = roomService;
        this.kafkaWriter = kafkaWriter;
    }

    @Transactional
    public synchronized void createRoom(String name,
                                        String algorithm,
                                        String mode,
                                        String padding) {

        if (roomService.existsByName(name)) {
            Notification.show("Room with same name already exist");
            return;
        }

        BigInteger[] params = DiffieHellmanAlgorithm.generateParams(32);
        byte[] g = params[0].toByteArray();
        byte[] p = params[1].toByteArray();

        roomService.createRoom(name, algorithm, mode, padding, g, p);
        Notification.show("Room created successfully");
    }

    @Transactional
    public synchronized void deleteRoom(long roomId) {
        Optional<Room> maybeRoom = roomService.getRoomById(roomId);

        if (maybeRoom.isEmpty()) {
            Notification.show("Room with id " + roomId + " does not exist");
            return;
        }

        Room room = maybeRoom.get();

        boolean status;
        Set<User> users = room.getUsers();
        for (User user : users) {
            status = userService.removeRoomFromUser(user, room);
            if (!status) {
                Notification.show("Something wrong");
                return;
            }
        }

        roomService.deleteRoom(roomId);

        Notification.show("Room deleted successfully");
    }

    @Transactional
    public synchronized boolean connectRoom(long userId, long roomId) {
        Optional<Room> maybeRoom = roomService.getRoomById(roomId);
        Optional<User> maybeUser = userService.getUserById(userId);

        if (maybeRoom.isEmpty()) {
            Notification.show("Room with id " + roomId + " does not exist");
            return false;
        }

        if (maybeUser.isEmpty()) {
            Notification.show("User with id " + userId + " does not exist");
            return false;
        }

        Room room = maybeRoom.get();
        User user = maybeUser.get();

        if (room.getUsers().contains(user)) {
            Notification.show("You already joined to this room");
            return false;
        }

        if (room.getUsers().size() == 2) {
            Notification.show("The maximum number of group members has been reached");
            return false;
        }

        long anotherUserId = 0;
        boolean setupConnection = false;
        if (room.getUsers().size() == 1) {
            anotherUserId = room.getUsers().iterator().next().getId();
            setupConnection = true;
        }

        roomService.addUserToRoom(user, room);
        userService.addRoomToUser(user, room);

        Notification.show("You joined to room successfully");

        if (setupConnection) {
            exchangeInformation(userId, anotherUserId, roomId);
        }

        return true;
    }

    private void exchangeInformation(long thisUserId, long anotherUserId, long roomId) {
        String thisUserTopic = String.format("input_room_%s_user_%s", roomId, thisUserId);
        String anotherUserTopic = String.format("input_room_%s_user_%s", roomId, anotherUserId);

        KafkaMessage messageToThisUser = new KafkaMessage(KafkaMessage.Action.SETUP_CONNECTION, anotherUserId);
        KafkaMessage messageToAnotherUser = new KafkaMessage(KafkaMessage.Action.SETUP_CONNECTION, thisUserId);

        kafkaWriter.write(messageToThisUser.toBytes(), thisUserTopic);
        kafkaWriter.write(messageToAnotherUser.toBytes(), anotherUserTopic);
    }

    @Transactional
    public synchronized void disconnectRoom(long userId, long roomId) {
        Optional<Room> maybeRoom = roomService.getRoomById(roomId);
        Optional<User> maybeUser = userService.getUserById(userId);

        if (maybeRoom.isEmpty()) {
            Notification.show("Room with id " + roomId + " does not exist");
            return;
        }

        if (maybeUser.isEmpty()) {
            Notification.show("User with id " + userId + " does not exist");
            return;
        }

        Room room = maybeRoom.get();
        User user = maybeUser.get();

        boolean roomStatus = roomService.removeUserFromRoom(user, room);
        boolean userStatus = userService.removeRoomFromUser(user, room);

        if (!roomStatus || !userStatus) {
            Notification.show("An error occurred while disconnecting from the room");
            return;
        }

        Notification.show("You have been successfully disconnected from the room");
    }
}
