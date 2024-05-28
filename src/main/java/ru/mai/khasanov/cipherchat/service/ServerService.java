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
import ru.mai.khasanov.cipherchat.model.message.DestinationMessage;

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

//    public ServerService(UserService userService, RoomService roomService) {
//        this.userService = userService;
//        this.roomService = roomService;
//    }

    @Transactional
    public synchronized Room createRoom(String name,
                                        String algorithm,
                                        String mode,
                                        String padding) {

        if (roomService.existsByName(name)) {
            Notification.show("Room with same name already exist");
            return null;
        }

        BigInteger[] params = DiffieHellmanAlgorithm.generateParams(32);
        byte[] g = params[0].toByteArray();
        byte[] p = params[1].toByteArray();

        Room room = roomService.createRoom(name, algorithm, mode, padding, g, p);
        Notification.show("Room created successfully");

        return room;
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
    public synchronized void connectRoom(long userId, long roomId) {
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

        if (room.getUsers().contains(user)) {
            Notification.show("You already joined to this room");
            return;
        }

        if (room.getUsers().size() == 2) {
            Notification.show("The maximum number of group members has been reached");
            return;
        }

        roomService.addUserToRoom(user, room);
        userService.addRoomToUser(user, room);

        Notification.show("You joined to room successfully");

        if (room.getUsers().size() == 2) {
            long anotherUserId = room.getUsers().iterator().next().getId();
            exchangeInformation(userId, anotherUserId, roomId);
        }
    }

    private void exchangeInformation(long thisUserId, long anotherUserId, long roomId) {
        String topic = String.format("input_room_%s", roomId);

        DestinationMessage thisUserMessage = new DestinationMessage(thisUserId, anotherUserId);
        DestinationMessage anotherUserMessage = new DestinationMessage(anotherUserId, thisUserId);

        KafkaMessage messageToThisUser = new KafkaMessage(KafkaMessage.Action.SETUP_CONNECTION, anotherUserMessage);
        KafkaMessage messageToAnotherUser = new KafkaMessage(KafkaMessage.Action.SETUP_CONNECTION, thisUserMessage);

        kafkaWriter.write(messageToThisUser.toBytes(), topic);
        kafkaWriter.write(messageToAnotherUser.toBytes(), topic);
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
