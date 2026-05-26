package com.bankapp.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data                    // Lombok: автоматически создаёт геттеры, сеттеры, toString
@Builder                 // Lombok: паттерн Builder для создания объектов
@NoArgsConstructor       // Lombok: конструктор без параметров
@AllArgsConstructor      // Lombok: конструктор со всеми параметрами
@Entity                  // JPA: это таблица в БД
@Table(name = "users")   // JPA: название таблицы
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist   // выполняется автоматически перед сохранением в БД
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate    // выполняется автоматически перед обновлением в БД
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}