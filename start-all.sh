#!/bin/bash

# 设置颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================"
echo "   农乐助农平台 - 一键启动"
echo "========================================"
echo ""

# 检查是否在项目根目录
if [ ! -d "farmer_happy" ] || [ ! -d "farmer_happy_backend" ]; then
    echo -e "${RED}[错误] 请在项目根目录（FarmerHappy）下运行此脚本！${NC}"
    exit 1
fi

# 检查前端依赖
echo -e "${BLUE}[1/4] 检查前端依赖...${NC}"
cd farmer_happy
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}[提示] 首次运行，正在安装前端依赖...${NC}"
    npm install
    if [ $? -ne 0 ]; then
        echo -e "${RED}[错误] 前端依赖安装失败！${NC}"
        exit 1
    fi
fi
cd ..

# 编译后端
echo -e "${BLUE}[2/4] 编译后端代码...${NC}"
cd farmer_happy_backend/src
javac -encoding UTF-8 -cp ".:../lib/*" application.java
if [ $? -ne 0 ]; then
    echo -e "${RED}[错误] 后端编译失败！${NC}"
    cd ../..
    exit 1
fi
echo -e "${GREEN}[成功] 后端编译完成！${NC}"
cd ../..

# 创建日志目录
mkdir -p logs

# 启动后端服务
echo ""
echo -e "${BLUE}[3/4] 启动后端服务...${NC}"
cd farmer_happy_backend/src
nohup java -cp ".:../lib/*" application > ../../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > ../../logs/backend.pid
cd ../..

if [ $? -eq 0 ]; then
    echo -e "${GREEN}[成功] 后端服务已启动 (PID: $BACKEND_PID)${NC}"
    echo -e "${YELLOW}[提示] 日志文件: logs/backend.log${NC}"
else
    echo -e "${RED}[错误] 后端服务启动失败！${NC}"
    exit 1
fi

# 等待后端启动
echo -e "${YELLOW}[提示] 等待后端完全启动...${NC}"
sleep 3

# 启动前端服务
echo ""
echo -e "${BLUE}[4/4] 启动前端服务...${NC}"
cd farmer_happy
nohup npm run serve > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > ../logs/frontend.pid
cd ..

if [ $? -eq 0 ]; then
    echo -e "${GREEN}[成功] 前端服务已启动 (PID: $FRONTEND_PID)${NC}"
    echo -e "${YELLOW}[提示] 日志文件: logs/frontend.log${NC}"
else
    echo -e "${RED}[错误] 前端服务启动失败！${NC}"
    exit 1
fi

echo ""
echo "========================================"
echo -e "${GREEN}   启动完成！${NC}"
echo "========================================"
echo ""
echo -e "${BLUE}[后端]${NC} http://localhost:8080"
echo -e "${BLUE}[前端]${NC} 通常为 http://localhost:8081"
echo ""
echo -e "${YELLOW}[提示] 查看实时日志:${NC}"
echo "  后端: tail -f logs/backend.log"
echo "  前端: tail -f logs/frontend.log"
echo ""
echo -e "${YELLOW}[提示] 停止所有服务:${NC}"
echo "  运行: ./stop-all.sh"
echo ""
echo -e "${YELLOW}[提示] 进程ID已保存:${NC}"
echo "  后端 PID: $BACKEND_PID"
echo "  前端 PID: $FRONTEND_PID"
echo ""

