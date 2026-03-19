package com.tlavu.linkforge.presentation.exception;

import com.tlavu.linkforge.domain.exception.InvalidDeleteTokenException;
import com.tlavu.linkforge.domain.exception.InvalidShortCodeException;
import com.tlavu.linkforge.domain.exception.InvalidShortLinkException;
import com.tlavu.linkforge.domain.exception.InvalidUrlException;
import com.tlavu.linkforge.domain.exception.DomainException;
import com.tlavu.linkforge.domain.exception.ShortLinkExpiredException;
import com.tlavu.linkforge.domain.exception.ShortLinkNotFoundException;
import com.tlavu.linkforge.domain.exception.AdTokenVerificationException;
import com.tlavu.linkforge.presentation.response.ApiResponse;
import com.tlavu.linkforge.shared.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(ShortLinkNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ShortLinkNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        String message = messageService.getMessage("shortlink.not_found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(ShortLinkExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpired(ShortLinkExpiredException ex) {
        String shortCode = ex.getMessage().contains(": ") ? ex.getMessage().split(": ")[1] : "";
        String message = messageService.getMessage("shortlink.expired", shortCode);
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler({
            InvalidUrlException.class,
            InvalidShortCodeException.class,
            InvalidShortLinkException.class,
            InvalidDeleteTokenException.class,
            AdTokenVerificationException.class,
            DomainException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        // Try to translate if message is a key, otherwise use the message as is
        String message;
        try {
            message = messageService.getMessage(ex.getMessage());
        } catch (Exception e) {
            message = ex.getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(messageService.getMessage("error.forbidden")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            // Try to translate validation message
            try {
                errorMessage = messageService.getMessage(errorMessage);
            } catch (Exception e) {
                // Ignore if not a key
            }
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(messageService.getMessage("error.bad_request"), errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(messageService.getMessage("error.internal_error")));
    }
}
