# SwiftChat System Architecture

## Table of Contents
- [System Overview](#system-overview)
- [Architecture Principles](#architecture-principles)
- [High-Level Architecture](#high-level-architecture)
- [Microservices Architecture](#microservices-architecture)
  - [Auth Service](#auth-service)
  - [User Service](#user-service)
  - [Chat Service](#chat-service)
  - [Notification Service](#notification-service)
  - [File Service](#file-service)
- [Data Architecture](#data-architecture)
- [Communication Patterns](#communication-patterns)
- [Caching Strategy](#caching-strategy)
- [Security Architecture](#security-architecture)
- [Scalability & Performance](#scalability--performance)
- [Deployment Architecture](#deployment-architecture)
- [Monitoring & Observability](#monitoring--observability)

## System Overview

SwiftChat is designed as a high-performance, scalable real-time messaging platform capable of supporting millions of concurrent users. The system is built using a microservices architecture pattern to ensure modularity, independent scalability, and resilience.

Key technical characteristics:
- **Stateless services** for horizontal scalability
- **Event-driven architecture** for asynchronous processing
- **Polyglot persistence** with different data stores optimized for specific use cases
- **Circuit breaker patterns** for fault tolerance
- **API Gateway** for unified access and security

## Architecture Principles

1. **Service Isolation**: Each microservice operates independently with its own database
2. **Single Responsibility**: Each service focuses on a specific business capability
3. **Domain-Driven Design**: Services are organized around business domains
4. **API First**: All functionality is exposed through well-defined APIs
5. **Event-Driven**: Using message brokers for async communication between services
6. **Infrastructure as Code**: All infrastructure is defined and managed through code
7. **Design for Failure**: Assume failures will happen and design accordingly
8. **Observability**: Comprehensive logging, monitoring, and tracing

## High-Level Architecture

The system uses a microservices architecture with an API Gateway serving as the entry point. Below is a diagram representation of the high-level architecture:

```
                                 +-----------------+
                                 |                 |
  +--------+    HTTPS   +--------+  API Gateway   +--------+
  | Client +----------->+        |  (Spring Cloud)|        |
  +--------+            +--------+-----------------+--------+
                                |
                                |
         +--------------------+-+--+--------------------+
         |                    |    |                    |
+--------v-------+ +----------v--+ +-v-----------+ +---v------------+
|                | |             | |             | |                |
| Auth Service   | | User Service| | Chat Service| | File Service   |
|                | |             | |             | |                |
+----------------+ +-------------+ +-------------+ +----------------+
         |                |              |                |
         |                |              |                |
+--------v----------------v--------------v----------------v--------+
|                                                                  |
|                       Message Broker (Kafka)                     |
|                                                                  |
+------+----------------+-------------+------------------+---------+
       |                |             |                  |
       |                |             |                  |
+------v-----+  +-------v-----+ +----v--------+  +------v------+
|            |  |             | |             |  |             |
| Auth DB    |  | User DB     | | Chat DB     |  | File Store  |
| (Postgres) |  | (Postgres)  | | (Postgres)  |  | (S3/MinIO)  |
|            |  |             | |             |  |             |
+------------+  +-------------+ +-------------+  +-------------+
                                      |
                                      |
                               +-----v-----+
                               |           |
                               |  Redis    |
                               |  Cache    |
                               |           |
                               +-----------+
```

## Microservices Architecture

### Auth Service

**Purpose**: Handle authentication, authorization, and user identity management

**Key Components**:
- **JWT Provider**: Issues, validates, and refreshes JWT tokens
- **OAuth2 Client**: Integrates with external identity providers (Google, Facebook)
- **Authorization Server**: Manages roles and permissions
- **User Registry**: Stores basic user identity information

**API Endpoints**:
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/oauth2/{provider}` - OAuth2 login
- `POST /api/auth/verify` - Verify email
- `POST /api/auth/reset-password` - Password reset

**Database Schema**:
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255),
  activated BOOLEAN DEFAULT FALSE,
  activation_key VARCHAR(20),
  reset_key VARCHAR(20),
  created_at TIMESTAMP,
  last_login_at TIMESTAMP
);

CREATE TABLE oauth2_accounts (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  provider VARCHAR(50) NOT NULL,
  provider_id VARCHAR(255) NOT NULL,
  created_at TIMESTAMP,
  UNIQUE(provider, provider_id)
);

CREATE TABLE roles (
  id UUID PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
  user_id UUID REFERENCES users(id),
  role_id UUID REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);
```

### User Service

**Purpose**: Manage user profiles, relationships, and preferences

**Key Components**:
- **Profile Manager**: Handles user profile data
- **Contact Service**: Manages user contacts and relationships
- **Preference Manager**: Stores user preferences

**API Endpoints**:
- `GET /api/users/{id}` - Get user profile
- `PUT /api/users/{id}` - Update user profile
- `GET /api/users/{id}/contacts` - Get user contacts
- `POST /api/users/contacts` - Add contact
- `PUT /api/users/settings` - Update user settings
- `GET /api/users/search` - Search users

**Database Schema**:
```sql
CREATE TABLE user_profiles (
  id UUID PRIMARY KEY,
  user_id UUID UNIQUE NOT NULL,
  display_name VARCHAR(100),
  avatar_url VARCHAR(255),
  bio TEXT,
  phone VARCHAR(20),
  status VARCHAR(50),
  last_active_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE contacts (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  contact_id UUID NOT NULL,
  relationship VARCHAR(50),
  blocked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP,
  UNIQUE(user_id, contact_id)
);

CREATE TABLE user_settings (
  user_id UUID PRIMARY KEY,
  notification_preferences JSONB,
  privacy_settings JSONB,
  theme_preferences JSONB,
  language VARCHAR(10) DEFAULT 'en',
  updated_at TIMESTAMP
);
```

### Chat Service

**Purpose**: Core messaging functionality including message delivery and storage

**Key Components**:
- **Message Broker**: Handles message routing and delivery
- **Chat Room Manager**: Manages chat rooms and participants
- **Message Store**: Persists messages
- **WebSocket Handler**: Manages WebSocket connections for real-time communication
- **Message Versioning System**: Tracks message edits and recall events

**API Endpoints**:
- `POST /api/chats` - Create new chat
- `GET /api/chats/{id}` - Get chat details
- `GET /api/chats` - Get user's chats
- `POST /api/chats/{id}/messages` - Send message
- `GET /api/chats/{id}/messages` - Get chat messages
- `PUT /api/chats/{id}/messages/{messageId}` - Update message
- `DELETE /api/chats/{id}/messages/{messageId}` - Delete message
- `PUT /api/chats/{id}/messages/{messageId}/recall` - Recall a message
- `GET /api/chats/{id}/messages/{messageId}/edits` - Get message edit history
- `POST /api/chats/{id}/messages/{messageId}/reactions` - Add reaction to message
- `DELETE /api/chats/{id}/messages/{messageId}/reactions/{reactionType}` - Remove reaction

**WebSocket Endpoints**:
- `/ws/chat` - WebSocket connection entry point
- Subscription topics:
  - `/topic/chats/{chatId}` - Receive messages for specific chat
  - `/user/queue/messages` - User-specific message queue
  - `/topic/status` - User status updates
  - `/topic/chats/{chatId}/typing` - Typing indicators for specific chat
  - `/topic/chats/{chatId}/reactions` - Real-time reactions

**Database Schema**:
```sql
CREATE TABLE chats (
  id UUID PRIMARY KEY,
  type VARCHAR(20) NOT NULL, -- 'private' or 'group'
  name VARCHAR(100),
  created_by UUID NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE chat_participants (
  chat_id UUID REFERENCES chats(id),
  user_id UUID NOT NULL,
  role VARCHAR(20), -- 'admin', 'member'
  joined_at TIMESTAMP,
  last_read_at TIMESTAMP,
  permissions JSONB NOT NULL DEFAULT '{"can_send_messages": true, "can_add_participants": false, "can_remove_participants": false, "can_edit_group_info": false}',
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE messages (
  id UUID PRIMARY KEY,
  chat_id UUID REFERENCES chats(id),
  sender_id UUID NOT NULL,
  content_type VARCHAR(50) NOT NULL, -- 'text', 'image', 'video', 'file'
  content TEXT,
  metadata JSONB,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  version INTEGER DEFAULT 1,
  parent_message_id UUID NULL REFERENCES messages(id), -- For reply feature
  is_recalled BOOLEAN DEFAULT FALSE, -- Flag for recalled messages
  recall_timestamp TIMESTAMP NULL, -- When the message was recalled
  encryption_metadata JSONB NULL -- Encryption-related metadata
);

-- Message edit history
CREATE TABLE message_edits (
  id UUID PRIMARY KEY,
  message_id UUID REFERENCES messages(id),
  previous_content TEXT,
  edited_at TIMESTAMP,
  edited_by UUID NOT NULL
);

-- Message reactions
CREATE TABLE message_reactions (
  id UUID PRIMARY KEY,
  message_id UUID REFERENCES messages(id),
  user_id UUID NOT NULL,
  reaction_type VARCHAR(50) NOT NULL, -- 'like', 'love', 'laugh', 'wow', 'sad', 'angry'
  created_at TIMESTAMP,
  UNIQUE(message_id, user_id, reaction_type)
);

-- Enhanced message status tracking
CREATE TABLE message_status (
  message_id UUID REFERENCES messages(id),
  user_id UUID NOT NULL,
  read_at TIMESTAMP,
  delivered_at TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'sent', -- 'sending', 'sent', 'delivered', 'read', 'failed'
  error_message TEXT NULL,
  PRIMARY KEY (message_id, user_id)
);

-- Pinned messages
CREATE TABLE pinned_messages (
  id UUID PRIMARY KEY,
  chat_id UUID REFERENCES chats(id),
  message_id UUID REFERENCES messages(id),
  pinned_by UUID NOT NULL,
  pinned_at TIMESTAMP,
  UNIQUE(chat_id, message_id)
);

-- Message attachments
CREATE TABLE message_attachments (
  id UUID PRIMARY KEY,
  message_id UUID REFERENCES messages(id),
  file_id UUID REFERENCES files(id),
  display_order INTEGER NOT NULL,
  created_at TIMESTAMP
);

-- Chat invitations
CREATE TABLE chat_invitations (
  id UUID PRIMARY KEY,
  chat_id UUID REFERENCES chats(id),
  inviter_id UUID NOT NULL,
  invitee_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending', 'accepted', 'rejected', 'expired'
  created_at TIMESTAMP,
  expires_at TIMESTAMP,
  response_at TIMESTAMP
);

-- Archived chats
CREATE TABLE archived_chats (
  user_id UUID NOT NULL,
  chat_id UUID REFERENCES chats(id),
  archived_at TIMESTAMP,
  PRIMARY KEY (user_id, chat_id)
);

-- Chat settings
CREATE TABLE chat_settings (
  chat_id UUID PRIMARY KEY REFERENCES chats(id),
  encryption_enabled BOOLEAN DEFAULT FALSE,
  message_retention_days INTEGER DEFAULT 365, -- How long to keep messages
  media_autodownload BOOLEAN DEFAULT TRUE,
  notification_settings JSONB
);

-- Table for tracking typing indicators
CREATE TABLE typing_indicators (
  chat_id UUID NOT NULL,
  user_id UUID NOT NULL,
  timestamp TIMESTAMP NOT NULL,
  PRIMARY KEY (chat_id, user_id)
);

-- Message partitioning for time-series data
CREATE TABLE messages_partitioned (
  id UUID PRIMARY KEY,
  chat_id UUID NOT NULL,
  sender_id UUID NOT NULL,
  content_type VARCHAR(50) NOT NULL,
  content TEXT,
  metadata JSONB,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  version INTEGER DEFAULT 1,
  parent_message_id UUID NULL,
  is_recalled BOOLEAN DEFAULT FALSE,
  recall_timestamp TIMESTAMP NULL,
  encryption_metadata JSONB NULL
) PARTITION BY RANGE (created_at);

-- Monthly partitions example
CREATE TABLE messages_y2023m01 PARTITION OF messages_partitioned
  FOR VALUES FROM ('2023-01-01') TO ('2023-02-01');
  
CREATE TABLE messages_y2023m02 PARTITION OF messages_partitioned
  FOR VALUES FROM ('2023-02-01') TO ('2023-03-01');
```

**High-performance Indexes**:
```sql
CREATE INDEX idx_messages_chat_id ON messages(chat_id, created_at);
CREATE INDEX idx_messages_sender_id ON messages(sender_id, created_at);
CREATE INDEX idx_messages_content ON messages USING gin(to_tsvector('english', content)); -- Full-text search
CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);
CREATE INDEX idx_messages_partitioned_chat_id ON messages_partitioned(chat_id, created_at);
CREATE INDEX idx_messages_partitioned_sender_id ON messages_partitioned(sender_id, created_at);
```

### Notification Service

**Purpose**: Manage and deliver notifications across multiple channels

**Key Components**:
- **Notification Generator**: Creates notifications from system events
- **Notification Router**: Routes notifications based on user preferences
- **Delivery Provider**: Integrates with various notification channels (push, email, in-app)

**API Endpoints**:
- `GET /api/notifications` - Get user notifications
- `PUT /api/notifications/{id}` - Mark notification as read
- `DELETE /api/notifications/{id}` - Delete notification
- `PUT /api/notifications/settings` - Update notification settings

**Database Schema**:
```sql
CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT,
  data JSONB,
  read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP,
  expires_at TIMESTAMP
);

CREATE TABLE notification_settings (
  user_id UUID PRIMARY KEY,
  email BOOLEAN DEFAULT TRUE,
  push BOOLEAN DEFAULT TRUE,
  in_app BOOLEAN DEFAULT TRUE,
  do_not_disturb_start TIME,
  do_not_disturb_end TIME,
  updated_at TIMESTAMP
);

CREATE TABLE push_tokens (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  device_id VARCHAR(255) NOT NULL,
  token TEXT NOT NULL,
  platform VARCHAR(20) NOT NULL, -- 'ios', 'android', 'web'
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  UNIQUE(user_id, device_id)
);
```

### File Service

**Purpose**: Handle file uploads, storage, and delivery

**Key Components**:
- **Upload Handler**: Processes file uploads
- **Storage Manager**: Manages file storage (using S3 or MinIO)
- **Transformation Service**: Processes images and videos (resizing, format conversion)
- **Quota Management**: Tracks and enforces user storage quotas
- **Retention Policy Engine**: Manages file lifecycle based on configurable policies

**API Endpoints**:
- `POST /api/files/upload` - Upload file
- `GET /api/files/{id}` - Get file metadata
- `GET /api/files/{id}/download` - Download file
- `DELETE /api/files/{id}` - Delete file
- `GET /api/files/quota` - Get user's storage quota and usage
- `PUT /api/files/{id}/retention` - Update file retention policy

**Database Schema**:
```sql
CREATE TABLE files (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  size BIGINT NOT NULL,
  storage_path VARCHAR(255) NOT NULL,
  access_type VARCHAR(20) NOT NULL, -- 'public', 'private', 'shared'
  metadata JSONB,
  created_at TIMESTAMP,
  expires_at TIMESTAMP
);

CREATE TABLE file_shares (
  file_id UUID REFERENCES files(id),
  shared_with UUID NOT NULL,
  permission VARCHAR(20) NOT NULL, -- 'view', 'download'
  created_at TIMESTAMP,
  expires_at TIMESTAMP,
  PRIMARY KEY (file_id, shared_with)
);

-- Storage quota management
CREATE TABLE storage_quotas (
  user_id UUID PRIMARY KEY,
  quota_bytes BIGINT NOT NULL DEFAULT 104857600, -- 100MB default
  used_bytes BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP
);

-- File retention policies
CREATE TABLE file_retention_policies (
  id UUID PRIMARY KEY,
  file_type VARCHAR(50) NOT NULL, -- 'image', 'video', 'document', 'audio'
  retention_days INTEGER NOT NULL,
  auto_delete BOOLEAN DEFAULT FALSE
);
```

## Data Architecture

### Data Partitioning Strategy

**Message Partitioning**:
- Messages are partitioned by time range (monthly partitions)
- Each partition is managed independently, improving query performance
- Historical messages are archived to cold storage after their retention period
- Partition rotation is automated with partition creation in advance

**User Data Partitioning**:
- User data is partitioned by user ID
- Sharding is implemented for users with high message volumes

### Data Migration and Retention

- Messages follow retention policies defined at the chat level (defaults to 365 days)
- Account data is retained for 30 days after account deletion
- Media files follow type-specific retention policies with optional auto-deletion
- Message edit history is preserved according to compliance requirements

### Time-Series Data Management

- Message data is treated as time-series data
- Recent data is stored in hot storage for quick access
- Older data is compressed and moved to cold storage
- Automatic partition management ensures optimal performance

## Communication Patterns

### Synchronous Communication
- REST APIs are used for direct service-to-service communication
- GraphQL API is exposed through the API Gateway for frontend clients

### Asynchronous Communication
- Kafka is used as the primary message broker for event-driven communication
- Key event topics:
  - `user-events`: User creation, updates, deletions
  - `message-events`: New messages, message updates, deletions
  - `notification-events`: Events that trigger notifications
  - `file-events`: File uploads, deletions, expiration

### Real-time Communication
- WebSockets with STOMP protocol for real-time messaging
- Connection pooling and load balancing across multiple WebSocket servers

## Caching Strategy

### Multi-Level Caching
- L1: Application-level cache using Caffeine
- L2: Redis distributed cache

### Cached Content
- **Message Cache**:
  - Recent messages for active chats (TTL: 10 minutes)
  - Read receipts (TTL: 1 hour)
  - Message reactions (TTL: 30 minutes)
  - Pinned messages (TTL: 1 hour)
  
- **User Cache**:
  - User profiles (TTL: 30 minutes)
  - User online status (TTL: 30 seconds)
  
- **Authentication Cache**:
  - JWT token blacklist (TTL: matches token expiry)
  - OAuth2 state tokens (TTL: 10 minutes)

### Cache Invalidation Strategies
- Time-based expiration
- Event-based invalidation through Kafka events
- Write-through cache for critical data

## Security Architecture

### Authentication
- **JWT-based authentication**:
  - Access tokens (15 minute expiry)
  - Refresh tokens (7 day expiry)
  - Token rotation on refresh
  
- **OAuth2 Integration**:
  - Support for Google, Facebook, Apple providers
  - PKCE flow for mobile clients
  - State parameter validation

### Authorization
- Role-based access control (RBAC)
- Resource-based access controls for chats and files
- Attribute-based access control for special operations

### Data Security
- Message encryption:
  - TLS for transport security
  - AES-256 for message content encryption
  - End-to-end encryption option for private chats
  - Message versioning to track edits and recalls
  - Secure key management for E2E encryption
  
- Data at rest:
  - Database encryption
  - Encrypted file storage
  - Secure key rotation

### API Security
- API rate limiting
- OWASP top 10 protections
- CORS configuration
- CSP headers

## Scalability & Performance

### Horizontal Scaling
- All services are designed for horizontal scaling
- Kubernetes auto-scaling based on CPU/memory metrics
- Database read replicas for query-heavy services

### Performance Optimizations
- Connection pooling for database connections
- Query optimization and strategic indexing
- Table partitioning for high-volume tables
- Batch processing for bulk operations
- Pagination for large result sets
- Materialized views for complex, frequently accessed data

### Load Handling
- **Chat Service**: Designed to handle 10,000+ messages/second
- **WebSocket Service**: Support for 100,000+ concurrent connections
- **File Service**: Parallel upload/download capabilities
- **Database**: Optimized for high write throughput with partitioned tables

## Deployment Architecture

### Kubernetes Infrastructure
- Multiple node pools optimized for different workloads
- Stateful services use StatefulSets with persistent volumes
- Istio service mesh for advanced traffic management

### CI/CD Pipeline
- GitLab CI/CD pipeline
- Automated testing
  - Unit tests
  - Integration tests
  - Performance tests
  - Security scans
- Blue/green deployment strategy

### Multi-Environment Setup
- Development
- Testing
- Staging
- Production

### Cloud Infrastructure
- Kubernetes deployed on AWS EKS
- AWS RDS for PostgreSQL
- AWS ElastiCache for Redis
- AWS S3 for file storage

## Monitoring & Observability

### Logging
- Structured JSON logging
- Log aggregation with Elasticsearch
- Log retention policies based on environment

### Metrics
- Prometheus for metrics collection
- Grafana for visualization
- Custom dashboards for each service
- Business KPIs tracking

### Tracing
- Distributed tracing with OpenTelemetry
- Correlation IDs across service boundaries
- Integration with Jaeger for trace visualization

### Alerting
- PagerDuty integration
- Alert thresholds for critical metrics
- Automated remediation for common issues

### Health Checks
- Liveness probes for service health
- Readiness probes for service availability
- Deep health checks for dependent services
