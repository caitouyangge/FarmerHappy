@echo off
chcp 65001 >nul
echo ========================================
echo    农乐助农平台 - 停止所有服务
echo ========================================
echo.

echo [提示] 正在查找并停止服务...
echo.

REM 停止后端 Java 进程
echo [1/2] 停止后端服务...
tasklist /FI "WINDOWTITLE eq 农乐平台-后端服务*" 2>nul | find /I "cmd.exe" >nul
if %errorlevel% equ 0 (
    taskkill /FI "WINDOWTITLE eq 农乐平台-后端服务*" /F >nul 2>&1
    echo [成功] 后端服务窗口已关闭
) else (
    echo [提示] 未找到后端服务窗口
)

REM 额外清理：停止所有 application 相关的 Java 进程
tasklist /FI "IMAGENAME eq java.exe" 2>nul | find /I "java.exe" >nul
if %errorlevel% equ 0 (
    for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| find "PID:"') do (
        wmic process where "ProcessId=%%a" get CommandLine 2>nul | find "application" >nul
        if !errorlevel! equ 0 (
            taskkill /PID %%a /F >nul 2>&1
        )
    )
)

echo.
REM 停止前端 Node 进程
echo [2/2] 停止前端服务...
tasklist /FI "WINDOWTITLE eq 农乐平台-前端服务*" 2>nul | find /I "cmd.exe" >nul
if %errorlevel% equ 0 (
    taskkill /FI "WINDOWTITLE eq 农乐平台-前端服务*" /F >nul 2>&1
    echo [成功] 前端服务窗口已关闭
) else (
    echo [提示] 未找到前端服务窗口
)

REM 额外清理：停止所有 node 相关的进程
taskkill /F /IM node.exe >nul 2>&1
if %errorlevel% equ 0 (
    echo [提示] 已清理残留的 Node 进程
)

echo.
echo ========================================
echo    所有服务已停止
echo ========================================
echo.
echo [提示] 如需重新启动，请运行 start-all.bat
echo.
pause

