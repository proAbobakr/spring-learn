# Doctor Social Platform - Architecture Documentation

## System Architecture Overview

This document provides a comprehensive overview of the Doctor Social Platform architecture, design decisions, and scalability considerations.

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Microservices Design](#microservices-design)
3. [Data Architecture](#data-architecture)
4. [Communication Patterns](#communication-patterns)
5. [Security Architecture](#security-architecture)
6. [Scalability Design](#scalability-design)
7. [Deployment Architecture](#deployment-architecture)

## High-Level Architecture

### Architecture Principles

1. **Microservices**: Each service is independently deployable and scalable
2. **Domain-Driven Design**: Services organized around business domains
3. **Event-Driven**: Asynchronous communication via Kafka
4. **API Gateway**: Single entry point for all client requests
5. **Service Discovery**: Dynamic service registration and discovery
6. **Database per Service**: Each service owns its data
7. **Resilience**: Circuit breakers, retries, and fallbacks

### System Components

```
┌─────────────────────────────────────────────────────────┐
│                     Clients Layer                        │
│  (Mobile Apps, Web Browser, Third-party Integrations)   │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│                     API Gateway                          │
│  - Authentication Filter                                 │
│  - Rate Limiting (Redis)                                │
│  - Load Balancing                                       │
│  - Circuit Breaker                                      │
│  - Request Routing                                      │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
┌───────────────────┐        ┌──────────────────┐
│Service Discovery  │        │  Configuration   │
│    (Eureka)       │        │     Server       │
└───────────────────┘        └──────────────────┘
        │
        │ Service Registration & Health Checks
        │
┌───────┴────────────────────────────────────────────────┐
│                  Microservices Layer                    │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌─────────┐  ┌─────────┐│
│  │   User   │  │   Post   │  │  Media  │  │ Comment ││
│  │ Service  │  │ Service  │  │ Service │  │ Service ││
│  └────┬─────┘  └────┬─────┘  └────┬────┘  └────┬────┘│
│       │             │              │             │     │
│  ┌────┴─────┐  ┌───┴──────┐  ┌───┴──────┐  ┌──┴────┐│
│  │   Call   │  │    AI    │  │   Notif  │  │Analytic││
│  │ Service  │  │ Service  │  │ Service  │  │Service ││
│  └──────────┘  └──────────┘  └──────────┘  └────────┘│
└─────────────────────────────────────────────────────────┘
        │                              │
        ▼                              ▼
┌──────────────────┐          ┌──────────────────┐
│  Data Layer      │          │  Message Layer   │
│                  │          │                  │
│  ┌────────────┐ │          │  ┌────────────┐ │
│  │ PostgreSQL │ │          │  │   Kafka    │ │
│  │  Cluster   │ │          │  │  Cluster   │ │
│  └────────────┘ │          │  └────────────┘ │
│                  │          │                  │
│  ┌────────────┐ │          └──────────────────┘
│  │   Redis    │ │
│  │  Cluster   │ │
│  └────────────┘ │
└──────────────────┘
```

## Microservices Design

### 1. User Service (Port: 8081)

**Responsibilities:**
- Doctor registration and authentication
- Profile management
- Following/Follower relationships
- User search and discovery

**Database Schema:**
- `doctors` table: Core user information
- `followings` table: Many-to-many relationships

**APIs:**
- POST `/api/auth/register`
- POST `/api/auth/login`
- GET `/api/users/{id}`
- POST `/api/users/{followerId}/follow/{followingId}`
- GET `/api/users/search`

**Scalability:**
- Horizontal scaling: 3-20 replicas
- Database: Master-slave replication
- Cache: Redis for user profiles
- Sharding strategy: By user ID

### 2. Post Service (Port: 8082)

**Responsibilities:**
- Create, read, update, delete posts
- Medical case studies
- Research papers
- Discussions
- Post likes
- Trending algorithm

**Database Schema:**
- `posts` table: Post content and metadata
- `likes` table: Post likes
- `post_tags` table: Tags for categorization
- `post_media_urls` table: Associated media

**APIs:**
- POST `/api/posts`
- GET `/api/posts/{id}`
- GET `/api/posts/trending`
- GET `/api/posts/search`
- POST `/api/posts/{postId}/like/{doctorId}`

**Scalability:**
- Read-heavy workload: Read replicas
- Cache: Popular posts in Redis
- Search: Elasticsearch integration (future)

### 3. Media Service (Port: 8083)

**Responsibilities:**
- Upload medical images, PDFs, documents
- Image processing and optimization
- Thumbnail generation
- Storage management (S3/MinIO)
- AI analysis integration

**Storage Strategy:**
- Original files: S3/MinIO
- Metadata: PostgreSQL
- Thumbnails: CDN

**APIs:**
- POST `/api/media/upload`
- GET `/api/media/{id}`
- DELETE `/api/media/{id}`
- GET `/api/media/doctor/{doctorId}`

### 4. Comment Service (Port: 8084)

**Responsibilities:**
- Add, edit, delete comments
- Nested/threaded comments
- Comment moderation

**Database Schema:**
- `comments` table with self-referential parent_id

**APIs:**
- POST `/api/comments`
- GET `/api/comments/post/{postId}`
- PUT `/api/comments/{id}`
- DELETE `/api/comments/{id}`

### 5. Call Service (Port: 8085)

**Responsibilities:**
- WebRTC signaling
- Call session management
- Call history
- Recording metadata

**Technology:**
- WebSocket for signaling
- STUN/TURN servers for NAT traversal
- Optional: Jitsi/Janus integration

**APIs:**
- WebSocket: `/ws/call`
- POST `/api/calls/initiate`
- GET `/api/calls/history/{doctorId}`

### 6. AI Service (Port: 8086)

**Responsibilities:**
- Medical image analysis
- Case analysis
- Symptom extraction (NLP)
- Diagnosis suggestions
- Literature search

**Processing Flow:**
1. Consume Kafka event (new media uploaded)
2. Fetch image from Media Service
3. Run ML model inference
4. Store analysis results
5. Publish completion event

**Integration Points:**
- TensorFlow Serving
- PyTorch models
- Hugging Face transformers
- Custom medical ML models

### 7. Notification Service (Port: 8087)

**Responsibilities:**
- Real-time notifications via WebSocket
- Push notifications
- Email notifications
- In-app notifications

**Event Types:**
- New follower
- Post liked
- Comment on post
- Call invitation
- AI analysis complete

**Technology:**
- WebSocket for real-time
- Firebase Cloud Messaging for push
- SendGrid/AWS SES for email

## Data Architecture

### Database Strategy: Database per Service

Each microservice has its own PostgreSQL database:
- `doctor_users` - User Service
- `doctor_posts` - Post Service
- `doctor_media` - Media Service
- `doctor_comments` - Comment Service

**Benefits:**
1. **Loose Coupling**: Services don't share database schemas
2. **Technology Diversity**: Each service can choose optimal DB
3. **Independent Scaling**: Scale databases independently
4. **Fault Isolation**: Database failure affects only one service

**Challenges & Solutions:**
- **Data Consistency**: Use Saga pattern for distributed transactions
- **Joins Across Services**: Use API composition or CQRS
- **Data Duplication**: Acceptable for read performance

### Caching Strategy

#### Three-Level Caching

**Level 1: Application Cache**
- Spring Cache abstraction
- In-memory cache per service instance
- TTL: 5 minutes
- Use case: Method results, computed values

**Level 2: Distributed Cache (Redis)**
- Shared across all service instances
- TTL: Varies by entity
  - User profiles: 1 hour
  - Posts: 30 minutes
  - Trending: 5 minutes
- Use case: Frequently accessed data

**Level 3: CDN**
- CloudFlare/Akamai
- Static assets and media
- TTL: 24 hours
- Use case: Images, thumbnails, static files

#### Cache Invalidation

**Event-Driven Invalidation:**
```
User updated → Publish event → Cache invalidation
Post created → Publish event → Trending cache invalidation
```

### Data Replication

#### PostgreSQL Replication

**Master-Slave Replication:**
```
Master (Write)
  ├── Slave 1 (Read)
  ├── Slave 2 (Read)
  └── Slave 3 (Read)
```

**Configuration:**
- Synchronous replication for critical data (users, posts)
- Asynchronous replication for analytics
- Automatic failover with PgPool or Patroni

#### Redis Replication

**Sentinel Configuration:**
```
Redis Master
  ├── Replica 1
  ├── Replica 2
  │
Sentinel 1, 2, 3 (Monitor & Failover)
```

## Communication Patterns

### Synchronous Communication

**REST via Spring Cloud OpenFeign:**
- User Service ↔ Post Service (verify doctor)
- Post Service ↔ Media Service (fetch media)
- Comment Service ↔ User Service (verify commenter)

**Best Practices:**
- Circuit breakers (Resilience4j)
- Timeouts (3-5 seconds)
- Retries with exponential backoff
- Fallback responses

### Asynchronous Communication

**Kafka Event Streaming:**

**Topics:**
- `user-events`: User registered, updated, deleted
- `post-events`: Post created, liked, deleted
- `media-events`: Media uploaded, analyzed
- `ai-events`: Analysis requested, completed
- `notification-events`: Notifications to send

**Event Schema Example:**
```json
{
  "eventType": "POST_CREATED",
  "eventId": "uuid",
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": {
    "postId": 123,
    "doctorId": 456,
    "postType": "CASE_STUDY"
  }
}
```

**Consumer Groups:**
- Each service has its own consumer group
- Parallel processing with multiple partitions
- Offset management for exactly-once processing

## Security Architecture

### Authentication & Authorization

**JWT Token Flow:**
```
1. Client → POST /api/auth/login → User Service
2. User Service → Validates credentials
3. User Service → Generates JWT token
4. Client → Stores token
5. Client → Requests with Authorization: Bearer {token}
6. API Gateway → Validates token signature
7. API Gateway → Forwards to microservice
```

**Token Structure:**
```json
{
  "sub": "doctor@example.com",
  "roles": ["ROLE_DOCTOR"],
  "doctorId": 123,
  "iat": 1642252800,
  "exp": 1642339200
}
```

### Network Security

**Kubernetes Network Policies:**
- Services can only communicate through defined paths
- External access only via API Gateway
- Database accessible only from service pods

### Data Security

**Encryption:**
- At rest: Database encryption (TDE)
- In transit: TLS 1.3 for all communications
- Secrets: Kubernetes Secrets or Vault

**Sensitive Data:**
- Patient information: Encrypted columns
- Passwords: BCrypt (cost factor: 12)
- Medical images: HIPAA-compliant storage

## Scalability Design

### Horizontal Scaling

**Kubernetes Horizontal Pod Autoscaler (HPA):**

```yaml
User Service:
  Min: 3 replicas
  Max: 20 replicas
  CPU Target: 70%
  Memory Target: 80%

Post Service:
  Min: 3 replicas
  Max: 30 replicas  # Higher due to read-heavy
  CPU Target: 70%
```

**Scaling Triggers:**
- CPU utilization
- Memory utilization
- Custom metrics (request rate, queue depth)

### Database Scaling

**Vertical Scaling:**
- Start: 4 vCPU, 16GB RAM
- Scale up to: 32 vCPU, 128GB RAM

**Horizontal Scaling:**
- **Reads**: Add read replicas (up to 15)
- **Writes**: Shard by user ID
  - Shard 1: User IDs 1-1M
  - Shard 2: User IDs 1M-2M
  - etc.

**Connection Pooling:**
- HikariCP configuration
- Max pool size: 20 per instance
- Connection timeout: 30s
- Idle timeout: 10 minutes

### Caching for Scale

**Cache Hit Ratio Target: >80%**

**Strategies:**
- **Cache-Aside**: Application checks cache, then DB
- **Write-Through**: Write to cache and DB simultaneously
- **Write-Behind**: Write to cache, async to DB

**Redis Cluster:**
- 3 master nodes (sharded)
- 3 replica nodes
- ~1TB total memory
- Eviction policy: `allkeys-lru`

### Message Queue Scaling

**Kafka Configuration:**
- Topics: 3-30 partitions (based on throughput)
- Replication factor: 3
- Consumer groups: Scale consumers = partitions

**Target Throughput:**
- 100,000+ events/second
- <10ms producer latency
- <100ms end-to-end latency

## Deployment Architecture

### Development Environment
- Docker Compose
- Local PostgreSQL, Redis, Kafka
- Single instance of each service

### Staging Environment
- Kubernetes cluster (3 nodes)
- Managed PostgreSQL (RDS/Cloud SQL)
- Managed Redis (ElastiCache)
- 2 replicas per service

### Production Environment

**Kubernetes Cluster:**
- Multi-zone deployment
- Node pools:
  - General: 5-20 nodes (8 vCPU, 32GB RAM)
  - Memory-intensive: 2-10 nodes (16 vCPU, 128GB RAM)
  - AI workloads: 2-5 nodes with GPU

**Database:**
- Managed PostgreSQL with automatic failover
- Multi-AZ deployment
- Automated backups (daily, 30-day retention)
- Point-in-time recovery

**Redis:**
- Managed Redis Cluster
- 6 nodes (3 master, 3 replica)
- Automatic failover
- 16GB per node

**Kafka:**
- Managed Kafka (MSK, Confluent Cloud)
- 3 brokers minimum
- Multi-AZ
- Monitoring and alerts

### Monitoring Stack

**Prometheus + Grafana:**
- Service-level metrics
- JVM metrics
- Custom business metrics
- Alerting rules

**ELK Stack:**
- Centralized logging
- Log correlation by request ID
- Search and analytics

**Distributed Tracing:**
- Jaeger or Zipkin
- Track requests across services
- Performance bottleneck identification

### Disaster Recovery

**Recovery Time Objective (RTO): 1 hour**
**Recovery Point Objective (RPO): 5 minutes**

**Backup Strategy:**
- Database: Automated backups every 6 hours
- Redis: RDB snapshots every hour
- Kafka: Data retention 7 days
- Media files: S3 versioning enabled

**Failover:**
- Active-Active multi-region for API Gateway
- Active-Passive for databases
- Automatic DNS failover

## Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| API Response Time (P50) | <100ms | - |
| API Response Time (P99) | <500ms | - |
| Database Query Time (P99) | <50ms | - |
| Cache Hit Rate | >80% | - |
| System Availability | 99.9% | - |
| Concurrent Users | 100,000+ | - |
| Requests per Second | 10,000+ | - |

## Future Enhancements

1. **Service Mesh (Istio/Linkerd)**
   - Advanced traffic management
   - Mutual TLS
   - Distributed tracing out-of-the-box

2. **GraphQL Gateway**
   - Flexible data fetching
   - Reduce over-fetching
   - Better mobile app support

3. **CQRS Pattern**
   - Separate read and write models
   - Optimized for each operation type

4. **Event Sourcing**
   - Complete audit trail
   - Time-travel debugging
   - Replay events

5. **Multi-Region Deployment**
   - Global user base
   - Reduced latency
   - Geographic compliance

---

This architecture is designed to scale from thousands to millions of users while maintaining high availability, performance, and security.
