# Troubleshooting Guide - Common Issues and Solutions

Complete troubleshooting guide for the Doctor Social Platform across all environments.

## Table of Contents

1. [Build & Compilation Issues](#build--compilation-issues)
2. [Runtime Issues](#runtime-issues)
3. [Database Issues](#database-issues)
4. [Network & Connectivity Issues](#network--connectivity-issues)
5. [Docker Issues](#docker-issues)
6. [Kubernetes Issues](#kubernetes-issues)
7. [Performance Issues](#performance-issues)
8. [Security Issues](#security-issues)

---

## Build & Compilation Issues

### Issue: "package does not exist" errors

**Symptoms:**
```
[ERROR] cannot find symbol
[ERROR] package com.medical.common.dto does not exist
```

**Cause:** Common library not installed in local Maven repository

**Solution:**
```bash
# Build parent POM and common library first
mvn clean install -N  # Parent POM only

cd common-library
mvn clean install

# Then build other services
cd ../user-service
mvn clean install
```

---

### Issue: Maven dependency download fails

**Symptoms:**
```
Could not transfer artifact...from/to central
Connection refused
```

**Solutions:**

**1. Check internet connection**
```bash
ping repo.maven.apache.org
```

**2. Clear Maven cache**
```bash
rm -rf ~/.m2/repository
mvn clean install
```

**3. Use different Maven mirror**
```xml
<!-- Add to ~/.m2/settings.xml -->
<mirrors>
  <mirror>
    <id>aliyun-central</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/central</url>
  </mirror>
</mirrors>
```

---

### Issue: OutOfMemoryError during build

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m -Xms512m"

# Or add to ~/.mavenrc
echo 'MAVEN_OPTS="-Xmx2048m -Xms512m"' >> ~/.mavenrc

# Rebuild
mvn clean install
```

---

## Runtime Issues

### Issue: Port already in use

**Symptoms:**
```
Web server failed to start. Port 8081 was already in use.
```

**Solutions:**

**Find and kill process (macOS/Linux):**
```bash
# Find process
lsof -i :8081

# Kill process
kill -9 <PID>

# Or one-liner
lsof -ti:8081 | xargs kill -9
```

**Find and kill process (Windows):**
```cmd
# Find process
netstat -ano | findstr :8081

# Kill process
taskkill /PID <PID> /F
```

**Use different port:**
```bash
# Run with custom port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8091
```

---

### Issue: Service not registering with Eureka

**Symptoms:**
- Service runs but doesn't appear in Eureka dashboard
- Other services can't find this service

**Diagnosis:**
```bash
# Check Eureka is accessible
curl http://localhost:8761/actuator/health

# Check service logs
tail -f logs/user-service.log | grep -i eureka

# Check registered services
curl http://localhost:8761/eureka/apps
```

**Solutions:**

**1. Verify Eureka configuration**
```yaml
# In application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

**2. Check network connectivity**
```bash
# From service container, test Eureka connectivity
curl http://service-discovery:8761/actuator/health
```

**3. Increase timeout**
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

**4. Restart services in order**
```bash
# Stop all services
# Start Eureka first
cd service-discovery && mvn spring-boot:run

# Wait 30 seconds

# Start other services
cd user-service && mvn spring-boot:run
```

---

### Issue: JWT authentication fails

**Symptoms:**
```
401 Unauthorized
Invalid JWT token
```

**Diagnosis:**
```bash
# Test token generation
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Decode JWT (online: jwt.io)
echo "eyJhbGci..." | base64 -d
```

**Solutions:**

**1. Verify JWT secret matches across services**
```bash
# Check all application.yml files
grep -r "jwt.secret" */src/main/resources/

# All should have same value:
jwt:
  secret: mySecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmSecurityPurposes
```

**2. Check token expiration**
```yaml
jwt:
  expiration: 86400000  # 24 hours in milliseconds
```

**3. Verify Authorization header format**
```bash
# Correct format
curl http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer eyJhbGci..."

# NOT: "Bearer: eyJhbGci..."
# NOT: "eyJhbGci..." (missing Bearer)
```

---

## Database Issues

### Issue: Cannot connect to PostgreSQL

**Symptoms:**
```
Connection to localhost:5432 refused
Could not open JDBC Connection
```

**Diagnosis:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Or for native installation
systemctl status postgresql  # Linux
brew services list | grep postgres  # macOS

# Test connection
psql -h localhost -p 5432 -U postgres -d doctor_users
```

**Solutions:**

**1. Start PostgreSQL**
```bash
# Docker
docker-compose -f docker-compose-platform.yml up -d postgres-users

# Native Linux
sudo systemctl start postgresql

# Native macOS
brew services start postgresql@15
```

**2. Check connection parameters**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/doctor_users
    username: postgres
    password: postgres
```

**3. Check PostgreSQL is listening**
```bash
# Edit postgresql.conf
listen_addresses = '*'  # or 'localhost' or specific IP

# Edit pg_hba.conf
# Add line:
host    all             all             127.0.0.1/32            md5

# Restart PostgreSQL
sudo systemctl restart postgresql
```

**4. Check firewall**
```bash
# Linux (allow PostgreSQL port)
sudo ufw allow 5432/tcp

# Check if port is open
netstat -tulpn | grep 5432
```

---

### Issue: Database exists but tables are not created

**Symptoms:**
```
Table "doctors" doesn't exist
```

**Solutions:**

**1. Enable Hibernate auto-create**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # or 'create' for fresh start
```

**2. Manually create tables**
```sql
-- Connect to database
psql -h localhost -U postgres -d doctor_users

-- Run DDL from migration scripts
\i user-service/src/main/resources/db/migration/V1__Initial_Schema.sql
```

**3. Check Flyway migration status**
```bash
# View migration history
psql -h localhost -U postgres -d doctor_users \
  -c "SELECT * FROM flyway_schema_history;"

# Reset Flyway (DANGER: Development only!)
psql -h localhost -U postgres -d doctor_users \
  -c "DROP TABLE flyway_schema_history;"
```

---

### Issue: Redis connection refused

**Symptoms:**
```
Unable to connect to Redis
Connection refused: localhost/127.0.0.1:6379
```

**Solutions:**

**1. Start Redis**
```bash
# Docker
docker-compose -f docker-compose-platform.yml up -d redis-master

# Native
redis-server
```

**2. Check Redis is running**
```bash
# Docker
docker logs doctor-redis-master

# Native
redis-cli ping
# Should return: PONG
```

**3. Verify password**
```bash
# Test with password
redis-cli -a redispassword ping

# In application.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: redispassword  # if configured
```

---

### Issue: Kafka connection failed

**Symptoms:**
```
Error connecting to node kafka:29092
Connection to node -1 could not be established
```

**Solutions:**

**1. Start Kafka and Zookeeper**
```bash
# Start Zookeeper first
docker-compose -f docker-compose-platform.yml up -d zookeeper

# Wait 10 seconds
sleep 10

# Start Kafka
docker-compose -f docker-compose-platform.yml up -d kafka

# Wait for Kafka to be ready
sleep 30
```

**2. Check Kafka is running**
```bash
# Check logs
docker logs doctor-kafka

# List topics (tests connectivity)
docker exec -it doctor-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

**3. Verify bootstrap servers**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # For host
    # OR
    bootstrap-servers: kafka:29092     # For Docker network
```

---

## Network & Connectivity Issues

### Issue: Services can't communicate in Docker

**Symptoms:**
```
UnknownHostException: postgres-users
Connection refused to user-service
```

**Solutions:**

**1. Verify all services are on same network**
```bash
# List networks
docker network ls

# Inspect network
docker network inspect spring-learn_doctor-network

# Should show all containers
```

**2. Use service names, not localhost**
```yaml
# ✓ Correct (Docker)
spring:
  datasource:
    url: jdbc:postgresql://postgres-users:5432/doctor_users
  redis:
    host: redis-master

# ✗ Wrong (Docker)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/doctor_users
```

**3. Reconnect container to network**
```bash
docker network connect spring-learn_doctor-network doctor-user-service
```

---

### Issue: API Gateway routing not working

**Symptoms:**
```
404 Not Found
No route found for /api/users/1
```

**Diagnosis:**
```bash
# Check Gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Check service registration
curl http://localhost:8761/eureka/apps
```

**Solutions:**

**1. Verify route configuration**
```yaml
# In api-gateway/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service  # Must match service name in Eureka
          predicates:
            - Path=/api/users/**
```

**2. Check service is registered**
```bash
# Service name in Eureka must match uri
# user-service → lb://user-service
```

**3. Restart Gateway**
```bash
docker restart doctor-api-gateway
```

---

## Docker Issues

### Issue: Docker build fails

**Symptoms:**
```
Cannot connect to Docker daemon
Error response from daemon
```

**Solutions:**

**1. Start Docker**
```bash
# Check Docker is running
docker info

# Start Docker
# macOS/Windows: Start Docker Desktop
# Linux:
sudo systemctl start docker
```

**2. Check disk space**
```bash
df -h

# Clean up Docker
docker system prune -a

# Remove unused volumes
docker volume prune
```

**3. Fix permissions (Linux)**
```bash
sudo usermod -aG docker $USER
newgrp docker
```

---

### Issue: Container keeps restarting

**Symptoms:**
```
docker ps shows STATUS: Restarting (1) X minutes ago
```

**Diagnosis:**
```bash
# Check logs
docker logs doctor-user-service --tail 100

# Check restart count
docker inspect doctor-user-service | grep RestartCount
```

**Solutions:**

**1. Common causes:**
- Application crashes on startup
- Health check fails
- Out of memory

**2. Disable health check temporarily**
```yaml
# In docker-compose.yml
services:
  user-service:
    # Comment out healthcheck
    # healthcheck:
    #   test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
```

**3. Increase memory**
```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          memory: 2G
```

---

### Issue: Volume permission denied

**Symptoms:**
```
Permission denied: '/var/lib/postgresql/data'
```

**Solutions:**

**1. Fix volume permissions**
```bash
# Find volume location
docker volume inspect spring-learn_postgres-users-data

# Fix permissions
sudo chown -R 999:999 /var/lib/docker/volumes/spring-learn_postgres-users-data/_data
```

**2. Recreate volume**
```bash
# DANGER: Deletes data!
docker-compose down -v
docker-compose up -d
```

---

## Kubernetes Issues

### Issue: Pods in CrashLoopBackOff

**Symptoms:**
```
NAME                  READY   STATUS             RESTARTS
user-service-abc123   0/1     CrashLoopBackOff   5
```

**Diagnosis:**
```bash
# Check pod logs
kubectl logs user-service-abc123 -n doctor-platform

# Check previous logs (if crashed)
kubectl logs user-service-abc123 -n doctor-platform --previous

# Describe pod for events
kubectl describe pod user-service-abc123 -n doctor-platform
```

**Common Causes & Solutions:**

**1. Image pull failure**
```bash
# Check events
kubectl describe pod user-service-abc123 -n doctor-platform | grep -A 5 Events

# Solution: Verify image exists
docker images | grep user-service

# Push to registry
docker tag doctor-platform/user-service:latest yourusername/user-service:1.0.0
docker push yourusername/user-service:1.0.0
```

**2. Insufficient resources**
```bash
# Check node resources
kubectl top nodes

# Solution: Reduce resource requests or add nodes
```

**3. Application error**
```bash
# Check logs for stack trace
kubectl logs user-service-abc123 -n doctor-platform --tail=50

# Common fixes:
# - Database not ready
# - Missing environment variables
# - Configuration errors
```

---

### Issue: Service not accessible

**Symptoms:**
```
curl: (7) Failed to connect to api-gateway
Connection refused
```

**Solutions:**

**1. Check service exists**
```bash
kubectl get svc -n doctor-platform
```

**2. Check selector matches pods**
```bash
# Get service selector
kubectl get svc api-gateway -n doctor-platform -o yaml | grep -A 3 selector

# Check if pods have matching labels
kubectl get pods -n doctor-platform --show-labels
```

**3. Use port-forward for testing**
```bash
kubectl port-forward svc/api-gateway 8080:80 -n doctor-platform
curl http://localhost:8080/actuator/health
```

---

### Issue: PersistentVolumeClaim pending

**Symptoms:**
```
NAME                  STATUS    VOLUME
postgres-users-pvc    Pending
```

**Solutions:**

**1. Check if StorageClass exists**
```bash
kubectl get storageclass

# Create if missing (example for local)
kubectl apply -f - <<EOF
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: standard
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
EOF
```

**2. Check PVC events**
```bash
kubectl describe pvc postgres-users-pvc -n doctor-platform
```

---

## Performance Issues

### Issue: High memory usage

**Diagnosis:**
```bash
# Check Java heap usage
docker exec doctor-user-service jmap -heap 1

# Check container stats
docker stats doctor-user-service
```

**Solutions:**

**1. Tune JVM parameters**
```dockerfile
# In Dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+UseG1GC", \
    "-jar", "app.jar"]
```

**2. Set memory limits**
```yaml
# In docker-compose.yml
deploy:
  resources:
    limits:
      memory: 1G
    reservations:
      memory: 512M
```

---

### Issue: Slow response times

**Diagnosis:**
```bash
# Check endpoint latency
time curl http://localhost:8080/api/users/1

# Check database slow queries
docker exec -it doctor-postgres-users psql -U postgres -d doctor_users \
  -c "SELECT query, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"

# Check Redis hit rate
docker exec -it doctor-redis-master redis-cli INFO stats | grep keyspace
```

**Solutions:**

**1. Enable caching**
```java
@Cacheable(value = "users", key = "#id")
public User findById(Long id) {
    return userRepository.findById(id);
}
```

**2. Add database indexes**
```sql
CREATE INDEX idx_doctors_email ON doctors(email);
CREATE INDEX idx_posts_created_at ON posts(created_at);
```

**3. Optimize queries**
```java
// Use JOIN FETCH to avoid N+1 queries
@Query("SELECT d FROM Doctor d LEFT JOIN FETCH d.posts WHERE d.id = :id")
Doctor findByIdWithPosts(@Param("id") Long id);
```

---

## Security Issues

### Issue: Secrets exposed in logs

**Problem:** Passwords/tokens visible in application logs

**Solution:**

**1. Use environment variables**
```yaml
# Don't:
spring:
  datasource:
    password: postgres

# Do:
spring:
  datasource:
    password: ${DB_PASSWORD}
```

**2. Configure logging**
```xml
<!-- logback-spring.xml -->
<configuration>
    <property name="LOG_PATTERN" value="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <!-- Mask sensitive data -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>
</configuration>
```

---

## Getting Help

### Collect debugging information

```bash
cat > collect-debug-info.sh << 'EOF'
#!/bin/bash

echo "=== System Information ===" > debug-info.txt
uname -a >> debug-info.txt
echo "" >> debug-info.txt

echo "=== Docker Version ===" >> debug-info.txt
docker --version >> debug-info.txt
docker-compose --version >> debug-info.txt
echo "" >> debug-info.txt

echo "=== Running Containers ===" >> debug-info.txt
docker ps -a >> debug-info.txt
echo "" >> debug-info.txt

echo "=== Container Logs ===" >> debug-info.txt
docker-compose -f docker-compose-platform.yml logs --tail=100 >> debug-info.txt
echo "" >> debug-info.txt

echo "=== Network Information ===" >> debug-info.txt
docker network ls >> debug-info.txt
echo "" >> debug-info.txt

echo "Debug information collected in debug-info.txt"
EOF

chmod +x collect-debug-info.sh
./collect-debug-info.sh
```

### Community Resources

- **GitHub Issues**: https://github.com/proAbobakr/spring-learn/issues
- **Stack Overflow**: Tag with `spring-boot`, `microservices`, `docker`
- **Spring Community**: https://spring.io/community

---

**Still having issues? Create an issue with:**
1. Error message (full stack trace)
2. Steps to reproduce
3. Environment (OS, Java version, Docker version)
4. Relevant configuration files
5. Output of diagnostic commands above
