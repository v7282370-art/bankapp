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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Unit тесты")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventProducer eventProducer;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("ivan.petrov")
                .email("ivan@test.com")
                .passwordHash("hash123")
                .firstName("Иван")
                .lastName("Петров")
                .phone("+79001234567")
                .status(UserStatus.ACTIVE)
                .build();

        testRequest = new UserRequest();
        testRequest.setUsername("ivan.petrov");
        testRequest.setEmail("ivan@test.com");
        testRequest.setPassword("password123");
        testRequest.setFirstName("Иван");
        testRequest.setLastName("Петров");
        testRequest.setPhone("+79001234567");
    }

    // ========================
    // Тесты создания пользователя
    // ========================
    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("Успешное создание пользователя")
        void shouldCreateUserSuccessfully() {
            // ARRANGE — подготавливаем моки
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.save(any())).thenReturn(testUser);

            // ACT — вызываем метод
            UserResponse result = userService.createUser(testRequest);

            // ASSERT — проверяем результат
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("ivan.petrov");
            assertThat(result.getEmail()).isEqualTo("ivan@test.com");
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // Проверяем что событие отправлено в Kafka
            verify(eventProducer, times(1)).sendUserCreatedEvent(any());
        }

        @Test
        @DisplayName("Ошибка — username уже занят")
        void shouldThrowExceptionWhenUsernameExists() {
            // ARRANGE
            when(userRepository.existsByUsername("ivan.petrov")).thenReturn(true);

            // ACT & ASSERT
            assertThatThrownBy(() -> userService.createUser(testRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("ivan.petrov");

            // Проверяем что в БД ничего не сохранилось
            verify(userRepository, never()).save(any());
            verify(eventProducer, never()).sendUserCreatedEvent(any());
        }

        @Test
        @DisplayName("Ошибка — email уже занят")
        void shouldThrowExceptionWhenEmailExists() {
            // ARRANGE
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(userRepository.existsByEmail("ivan@test.com")).thenReturn(true);

            // ACT & ASSERT
            assertThatThrownBy(() -> userService.createUser(testRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("ivan@test.com");

            verify(userRepository, never()).save(any());
        }
    }

    // ========================
    // Тесты получения пользователя
    // ========================
    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("Успешное получение пользователя")
        void shouldReturnUserById() {
            // ARRANGE
            UUID id = testUser.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(testUser));

            // ACT
            UserResponse result = userService.getUserById(id);

            // ASSERT
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getUsername()).isEqualTo("ivan.petrov");
        }

        @Test
        @DisplayName("Ошибка — пользователь не найден")
        void shouldThrowExceptionWhenUserNotFound() {
            // ARRANGE
            UUID randomId = UUID.randomUUID();
            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            // ACT & ASSERT
            assertThatThrownBy(() -> userService.getUserById(randomId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(randomId.toString());
        }
    }

    // ========================
    // Тесты блокировки
    // ========================
    @Nested
    @DisplayName("blockUser()")
    class BlockUserTests {

        @Test
        @DisplayName("Успешная блокировка пользователя")
        void shouldBlockUserSuccessfully() {
            // ARRANGE
            UUID id = testUser.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);

            // ACT
            userService.blockUser(id);

            // ASSERT
            verify(userRepository, times(1)).save(any());
            verify(eventProducer, times(1)).sendUserBlockedEvent(any());
        }

        @Test
        @DisplayName("Ошибка — пользователь уже заблокирован")
        void shouldThrowExceptionWhenAlreadyBlocked() {
            // ARRANGE
            testUser.setStatus(UserStatus.BLOCKED);
            when(userRepository.findById(testUser.getId()))
                    .thenReturn(Optional.of(testUser));

            // ACT & ASSERT
            assertThatThrownBy(() -> userService.blockUser(testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("уже заблокирован");
        }

        @Test
        @DisplayName("Ошибка — нельзя заблокировать удалённого пользователя")
        void shouldThrowExceptionWhenUserDeleted() {
            // ARRANGE
            testUser.setStatus(UserStatus.DELETED);
            when(userRepository.findById(testUser.getId()))
                    .thenReturn(Optional.of(testUser));

            // ACT & ASSERT
            assertThatThrownBy(() -> userService.blockUser(testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("удалённого");
        }
    }

    // ========================
    // Тесты удаления
    // ========================
    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("Успешное удаление пользователя")
        void shouldDeleteUserSuccessfully() {
            // ARRANGE
            UUID id = testUser.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);

            // ACT
            userService.deleteUser(id);

            // ASSERT
            verify(userRepository, times(1)).save(any());
            verify(eventProducer, times(1)).sendUserDeletedEvent(any());
        }

        @Test
        @DisplayName("Ошибка — пользователь уже удалён")
        void shouldThrowExceptionWhenAlreadyDeleted() {
            // ARRANGE
            testUser.setStatus(UserStatus.DELETED);
            when(userRepository.findById(testUser.getId()))
                    .thenReturn(Optional.of(testUser));

            // ACT & ASSERT
            assertThatThrownBy(() -> userService.deleteUser(testUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("уже удалён");
        }
    }

    @Test
    @DisplayName("getAllUsers() — возвращает список пользователей")
    void shouldReturnAllUsers() {
        // ARRANGE
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // ACT
        List<UserResponse> result = userService.getAllUsers();

        // ASSERT
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("ivan.petrov");
    }
}