#!/usr/bin/env bash
# =====================================================================
#             KESTREL AUTOMATED LIVE E2E INTEGRATION TEST (PT4-02)
# =====================================================================

set -e

GATEWAY_URL="http://localhost:8080/api/v1"

echo "====================================================================="
echo "        KESTREL PROTOTYPE AUTOMATED E2E INTEGRATION TEST             "
echo "====================================================================="
echo ""

# 1. Connectivity check
echo "[1/5] Checking Gateway connectivity..."
if ! curl -sf "$GATEWAY_URL/rules/status" > /dev/null; then
    echo "[FAIL] Cannot reach Gateway at $GATEWAY_URL/rules/status"
    echo "Please ensure the microservices stack is running (start-prototype-microservices.sh)."
    exit 1
fi
echo "  -> Gateway and Rules Engine reachable."

# 2. Simulate 5-Year Flood Warning on Kurla
echo "[2/5] Simulating 5-Year Flood Scenario on Kurla Ward L, Mumbai..."
SIM_RESP=$(curl -sf -X POST "$GATEWAY_URL/rules/simulate" \
    -H "Content-Type: application/json" \
    -d '{"locationId":"mumbai-kurla","scenario":"WARNING_5YR"}')
echo "  -> Simulation accepted: $SIM_RESP"

# 3. Verify Rules Engine transitions to WARNING
echo "[3/5] Verifying Rules Engine transitions to WARNING..."
sleep 1
STATUS_RESP=$(curl -sf "$GATEWAY_URL/rules/status")
if echo "$STATUS_RESP" | grep -q '"locationId":"mumbai-kurla"' && echo "$STATUS_RESP" | grep -q '"severity":"WARNING"'; then
    echo "  -> Verified: mumbai-kurla severity is WARNING"
else
    echo "[FAIL] Expected severity WARNING for mumbai-kurla"
    exit 1
fi

# 4. Submit Community Ground Report
echo "[4/5] Submitting community ground report to upgrade provenance..."
REPORT_RESP=$(curl -sf -X POST "$GATEWAY_URL/fanout/reports" \
    -H "Content-Type: application/json" \
    -d '{"locationId":"mumbai-kurla","category":"WATER_RISING","description":"Live test report: water level rising along Mithi riverbank","reporterAlias":"E2E Reporter"}')
if echo "$REPORT_RESP" | grep -q '"newProvenance":"MODEL_PLUS_REPORTS"'; then
    echo "  -> Verified: Provenance immediately upgraded to MODEL_PLUS_REPORTS"
else
    echo "[FAIL] Expected MODEL_PLUS_REPORTS in response"
    exit 1
fi

# 5. Trigger Broadcast and verify fanout queue
echo "[5/5] Triggering coordinator broadcast and verifying fanout queue..."
BC_RESP=$(curl -sf -X POST "$GATEWAY_URL/fanout/broadcast" \
    -H "Content-Type: application/json" \
    -d '{"locationId":"mumbai-kurla","hazard":"FLOOD","severity":"WARNING","customNote":"Automated test broadcast","channels":["TELEGRAM","MOCK"]}')
echo "  -> Broadcast dispatched: $BC_RESP"

sleep 1
RECENT_RESP=$(curl -sf "$GATEWAY_URL/fanout/recent?limit=10")
if echo "$RECENT_RESP" | grep -q '"locationId":"mumbai-kurla"' && echo "$RECENT_RESP" | grep -q '"severity":"WARNING"'; then
    echo "  -> Verified: Dispatch logged in Mock fanout queue with WARNING severity!"
else
    echo "[FAIL] Dispatch not found in recent queue"
    exit 1
fi

echo ""
echo "====================================================================="
echo "    ALL E2E INTEGRATION CHECKS PASSED (5-Yr Flood -> Upgrade -> Queue) "
echo "====================================================================="
