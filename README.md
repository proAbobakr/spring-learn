# Location Rating Service - Complete Full-Stack Application

A comprehensive, production-ready full-stack application featuring a Spring Boot backend and React frontend, demonstrating best practices for building modern web applications with RESTful APIs, microservices architecture, and interactive user interfaces.

**Backend:** Spring Boot + PostgreSQL + Redis + Kafka
**Frontend:** React + Vite + React Router

## 📚 What You'll Learn

This project is a complete full-stack learning resource that covers:

### Backend (Spring Boot)
- ✅ **Spring Boot & Spring Framework** fundamentals
- ✅ **Dependency Injection** (similar to Dagger/Hilt)
- ✅ **REST API** development
- ✅ **JWT Authentication** (token-based auth)
- ✅ **JPA/Hibernate** ORM
- ✅ **Redis Caching** (distributed caching)
- ✅ **Apache Kafka** (event streaming)
- ✅ **Microservices Architecture** patterns
- ✅ **PostgreSQL** database
- ✅ **Geolocation** queries (maps integration)

### Frontend (React)
- ✅ **React 18** with modern hooks
- ✅ **React Router 6** for SPA routing
- ✅ **Vite** for fast development
- ✅ **Context API** for state management
- ✅ **Axios** for API integration
- ✅ **JWT Authentication** flow
- ✅ **Responsive Design** with CSS
- ✅ **Image Upload** with drag & drop
- ✅ **Real-time Updates** and notifications

### DevOps
- ✅ **Docker & Docker Compose** deployment
- ✅ **CORS Configuration**
- ✅ **Environment Management**

## 🚀 Quick Start

### Prerequisites

**Backend:**
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 15
- Redis (optional, for caching)
- Kafka (optional, for event streaming)

**Frontend:**
- Node.js 16+ and npm/yarn

**Deployment:**
- Docker & Docker Compose (recommended)

### Option 1: Run with Docker (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd spring-learn

# Start backend services with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f app

# Install frontend dependencies
cd frontend
npm install

# Start frontend development server
npm run dev

# Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Option 2: Run Locally

**Backend:**
```bash
# 1. Start PostgreSQL
sudo service postgresql start
sudo -u postgres psql
CREATE DATABASE locationdb;
\q

# 2. Start Redis (optional)
sudo service redis-server start

# 3. Start Kafka (optional)
# Download and extract Kafka, then:
bin/zookeeper-server-start.sh config/zookeeper.properties &
bin/kafka-server-start.sh config/server.properties &

# 4. Run the Spring Boot application
mvn clean install
mvn spring-boot:run
# API will be available at http://localhost:8080
```

