# Notification Hub API

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](Dockerfile)
[![Redis](https://img.shields.io/badge/Redis-7+-red?logo=redis)](https://redis.io/)

A multi-channel notification and communication hub supporting email, SMS, push notifications, and in-app messaging with Redis caching and webhook integrations.

## Features

- **Multi-Channel Delivery**: Email, SMS, Push, In-App notifications
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
| PostgreSQL | 15+ |
| Redis | 7+ |
| H2 (dev) | 2.x |

## Quick Start

```bash
# Clone and run
git clone https://github.com/jzavalaq/notification-hub-api.git
cd notification-hub-api

# Development mode
mvn spring-boot:run

# Docker Compose
docker-compose up -d
```

## API Examples

### Authentication

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","email":"user@example.com","password":"Secure123!"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"Secure123!"}'
```

### Templates

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

# Get all templates
curl -X GET http://localhost:8080/api/v1/templates \
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

# Send direct notification
curl -X POST http://localhost:8080/api/v1/notifications/send-direct \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SMS",
    "recipient": "+1234567890",
    "message": "Your verification code is 123456"
  }'

# Batch notifications
curl -X POST http://localhost:8080/api/v1/notifications/batch \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "notifications": [
      {"channel": "EMAIL", "recipient": "user1@example.com", "message": "Hello 1"},
      {"channel": "EMAIL", "recipient": "user2@example.com", "message": "Hello 2"}
    ]
  }'
```

### User Preferences

```bash
# Set notification preferences
curl -X PUT http://localhost:8080/api/v1/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "emailEnabled": true,
    "smsEnabled": false,
    "pushEnabled": true,
    "quietHoursStart": "22:00",
    "quietHoursEnd": "08:00"
  }'

# Get preferences
curl -X GET http://localhost:8080/api/v1/preferences \
  -H "Authorization: Bearer $TOKEN"
```

### Webhooks

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
