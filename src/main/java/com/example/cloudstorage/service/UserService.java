package com.example.cloudstorage.service;

import com.example.cloudstorage.entity.UserEntity;
import com.example.cloudstorage.config.SecurityConfig;
import com.example.cloudstorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
        return userRepository.findByLogin(login)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }
}