**Frontend:**
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
# App will be available at http://localhost:3000
```

## 📖 Features

### Core Features

1. **User Management**
   - User registration with validation
   - JWT-based authentication
   - Role-based access control (USER, ADMIN, MODERATOR)
   - User profiles

2. **Location Management**
   - Create, read, update, delete locations
   - Geospatial queries (find nearby locations)
   - Category-based filtering
   - City/country-based search
   - Trending locations (most viewed)
   - Top-rated locations

3. **Rating System**
   - Rate locations (1-5 stars)
   - Add written reviews
   - View rating distribution
   - One rating per user per location
   - Automatic average calculation

4. **Comment System**
   - Add comments to locations
   - Nested replies support
   - Edit and delete comments
   - Like comments
   - Pagination support

5. **Image Management**
   - Upload multiple images per location
   - Set primary/featured image
   - Image captions
   - Support for JPEG, PNG, GIF, WebP
   - File size validation

### Technical Features

- **Redis Caching**: Locations, trending data, nearby searches cached
- **Kafka Events**: Asynchronous event streaming for locations, ratings, comments
- **Geospatial Queries**: Haversine formula for distance calculations
- **API Documentation**: Swagger/OpenAPI 3.0 with interactive UI
- **Monitoring**: Spring Boot Actuator with health checks and metrics
- **Exception Handling**: Global exception handler with proper error responses
- **Validation**: Bean Validation (JSR-303) for request DTOs
- **Security**: JWT authentication, CORS configuration, password encryption

## 📁 Project Structure

```
spring-learn/
├── src/                                         # Backend (Spring Boot)
│   ├── main/
│   │   ├── java/com/locationapp/service/
│   │   │   ├── LocationServiceApplication.java
│   │   │   ├── config/                          # Configuration
│   │   │   ├── controller/                      # REST endpoints
│   │   │   ├── service/                         # Business logic
│   │   │   ├── repository/                      # Data access
│   │   │   ├── model/                           # Entities
│   │   │   ├── dto/                             # Data Transfer Objects
│   │   │   ├── security/                        # JWT & Security
│   │   │   ├── event/                           # Kafka events
│   │   │   └── exception/                       # Exception handling
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── frontend/                                    # Frontend (React)
│   ├── public/
│   ├── src/
│   │   ├── components/                          # Reusable components
│   │   │   ├── Comment/
│   │   │   ├── Image/
│   │   │   ├── Layout/
│   │   │   ├── Location/
│   │   │   └── Rating/
│   │   ├── context/                             # React Context
│   │   ├── hooks/                               # Custom hooks
│   │   ├── pages/                               # Page components
│   │   ├── services/                            # API services
│   │   ├── utils/                               # Utilities
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   ├── vite.config.js
│   └── README.md                                # Frontend documentation
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md                                    # This file
```

See detailed structure in [frontend/README.md](frontend/README.md)

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Locations
- `GET /api/locations` - Get all locations (paginated)
- `GET /api/locations/{id}` - Get location by ID
- `POST /api/locations` - Create location (auth required)
- `PUT /api/locations/{id}` - Update location (auth required)
- `DELETE /api/locations/{id}` - Delete location (auth required)
- `GET /api/locations/search?keyword=` - Search locations
- `GET /api/locations/nearby?lat=&lng=&radius=` - Find nearby
- `GET /api/locations/top-rated` - Top-rated locations
- `GET /api/locations/trending` - Trending locations
- `GET /api/locations/by-city?city=` - Filter by city
- `GET /api/locations/by-category?category=` - Filter by category

### Ratings
- `POST /api/ratings` - Add/update rating (auth required)
- `GET /api/ratings/{id}` - Get rating
- `GET /api/ratings/location/{id}` - Get location ratings
- `DELETE /api/ratings/{id}` - Delete rating (auth required)

### Comments
- `POST /api/comments` - Add comment/reply (auth required)
- `GET /api/comments/{id}` - Get comment
- `GET /api/comments/location/{id}` - Get location comments
- `GET /api/comments/{id}/replies` - Get comment replies
- `PUT /api/comments/{id}` - Update comment (auth required)
- `DELETE /api/comments/{id}` - Delete comment (auth required)
- `POST /api/comments/{id}/like` - Like comment (auth required)

### Images
- `POST /api/images/upload` - Upload image (auth required)
- `GET /api/images/{id}` - Get image metadata
- `GET /api/images/location/{id}` - Get location images
- `GET /api/images/view/{filename}` - View image file
- `PUT /api/images/{id}` - Update image (auth required)
- `DELETE /api/images/{id}` - Delete image (auth required)

**Full API documentation with examples**: See [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

## 🧪 Testing the API

### Using Swagger UI

1. Start the application
2. Navigate to: http://localhost:8080/swagger-ui.html
3. Use the interactive UI to test endpoints

### Using cURL

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "Password123",
    "fullName": "John Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "Password123"
  }'

# Save the token from the response
TOKEN="your-jwt-token-here"

# Create a location
curl -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Central Park",
    "description": "Beautiful park in NYC",
    "latitude": 40.785091,
    "longitude": -73.968285,
    "address": "Central Park",
    "city": "New York",
    "country": "USA",
    "category": "PARK"
  }'

# Find nearby locations
curl "http://localhost:8080/api/locations/nearby?latitude=40.785091&longitude=-73.968285&radiusKm=5"

# Get trending locations
curl http://localhost:8080/api/locations/trending?limit=10
```

