package ru.mai.khasanov.cipherchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

@SpringBootApplication
public class CipherChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CipherChatApplication.class, args);
    }

}
