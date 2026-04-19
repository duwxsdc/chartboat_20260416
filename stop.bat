@echo off
chcp 65001 >nul 2>&1
title AI Assistant - Stop Services
echo.
echo ========================================
echo    AI Assistant - Stop All Services
echo ========================================
echo.

echo Stopping all services...
echo.

REM Stop frontend on port 5173
echo [1/3] Stopping frontend (port 5173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

REM Stop frontend on port 5174
echo [2/3] Stopping frontend (port 5174)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5174 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

REM Stop backend on port 8080
echo [3/3] Stopping backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo ========================================
echo    All services stopped
echo ========================================
echo.
pause
