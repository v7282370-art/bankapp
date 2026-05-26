package com.bankapp.user.repository;

import com.bankapp.user.entity.User;
import com.bankapp.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Spring сам напишет SQL по названию метода — найти по username
    Optional<User> findByUsername(String username);

    // найти по email
    Optional<User> findByEmail(String email);

    // найти всех пользователей с определённым статусом
    List<User> findAllByStatus(UserStatus status);

    // проверить существует ли пользователь с таким email
    boolean existsByEmail(String email);

    // проверить существует ли пользователь с таким username
    boolean existsByUsername(String username);

    // пример кастомного SQL запроса
    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:name% OR u.lastName LIKE %:name%")
    List<User> searchByName(String name);
}