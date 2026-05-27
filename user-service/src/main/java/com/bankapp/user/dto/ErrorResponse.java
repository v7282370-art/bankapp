package com.bankapp.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // не показывать null поля в JSON
public class ErrorResponse {

    private int status;
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, String> validationErrors; // для ошибок валидации
}