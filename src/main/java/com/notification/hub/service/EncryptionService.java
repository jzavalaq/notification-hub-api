package com.notification.hub.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting and decrypting sensitive data.
 * <p>
 * Uses Jasypt with PBE (Password-Based Encryption) for secure
 * encryption of sensitive fields like webhook secrets.
 * </p>
 */
@Service
@Slf4j
public class EncryptionService {

    @Value("${encryption.secret:${ENCRYPTION_SECRET:change-me-in-production-use-256-bit-key}}")
    private String encryptionSecret;

    private StandardPBEStringEncryptor encryptor;

    @PostConstruct
    public void init() {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptionSecret);
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        log.info("Encryption service initialized");
    }

    /**
     * Encrypts a plaintext string.
     *
     * @param plainText the text to encrypt
     * @return encrypted text
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return encryptor.encrypt(plainText);
    }

    /**
     * Decrypts an encrypted string.
     *
     * @param encryptedText the text to decrypt
     * @return decrypted text
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            return encryptor.decrypt(encryptedText);
        } catch (Exception e) {
            log.warn("Failed to decrypt value, returning as-is: {}", e.getMessage());
            return encryptedText;
        }
    }
}
