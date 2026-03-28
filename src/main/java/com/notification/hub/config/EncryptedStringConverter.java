package com.notification.hub.config;

import com.notification.hub.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for encrypting sensitive string fields in the database.
 *
 * <p>Used to encrypt webhook secrets and other sensitive data at rest.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;Column(nullable = false)
 * &#64;Convert(converter = EncryptedStringConverter.class)
 * private String secret;
 * </pre>
 */
@Converter
@Component
@Slf4j
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        EncryptedStringConverter.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        if (encryptionService == null) {
            log.warn("EncryptionService not available, storing value unencrypted");
            return attribute;
        }

        try {
            return encryptionService.encrypt(attribute);
        } catch (Exception e) {
            log.error("Failed to encrypt attribute, storing unencrypted: {}", e.getMessage());
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        if (encryptionService == null) {
            log.warn("EncryptionService not available, returning value as-is");
            return dbData;
        }

        try {
            return encryptionService.decrypt(dbData);
        } catch (Exception e) {
            log.error("Failed to decrypt attribute, returning as-is: {}", e.getMessage());
            return dbData;
        }
    }
}
