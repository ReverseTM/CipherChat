//package ru.mai.khasanov.cipherchat.service;
//
//import org.springframework.stereotype.Service;
//import ru.mai.khasanov.cipherchat.model.Message;
//import ru.mai.khasanov.cipherchat.model.User;
//import ru.mai.khasanov.cipherchat.repository.MessageRepository;
//
//@Service
//public class MessageService {
//    private final MessageRepository messageRepository;
//
//    public MessageService(MessageRepository messageRepository) {
//        this.messageRepository = messageRepository;
//    }
//
//    public Message save(User producer, User consumer, byte[] messageBytes) {
//        Message message = Message.builder()
//                .producer(producer)
//                .consumer(consumer)
//                .message(messageBytes)
//                .build();
//
//        return messageRepository.save(message);
//    }
//}
