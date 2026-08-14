# QuickBite - Food Delivery Platform

Projet pédagogique de construction d'une plateforme de livraison de repas en microservices avec Spring Boot.

## Videos

- [x] Video 1 : Decomposition en microservices (tag `v1.0`)
- [x] Video 2 : API Gateway (tag `v2.0`)
- [x] Video 3 : Authentification OAuth2 (tag `v3.0`)
- [x] Video 4 : Database per Service + Flyway + JPA (tag `v4.0`)
- [x] Video 5 : REST externe + gRPC interne (tag `v5.0`)
- [x] Video 6 : Communication asynchrone - Events et Kafka (tag `v6.0`)
- [x] Video 7 : Le flux de commande - Saga orchestree (tag `v7.0`)
- [x] Video 8 : Paiement - Idempotence et webhooks Stripe (tag `v8.0`)
- [x] Video 9 : Cache distribue - Redis (tag `v9.0`)
- [x] Video 10 : Recherche Elasticsearch (tag `v10.0`)

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

### 7. Lister les clients Keycloak

```bash
# Obtenir un token admin et lister les clients du realm
curl -s http://localhost:8180/admin/realms/quickbite/clients \
  -H "Authorization: Bearer $(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d 'grant_type=password&client_id=admin-cli&username=admin&password=admin' | jq -r '.access_token')" \
  | jq '.[].clientId'
```

Clients configures :
| Client ID         | Usage                                |
|-------------------|--------------------------------------|
| quickbite-mobile  | App mobile (Direct Access Grant)     |
| order-svc         | Service-to-service (Client Credentials) |
| payment-svc       | Service-to-service (Client Credentials) |
| restaurant-svc    | Service-to-service (Client Credentials) |
| delivery-svc      | Service-to-service (Client Credentials) |
| notification-svc  | Service-to-service (Client Credentials) |
| user-svc          | Service-to-service (Client Credentials) |

> **Important** : le client pour les tokens utilisateur est `quickbite-mobile`, pas `quickbite-app`.

### 8. Vérifier les endpoints Keycloak

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

## Tester Kafka - Communication asynchrone (Video 6)

### 1. Verifier que Kafka est UP

```bash
docker compose ps | grep kafka
# → quickbite-kafka doit etre "running" sur le port 9092
```

### 2. Lister les topics Kafka

```bash
docker exec quickbite-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

Resultat attendu (5 topics) :
```
delivery-events
notification-events
order-events
payment-events
restaurant-events
```

### 3. Verifier les details d'un topic

```bash
docker exec quickbite-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe --topic order-events
```

Resultat attendu :
```
Topic: order-events  PartitionCount: 3  ReplicationFactor: 1
```

### 4. Publier un event (creer une commande)

```bash
# Obtenir un token client (si pas deja fait)
TOKEN_CLIENT=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=client1" \
  -d "password=password" | jq -r .access_token)

# Creer une commande → declenche OrderCreatedEvent sur order-events
curl -s -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "a1b2c3d4-0001-4000-8000-000000000002",
    "items": [
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000001", "quantity": 2},
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000003", "quantity": 1}
    ],
    "deliveryAddress": "10 Rue de Rivoli, 75001 Paris"
  }' | jq
```

### 5. Lire les events dans un topic

```bash
# Lire les messages du topic order-events (timeout 5 sec pour ne pas bloquer)
docker exec quickbite-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --max-messages 5
```

> **Astuce** : sans `--timeout-ms`, le consumer reste bloque indefiniment en attente de nouveaux messages. Ajouter `--timeout-ms 5000` pour qu'il s'arrete apres 5 secondes d'inactivite.

### 6. Verifier les consumer groups

```bash
# Lister tous les consumer groups
docker exec quickbite-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list

# Details d'un group (lag, partitions, offsets)
  docker exec quickbite-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group payment-group
```

Resultat attendu (5 consumer groups) :
```
delivery-group
notification-group
order-group
payment-group
restaurant-group
```

### 7. Verifier dans les logs des services

```bash
# order-service : doit afficher "OrderCreatedEvent publie pour orderId=..."
# payment-service : doit afficher "Payment recu event: OrderCreatedEvent pour orderId=..."
# restaurant-service : doit afficher "Restaurant recu event OrderCreatedEvent pour orderId=..."
# notification-service : doit afficher "Email: 'Votre commande ... a ete creee'"
```

### Architecture Kafka QuickBite

```
                    ┌──────────────┐
                    │ order-service │
                    │  (Producer)  │
                    └──────┬───────┘
                           │ OrderCreatedEvent
                           ▼
                    ┌──────────────┐
                    │ order-events │  (3 partitions, cle = orderId)
                    └──┬────┬────┬─┘
                       │    │    │
          ┌────────────┘    │    └────────────┐
          ▼                 ▼                 ▼
   payment-group    restaurant-group   notification-group
   (PaymentSvc)     (RestaurantSvc)    (NotificationSvc)
