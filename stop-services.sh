#!/bin/bash
# AI Assistant - 停止前后端服务脚本
# 支持 macOS 和 Linux
# 使用方法: ./stop-services.sh

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 初始化变量
STOPPED_SERVICES=()
FAILED_SERVICES=()

# 检测操作系统
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS_NAME="macOS"
    IS_MAC=true
    IS_LINUX=false
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS_NAME="Linux"
    IS_MAC=false
    IS_LINUX=true
else
    OS_NAME="Unknown Unix"
    IS_MAC=false
    IS_LINUX=false
fi

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}   AI Assistant 服务停止脚本${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

echo -e "[检测] 操作系统: $OS_NAME"
echo ""

# 停止函数
stop_process_by_port() {
    local port=$1
    local pid=$(lsof -ti:$port 2>/dev/null)
    
    if [ -n "$pid" ]; then
        echo -e "  - 释放端口 $port (PID: $pid)"
        if kill -15 $pid 2>/dev/null; then
            sleep 1
            # 检查进程是否仍在运行
            if kill -0 $pid 2>/dev/null; then
                kill -9 $pid 2>/dev/null
            fi
            STOPPED_SERVICES+=("Port $port")
            return 0
        else
            echo -e "  - 端口 $port 释放失败"
            FAILED_SERVICES+=("Port $port")
            return 1
        fi
    else
        echo -e "  - 端口 $port 未被占用"
        return 0
    fi
}

stop_node_process() {
    # 查找 Node.js 相关进程
    local node_pids=$(pgrep -f "vite|npm|yarn|pnpm|node.*dev" 2>/dev/null)
    
    if [ -n "$node_pids" ]; then
        for pid in $node_pids; do
            if [ -n "$pid" ] && [ "$pid" != "$$" ]; then
                local process_info=$(ps -p $pid -o comm= 2>/dev/null)
                if [ -n "$process_info" ]; then
                    echo -e "  - 停止 Node.js 服务 (PID: $pid)"
                    if kill -15 $pid 2>/dev/null; then
                        sleep 1
                        if kill -0 $pid 2>/dev/null; then
                            kill -9 $pid 2>/dev/null
                        fi
                        STOPPED_SERVICES+=("Node.js - PID: $pid")
                    else
                        FAILED_SERVICES+=("Node.js - PID: $pid")
                    fi
                fi
            fi
        done
    fi
    
    # 也检查特定的端口
    for port in 5173 5174; do
        stop_process_by_port $port
    done
}

stop_java_process() {
    # 查找 Java Spring Boot 进程
    local java_pids=$(pgrep -f "ai-assistant|spring-boot|maven|spring-boot:run" 2>/dev/null)
    
    if [ -n "$java_pids" ]; then
        for pid in $java_pids; do
            if [ -n "$pid" ] && [ "$pid" != "$$" ]; then
                local process_info=$(ps -p $pid -o comm= 2>/dev/null)
                if [ -n "$process_info" ]; then
                    echo -e "  - 停止 Java Spring Boot (PID: $pid)"
                    if kill -15 $pid 2>/dev/null; then
                        sleep 1
                        if kill -0 $pid 2>/dev/null; then
                            kill -9 $pid 2>/dev/null
                        fi
                        STOPPED_SERVICES+=("Java Spring Boot - PID: $pid")
                    else
                        FAILED_SERVICES+=("Java Spring Boot - PID: $pid")
                    fi
                fi
            fi
        done
    fi
    
    # 也检查端口 8080
    stop_process_by_port 8080
}

stop_python_process() {
    # 查找 Python 后端服务
    local python_pids=$(pgrep -f "python.*server|python.*api|uvicorn|fastapi|flask" 2>/dev/null)
    
    if [ -n "$python_pids" ]; then
        for pid in $python_pids; do
            if [ -n "$pid" ] && [ "$pid" != "$$" ]; then
                local process_info=$(ps -p $pid -o comm= 2>/dev/null)
                if [ -n "$process_info" ]; then
                    echo -e "  - 停止 Python 服务 (PID: $pid)"
                    if kill -15 $pid 2>/dev/null; then
                        sleep 1
                        if kill -0 $pid 2>/dev/null; then
                            kill -9 $pid 2>/dev/null
                        fi
                        STOPPED_SERVICES+=("Python Service - PID: $pid")
                    else
                        FAILED_SERVICES+=("Python Service - PID: $pid")
                    fi
                fi
            fi
        done
    fi
}

stop_dotnet_process() {
    # 查找 .NET 后端服务
    local dotnet_pids=$(pgrep -f "dotnet.*api|dotnet.*server" 2>/dev/null)
    
    if [ -n "$dotnet_pids" ]; then
        for pid in $dotnet_pids; do
            if [ -n "$pid" ] && [ "$pid" != "$$" ]; then
                local process_info=$(ps -p $pid -o comm= 2>/dev/null)
                if [ -n "$process_info" ]; then
                    echo -e "  - 停止 .NET 服务 (PID: $pid)"
                    if kill -15 $pid 2>/dev/null; then
                        sleep 1
                        if kill -0 $pid 2>/dev/null; then
                            kill -9 $pid 2>/dev/null
                        fi
                        STOPPED_SERVICES+=(".NET Service - PID: $pid")
                    else
                        FAILED_SERVICES+=(".NET Service - PID: $pid")
                    fi
                fi
            fi
        done
    fi
}

echo -e "${YELLOW}[步骤 1/3] 停止前端服务...${NC}"
FRONTEND_STOPPED=false

# 尝试停止 Node.js 相关进程
stop_node_process

if [ ${#STOPPED_SERVICES[@]} -eq 0 ]; then
    echo -e "  ${GRAY}[跳过] 未检测到运行的前端服务${NC}"
fi

echo ""
echo -e "${YELLOW}[步骤 2/3] 停止后端服务...${NC}"
BACKEND_STOPPED=false

# 尝试停止 Java Spring Boot
stop_java_process

# 尝试停止 Python 服务
stop_python_process

# 尝试停止 .NET 服务
stop_dotnet_process

if [ ${#STOPPED_SERVICES[@]} -eq 0 ]; then
    echo -e "  ${GRAY}[跳过] 未检测到运行的后端服务${NC}"
fi

echo ""
echo -e "${YELLOW}[步骤 3/3] 清理残留进程...${NC}"

# 确保常用端口被释放
for port in 5173 5174 8080; do
    stop_process_by_port $port
done

# 显示汇总信息
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}   服务停止汇总${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

if [ ${#STOPPED_SERVICES[@]} -gt 0 ]; then
    echo -e "${GREEN}成功停止的服务 (共 ${#STOPPED_SERVICES[@]} 个):${NC}"
    for service in "${STOPPED_SERVICES[@]}"; do
        echo -e "  ✓ $service"
    done
else
    echo -e "${GRAY}没有需要停止的服务${NC}"
fi

if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
    echo ""
    echo -e "${RED}停止失败的服务 (共 ${#FAILED_SERVICES[@]} 个):${NC}"
    for service in "${FAILED_SERVICES[@]}"; do
        echo -e "  ✗ $service"
    done
fi

echo ""
echo -e "${CYAN}脚本执行完成！${NC}"
echo ""
