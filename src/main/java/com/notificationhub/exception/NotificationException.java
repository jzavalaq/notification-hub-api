package com.notificationhub.exception;

/**
 * Exception thrown when a notification operation fails.
 */
public class NotificationException extends RuntimeException {
    /**
     * Constructs a new NotificationException with the specified message.
     *
     * @param message the detail message
     */
    public NotificationException(String message) {
        super(message);
    }

    /**
     * Constructs a new NotificationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
