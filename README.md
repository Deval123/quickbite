# QuickBite - Food Delivery Platform

Projet pédagogique de construction d'une plateforme de livraison de repas en microservices avec Spring Boot.

## Videos

- [x] Video 1 : Decomposition en microservices (tag `v1.0`)
- [x] Video 2 : API Gateway (tag `v2.0`)
- [x] Video 3 : Authentification OAuth2 (tag `v3.0`)
- [x] Video 4 : Database per Service + Flyway + JPA (tag `v4.0`)
- [x] Video 5 : REST externe + gRPC interne (tag `v5.0`)
- [ ] Video 6 : CQRS
- [ ] Video 7 : Event Sourcing

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

### 2. Vérifier que tous les users Keycloak existent

> Si le realm a été importé avant l'ajout de certains users, Keycloak ne réimporte pas (`--import-realm` skip si le realm existe).
> En cas de `invalid_grant` : `docker compose down keycloak keycloak-db && docker volume rm quickbite_keycloak-db-data && docker compose up -d keycloak-db keycloak`

```bash
for user in client1 client2 restaurant1 driver1; do
  echo -n "$user: "
  curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
    -d "grant_type=password" \
    -d "client_id=quickbite-mobile" \
    -d "username=$user" \
    -d "password=password" | jq -r 'if .access_token then "OK" else .error end'
done
echo -n "admin: "
curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=admin" \
  -d "password=admin" | jq -r 'if .access_token then "OK" else .error end'
# Les 5 doivent afficher "OK"
```

### 3. Obtenir un token utilisateur (Direct Access Grant)

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

### 4. Décoder le JWT (sans vérifier la signature)

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

### 5. Appeler un service via le Gateway

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

### 6. Obtenir un token service-à-service (Client Credentials)

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

### 7. Vérifier les endpoints Keycloak

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

## Tester REST + gRPC + Sécurité (Vidéo 5)

### 1. Compiler les stubs gRPC (Protobuf)

```bash
# Compiler le shared-kernel (génère les classes Java à partir du .proto)
mvn clean compile -pl shared-kernel

# Vérifier que les stubs sont générés
ls shared-kernel/target/generated-sources/protobuf/java/com/devalere/quickbite/grpc/restaurant/
# → GetMenuItemsRequest.java, GetMenuItemsResponse.java, MenuItemProto.java,
#   CheckItemsRequest.java, CheckItemsResponse.java, ItemAvailability.java

ls shared-kernel/target/generated-sources/protobuf/grpc-java/com/devalere/quickbite/grpc/restaurant/
# → RestaurantServiceGrpc.java (les stubs client/serveur)

# Compiler tout le projet
mvn clean compile
```

### 2. Obtenir les tokens Keycloak

> Prérequis : `docker compose up -d` (Keycloak doit être démarré).
> Les tokens expirent après 5 min. Si un curl retourne 401, régénérer le token.

**Utilisateurs de test** (définis dans `infra/keycloak/quickbite-realm.json`) :

