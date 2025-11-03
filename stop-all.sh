#!/bin/bash

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================"
echo "   农乐助农平台 - 停止所有服务"
echo "========================================"
echo ""

# 停止后端服务
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    echo -e "${BLUE}[1/2] 停止后端服务 (PID: $BACKEND_PID)...${NC}"
    
    if kill -0 $BACKEND_PID 2>/dev/null; then
        kill $BACKEND_PID
        sleep 2
        
        # 如果还没停止，强制停止
        if kill -0 $BACKEND_PID 2>/dev/null; then
            echo -e "${YELLOW}[提示] 正在强制停止...${NC}"
            kill -9 $BACKEND_PID
        fi
        
        echo -e "${GREEN}[成功] 后端服务已停止${NC}"
    else
        echo -e "${YELLOW}[提示] 后端服务未运行${NC}"
    fi
    
    rm -f logs/backend.pid
else
    echo -e "${YELLOW}[提示] 未找到后端服务 PID 文件${NC}"
fi

# 停止前端服务
if [ -f "logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    echo -e "${BLUE}[2/2] 停止前端服务 (PID: $FRONTEND_PID)...${NC}"
    
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        kill $FRONTEND_PID
        sleep 2
        
        # 如果还没停止，强制停止
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            echo -e "${YELLOW}[提示] 正在强制停止...${NC}"
            kill -9 $FRONTEND_PID
        fi
        
        echo -e "${GREEN}[成功] 前端服务已停止${NC}"
    else
        echo -e "${YELLOW}[提示] 前端服务未运行${NC}"
    fi
    
    rm -f logs/frontend.pid
else
    echo -e "${YELLOW}[提示] 未找到前端服务 PID 文件${NC}"
fi

# 额外清理：查找并停止可能的残留进程
echo ""
echo -e "${BLUE}[清理] 检查残留进程...${NC}"

# 清理后端 Java 进程
JAVA_PIDS=$(pgrep -f "application")
if [ ! -z "$JAVA_PIDS" ]; then
    echo -e "${YELLOW}[提示] 发现残留的后端进程，正在清理...${NC}"
    pkill -f "application"
fi

# 清理前端 Node 进程
NODE_PIDS=$(pgrep -f "vue-cli-service serve")
if [ ! -z "$NODE_PIDS" ]; then
    echo -e "${YELLOW}[提示] 发现残留的前端进程，正在清理...${NC}"
    pkill -f "vue-cli-service serve"
fi

echo ""
echo "========================================"
echo -e "${GREEN}   所有服务已停止${NC}"
echo "========================================"
echo ""