```

---

## Tester la Saga - Flux de commande (Video 7)

### 1. State machine de la commande

```
CREATED -> PAYMENT_PENDING -> CONFIRMED -> PREPARING -> READY -> PICKED_UP -> DELIVERED
                |                 |
                v                 v
           CANCELLED          CANCELLED
         (paiement KO)     (restaurant refuse)
```

### 2. Creer une commande (declenche la Saga)

```bash
# Token client
TOKEN_CLIENT=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=quickbite-mobile" \
  -d "username=client1" \
  -d "password=password" | jq -r .access_token)

# Creer la commande
ORDER_ID=$(curl -s -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "a1b2c3d4-0001-4000-8000-000000000002",
    "items": [
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000001", "quantity": 2}
    ],
    "deliveryAddress": "10 Rue de Rivoli, 75001 Paris"
  }' | jq -r '.id')
echo "Order cree : $ORDER_ID"
```

### 3. Verifier le statut (doit etre PAYMENT_PENDING)

```bash
curl -s http://localhost:8083/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN_CLIENT" | jq '.status'
# -> "PAYMENT_PENDING"
```

### 4. Verifier le flux dans les logs

```bash
# order-service :
#   "OrderCreatedEvent publie pour orderId=..."
#   "Commande ... en attente de paiement -> PAYMENT_PENDING"

# payment-service :
#   "Payment recu event: OrderCreatedEvent pour orderId=..."

# restaurant-service :
#   "Restaurant recu event OrderCreatedEvent pour orderId=..."

# notification-service :
#   "Email: 'Votre commande ... a ete creee'"
```

### 5. Lire les events et consumer grougs Kafka

```bash
# Lire les events Kafka
docker exec -it quickbite-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --timeout-ms 5000
  
# Verifier les consumer groups  
docker exec quickbite-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list
```

### 6. Verifier la compensation (timeout)

> Les commandes en PAYMENT_PENDING depuis plus de 5 min sont automatiquement annulees par le `SagaTimeoutScheduler`.

```bash
# Attendre 5+ min, puis verifier le statut
curl -s http://localhost:8083/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN_CLIENT" | jq '.status'
# -> "CANCELLED" (si le PaymentService n'a pas repondu)
```

### Architecture Saga orchestree

```
               OrderService
              (Orchestrateur)
             /      |       \
            v       v        v
    PaymentSvc  RestaurantSvc  DeliverySvc
        |           |             |
        v           v             v
  payment-events  restaurant-events  delivery-events
        \           |             /
         \          |            /
          v         v           v
         OrderEventConsumer (order-group)
              -> met a jour le statut
              -> compense si echec
```

---

## Video 8 : Paiement - Idempotence et Webhooks Stripe

### 1. Configurer Stripe (pre-requis)

```bash
# Verifier que la cle Stripe est configuree dans payment-service/application.yaml
# stripe.secret-key et stripe.webhook-secret

# Installer Stripe CLI pour les webhooks locaux
# https://docs.stripe.com/stripe-cli
brew install stripe/stripe-cli/stripe

# Se connecter a Stripe
stripe login

# Ecouter les webhooks en local (dans un terminal dedie)
stripe listen --forward-to localhost:8086/api/webhooks/stripe
# -> Copier le whsec_... affiche et le mettre dans application.yaml (stripe.webhook-secret)
```

### 2. Lancer les services

```bash
# Infrastructure
docker compose up -d

# Lancer les services (dans des terminaux separes)
cd order-service       && mvn spring-boot:run
cd restaurant-service  && mvn spring-boot:run
cd payment-service     && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

### 3. Creer une commande (declenche le flux Saga + Paiement)

