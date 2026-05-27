package com.bankapp.user.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " не найден с id: " + id);
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(resource + " не найден: " + field + " = " + value);
    }
}