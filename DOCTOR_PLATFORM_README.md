# Doctor Social Platform - Comprehensive Medical Collaboration System

## 🏥 Overview

A production-ready, scalable microservices-based social platform designed specifically for medical professionals. This platform enables doctors worldwide to share medical cases, collaborate on diagnoses, access research papers, conduct video consultations, and leverage AI-powered medical image analysis.

### Key Features

- **👨‍⚕️ Doctor Profiles & Authentication**: Secure JWT-based authentication with detailed professional profiles
- **📋 Medical Case Sharing**: Share case studies, research, discussions with rich media support
- **🖼️ Media Management**: Upload and manage medical images, papers, and documents
- **💬 Comments & Discussions**: Threaded discussions on medical cases
- **📞 Video/Voice Calls**: WebRTC-based real-time communication
- **🤖 AI Analysis**: Machine learning-powered medical image and case analysis
- **🔔 Real-time Notifications**: WebSocket-based instant updates
- **👥 Social Features**: Follow doctors, like posts, trending content
- **🔍 Advanced Search**: Find cases by specialty, symptoms, diagnosis
- **📊 Analytics**: Track engagement, popular cases, and user metrics

## 🏗️ Architecture

### Microservices Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway (8080)                    │
│         (Load Balancing, Rate Limiting, Auth Filter)        │
└──────────────┬──────────────────────────────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    │  Service Discovery  │
    │   (Eureka - 8761)   │
    │                     │
    └──────────┬──────────┘
               │
    ┏━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
    ┃                                                   ┃
┌───▼────┐  ┌────────┐  ┌──────┐  ┌─────────┐  ┌──────────┐
│  User  │  │  Post  │  │Media │  │ Comment │  │   Call   │
│Service │  │Service │  │Service│  │ Service │  │ Service  │
│ :8081  │  │ :8082  │  │:8083 │  │  :8084  │  │  :8085   │
└────┬───┘  └───┬────┘  └──┬───┘  └────┬────┘  └─────┬────┘
     │          │           │           │             │
┌────▼────┐  ┌─▼────┐  ┌───▼───┐  ┌───▼───┐    ┌────▼────┐
│   AI    │  │Notif │  │ Redis │  │ Kafka │    │WebSocket│
│Service  │  │Service│  │Cluster│  │       │    │         │
│ :8086   │  │:8087 │  │       │  │       │    │         │
└─────────┘  └──────┘  └───────┘  └───────┘    └─────────┘
                            │           │
                    ┌───────▼───────────▼───────┐
                    │   PostgreSQL Cluster      │
                    │  (Separate DB per service)│
                    └──────────────────────────┘
```

### Technology Stack

#### Backend
- **Java 17** - Latest LTS version
- **Spring Boot 3.2.0** - Core framework
- **Spring Cloud 2023.0.0** - Microservices ecosystem
  - Eureka - Service Discovery
  - Gateway - API Gateway with WebFlux
  - OpenFeign - Declarative REST clients
- **PostgreSQL 15** - Primary database (one per service)
- **Redis 7** - Caching and rate limiting
- **Apache Kafka** - Event streaming and async messaging

#### Infrastructure
- **Docker** - Containerization
- **Kubernetes** - Orchestration and scaling
- **Prometheus** - Metrics collection
- **Grafana** - Monitoring dashboards

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Kubernetes (for production)
- 8GB+ RAM recommended

### Local Development Setup

#### 1. Clone the Repository
```bash
git clone https://github.com/your-org/doctor-social-platform.git
cd doctor-social-platform
```

#### 2. Start Infrastructure Services
```bash
docker-compose -f docker-compose-platform.yml up -d postgres-users postgres-posts redis-master kafka zookeeper
```

#### 3. Build All Services
```bash
mvn clean install
```

#### 4. Start Service Discovery
```bash
cd service-discovery
mvn spring-boot:run
```

#### 5. Start API Gateway
```bash
cd api-gateway
mvn spring-boot:run
```

#### 6. Start Individual Services
```bash
# Terminal 1 - User Service
cd user-service && mvn spring-boot:run

