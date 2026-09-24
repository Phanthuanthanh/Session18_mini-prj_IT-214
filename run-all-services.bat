@echo off
echo ========================================================
echo   RikkeiBank Microservices Architecture Launcher
echo ========================================================

if not exist "discovery-service\build\libs\discovery-service-1.0.0-SNAPSHOT.jar" (
    echo Building all microservices jars... Please wait...
    call gradlew.bat assemble -x test
)

echo [1/8] Starting Discovery Service (Eureka Server: 8761)...
start "1. Discovery Service (8761)" cmd /k "java -jar discovery-service\build\libs\discovery-service-1.0.0-SNAPSHOT.jar"
echo Waiting 8s for Eureka to initialize...
timeout /t 8 /nobreak >nul

echo [2/8] Starting Config Service (8888)...
start "2. Config Service (8888)" cmd /k "java -jar config-service\build\libs\config-service-1.0.0-SNAPSHOT.jar"
timeout /t 4 /nobreak >nul

echo [3/8] Starting Gateway Service (8080)...
start "3. Gateway Service (8080)" cmd /k "java -jar gateway-service\build\libs\gateway-service-1.0.0-SNAPSHOT.jar"

echo [4/8] Starting Identity Service (8081)...
start "4. Identity Service (8081)" cmd /k "java -jar identity-service\build\libs\identity-service-1.0.0-SNAPSHOT.jar"

echo [5/8] Starting Customer Service (8082)...
start "5. Customer Service (8082)" cmd /k "java -jar customer-service\build\libs\customer-service-1.0.0-SNAPSHOT.jar"

echo [6/8] Starting Account Service (8083)...
start "6. Account Service (8083)" cmd /k "java -jar account-service\build\libs\account-service-1.0.0-SNAPSHOT.jar"

echo [7/8] Starting Transaction Service (8084)...
start "7. Transaction Service (8084)" cmd /k "java -jar transaction-service\build\libs\transaction-service-1.0.0-SNAPSHOT.jar"

echo [8/8] Starting Notification Service (8085)...
start "8. Notification Service (8085)" cmd /k "java -jar notification-service\build\libs\notification-service-1.0.0-SNAPSHOT.jar"

echo ========================================================
echo   All 8 Microservices are running!
echo   Eureka Dashboard: http://localhost:8761
echo   API Gateway URL:  http://localhost:8080
echo ========================================================
pause
