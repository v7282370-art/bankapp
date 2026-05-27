package com.bankapp.user.controller;

import com.bankapp.user.BaseIntegrationTest;
import com.bankapp.user.dto.UserRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("UserController — API тесты")
class UserControllerTest extends BaseIntegrationTest {

    // ========================
    // POST /api/v1/users
    // ========================
    @Nested
    @DisplayName("POST /users — создание пользователя")
    class CreateUserTests {

        @Test
        @DisplayName("201 — успешное создание пользователя")
        void shouldCreateUser() {
            given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .when()
                    .post("/users")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("id", notNullValue())
                    .body("username", equalTo("ivan.petrov"))
                    .body("email", equalTo("ivan@test.com"))
                    .body("status", equalTo("ACTIVE"))
                    .body("createdAt", notNullValue());
        }

        @Test
        @DisplayName("409 — дубликат username")
        void shouldReturn409WhenUsernameExists() {
            // Создаём первый раз
            given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .when()
                    .post("/users");

            // Создаём второй раз с тем же username
            given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "other@test.com"))
                    .when()
                    .post("/users")
                    .then()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .body("code", equalTo("DUPLICATE_RESOURCE"))
                    .body("message", containsString("ivan.petrov"));
        }

        @Test
        @DisplayName("400 — невалидный email")
        void shouldReturn400WhenEmailInvalid() {
            UserRequest request = buildRequest("ivan", "not-an-email");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/users")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("VALIDATION_ERROR"))
                    .body("validationErrors.email", notNullValue());
        }

        @Test
        @DisplayName("400 — пустой username")
        void shouldReturn400WhenUsernameBlank() {
            UserRequest request = buildRequest("", "ivan@test.com");

            given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/users")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("VALIDATION_ERROR"))
                    .body("validationErrors.username", notNullValue());
        }
    }

    // ========================
    // GET /api/v1/users
    // ========================
    @Nested
    @DisplayName("GET /users — получение пользователей")
    class GetUsersTests {

        @Test
        @DisplayName("200 — получить всех пользователей")
        void shouldReturnAllUsers() {
            // Создаём двух пользователей
            given().contentType(ContentType.JSON)
                    .body(buildRequest("user1", "user1@test.com"))
                    .post("/users");

            given().contentType(ContentType.JSON)
                    .body(buildRequest("user2", "user2@test.com"))
                    .post("/users");

            // Получаем список
            given()
                    .when()
                    .get("/users")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("$", hasSize(2))
                    .body("username", hasItems("user1", "user2"));
        }

        @Test
        @DisplayName("200 — получить пользователя по ID")
        void shouldReturnUserById() {
            // Создаём пользователя и берём его ID
            String userId = given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .post("/users")
                    .then()
                    .extract().path("id");

            // Получаем по ID
            given()
                    .when()
                    .get("/users/" + userId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("id", equalTo(userId))
                    .body("username", equalTo("ivan.petrov"));
        }

        @Test
        @DisplayName("404 — пользователь не найден")
        void shouldReturn404WhenUserNotFound() {
            given()
                    .when()
                    .get("/users/00000000-0000-0000-0000-000000000000")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo("RESOURCE_NOT_FOUND"));
        }
    }

    // ========================
    // PATCH /api/v1/users/{id}/block
    // ========================
    @Nested
    @DisplayName("PATCH /users/{id}/block — блокировка")
    class BlockUserTests {

        @Test
        @DisplayName("200 — успешная блокировка")
        void shouldBlockUser() {
            String userId = given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .post("/users")
                    .then()
                    .extract().path("id");

            given()
                    .when()
                    .patch("/users/" + userId + "/block")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("status", equalTo("BLOCKED"));
        }

        @Test
        @DisplayName("400 — повторная блокировка")
        void shouldReturn400WhenAlreadyBlocked() {
            String userId = given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .post("/users")
                    .then()
                    .extract().path("id");

            // Блокируем первый раз
            given().patch("/users/" + userId + "/block");

            // Блокируем второй раз
            given()
                    .when()
                    .patch("/users/" + userId + "/block")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("USER_ALREADY_BLOCKED"));
        }
    }

    // ========================
    // DELETE /api/v1/users/{id}
    // ========================
    @Nested
    @DisplayName("DELETE /users/{id} — удаление")
    class DeleteUserTests {

        @Test
        @DisplayName("204 — успешное удаление")
        void shouldDeleteUser() {
            String userId = given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .post("/users")
                    .then()
                    .extract().path("id");

            given()
                    .when()
                    .delete("/users/" + userId)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        }

        @Test
        @DisplayName("400 — повторное удаление")
        void shouldReturn400WhenAlreadyDeleted() {
            String userId = given()
                    .contentType(ContentType.JSON)
                    .body(buildRequest("ivan.petrov", "ivan@test.com"))
                    .post("/users")
                    .then()
                    .extract().path("id");

            // Удаляем первый раз
            given().delete("/users/" + userId);

            // Удаляем второй раз
            given()
                    .when()
                    .delete("/users/" + userId)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("USER_ALREADY_DELETED"));
        }
    }

    // Вспомогательный метод для создания запроса
    private UserRequest buildRequest(String username, String email) {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("password123");
        request.setFirstName("Иван");
        request.setLastName("Петров");
        request.setPhone("+79001234567");
        return request;
    }
}