| Username     | Password | Rôle       | Permissions                           |
|-------------|----------|------------|---------------------------------------|
| client1     | password | CLIENT     | Créer une commande, voir sa commande  |
| client2     | password | CLIENT     | Idem (teste l'isolation entre clients)|
| restaurant1 | password | RESTAURANT | Voir/modifier le statut des commandes |
| driver1     | password | DRIVER     | Modifier le statut (livraison)        |
| admin       | admin    | ADMIN      | Tout voir, tout modifier              |

```bash
# Token CLIENT
TOKEN_CLIENT=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=client1" \
  -d "password=password" | jq -r .access_token)

# Token CLIENT 2 (pour tester l'isolation)
TOKEN_CLIENT2=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=client2" \
  -d "password=password" | jq -r .access_token)

# Token RESTAURANT
TOKEN_RESTAURANT=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=restaurant1" \
  -d "password=password" | jq -r .access_token)

# Token DRIVER
TOKEN_DRIVER=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=driver1" \
  -d "password=password" | jq -r .access_token)

# Token ADMIN
TOKEN_ADMIN=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=admin" \
  -d "password=admin" | jq -r .access_token)

# Vérifier (doit afficher eyJhbG...)
echo $TOKEN_CLIENT | head -c 20

# Décoder le payload pour voir les rôles
echo $TOKEN_CLIENT | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

### 3. Restaurants de demo (seed data)

5 restaurants sont insérés automatiquement via Flyway (`V3__insert_seed_restaurants.sql`) :

| Restaurant            | Cuisine   | UUID (pour les curls)                        |
|-----------------------|-----------|----------------------------------------------|
| Le Petit Bistrot      | Français  | `a1b2c3d4-0001-4000-8000-000000000001`       |
| Tokyo Ramen House     | Japonais  | `a1b2c3d4-0001-4000-8000-000000000002`       |
| Pizza Napoli          | Italien   | `a1b2c3d4-0001-4000-8000-000000000003`       |
| Le Kebab du Quartier  | Turc      | `a1b2c3d4-0001-4000-8000-000000000004`       |
| Chez Mama Africa      | Africain  | `a1b2c3d4-0001-4000-8000-000000000005`       |

Chaque restaurant a 5 items au menu (dont 1 indisponible pour tester les erreurs).

### 4. Tester REST — Restaurant Service

```bash
# Lister les 5 restaurants
curl -s http://localhost:8085/api/restaurants \
  -H "Authorization: Bearer $TOKEN_CLIENT" | jq

# Menu du Tokyo Ramen House
curl -s http://localhost:8085/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items \
  -H "Authorization: Bearer $TOKEN_CLIENT" | jq

# Sans token → 401
curl -s -o /dev/null -w "Sans token: %{http_code}\n" http://localhost:8085/api/restaurants
```

### 5. Tester REST + gRPC — Order Service (flux complet)

```bash
# CLIENT commande chez Tokyo Ramen (Tonkotsu x2 + Gyoza x1)
# → POST REST au order-service
# → order-service appelle restaurant-service en gRPC pour vérifier les items
# → commande créée si tous les items sont disponibles

ORDER_ID=$(curl -s -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "a1b2c3d4-0001-4000-8000-000000000002",
    "items": [
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000001", "quantity": 2},
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000003", "quantity": 1}
    ]
  }' | jq -r '.id')

echo "Order créé : $ORDER_ID"
# → 201 Created (noter l'UUID de la commande pour les tests suivants)

# RESTAURANT ne peut PAS créer de commande → 403
curl -s -o /dev/null -w "RESTAURANT crée commande: %{http_code}\n" \
  -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $TOKEN_RESTAURANT" \
  -H "Content-Type: application/json" \
  -d '{"restaurantId":"a1b2c3d4-0001-4000-8000-000000000001","items":[{"menuItemId":"b1b2c3d4-0001-4000-8000-000000000001","quantity":1}],"deliveryAddress":"Paris"}'

# CLIENT consulte SA commande → 200
curl -s http://localhost:8083/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN_CLIENT" | jq

# CLIENT2 essaie de voir la commande de CLIENT1 → 403 (isolation)
curl -s -o /dev/null -w "Client2 voit commande client1: %{http_code}\n" \
  http://localhost:8083/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN_CLIENT2"

# ADMIN consulte n'importe quelle commande → 200
curl -s http://localhost:8083/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN_ADMIN" | jq

# RESTAURANT confirme la commande → 200
curl -s -X PATCH "http://localhost:8083/api/orders/$ORDER_ID/status?status=CONFIRMED" \
  -H "Authorization: Bearer $TOKEN_RESTAURANT" | jq

# CLIENT ne peut PAS changer le statut → 403
curl -s -o /dev/null -w "CLIENT change statut: %{http_code}\n" \
  -X PATCH "http://localhost:8083/api/orders/$ORDER_ID/status?status=CONFIRMED" \
  -H "Authorization: Bearer $TOKEN_CLIENT"

# DRIVER livre la commande → 200
curl -s -X PATCH "http://localhost:8083/api/orders/$ORDER_ID/status?status=DELIVERED" \
  -H "Authorization: Bearer $TOKEN_DRIVER" | jq
```

### 6. Tester gRPC directement (communication interne)

```bash
# gRPC = communication interne, pas de JWT (sécurité réseau / mTLS en prod)
# Installer grpcurl : brew install grpcurl

# Vérifier que grpcurl est installé
grpcurl --version

# Lister les services gRPC
grpcurl -plaintext localhost:9001 list

# Récupérer le menu du Petit Bistrot
grpcurl -plaintext \
  -d '{"restaurant_id": "a1b2c3d4-0001-4000-8000-000000000001"}' \
  localhost:9001 com.devalere.quickbite.proto.RestaurantService/GetMenuItems
  
# Vérifier la disponibilité d'items
grpcurl -plaintext \
  -d '{"restaurant_id": "a1b2c3d4-0001-4000-8000-000000000001", "item_ids": ["b1b2c3d4-0001-4000-8000-000000000001", "b1b2c3d4-0001-4000-8000-000000000005"]}' \
  localhost:9001 com.devalere.quickbite.proto.RestaurantService/CheckItemsAvailability
  
# → allAvailable: false (la Salade Niçoise est indisponible)
```

### 7. Tester via Gateway (flux complet mobile → gateway → services)

```bash
# Le flux : Mobile (REST) → Gateway :8080 → Order :8083 (REST) → Restaurant :8085 (gRPC)
# Le client mobile ne sait pas que gRPC existe.

curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "a1b2c3d4-0001-4000-8000-000000000005",
    "items": [
      {"menuItemId": "b1b2c3d4-0005-4000-8000-000000000001", "quantity": 1},
      {"menuItemId": "b1b2c3d4-0005-4000-8000-000000000004", "quantity": 2}
    ],
    "deliveryAddress": "15 Rue Dejean, 75018 Paris"
  }' | jq
