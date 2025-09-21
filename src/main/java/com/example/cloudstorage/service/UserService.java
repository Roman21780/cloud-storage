package com.example.cloudstorage.service;

import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UserEntity> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public boolean validateUser(String login, String password) {
        Optional<UserEntity> user = userRepository.findByLogin(login);

        if (user.isEmpty()) {
            return false;
        }

        // Проверяем пароль
        UserEntity userEntity = user.get();
        return passwordEncoder.matches(password, userEntity.getPassword());
    }

    public UserEntity registerUser(String login, String password) {
        if (userRepository.findByLogin(login).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        UserEntity user = new UserEntity();
        user.setLogin(login);
        user.setEmail(login); // Используем login как email
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}