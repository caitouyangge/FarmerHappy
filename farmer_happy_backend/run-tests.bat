@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════════════════╗
echo ║     FarmerHappy 后端测试运行脚本                  ║
echo ╚════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0"

:menu
echo.
echo 请选择测试运行方式：
echo.
echo [1] 运行所有测试
echo [2] 运行特定测试类
echo [3] 运行并生成测试报告
echo [4] 运行并查看覆盖率报告
echo [5] 清理测试缓存
echo [0] 退出
echo.
set /p choice="请输入选项 (0-5): "

if "%choice%"=="1" goto run_all
if "%choice%"=="2" goto run_specific
if "%choice%"=="3" goto run_with_report
if "%choice%"=="4" goto run_with_coverage
if "%choice%"=="5" goto clean_test
if "%choice%"=="0" goto end
goto menu

:run_all
echo.
echo ===== 运行所有测试 =====
echo.
mvn test
echo.
echo 测试完成！
pause
goto menu

:run_specific
echo.
echo 示例：
echo   - entity.UserTest
echo   - dto.auth.LoginRequestDTOTest
echo   - service.auth.AuthServiceImplTest
echo.
set /p testclass="请输入测试类名（不含.java）: "
echo.
echo ===== 运行测试类: %testclass% =====
echo.
mvn test -Dtest=%testclass%
echo.
echo 测试完成！
pause
goto menu

:run_with_report
echo.
echo ===== 运行测试并生成HTML报告 =====
echo.
mvn clean test surefire-report:report
echo.
echo 报告生成完成！
echo 报告位置: target\site\surefire-report.html
echo.
set /p open="是否打开报告？(Y/N): "
if /i "%open%"=="Y" start target\site\surefire-report.html
pause
goto menu

:run_with_coverage
echo.
echo ===== 运行测试并生成覆盖率报告 =====
echo.
echo 注意：需要先在 pom.xml 中配置 JaCoCo 插件
echo.
mvn clean test
echo.
echo 如果已配置 JaCoCo，覆盖率报告位置: target\site\jacoco\index.html
pause
goto menu

:clean_test
echo.
echo ===== 清理测试缓存 =====
echo.
mvn clean
echo.
echo 清理完成！
pause
goto menu

:end
echo.
echo 感谢使用！再见！
timeout /t 2 > nul
exit /b 0

