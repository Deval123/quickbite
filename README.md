# QuickBite - Food Delivery Platform

Projet pédagogique de construction d'une plateforme de livraison de repas en microservices avec Spring Boot.

## Videos

- [x] Video 1 : Decomposition en microservices (tag `v1.0`)
- [x] Video 2 : API Gateway (tag `v2.0`)
- [ ] Video 3 : Authentification OAuth2
- [ ] Video 4 : Service Discovery
- [ ] Video 5 : Communication inter-services (Kafka)
- [ ] Video 6 : Observabilité (Prometheus / Grafana)

## Architecture

```
                    ┌─────────────────────┐
                    │  gateway-service     │
     Client ───────►│  :8080              │
                    │  Rate Limit (Redis) │
                    └──────────┬──────────┘
                               │ routing
         ┌─────────────────────┼──────────────────────┐
         ▼                     ▼                      ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│ user-service    │  │restaurant-service│  │   order-service      │
│ :8084           │  │ :8085            │  │   :8083              │
│ postgres :5438  │  │ postgres :5433   │  │   postgres :5434     │
└─────────────────┘  └──────────────────┘  └──────────────────────┘
         ▼                     ▼                      ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│ payment-service │  │delivery-service  │  │notification-service  │
│ :8086           │  │ :8088            │  │ :8087                │
│ postgres :5435  │  │ postgres :5436   │  │ postgres :5437       │
└─────────────────┘  └──────────────────┘  └──────────────────────┘
                               │
              ┌────────────────▼────────────────┐
              │         Kafka :9092              │
              └─────────────────────────────────┘
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
Lance : Kafka, PostgreSQL (x6), Redis

### 2. Compiler le projet
```bash
mvn clean install -DskipTests
```

### 3. Démarrer les services
Lancer chaque service depuis IntelliJ ou via :
```bash
cd user-service         && mvn spring-boot:run &
cd restaurant-service   && mvn spring-boot:run &
cd order-service        && mvn spring-boot:run &
cd payment-service      && mvn spring-boot:run &
cd delivery-service     && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
cd gateway-service      && mvn spring-boot:run &
```

### 4. Vérifier le gateway
```bash
# Health check
curl http://localhost:8080/actuator/health

# Routes configurées
curl http://localhost:8080/actuator/gateway/routes

# Header custom (doit afficher X-Gateway: QuickBite-Gateway)
curl -v http://localhost:8080/api/orders 2>&1 | grep X-Gateway
```

## Stack technique

| Technologie | Usage |
|-------------|-------|
| Spring Boot 3.x | Framework microservices |
| Spring Cloud Gateway 5.x | API Gateway + Rate Limiting |
| Spring Data JPA | Persistance |
| PostgreSQL | Base de données (une par service) |
| Redis | Rate limiting du Gateway |
| Apache Kafka | Messaging asynchrone |
| Docker Compose | Infrastructure locale |
| Maven multi-module | Build |
