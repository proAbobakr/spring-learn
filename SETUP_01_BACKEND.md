# Backend Setup Guide - From Zero to Running

This guide walks you through setting up the backend services from scratch, assuming you're starting with a clean machine.

## Table of Contents

1. [Prerequisites Installation](#prerequisites-installation)
2. [Project Setup](#project-setup)
3. [Building the Project](#building-the-project)
4. [Running Services Locally](#running-services-locally)
5. [Verifying the Setup](#verifying-the-setup)
6. [Common Issues](#common-issues)

---

## Prerequisites Installation

### Step 1: Install Java 17

#### Windows
1. Download Java 17 from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [AdoptiumTemurin](https://adoptium.net/)
2. Run the installer
3. Verify installation:
```cmd
java -version
```
Expected output:
```
openjdk version "17.0.x"
```

4. Set JAVA_HOME environment variable:
   - Right-click "This PC" → Properties → Advanced System Settings
   - Click "Environment Variables"
   - Add new System Variable:
     - Name: `JAVA_HOME`
     - Value: `C:\Program Files\Java\jdk-17` (adjust path as needed)
   - Edit PATH and add: `%JAVA_HOME%\bin`

#### macOS
```bash
# Using Homebrew
brew install openjdk@17

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version
```

#### Linux (Ubuntu/Debian)
```bash
# Install Java 17
sudo apt update
sudo apt install openjdk-17-jdk -y

# Verify
java -version

# Set JAVA_HOME (add to ~/.bashrc or ~/.zshrc)
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### Step 2: Install Maven

#### Windows
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH:
   - Add System Variable:
     - Name: `MAVEN_HOME`
     - Value: `C:\Program Files\Apache\maven`
   - Edit PATH and add: `%MAVEN_HOME%\bin`

4. Verify:
```cmd
mvn -version
```

#### macOS
```bash
# Using Homebrew
brew install maven

# Verify
mvn -version
```

#### Linux
```bash
# Install Maven
sudo apt install maven -y

# Verify
mvn -version
```

Expected output:
```
Apache Maven 3.8.x or higher
Java version: 17.0.x
```

### Step 3: Install Git

#### Windows
1. Download from [git-scm.com](https://git-scm.com/download/win)
2. Run installer (use default settings)
3. Verify:
```cmd
git --version
```

#### macOS
```bash
# Install via Homebrew
brew install git

# Verify
git --version
```

#### Linux
```bash
# Install Git
sudo apt install git -y

# Verify
git --version
```

### Step 4: Install IDE (Optional but Recommended)

**IntelliJ IDEA Community Edition** (Recommended)
- Download from [JetBrains](https://www.jetbrains.com/idea/download/)
- Install and configure Java SDK

**VS Code** (Alternative)
```bash
# Install VS Code
# Download from https://code.visualstudio.com/

# Install Java extensions
# - Extension Pack for Java
# - Spring Boot Extension Pack
```

---

## Project Setup

### Step 1: Clone the Repository

```bash
# Navigate to your workspace
cd ~/workspace  # macOS/Linux
cd C:\workspace  # Windows

# Clone the repository
git clone https://github.com/proAbobakr/spring-learn.git

# Navigate to project
cd spring-learn

# Checkout the feature branch
git checkout claude/add-doctor-social-platform-011Wa3eSs3mXFKGeVpBxgzH1
```

### Step 2: Understanding the Project Structure

```
spring-learn/
├── pom.xml                          # Parent POM (manages all modules)
├── common-library/                  # Shared utilities, DTOs, security
│   ├── pom.xml
│   └── src/main/java/com/medical/common/
│       ├── dto/                     # Data Transfer Objects
│       ├── exception/               # Exception handlers
│       └── security/                # JWT utilities
│
├── service-discovery/               # Eureka Server (Port 8761)
│   ├── pom.xml
│   └── src/main/java/com/medical/discovery/
│
├── api-gateway/                     # API Gateway (Port 8080)
│   ├── pom.xml
│   └── src/main/java/com/medical/gateway/
│       ├── filter/                  # Authentication filter
│       └── ApiGatewayApplication.java
│
├── user-service/                    # User/Doctor Service (Port 8081)
│   ├── pom.xml
│   └── src/main/java/com/medical/user/
│       ├── model/                   # JPA entities
│       ├── repository/              # Data access
│       ├── service/                 # Business logic
│       ├── controller/              # REST endpoints
│       └── dto/                     # Request/Response objects
│
├── post-service/                    # Post/Case Service (Port 8082)
│   ├── pom.xml
│   └── src/main/java/com/medical/post/
│
├── media-service/                   # Media Service (Port 8083)
├── comment-service/                 # Comment Service (Port 8084)
├── call-service/                    # WebRTC Call Service (Port 8085)
├── ai-service/                      # AI Analysis Service (Port 8086)
└── notification-service/            # Notification Service (Port 8087)
```

### Step 3: Configure Maven Settings (Optional)

Create or edit `~/.m2/settings.xml` for faster builds:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <mirrors>
        <!-- Use Maven Central mirror for faster downloads -->
        <mirror>
            <id>maven-central</id>
            <mirrorOf>central</mirrorOf>
            <url>https://repo.maven.apache.org/maven2</url>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>default</id>
            <properties>
                <downloadSources>true</downloadSources>
                <downloadJavadocs>false</downloadJavadocs>
            </properties>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>default</activeProfile>
    </activeProfiles>
</settings>
```

---

## Building the Project

### Step 1: Build All Modules

```bash
# Navigate to project root
cd spring-learn

# Clean and install all modules
mvn clean install

# This will:
# 1. Download all dependencies (~500MB first time)
# 2. Compile all Java code
# 3. Run tests (if any)
# 4. Package JARs
# 5. Install to local Maven repository
```

**Expected Output:**
```
[INFO] Reactor Summary:
[INFO]
[INFO] Doctor Social Platform ............................. SUCCESS
[INFO] Common Library ..................................... SUCCESS
[INFO] Service Discovery .................................. SUCCESS
[INFO] API Gateway ........................................ SUCCESS
[INFO] User Service ....................................... SUCCESS
[INFO] Post Service ....................................... SUCCESS
[INFO] Media Service ...................................... SUCCESS
[INFO] Comment Service .................................... SUCCESS
[INFO] Call Service ....................................... SUCCESS
[INFO] AI Service ......................................... SUCCESS
[INFO] Notification Service ............................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2:15 min
```

### Step 2: Build Individual Service (Optional)

```bash
# Build only user-service
cd user-service
mvn clean install

# Build with skipping tests (faster)
mvn clean install -DskipTests

# Build with verbose output
mvn clean install -X
```

### Step 3: Verify Build Artifacts

```bash
# Check generated JAR files
ls -lh service-discovery/target/*.jar
ls -lh api-gateway/target/*.jar
ls -lh user-service/target/*.jar
ls -lh post-service/target/*.jar

# Expected output:
# service-discovery-1.0.0-SNAPSHOT.jar  (~50MB)
# api-gateway-1.0.0-SNAPSHOT.jar        (~60MB)
# user-service-1.0.0-SNAPSHOT.jar       (~70MB)
# post-service-1.0.0-SNAPSHOT.jar       (~70MB)
```

---

## Running Services Locally

### Prerequisites: Start Infrastructure

Before running the services, you need databases and messaging. See [SETUP_02_DATABASE.md](./SETUP_02_DATABASE.md) first.

**Quick Start with Docker:**
```bash
# Start only infrastructure (PostgreSQL, Redis, Kafka)
docker-compose -f docker-compose-platform.yml up -d \
  postgres-users \
  postgres-posts \
  redis-master \
  zookeeper \
  kafka
```

### Running Order (Important!)

Services must start in this order due to dependencies:

1. **Service Discovery** (Eureka) - First!
2. **API Gateway** - After Eureka is up
3. **Business Services** - After Gateway is up

### Step 1: Start Service Discovery

```bash
# Terminal 1 - Service Discovery
cd service-discovery

# Run with Maven
mvn spring-boot:run

# OR run the JAR directly
java -jar target/service-discovery-1.0.0-SNAPSHOT.jar
```

**Wait for this message:**
```
Started ServiceDiscoveryApplication in 15.xxx seconds
```

**Verify:** Open http://localhost:8761 in browser
- You should see Eureka Dashboard
- "Instances currently registered with Eureka" will be empty initially

### Step 2: Start API Gateway

```bash
# Terminal 2 - API Gateway
cd api-gateway

# Run with Maven
mvn spring-boot:run

# Set custom port if needed
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

**Wait for this message:**
```
Started ApiGatewayApplication in 20.xxx seconds
```

**Verify:**
- Refresh http://localhost:8761
- You should see "API-GATEWAY" in registered instances
- Check gateway: http://localhost:8080/actuator/health
  ```json
  {"status":"UP"}
  ```

### Step 3: Start User Service

```bash
# Terminal 3 - User Service
cd user-service

# Run with Maven
mvn spring-boot:run
```

**Environment Variables (if needed):**
```bash
# Run with custom database
mvn spring-boot:run \
  -Dspring-boot.run.arguments="\
    --spring.datasource.url=jdbc:postgresql://localhost:5432/doctor_users \
    --spring.datasource.username=postgres \
    --spring.datasource.password=postgres"
```

**Wait for this message:**
```
Started UserServiceApplication in 25.xxx seconds
```

**Verify:**
- Check Eureka: http://localhost:8761 - Should show "USER-SERVICE"
- Test endpoint:
  ```bash
  curl http://localhost:8081/actuator/health
  ```

### Step 4: Start Post Service

```bash
# Terminal 4 - Post Service
cd post-service

mvn spring-boot:run
```

**Wait for this message:**
```
Started PostServiceApplication in 25.xxx seconds
```

### Step 5: Running Multiple Services (Alternative)

**Option A: Using Screen (Linux/macOS)**
```bash
# Install screen
sudo apt install screen  # Linux
brew install screen      # macOS

# Create script: start-all.sh
cat > start-all.sh << 'EOF'
#!/bin/bash

# Start Service Discovery
screen -dmS eureka bash -c "cd service-discovery && mvn spring-boot:run"
echo "Started Eureka (waiting 30s)..."
sleep 30

# Start API Gateway
screen -dmS gateway bash -c "cd api-gateway && mvn spring-boot:run"
echo "Started Gateway (waiting 20s)..."
sleep 20

# Start User Service
screen -dmS user bash -c "cd user-service && mvn spring-boot:run"
echo "Started User Service (waiting 20s)..."
sleep 20

# Start Post Service
screen -dmS post bash -c "cd post-service && mvn spring-boot:run"
echo "Started Post Service"

echo "All services started!"
echo "View logs: screen -r <service-name>"
echo "Services: eureka, gateway, user, post"
EOF

chmod +x start-all.sh
./start-all.sh

# View logs
screen -r eureka   # Press Ctrl+A then D to detach
screen -r gateway
screen -r user
screen -r post

# Stop all
screen -X -S eureka quit
screen -X -S gateway quit
screen -X -S user quit
screen -X -S post quit
```

**Option B: Using tmux**
```bash
# Install tmux
sudo apt install tmux  # Linux
brew install tmux      # macOS

# Create script: start-all-tmux.sh
cat > start-all-tmux.sh << 'EOF'
#!/bin/bash

# Create new tmux session
tmux new-session -d -s doctor-platform

# Split into 4 panes
tmux split-window -h
tmux split-window -v
tmux select-pane -t 0
tmux split-window -v

# Start services in each pane
tmux select-pane -t 0
tmux send-keys "cd service-discovery && mvn spring-boot:run" C-m

tmux select-pane -t 1
tmux send-keys "sleep 30 && cd api-gateway && mvn spring-boot:run" C-m

tmux select-pane -t 2
tmux send-keys "sleep 50 && cd user-service && mvn spring-boot:run" C-m

tmux select-pane -t 3
tmux send-keys "sleep 70 && cd post-service && mvn spring-boot:run" C-m

# Attach to session
tmux attach-session -t doctor-platform
EOF

chmod +x start-all-tmux.sh
./start-all-tmux.sh

# Detach: Ctrl+B then D
# Reattach: tmux attach -t doctor-platform
# Kill: tmux kill-session -t doctor-platform
```

**Option C: Using IntelliJ IDEA**
1. Open project in IntelliJ
2. Right-click on `ServiceDiscoveryApplication.java` → Run
3. Wait for it to start
4. Right-click on `ApiGatewayApplication.java` → Run
5. Right-click on `UserServiceApplication.java` → Run
6. Right-click on `PostServiceApplication.java` → Run

**Option D: Using VS Code**
1. Install "Spring Boot Extension Pack"
2. Open Command Palette (Ctrl+Shift+P)
3. Type "Spring Boot Dashboard"
4. Click on each application to start

---

## Verifying the Setup

### Step 1: Check Eureka Dashboard

Open http://localhost:8761

**Expected view:**
```
Instances currently registered with Eureka
Application         AMIs        Availability Zones      Status
API-GATEWAY         n/a (1)     (1)                     UP (1)
USER-SERVICE        n/a (1)     (1)                     UP (1)
POST-SERVICE        n/a (1)     (1)                     UP (1)
```

### Step 2: Test API Gateway

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected:
{"status":"UP"}
```

### Step 3: Test User Service Registration

```bash
# Register a new doctor
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "dr.john@hospital.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "specialty": "Cardiology",
    "licenseNumber": "LIC123456",
    "institution": "General Hospital",
    "country": "USA",
    "city": "New York",
    "yearsOfExperience": 10
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "doctor": {
      "id": 1,
      "email": "dr.john@hospital.com",
      "firstName": "John",
      "lastName": "Doe",
      "specialty": "Cardiology",
      "verified": false
    }
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### Step 4: Test Login

```bash
# Login with the created user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "dr.john@hospital.com",
    "password": "SecurePass123!"
  }'
```

### Step 5: Test Post Creation

```bash
# Save the token from registration/login
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Create a medical case post
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "doctorId": 1,
    "title": "Interesting Cardiac Case",
    "content": "Patient presented with atypical chest pain...",
    "postType": "CASE_STUDY",
    "specialty": "Cardiology",
    "tags": ["cardiology", "diagnosis", "case-study"],
    "patientAge": "55",
    "patientGender": "Male",
    "diagnosis": "Unstable Angina",
    "treatment": "PCI with stenting",
    "outcome": "Successful, patient stable"
  }'
```

### Step 6: Monitor Logs

**Service Discovery Logs:**
```
Registered instance USER-SERVICE/192.168.1.100:user-service:8081 with status UP
Registered instance POST-SERVICE/192.168.1.100:post-service:8082 with status UP
```

**API Gateway Logs:**
```
Mapped route [api/users/**] to [lb://user-service]
Mapped route [api/posts/**] to [lb://post-service]
```

**User Service Logs:**
```
DiscoveryClient_USER-SERVICE - registration status: 204
Hibernate: create table doctors...
Kafka producer started successfully
```

---

## Common Issues

### Issue 1: Port Already in Use

**Error:**
```
Web server failed to start. Port 8081 was already in use.
```

**Solution:**
```bash
# Find process using the port
# macOS/Linux:
lsof -i :8081
kill -9 <PID>

# Windows:
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# Or change port in application.yml
server:
  port: 8091  # Use different port
```

### Issue 2: Cannot Connect to Database

**Error:**
```
Connection to localhost:5432 refused
```

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Start if not running
docker-compose -f docker-compose-platform.yml up -d postgres-users

# Check connection
psql -h localhost -p 5432 -U postgres -d doctor_users
```

### Issue 3: Service Not Registering with Eureka

**Error:**
Service doesn't appear in Eureka dashboard

**Solution:**
1. Check Eureka is running:
   ```bash
   curl http://localhost:8761/actuator/health
   ```

2. Check service configuration:
   ```yaml
   # In application.yml
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
       register-with-eureka: true
       fetch-registry: true
   ```

3. Wait 30 seconds (registration interval)

4. Check service logs for errors

### Issue 4: Out of Memory

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx2048m -Xms512m"

# Or run JAR with more memory
java -Xmx2048m -Xms512m -jar user-service/target/user-service-1.0.0-SNAPSHOT.jar
```

### Issue 5: Dependencies Not Downloading

**Error:**
```
Failed to collect dependencies
```

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Update dependencies
mvn clean install -U

# Check Maven settings
mvn -version
cat ~/.m2/settings.xml
```

### Issue 6: JWT Token Invalid

**Error:**
```
401 Unauthorized - Invalid token
```

**Solution:**
1. Ensure same JWT secret across all services:
   ```yaml
   jwt:
     secret: mySecretKeyThatIsAtLeast256BitsLongForHS256AlgorithmSecurityPurposes
   ```

2. Check token hasn't expired (24 hours default)

3. Verify Authorization header format:
   ```
   Authorization: Bearer eyJhbGci...
   ```

### Issue 7: Kafka Connection Failed

**Error:**
```
Error connecting to node kafka:29092
```

**Solution:**
```bash
# Check Kafka is running
docker ps | grep kafka

# Start Kafka
docker-compose -f docker-compose-platform.yml up -d kafka zookeeper

# Wait for Kafka to be ready (30 seconds)
docker logs doctor-kafka

# Test connection
docker exec -it doctor-kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## Next Steps

Once all services are running:

1. **Test Full Flow**: Register → Login → Create Post → Like Post
2. **Check Monitoring**: http://localhost:8761 (Eureka)
3. **View Metrics**: http://localhost:8081/actuator/metrics
4. **Setup Database**: See [SETUP_02_DATABASE.md](./SETUP_02_DATABASE.md)
5. **Setup Docker**: See [SETUP_03_DOCKER.md](./SETUP_03_DOCKER.md)

---

## Performance Tips

### 1. Skip Tests During Development
```bash
mvn spring-boot:run -DskipTests
```

### 2. Use Spring Boot DevTools
Add to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### 3. Increase Maven Performance
```bash
# Use multiple threads
mvn clean install -T 4

# Skip Javadoc generation
mvn clean install -Dmaven.javadoc.skip=true
```

### 4. Use Profiles
```bash
# Development profile with debug logging
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Summary Checklist

- [ ] Java 17 installed and verified
- [ ] Maven 3.8+ installed and verified
- [ ] Git installed
- [ ] Project cloned from repository
- [ ] All modules built successfully (`mvn clean install`)
- [ ] Infrastructure running (PostgreSQL, Redis, Kafka)
- [ ] Service Discovery started (Port 8761)
- [ ] API Gateway started (Port 8080)
- [ ] User Service started (Port 8081)
- [ ] Post Service started (Port 8082)
- [ ] All services registered in Eureka
- [ ] Successfully registered a test user
- [ ] Successfully created a test post

**Congratulations! Your backend is now running! 🎉**

For database setup details, continue to [SETUP_02_DATABASE.md](./SETUP_02_DATABASE.md)
