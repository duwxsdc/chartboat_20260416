@echo off
echo =============================================
echo   AI Assistant - Stop Services Script
echo =============================================
echo.
echo Stopping backend service (8080 port)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)
echo Stopping frontend service (5173 and 5174 ports)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173 :5174" ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)
echo.
echo Verifying service status...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [OK] Backend service stopped
) else (
    echo [FAIL] Backend service still running
)
netstat -ano | findstr ":5173 :5174" | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo [OK] Frontend service stopped
) else (
    echo [FAIL] Frontend service still running
)
echo.
echo =============================================
echo   Operation completed
echo =============================================
pause