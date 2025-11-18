# Docker Setup Guide - Containerization from Zero

Complete guide for containerizing and running the Doctor Social Platform with Docker.

## Table of Contents

1. [Docker Basics](#docker-basics)
2. [Building Docker Images](#building-docker-images)
3. [Docker Compose Setup](#docker-compose-setup)
4. [Running the Platform](#running-the-platform)
5. [Docker Networking](#docker-networking)
6. [Volume Management](#volume-management)
7. [Production Best Practices](#production-best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Docker Basics

### What is Docker?

Docker packages your application and its dependencies into containers - lightweight, portable units that run consistently across different environments.

**Benefits:**
- **Consistency**: Same environment in dev, staging, and production
- **Isolation**: Each service runs independently
- **Portability**: Run anywhere Docker is installed
- **Scalability**: Easy to scale services up/down

### Key Concepts

```
┌─────────────────────────────────────┐
│         Docker Image                │  ← Blueprint (like a class)
│  (Immutable template with app code, │
│   dependencies, and configuration)  │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│      Docker Container               │  ← Running instance
│  (Running instance of an image)     │     (like an object)
│  - Has its own filesystem           │
│  - Has its own network              │
│  - Isolated from other containers   │
└─────────────────────────────────────┘
```

---

## Building Docker Images

### Step 1: Create Dockerfiles

#### Service Discovery Dockerfile

```bash
# Create Dockerfile
cat > service-discovery/Dockerfile << 'EOF'
# Multi-stage build for smaller image size
FROM maven:3.8-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy parent POM and common library first (for caching)
COPY pom.xml .
COPY common-library common-library/

# Copy service POM and source
COPY service-discovery/pom.xml service-discovery/
COPY service-discovery/src service-discovery/src/

# Build the application
RUN mvn -f service-discovery/pom.xml clean package -DskipTests

# Production image
FROM openjdk:17-jdk-slim

# Add metadata
LABEL maintainer="doctor-platform@example.com"
LABEL version="1.0.0"
LABEL description="Service Discovery (Eureka Server)"

# Create app user (security best practice)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/service-discovery/target/service-discovery-*.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
EOF
```

#### API Gateway Dockerfile

```bash
cat > api-gateway/Dockerfile << 'EOF'
FROM maven:3.8-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY common-library common-library/
COPY api-gateway/pom.xml api-gateway/
COPY api-gateway/src api-gateway/src/
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

FROM openjdk:17-jdk-slim
LABEL service="api-gateway"
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
EOF
```

#### User Service Dockerfile

```bash
cat > user-service/Dockerfile << 'EOF'
FROM maven:3.8-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY common-library common-library/
COPY user-service/pom.xml user-service/
COPY user-service/src user-service/src/
RUN mvn -f user-service/pom.xml clean package -DskipTests

FROM openjdk:17-jdk-slim
LABEL service="user-service"
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=build /app/user-service/target/user-service-*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
EOF
```

#### Post Service Dockerfile

```bash
cat > post-service/Dockerfile << 'EOF'
FROM maven:3.8-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY common-library common-library/
COPY post-service/pom.xml post-service/
COPY post-service/src post-service/src/
RUN mvn -f post-service/pom.xml clean package -DskipTests

FROM openjdk:17-jdk-slim
LABEL service="post-service"
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=build /app/post-service/target/post-service-*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
EOF
```

### Step 2: Build Images

```bash
# Build all images from project root
cd spring-learn

# Build Service Discovery
docker build -t doctor-platform/service-discovery:1.0.0 \
  -f service-discovery/Dockerfile .

# Build API Gateway
docker build -t doctor-platform/api-gateway:1.0.0 \
  -f api-gateway/Dockerfile .

# Build User Service
docker build -t doctor-platform/user-service:1.0.0 \
  -f user-service/Dockerfile .

# Build Post Service
docker build -t doctor-platform/post-service:1.0.0 \
  -f post-service/Dockerfile .

# Verify images
docker images | grep doctor-platform

# Expected output:
# doctor-platform/service-discovery   1.0.0   abc123   2 min ago   350MB
# doctor-platform/api-gateway         1.0.0   def456   3 min ago   380MB
# doctor-platform/user-service        1.0.0   ghi789   4 min ago   390MB
# doctor-platform/post-service        1.0.0   jkl012   5 min ago   390MB
```

### Step 3: Build Script (Automated)

```bash
# Create build script
cat > build-all-images.sh << 'EOF'
#!/bin/bash

set -e  # Exit on error

echo "🏗️  Building all Docker images..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

VERSION=${1:-1.0.0}

echo -e "${BLUE}Version: ${VERSION}${NC}"

# Build images
services=("service-discovery" "api-gateway" "user-service" "post-service")

for service in "${services[@]}"; do
    echo -e "${BLUE}Building ${service}...${NC}"
    docker build \
        -t doctor-platform/${service}:${VERSION} \
        -t doctor-platform/${service}:latest \
        -f ${service}/Dockerfile \
        .
    echo -e "${GREEN}✓ ${service} built successfully${NC}"
done

echo -e "${GREEN}🎉 All images built successfully!${NC}"

# Show images
docker images | grep doctor-platform
EOF

chmod +x build-all-images.sh

# Build all
./build-all-images.sh 1.0.0
```

---

## Docker Compose Setup

### Understanding docker-compose-platform.yml

The provided `docker-compose-platform.yml` already defines the complete platform. Let's understand each section:

#### Infrastructure Services

```yaml
services:
  # PostgreSQL for User Service
  postgres-users:
    image: postgres:15-alpine
    container_name: doctor-postgres-users
    environment:
      POSTGRES_DB: doctor_users
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-users-data:/var/lib/postgresql/data
    networks:
      - doctor-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
```

**Key Points:**
- `image`: Base image from Docker Hub
- `container_name`: Custom name for easy reference
- `environment`: Configuration via env vars
- `ports`: Map host:container ports
- `volumes`: Persistent data storage
- `networks`: Custom network for service communication
- `healthcheck`: Verify service is ready

#### Application Services

```yaml
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    image: doctor-platform/user-service:latest
    container_name: doctor-user-service
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-users:5432/doctor_users
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis-master
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-discovery:8761/eureka/
    depends_on:
      - postgres-users
      - redis-master
      - kafka
      - service-discovery
    networks:
      - doctor-network
```

**Key Points:**
- `build`: Build from local Dockerfile
- `depends_on`: Start order (dependencies first)
- Service names are DNS names within the network

---

## Running the Platform

### Step 1: Start Infrastructure Only

```bash
# Navigate to project
cd spring-learn

# Start databases and messaging
docker-compose -f docker-compose-platform.yml up -d \
  postgres-users \
  postgres-posts \
  postgres-media \
  postgres-comments \
  redis-master \
  redis-replica-1 \
  zookeeper \
  kafka

# Wait for services to be ready
echo "Waiting for infrastructure to start..."
sleep 30

# Verify
docker-compose -f docker-compose-platform.yml ps
```

### Step 2: Start Service Discovery

```bash
# Start Eureka
docker-compose -f docker-compose-platform.yml up -d service-discovery

# Check logs
docker-compose -f docker-compose-platform.yml logs -f service-discovery

# Wait for "Started ServiceDiscoveryApplication"
# Press Ctrl+C to stop following logs

# Verify
curl http://localhost:8761/actuator/health
```

### Step 3: Start API Gateway

```bash
# Start Gateway
docker-compose -f docker-compose-platform.yml up -d api-gateway

# Check logs
docker logs -f doctor-api-gateway

# Verify registration in Eureka
curl http://localhost:8761/eureka/apps
```

### Step 4: Start Business Services

```bash
# Start all business services
docker-compose -f docker-compose-platform.yml up -d \
  user-service \
  post-service

# Check all services
docker-compose -f docker-compose-platform.yml ps

# Follow all logs
docker-compose -f docker-compose-platform.yml logs -f
```

### Step 5: Start Everything (Alternative)

```bash
# Start entire platform at once
docker-compose -f docker-compose-platform.yml up -d

# This starts services in dependency order automatically

# Check status
docker-compose -f docker-compose-platform.yml ps

# View logs
docker-compose -f docker-compose-platform.yml logs -f

# View specific service logs
docker-compose -f docker-compose-platform.yml logs -f user-service
```

### Step 6: Create Management Script

```bash
cat > docker-platform.sh << 'EOF'
#!/bin/bash

case "$1" in
  start)
    echo "🚀 Starting Doctor Platform..."
    docker-compose -f docker-compose-platform.yml up -d
    echo "✓ Platform started!"
    echo "Eureka: http://localhost:8761"
    echo "API Gateway: http://localhost:8080"
    ;;

  stop)
    echo "🛑 Stopping Doctor Platform..."
    docker-compose -f docker-compose-platform.yml stop
    echo "✓ Platform stopped!"
    ;;

  restart)
    echo "🔄 Restarting Doctor Platform..."
    docker-compose -f docker-compose-platform.yml restart
    echo "✓ Platform restarted!"
    ;;

  logs)
    service=${2:-""}
    if [ -z "$service" ]; then
      docker-compose -f docker-compose-platform.yml logs -f
    else
      docker-compose -f docker-compose-platform.yml logs -f $service
    fi
    ;;

  status)
    docker-compose -f docker-compose-platform.yml ps
    ;;

  clean)
    echo "🧹 Cleaning up..."
    docker-compose -f docker-compose-platform.yml down
    echo "✓ Containers removed!"
    ;;

  clean-all)
    echo "⚠️  WARNING: This will delete all data!"
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
      docker-compose -f docker-compose-platform.yml down -v
      echo "✓ Containers and volumes removed!"
    else
      echo "Cancelled."
    fi
    ;;

  rebuild)
    echo "🏗️  Rebuilding services..."
    docker-compose -f docker-compose-platform.yml build --no-cache
    docker-compose -f docker-compose-platform.yml up -d
    echo "✓ Rebuild complete!"
    ;;

  *)
    echo "Doctor Platform Management"
    echo ""
    echo "Usage: $0 {start|stop|restart|logs|status|clean|clean-all|rebuild}"
    echo ""
    echo "Commands:"
    echo "  start      - Start all services"
    echo "  stop       - Stop all services"
    echo "  restart    - Restart all services"
    echo "  logs       - View logs (add service name for specific service)"
    echo "  status     - Show service status"
    echo "  clean      - Stop and remove containers"
    echo "  clean-all  - Stop and remove containers AND volumes (deletes data!)"
    echo "  rebuild    - Rebuild images and restart"
    echo ""
    echo "Examples:"
    echo "  $0 start"
    echo "  $0 logs user-service"
    echo "  $0 status"
    exit 1
    ;;
esac
EOF

chmod +x docker-platform.sh

# Usage examples
./docker-platform.sh start
./docker-platform.sh status
./docker-platform.sh logs user-service
./docker-platform.sh stop
```

---

## Docker Networking

### Understanding the Network

```bash
# Inspect the network
docker network inspect spring-learn_doctor-network

# Shows all connected containers and their IPs
```

**Service DNS Resolution:**
```
Within the docker network:
- postgres-users      → docker-postgres-users
- redis-master        → docker-redis-master
- kafka               → doctor-kafka
- service-discovery   → doctor-service-discovery
- user-service        → doctor-user-service
```

**Example Connection:**
```yaml
# User service connects to PostgreSQL using service name
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-users:5432/doctor_users
# Docker's DNS resolves 'postgres-users' to the container's IP
```

### Testing Network Connectivity

```bash
# Access a container's shell
docker exec -it doctor-user-service sh

# Test DNS resolution
ping postgres-users
nslookup postgres-users

# Test PostgreSQL connection
apt-get update && apt-get install -y postgresql-client
psql -h postgres-users -U postgres -d doctor_users

# Test Redis connection
apt-get install -y redis-tools
redis-cli -h redis-master -a redispassword ping

# Exit container
exit
```

---

## Volume Management

### Understanding Volumes

Volumes persist data even when containers are removed.

```bash
# List volumes
docker volume ls | grep doctor

# Expected volumes:
# spring-learn_postgres-users-data
# spring-learn_postgres-posts-data
# spring-learn_redis-master-data
# spring-learn_kafka-data
# spring-learn_zookeeper-data

# Inspect a volume
docker volume inspect spring-learn_postgres-users-data

# Shows mount point on host:
# /var/lib/docker/volumes/spring-learn_postgres-users-data/_data
```

### Backup Volumes

```bash
# Backup PostgreSQL volume
docker run --rm \
  -v spring-learn_postgres-users-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/postgres-users-$(date +%Y%m%d).tar.gz /data

# Backup Redis volume
docker run --rm \
  -v spring-learn_redis-master-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/redis-master-$(date +%Y%m%d).tar.gz /data
```

### Restore Volumes

```bash
# Stop services first
docker-compose -f docker-compose-platform.yml stop postgres-users

# Restore volume
docker run --rm \
  -v spring-learn_postgres-users-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar xzf /backup/postgres-users-20240115.tar.gz -C /

# Restart service
docker-compose -f docker-compose-platform.yml start postgres-users
```

### Clean Unused Volumes

```bash
# WARNING: This removes ALL unused volumes
docker volume prune

# Remove specific volume (careful!)
docker volume rm spring-learn_postgres-users-data
```

---

## Production Best Practices

### 1. Use Specific Image Tags

```yaml
# Bad - uses latest (unpredictable)
image: postgres:latest

# Good - specific version
image: postgres:15.3-alpine
```

### 2. Set Resource Limits

```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. Use Secrets for Sensitive Data

```bash
# Create secrets
echo "SecurePassword123!" | docker secret create postgres_password -

# Use in compose (Swarm mode)
services:
  postgres-users:
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/postgres_password
    secrets:
      - postgres_password

secrets:
  postgres_password:
    external: true
```

### 4. Enable Logging

```yaml
services:
  user-service:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 5. Health Checks

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

### 6. Security Hardening

```dockerfile
# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Scan for vulnerabilities
RUN apk upgrade --no-cache

# Use minimal base images
FROM alpine:3.18
```

### 7. Multi-stage Builds

Already implemented in our Dockerfiles - keeps production images small:

```dockerfile
# Build stage - large image with Maven
FROM maven:3.8-openjdk-17-slim AS build
# ... build steps ...

# Production stage - minimal image
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/*.jar app.jar
```

---

## Troubleshooting

### Issue 1: Container Won't Start

```bash
# Check logs
docker logs doctor-user-service

# Common errors:
# - Port already in use
# - Can't connect to database
# - Environment variable missing
```

**Solution:**
```bash
# Check if port is available
netstat -tlnp | grep 8081

# Check environment variables
docker inspect doctor-user-service | grep -A 20 Env
```

### Issue 2: Container Keeps Restarting

```bash
# Check restart count
docker ps -a | grep user-service

# Check logs for crash reason
docker logs doctor-user-service --tail 100

# Common causes:
# - Out of memory
# - Application crash on startup
# - Failed health check
```

**Solution:**
```bash
# Increase memory limit
docker-compose -f docker-compose-platform.yml up -d \
  --scale user-service=0
docker run -m 2g doctor-platform/user-service:latest

# Disable health check temporarily
# Edit docker-compose.yml and comment out healthcheck
```

### Issue 3: Cannot Connect to Other Services

```bash
# Check network
docker network ls
docker network inspect spring-learn_doctor-network

# Verify service is in network
docker inspect doctor-user-service | grep NetworkMode
```

**Solution:**
```bash
# Reconnect to network
docker network connect spring-learn_doctor-network doctor-user-service

# Or recreate containers
docker-compose -f docker-compose-platform.yml up -d --force-recreate
```

### Issue 4: High Memory Usage

```bash
# Check resource usage
docker stats

# Shows real-time CPU, memory, network, I/O
```

**Solution:**
```bash
# Set memory limits
docker update --memory 1g doctor-user-service

# Or in docker-compose.yml
deploy:
  resources:
    limits:
      memory: 1G
```

### Issue 5: Slow Build Times

```bash
# Use BuildKit for parallel builds
export DOCKER_BUILDKIT=1

# Build with cache
docker build --cache-from doctor-platform/user-service:latest \
  -t doctor-platform/user-service:latest .

# Use .dockerignore
cat > .dockerignore << 'EOF'
target/
*.log
.git/
.idea/
*.md
EOF
```

### Issue 6: Database Connection Refused

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs doctor-postgres-users

# Test connection from host
psql -h localhost -p 5432 -U postgres -d doctor_users

# Test from within network
docker run --rm --network spring-learn_doctor-network \
  postgres:15-alpine \
  psql -h postgres-users -U postgres -d doctor_users
```

---

## Useful Commands Reference

### Container Management
```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# Start container
docker start doctor-user-service

# Stop container
docker stop doctor-user-service

# Restart container
docker restart doctor-user-service

# Remove container
docker rm doctor-user-service

# Remove all stopped containers
docker container prune
```

### Image Management
```bash
# List images
docker images

# Remove image
docker rmi doctor-platform/user-service:1.0.0

# Remove unused images
docker image prune

# Remove all unused images
docker image prune -a

# Save image to file
docker save doctor-platform/user-service:1.0.0 > user-service.tar

# Load image from file
docker load < user-service.tar
```

### Logs and Debugging
```bash
# View logs
docker logs doctor-user-service

# Follow logs
docker logs -f doctor-user-service

# Last 100 lines
docker logs --tail 100 doctor-user-service

# With timestamps
docker logs -t doctor-user-service

# Enter container shell
docker exec -it doctor-user-service sh

# Run command in container
docker exec doctor-user-service ls -la /app

# Copy files from container
docker cp doctor-user-service:/app/logs ./logs

# Copy files to container
docker cp ./config.yml doctor-user-service:/app/
```

### Docker Compose
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose stop

# Stop and remove
docker-compose down

# View logs
docker-compose logs -f

# Rebuild services
docker-compose build

# Scale service
docker-compose up -d --scale user-service=3

# Execute command
docker-compose exec user-service sh
```

---

## Next Steps

Docker setup complete! Continue to:
- [SETUP_04_KUBERNETES.md](./SETUP_04_KUBERNETES.md) - Kubernetes production deployment

**Your platform is now containerized and running! 🐳**
