<div align="center">
  <h1>SwiftChat</h1>
  <p>Realtime Chat System built with Spring Boot</p>
  
  ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-brightgreen.svg)
  ![WebSocket](https://img.shields.io/badge/WebSocket-enabled-blue.svg)
  ![Redis](https://img.shields.io/badge/Redis-8.0-red.svg)
  ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)
</div>

## 📖 Project Introduction

SwiftChat is a realtime messaging application built with Spring Boot, ensuring multi-threading, high performance, and scalability. The system supports both one-on-one and group chats, allows sending various media types, and integrates realtime notifications.

## 🚀 Key Features

### Account Management
- Register, login with email/password, OAuth2 (Google, Facebook)
- Password recovery, password change
- Profile updates, avatar management

### Realtime Messaging
- Support for group chats and direct messaging
- Send text messages, images, videos, files
- Emoji support, reactions
- Read receipt status

### Group Chat Management
- Create, edit and delete groups
- Add, remove members
- Group admin permissions

### Notifications & Status
- Notifications for new messages
- Online/offline status display
- Typing indicators

## 🛠️ Technologies Used

### Backend
- **Framework:** Spring Boot 3.4.3
- **Realtime:** WebSockets with STOMP
- **Caching:** Redis 8.0
- **Database:** PostgreSQL 17
- **Authentication:** JWT, OAuth2

### Frontend
- **Web:** React.js 19
- **Mobile:** Flutter 4.0

### DevOps
- **Containerization:** Docker
- **Orchestration:** Kubernetes
- **Monitoring:** OpenTelemetry with Grafana

## 🏗️ System Architecture

The project uses a microservices architecture with these main components:
- Auth Service
- Chat Service
- Notification Service
- File Service
- User Service

Architecture details can be found in the design documentation (`/docs/architecture.md`).

## 🔧 Installation & Running

### System Requirements
- JDK 21+
- Maven 4.0+
- Docker & Docker Compose (recommended)
- PostgreSQL 17+
- Redis 8.0+

### Run with Docker

```bash
# Clone repository
git clone https://github.com/tankietse/swiftchat.git
cd swiftchat

# Start with Docker Compose
docker-compose up -d
```

### Run Directly

```bash
# Clone repository
git clone https://github.com/tankietse/swiftchat.git
cd swiftchat

# Build project
mvn clean install

# Run application
java -jar target/swiftchat-0.0.1-SNAPSHOT.jar
```

## ⚙️ Configuration

Edit configuration files in the `/src/main/resources` directory:

- `application.properties`: General configuration
- `application-dev.properties`: Development environment configuration
- `application-prod.properties`: Production environment configuration

### Database Connection

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/swiftchat
spring.datasource.username=postgres
spring.datasource.password=password

# Redis
spring.redis.host=localhost
spring.redis.port=6379
```

## 📚 API Documentation

API documentation is automatically generated with Swagger and can be accessed at:
```
http://localhost:8080/swagger-ui.html
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify
```

## 🔐 Security

- Message data encryption (AES-256)
- JWT for authentication
- Login attempt limiting to prevent brute-force attacks

## 📋 Performance & Scalability

- Supports connections for millions of users
- Redis caching for query optimization
- Database sharding to ensure performance

## 🤝 Contributing

We welcome all contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## 📄 License

This project is distributed under the MIT License. See [LICENSE](LICENSE) for more information.

## 👨‍💻 Development Team

- [Team Member 1](https://github.com/tankietse)