# Terminal 2 - Post Service
cd post-service && mvn spring-boot:run

# Add other services as needed
```

### Using Docker Compose (Recommended)

```bash
# Build all services
mvn clean package

# Start entire platform
docker-compose -f docker-compose-platform.yml up --build
```

**Access Points:**
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090

## 📚 API Documentation

### Authentication

#### Register Doctor
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "doctor@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "specialty": "Cardiology",
  "licenseNumber": "LIC123456",
  "institution": "General Hospital",
  "country": "USA",
  "city": "New York",
  "bio": "Experienced cardiologist...",
  "yearsOfExperience": 10
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "doctor": {
      "id": 1,
      "email": "doctor@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "specialty": "Cardiology",
      "verified": false
    }
  }
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "doctor@example.com",
  "password": "SecurePass123!"
}
```

### Posts (Medical Cases)

#### Create Post
```http
POST /api/posts
Authorization: Bearer {token}
Content-Type: application/json

{
  "doctorId": 1,
  "title": "Complex Cardiac Arrhythmia Case",
  "content": "Patient presented with irregular heartbeat...",
  "postType": "CASE_STUDY",
  "specialty": "Cardiology",
  "tags": ["arrhythmia", "ECG", "cardiac"],
  "mediaUrls": ["https://storage/ecg-image-1.jpg"],
  "paperUrls": ["https://storage/research-paper.pdf"],
  "patientAge": "45",
  "patientGender": "Male",
  "diagnosis": "Atrial Fibrillation",
  "treatment": "Rate control with beta-blockers",
  "outcome": "Stable rhythm achieved"
}
```

#### Get Trending Posts
```http
GET /api/posts/trending?page=0&size=20
Authorization: Bearer {token}
```

#### Search Posts
```http
GET /api/posts/search?query=cardiac&page=0&size=20
Authorization: Bearer {token}
```

### Users

#### Get Doctor Profile
```http
GET /api/users/{id}
Authorization: Bearer {token}
```

#### Follow Doctor
```http
POST /api/users/{followerId}/follow/{followingId}
Authorization: Bearer {token}
```

#### Search Doctors
```http
GET /api/users/search?query=cardiology&page=0&size=20
Authorization: Bearer {token}
```

## 🔐 Security

### Authentication & Authorization
- JWT-based stateless authentication
- Token expiration: 24 hours (configurable)
- Password encryption: BCrypt
- Role-based access control (RBAC)

### API Gateway Security
- Rate limiting (Redis-backed)
- Request validation
- CORS configuration
- Circuit breaker pattern

### Best Practices Implemented
- Input validation
- SQL injection prevention (JPA/Hibernate)
- XSS protection
- HTTPS only in production
- Secrets management (Kubernetes secrets)

## 📊 Scalability Features

### Database Sharding & Replication
Each microservice has its own database for:
- Data isolation
- Independent scaling
- Fault tolerance

**Recommended Setup:**
- Master-slave replication for read scaling
- Sharding by doctor ID for write scaling
- Connection pooling (HikariCP)

### Caching Strategy
- **L1 Cache**: Application-level (Spring Cache)
- **L2 Cache**: Distributed Redis cache
- **TTL**: Configurable per entity type
- **Cache Invalidation**: Event-driven via Kafka

### Horizontal Scaling
- **Kubernetes HPA**: Auto-scaling based on CPU/Memory
- **Min Replicas**: 3 per service
- **Max Replicas**: 20 per service
- **Target CPU**: 70%

### Load Balancing
- API Gateway: Round-robin with health checks
- Database: Read replicas for read-heavy operations
- Redis: Sentinel for high availability

## 🤖 AI Capabilities

### Medical Image Analysis
- Automatic analysis of uploaded medical images
- Detection of anomalies
- Similarity matching with existing cases
- Integration ready for TensorFlow/PyTorch models

### Case Analysis
- NLP-based symptom extraction
- Diagnosis suggestion
- Treatment recommendation
- Medical literature search

