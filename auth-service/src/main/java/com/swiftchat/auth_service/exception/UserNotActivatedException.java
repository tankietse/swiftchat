package com.swiftchat.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UserNotActivatedException extends RuntimeException {

    public UserNotActivatedException(String message) {
        super(message);
    }
}
