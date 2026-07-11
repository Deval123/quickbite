# QuickBite - Food Delivery Platform

Projet pédagogique de construction d'une plateforme de livraison de repas en microservices avec Spring Boot.

## Videos

- [x] Video 1 : Decomposition en microservices (tag `v1.0`)
- [ ] Video 2 : API Gateway
- [ ] Video 3 : Authentification OAuth2
- [ ] Video 4 : Service Discovery
- [ ] Video 5 : Communication inter-services (Kafka)
- [ ] Video 6 : Observabilité (Prometheus / Grafana)

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                    Microservices                     │
│                                                      │
│  user-service        :8084  ──► postgres :5438       │
│  restaurant-service  :8085  ──► postgres :5433       │
│  order-service       :8083  ──► postgres :5434       │
│  payment-service     :8086  ──► postgres :5435       │
│  delivery-service    :8088  ──► postgres :5436       │
│  notification-service:8087  ──► postgres :5437       │
│                                                      │
│  shared-kernel (bibliothèque commune)                │
└──────────────────────────────────────────────────────┘
                         │
              ┌──────────▼──────────┐
              │    Kafka :9092      │
              └─────────────────────┘
```

## Pour lancer

### Prérequis
- Java 17+
- Maven 3.9+
- Docker Desktop

### 1. Démarrer l'infrastructure
```bash
docker compose up -d
```
Lance : Kafka, PostgreSQL (x6)

### 2. Compiler le projet
```bash
mvn clean install -DskipTests
```

### 3. Démarrer les services
Lancer chaque service depuis IntelliJ ou via :
```bash
cd user-service        && mvn spring-boot:run &
cd restaurant-service  && mvn spring-boot:run &
cd order-service       && mvn spring-boot:run &
cd payment-service     && mvn spring-boot:run &
cd delivery-service    && mvn spring-boot:run &
cd notification-service&& mvn spring-boot:run &
```

## Stack technique

| Technologie | Usage |
|-------------|-------|
| Spring Boot 3.x | Framework microservices |
| Spring Data JPA | Persistance |
| PostgreSQL | Base de données (une par service) |
| Apache Kafka | Messaging asynchrone |
| Docker Compose | Infrastructure locale |
| Maven multi-module | Build |
