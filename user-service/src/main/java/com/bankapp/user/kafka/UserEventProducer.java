package com.bankapp.user.kafka;

import com.bankapp.user.config.KafkaConfig;
import com.bankapp.user.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void sendUserCreatedEvent(UserEvent event) {
        sendEvent(KafkaConfig.TOPIC_USER_EVENTS, event);
    }

    public void sendUserBlockedEvent(UserEvent event) {
        sendEvent(KafkaConfig.TOPIC_USER_EVENTS, event);
    }

    public void sendUserDeletedEvent(UserEvent event) {
        sendEvent(KafkaConfig.TOPIC_USER_EVENTS, event);
    }

    private void sendEvent(String topic, UserEvent event) {
        // ключ = userId чтобы события одного пользователя шли в одну партицию
        String key = event.getUserId().toString();

        CompletableFuture<SendResult<String, UserEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Событие [{}] отправлено в топик '{}', partition={}, offset={}",
                        event.getEventType(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Ошибка отправки события [{}]: {}",
                        event.getEventType(), ex.getMessage());
            }
        });
    }

    // Вспомогательный метод для создания события
    public static UserEvent buildEvent(String eventType, com.bankapp.user.dto.UserResponse user) {
        return UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus().name())
                .occurredAt(LocalDateTime.now())
                .build();
    }
}