# → Mafé Poulet x1 + Alloco x2 chez Mama Africa

# Vérifier les logs de order-service :
# "gRPC call: checkItemsAvailability for restaurant ... with 2 items"
# "gRPC response: allAvailable=true"
```

### 8. Tester les erreurs

```bash
# Item indisponible (Salade Niçoise — available: false)
curl -s -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "a1b2c3d4-0001-4000-8000-000000000001",
    "items": [{"menuItemId": "b1b2c3d4-0001-4000-8000-000000000005", "quantity": 1}],
    "deliveryAddress": "Paris"
  }' | jq
# → Items unavailable

# Sans token → 401
curl -s -o /dev/null -w "Sans token: %{http_code}\n" http://localhost:8083/api/orders/$ORDER_ID
```

### Matrice de sécurité

```
Endpoint                        | CLIENT | RESTAURANT | DRIVER | ADMIN | Sans token
POST   /api/orders              |  201   |    403     |  403   |  403  |    401
GET    /api/orders/{id}         |  200*  |    403     |  403   |  200  |    401
PATCH  /api/orders/{id}/status  |  403   |    200     |  200   |  200  |    401
GET    /api/restaurants         |  200   |    200     |  200   |  200  |    401
GET    /api/restaurants/{id}    |  200   |    200     |  200   |  200  |    401

* 200 seulement si c'est SA commande (@PostAuthorize owner check)
```

### Ports (Vidéo 5)

| Service             | REST  | gRPC |
|---------------------|-------|------|
| gateway-service     | 8080  | —    |
| user-service        | 8084  | —    |
| restaurant-service  | 8085  | 9001 |
| order-service       | 8083  | —    |
| payment-service     | 8086  | —    |
| delivery-service    | 8088  | —    |
| notification-service| 8087  | —    |

---

## Stack technique

| Technologie               | Usage                              |
|---------------------------|------------------------------------|
| Spring Boot 3.x           | Framework microservices            |
| Spring Cloud Gateway 5.x  | API Gateway + Rate Limiting        |
| Spring Data JPA           | Persistance                        |
| PostgreSQL                | Base de données (une par service)  |
| Flyway                    | Migrations de schéma               |
| gRPC + Protobuf           | Communication interne (service-to-service) |
| Redis                     | Rate limiting du Gateway           |
| Apache Kafka              | Messaging asynchrone               |
| Keycloak                  | Identity Provider OAuth2 / OIDC    |
| Docker Compose            | Infrastructure locale              |
| Maven multi-module        | Build                              |
