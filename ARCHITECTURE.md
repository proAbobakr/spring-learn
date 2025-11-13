# System Architecture Documentation

Complete architecture overview of the Location Rating Service.

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Patterns](#architecture-patterns)
3. [Component Diagram](#component-diagram)
4. [Database Schema](#database-schema)
5. [API Architecture](#api-architecture)
6. [Caching Strategy](#caching-strategy)
7. [Event-Driven Architecture](#event-driven-architecture)
8. [Security Architecture](#security-architecture)
9. [Scalability & High Availability](#scalability--high-availability)
10. [Microservices Evolution Path](#microservices-evolution-path)

---

## System Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                             │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│   │ Mobile App  │  │   Web App   │  │  Third-Party│           │
│   │  (Android)  │  │  (React)    │  │     API     │           │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘           │
└──────────┼─────────────────┼─────────────────┼─────────────────┘
           │                 │                 │
           └─────────────────┴─────────────────┘
                             │
                    ┌────────▼─────────┐
                    │   Load Balancer  │ (nginx/AWS ELB)
                    │   (Port 80/443)  │
                    └────────┬─────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   ┌────▼────┐          ┌────▼────┐         ┌────▼────┐
   │ Spring  │          │ Spring  │         │ Spring  │
   │ Boot    │          │ Boot    │         │ Boot    │
   │ App 1   │          │ App 2   │         │ App 3   │
   │ :8080   │          │ :8080   │         │ :8080   │
   └────┬────┘          └────┬────┘         └────┬────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
        ┌──────────┬─────────┴─────────┬──────────┐
        │          │                   │          │
   ┌────▼────┐┌───▼────┐        ┌─────▼────┐┌───▼────┐
   │PostgreSQL││ Redis  │        │  Kafka   ││ S3/    │
   │  :5432  ││ :6379  │        │  :9092   ││ Files  │
   └─────────┘└────────┘        └──────────┘└────────┘
    (Database)  (Cache)          (Events)    (Storage)
```

---

## Architecture Patterns

### 1. Layered Architecture (Current Implementation)

```
┌───────────────────────────────────────────────────────────┐
│                    Presentation Layer                     │
│  ┌────────────────────────────────────────────────────┐  │
│  │        REST Controllers (@RestController)          │  │
│  │  - AuthController    - LocationController          │  │
│  │  - RatingController  - CommentController           │  │
│  │  - ImageController                                 │  │
│  └────────────────┬───────────────────────────────────┘  │
└───────────────────┼───────────────────────────────────────┘
                    │ DTOs (Request/Response)
┌───────────────────▼───────────────────────────────────────┐
│                   Business Logic Layer                    │
│  ┌────────────────────────────────────────────────────┐  │
│  │            Services (@Service)                     │  │
│  │  - AuthService       - LocationService             │  │
│  │  - RatingService     - CommentService              │  │
│  │  - ImageService      - EventPublisher              │  │
│  └────────────────┬───────────────────────────────────┘  │
└───────────────────┼───────────────────────────────────────┘
                    │ Domain Models
┌───────────────────▼───────────────────────────────────────┐
│                  Data Access Layer                        │
│  ┌────────────────────────────────────────────────────┐  │
│  │     Repositories (JpaRepository)                   │  │
│  │  - UserRepository    - LocationRepository          │  │
│  │  - RatingRepository  - CommentRepository           │  │
│  │  - ImageRepository                                 │  │
│  └────────────────┬───────────────────────────────────┘  │
└───────────────────┼───────────────────────────────────────┘
                    │ JPA/Hibernate
┌───────────────────▼───────────────────────────────────────┐
│                   Database Layer                          │
│                  PostgreSQL                               │
└───────────────────────────────────────────────────────────┘

        Cross-Cutting Concerns (AOP)
┌─────────────────────────────────────────────────────────┐
│  - Security (JWT Filter)    - Exception Handling        │
│  - Caching (Redis)          - Event Publishing (Kafka)  │
│  - Logging                  - Validation                │
└─────────────────────────────────────────────────────────┘
```

### Key Benefits of Layered Architecture

1. **Separation of Concerns**: Each layer has specific responsibilities
2. **Testability**: Easy to unit test each layer independently
3. **Maintainability**: Changes in one layer don't affect others
4. **Reusability**: Services can be reused across controllers
5. **Android Similarity**: Matches Android's Activity→ViewModel→Repository pattern

---

## Component Diagram

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Security Components                        │  │
│  │  ┌────────────┐  ┌──────────────┐  ┌────────────┐  │  │
│  │  │  JwtUtil   │  │JwtAuthFilter │  │UserDetails │  │  │
│  │  │  (Token    │→ │(Intercepts   │→ │  Service   │  │  │
│  │  │Generation) │  │  Requests)   │  │ (Auth)     │  │  │
│  │  └────────────┘  └──────────────┘  └────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Configuration Components                   │  │
│  │  ┌──────────┐  ┌───────────┐  ┌────────────┐       │  │
│  │  │ Security │  │   Redis   │  │   Kafka    │       │  │
│  │  │  Config  │  │  Config   │  │  Config    │       │  │
│  │  └──────────┘  └───────────┘  └────────────┘       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Core Business Services                    │  │
│  │                                                       │  │
│  │   ┌──────────────┐    ┌──────────────┐             │  │
│  │   │ Location     │───▶│ Event        │             │  │
│  │   │ Service      │    │ Publisher    │             │  │
│  │   │ (Geospatial) │    │ (Kafka)      │             │  │
│  │   └───┬──────────┘    └──────────────┘             │  │
│  │       │ Caching (Redis)                             │  │
│  │   ┌───▼──────────┐    ┌──────────────┐             │  │
│  │   │ Rating       │    │ Comment      │             │  │
│  │   │ Service      │    │ Service      │             │  │
│  │   │ (Aggregate)  │    │ (Nested)     │             │  │
│  │   └──────────────┘    └──────────────┘             │  │
│  │                                                       │  │
│  │   ┌──────────────┐    ┌──────────────┐             │  │
│  │   │ Image        │    │ Auth         │             │  │
│  │   │ Service      │    │ Service      │             │  │
│  │   │ (Upload)     │    │ (JWT)        │             │  │
│  │   └──────────────┘    └──────────────┘             │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Data Access Layer                         │  │
│  │   ┌─────────────────────────────────────────────┐   │  │
│  │   │  Spring Data JPA Repositories               │   │  │
│  │   │  (Auto-implementation of CRUD operations)   │   │  │
│  │   └─────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────────┐
│       USERS         │
├─────────────────────┤
│ id (PK)            │
│ username (unique)   │◄──────────┐
│ email (unique)      │           │
│ password            │           │
│ full_name           │           │
│ role                │           │
│ created_at          │           │
│ updated_at          │           │
└──────┬──────────────┘           │
       │                          │
       │ 1:N                      │
       │                          │
┌──────▼──────────────┐           │
│     LOCATIONS       │           │
├─────────────────────┤           │
│ id (PK)            │           │
│ user_id (FK)       │───────────┘
│ name                │
│ description         │
│ latitude            │◄──────────┐
│ longitude           │           │
│ address             │           │
│ city                │           │
│ country             │           │
│ category            │           │
│ average_rating      │           │
│ total_ratings       │           │
│ view_count          │           │
│ active              │           │
│ created_at          │           │
│ updated_at          │           │
└──────┬──────────────┘           │
       │                          │
       │ 1:N                      │
       ├──────────────────────────┼──────────┐
       │                          │          │
┌──────▼──────────┐   ┌──────────▼────┐  ┌──▼──────────┐
│    RATINGS      │   │   COMMENTS    │  │   IMAGES    │
├─────────────────┤   ├───────────────┤  ├─────────────┤
│ id (PK)        │   │ id (PK)       │  │ id (PK)     │
│ user_id (FK)   │   │ user_id (FK)  │  │ location_id │
│ location_id(FK)│   │ location_id   │  │ user_id     │
│ score (1-5)    │   │ parent_id(FK) │◄─┤ file_name   │
│ review         │   │ content       │  │ file_path   │
│ created_at     │   │ like_count    │  │ url         │
│ updated_at     │   │ edited        │  │ is_primary  │
└────────────────┘   │ created_at    │  │ created_at  │
                     │ updated_at    │  └─────────────┘
  Constraint:        └───────────────┘
  UNIQUE(user_id,       Self-referencing
  location_id)          for replies
```

### Key Indexes

```sql
-- Performance-critical indexes
CREATE INDEX idx_location_coords ON locations(latitude, longitude);
CREATE INDEX idx_location_city ON locations(city);
CREATE INDEX idx_location_category ON locations(category);
CREATE INDEX idx_rating_location ON ratings(location_id);
CREATE INDEX idx_rating_user ON ratings(user_id);
CREATE INDEX idx_comment_location ON comments(location_id);
CREATE INDEX idx_comment_parent ON comments(parent_comment_id);
CREATE INDEX idx_image_location ON images(location_id);
```

---

## API Architecture

### RESTful API Design

```
Resource-Based URLs:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Collection Resources:
  GET    /api/locations              → List all
  POST   /api/locations              → Create new

Individual Resources:
  GET    /api/locations/{id}         → Get one
  PUT    /api/locations/{id}         → Update
  DELETE /api/locations/{id}         → Delete

Sub-resources:
  GET    /api/locations/{id}/ratings → Get location's ratings
  GET    /api/locations/{id}/comments → Get location's comments
  GET    /api/locations/{id}/images  → Get location's images

Filtering & Pagination:
  GET    /api/locations?page=0&size=20
  GET    /api/locations?city=NYC
  GET    /api/locations?category=PARK

Special Operations:
  GET    /api/locations/nearby?lat=...&lng=...
  GET    /api/locations/search?keyword=...
  GET    /api/locations/trending
```

### HTTP Methods & Status Codes

| Method | Purpose | Success Code | Example |
|--------|---------|--------------|---------|
| GET | Retrieve resource | 200 OK | Get location |
| POST | Create resource | 201 Created | Create location |
| PUT | Update resource | 200 OK | Update location |
| DELETE | Delete resource | 204 No Content | Delete location |

### Authentication Flow

```
┌──────────┐                               ┌──────────┐
│  Client  │                               │  Server  │
└────┬─────┘                               └────┬─────┘
     │                                          │
     │  POST /api/auth/register                │
     │  {username, email, password}            │
     ├────────────────────────────────────────►│
     │                                          │
     │  201 Created                             │
     │  {token, userId, username, ...}          │
     │◄────────────────────────────────────────┤
     │                                          │
     │  For subsequent requests:                │
     │  Authorization: Bearer <token>           │
     │                                          │
     │  GET /api/locations                      │
     │  Authorization: Bearer eyJhbG...         │
     ├────────────────────────────────────────►│
     │                                          │
     │  ┌─────────────────────────┐            │
     │  │ JwtAuthenticationFilter  │            │
     │  │ - Extract token          │            │
     │  │ - Validate signature     │            │
     │  │ - Check expiration       │            │
     │  │ - Load user details      │            │
     │  │ - Set SecurityContext    │            │
     │  └─────────────────────────┘            │
     │                                          │
     │  200 OK {locations: [...]}               │
     │◄────────────────────────────────────────┤
     │                                          │
```

---

## Caching Strategy

### Redis Caching Architecture

```
┌──────────────────────────────────────────────────────────┐
│                   Application Layer                      │
└────────────────────┬─────────────────────────────────────┘
                     │
                     │ @Cacheable, @CachePut, @CacheEvict
                     │
┌────────────────────▼─────────────────────────────────────┐
│                   Redis Cache                            │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Cache Keys:                     TTL:                    │
│  ━━━━━━━━━━                     ━━━━━                   │
│  locations:{id}                 30 minutes               │
│  trending                       5 minutes                │
│  nearby:{lat}:{lng}:{radius}    10 minutes               │
│  location:views:{id}            1 hour                   │
│                                                          │
└────────────────────┬─────────────────────────────────────┘
                     │
                     │ Cache Miss
                     │
┌────────────────────▼─────────────────────────────────────┐
│                PostgreSQL Database                       │
└──────────────────────────────────────────────────────────┘
```

### Cache Flow

```
Client Request
     │
     ▼
┌─────────────┐
│ Controller  │
└──────┬──────┘
       │
       ▼
┌─────────────┐    Cache Hit      ┌──────────┐
│  Service    │──────────────────► │  Redis   │
│(@Cacheable) │                    │          │
└──────┬──────┘                    └──────────┘
       │
       │ Cache Miss
       ▼
┌─────────────┐
│ Repository  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Database   │
└──────┬──────┘
       │
       │ Store in cache
       ▼
┌─────────────┐
│   Redis     │
└─────────────┘
```

### Cache Invalidation Strategy

```java
// Update location → Invalidate cache
@CachePut(value = "locations", key = "#id")
public Location updateLocation(Long id, LocationDTO dto) {
    // Update logic
}

// Delete location → Remove from cache
@CacheEvict(value = "locations", key = "#id")
public void deleteLocation(Long id) {
    // Delete logic
}
```

---

## Event-Driven Architecture

### Kafka Event Flow

```
┌───────────────────────────────────────────────────────────┐
│                 Application Services                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ Location Svc │  │ Rating Svc   │  │ Comment Svc  │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
└─────────┼──────────────────┼──────────────────┼───────────┘
          │                  │                  │
          │ Publish Events   │                  │
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────┐
│                    Kafka Broker                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Topics:                    Partitions:                 │
│  ━━━━━━━                   ━━━━━━━━━━                 │
│  • location-events          3 partitions                │
│  • rating-events            3 partitions                │
│  • comment-events           3 partitions                │
│  • image-events             3 partitions                │
│                                                         │
└────┬──────────────┬──────────────┬──────────────┬───────┘
     │              │              │              │
     │ Subscribe    │              │              │
     │              │              │              │
┌────▼──────┐  ┌───▼───────┐  ┌──▼───────┐  ┌───▼────────┐
│Notification│  │ Analytics │  │  Email   │  │  Webhook   │
│ Consumer   │  │ Consumer  │  │ Consumer │  │  Consumer  │
└────────────┘  └───────────┘  └──────────┘  └────────────┘
   (Future)       (Future)       (Future)      (Future)
```

### Event Types

```java
// Location Events
LocationEvent {
    eventType: CREATED | UPDATED | DELETED | VIEWED
    locationId, locationName, latitude, longitude
    userId, username, timestamp
}

// Rating Events
RatingEvent {
    eventType: CREATED | UPDATED | DELETED
    ratingId, locationId, userId
    score, timestamp
}

// Comment Events
CommentEvent {
    eventType: CREATED | UPDATED | DELETED | LIKED
    commentId, locationId, userId
    content, parentCommentId, timestamp
}
```

### Benefits of Event-Driven Architecture

1. **Decoupling**: Services don't need to know about each other
2. **Scalability**: Easy to add new consumers
3. **Asynchronous Processing**: Non-blocking operations
4. **Audit Trail**: All events are logged
5. **Real-time Updates**: Stream processing capabilities

---

## Security Architecture

### JWT Authentication Flow

```
┌────────────────────────────────────────────────────────────┐
│                    Security Filter Chain                   │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  1. ┌─────────────────────────┐                           │
│     │ CORS Filter             │ → Handle cross-origin     │
│     └───────────┬─────────────┘                           │
│                 │                                          │
│  2. ┌───────────▼─────────────┐                           │
│     │ JWT Authentication      │ → Extract & validate JWT  │
│     │ Filter                  │                           │
│     └───────────┬─────────────┘                           │
│                 │                                          │
│  3. ┌───────────▼─────────────┐                           │
│     │ Username/Password       │ → For /api/auth/login     │
│     │ Authentication Filter   │                           │
│     └───────────┬─────────────┘                           │
│                 │                                          │
│  4. ┌───────────▼─────────────┐                           │
│     │ Authorization Filter    │ → Check permissions       │
│     └───────────┬─────────────┘                           │
│                 │                                          │
│                 ▼                                          │
│         ┌───────────────┐                                 │
│         │  Controller   │                                 │
│         └───────────────┘                                 │
└────────────────────────────────────────────────────────────┘
```

### Security Layers

```
Defense in Depth:
━━━━━━━━━━━━━━━━━

Layer 1: Network Security
  • Firewall rules
  • VPC/Security groups
  • DDoS protection

Layer 2: Transport Security
  • HTTPS/TLS encryption
  • Certificate validation

Layer 3: Application Security
  • JWT token validation
  • Password encryption (BCrypt)
  • CORS configuration
  • Input validation

Layer 4: Data Security
  • Database encryption at rest
  • Parameterized queries (SQL injection protection)
  • File upload validation

Layer 5: Monitoring
  • Access logs
  • Security event monitoring
  • Anomaly detection
```

---

## Scalability & High Availability

### Horizontal Scaling Architecture

```
┌────────────────────────────────────────────────────────────┐
│                     Load Balancer                          │
│                  (nginx / AWS ALB)                         │
└──────┬──────────────┬──────────────┬──────────────────────┘
       │              │              │
   ┌───▼───┐      ┌───▼───┐      ┌───▼───┐
   │ App 1 │      │ App 2 │      │ App 3 │ → Auto-scaling
   │       │      │       │      │       │   based on CPU/Memory
   └───┬───┘      └───┬───┘      └───┬───┘
       │              │              │
       └──────────────┴──────────────┘
                      │
       ┌──────────────┼──────────────┐
       │              │              │
   ┌───▼───┐      ┌───▼───┐      ┌───▼───┐
   │Primary│      │ Redis │      │ Kafka │
   │  DB   │      │Cluster│      │Cluster│
   └───┬───┘      └───────┘      └───────┘
       │
   ┌───▼───┐
   │Replica│ → Read replicas for
   │  DB   │   scaling reads
   └───────┘
```

### Database Replication

```
Primary (Write):
┌──────────────┐
│  PostgreSQL  │
│   Primary    │ ← All writes go here
└───────┬──────┘
        │ Async replication
        ├──────────────┬──────────────┐
        │              │              │
   ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
   │Replica 1│    │Replica 2│    │Replica 3│ ← Read queries
   └─────────┘    └─────────┘    └─────────┘   distributed
```

---

## Microservices Evolution Path

### Phase 1: Modular Monolith (Current)

```
┌────────────────────────────────────────┐
│      Single Spring Boot Application   │
│  ┌──────────┐  ┌──────────┐          │
│  │  User    │  │ Location │          │
│  │  Module  │  │  Module  │          │
│  └──────────┘  └──────────┘          │
│  ┌──────────┐  ┌──────────┐          │
│  │ Rating   │  │ Comment  │          │
│  │ Module   │  │  Module  │          │
│  └──────────┘  └──────────┘          │
└────────────────────────────────────────┘
         │
    Single Database
```

### Phase 2: Microservices (Future)

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │Location Svc  │  │Rating Service│
│   :8081      │  │   :8082      │  │   :8083      │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
   ┌───▼───┐        ┌────▼────┐       ┌────▼────┐
   │User DB│        │Location │       │Rating DB│
   └───────┘        │   DB    │       └─────────┘
                    └─────────┘

           Connected via Kafka Events
```

### When to Split into Microservices?

✅ Split when:
- Team size > 10 developers
- Need to scale components independently
- Different technology needs per service
- Deploy different components independently

❌ Don't split when:
- Team size < 5 developers
- Application is simple
- You're still iterating on features
- Distributed system complexity outweighs benefits

---

**Architecture is Evolving! 🚀**

Start with monolith, split into microservices when needed.
