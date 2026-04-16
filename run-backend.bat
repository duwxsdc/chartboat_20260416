@echo off
echo ============================================
echo   AI Assistant - Backend Build and Run
echo ============================================
echo.

cd backend

echo [1/3] Checking Java version...
java -version
if errorlevel 1 (
    echo ERROR: Java 21 is required!
    pause
    exit /b 1
)

echo.
echo [2/3] Building project with Maven...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo [3/3] Starting Spring Boot application...
echo.
echo ============================================
echo   Server starting on http://localhost:8080
echo   API: http://localhost:8080/api
echo   Press Ctrl+C to stop
echo ============================================
echo.

java -jar target\ai-assistant-1.0.0.jar

pause
