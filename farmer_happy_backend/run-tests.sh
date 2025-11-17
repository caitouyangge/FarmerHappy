#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     FarmerHappy 后端测试运行脚本                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}"
echo ""

# 主菜单函数
show_menu() {
    echo ""
    echo -e "${GREEN}请选择测试运行方式：${NC}"
    echo ""
    echo "[1] 运行所有测试"
    echo "[2] 运行特定测试类"
    echo "[3] 运行并生成测试报告"
    echo "[4] 运行并查看覆盖率报告"
    echo "[5] 清理测试缓存"
    echo "[0] 退出"
    echo ""
    read -p "请输入选项 (0-5): " choice
    
    case $choice in
        1) run_all_tests ;;
        2) run_specific_test ;;
        3) run_with_report ;;
        4) run_with_coverage ;;
        5) clean_test ;;
        0) exit 0 ;;
        *) echo -e "${RED}无效选项！${NC}" && show_menu ;;
    esac
}

# 运行所有测试
run_all_tests() {
    echo ""
    echo -e "${YELLOW}===== 运行所有测试 =====${NC}"
    echo ""
    mvn test
    echo ""
    echo -e "${GREEN}测试完成！${NC}"
    read -p "按回车键继续..."
    show_menu
}

# 运行特定测试类
run_specific_test() {
    echo ""
    echo "示例："
    echo "  - entity.UserTest"
    echo "  - dto.auth.LoginRequestDTOTest"
    echo "  - service.auth.AuthServiceImplTest"
    echo ""
    read -p "请输入测试类名（不含.java）: " testclass
    
    if [ -z "$testclass" ]; then
        echo -e "${RED}测试类名不能为空！${NC}"
        run_specific_test
        return
    fi
    
    echo ""
    echo -e "${YELLOW}===== 运行测试类: $testclass =====${NC}"
    echo ""
    mvn test -Dtest="$testclass"
    echo ""
    echo -e "${GREEN}测试完成！${NC}"
    read -p "按回车键继续..."
    show_menu
}

# 运行测试并生成报告
run_with_report() {
    echo ""
    echo -e "${YELLOW}===== 运行测试并生成HTML报告 =====${NC}"
    echo ""
    mvn clean test surefire-report:report
    echo ""
    echo -e "${GREEN}报告生成完成！${NC}"
    echo "报告位置: target/site/surefire-report.html"
    echo ""
    read -p "是否打开报告？(y/n): " open
    
    if [[ "$open" == "y" || "$open" == "Y" ]]; then
        if command -v xdg-open &> /dev/null; then
            xdg-open target/site/surefire-report.html
        elif command -v open &> /dev/null; then
            open target/site/surefire-report.html
        else
            echo -e "${YELLOW}请手动打开: target/site/surefire-report.html${NC}"
        fi
    fi
    
    read -p "按回车键继续..."
    show_menu
}

# 运行测试并生成覆盖率报告
run_with_coverage() {
    echo ""
    echo -e "${YELLOW}===== 运行测试并生成覆盖率报告 =====${NC}"
    echo ""
    echo -e "${YELLOW}注意：需要先在 pom.xml 中配置 JaCoCo 插件${NC}"
    echo ""
    mvn clean test
    echo ""
    echo -e "${GREEN}如果已配置 JaCoCo，覆盖率报告位置: target/site/jacoco/index.html${NC}"
    
    read -p "按回车键继续..."
    show_menu
}

# 清理测试缓存
clean_test() {
    echo ""
    echo -e "${YELLOW}===== 清理测试缓存 =====${NC}"
    echo ""
    mvn clean
    echo ""
    echo -e "${GREEN}清理完成！${NC}"
    read -p "按回车键继续..."
    show_menu
}

# 开始运行
show_menu

