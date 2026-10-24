# =====================================================================
#             KESTREL AUTOMATED LIVE E2E INTEGRATION TEST (PT4-02)
# =====================================================================
# Run against the active microservices stack (Gateway on port 8080)
# Usage: powershell -ExecutionPolicy Bypass -File test-e2e-scenario.ps1
# =====================================================================

$ErrorActionPreference = "Stop"
$GATEWAY_URL = "http://localhost:8080/api/v1"

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "        KESTREL PROTOTYPE AUTOMATED E2E INTEGRATION TEST             " -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

# Check connectivity
Write-Host "[1/5] Checking Gateway connectivity..." -ForegroundColor Yellow
try {
    $status = Invoke-RestMethod -Uri "$GATEWAY_URL/rules/status" -Method Get -TimeoutSec 5
    Write-Host "  -> Gateway and Rules Engine reachable. Found $($status.Count) monitored locations." -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Cannot reach Gateway at $GATEWAY_URL/rules/status" -ForegroundColor Red
    Write-Host "  Please ensure the microservices stack is running (start-prototype-microservices.bat)." -ForegroundColor Yellow
    exit 1
}

# Step 1: Simulate 5-Year Flood Warning on Kurla Ward
Write-Host "[2/5] Simulating 5-Year Flood Scenario on Kurla Ward L, Mumbai..." -ForegroundColor Yellow
$simPayload = @{
    locationId = "mumbai-kurla"
    scenario   = "WARNING_5YR"
} | ConvertTo-Json

try {
    $simResp = Invoke-RestMethod -Uri "$GATEWAY_URL/rules/simulate" -Method Post -Body $simPayload -ContentType "application/json"
    Write-Host "  -> Simulation accepted: $($simResp.message)" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Simulation trigger failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Verify Rules Engine transitions to WARNING
Write-Host "[3/5] Verifying Rules Engine transitions to WARNING..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
$statusList = Invoke-RestMethod -Uri "$GATEWAY_URL/rules/status" -Method Get
$kurlaStatus = $statusList | Where-Object { $_.locationId -eq "mumbai-kurla" }

if ($kurlaStatus.severity -eq "WARNING") {
    Write-Host "  -> Verified: mumbai-kurla severity is WARNING (Peak share: $($kurlaStatus.peakExceedanceShare))" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Expected severity WARNING but found $($kurlaStatus.severity)" -ForegroundColor Red
    exit 1
}

# Step 3: Submit Community Ground Report
Write-Host "[4/5] Submitting community ground report to upgrade provenance..." -ForegroundColor Yellow
$reportPayload = @{
    locationId    = "mumbai-kurla"
    category      = "WATER_RISING"
    description   = "Live E2E test report: water level rising along Mithi riverbank near CST Road."
    reporterAlias = "E2E Automated Reporter"
} | ConvertTo-Json

try {
    $reportResp = Invoke-RestMethod -Uri "$GATEWAY_URL/fanout/reports" -Method Post -Body $reportPayload -ContentType "application/json"
    if ($reportResp.newProvenance -eq "MODEL_PLUS_REPORTS") {
        Write-Host "  -> Verified: Provenance immediately upgraded to $($reportResp.newProvenance)" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Expected MODEL_PLUS_REPORTS but got $($reportResp.newProvenance)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  [FAIL] Report submission failed: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Trigger Broadcast and verify fanout queue
Write-Host "[5/5] Triggering coordinator broadcast and verifying fanout queue..." -ForegroundColor Yellow
$broadcastPayload = @{
    locationId = "mumbai-kurla"
    hazard     = "FLOOD"
    severity   = "WARNING"
    customNote = "Automated E2E Test Broadcast: evacuate ground floor if water breaches threshold."
    channels   = @("TELEGRAM", "MOCK")
} | ConvertTo-Json

try {
    $bcResp = Invoke-RestMethod -Uri "$GATEWAY_URL/fanout/broadcast" -Method Post -Body $broadcastPayload -ContentType "application/json"
    Write-Host "  -> Broadcast dispatched! ID: $($bcResp.broadcastId) Dispatched count: $($bcResp.dispatchedCount)" -ForegroundColor Green
    Write-Host "  -> WhatsApp Share Link generated: $($bcResp.whatsAppShareUrl.Substring(0, 45))..." -ForegroundColor DarkGray

    Start-Sleep -Milliseconds 500
    $recentQueue = Invoke-RestMethod -Uri "$GATEWAY_URL/fanout/recent?limit=10" -Method Get
    $matched = $recentQueue | Where-Object { $_.locationId -eq "mumbai-kurla" -and $_.severity -eq "WARNING" }

    if ($matched) {
        Write-Host "  -> Verified: Dispatch logged in Mock fanout queue with provenance $($matched[0].provenance)!" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Dispatch was not found in fanout recent queue." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  [FAIL] Broadcast execution failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host "    ALL E2E INTEGRATION CHECKS PASSED (5-Yr Flood -> Upgrade -> Queue) " -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Green
