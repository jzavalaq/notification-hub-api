package com.notificationhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications.
 * Enables STOMP protocol over WebSocket with SockJS fallback.
 *
 * <p>Features:
 * <ul>
 *   <li>STOMP message broker for pub/sub messaging</li>
 *   <li>User-specific destinations for targeted notifications</li>
 *   <li>SockJS fallback for browsers without WebSocket support</li>
 *   <li>JWT authentication integration</li>
 * </ul>
 *
 * <p>Client connection example:
 * <pre>
 * const socket = new SockJS('/ws/notifications');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({ 'Authorization': 'Bearer ' + token }, function(frame) {
 *     stompClient.subscribe('/user/queue/notifications', function(message) {
 *         console.log(JSON.parse(message.body));
 *     });
 * });
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    public WebSocketConfig(WebSocketAuthChannelInterceptor authChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
    }

    /**
     * Configure the message broker.
     *
     * <p>Destination prefixes:
     * <ul>
     *   <li>/topic - broadcast messages to all subscribers</li>
     *   <li>/queue - user-specific messages (point-to-point)</li>
     *   <li>/app - application destinations (for @MessageMapping)</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple memory-based message broker
        // In production, consider using external broker (RabbitMQ, ActiveMQ)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages bound for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        // Allows sending to /user/{username}/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     *
     * <p>Endpoints:
     * <ul>
     *   <li>/ws/notifications - Main WebSocket endpoint with SockJS fallback</li>
     * </ul>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("${app.cors.allowed-origins:http://localhost:3000}".split(","))
                .withSockJS();
    }

    /**
     * Configure client inbound channel for authentication.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
