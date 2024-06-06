//package ru.mai.khasanov.cipherchat.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Getter
//@Setter
//@Entity
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@Table(name = "messages")
//public class Message {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "producer", nullable = false)
//    private User producer;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "consumer", nullable = false)
//    private User consumer;
//
//    @Lob
//    private byte[] message;
//}
