package ru.mai.khasanov.cipherchat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mai.khasanov.cipherchat.cryptography.DiffieHellmanAlgorithm;
import ru.mai.khasanov.cipherchat.kafka.KafkaWriter;
//import ru.mai.khasanov.cipherchat.model.Message;
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
    //private final MessageService messageService;

    private final KafkaWriter kafkaWriter;

    @Autowired
    public ServerService(UserService userService, RoomService roomService, KafkaWriter kafkaWriter) {
        this.userService = userService;
        this.roomService = roomService;
        //this.messageService = messageService;
        this.kafkaWriter = kafkaWriter;
    }

    @Transactional
    public synchronized boolean createRoom(
            long userId,
            String name,
            String algorithm,
            String mode,
            String padding) {

        if (!roomService.existsByName(name)) {
            BigInteger[] params = DiffieHellmanAlgorithm.generateParams(32);
            byte[] g = params[0].toByteArray();
            byte[] p = params[1].toByteArray();

            roomService.createRoom(userId, name, algorithm, mode, padding, g, p);

            return true;
        }

        return false;
    }

    @Transactional
    public synchronized boolean deleteRoom(long roomId) {
        Optional<Room> maybeRoom = roomService.getRoomById(roomId);

        if (maybeRoom.isPresent()) {
            Room room = maybeRoom.get();
            boolean status;
            Set<User> users = room.getUsers();
            for (User user : users) {
                status = disconnectRoom(user.getId(), room.getId());
                if (!status) {
                    return false;
                }
            }

            roomService.deleteRoom(roomId);

            return true;
        }

        return false;
    }

    @Transactional
    public synchronized boolean connectRoom(long userId, long roomId) {
        Optional<Room> maybeRoom = roomService.getRoomById(roomId);
        Optional<User> maybeUser = userService.getUserById(userId);

        if (maybeRoom.isPresent() && maybeUser.isPresent()) {
            Room room = maybeRoom.get();
            User user = maybeUser.get();

            if (!(room.getUsers().contains(user))) {
                if (!(room.getUsers().size() == 2)) {
                    long consumerId = 0;
                    boolean setupConnection = false;
                    if (room.getUsers().size() == 1) {
                        consumerId = room.getUsers().iterator().next().getId();
                        setupConnection = true;
                    }

                    roomService.addUserToRoom(user, room);
                    userService.addRoomToUser(user, room);

                    if (setupConnection) {
                        exchangeInformation(userId, consumerId, roomId);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Transactional
    public synchronized boolean disconnectRoom(long userId, long roomId) {
        Optional<Room> maybeRoom = roomService.getRoomById(roomId);
        Optional<User> maybeUser = userService.getUserById(userId);

        if (maybeRoom.isPresent() && maybeUser.isPresent()) {
            Room room = maybeRoom.get();
            User user = maybeUser.get();

            boolean roomStatus = roomService.removeUserFromRoom(user, room);
            boolean userStatus = userService.removeRoomFromUser(user, room);

            return roomStatus && userStatus;
        }

        return false;
    }

//    public synchronized boolean saveMessage(long producerId, long consumerId, byte[] messageBytes) {
//        Optional<User> maybeProducer = userService.getUserById(producerId);
//        Optional<User> maybeConsumer = userService.getUserById(consumerId);
//
//        if (maybeProducer.isPresent() && maybeConsumer.isPresent()) {
//            User producer = maybeProducer.get();
//            User consumer = maybeConsumer.get();
//
//            Message message = messageService.save(producer, consumer, messageBytes);
//            userService.addSentMessage(producer, message);
//            userService.addReceivedMessage(consumer, message);
//
//            return true;
//        }
//
//        return false;
//    }

    private void exchangeInformation(long producerId, long consumerId, long roomId) {
        String producerTopic = String.format("input_room_%s_user_%s", roomId, producerId);
        String consumerTopic = String.format("input_room_%s_user_%s", roomId, consumerId);

        KafkaMessage messageToThisUser = new KafkaMessage(KafkaMessage.Action.SETUP_CONNECTION, consumerId);
        KafkaMessage messageToAnotherUser = new KafkaMessage(KafkaMessage.Action.SETUP_CONNECTION, producerId);

        kafkaWriter.write(messageToThisUser.toBytes(), producerTopic);
        kafkaWriter.write(messageToAnotherUser.toBytes(), consumerTopic);
    }
}
