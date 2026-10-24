@echo off
setlocal enabledelayedexpansion

echo =====================================================================
echo                KESTREL MULTI-HAZARD ALERT RELAY
echo             Microservices Prototype Orchestration Stack
echo =====================================================================
echo.

rem Determine project root directory
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%backend\pom.xml" (
    set "ROOT_DIR=%SCRIPT_DIR%"
) else if exist "%SCRIPT_DIR%..\backend\pom.xml" (
    set "ROOT_DIR=%SCRIPT_DIR%..\"
) else (
    echo [ERROR] Could not locate Kestrel root directory.
    pause
    exit /b 1
)

echo [*] Project Root: %ROOT_DIR%
echo.

rem Check Java
echo [*] Checking Java environment...
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java 21 is required but 'java' command was not found in PATH.
    pause
    exit /b 1
)
java -version 2>&1 | findstr /i "version"
echo.

rem Check Node
echo [*] Checking Node.js environment...
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is required for Vite frontend but 'node' was not found in PATH.
    pause
    exit /b 1
)
node -v
echo.

rem Build Maven monorepo
echo =====================================================================
echo [1/6] Building Maven Multi-Module Microservices (skipping tests)...
echo =====================================================================
cd /d "%ROOT_DIR%backend"
if exist "mvnw.cmd" (
    call mvnw.cmd clean package -DskipTests
) else (
    call mvn clean package -DskipTests
)
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo =====================================================================
echo [2/6] Starting Downstream Microservices...
echo =====================================================================

echo [*] Starting Subscriber Registry (Port 8081)...
start "Kestrel: Subscriber Registry (8081)" cmd /k "cd /d "%ROOT_DIR%backend" && java -jar services/subscriber-registry/target/subscriber-registry-0.0.1-SNAPSHOT.jar"

echo [*] Starting Flood Ingest (Port 8082)...
start "Kestrel: Flood Ingest (8082)" cmd /k "cd /d "%ROOT_DIR%backend" && java -jar services/flood-ingest/target/flood-ingest-0.0.1-SNAPSHOT.jar"

echo [*] Starting Rules Engine (Port 8083)...
start "Kestrel: Rules Engine (8083)" cmd /k "cd /d "%ROOT_DIR%backend" && java -jar services/rules-engine/target/rules-engine-0.0.1-SNAPSHOT.jar"

echo [*] Starting Alert Fanout (Port 8084)...
start "Kestrel: Alert Fanout (8084)" cmd /k "cd /d "%ROOT_DIR%backend" && java -jar services/alert-fanout/target/alert-fanout-0.0.1-SNAPSHOT.jar"

echo.
echo [*] Waiting 8 seconds for downstream microservices to initialize...
timeout /t 8 /nobreak > nul

echo =====================================================================
echo [3/6] Starting Spring Cloud Gateway (Port 8080)...
echo =====================================================================
start "Kestrel: API Gateway (8080)" cmd /k "cd /d "%ROOT_DIR%backend" && java -jar services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar"

echo.
echo [*] Waiting 4 seconds for API Gateway to initialize...
timeout /t 4 /nobreak > nul

echo =====================================================================
echo [4/6] Starting Vite React Redux Frontend (Port 5173)...
echo =====================================================================
cd /d "%ROOT_DIR%frontend"
start "Kestrel: Frontend (5173)" cmd /k "npm run dev"

echo.
echo =====================================================================
echo          KESTREL MICROSERVICES PROTOTYPE IS NOW RUNNING!
echo =====================================================================
echo.
echo   - Frontend PWA:         http://localhost:5173
echo   - Spring Cloud Gateway: http://localhost:8080
echo.
echo Downstream Microservice Direct Access / Actuator Health:
echo   - Subscriber Registry:  http://localhost:8081/actuator/health
echo   - Flood Ingest:         http://localhost:8082/actuator/health
echo   - Rules Engine:         http://localhost:8083/actuator/health
echo   - Alert Fanout:         http://localhost:8084/actuator/health
echo.
echo To shut down all services cleanly, run:
echo   stop-prototype-microservices.bat
echo.
echo =====================================================================
pause
