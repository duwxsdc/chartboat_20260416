@echo off
chcp 65001 >nul
title AI Assistant - 启动服务
echo.
echo ========================================
echo    AI Assistant 一键部署
echo ========================================
echo.

REM 检查环境
echo [检查] 正在检查环境...

REM 检查 Node.js
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未安装 Node.js，请先安装
    echo.
    pause
    exit /b 1
)

REM 检查 Maven
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未安装 Maven，请先安装
    echo.
    pause
    exit /b 1
)

echo [OK] Node.js: 
node --version
echo [OK] Maven 已安装
echo.

echo ========================================
echo.
echo 正在启动服务...
echo   后端: http://localhost:8080
echo   前端: http://localhost:5173
echo.
echo 按任意键继续...
pause >nul

REM 启动后端
echo.
echo [1/2] 启动后端服务...
start "AI-Backend" cmd /k "cd /d %~dp0backend && mvn spring-boot:run"

echo.
echo [2/2] 启动前端服务...
timeout /t 5 /nobreak >nul
start "AI-Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"

echo.
echo ========================================
echo    服务启动中，请稍候...
echo ========================================
echo.
echo 访问地址: http://localhost:5173
echo.
echo 测试账号:
echo   用户名: test    密码: test123
echo.
echo 按任意键打开浏览器...
pause >nul
start http://localhost:5173

echo.
echo 完成！请查看服务窗口查看日志
pause