```bash
# Recuperer un token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password&client_id=quickbite-mobile&username=client1&password=password" \
  | jq -r '.access_token')

# Creer une commande chez Tokyo Ramen House
# Tonkotsu Ramen x2 (14.50) + Gyoza x1 (7.50) = 36.50
ORDER_ID=$(curl -s -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "a1b2c3d4-0001-4000-8000-000000000002",
    "deliveryAddress": "42 rue de la Paix, Paris",
    "items": [
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000001", "quantity": 2},
      {"menuItemId": "b1b2c3d4-0002-4000-8000-000000000003", "quantity": 1}
    ]
  }' | jq -r '.id')

echo "Order ID: $ORDER_ID"
```

### 4. Verifier le paiement en base

```bash
docker exec quickbite-postgres-payment psql -U quickbite -d quickbite-payment \
  -c "SELECT id, order_id, amount, status, stripe_payment_intent_id, idempotency_key FROM payments;"
```

### 5. Verifier le statut de la commande

```bash
curl -s http://localhost:8083/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.status'
# -> "PAYMENT_PENDING" (en attente du webhook Stripe)
```

### 6. Simuler un webhook Stripe (via Stripe CLI)

```bash
# Le webhook arrive automatiquement si Stripe CLI ecoute
# Sinon, declencher manuellement :
stripe trigger payment_intent.succeeded
```

### 7. Verifier les logs du flux complet

```bash
# order-service :
#   "OrderCreatedEvent publie pour orderId=..."
#   "Commande ... en attente de paiement -> PAYMENT_PENDING"

# payment-service :
#   "Payment recu event: OrderCreatedEvent pour orderId=..."
#   "PaymentIntent cree: pi_... pour order ..."
#   "Webhook recu: payment_intent.succeeded"

# notification-service :
#   "Email: 'Votre commande ... a ete creee'"
```

### Points cles - Architecture Paiement

```
Client -> OrderService -> Kafka (order-events) -> PaymentService -> Stripe API
                                                       |
                                                  Idempotency-Key
                                                  (order_pay_{orderId})
                                                       |
Stripe -> Webhook POST /api/webhooks/stripe -> PaymentService
              |                                    |
         Signature HMAC                    Deduplication stripeEventId
         (Stripe-Signature header)         (existsByStripeEventId)
```

- **Pre-autorisation + Capture** : on bloque le montant, on ne debite qu'apres confirmation restaurant (ADR #7)
- **Idempotency-Key** : chaque appel Stripe porte `order_pay_{orderId}`, pas de double PaymentIntent
- **Deduplication webhook** : le `stripeEventId` est stocke en base, les doublons sont ignores
- **Compensation** : si la Saga echoue apres paiement, on appelle `PaymentIntent.cancel()` (pas de refund)

---

## Video 9 : Cache distribue - Redis

### 1. Pre-requis

```bash
# Infrastructure (Redis deja inclus dans docker-compose)
docker compose up -d

# Verifier que Redis est UP
docker exec quickbite-redis redis-cli PING
# -> PONG

# Lancer le restaurant-service
cd restaurant-service && mvn spring-boot:run
```

### 2. Obtenir un token

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/quickbite/protocol/openid-connect/token \
  -d "grant_type=password&client_id=quickbite-mobile&username=client1&password=password" \
  | jq -r '.access_token')
```

### 3. Tester le cache (Cache-Aside Pattern)

```bash
# Vider le cache Redis
docker exec quickbite-redis redis-cli FLUSHDB

# Premier appel : CACHE MISS (log "CACHE MISS - Chargement menu depuis PostgreSQL")
curl -s http://localhost:8085/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items \
  -H "Authorization: Bearer $TOKEN" | jq '.[].name'

# Deuxieme appel : CACHE HIT (aucun log PostgreSQL, reponse plus rapide)
curl -s http://localhost:8085/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items \
  -H "Authorization: Bearer $TOKEN" | jq '.[].name'
```

### 4. Verifier dans Redis

```bash
# Lister les cles
docker exec quickbite-redis redis-cli KEYS "quickbite:*"
# -> "quickbite:menus::a1b2c3d4-0001-4000-8000-000000000002"

# Voir le contenu du cache (JSON avec @class pour le typage)
docker exec quickbite-redis redis-cli GET "quickbite:menus::a1b2c3d4-0001-4000-8000-000000000002"

# Voir le TTL restant (300 secondes = 5 min)
docker exec quickbite-redis redis-cli TTL "quickbite:menus::a1b2c3d4-0001-4000-8000-000000000002"
```

### 5. Tester l'invalidation (@CacheEvict + Kafka)

```bash
# Verifier que le cache existe
docker exec quickbite-redis redis-cli EXISTS "quickbite:menus::a1b2c3d4-0001-4000-8000-000000000002"
# -> 1

