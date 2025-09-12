package com.notificationhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the Notification Hub API.
 * <p>
 * This is a multi-channel notification and communication hub supporting
 * email, SMS, push notifications, and in-app messaging.
 * </p>
 *
 * @author Notification Hub Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
@EnableAsync
@EnableCaching
@EnableScheduling
public class Application {
    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
