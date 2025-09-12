# Notification Hub API

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-purple)](https://stomp.github.io/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](Dockerfile)
[![Redis](https://img.shields.io/badge/Redis-7+-red?logo=redis)](https://redis.io/)

A multi-channel notification and communication hub supporting email, SMS, push notifications, in-app messaging, and **real-time WebSocket notifications** with Redis caching and webhook integrations.

## Features

- **Multi-Channel Delivery**: Email, SMS, Push, In-App notifications
- **Real-Time WebSocket**: STOMP over WebSocket with SockJS fallback
- **Template Management**: Dynamic templates with variable interpolation
- **User Preferences**: Quiet hours, channel preferences, opt-in/out
- **Webhook Integrations**: Event-driven notifications with retry logic
- **Redis Caching**: High-performance template and preference caching
- **Audit Trail**: Complete notification lifecycle tracking

## Tech Stack

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.2.5 |
| Spring WebSocket | STOMP + SockJS |
| PostgreSQL | 15+ |
| Redis | 7+ |
| H2 (dev) | 2.x |

## Quick Start

### Development Mode

```bash
# Clone and run
git clone https://github.com/jzavalaq/notification-hub-api.git
cd notification-hub-api

# Run with Maven (uses H2 in-memory database)
mvn spring-boot:run

# App available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
# H2 Console at http://localhost:8080/h2-console (when H2_CONSOLE_ENABLED=true)
```

### Quick Start with Docker Compose (Recommended for Production-like Testing)

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your values (especially JWT_SECRET and DB_PASSWORD)
nano .env

# Start all services (app + PostgreSQL + Redis)
docker-compose up -d

# Check logs
docker-compose logs -f app

# App available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
# Health check at http://localhost:8080/actuator/health
```

## Docker Run

```bash
# Build the Docker image
docker build -t notification-hub-api .

# Run with environment variables
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET=your-secure-jwt-secret-min-256-bits \
  -e DB_URL=jdbc:postgresql://postgres:5432/notificationhub \
  -e DB_USERNAME=notificationhub \
  -e DB_PASSWORD=your-password \
  -e ALLOWED_ORIGINS=http://localhost:3000 \
  notification-hub-api

# Run with docker-compose (recommended)
docker-compose up -d
```

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 15+ (production) or H2 (development)
- Redis 7+ (optional, for caching)
- Docker and Docker Compose (for containerized deployment)

## Build Instructions

```bash
# Compile the project
mvn compile

# Run tests
mvn test

# Package the application
mvn package -DskipTests

# Run the JAR
java -jar target/notification-hub-api-1.0.0-SNAPSHOT.jar
```

## API Examples

### Templates

Note: This API uses JWT authentication. Configure your JWT_SECRET in the environment variables or .env file before making authenticated requests.

```bash
TOKEN="your-jwt-token"

# Create a notification template
curl -X POST http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "welcome_email",
    "channel": "EMAIL",
    "subject": "Welcome, {{userName}}!",
    "body": "Hello {{userName}}, welcome to our platform!",
    "variables": ["userName"]
  }'

# List templates with pagination
curl -X GET "http://localhost:8080/api/v1/templates?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Send Notifications

```bash
# Send notification using template
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "templateName": "welcome_email",
    "recipient": "newuser@example.com",
    "variables": {"userName": "John"},
    "channels": ["EMAIL"]
  }'

# Get user notifications with pagination
curl -X GET "http://localhost:8080/api/v1/notifications/user/user123?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

---

## WebSocket Real-Time Notifications

### Connection

Connect to the WebSocket endpoint with JWT authentication:

```javascript
// Using SockJS and STOMP.js
const socket = new SockJS('http://localhost:8080/ws/notifications');
const stompClient = Stomp.over(socket);

const token = 'your-jwt-token';

stompClient.connect(
  { 'Authorization': 'Bearer ' + token },
  function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to personal notifications
    stompClient.subscribe('/user/queue/notifications', function(message) {
      const notification = JSON.parse(message.body);
      console.log('Received notification:', notification);
    });

    // Subscribe to broadcast notifications
    stompClient.subscribe('/topic/broadcast', function(message) {
      console.log('Broadcast:', JSON.parse(message.body));
    });

    // Subscribe to user presence
    stompClient.subscribe('/topic/presence', function(message) {
      console.log('Presence update:', JSON.parse(message.body));
    });
  },
  function(error) {
    console.error('Connection error:', error);
  }
);
```

### Send Real-Time Notification

```javascript
// Send notification to specific user
stompClient.send('/app/notify.send/john_doe', {}, JSON.stringify({
  type: 'ALERT',
  title: 'New Order',
  message: 'You have received a new order #12345',
  data: { orderId: '12345', amount: 99.99 }
}));

// Broadcast to all users
stompClient.send('/app/notify.broadcast', {}, JSON.stringify({
  type: 'SYSTEM',
  title: 'Maintenance Notice',
  message: 'System maintenance in 30 minutes'
}));
```

### Typing Indicator

```javascript
// Send typing indicator
stompClient.send('/app/notify.typing', {}, JSON.stringify({
  toUser: 'john_doe',
  isTyping: true,
  conversationId: 'conv-123'
}));

// Subscribe to typing indicators
stompClient.subscribe('/user/queue/typing', function(message) {
  const data = JSON.parse(message.body);
  console.log(data.fromUser + ' is typing: ' + data.isTyping);
});
```

### User Presence

```javascript
// Update your presence status
stompClient.send('/app/presence', {}, JSON.stringify({
  status: 'ONLINE',
  message: 'Available for chat'
}));

// Presence update format:
// { user: 'john_doe', status: 'ONLINE', message: 'Available', timestamp: '2024-...' }
```

### Notification Types

| Type | Description | Use Case |
|------|-------------|----------|
| `ALERT` | High priority alerts | New orders, security alerts |
| `INFO` | Informational | System updates, tips |
| `SUCCESS` | Success confirmations | Payment received, task completed |
| `WARNING` | Warnings | Low balance, expiring soon |
| `SYSTEM` | System broadcasts | Maintenance, downtime |
| `CHAT` | Chat messages | New message, typing indicator |

### Notification Payload Format

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "type": "ALERT",
  "title": "New Order Received",
  "message": "Order #12345 has been placed",
  "data": {
    "orderId": "12345",
    "amount": 99.99,
    "currency": "USD"
  },
  "fromUser": "system",
  "timestamp": "2024-03-23T10:30:00Z",
  "read": false
}
```

---

## Webhooks

```bash
# Register a webhook endpoint
curl -X POST http://localhost:8080/api/v1/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Order Updates",
    "url": "https://example.com/webhooks/notifications",
    "events": ["notification.delivered", "notification.failed"],
    "secret": "webhook-secret-key"
  }'

# List all webhooks with pagination
curl -X GET "http://localhost:8080/api/v1/webhooks?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Get webhooks for a specific user with pagination
curl -X GET "http://localhost:8080/api/v1/webhooks/user/user123?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_URL` | PostgreSQL URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `REDIS_URL` | Redis connection URL |
| `JWT_SECRET` | JWT signing key |

## License

MIT License - see [LICENSE](LICENSE)
