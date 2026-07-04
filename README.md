# QuickBite - Food Delivery Platform

Projet pédagogique de construction d'une plateforme de livraison de repas en microservices avec Spring Boot.

## Videos

- [x] Video 1 : Decomposition en microservices (tag `v1.0`)
- [x] Video 2 : API Gateway (tag `v2.0`)
- [x] Video 3 : Authentification OAuth2 (tag `v3.0`)
- [x] Video 4 : Database per Service + Flyway + JPA (tag `v4.0`)
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

## Tester l'authentification OAuth2 (Vidéo 3)

### 1. Démarrer l'infrastructure (avec Keycloak)

```bash
# Lancer Docker (Kafka + PostgreSQL + Redis + Keycloak)
docker compose up -d

# Attendre que Keycloak démarre (30-60 sec), vérifier les logs :
docker logs quickbite-keycloak --tail 5
# Chercher : "Keycloak ... started in ... "
```

Accéder à la console admin : **http://localhost:8180** (login : `admin` / `admin`)  
Vérifier que le realm **`quickbite`** existe.

### 2. Obtenir un token utilisateur (Direct Access Grant)

> En dev uniquement — en prod on utilise Authorization Code + PKCE.

```bash
ACCESS_TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=client1" \
  -d "password=password" \
  | jq -r '.access_token')

echo $ACCESS_TOKEN
```

### 3. Décoder le JWT (sans vérifier la signature)

```bash
# Le payload est en base64 (2ème partie du token)
echo $ACCESS_TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

Résultat attendu :
```json
{
  "sub": "...",
  "email": "client1@quickbite.com",
  "realm_access": { "roles": ["CLIENT"] },
  "exp": "...",
  "iss": "http://localhost:8180/realms/quickbite"
}
```

### 4. Appeler un service via le Gateway

```bash
# Requête authentifiée (doit retourner 200)
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/orders

# Requête sans token (doit retourner 401)
curl -v http://localhost:8080/api/orders

# Requête avec token invalide (doit retourner 401)
curl -H "Authorization: Bearer invalid-token" \
  http://localhost:8080/api/orders
```

### 5. Obtenir un token service-à-service (Client Credentials)

```bash
SVC_TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=order-svc" \
  -d "client_secret=order-svc-secret" \
  | jq -r '.access_token')

# Décoder — le claim "azp" doit valoir "order-svc"
echo $SVC_TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

### 6. Vérifier les endpoints Keycloak

```bash
# Configuration auto-découverte (utilisée par Spring)
curl -s http://localhost:8180/realms/quickbite/.well-known/openid-configuration | jq .

# Clés publiques JWKS (utilisées par les services pour vérifier les tokens)
curl -s http://localhost:8180/realms/quickbite/protocol/openid-connect/certs | jq .
```

---

## Vérifier la persistance - Database per Service (Vidéo 4)

### 1. Vérifier que les 5 PostgreSQL sont UP

```bash
docker compose ps
```

### 2. Démarrer les services (Flyway s'exécute au démarrage)

```bash
cd user-service       && mvn spring-boot:run &
cd restaurant-service  && mvn spring-boot:run &
cd order-service       && mvn spring-boot:run &
cd payment-service     && mvn spring-boot:run &
cd delivery-service    && mvn spring-boot:run &
```

Dans les logs, chercher :
```
Flyway ... Successfully applied N migrations
```

### 3. Vérifier les tables de chaque base — isolation totale

Chaque service a sa propre base PostgreSQL et ne voit **que** ses propres tables.

```bash
docker exec -it quickbite-postgres-user psql -U quickbite -d quickbite-user -c "\dt"
```
```
 Schema |         Name          | Type  |   Owner
--------+-----------------------+-------+-----------
 public | flyway_schema_history | table | quickbite
 public | users                 | table | quickbite
```

```bash
docker exec -it quickbite-postgres-restaurant psql -U quickbite -d quickbite-restaurant -c "\dt"
```
```
 Schema |         Name          | Type  |   Owner
--------+-----------------------+-------+-----------
 public | flyway_schema_history | table | quickbite
 public | menu_items            | table | quickbite
 public | restaurants           | table | quickbite
```

```bash
docker exec -it quickbite-postgres-order psql -U quickbite -d quickbite-order -c "\dt"
```
```
 Schema |         Name          | Type  |   Owner
--------+-----------------------+-------+-----------
 public | flyway_schema_history | table | quickbite
 public | order_items           | table | quickbite
 public | orders                | table | quickbite
```

```bash
docker exec -it quickbite-postgres-delivery psql -U quickbite -d quickbite-delivery -c "\dt"
```
```
 Schema |         Name          | Type  |   Owner
--------+-----------------------+-------+-----------
 public | deliveries            | table | quickbite
 public | flyway_schema_history | table | quickbite
```

`quickbite-postgres-payment` suit le même principe (table `payments` + `flyway_schema_history`).

### 4. Vérifier les migrations Flyway appliquées

```bash
docker exec -it quickbite-postgres-order psql -U quickbite -d quickbite-order \
  -c "SELECT version, description, success FROM flyway_schema_history;"
```

Résultat attendu (order-service) :
```
 version |      description       | success
---------+-------------------------+---------
 1       | create orders table     | t
 2       | create order items table| t
 3       | add status index        | t
```

### 5. Vérifier via l'API Actuator

```bash
curl -s http://localhost:8083/actuator/flyway | jq .
```

---

## Stack technique

| Technologie               | Usage                              |
|---------------------------|------------------------------------|
| Spring Boot 3.x           | Framework microservices            |
| Spring Cloud Gateway 5.x  | API Gateway + Rate Limiting        |
| Spring Data JPA           | Persistance                        |
| PostgreSQL                | Base de données (une par service)  |
| Redis                     | Rate limiting du Gateway           |
| Apache Kafka              | Messaging asynchrone               |
| Keycloak                  | Identity Provider OAuth2 / OIDC    |
| Docker Compose            | Infrastructure locale              |
| Maven multi-module        | Build                              |
