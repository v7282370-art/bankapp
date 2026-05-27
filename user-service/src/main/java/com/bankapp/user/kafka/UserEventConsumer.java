package com.bankapp.user.kafka;

import com.bankapp.user.config.KafkaConfig;
import com.bankapp.user.event.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserEventConsumer {

    @KafkaListener(
            topics = KafkaConfig.TOPIC_USER_EVENTS,
            groupId = "user-service-group"
    )
    public void handleUserEvent(
            @Payload UserEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Получено событие [{}] из partition={}, offset={}: userId={}",
                event.getEventType(), partition, offset, event.getUserId());

        switch (event.getEventType()) {
            case "USER_CREATED" -> handleUserCreated(event);
            case "USER_BLOCKED" -> handleUserBlocked(event);
            case "USER_DELETED" -> handleUserDeleted(event);
            default -> log.warn("Неизвестный тип события: {}", event.getEventType());
        }
    }

    private void handleUserCreated(UserEvent event) {
        // В реальном проекте здесь: отправить приветственное письмо,
        // создать счёт в account-service и т.д.
        log.info("Обработка USER_CREATED: пользователь {} зарегистрирован",
                event.getUsername());
    }

    private void handleUserBlocked(UserEvent event) {
        // В реальном проекте: заблокировать все карты, уведомить пользователя
        log.info("Обработка USER_BLOCKED: пользователь {} заблокирован",
                event.getUsername());
    }

    private void handleUserDeleted(UserEvent event) {
        // В реальном проекте: закрыть счета, отозвать токены
        log.info("Обработка USER_DELETED: пользователь {} удалён",
                event.getUsername());
    }
}