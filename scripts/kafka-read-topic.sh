#!/bin/bash
# Lire les messages d'un topic Kafka
# Usage : ./kafka-read-topic.sh [topic] [timeout_ms]
# Exemple : ./kafka-read-topic.sh order-events 5000

TOPIC=${1:-order-events}
TIMEOUT=${2:-5000}

echo "=== Lecture du topic '$TOPIC' (timeout ${TIMEOUT}ms) ==="
docker exec -it quickbite-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic "$TOPIC" \
  --from-beginning \
  --timeout-ms "$TIMEOUT"
