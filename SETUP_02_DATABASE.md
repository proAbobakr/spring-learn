# Database Setup Guide - From Zero to Production

Complete guide for setting up PostgreSQL, Redis, and Kafka for the Doctor Social Platform.

## Table of Contents

1. [Database Architecture](#database-architecture)
2. [PostgreSQL Setup](#postgresql-setup)
3. [Redis Setup](#redis-setup)
4. [Kafka Setup](#kafka-setup)
5. [Database Migration & Management](#database-migration--management)
6. [Backup & Recovery](#backup--recovery)
7. [Monitoring & Optimization](#monitoring--optimization)

---

## Database Architecture

### Database per Service Pattern

Each microservice has its own database for isolation and independence:

```
User Service     → doctor_users      (PostgreSQL on port 5432)
Post Service     → doctor_posts      (PostgreSQL on port 5433)
Media Service    → doctor_media      (PostgreSQL on port 5434)
Comment Service  → doctor_comments   (PostgreSQL on port 5435)
```

**Benefits:**
- **Isolation**: Service failure doesn't affect others
- **Scalability**: Scale databases independently
- **Technology Freedom**: Each service can use optimal DB
- **Deployment**: Independent deployment cycles

---

## PostgreSQL Setup

### Option 1: Using Docker (Recommended for Development)

#### Step 1: Install Docker

**Windows:**
1. Download [Docker Desktop](https://www.docker.com/products/docker-desktop)
2. Run installer
3. Start Docker Desktop
4. Verify:
```cmd
docker --version
docker-compose --version
```

**macOS:**
```bash
# Using Homebrew
brew install --cask docker

# Start Docker Desktop from Applications
# Verify
docker --version
docker-compose --version
```

**Linux (Ubuntu/Debian):**
```bash
# Install Docker
sudo apt update
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Verify
docker --version
docker-compose --version
```

#### Step 2: Start PostgreSQL Containers

```bash
# Navigate to project directory
cd spring-learn

# Start all PostgreSQL databases
docker-compose -f docker-compose-platform.yml up -d \
  postgres-users \
  postgres-posts \
  postgres-media \
  postgres-comments

# Verify containers are running
docker ps | grep postgres

# Expected output:
# doctor-postgres-users    Up 2 minutes   0.0.0.0:5432->5432/tcp
# doctor-postgres-posts    Up 2 minutes   0.0.0.0:5433->5432/tcp
# doctor-postgres-media    Up 2 minutes   0.0.0.0:5434->5432/tcp
# doctor-postgres-comments Up 2 minutes   0.0.0.0:5435->5432/tcp
```

#### Step 3: Verify Databases

```bash
# Check logs
docker logs doctor-postgres-users

# Should see:
# database system is ready to accept connections

# Connect to database
docker exec -it doctor-postgres-users psql -U postgres -d doctor_users

# You should see PostgreSQL prompt:
# doctor_users=#

# List databases
\l

# Exit
\q
```

#### Step 4: Create Tables (Automatic)

Tables are created automatically by Hibernate when services start:

```yaml
# In application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Creates/updates tables automatically
```

**To manually create tables:**
```bash
# Connect to database
docker exec -it doctor-postgres-users psql -U postgres -d doctor_users

# Create doctors table
CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    license_number VARCHAR(255),
    institution VARCHAR(255),
    country VARCHAR(255),
    city VARCHAR(255),
    bio VARCHAR(1000),
    profile_image_url VARCHAR(500),
    years_of_experience INTEGER,
    verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    total_posts BIGINT DEFAULT 0,
    total_followers BIGINT DEFAULT 0,
    total_following BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

# Create indexes
CREATE INDEX idx_email ON doctors(email);
CREATE INDEX idx_specialty ON doctors(specialty);
CREATE INDEX idx_location ON doctors(country, city);

# Create followings table
CREATE TABLE followings (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, following_id)
);

CREATE INDEX idx_follower ON followings(follower_id);
CREATE INDEX idx_following ON followings(following_id);

# Verify tables
\dt

# Exit
\q
```

---

### Option 2: Native PostgreSQL Installation

#### Windows

1. **Download PostgreSQL 15**
   - Go to [PostgreSQL Downloads](https://www.postgresql.org/download/windows/)
   - Download installer
   - Run installer
   - Set password for `postgres` user: `postgres`
   - Port: `5432`
   - Complete installation

2. **Add to PATH**
   - Add `C:\Program Files\PostgreSQL\15\bin` to PATH

3. **Create Databases**
```cmd
# Open Command Prompt as Administrator
# Access PostgreSQL
psql -U postgres

# Create databases
CREATE DATABASE doctor_users;
CREATE DATABASE doctor_posts;
CREATE DATABASE doctor_media;
CREATE DATABASE doctor_comments;

# List databases
\l

# Exit
\q
```

#### macOS

```bash
# Install PostgreSQL via Homebrew
brew install postgresql@15

# Start PostgreSQL service
brew services start postgresql@15

# Create databases
createdb doctor_users
createdb doctor_posts
createdb doctor_media
createdb doctor_comments

# Verify
psql -l
```

#### Linux (Ubuntu/Debian)

```bash
# Install PostgreSQL
sudo apt update
sudo apt install -y postgresql-15 postgresql-contrib-15

# Start PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Switch to postgres user
sudo -i -u postgres

# Create databases
psql
CREATE DATABASE doctor_users;
CREATE DATABASE doctor_posts;
CREATE DATABASE doctor_media;
CREATE DATABASE doctor_comments;

# Create application user
CREATE USER doctor_app WITH PASSWORD 'SecurePass123!';
GRANT ALL PRIVILEGES ON DATABASE doctor_users TO doctor_app;
GRANT ALL PRIVILEGES ON DATABASE doctor_posts TO doctor_app;
GRANT ALL PRIVILEGES ON DATABASE doctor_media TO doctor_app;
GRANT ALL PRIVILEGES ON DATABASE doctor_comments TO doctor_app;

\q
exit
```

---

### PostgreSQL Configuration

#### 1. Connection Pooling (HikariCP)

Already configured in `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # Max connections
      minimum-idle: 5             # Min idle connections
      connection-timeout: 30000   # 30 seconds
      idle-timeout: 600000        # 10 minutes
      max-lifetime: 1800000       # 30 minutes
```

#### 2. Performance Tuning

Edit PostgreSQL config (`postgresql.conf`):

```bash
# Find config location
docker exec -it doctor-postgres-users psql -U postgres -c "SHOW config_file"

# For native installation
# Linux: /etc/postgresql/15/main/postgresql.conf
# macOS: /opt/homebrew/var/postgresql@15/postgresql.conf
# Windows: C:\Program Files\PostgreSQL\15\data\postgresql.conf
```

**Recommended Settings:**

```conf
# Memory Settings
shared_buffers = 256MB              # 25% of RAM for dedicated server
effective_cache_size = 1GB          # 50-75% of RAM
work_mem = 16MB                     # Memory for sorts/joins
maintenance_work_mem = 128MB        # Memory for maintenance operations

# Connection Settings
max_connections = 100               # Adjust based on services

# Write-Ahead Log
wal_buffers = 16MB
checkpoint_completion_target = 0.9

# Query Planner
random_page_cost = 1.1              # For SSD storage
effective_io_concurrency = 200      # For SSD storage

# Logging
log_min_duration_statement = 1000   # Log slow queries (>1s)
log_connections = on
log_disconnections = on
```

**Restart PostgreSQL:**
```bash
# Docker
docker restart doctor-postgres-users

# Native Linux
sudo systemctl restart postgresql

# Native macOS
brew services restart postgresql@15
```

#### 3. Enable Replication (Production)

**Master Configuration:**

```bash
# Edit postgresql.conf
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
synchronous_commit = on

# Edit pg_hba.conf
# Add replication user
host    replication     replicator      192.168.1.0/24      md5
```

**Create Replication User:**
```sql
CREATE USER replicator WITH REPLICATION PASSWORD 'ReplicaPass123!';
```

**Slave Configuration:**
```bash
# Stop slave
pg_basebackup -h master_ip -D /var/lib/postgresql/15/main -U replicator -P

# Create recovery.conf (or postgresql.auto.conf in PG 12+)
primary_conninfo = 'host=master_ip port=5432 user=replicator password=ReplicaPass123!'
hot_standby = on

# Start slave
sudo systemctl start postgresql
```

---

## Redis Setup

### Option 1: Using Docker (Recommended)

#### Step 1: Start Redis

```bash
# Start Redis master and replica
docker-compose -f docker-compose-platform.yml up -d redis-master redis-replica-1

# Verify
docker ps | grep redis

# Expected:
# doctor-redis-master     Up 1 minute   0.0.0.0:6379->6379/tcp
# doctor-redis-replica-1  Up 1 minute   0.0.0.0:6380->6379/tcp
```

#### Step 2: Test Redis

```bash
# Connect to Redis
docker exec -it doctor-redis-master redis-cli

# Authenticate (password: redispassword)
AUTH redispassword

# Test commands
SET test "Hello Redis"
GET test
# Returns: "Hello Redis"

PING
# Returns: PONG

INFO replication
# Shows master/slave info

# Exit
exit
```

#### Step 3: Configure Redis for Production

**Create redis.conf:**
```bash
cat > redis-prod.conf << 'EOF'
# Network
bind 0.0.0.0
protected-mode yes
port 6379
requirepass redispassword

# Persistence
save 900 1      # Save after 900 sec if at least 1 key changed
save 300 10     # Save after 300 sec if at least 10 keys changed
save 60 10000   # Save after 60 sec if at least 10000 keys changed

appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec

# Memory
maxmemory 2gb
maxmemory-policy allkeys-lru

# Replication
min-replicas-to-write 1
min-replicas-max-lag 10

# Performance
tcp-backlog 511
timeout 0
tcp-keepalive 300
EOF
```

**Use custom config:**
```yaml
# In docker-compose-platform.yml
redis-master:
  image: redis:7-alpine
  command: redis-server /usr/local/etc/redis/redis.conf
  volumes:
    - ./redis-prod.conf:/usr/local/etc/redis/redis.conf
```

---

### Option 2: Native Redis Installation

#### macOS
```bash
brew install redis

# Start Redis
brew services start redis

# Or run in foreground
redis-server /opt/homebrew/etc/redis.conf

# Test
redis-cli ping
# Returns: PONG
```

#### Linux (Ubuntu/Debian)
```bash
# Install Redis
sudo apt update
sudo apt install -y redis-server

# Configure
sudo nano /etc/redis/redis.conf

# Change these settings:
bind 0.0.0.0
requirepass redispassword
maxmemory 2gb
maxmemory-policy allkeys-lru

# Restart
sudo systemctl restart redis-server
sudo systemctl enable redis-server

# Test
redis-cli -a redispassword ping
```

#### Windows
```cmd
# Download from: https://github.com/microsoftarchive/redis/releases
# Extract and run
redis-server.exe redis.windows.conf

# In another terminal
redis-cli.exe
ping
```

---

### Redis Cluster Setup (Production)

For high availability with automatic failover:

```bash
# Create 6 Redis instances (3 masters, 3 slaves)
# redis-7000.conf, redis-7001.conf, ... redis-7005.conf

port 7000
cluster-enabled yes
cluster-config-file nodes-7000.conf
cluster-node-timeout 5000
appendonly yes

# Start all instances
redis-server redis-7000.conf &
redis-server redis-7001.conf &
redis-server redis-7002.conf &
redis-server redis-7003.conf &
redis-server redis-7004.conf &
redis-server redis-7005.conf &

# Create cluster
redis-cli --cluster create \
  127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
  127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
  --cluster-replicas 1

# Test cluster
redis-cli -c -p 7000
cluster info
cluster nodes
```

---

## Kafka Setup

### Option 1: Using Docker (Recommended)

#### Step 1: Start Zookeeper and Kafka

```bash
# Start Zookeeper first (Kafka dependency)
docker-compose -f docker-compose-platform.yml up -d zookeeper

# Wait 10 seconds for Zookeeper to be ready
sleep 10

# Start Kafka
docker-compose -f docker-compose-platform.yml up -d kafka

# Verify
docker ps | grep -E 'zookeeper|kafka'

# Check logs
docker logs doctor-zookeeper
docker logs doctor-kafka
```

#### Step 2: Create Topics

```bash
# Create topics for each event type
docker exec -it doctor-kafka kafka-topics \
  --create \
  --topic user-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it doctor-kafka kafka-topics \
  --create \
  --topic post-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it doctor-kafka kafka-topics \
  --create \
  --topic media-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it doctor-kafka kafka-topics \
  --create \
  --topic ai-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it doctor-kafka kafka-topics \
  --create \
  --topic notification-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# List topics
docker exec -it doctor-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

#### Step 3: Test Kafka

```bash
# Produce messages
docker exec -it doctor-kafka kafka-console-producer \
  --topic user-events \
  --bootstrap-server localhost:9092

# Type messages:
> {"eventType":"USER_REGISTERED","userId":1}
> {"eventType":"USER_UPDATED","userId":1}
# Press Ctrl+C to exit

# Consume messages (in another terminal)
docker exec -it doctor-kafka kafka-console-consumer \
  --topic user-events \
  --from-beginning \
  --bootstrap-server localhost:9092

# You should see the messages you sent
```

---

### Option 2: Native Kafka Installation

#### Download and Install

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
bin/kafka-topics.sh --create \
  --topic user-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

---

## Database Migration & Management

### Using Flyway (Recommended for Production)

#### Step 1: Add Flyway to User Service

```xml
<!-- Add to user-service/pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

#### Step 2: Create Migration Scripts

```bash
# Create migration directory
mkdir -p user-service/src/main/resources/db/migration

# Create initial migration
cat > user-service/src/main/resources/db/migration/V1__Initial_Schema.sql << 'EOF'
-- V1__Initial_Schema.sql

CREATE TABLE doctors (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    license_number VARCHAR(255),
    institution VARCHAR(255),
    country VARCHAR(255),
    city VARCHAR(255),
    bio VARCHAR(1000),
    profile_image_url VARCHAR(500),
    years_of_experience INTEGER,
    verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    total_posts BIGINT DEFAULT 0,
    total_followers BIGINT DEFAULT 0,
    total_following BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_email ON doctors(email);
CREATE INDEX idx_specialty ON doctors(specialty);
CREATE INDEX idx_location ON doctors(country, city);

CREATE TABLE followings (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, following_id)
);

CREATE INDEX idx_follower ON followings(follower_id);
CREATE INDEX idx_following ON followings(following_id);

CREATE TABLE doctor_roles (
    doctor_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);
EOF
```

#### Step 3: Configure Flyway

```yaml
# Add to user-service/src/main/resources/application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    schemas: public
    table: flyway_schema_history
  jpa:
    hibernate:
      ddl-auto: validate  # Change from 'update' to 'validate'
```

#### Step 4: Run Migration

```bash
# Migration runs automatically when service starts
cd user-service
mvn spring-boot:run

# You should see in logs:
# Flyway: Successfully validated 1 migration
# Flyway: Migrating schema "public" to version "1 - Initial Schema"
# Flyway: Successfully applied 1 migration
```

#### Step 5: Add New Migration

```bash
# Create new migration for adding a column
cat > user-service/src/main/resources/db/migration/V2__Add_Verification_Date.sql << 'EOF'
-- V2__Add_Verification_Date.sql

ALTER TABLE doctors ADD COLUMN verified_at TIMESTAMP;

CREATE INDEX idx_verified ON doctors(verified, verified_at);
EOF

# Restart service - migration applies automatically
```

---

## Backup & Recovery

### PostgreSQL Backup

#### 1. Manual Backup

```bash
# Backup single database
docker exec -t doctor-postgres-users pg_dump -U postgres doctor_users > backup_users_$(date +%Y%m%d).sql

# Backup all databases
docker exec -t doctor-postgres-users pg_dumpall -U postgres > backup_all_$(date +%Y%m%d).sql

# Compressed backup
docker exec -t doctor-postgres-users pg_dump -U postgres -Fc doctor_users > backup_users_$(date +%Y%m%d).dump
```

#### 2. Automated Backup Script

```bash
cat > backup-databases.sh << 'EOF'
#!/bin/bash

BACKUP_DIR="/backup/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup each database
docker exec -t doctor-postgres-users pg_dump -U postgres -Fc doctor_users > $BACKUP_DIR/users_$DATE.dump
docker exec -t doctor-postgres-posts pg_dump -U postgres -Fc doctor_posts > $BACKUP_DIR/posts_$DATE.dump
docker exec -t doctor-postgres-media pg_dump -U postgres -Fc doctor_media > $BACKUP_DIR/media_$DATE.dump
docker exec -t doctor-postgres-comments pg_dump -U postgres -Fc doctor_comments > $BACKUP_DIR/comments_$DATE.dump

# Delete backups older than 30 days
find $BACKUP_DIR -name "*.dump" -mtime +30 -delete

echo "Backup completed: $DATE"
EOF

chmod +x backup-databases.sh

# Schedule with cron (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * /path/to/backup-databases.sh") | crontab -
```

#### 3. Restore from Backup

```bash
# Restore from SQL file
docker exec -i doctor-postgres-users psql -U postgres doctor_users < backup_users_20240115.sql

# Restore from compressed dump
docker exec -i doctor-postgres-users pg_restore -U postgres -d doctor_users -Fc backup_users_20240115.dump

# Restore entire database cluster
docker exec -i doctor-postgres-users psql -U postgres < backup_all_20240115.sql
```

### Redis Backup

```bash
# Force a save
docker exec doctor-redis-master redis-cli -a redispassword SAVE

# Copy RDB file
docker cp doctor-redis-master:/data/dump.rdb ./backup/redis_$(date +%Y%m%d).rdb

# Restore: Copy RDB file back and restart
docker cp ./backup/redis_20240115.rdb doctor-redis-master:/data/dump.rdb
docker restart doctor-redis-master
```

---

## Monitoring & Optimization

### PostgreSQL Monitoring

#### 1. Check Connection Stats

```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Connections by database
SELECT datname, count(*)
FROM pg_stat_activity
GROUP BY datname;

-- Long-running queries
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active'
AND now() - pg_stat_activity.query_start > interval '5 minutes'
ORDER BY duration DESC;

-- Kill long-running query
SELECT pg_terminate_backend(pid);
```

#### 2. Check Table Sizes

```sql
-- Database sizes
SELECT datname, pg_size_pretty(pg_database_size(datname))
FROM pg_database
ORDER BY pg_database_size(datname) DESC;

-- Table sizes
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

#### 3. Analyze Query Performance

```sql
-- Enable query statistics
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Slowest queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Explain a query
EXPLAIN ANALYZE
SELECT * FROM doctors WHERE specialty = 'Cardiology';
```

### Redis Monitoring

```bash
# Connect to Redis
docker exec -it doctor-redis-master redis-cli -a redispassword

# Memory usage
INFO memory

# Statistics
INFO stats

# Keyspace
INFO keyspace

# Monitor commands in real-time
MONITOR

# Slow log
SLOWLOG GET 10
```

### Kafka Monitoring

```bash
# Consumer group status
docker exec -it doctor-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group user-service-group

# Topic details
docker exec -it doctor-kafka kafka-topics \
  --describe \
  --topic user-events \
  --bootstrap-server localhost:9092

# Log size
docker exec -it doctor-kafka kafka-log-dirs \
  --describe \
  --bootstrap-server localhost:9092
```

---

## Production Checklist

### PostgreSQL
- [ ] Enable SSL/TLS connections
- [ ] Set up replication (1 master, 2+ replicas)
- [ ] Configure automatic failover
- [ ] Set up automated backups
- [ ] Enable query logging for slow queries
- [ ] Configure connection pooling
- [ ] Set appropriate memory settings
- [ ] Create read-only users for reporting
- [ ] Set up monitoring and alerts

### Redis
- [ ] Enable persistence (RDB + AOF)
- [ ] Set up Redis Sentinel or Cluster
- [ ] Configure maxmemory and eviction policy
- [ ] Enable authentication
- [ ] Set up replication
- [ ] Configure backup schedule
- [ ] Monitor memory usage
- [ ] Set up alerts for memory threshold

### Kafka
- [ ] Set up multi-broker cluster (3+ brokers)
- [ ] Configure appropriate replication factor (3)
- [ ] Set retention policies
- [ ] Enable authentication (SASL)
- [ ] Configure quotas
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure appropriate partition count
- [ ] Set up backup for topic configurations

---

## Next Steps

Database infrastructure is ready! Continue to:
- [SETUP_03_DOCKER.md](./SETUP_03_DOCKER.md) - Docker containerization
- [SETUP_04_KUBERNETES.md](./SETUP_04_KUBERNETES.md) - Kubernetes deployment

**Your databases are now production-ready! 🎉**
