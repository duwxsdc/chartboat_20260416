@echo off
echo ============================================
echo   AI Assistant - One-Click Deploy
echo ============================================
echo.

echo This script will:
echo   1. Build and start the backend (Spring Boot)
echo   2. Install and start the frontend (Vue + Vite)
echo.
echo Prerequisites:
echo   - Java 21+
echo   - Node.js 18+
echo   - Maven 3.6+
echo.
pause

echo.
echo ============================================
echo   Step 1: Starting Backend...
echo ============================================
echo.

start "AI Assistant Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"

echo Waiting for backend to start...
timeout /t 10 /nobreak > nul

echo.
echo ============================================
echo   Step 2: Starting Frontend...
echo ============================================
echo.

start "AI Assistant Frontend" cmd /k "cd /d %~dp0frontend && npm install && npm run dev"

echo.
echo ============================================
echo   Deployment Complete!
echo ============================================
echo.
echo   Backend:  http://localhost:8080/api
echo   Frontend: http://localhost:5173
echo.
echo   Open http://localhost:5173 in your browser
echo.
pause
