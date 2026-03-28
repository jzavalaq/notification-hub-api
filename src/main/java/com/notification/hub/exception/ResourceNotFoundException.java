package com.notification.hub.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Constructs a new ResourceNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException with resource details.
     *
     * @param resource the type of resource
     * @param field the field used to search
     * @param value the value that was not found
     */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }
}
