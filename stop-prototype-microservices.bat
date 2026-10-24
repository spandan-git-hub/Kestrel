@echo off
setlocal enabledelayedexpansion

echo =====================================================================
echo                KESTREL MULTI-HAZARD ALERT RELAY
echo             Stopping Microservices Prototype Stack
echo =====================================================================
echo.

echo [*] Terminating processes on ports 8080, 8081, 8082, 8083, 8084, 5173...

powershell -NoProfile -Command ^
  "$ports = @(8080, 8081, 8082, 8083, 8084, 5173);" ^
  "foreach ($p in $ports) {" ^
  "  $conns = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue;" ^
  "  if ($conns) {" ^
  "    $conns | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {" ^
  "      try {" ^
  "        $proc = Get-Process -Id $_ -ErrorAction SilentlyContinue;" ^
  "        if ($proc) {" ^
  "          Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue;" ^
  "          Write-Host ('[STOPPED] Killed process ' + $_ + ' (' + $proc.ProcessName + ') on port ' + $p) -ForegroundColor Green;" ^
  "        }" ^
  "      } catch {" ^
  "        Write-Host ('[WARNING] Failed to terminate PID ' + $_ + ' on port ' + $p) -ForegroundColor Yellow;" ^
  "      }" ^
  "    }" ^
  "  } else {" ^
  "    Write-Host ('[CLEAN] No active process on port ' + $p) -ForegroundColor Gray;" ^
  "  }" ^
  "}"

echo.
echo =====================================================================
echo Kestrel microservices and frontend have been stopped.
echo =====================================================================
pause
