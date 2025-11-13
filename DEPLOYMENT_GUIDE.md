# Deployment Guide

Complete guide for deploying the Location Rating Service in various environments.

## Table of Contents

1. [Local Development Setup](#local-development-setup)
2. [Docker Deployment](#docker-deployment)
3. [Production Deployment](#production-deployment)
4. [Environment Configuration](#environment-configuration)
5. [Database Migration](#database-migration)
6. [Monitoring & Logging](#monitoring--logging)
7. [Performance Tuning](#performance-tuning)
8. [Troubleshooting](#troubleshooting)

---

## Local Development Setup

### Prerequisites

Install the following software:

```bash
# Java 17
sudo apt-get update
sudo apt-get install openjdk-17-jdk

# Maven
sudo apt-get install maven

# PostgreSQL
sudo apt-get install postgresql postgresql-contrib

# Redis (optional)
sudo apt-get install redis-server

# Verify installations
java -version   # Should show Java 17
mvn -version    # Should show Maven 3.6+
psql --version  # Should show PostgreSQL 15
redis-cli --version
```

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd spring-learn
```

### Step 2: Database Setup

```bash
# Start PostgreSQL
sudo service postgresql start

# Create database
sudo -u postgres psql
CREATE DATABASE locationdb;
CREATE USER appuser WITH ENCRYPTED PASSWORD 'apppassword';
GRANT ALL PRIVILEGES ON DATABASE locationdb TO appuser;
\q

# Verify connection
psql -U appuser -d locationdb -h localhost
```

### Step 3: Configure Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/locationdb
spring.datasource.username=appuser
spring.datasource.password=apppassword

# Optional: Disable Redis/Kafka for local development
spring.redis.host=localhost  # Comment out if not using Redis
spring.kafka.bootstrap-servers=localhost:9092  # Comment out if not using Kafka
```

### Step 4: Build and Run

```bash
# Build project
mvn clean install

# Run tests
mvn test

# Run application
mvn spring-boot:run

# Or run as JAR
mvn clean package
java -jar target/location-service-0.0.1-SNAPSHOT.jar
```

### Step 5: Verify Deployment

```bash
# Check health
curl http://localhost:8080/actuator/health

# Test API
curl http://localhost:8080/api/public/health

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Docker Deployment

### Quick Start with Docker Compose

The easiest way to deploy the complete stack:

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Check status
docker-compose ps

# Stop all services
docker-compose down
```

### What Docker Compose Includes

- **PostgreSQL**: Database server
- **Redis**: Caching layer
- **Zookeeper**: Kafka dependency
- **Kafka**: Event streaming
- **Spring Boot App**: Your application

### Manual Docker Build

```bash
# Build image
docker build -t location-service:latest .

# Run container (requires database)
docker run -d \
  --name location-service \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/locationdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres123 \
  location-service:latest
```

### Docker Compose Configuration

View `docker-compose.yml` for full configuration. Key features:

- **Health Checks**: Automatic service health monitoring
- **Depends On**: Proper startup order
- **Volumes**: Data persistence
- **Networks**: Isolated network for services
- **Environment Variables**: Configuration injection

### Scaling with Docker Compose

```bash
# Scale application instances
docker-compose up -d --scale app=3

# Add load balancer (nginx)
# See nginx.conf example below
```

### Docker Useful Commands

```bash
# View logs
docker-compose logs -f app
docker-compose logs -f postgres

# Execute commands in container
docker-compose exec app bash
docker-compose exec postgres psql -U postgres

# Restart service
docker-compose restart app

# Remove volumes (clean slate)
docker-compose down -v

# Rebuild image
docker-compose up -d --build
```

---

## Production Deployment

### AWS Deployment (EC2)

#### Step 1: Launch EC2 Instance

```bash
# Instance type: t3.medium or larger
# OS: Ubuntu 22.04 LTS
# Security group: Open ports 8080, 80, 443, 5432 (internal only)
```

#### Step 2: Install Dependencies

```bash
# Connect to EC2
ssh -i your-key.pem ubuntu@your-ec2-ip

# Update system
sudo apt-get update
sudo apt-get upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

#### Step 3: Deploy Application

```bash
# Clone repository
git clone <repository-url>
cd spring-learn

# Create production env file
cat > .env << EOF
SPRING_DATASOURCE_PASSWORD=<strong-password>
JWT_SECRET=<generate-strong-secret>
REDIS_PASSWORD=<redis-password>
EOF

# Start services
docker-compose -f docker-compose.yml up -d

# Setup SSL (Let's Encrypt)
sudo apt-get install certbot
sudo certbot certonly --standalone -d yourdomain.com
```

#### Step 4: Configure Nginx Reverse Proxy

```nginx
# /etc/nginx/sites-available/location-service
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Enable site
sudo ln -s /etc/nginx/sites-available/location-service /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Kubernetes Deployment

#### deployment.yaml

```yaml
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
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
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

Deploy:

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl get pods
kubectl get services
```

---

## Environment Configuration

### Environment Variables

For production, use environment variables instead of application.properties:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/locationdb
export SPRING_DATASOURCE_USERNAME=dbuser
export SPRING_DATASOURCE_PASSWORD=<secret>

# Redis
export SPRING_DATA_REDIS_HOST=redis-host
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATA_REDIS_PASSWORD=<secret>

# Kafka
export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# JWT
export JWT_SECRET=<generate-strong-random-secret>
export JWT_EXPIRATION=86400000

# File Upload
export FILE_UPLOAD_DIR=/opt/app/uploads
export FILE_UPLOAD_MAX_SIZE=10485760

# Logging
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_COM_LOCATIONAPP_SERVICE=INFO
```

### Production application.properties

```properties
# Profile-specific configuration
spring.profiles.active=production

# Database - use environment variables
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# Connection pooling
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# JPA
spring.jpa.hibernate.ddl-auto=validate  # Never use 'update' in production
spring.jpa.show-sql=false

# Redis
spring.data.redis.host=${SPRING_DATA_REDIS_HOST}
spring.data.redis.port=${SPRING_DATA_REDIS_PORT}
spring.data.redis.password=${SPRING_DATA_REDIS_PASSWORD}

# Actuator - limit exposure in production
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=when-authorized

# Logging
logging.level.root=INFO
logging.file.name=/var/log/location-service/app.log
```

---

## Database Migration

### Using Flyway for Database Migrations

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Create migration files in `src/main/resources/db/migration/`:

**V1__Initial_Schema.sql**:
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_location_coords ON locations(latitude, longitude);
```

**V2__Add_Ratings.sql**:
```sql
CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    location_id BIGINT REFERENCES locations(id),
    score DOUBLE PRECISION NOT NULL CHECK (score >= 1.0 AND score <= 5.0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, location_id)
);
```

Configure in application.properties:

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

---

## Monitoring & Logging

### Application Metrics

Access metrics endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### Prometheus + Grafana Setup

**docker-compose.monitoring.yml**:

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  grafana_data:
```

**prometheus.yml**:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

Start monitoring stack:

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

Access:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

### Centralized Logging with ELK Stack

Configure Logback for JSON logging (`src/main/resources/logback-spring.xml`):

```xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON" />
    </root>
</configuration>
```

---

## Performance Tuning

### JVM Settings

```bash
java -jar \
  -Xms512m \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  app.jar
```

### Database Optimization

```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_location_city ON locations(city);
CREATE INDEX idx_location_category ON locations(category);
CREATE INDEX idx_rating_location ON ratings(location_id);
CREATE INDEX idx_comment_location ON comments(location_id);

-- Analyze query performance
EXPLAIN ANALYZE SELECT * FROM locations WHERE city = 'New York';
```

### Redis Caching Strategy

```properties
# Cache TTL configuration
spring.cache.redis.time-to-live=3600000  # 1 hour

# Configure specific cache TTLs in RedisConfig.java
# - locations: 30 minutes
# - trending: 5 minutes
# - nearby: 10 minutes
```

### Connection Pooling

```properties
# HikariCP settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check port availability
lsof -i :8080
netstat -tulpn | grep 8080

# Kill process on port 8080
sudo kill -9 $(lsof -t -i:8080)

# Check logs
tail -f /var/log/location-service/app.log
docker-compose logs -f app
```

### Database Connection Errors

```bash
# Test database connectivity
psql -h localhost -U postgres -d locationdb

# Check PostgreSQL is running
sudo service postgresql status
docker-compose ps postgres

# Check connection string
echo $SPRING_DATASOURCE_URL

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

### Out of Memory Errors

```bash
# Increase JVM heap size
export JAVA_OPTS="-Xms1g -Xmx4g"
java $JAVA_OPTS -jar app.jar

# Monitor memory usage
docker stats
```

### Slow API Responses

```bash
# Check database query performance
# Enable query logging in application.properties:
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG

# Monitor with Actuator
curl http://localhost:8080/actuator/metrics/http.server.requests

# Check Redis cache hit rate
redis-cli INFO stats
```

### Kafka Connection Issues

```bash
# Check Kafka is running
docker-compose ps kafka

# List topics
docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Check consumer lag
docker-compose exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group location-service-group
```

---

## Backup and Recovery

### Database Backup

```bash
# Backup PostgreSQL
docker-compose exec postgres pg_dump -U postgres locationdb > backup_$(date +%Y%m%d).sql

# Restore
docker-compose exec -T postgres psql -U postgres locationdb < backup_20240115.sql

# Automated daily backups (cron)
0 2 * * * /path/to/backup-script.sh
```

### Application Data Backup

```bash
# Backup uploads directory
tar -czf uploads-backup-$(date +%Y%m%d).tar.gz uploads/

# Backup Docker volumes
docker run --rm -v location_postgres_data:/data -v $(pwd):/backup ubuntu tar czf /backup/postgres-backup.tar.gz /data
```

---

## Security Checklist

- [ ] Change default passwords
- [ ] Use strong JWT secret (min 512 bits)
- [ ] Enable HTTPS/TLS
- [ ] Configure firewall rules
- [ ] Limit actuator endpoint exposure
- [ ] Enable SQL injection protection (JPA provides this)
- [ ] Implement rate limiting
- [ ] Regular security updates
- [ ] Monitor access logs
- [ ] Backup regularly

---

## Continuous Deployment

### GitHub Actions Example

`.github/workflows/deploy.yml`:

```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: docker build -t location-service:${{ github.sha }} .

      - name: Push to registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push location-service:${{ github.sha }}

      - name: Deploy to production
        run: |
          ssh ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }} "cd /opt/app && docker-compose pull && docker-compose up -d"
```

---

**Deployment Complete! 🚀**
