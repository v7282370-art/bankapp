package com.bankapp.user.service;

import com.bankapp.user.dto.UserRequest;
import com.bankapp.user.dto.UserResponse;
import com.bankapp.user.entity.User;
import com.bankapp.user.entity.UserStatus;
import com.bankapp.user.exception.BusinessException;
import com.bankapp.user.exception.DuplicateResourceException;
import com.bankapp.user.exception.ResourceNotFoundException;
import com.bankapp.user.kafka.UserEventProducer;
import com.bankapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserEventProducer eventProducer;

    private static final String CACHE_USER = "user";
    private static final String CACHE_USERS = "users";

    @Transactional
    @Caching(
            put = { @CachePut(value = CACHE_USER, key = "#result.id") },
            evict = { @CacheEvict(value = CACHE_USERS, allEntries = true) }
    )
    public UserResponse createUser(UserRequest request) {
        log.info("Создание пользователя: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        UserResponse response = toResponse(saved);

        // Отправляем событие в Kafka
        eventProducer.sendUserCreatedEvent(
                UserEventProducer.buildEvent("USER_CREATED", response));

        log.info("Пользователь создан с id: {}", saved.getId());
        return response;
    }

    @Cacheable(value = CACHE_USER, key = "#id")
    public UserResponse getUserById(UUID id) {
        log.info("Получение пользователя из БД по id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    @Cacheable(value = CACHE_USERS)
    public List<UserResponse> getAllUsers() {
        log.info("Получение всех пользователей из БД");
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Caching(
            put = { @CachePut(value = CACHE_USER, key = "#result.id") },
            evict = { @CacheEvict(value = CACHE_USERS, allEntries = true) }
    )
    public UserResponse blockUser(UUID id) {
        log.info("Блокировка пользователя: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException("USER_ALREADY_BLOCKED",
                    "Пользователь уже заблокирован");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException("USER_DELETED",
                    "Нельзя заблокировать удалённого пользователя");
        }

        user.setStatus(UserStatus.BLOCKED);
        UserResponse response = toResponse(userRepository.save(user));

        // Отправляем событие в Kafka
        eventProducer.sendUserBlockedEvent(
                UserEventProducer.buildEvent("USER_BLOCKED", response));

        return response;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_USER, key = "#id"),
            @CacheEvict(value = CACHE_USERS, allEntries = true)
    })
    public void deleteUser(UUID id) {
        log.info("Удаление пользователя: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException("USER_ALREADY_DELETED",
                    "Пользователь уже удалён");
        }

        user.setStatus(UserStatus.DELETED);
        UserResponse response = toResponse(userRepository.save(user));

        // Отправляем событие в Kafka
        eventProducer.sendUserDeletedEvent(
                UserEventProducer.buildEvent("USER_DELETED", response));
    }

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