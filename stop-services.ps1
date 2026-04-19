# AI Assistant - 停止前后端服务脚本
# 支持 Windows PowerShell 5.1+
# 使用方法: .\stop-services.ps1

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   AI Assistant 服务停止脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Continue"
$stoppedServices = @()
$failedServices = @()

# 检测操作系统
$isWindows = $PSVersionTable.Platform -eq "Win32NT" -or $null -eq $PSVersionTable.Platform
$isMacOS = $PSVersionTable.Platform -eq "Darwin"
$isLinux = $PSVersionTable.Platform -eq "Unix" -and -not $isMacOS

Write-Host "[检测] 操作系统: $((Get-CimInstance Win32_OperatingSystem).Caption)" -ForegroundColor Yellow
Write-Host ""

# 停止前端服务
Write-Host "[步骤 1/3] 停止前端服务..." -ForegroundColor Yellow

# 停止 Node.js 前端开发服务器
$frontendProcesses = @(
    @{Name="npm"; Description="npm dev server"},
    @{Name="yarn"; Description="yarn dev server"},
    @{Name="pnpm"; Description="pnpm dev server"},
    @{Name="node"; Description="Node.js server (Vite)"},
    @{Name="vite"; Description="Vite dev server"}
)

$frontendStopped = $false
foreach ($proc in $frontendProcesses) {
    if ($proc.Name -eq "node") {
        # 特别处理 node 进程，查找监听前端端口的进程
        $nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue | Where-Object {
            $cmd = $_.CommandLine
            if ($cmd -match "vite|npm|yarn|pnpm") { $true } else { $false }
        }
        foreach ($p in $nodeProcesses) {
            try {
                Write-Host "  - 停止 Node.js 进程 (PID: $($p.Id))" -ForegroundColor White
                Stop-Process -Id $p.Id -Force -ErrorAction Stop
                $stoppedServices += "Node.js (Vite) - PID: $($p.Id)"
                $frontendStopped = $true
            } catch {
                Write-Host "  - 停止 Node.js 进程失败: $($_.Exception.Message)" -ForegroundColor Red
                $failedServices += "Node.js (Vite) - PID: $($p.Id)"
            }
        }
    } else {
        $processes = Get-Process -Name $proc.Name -ErrorAction SilentlyContinue
        foreach ($p in $processes) {
            try {
                $cmdLine = if ($p.CommandLine) { $p.CommandLine } else { "" }
                # 检查是否是与项目相关的进程
                if ($cmdLine -match "vite|dev|start|npm run" -or $proc.Name -eq "vite") {
                    Write-Host "  - 停止 $($proc.Description) (PID: $($p.Id))" -ForegroundColor White
                    Stop-Process -Id $p.Id -Force -ErrorAction Stop
                    $stoppedServices += "$($proc.Description) - PID: $($p.Id)"
                    $frontendStopped = $true
                }
            } catch {
                Write-Host "  - 停止 $($proc.Description) 失败: $($_.Exception.Message)" -ForegroundColor Red
                $failedServices += "$($proc.Description)"
            }
        }
    }
}

if (-not $frontendStopped) {
    Write-Host "  [跳过] 未检测到运行的前端服务" -ForegroundColor Gray
}

# 停止后端服务
Write-Host ""
Write-Host "[步骤 2/3] 停止后端服务..." -ForegroundColor Yellow

# 停止 Java Spring Boot 服务
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $cmd = $_.CommandLine
    if ($cmd -match "ai-assistant|spring-boot|maven|spring-boot:run") { $true } else { $false }
}

$backendStopped = $false
foreach ($p in $javaProcesses) {
    try {
        Write-Host "  - 停止 Java Spring Boot 服务 (PID: $($p.Id))" -ForegroundColor White
        Stop-Process -Id $p.Id -Force -ErrorAction Stop
        $stoppedServices += "Java Spring Boot - PID: $($p.Id)"
        $backendStopped = $true
    } catch {
        Write-Host "  - 停止 Java Spring Boot 服务失败: $($_.Exception.Message)" -ForegroundColor Red
        $failedServices += "Java Spring Boot - PID: $($p.Id)"
    }
}

# 停止其他常见后端进程
$backendProcesses = @(
    @{Name="java"; Description="Java Application"},
    @{Name="python"; Description="Python Service"},
    @{Name="python3"; Description="Python3 Service"},
    @{Name="dotnet"; Description=".NET Service"}
)

foreach ($proc in $backendProcesses) {
    if ($proc.Name -eq "java") { continue } # Java进程已单独处理
    
    $processes = Get-Process -Name $proc.Name -ErrorAction SilentlyContinue
    foreach ($p in $processes) {
        try {
            $cmdLine = if ($p.CommandLine) { $p.CommandLine } else { "" }
            if ($cmdLine -match "ai-assistant|backend|server|app.py|main.py|api") {
                Write-Host "  - 停止 $($proc.Description) (PID: $($p.Id))" -ForegroundColor White
                Stop-Process -Id $p.Id -Force -ErrorAction Stop
                $stoppedServices += "$($proc.Description) - PID: $($p.Id)"
                $backendStopped = $true
            }
        } catch {
            Write-Host "  - 停止 $($proc.Description) 失败: $($_.Exception.Message)" -ForegroundColor Red
            $failedServices += "$($proc.Description)"
        }
    }
}

if (-not $backendStopped) {
    Write-Host "  [跳过] 未检测到运行的后端服务" -ForegroundColor Gray
}

# 释放端口
Write-Host ""
Write-Host "[步骤 3/3] 释放端口..." -ForegroundColor Yellow

$ports = @(5173, 5174, 8080)
foreach ($port in $ports) {
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        try {
            $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
            if ($process) {
                Write-Host "  - 释放端口 $port (由 PID $($connection.OwningProcess) 占用)" -ForegroundColor White
                Stop-Process -Id $connection.OwningProcess -Force -ErrorAction Stop
                $stoppedServices += "Port $port release"
            }
        } catch {
            Write-Host "  - 端口 $port 释放失败: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "  - 端口 $port 未被占用" -ForegroundColor Gray
    }
}

# 显示汇总信息
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   服务停止汇总" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($stoppedServices.Count -gt 0) {
    Write-Host "成功停止的服务 (共 $($stoppedServices.Count) 个):" -ForegroundColor Green
    foreach ($service in $stoppedServices) {
        Write-Host "  ✓ $service" -ForegroundColor Green
    }
} else {
    Write-Host "没有需要停止的服务" -ForegroundColor Gray
}

if ($failedServices.Count -gt 0) {
    Write-Host ""
    Write-Host "停止失败的服务 (共 $($failedServices.Count) 个):" -ForegroundColor Red
    foreach ($service in $failedServices) {
        Write-Host "  ✗ $service" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "脚本执行完成！" -ForegroundColor Cyan
Write-Host ""
