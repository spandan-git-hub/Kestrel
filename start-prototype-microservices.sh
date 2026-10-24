#!/usr/bin/env bash
# =====================================================================
#                KESTREL MULTI-HAZARD ALERT RELAY
#             Microservices Prototype Orchestration Stack
# =====================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$SCRIPT_DIR/backend/pom.xml" ]; then
    ROOT_DIR="$SCRIPT_DIR"
elif [ -f "$SCRIPT_DIR/../backend/pom.xml" ]; then
    ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
else
    echo "[ERROR] Could not locate Kestrel root directory."
    exit 1
fi

echo "====================================================================="
echo "Project Root: $ROOT_DIR"
echo "====================================================================="

# Check Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 21+ is required but 'java' was not found in PATH."
    exit 1
fi
java -version 2>&1 | head -n 1

# Check Node
if ! command -v node &> /dev/null; then
    echo "[ERROR] Node.js is required but 'node' was not found in PATH."
    exit 1
fi
node -v

mkdir -p "$ROOT_DIR/logs"
PID_FILE="$ROOT_DIR/.kestrel_pids"
> "$PID_FILE"

echo "====================================================================="
echo "[1/6] Building Maven Multi-Module Microservices (skipping tests)..."
echo "====================================================================="
cd "$ROOT_DIR/backend"
if [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    mvn clean package -DskipTests
fi

echo "====================================================================="
echo "[2/6] Starting Downstream Microservices..."
echo "====================================================================="

echo "[*] Starting Subscriber Registry (Port 8081)..."
java -jar services/subscriber-registry/target/subscriber-registry-0.0.1-SNAPSHOT.jar > "$ROOT_DIR/logs/subscriber-registry.log" 2>&1 &
echo $! >> "$PID_FILE"

echo "[*] Starting Flood Ingest (Port 8082)..."
java -jar services/flood-ingest/target/flood-ingest-0.0.1-SNAPSHOT.jar > "$ROOT_DIR/logs/flood-ingest.log" 2>&1 &
echo $! >> "$PID_FILE"

echo "[*] Starting Rules Engine (Port 8083)..."
java -jar services/rules-engine/target/rules-engine-0.0.1-SNAPSHOT.jar > "$ROOT_DIR/logs/rules-engine.log" 2>&1 &
echo $! >> "$PID_FILE"

echo "[*] Starting Alert Fanout (Port 8084)..."
java -jar services/alert-fanout/target/alert-fanout-0.0.1-SNAPSHOT.jar > "$ROOT_DIR/logs/alert-fanout.log" 2>&1 &
echo $! >> "$PID_FILE"

echo "[*] Waiting 8 seconds for downstream microservices to initialize..."
sleep 8

echo "====================================================================="
echo "[3/6] Starting Spring Cloud Gateway (Port 8080)..."
echo "====================================================================="
java -jar services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar > "$ROOT_DIR/logs/api-gateway.log" 2>&1 &
echo $! >> "$PID_FILE"

echo "[*] Waiting 4 seconds for API Gateway to initialize..."
sleep 4

echo "====================================================================="
echo "[4/6] Starting Vite React Redux Frontend (Port 5173)..."
echo "====================================================================="
cd "$ROOT_DIR/frontend"
npm run dev > "$ROOT_DIR/logs/frontend.log" 2>&1 &
echo $! >> "$PID_FILE"

echo "====================================================================="
echo "         KESTREL MICROSERVICES PROTOTYPE IS NOW RUNNING!"
echo "====================================================================="
echo "  - Frontend PWA:         http://localhost:5173"
echo "  - Spring Cloud Gateway: http://localhost:8080"
echo "  - Logs Directory:       $ROOT_DIR/logs/"
echo ""
echo "To shut down all services cleanly, run:"
echo "  ./stop-prototype-microservices.sh"
echo "====================================================================="
