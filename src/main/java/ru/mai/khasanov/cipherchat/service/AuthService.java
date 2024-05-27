package ru.mai.khasanov.cipherchat.service;

import com.vaadin.flow.server.VaadinSession;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User authenticate(String username, String password) throws Exception {
        Optional<User> maybeUser = userRepository.findByUsername(username);

        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                VaadinSession.getCurrent().setAttribute(User.class, user);
                return user;
            }
        }

        throw new AuthException("Authenticate exception");
    }

    @Transactional
    public User register(String username, String password) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .build();

        userRepository.save(user);

        VaadinSession.getCurrent().setAttribute(User.class, user);

        return user;
    }
}
