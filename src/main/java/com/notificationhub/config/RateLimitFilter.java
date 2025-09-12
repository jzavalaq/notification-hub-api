package com.notificationhub.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.BlockingStrategy;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using Bucket4j token bucket algorithm.
 * <p>
 * Limits requests per client IP to prevent abuse and DoS attacks.
 * Configurable capacity and refill rate via application properties.
 * </p>
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate.limit.capacity:100}")
    private int capacity;

    @Value("${rate.limit.refill.tokens:100}")
    private int refillTokens;

    @Value("${rate.limit.refill.minutes:1}")
    private int refillMinutes;

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health checks and actuator endpoints
        String path = request.getRequestURI();
        if (path.equals("/actuator/health") || path.equals("/api/v1/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = getClientIdentifier(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {}", key);
            response.setStatus(429);
            response.setHeader("X-Rate-Limit-Remaining", "0");
            response.setHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    /**
     * Creates a token bucket with configured capacity and refill rate.
     */
    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
            capacity,
            Refill.greedy(refillTokens, Duration.ofMinutes(refillMinutes))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Extracts client identifier from request.
     * Uses X-Forwarded-For header if present (behind proxy), otherwise remote address.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Periodically cleanup expired buckets to prevent memory leaks.
     * This could be enhanced with a scheduled task in production.
     */
    public void cleanupBuckets() {
        buckets.clear();
        log.info("Rate limit buckets cleared");
    }
}
