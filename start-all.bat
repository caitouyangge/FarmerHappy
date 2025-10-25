@echo off
chcp 65001 >nul
echo ========================================
echo    农乐助农平台 - 一键启动
echo ========================================
echo.

REM 检查是否在项目根目录
if not exist "farmer_happy" (
    echo [错误] 请在项目根目录（FarmerHappy）下运行此脚本！
    pause
    exit /b 1
)

if not exist "farmer_happy_backend" (
    echo [错误] 找不到后端目录！
    pause
    exit /b 1
)

echo [1/4] 检查前端依赖...
cd farmer_happy
if not exist "node_modules" (
    echo [提示] 首次运行，正在安装前端依赖...
    call npm install
    if errorlevel 1 (
        echo [错误] 前端依赖安装失败！
        cd ..
        pause
        exit /b 1
    )
)
cd ..

echo [2/4] 编译后端代码...
cd farmer_happy_backend\src
javac -encoding UTF-8 -cp ".;..\lib\*" application.java
if errorlevel 1 (
    echo [错误] 后端编译失败！
    cd ..\..
    pause
    exit /b 1
)
echo [成功] 后端编译完成！
cd ..\..

echo.
echo [3/4] 启动后端服务（新窗口）...
start "农乐平台-后端服务" cmd /k "cd /d %cd%\farmer_happy_backend\src && echo 正在启动后端服务... && java -cp ".;..\lib\*" application"

echo [提示] 等待后端启动...
timeout /t 3 /nobreak >nul

echo.
echo [4/4] 启动前端服务（新窗口）...
start "农乐平台-前端服务" cmd /k "cd /d %cd%\farmer_happy && echo 正在启动前端服务... && npm run serve"

echo.
echo ========================================
echo    启动完成！
echo ========================================
echo.
echo [后端] http://localhost:8080
echo [前端] 请查看前端窗口显示的地址
echo        通常为 http://localhost:8081
echo.
echo [提示] 两个服务窗口会自动打开
echo [提示] 按 F12 打开浏览器控制台查看日志
echo [提示] 按 Ctrl+C 可停止相应服务
echo.
pause