# Modifier un item du menu (declenche @CacheEvict + MenuUpdatedEvent sur Kafka)
curl -s -X PUT http://localhost:8085/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items/b1b2c3d4-0002-4000-8000-000000000001 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tonkotsu Ramen Special",
    "price": 16.50,
    "description": "Bouillon porc 14h, double chashu, oeuf mollet, nori, truffe",
    "available": true
  }' | jq .

# Verifier que le cache a ete invalide
docker exec quickbite-redis redis-cli EXISTS "quickbite:menus::a1b2c3d4-0001-4000-8000-000000000002"
# -> 0 (cache vide)

# Prochain appel : CACHE MISS (reconstruit avec les nouvelles donnees)
curl -s http://localhost:8085/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items \
  -H "Authorization: Bearer $TOKEN" | jq '.[0]'
```

### 6. Tester le stampede protection (sync=true)

```bash
# Vider le cache
docker exec quickbite-redis redis-cli FLUSHDB

# Envoyer 50 requetes simultanees (seule la premiere ira en DB)
for i in $(seq 1 50); do
  curl -s http://localhost:8085/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items \
    -H "Authorization: Bearer $TOKEN" -o /dev/null &
done
wait

# Verifier les logs : UN SEUL "CACHE MISS - Chargement menu depuis PostgreSQL"
# Les 49 autres ont attendu le lock et lu le cache
```

### 7. Verifier les logs

```bash
# restaurant-service :
#   Premier GET  : "CACHE MISS - Chargement menu depuis PostgreSQL pour restaurant a1b2c3d4-..."
#   Deuxieme GET : (rien — cache hit, pas de log)
#   PUT          : "Menu item modifie: b1b2c3d4-... pour restaurant a1b2c3d4-..."
#                  "MenuUpdatedEvent publie pour restaurant a1b2c3d4-..."
#   GET apres PUT: "CACHE MISS - Chargement menu depuis PostgreSQL pour restaurant a1b2c3d4-..."
```

### Points cles - Architecture Cache Redis

```
Client -> RestaurantService -> @Cacheable -> Redis (quickbite:menus::{id})
                                  |
                              CACHE HIT ? -> retourne le JSON depuis Redis
                              CACHE MISS ? -> PostgreSQL -> stocke dans Redis -> retourne
                                  |
                             sync=true (lock JVM pour eviter le stampede)

PUT /menu-items/{id} -> @CacheEvict -> supprime quickbite:menus::{restaurantId}
                             |
                        MenuUpdatedEvent -> Kafka (restaurant-events)
                             |
                        MenuCacheInvalidator -> @CacheEvict (autres instances)
```

- **Cache-Aside** : l'application controle le cache, pas la DB
- **TTL 5 min** : filet de securite, meme si l'invalidation echoue
- **sync=true** : un seul thread reconstruit le cache (stampede protection)
- **Invalidation event-driven** : Kafka propage l'invalidation a toutes les instances
- **Serialisation JSON** : GenericJacksonJsonRedisSerializer avec default typing (@class)

---

## Video 10 : Recherche - Elasticsearch

### Pre-requis

- Elasticsearch 8.13.4 demarre via `docker-compose up -d elasticsearch`
- restaurant-service demarre avec l'annotation `@EventListener(ApplicationReadyEvent.class)` sur `RestaurantSearchInitializer`
- Les 5 restaurants seed inseres via Flyway (V3)

### 1. Diagnostics - Verifier l'etat d'Elasticsearch

```bash
# Sante du cluster
curl -s "http://localhost:9200/_cluster/health" | jq .

# L'index existe-t-il ?
curl -s "http://localhost:9200/quickbite-restaurants" | jq .

# Nombre de documents indexes
curl -s "http://localhost:9200/quickbite-restaurants/_count" | jq .
# Attendu : {"count":5, ...}

# Voir un echantillon de documents
curl -s "http://localhost:9200/quickbite-restaurants/_search?pretty&size=1" | jq .
```

### 2. Verifier le bootstrap au demarrage

```bash
# Logs du restaurant-service au demarrage
# Chercher ces deux lignes :
#   "=== Bootstrap Elasticsearch : indexation de tous les restaurants ==="
#   "=== Bootstrap termine : 5 restaurants indexes ==="

