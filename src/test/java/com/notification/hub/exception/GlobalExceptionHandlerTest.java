package com.notification.hub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Notification", "id", 999L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Notification"));
    }

    @Test
    void handleResourceNotFound_withMessage_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().getMessage());
    }

    @Test
    void handleDuplicateResource_returns409() {
        DuplicateResourceException ex = new DuplicateResourceException("Template", "code", "welcome-email");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDuplicateResource(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Template"));
    }

    @Test
    void handleDuplicateResource_withMessage_returns409() {
        DuplicateResourceException ex = new DuplicateResourceException("Duplicate resource");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDuplicateResource(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Duplicate resource", response.getBody().getMessage());
    }

    @Test
    void handleNotificationException_returns400() {
        NotificationException ex = new NotificationException("User has opted out");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNotificationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("User has opted out", response.getBody().getMessage());
    }

    @Test
    void handleBadRequestException_returns400() {
        BadRequestException ex = new BadRequestException("Invalid request");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadRequestException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid request", response.getBody().getMessage());
    }

    @Test
    void handleBadRequestException_withCause_returns400() {
        BadRequestException ex = new BadRequestException("Invalid request", new RuntimeException("Cause"));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBadRequestException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "must be valid");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation Failed", response.getBody().getError());
        assertNotNull(response.getBody().getErrors());
        assertTrue(response.getBody().getErrors().containsKey("email"));
        assertEquals("must be valid", response.getBody().getErrors().get("email"));
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void errorResponse_gettersAndSetters_work() {
        GlobalExceptionHandler.ErrorResponse errorResponse = GlobalExceptionHandler.ErrorResponse.builder()
                .timestamp(java.time.Instant.now())
                .status(400)
                .error("Bad Request")
                .message("Test message")
                .errors(Map.of("field", "error"))
                .build();

        assertNotNull(errorResponse.getTimestamp());
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Test message", errorResponse.getMessage());
        assertNotNull(errorResponse.getErrors());
    }
}