### Implementation
The AI Service is designed as a separate microservice that:
- Consumes Kafka events when media is uploaded
- Processes images using ML models
- Stores analysis results
- Triggers notifications

**Note**: ML models need to be integrated separately based on your specific requirements.

## 📞 Real-time Features

### WebRTC Video/Voice Calls
- P2P connection establishment
- Signaling server via WebSocket
- STUN/TURN server support
- Recording capability (optional)

### Real-time Notifications
- WebSocket connections
- Server-Sent Events (SSE)
- Event types:
  - New followers
  - Post likes/comments
  - Call invitations
  - AI analysis complete

## 🐳 Deployment

### Docker Deployment

#### Build Images
```bash
# Build all services
./build-all-images.sh
```

#### Deploy with Docker Compose
```bash
docker-compose -f docker-compose-platform.yml up -d
```

### Kubernetes Deployment

#### Prerequisites
- kubectl configured
- Kubernetes cluster running
- Docker images pushed to registry

#### Deploy
```bash
# Create namespace
kubectl apply -f kubernetes/namespace.yml

# Create secrets
kubectl apply -f kubernetes/secrets/

# Create config maps
kubectl apply -f kubernetes/configmaps/

# Deploy databases
kubectl apply -f kubernetes/deployments/postgres-statefulset.yml
kubectl apply -f kubernetes/deployments/redis-statefulset.yml

# Deploy services
kubectl apply -f kubernetes/deployments/user-service-deployment.yml
kubectl apply -f kubernetes/deployments/api-gateway-deployment.yml

# Verify deployment
kubectl get pods -n doctor-platform
kubectl get svc -n doctor-platform
```

### Production Considerations

#### Database
- Use managed PostgreSQL (AWS RDS, Google Cloud SQL)
- Enable automatic backups
- Set up read replicas
- Monitor query performance

#### Redis
- Use managed Redis (AWS ElastiCache, Azure Cache)
- Enable clustering
- Set up replication
- Configure eviction policies

#### Kafka
- Use managed Kafka (AWS MSK, Confluent Cloud)
- Set appropriate retention policies
- Monitor consumer lag
- Set up proper topic partitioning

#### Monitoring
- Set up Prometheus alerts
- Configure Grafana dashboards
- Enable application logging (ELK stack)
- Set up distributed tracing (Jaeger/Zipkin)

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Load Testing
Use tools like Apache JMeter or Gatling:
- Test API Gateway throughput
- Test database connection pooling
- Test Redis cache performance
- Test Kafka message throughput

**Target Performance:**
- API Gateway: 10,000+ req/sec
- Database: Sub-100ms query time
- Cache hit ratio: >80%
- P99 latency: <500ms

## 📈 Monitoring & Observability

### Metrics
- **Application Metrics**: Micrometer + Prometheus
- **JVM Metrics**: Memory, GC, threads
- **HTTP Metrics**: Request rate, latency, errors
- **Database Metrics**: Connection pool, query time
- **Cache Metrics**: Hit rate, evictions

### Logs
- **Format**: JSON structured logging
- **Levels**: ERROR, WARN, INFO, DEBUG
- **Aggregation**: ELK Stack or Splunk
- **Correlation**: Request ID tracing

### Alerts
Set up alerts for:
- High error rates (>1%)
- High latency (P99 >1s)
- Low cache hit rate (<70%)
- Database connection pool exhaustion
- High memory usage (>80%)
- Pod restarts

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For issues and questions:
- Create an issue on GitHub
- Email: support@doctorplatform.com
- Documentation: https://docs.doctorplatform.com

## 🗺️ Roadmap

### Phase 1 (Current)
- ✅ Core microservices architecture
- ✅ User management and authentication
- ✅ Post creation and management
- ✅ Basic search and discovery

### Phase 2 (Next)
- ⬜ Enhanced AI capabilities
- ⬜ Video/voice calling
- ⬜ Mobile app support
- ⬜ Advanced analytics

### Phase 3 (Future)
- ⬜ Telemedicine features
- ⬜ Prescription management
- ⬜ Insurance integration
- ⬜ Multi-language support

---

**Built with ❤️ for the medical community**