docker logs quickbite-restaurant-service 2>&1 | grep -i "bootstrap"
```

### 3. Nettoyer un document de test

```bash
# Supprimer un document de test (si indexe manuellement)
curl -s -X DELETE "http://localhost:9200/quickbite-restaurants/_doc/test1" | jq .
```

### 4. Recherche full-text

```bash
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/quickbite/protocol/openid-connect/token" \
  -d "client_id=quickbite-mobile" \
  -d "grant_type=password" \
  -d "username=client1" \
  -d "password=password" | jq -r '.access_token')

# Recherche "ramen"
curl -s "http://localhost:8085/api/search/restaurants?q=ramen" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Recherche "pizza"
curl -s "http://localhost:8085/api/search/restaurants?q=pizza" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 5. Recherche fuzzy (tolerance aux fautes)

```bash
# "piza" avec un seul z -> doit trouver "Pizza Napoli"
curl -s "http://localhost:8085/api/search/restaurants?q=piza" \
  -H "Authorization: Bearer $TOKEN" | jq .

# "ramn" -> doit trouver "Tokyo Ramen House"
curl -s "http://localhost:8085/api/search/restaurants?q=ramn" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 6. Recherche geo (par distance)

```bash
# Restaurants dans un rayon de 5 km autour de (48.8566, 2.3522) - centre de Paris
curl -s "http://localhost:8085/api/search/restaurants?q=restaurant&lat=48.8566&lon=2.3522&radius=5" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 7. Recherche avec filtres

```bash
# Filtrer par type de cuisine
curl -s "http://localhost:8085/api/search/restaurants?q=ramen&cuisine=Japonais" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Filtrer par note minimale
curl -s "http://localhost:8085/api/search/restaurants?q=ramen&minRating=4.0" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 8. Synchronisation event-driven (Kafka -> Elasticsearch)

```bash
# 1. Modifier un menu item via l'API (declenche un event Kafka)
curl -s -X PUT "http://localhost:8080/api/restaurants/a1b2c3d4-0001-4000-8000-000000000002/menu-items/b1b2c3d4-0002-4000-8000-000000000001" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Tonkotsu Ramen UPDATED","price":17.0,"category":"Ramen"}' | jq .

# 2. Attendre 2 secondes (propagation Kafka)
sleep 2

# 3. Verifier que le menu item est mis a jour dans Elasticsearch (recherche nested)
curl -s "http://localhost:8085/api/search/restaurants?q=UPDATED" \
  -H "Authorization: Bearer $TOKEN" | jq .
# Attendu : Tokyo Ramen House avec "Tonkotsu Ramen UPDATED" dans menuItems

# 4. Verifier que la recherche par nom de restaurant fonctionne toujours
curl -s "http://localhost:8085/api/search/restaurants?q=ramen" \
  -H "Authorization: Bearer $TOKEN" | jq .
# Attendu : Tokyo Ramen House (match sur name + description)
```

### 9. Re-indexation complete (si necessaire)

```bash
# Supprimer l'index et redemarrer le service pour re-indexer
curl -s -X DELETE "http://localhost:9200/quickbite-restaurants" | jq .

# Redemarrer le restaurant-service -> RestaurantSearchInitializer re-indexe tout
docker restart quickbite-restaurant-service

# Verifier apres ~10 secondes
sleep 10
curl -s "http://localhost:9200/quickbite-restaurants/_count" | jq .
```

### Points cles - Architecture Elasticsearch

```
WRITE PATH (event-driven) :
  RestaurantService -> PostgreSQL (source de verite)
        |
        v
  KafkaProducer -> topic "restaurant-event"
        |
        v
  SearchIndexer (@KafkaListener) -> Elasticsearch (index quickbite-restaurants)

READ PATH (recherche) :
  Client -> Gateway (8080) -> SearchController -> SearchService -> Elasticsearch
                                                      |
                                              full-text + geo + fuzzy + filtres
```

- **PostgreSQL = source de verite** : on ne fait jamais de Dual Write
- **Event-driven sync** : Kafka garantit la coherence eventuelle (replay si ES down)
- **Inverted index** : recherche full-text en O(1) vs LIKE en O(n)
- **geo_point** : filtre par distance sans calcul cote application
- **Fuzzy matching** : fuzziness=AUTO tolere 1-2 fautes de frappe
- **Bootstrap** : `RestaurantSearchInitializer` indexe tous les restaurants existants au demarrage

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