### Using Postman

Import the API collection from Swagger: http://localhost:8080/v3/api-docs

## 🏗️ Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────┐
│           Controller Layer                  │  ← REST endpoints
├─────────────────────────────────────────────┤
│           Service Layer                     │  ← Business logic
├─────────────────────────────────────────────┤
│           Repository Layer                  │  ← Data access
├─────────────────────────────────────────────┤
│           Database Layer (PostgreSQL)       │
└─────────────────────────────────────────────┘

        ↕ (Events)                    ↕ (Cache)

┌──────────────┐              ┌──────────────┐
│    Kafka     │              │    Redis     │
└──────────────┘              └──────────────┘
```

### Technology Stack

| Layer | Technology | Android Equivalent |
|-------|-----------|-------------------|
| Framework | Spring Boot | Android SDK |
| DI | Spring IoC | Dagger/Hilt |
| Database | JPA/Hibernate | Room |
| HTTP Client | RestTemplate | Retrofit |
| Cache | Redis | SharedPreferences/DataStore |
| Messaging | Kafka | WorkManager/Broadcast |
| Auth | JWT | OAuth/Token Storage |
| Async | CompletableFuture | Coroutines |

**Detailed architecture**: See [ARCHITECTURE.md](ARCHITECTURE.md)

## 📚 Documentation

### Frontend
- **[frontend/README.md](frontend/README.md)** - Complete React frontend guide:
  - Setup and installation
  - Component structure
  - API integration
  - State management
  - Styling approach

### Backend
- **[SPRING_GUIDE_FOR_KOTLIN_DEVELOPERS.md](SPRING_GUIDE_FOR_KOTLIN_DEVELOPERS.md)** - Spring Framework guide
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - Complete API reference
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Deployment instructions
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Architecture overview

## 🔐 Security

- **Password Encryption**: BCrypt hashing
- **JWT Tokens**: Secure token-based authentication
- **CORS**: Configurable cross-origin resource sharing
- **Input Validation**: Bean Validation (JSR-303)
- **SQL Injection**: Protected by JPA/Hibernate
- **CSRF**: Disabled for stateless REST API

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics (Prometheus format)
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### Application Info
```bash
curl http://localhost:8080/actuator/info
```

## 🐛 Troubleshooting

### Application won't start

```bash
# Check if ports are available
lsof -i :8080  # Spring Boot
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka

# Check Docker containers
docker-compose ps
docker-compose logs app
```

### Database connection errors

```bash
# Verify PostgreSQL is running
sudo service postgresql status

# Check database exists
sudo -u postgres psql -l

# Reset database
sudo -u postgres psql
DROP DATABASE IF EXISTS locationdb;
CREATE DATABASE locationdb;
```

### Redis connection errors

```bash
# Start Redis
sudo service redis-server start

# Test connection
redis-cli ping

# Check if Redis is running in Docker
docker-compose ps redis
docker-compose logs redis
```

## 🚢 Deployment

### Docker Deployment (Production)

```bash
# Build and start all services
docker-compose up -d

# Scale application instances
docker-compose up -d --scale app=3

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Clean up volumes
docker-compose down -v
```

### Manual Deployment

See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for detailed instructions.

## 🤝 Contributing

This is a learning project. Feel free to:
- Fork and experiment
- Submit issues for bugs
- Propose improvements
- Add more features

## 📝 License

This project is open source and available for learning purposes.

## 🙏 Acknowledgments

Built as a comprehensive learning resource for Android developers transitioning to backend development with Spring Boot.

---

## 📞 Support

For questions or issues:
1. Check the detailed guides in this repository
2. Review the Swagger API documentation
3. Examine the code comments (heavily documented)
4. Test with the provided Docker setup

**Happy Learning! 🚀**
