package com.swiftchat.auth_service.exception;

import com.swiftchat.auth_service.dto.MessageResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Provides consistent error responses across all controllers.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<MessageResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(new MessageResponseDto(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<MessageResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("User already exists: {}", ex.getMessage());
        return new ResponseEntity<>(new MessageResponseDto(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({ InvalidCredentialsException.class, BadCredentialsException.class,
            AuthenticationException.class })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<MessageResponseDto> handleAuthenticationException(Exception ex) {
        log.error("Authentication error: {}", ex.getMessage());
        return new ResponseEntity<>(new MessageResponseDto("Authentication failed: " + ex.getMessage()),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<MessageResponseDto> handleInvalidTokenException(InvalidTokenException ex) {
        log.error("Invalid token: {}", ex.getMessage());
        return new ResponseEntity<>(new MessageResponseDto(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotActivatedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<MessageResponseDto> handleUserNotActivatedException(UserNotActivatedException ex) {
        log.error("Account not activated: {}", ex.getMessage());
        return new ResponseEntity<>(new MessageResponseDto(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<MessageResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(
                new MessageResponseDto("Access denied: You don't have permission to access this resource"),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.error("Validation error in request");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", HttpStatus.BAD_REQUEST.value());
        responseBody.put("error", "Validation Error");

        // Collect all field errors
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existingMessage, newMessage) -> existingMessage + "; " + newMessage));

        responseBody.put("errors", errors);

        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<MessageResponseDto> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return new ResponseEntity<>(
                new MessageResponseDto("An unexpected error occurred. Please try again later."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
