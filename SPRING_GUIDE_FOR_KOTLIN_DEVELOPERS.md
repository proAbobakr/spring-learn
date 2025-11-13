# Spring Framework Complete Guide for Kotlin Android Developers

## Table of Contents
1. [Introduction](#introduction)
2. [Spring Framework Fundamentals](#spring-framework-fundamentals)
3. [Dependency Injection Deep Dive](#dependency-injection-deep-dive)
4. [Spring Boot Essentials](#spring-boot-essentials)
5. [Major Spring Libraries](#major-spring-libraries)
6. [Microservices Architecture](#microservices-architecture)
7. [Distributed Systems](#distributed-systems)
8. [Redis Integration](#redis-integration)
9. [Kafka Integration](#kafka-integration)
10. [Real-World Architecture Examples](#real-world-architecture-examples)
11. [Local Deployment](#local-deployment)
12. [Docker Deployment](#docker-deployment)

---

## Introduction

### Welcome Kotlin/Android Developers! 🚀

If you're coming from Kotlin and Android development, you'll find many familiar concepts in Spring:
- **Dependency Injection**: Similar to Dagger/Hilt in Android
- **Annotations**: Like Android's `@Override`, `@Inject`
- **Lifecycle**: Similar to Activity/Fragment lifecycle
- **Asynchronous Operations**: Like Coroutines, but with CompletableFuture
- **REST APIs**: Like Retrofit, but you're building the server side!

### Key Differences: Kotlin/Android vs Spring/Java

| Concept | Android (Kotlin) | Spring (Java) |
|---------|------------------|---------------|
| DI Framework | Dagger/Hilt | Spring Core |
| HTTP Client | Retrofit | RestTemplate/WebClient |
| Database | Room | JPA/Hibernate |
| Async | Coroutines | CompletableFuture/Reactive |
| UI Layer | Activities/Fragments | Controllers/REST endpoints |
| Configuration | XML/Kotlin DSL | application.properties/YAML |

---

## Spring Framework Fundamentals

### What is Spring?

Spring is a comprehensive framework for building enterprise Java applications. Think of it as the "Android SDK" for server-side development.

**Core Principles:**
1. **Inversion of Control (IoC)**: Framework manages object creation
2. **Aspect-Oriented Programming (AOP)**: Cross-cutting concerns
3. **Convention over Configuration**: Sensible defaults
4. **Loose Coupling**: Interfaces over implementations

### Spring vs Spring Boot

```
Spring Framework = Android SDK (low-level)
Spring Boot = Android Jetpack (high-level, opinionated)
```

- **Spring Framework**: Core container, requires manual configuration
- **Spring Boot**: Auto-configuration, embedded server, production-ready

---

## Dependency Injection Deep Dive

### Android DI (Dagger/Hilt) vs Spring DI

#### Android (Hilt) Example:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com")
            .build()
    }
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var retrofit: Retrofit
}
```

#### Spring (Java) Equivalent:
```java
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
public class ApiController {
    @Autowired
    private RestTemplate restTemplate;

    // Or use constructor injection (preferred)
    public ApiController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
```

### Spring DI Types

#### 1. Constructor Injection (RECOMMENDED)
```java
@Service
public class UserService {
    private final UserRepository repository;

    // @Autowired is optional for single constructor
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

#### 2. Field Injection
```java
@Service
public class UserService {
    @Autowired
    private UserRepository repository;
}
```

#### 3. Setter Injection
```java
@Service
public class UserService {
    private UserRepository repository;

    @Autowired
    public void setRepository(UserRepository repository) {
        this.repository = repository;
    }
}
```

### Bean Scopes (Like Android Component Scopes)

| Spring Scope | Android Equivalent | Description |
|--------------|-------------------|-------------|
| `@Singleton` | `@Singleton` | One instance per application |
| `@Prototype` | `@Reusable` | New instance each time |
| `@Request` | N/A | One per HTTP request |
| `@Session` | N/A | One per HTTP session |

---

## Spring Boot Essentials

### Project Structure

```
my-spring-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/app/
│   │   │       ├── Application.java          # Like MainActivity
│   │   │       ├── controller/               # REST endpoints (like Activities)
│   │   │       ├── service/                  # Business logic (like ViewModels)
│   │   │       ├── repository/               # Data access (like Room DAOs)
│   │   │       ├── model/                    # Entities (like data classes)
│   │   │       ├── dto/                      # Data Transfer Objects
│   │   │       └── config/                   # Configuration (like Hilt modules)
│   │   └── resources/
│   │       ├── application.properties        # Like build.gradle configs
│   │       └── static/                       # Static resources
│   └── test/
├── pom.xml                                   # Like build.gradle
└── Dockerfile
```

### Main Application Class

```java
@SpringBootApplication  // Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

This is like:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Hilt, etc.
    }
}
```

---

## Major Spring Libraries

### 1. Spring Data JPA (Like Room)

#### Android (Room):
```kotlin
@Entity
data class User(
    @PrimaryKey val id: Long,
    val name: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>
}
```

#### Spring (JPA):
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Getters and setters
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByName(String name);
    // Method name becomes SQL query automatically!
}
```

### 2. Spring Web (REST API)

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserDTO dto) {
        User user = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @RequestBody UserDTO dto
    ) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 3. Spring Security (Authentication/Authorization)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 4. Spring Validation

```java
public class UserDTO {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50)
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank
    private String email;

    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
             message = "Password must be at least 8 characters with uppercase, lowercase, and number")
    private String password;
}

@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody UserDTO dto) {
    // @Valid triggers validation
    return ResponseEntity.ok(userService.register(dto));
}
```

---

## Microservices Architecture

### Monolith vs Microservices

```
MONOLITH (Single Application)
┌─────────────────────────────────┐
│                                 │
│  ┌──────┐  ┌──────┐  ┌──────┐ │
│  │ User │  │ Order│  │ Pay  │ │
│  │Service  │Service  │Service│ │
│  └──────┘  └──────┘  └──────┘ │
│            ↓                    │
│      Single Database            │
└─────────────────────────────────┘

MICROSERVICES (Distributed)
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │ Order Service│  │ Pay Service  │
│   Port:8081  │  │   Port:8082  │  │   Port:8083  │
│      ↓       │  │      ↓       │  │      ↓       │
│   User DB    │  │   Order DB   │  │   Payment DB │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       └──────────────────┴──────────────────┘
              API Gateway (Port 8080)
```

### When to Use Microservices

| Use Case | Monolith | Microservices |
|----------|----------|---------------|
| Team Size | < 10 developers | > 10 developers |
| Scalability Needs | Moderate | High, different parts scale differently |
| Deployment Frequency | Monthly/Weekly | Daily/Multiple times per day |
| Technology Stack | Single stack | Different services need different tech |
| Example | Small e-commerce site | Netflix, Uber, Airbnb |

### Microservices Communication Patterns

#### 1. Synchronous (REST/HTTP)
```java
@Service
public class OrderService {
    private final RestTemplate restTemplate;

    public void createOrder(OrderDTO order) {
        // Call User Service
        User user = restTemplate.getForObject(
            "http://user-service/api/users/" + order.getUserId(),
            User.class
        );

        // Call Payment Service
        PaymentResponse payment = restTemplate.postForObject(
            "http://payment-service/api/payments",
            new PaymentRequest(order.getAmount()),
            PaymentResponse.class
        );
    }
}
```

#### 2. Asynchronous (Message Queue - Kafka)
```java
@Service
public class OrderService {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void createOrder(Order order) {
        // Save order
        orderRepository.save(order);

        // Publish event (don't wait for response)
        OrderEvent event = new OrderEvent(order.getId(), order.getUserId());
        kafkaTemplate.send("order-created", event);
    }
}

@Service
public class NotificationService {
    @KafkaListener(topics = "order-created")
    public void handleOrderCreated(OrderEvent event) {
        // Send notification asynchronously
        sendEmailNotification(event);
    }
}
```

---

## Distributed Systems

### CAP Theorem

```
        Consistency
            /\
           /  \
          /    \
         /  CA  \
        /_______ \
       /  \  /\  /\
      / CP \/__\/ AP\
     /______\  /____\
Partition     \/     Availability
Tolerance
```

- **CA**: Traditional relational databases (PostgreSQL, MySQL)
- **CP**: MongoDB, HBase, Redis
- **AP**: Cassandra, DynamoDB, Couchbase

### Common Distributed System Patterns

#### 1. API Gateway Pattern
```
Mobile App ──┐
             ├─→ API Gateway ──┬─→ User Service
Web App   ───┘    (Port 8080)  ├─→ Order Service
                                ├─→ Payment Service
                                └─→ Notification Service
```

#### 2. Circuit Breaker Pattern (Resilience)

```java
@Service
public class OrderService {

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
    public PaymentResponse processPayment(Payment payment) {
        return paymentServiceClient.process(payment);
    }

    // Fallback when payment service is down
    public PaymentResponse fallbackPayment(Payment payment, Exception e) {
        // Queue for later processing
        paymentQueue.add(payment);
        return new PaymentResponse("PENDING", "Payment queued for processing");
    }
}
```

#### 3. Saga Pattern (Distributed Transactions)

```java
// Choreography-based Saga with Kafka

@Service
public class OrderSaga {

    // Step 1: Create Order
    public void createOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        kafkaTemplate.send("order-created", new OrderCreatedEvent(order.getId()));
    }

    // Step 2: Reserve Inventory
    @KafkaListener(topics = "order-created")
    public void reserveInventory(OrderCreatedEvent event) {
        try {
            inventoryService.reserve(event.getOrderId());
            kafkaTemplate.send("inventory-reserved", event);
        } catch (Exception e) {
            kafkaTemplate.send("inventory-failed", new InventoryFailedEvent(event.getOrderId()));
        }
    }

    // Step 3: Process Payment
    @KafkaListener(topics = "inventory-reserved")
    public void processPayment(OrderCreatedEvent event) {
        try {
            paymentService.charge(event.getOrderId());
            kafkaTemplate.send("payment-completed", event);
        } catch (Exception e) {
            kafkaTemplate.send("payment-failed", event);
            // Compensating transaction
            inventoryService.release(event.getOrderId());
        }
    }

    // Compensating transactions for rollback
    @KafkaListener(topics = "payment-failed")
    public void rollbackOrder(OrderCreatedEvent event) {
        orderRepository.updateStatus(event.getOrderId(), "CANCELLED");
    }
}
```

---

## Redis Integration

### What is Redis?

Redis is an in-memory data store used for:
- **Caching**: Speed up database queries
- **Session Storage**: User sessions in distributed systems
- **Rate Limiting**: API throttling
- **Real-time Analytics**: Leaderboards, counters
- **Message Broker**: Pub/Sub messaging

### Redis in Spring Boot

#### 1. Setup Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

#### 2. Configuration
```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

#### 3. Caching Examples

```java
@Service
public class LocationService {

    // Cache the result for 1 hour
    @Cacheable(value = "locations", key = "#id")
    public Location findById(Long id) {
        // This will only hit DB if not in cache
        return locationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Location not found"));
    }

    // Update cache when data changes
    @CachePut(value = "locations", key = "#result.id")
    public Location update(Long id, LocationDTO dto) {
        Location location = findById(id);
        location.setName(dto.getName());
        return locationRepository.save(location);
    }

    // Remove from cache when deleted
    @CacheEvict(value = "locations", key = "#id")
    public void delete(Long id) {
        locationRepository.deleteById(id);
    }
}
```

#### 4. Manual Redis Operations

```java
@Service
public class SessionService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveUserSession(String token, User user) {
        // Save with 24 hour expiration
        redisTemplate.opsForValue().set(
            "session:" + token,
            user,
            24,
            TimeUnit.HOURS
        );
    }

    public User getUserSession(String token) {
        return (User) redisTemplate.opsForValue().get("session:" + token);
    }

    public void incrementPageViews(Long locationId) {
        redisTemplate.opsForValue().increment("views:location:" + locationId);
    }

    public Long getPageViews(Long locationId) {
        Object views = redisTemplate.opsForValue().get("views:location:" + locationId);
        return views != null ? Long.parseLong(views.toString()) : 0L;
    }
}
```

---

## Kafka Integration

### What is Apache Kafka?

Kafka is a distributed event streaming platform:
- **Message Queue**: Asynchronous communication between services
- **Event Sourcing**: Store all changes as events
- **Stream Processing**: Real-time data processing
- **Log Aggregation**: Centralized logging

### Kafka Concepts

```
Producer ──→ Topic (Partition 0) ──→ Consumer Group A
         ──→ Topic (Partition 1) ──→ Consumer Group B
         ──→ Topic (Partition 2)
```

- **Producer**: Sends messages
- **Consumer**: Receives messages
- **Topic**: Category/feed name
- **Partition**: Parallel processing
- **Consumer Group**: Load balancing

### Kafka in Spring Boot

#### 1. Setup Dependencies
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### 2. Configuration
```java
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

#### 3. Producer Example

```java
@Service
public class LocationEventProducer {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLocationCreated(Location location) {
        LocationCreatedEvent event = new LocationCreatedEvent(
            location.getId(),
            location.getName(),
            location.getLatitude(),
            location.getLongitude(),
            LocalDateTime.now()
        );

        kafkaTemplate.send("location-events", event);
    }

    public void publishRatingAdded(Rating rating) {
        RatingAddedEvent event = new RatingAddedEvent(
            rating.getId(),
            rating.getLocationId(),
            rating.getUserId(),
            rating.getScore()
        );

        kafkaTemplate.send("rating-events", event);
    }
}
```

#### 4. Consumer Example

```java
@Service
public class NotificationConsumer {

    @KafkaListener(topics = "location-events", groupId = "notification-service")
    public void handleLocationCreated(LocationCreatedEvent event) {
        // Send notifications to nearby users
        notificationService.notifyNearbyUsers(event);
    }

    @KafkaListener(topics = "rating-events", groupId = "analytics-service")
    public void handleRatingAdded(RatingAddedEvent event) {
        // Update analytics dashboard
        analyticsService.updateLocationStats(event.getLocationId());
    }
}
```

---

## Real-World Architecture Examples

### Example 1: Booking System (like Booking.com)

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (8080)                      │
│                    [Rate Limiting with Redis]                │
└───────┬─────────────────┬─────────────────┬─────────────────┘
        │                 │                 │
    ┌───▼────┐      ┌────▼─────┐     ┌─────▼────┐
    │ User   │      │ Property │     │ Booking  │
    │Service │      │ Service  │     │ Service  │
    │ 8081   │      │  8082    │     │  8083    │
    └───┬────┘      └────┬─────┘     └─────┬────┘
        │                │                 │
    ┌───▼────┐      ┌────▼─────┐     ┌─────▼────┐
    │User DB │      │Property  │     │Booking   │
    │        │      │   DB     │     │  DB      │
    └────────┘      └──────────┘     └──────────┘
                          │
                    ┌─────▼──────┐
                    │  Kafka     │
                    │  Events    │
                    └─────┬──────┘
                          │
              ┌───────────┼───────────┐
         ┌────▼────┐  ┌───▼────┐  ┌──▼─────┐
         │ Email   │  │ Payment│  │Analytics│
         │ Service │  │ Service│  │ Service │
         └─────────┘  └────────┘  └─────────┘
```

**When to use this architecture:**
- High traffic (millions of bookings)
- Different scaling needs (search vs booking)
- Multiple teams working independently
- Need for resilience (one service down doesn't affect others)

### Example 2: Airbnb-like Architecture

```
Mobile/Web App
      │
┌─────▼──────────────────────────────────────────────────┐
│           API Gateway + Load Balancer                   │
│         [Kong/Nginx + Redis Session Store]              │
└─────┬──────────┬─────────┬─────────┬────────┬──────────┘
      │          │         │         │        │
   ┌──▼──┐   ┌──▼──┐  ┌───▼──┐  ┌───▼──┐  ┌─▼───┐
   │User │   │List │  │Search│  │Book  │  │Pay  │
   │Svc  │   │Svc  │  │ Svc  │  │ Svc  │  │ Svc │
   └──┬──┘   └──┬──┘  └───┬──┘  └───┬──┘  └─┬───┘
      │         │         │         │        │
   ┌──▼──┐   ┌─▼───┐  ┌──▼────┐ ┌──▼──┐  ┌─▼────┐
   │User │   │List │  │Elastic│ │Book │  │Pay   │
   │ DB  │   │ DB  │  │Search │ │ DB  │  │ DB   │
   └─────┘   └──┬──┘  └───────┘ └─────┘  └──────┘
                │
           ┌────▼─────┐
           │  S3/CDN  │
           │  Images  │
           └──────────┘
                │
         ┌──────▼────────┐
         │ Image Resize  │
         │   Service     │
         └───────────────┘
```

**Technology Choices:**
- **ElasticSearch**: Fast location/property search with geo-queries
- **S3/CDN**: Store and serve property images globally
- **Redis**: Cache popular listings, user sessions, rate limiting
- **Kafka**: Event streaming for bookings, reviews, notifications
- **PostgreSQL**: ACID compliance for bookings and payments

### Example 3: Monolith (Good for Starting)

```
┌────────────────────────────────────────┐
│     Spring Boot Application (8080)     │
│                                        │
│  ┌──────────┐  ┌──────────┐          │
│  │Controller│  │Controller│          │
│  │  Layer   │  │  Layer   │          │
│  └─────┬────┘  └─────┬────┘          │
│        │             │                │
│  ┌─────▼────┐  ┌─────▼────┐          │
│  │ Service  │  │ Service  │          │
│  │  Layer   │  │  Layer   │          │
│  └─────┬────┘  └─────┬────┘          │
│        │             │                │
│  ┌─────▼─────────────▼────┐          │
│  │   Repository Layer      │          │
│  └─────────┬───────────────┘          │
└────────────┼────────────────────────────┘
             │
      ┌──────▼─────┐
      │ PostgreSQL │
      └────────────┘
```

**When to use:**
- MVP or small application
- Small team (< 5 developers)
- Limited traffic (< 1000 requests/second)
- Simple deployment requirements

**Migration Path to Microservices:**
1. Start with modular monolith (clear module boundaries)
2. Use interfaces between modules
3. Extract one service at a time when needed
4. Share database initially, then split databases

---

## Local Deployment

### Prerequisites
```bash
# Install Java 17
sudo apt-get update
sudo apt-get install openjdk-17-jdk

# Install Maven
sudo apt-get install maven

# Verify installations
java -version
mvn -version
```

### Running the Application

#### Step 1: Clone and Build
```bash
git clone <repository-url>
cd spring-learn
mvn clean install
```

#### Step 2: Configure Database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/locationdb
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

#### Step 3: Start PostgreSQL
```bash
# Using system PostgreSQL
sudo service postgresql start
sudo -u postgres psql
CREATE DATABASE locationdb;
\q
```

#### Step 4: Start Redis (for caching)
```bash
# Ubuntu/Debian
sudo apt-get install redis-server
sudo service redis-server start

# Verify
redis-cli ping
# Should return: PONG
```

#### Step 5: Start Kafka (optional, for events)
```bash
# Download Kafka
wget https://downloads.apache.org/kafka/3.6.0/kafka_2.13-3.6.0.tgz
tar -xzf kafka_2.13-3.6.0.tgz
cd kafka_2.13-3.6.0

# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties &

# Start Kafka
bin/kafka-server-start.sh config/server.properties &

# Create topics
bin/kafka-topics.sh --create --topic location-events --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic rating-events --bootstrap-server localhost:9092
```

#### Step 6: Run Spring Boot Application
```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using JAR
mvn clean package
java -jar target/location-service-0.0.1-SNAPSHOT.jar

# Option 3: With custom port
java -jar -Dserver.port=8081 target/location-service-0.0.1-SNAPSHOT.jar
```

#### Step 7: Test the API
```bash
# Health check
curl http://localhost:8080/actuator/health

# Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123"
  }'
```

---

## Docker Deployment

### Complete Docker Setup

#### 1. Dockerfile
```dockerfile
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. docker-compose.yml (Complete Stack)
```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: location-postgres
    environment:
      POSTGRES_DB: locationdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - location-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: location-redis
    ports:
      - "6379:6379"
    networks:
      - location-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  # Zookeeper (for Kafka)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: location-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - location-network

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: location-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - location-network

  # Spring Boot Application
  app:
    build: .
    container_name: location-service
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_started
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/locationdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      JWT_SECRET: mySecretKey123456789012345678901234567890
      FILE_UPLOAD_DIR: /app/uploads
    volumes:
      - app_uploads:/app/uploads
    networks:
      - location-network
    restart: unless-stopped

volumes:
  postgres_data:
  app_uploads:

networks:
  location-network:
    driver: bridge
```

#### 3. Deploy with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Check service status
docker-compose ps

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

#### 4. Production Deployment with Docker Swarm

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.yml location-stack

# Scale application
docker service scale location-stack_app=3

# View services
docker stack services location-stack

# Remove stack
docker stack rm location-stack
```

#### 5. Kubernetes Deployment (Advanced)

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: location-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: location-service
  template:
    metadata:
      labels:
        app: location-service
    spec:
      containers:
      - name: location-service
        image: location-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/locationdb
        - name: SPRING_REDIS_HOST
          value: redis
---
apiVersion: v1
kind: Service
metadata:
  name: location-service
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: location-service
```

---

## Performance Optimization Tips

### 1. Database Optimization

```java
// Use projections to fetch only needed fields
public interface LocationSummary {
    Long getId();
    String getName();
    Double getRating();
}

@Query("SELECT l.id as id, l.name as name, AVG(r.score) as rating " +
       "FROM Location l LEFT JOIN l.ratings r GROUP BY l.id, l.name")
List<LocationSummary> findAllSummaries();

// Use pagination
Page<Location> findAll(Pageable pageable);

// Use Pageable.of(page, size, Sort.by("createdAt").descending())
```

### 2. Caching Strategy

```java
// Cache frequently accessed data
@Cacheable("locations")
public List<Location> getAllLocations() {
    return locationRepository.findAll();
}

// Use TTL for time-sensitive data
@Cacheable(value = "trending", unless = "#result.size() == 0")
public List<Location> getTrendingLocations() {
    return locationRepository.findTrendingLast24Hours();
}
```

### 3. Async Processing

```java
@Async
public CompletableFuture<Void> sendNotification(User user, String message) {
    emailService.send(user.getEmail(), message);
    return CompletableFuture.completedFuture(null);
}
```

---

## Monitoring and Observability

### Spring Boot Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
management.endpoints.web.exposure.include=health,metrics,info,prometheus
management.endpoint.health.show-details=always
```

Access metrics:
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`

---

## Summary: From Android to Spring

| You Know (Android) | You'll Learn (Spring) | Why It Matters |
|-------------------|----------------------|----------------|
| Retrofit client | REST controller | You're building the API, not consuming it |
| Room database | JPA/Hibernate | Similar ORM concepts, different syntax |
| Dagger/Hilt DI | Spring DI | Same concept, Spring is more mature |
| Coroutines | CompletableFuture/Reactive | Async handling on server |
| LiveData/StateFlow | Server-Sent Events/WebSocket | Push updates to clients |
| WorkManager | @Scheduled tasks | Background job processing |
| SharedPreferences | Redis/application.properties | Configuration and caching |

---

## Next Steps

1. ✅ Read this guide thoroughly
2. ✅ Set up the sample application (see project structure)
3. ✅ Run locally and experiment with endpoints
4. ✅ Deploy with Docker
5. ✅ Study the code and modify features
6. ✅ Build your own microservice
7. ✅ Explore Spring Cloud for advanced microservices

**Happy Learning! 🚀**
