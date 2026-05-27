package com.bankapp.user.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(resource + " уже существует с " + field + ": " + value);
    }
}