@echo off
echo ============================================
echo   AI Assistant - Frontend Dev Server
echo ============================================
echo.

cd frontend

echo [1/2] Installing dependencies...
call npm install
if errorlevel 1 (
    echo ERROR: npm install failed!
    pause
    exit /b 1
)

echo.
echo [2/2] Starting Vite dev server...
echo.
echo ============================================
echo   Frontend: http://localhost:5173
echo   Backend API proxy enabled
echo   Press Ctrl+C to stop
echo ============================================
echo.

call npm run dev

pause
