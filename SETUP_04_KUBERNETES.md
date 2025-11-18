# Kubernetes Setup Guide - Production Deployment from Zero

Complete guide for deploying the Doctor Social Platform on Kubernetes for production.

## Table of Contents

1. [Kubernetes Basics](#kubernetes-basics)
2. [Local Kubernetes Setup](#local-kubernetes-setup)
3. [Production Cluster Setup](#production-cluster-setup)
4. [Deploying the Platform](#deploying-the-platform)
5. [Scaling & Auto-scaling](#scaling--auto-scaling)
6. [Monitoring & Logging](#monitoring--logging)
7. [CI/CD Integration](#cicd-integration)
8. [Production Checklist](#production-checklist)

---

## Kubernetes Basics

### What is Kubernetes?

Kubernetes (K8s) is a container orchestration platform that automates deployment, scaling, and management of containerized applications.

**Key Benefits:**
- **Auto-scaling**: Scale based on CPU/memory
- **Self-healing**: Restart failed containers
- **Load balancing**: Distribute traffic
- **Rolling updates**: Zero-downtime deployments
- **Service discovery**: Automatic DNS for services

### Key Concepts

```
┌─────────────────────────────────────────────────┐
│                  Cluster                        │
│  ┌───────────────────────────────────────┐     │
│  │              Namespace                │     │
│  │  doctor-platform                      │     │
│  │                                       │     │
│  │  ┌──────────────────────────────┐    │     │
│  │  │        Deployment            │    │     │
│  │  │  Manages ReplicaSets         │    │     │
│  │  │  ┌────────────────────┐      │    │     │
│  │  │  │   ReplicaSet       │      │    │     │
│  │  │  │  Manages Pods      │      │    │     │
│  │  │  │  ┌──────────┐      │      │    │     │
│  │  │  │  │   Pod    │      │      │    │     │
│  │  │  │  │ ┌──────┐ │      │      │    │     │
│  │  │  │  │ │ Con- │ │      │      │    │     │
│  │  │  │  │ │tainer│ │      │      │    │     │
│  │  │  │  │ └──────┘ │      │      │    │     │
│  │  │  │  └──────────┘      │      │    │     │
│  │  │  └────────────────────┘      │    │     │
│  │  └──────────────────────────────┘    │     │
│  │                                       │     │
│  │  ┌──────────────────────────────┐    │     │
│  │  │         Service              │    │     │
│  │  │  Exposes Pods via            │    │     │
│  │  │  ClusterIP/LoadBalancer      │    │     │
│  │  └──────────────────────────────┘    │     │
│  └───────────────────────────────────────┘     │
└─────────────────────────────────────────────────┘
```

**Pod**: Smallest unit, runs one or more containers
**Deployment**: Manages desired state of Pods
**Service**: Network endpoint to access Pods
**ConfigMap**: Configuration data
**Secret**: Sensitive data (passwords, keys)
**PersistentVolume**: Storage
**Ingress**: HTTP(S) routing

---

## Local Kubernetes Setup

### Option 1: Minikube (Recommended for Learning)

#### Installation

**macOS:**
```bash
# Install minikube
brew install minikube

# Start cluster
minikube start --memory=8192 --cpus=4

# Enable addons
minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable dashboard

# Verify
kubectl cluster-info
kubectl get nodes
```

**Linux:**
```bash
# Install minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start cluster
minikube start --memory=8192 --cpus=4 --driver=docker

# Enable addons
minikube addons enable ingress
minikube addons enable metrics-server

# Verify
kubectl get nodes
```

**Windows:**
```powershell
# Install via Chocolatey
choco install minikube

# Or download from: https://minikube.sigs.k8s.io/docs/start/

# Start cluster
minikube start --memory=8192 --cpus=4

# Verify
kubectl get nodes
```

### Option 2: Docker Desktop Kubernetes

**macOS/Windows:**
1. Open Docker Desktop
2. Go to Settings → Kubernetes
3. Check "Enable Kubernetes"
4. Click "Apply & Restart"
5. Wait for "Kubernetes is running" status

**Verify:**
```bash
kubectl config get-contexts
kubectl config use-context docker-desktop
kubectl get nodes
```

### Option 3: kind (Kubernetes in Docker)

```bash
# Install kind
# macOS
brew install kind

# Linux
curl -Lo ./kind https://kind.sigs.k8s.io/dl/latest/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# Create cluster
kind create cluster --name doctor-platform

# Verify
kubectl cluster-info --context kind-doctor-platform
```

### Install kubectl

**macOS:**
```bash
brew install kubectl
```

**Linux:**
```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

**Windows:**
```powershell
choco install kubernetes-cli
```

**Verify:**
```bash
kubectl version --client
```

---

## Production Cluster Setup

### Option 1: AWS EKS (Amazon Elastic Kubernetes Service)

#### Prerequisites
- AWS Account
- AWS CLI installed
- eksctl installed

#### Install eksctl

**macOS/Linux:**
```bash
# Install eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Verify
eksctl version
```

#### Create EKS Cluster

```bash
# Configure AWS CLI
aws configure
# Enter: Access Key, Secret Key, Region (us-east-1), Output (json)

# Create cluster (takes 15-20 minutes)
eksctl create cluster \
  --name doctor-platform \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 3 \
  --nodes-max 10 \
  --managed

# Update kubeconfig
aws eks update-kubeconfig --name doctor-platform --region us-east-1

# Verify
kubectl get nodes
```

**Alternative: Using CloudFormation**
```yaml
# Save as eks-cluster.yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: doctor-platform
  region: us-east-1

managedNodeGroups:
  - name: general
    instanceType: t3.medium
    minSize: 3
    maxSize: 10
    desiredCapacity: 3
    volumeSize: 80
    labels:
      role: general
    tags:
      Environment: production
      Project: doctor-platform

  - name: memory-intensive
    instanceType: r5.large
    minSize: 2
    maxSize: 5
    desiredCapacity: 2
    volumeSize: 100
    labels:
      role: memory-intensive
```

```bash
# Create cluster
eksctl create cluster -f eks-cluster.yaml
```

### Option 2: Google GKE (Google Kubernetes Engine)

```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Create cluster
gcloud container clusters create doctor-platform \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type n1-standard-2 \
  --enable-autoscaling \
  --min-nodes 3 \
  --max-nodes 10

# Get credentials
gcloud container clusters get-credentials doctor-platform --zone us-central1-a

# Verify
kubectl get nodes
```

### Option 3: Azure AKS (Azure Kubernetes Service)

```bash
# Install Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Login
az login

# Create resource group
az group create --name doctor-platform-rg --location eastus

# Create cluster
az aks create \
  --resource-group doctor-platform-rg \
  --name doctor-platform \
  --node-count 3 \
  --enable-addons monitoring \
  --generate-ssh-keys

# Get credentials
az aks get-credentials --resource-group doctor-platform-rg --name doctor-platform

# Verify
kubectl get nodes
```

---

## Deploying the Platform

### Step 1: Create Namespace

```bash
# Navigate to project
cd spring-learn

# Create namespace
kubectl apply -f kubernetes/namespace.yml

# Verify
kubectl get namespaces
kubectl config set-context --current --namespace=doctor-platform
```

### Step 2: Create Secrets

```bash
# Create secrets
kubectl apply -f kubernetes/secrets/secrets.yml

# Verify
kubectl get secrets -n doctor-platform

# View secret (base64 encoded)
kubectl get secret postgres-secret -n doctor-platform -o yaml
```

**For production, use proper secret management:**

```bash
# Option 1: From literal values
kubectl create secret generic postgres-secret \
  -n doctor-platform \
  --from-literal=username=postgres \
  --from-literal=password="$(openssl rand -base64 32)"

# Option 2: From files
echo -n "postgres" > username.txt
echo -n "SecurePass123!" > password.txt
kubectl create secret generic postgres-secret \
  -n doctor-platform \
  --from-file=username=username.txt \
  --from-file=password=password.txt
rm username.txt password.txt

# Option 3: Using external secret manager (AWS Secrets Manager)
# Install external-secrets operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets-system --create-namespace
```

### Step 3: Create ConfigMaps

```bash
# Apply ConfigMaps
kubectl apply -f kubernetes/configmaps/

# Verify
kubectl get configmaps -n doctor-platform

# View ConfigMap
kubectl describe configmap user-service-config -n doctor-platform
```

### Step 4: Deploy Databases

```bash
# Deploy PostgreSQL StatefulSets
kubectl apply -f kubernetes/deployments/postgres-statefulset.yml

# Wait for ready
kubectl wait --for=condition=ready pod -l app=postgres-users -n doctor-platform --timeout=300s

# Check status
kubectl get statefulsets -n doctor-platform
kubectl get pods -n doctor-platform | grep postgres

# Check logs
kubectl logs postgres-users-0 -n doctor-platform

# Test database
kubectl exec -it postgres-users-0 -n doctor-platform -- psql -U postgres -d doctor_users
```

### Step 5: Deploy Redis

```bash
# Deploy Redis
kubectl apply -f kubernetes/deployments/redis-statefulset.yml

# Wait for ready
kubectl wait --for=condition=ready pod -l app=redis -n doctor-platform --timeout=300s

# Test Redis
kubectl exec -it redis-master-0 -n doctor-platform -- redis-cli -a changeme_in_production ping
# Should return: PONG
```

### Step 6: Deploy Kafka (Optional - using Strimzi operator)

```bash
# Install Strimzi Kafka operator
kubectl create namespace kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

# Wait for operator
kubectl wait --for=condition=ready pod -l name=strimzi-cluster-operator -n kafka --timeout=300s

# Create Kafka cluster
cat > kafka-cluster.yaml << 'EOF'
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: doctor-kafka
  namespace: doctor-platform
spec:
  kafka:
    version: 3.6.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: jbod
      volumes:
      - id: 0
        type: persistent-claim
        size: 100Gi
        deleteClaim: false
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 10Gi
      deleteClaim: false
  entityOperator:
    topicOperator: {}
    userOperator: {}
EOF

kubectl apply -f kafka-cluster.yaml
```

### Step 7: Deploy Microservices

```bash
# Build and push images to registry first
# For Docker Hub:
docker login
docker tag doctor-platform/service-discovery:latest yourusername/service-discovery:1.0.0
docker push yourusername/service-discovery:1.0.0

# Update image references in deployment files
# Edit kubernetes/deployments/user-service-deployment.yml
# Change: image: doctor-platform/user-service:latest
# To:     image: yourusername/user-service:1.0.0

# Deploy Service Discovery
kubectl apply -f kubernetes/deployments/service-discovery-deployment.yml

# Wait and verify
kubectl wait --for=condition=available deployment/service-discovery -n doctor-platform --timeout=300s
kubectl get pods -n doctor-platform -l app=service-discovery

# Deploy API Gateway
kubectl apply -f kubernetes/deployments/api-gateway-deployment.yml
kubectl wait --for=condition=available deployment/api-gateway -n doctor-platform --timeout=300s

# Deploy User Service
kubectl apply -f kubernetes/deployments/user-service-deployment.yml
kubectl wait --for=condition=available deployment/user-service -n doctor-platform --timeout=300s

# Check all deployments
kubectl get deployments -n doctor-platform
kubectl get pods -n doctor-platform
```

### Step 8: Create Deployment Script

```bash
cat > deploy-k8s.sh << 'EOF'
#!/bin/bash

set -e

NAMESPACE="doctor-platform"

echo "🚀 Deploying Doctor Platform to Kubernetes..."

# Create namespace
echo "Creating namespace..."
kubectl apply -f kubernetes/namespace.yml

# Create secrets
echo "Creating secrets..."
kubectl apply -f kubernetes/secrets/

# Create ConfigMaps
echo "Creating ConfigMaps..."
kubectl apply -f kubernetes/configmaps/

# Deploy databases
echo "Deploying databases..."
kubectl apply -f kubernetes/deployments/postgres-statefulset.yml
kubectl apply -f kubernetes/deployments/redis-statefulset.yml

echo "Waiting for databases to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres-users -n $NAMESPACE --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=300s

# Deploy services
echo "Deploying microservices..."
kubectl apply -f kubernetes/deployments/service-discovery-deployment.yml
sleep 30  # Wait for Eureka to start

kubectl apply -f kubernetes/deployments/api-gateway-deployment.yml
sleep 20  # Wait for Gateway

kubectl apply -f kubernetes/deployments/user-service-deployment.yml

echo "Waiting for services to be ready..."
kubectl wait --for=condition=available deployment --all -n $NAMESPACE --timeout=600s

echo ""
echo "✅ Deployment complete!"
echo ""
echo "Services:"
kubectl get svc -n $NAMESPACE

echo ""
echo "Pods:"
kubectl get pods -n $NAMESPACE

echo ""
echo "To access the API Gateway:"
echo "kubectl port-forward -n $NAMESPACE svc/api-gateway 8080:80"
EOF

chmod +x deploy-k8s.sh
./deploy-k8s.sh
```

### Step 9: Expose Services

#### Option 1: LoadBalancer (Cloud providers)

```yaml
# Already configured in api-gateway-deployment.yml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: doctor-platform
spec:
  type: LoadBalancer
  selector:
    app: api-gateway
  ports:
  - port: 80
    targetPort: 8080
```

```bash
# Get external IP
kubectl get svc api-gateway -n doctor-platform

# Access via external IP
curl http://<EXTERNAL-IP>/actuator/health
```

#### Option 2: Ingress (Recommended)

```yaml
# Create ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: doctor-platform-ingress
  namespace: doctor-platform
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.doctorplatform.com
    secretName: doctorplatform-tls
  rules:
  - host: api.doctorplatform.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 80
EOF

# Install Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Apply ingress
kubectl apply -f ingress.yaml

# Get ingress IP
kubectl get ingress -n doctor-platform
```

#### Option 3: Port Forwarding (Development)

```bash
# Forward API Gateway
kubectl port-forward -n doctor-platform svc/api-gateway 8080:80

# Forward Eureka Dashboard
kubectl port-forward -n doctor-platform svc/service-discovery 8761:8761

# Access
curl http://localhost:8080/actuator/health
```

---

## Scaling & Auto-scaling

### Manual Scaling

```bash
# Scale User Service to 5 replicas
kubectl scale deployment user-service -n doctor-platform --replicas=5

# Verify
kubectl get pods -n doctor-platform -l app=user-service
```

### Horizontal Pod Autoscaler (HPA)

Already configured in deployment files:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: doctor-platform
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

**Monitor HPA:**
```bash
# Check HPA status
kubectl get hpa -n doctor-platform

# Watch HPA in real-time
kubectl get hpa -n doctor-platform --watch

# Describe HPA
kubectl describe hpa user-service-hpa -n doctor-platform
```

**Test Autoscaling:**
```bash
# Generate load
kubectl run -it --rm load-generator -n doctor-platform \
  --image=busybox \
  --restart=Never \
  -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://api-gateway/api/users/1; done"

# Watch scaling
kubectl get hpa user-service-hpa -n doctor-platform --watch
```

### Cluster Autoscaler

**For AWS EKS:**
```bash
# Deploy cluster autoscaler
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml

# Edit deployment
kubectl -n kube-system edit deployment cluster-autoscaler

# Add your cluster name
--node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/doctor-platform
```

---

## Monitoring & Logging

### Prometheus & Grafana

```bash
# Add Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Prometheus + Grafana
helm install prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace

# Access Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# Default credentials: admin/prom-operator

# Access Prometheus
kubectl port-forward -n monitoring svc/prometheus-kube-prometheus-prometheus 9090:9090
```

**Import Dashboards:**
- Spring Boot Dashboard ID: 12464
- JVM Dashboard ID: 4701
- Kubernetes Dashboard ID: 7249

### Logging with ELK Stack

```bash
# Add Elastic Helm repo
helm repo add elastic https://helm.elastic.co
helm repo update

# Install Elasticsearch
helm install elasticsearch elastic/elasticsearch -n logging --create-namespace

# Install Kibana
helm install kibana elastic/kibana -n logging

# Install Filebeat
helm install filebeat elastic/filebeat -n logging

# Access Kibana
kubectl port-forward -n logging svc/kibana-kibana 5601:5601
```

### View Logs

```bash
# View logs of specific pod
kubectl logs user-service-7d4f5c8b9d-abcde -n doctor-platform

# Follow logs
kubectl logs -f user-service-7d4f5c8b9d-abcde -n doctor-platform

# Logs from all pods in deployment
kubectl logs -f deployment/user-service -n doctor-platform

# Previous container logs (if crashed)
kubectl logs user-service-7d4f5c8b9d-abcde -n doctor-platform --previous

# Logs from last hour
kubectl logs user-service-7d4f5c8b9d-abcde -n doctor-platform --since=1h

# Tail last 100 lines
kubectl logs user-service-7d4f5c8b9d-abcde -n doctor-platform --tail=100
```

---

## CI/CD Integration

### GitHub Actions

```yaml
# Create .github/workflows/deploy.yml
name: Deploy to Kubernetes

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build with Maven
      run: mvn clean package -DskipTests

    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v1
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-east-1

    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v1

    - name: Build and push Docker images
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        IMAGE_TAG: ${{ github.sha }}
      run: |
        docker build -t $ECR_REGISTRY/user-service:$IMAGE_TAG -f user-service/Dockerfile .
        docker push $ECR_REGISTRY/user-service:$IMAGE_TAG

    - name: Update kube config
      run: aws eks update-kubeconfig --name doctor-platform --region us-east-1

    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/user-service \
          user-service=$ECR_REGISTRY/user-service:$IMAGE_TAG \
          -n doctor-platform
```

---

## Production Checklist

### Security
- [ ] Enable RBAC
- [ ] Use Network Policies
- [ ] Scan images for vulnerabilities
- [ ] Use Pod Security Policies
- [ ] Enable audit logging
- [ ] Use secrets for sensitive data
- [ ] Enable TLS for all services
- [ ] Set up certificate management (cert-manager)

### High Availability
- [ ] Multi-zone deployment
- [ ] Set appropriate replica counts (minimum 3)
- [ ] Configure Pod Disruption Budgets
- [ ] Set resource requests and limits
- [ ] Enable cluster autoscaling
- [ ] Configure health checks

### Monitoring
- [ ] Install Prometheus & Grafana
- [ ] Set up alerts
- [ ] Configure logging (ELK/Loki)
- [ ] Enable distributed tracing
- [ ] Monitor resource usage

### Backup & Recovery
- [ ] Database backup strategy
- [ ] Velero for cluster backups
- [ ] Disaster recovery plan
- [ ] Test restore procedures

### Performance
- [ ] Configure HPA
- [ ] Use PodAntiAffinity
- [ ] Optimize resource requests/limits
- [ ] Use caching effectively

---

## Useful Commands

```bash
# Cluster Info
kubectl cluster-info
kubectl get nodes
kubectl top nodes

# Namespace Operations
kubectl get namespaces
kubectl create namespace doctor-platform
kubectl delete namespace doctor-platform

# Pod Operations
kubectl get pods -n doctor-platform
kubectl describe pod <pod-name> -n doctor-platform
kubectl logs <pod-name> -n doctor-platform
kubectl exec -it <pod-name> -n doctor-platform -- /bin/sh
kubectl delete pod <pod-name> -n doctor-platform

# Deployment Operations
kubectl get deployments -n doctor-platform
kubectl describe deployment user-service -n doctor-platform
kubectl rollout status deployment/user-service -n doctor-platform
kubectl rollout history deployment/user-service -n doctor-platform
kubectl rollout undo deployment/user-service -n doctor-platform

# Service Operations
kubectl get services -n doctor-platform
kubectl describe service user-service -n doctor-platform

# ConfigMap & Secrets
kubectl get configmaps -n doctor-platform
kubectl get secrets -n doctor-platform
kubectl describe configmap user-service-config -n doctor-platform

# Events
kubectl get events -n doctor-platform --sort-by='.lastTimestamp'

# Resource Usage
kubectl top pods -n doctor-platform
kubectl top nodes

# Port Forwarding
kubectl port-forward svc/api-gateway 8080:80 -n doctor-platform

# Debugging
kubectl describe pod <pod-name> -n doctor-platform
kubectl logs <pod-name> -n doctor-platform --previous
kubectl get events --sort-by='.lastTimestamp' -n doctor-platform
```

---

**Your production Kubernetes cluster is ready! ☸️**

**Congratulations! You now have a fully scalable, production-ready Doctor Social Platform! 🎉**
