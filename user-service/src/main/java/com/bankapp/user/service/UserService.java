package com.bankapp.user.service;

import com.bankapp.user.dto.UserRequest;
import com.bankapp.user.dto.UserResponse;
import com.bankapp.user.entity.User;
import com.bankapp.user.entity.UserStatus;
import com.bankapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j                    // Lombok: добавляет логгер log.info(), log.error() и т.д.
@Service                  // Spring: это сервисный слой
@RequiredArgsConstructor  // Lombok: конструктор для всех final полей (инъекция зависимостей)
public class UserService {

    private final UserRepository userRepository;

    // Создать пользователя
    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Создание пользователя: {}", request.getUsername());

        // Проверяем что username и email свободны
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username уже занят: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email уже занят: " + request.getEmail());
        }

        // Создаём сущность
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // в реальном проекте здесь BCrypt
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("Пользователь создан с id: {}", saved.getId());

        return toResponse(saved);
    }

    // Получить пользователя по ID
    public UserResponse getUserById(UUID id) {
        log.info("Получение пользователя по id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        return toResponse(user);
    }

    // Получить всех пользователей
    public List<UserResponse> getAllUsers() {
        log.info("Получение всех пользователей");
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Заблокировать пользователя
    @Transactional
    public UserResponse blockUser(UUID id) {
        log.info("Блокировка пользователя: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        user.setStatus(UserStatus.BLOCKED);
        return toResponse(userRepository.save(user));
    }

    // Удалить пользователя (мягкое удаление — меняем статус)
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Удаление пользователя: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    // Конвертация Entity → DTO (приватный вспомогательный метод)
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}