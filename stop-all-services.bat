@echo off
echo Stopping all running microservices ports (8761, 8888, 8080, 8081, 8082, 8083, 8084, 8085)...

for %%p in (8761 8888 8080 8081 8082 8083 8084 8085) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :%%p ^| findstr LISTENING') do (
        echo Killing process PID %%a on port %%p...
        taskkill /F /PID %%a 2>nul
    )
)

echo All microservices stopped successfully.
pause
