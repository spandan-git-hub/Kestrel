#!/usr/bin/env bash
# =====================================================================
#                KESTREL MULTI-HAZARD ALERT RELAY
#             Stopping Microservices Prototype Stack
# =====================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$SCRIPT_DIR/backend/pom.xml" ]; then
    ROOT_DIR="$SCRIPT_DIR"
elif [ -f "$SCRIPT_DIR/../backend/pom.xml" ]; then
    ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
else
    ROOT_DIR="$SCRIPT_DIR"
fi

PID_FILE="$ROOT_DIR/.kestrel_pids"

echo "[*] Stopping tracked Kestrel processes..."
if [ -f "$PID_FILE" ]; then
    while IFS= read -r pid; do
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            echo "[STOPPED] Killed PID $pid"
        fi
    done < "$PID_FILE"
    rm -f "$PID_FILE"
fi

echo "[*] Cleaning up any remaining processes on ports 8080-8084, 5173..."
for port in 8080 8081 8082 8083 8084 5173; do
    if command -v lsof &> /dev/null; then
        pids=$(lsof -ti :"$port" 2>/dev/null || true)
        if [ -n "$pids" ]; then
            kill -9 $pids 2>/dev/null || true
            echo "[STOPPED] Killed process on port $port"
        fi
    fi
done

echo "====================================================================="
echo "Kestrel microservices and frontend have been stopped."
echo "====================================================================="
