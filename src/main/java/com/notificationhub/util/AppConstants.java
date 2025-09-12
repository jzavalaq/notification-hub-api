package com.notificationhub.util;

/**
 * Application-wide constants.
 * Centralizes magic numbers and strings to improve maintainability.
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - prevent instantiation
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // Notification defaults
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final int MAX_BATCH_PARALLEL_THREADS = 10;

    // Webhook defaults
    public static final int DEFAULT_WEBHOOK_MAX_RETRIES = 5;
    public static final int DEFAULT_WEBHOOK_TIMEOUT_SECONDS = 30;
    public static final int WEBHOOK_MAX_IN_MEMORY_SIZE = 1024 * 1024; // 1MB

    // Retry backoff configuration (in minutes)
    public static final long[] WEBHOOK_RETRY_BACKOFF_MINUTES = {1, 5, 15, 60, 360};

    // Date range defaults
    public static final int DEFAULT_ANALYTICS_DAYS = 30;
}
