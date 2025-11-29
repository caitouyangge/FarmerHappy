package repository;

import config.DatabaseConfig;
import entity.Comment;
import entity.Content;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private DatabaseConfig config;

    // 私有构造函数（单例模式）
    private DatabaseManager() {
        this.config = DatabaseConfig.getInstance();
    }

    // 获取单例实例
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // 获取数据库连接
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(config.getDriver());
            connection = DriverManager.getConnection(
                config.getFullUrl(), 
                config.getUsername(), 
                config.getPassword()
            );
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }

    // 初始化数据库和表
    public void initializeDatabase() {
        try {
            // 首先连接到 MySQL 服务器（不指定数据库）
            Connection serverConnection = DriverManager.getConnection(
                config.getUrl(), 
                config.getUsername(), 
                config.getPassword()
            );
            Statement serverStatement = serverConnection.createStatement();

            // 创建数据库（如果不存在）
            serverStatement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + config.getDatabaseName());
            serverStatement.close();
            serverConnection.close();

            // 连接到指定数据库并创建表
            Connection dbConnection = getConnection();
            Statement dbStatement = dbConnection.createStatement();

            // 示例：创建用户表
            String createUserTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "    uid VARCHAR(36) PRIMARY KEY DEFAULT (UUID())," +
                    "    phone VARCHAR(11) UNIQUE NOT NULL COMMENT '手机号，11位数字'," +
                    "    password VARCHAR(255) NOT NULL COMMENT '密码（存储基本字符串）'," +
                    "    nickname VARCHAR(30) DEFAULT '' COMMENT '用户昵称，1-30个字符'," +
                    "    login_attempts INT DEFAULT 0 COMMENT '连续登录失败次数'," +
                    "    locked_until TIMESTAMP NULL COMMENT '账号锁定截止时间'," +
                    "    is_active BOOLEAN DEFAULT TRUE COMMENT '账号是否激活'," +
                    "    money DECIMAL(10,2) DEFAULT 1000 COMMENT '账户余额（元）'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "    INDEX idx_phone (phone)," +
                    "    INDEX idx_created_at (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息表';";
            dbStatement.executeUpdate(createUserTable);

            // 检查并添加money字段（如果不存在）
            try {
                String checkMoneyColumnSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = '" + config.getDatabaseName() + "' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'money'";
                ResultSet rsCheckMoney = dbStatement.executeQuery(checkMoneyColumnSql);

                if (!rsCheckMoney.next()) {
                    // money 列不存在，添加它
                    String addMoneyColumnSql = "ALTER TABLE users ADD COLUMN money DECIMAL(10,2) DEFAULT 1000 COMMENT '账户余额（元）' AFTER is_active";
                    dbStatement.executeUpdate(addMoneyColumnSql);
                    System.out.println("表结构更新成功：为users表添加money字段");
                } else {
                    System.out.println("表结构检查：users表的money字段已存在，无需更新");
                }
                rsCheckMoney.close();
            } catch (SQLException e) {
                // 如果更新失败，记录错误但不中断程序
                System.err.println("表结构更新失败（添加money字段）：" + e.getMessage());
            }

            // 创建买家扩展表
            String createUserBuyersTable = "CREATE TABLE IF NOT EXISTS user_buyers (" +
                    "    buyer_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    uid VARCHAR(36) NOT NULL COMMENT '用户UID'," +
                    "    shipping_address VARCHAR(500) COMMENT '默认收货地址'," +
                    "    member_level ENUM('regular', 'silver', 'gold', 'platinum') DEFAULT 'regular' COMMENT '会员等级'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用买家功能'," +
                    "    UNIQUE KEY uk_uid (uid)," +
                    "    INDEX idx_enable (enable)," +
                    "    INDEX idx_member_level (member_level)," +
                    "    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='买家用户扩展信息表';";
            dbStatement.executeUpdate(createUserBuyersTable);

            // 创建农户扩展表
            String createUserFarmersTable = "CREATE TABLE IF NOT EXISTS user_farmers (" +
                    "    farmer_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    uid VARCHAR(36) NOT NULL COMMENT '用户UID'," +
                    "    farm_name VARCHAR(100) NOT NULL COMMENT '农场名称'," +
                    "    farm_address VARCHAR(200) COMMENT '农场地址'," +
                    "    farm_size DECIMAL(10,2) COMMENT '农场面积（亩）'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用农户功能'," +
                    "    UNIQUE KEY uk_uid (uid)," +
                    "    INDEX idx_enable (enable)," +
                    "    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农户用户扩展信息表';";
            dbStatement.executeUpdate(createUserFarmersTable);

            // 创建技术专家扩展表
            String createUserExpertsTable = "CREATE TABLE IF NOT EXISTS user_experts (" +
                    "    expert_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    uid VARCHAR(36) NOT NULL COMMENT '用户UID'," +
                    "    expertise_field VARCHAR(100) NOT NULL COMMENT '专业领域'," +
                    "    work_experience INT COMMENT '工作经验（年）'," +
                    "    service_area VARCHAR(200) COMMENT '服务区域'," +
                    "    consultation_fee DECIMAL(10,2) COMMENT '咨询费用'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用专家功能'," +
                    "    UNIQUE KEY uk_uid (uid)," +
                    "    INDEX idx_enable (enable)," +
                    "    INDEX idx_expertise (expertise_field)," +
                    "    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技术专家用户扩展信息表';";
            dbStatement.executeUpdate(createUserExpertsTable);

            // 创建银行扩展表
            String createUserBanksTable = "CREATE TABLE IF NOT EXISTS user_banks (" +
                    "    bank_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    uid VARCHAR(36) NOT NULL COMMENT '用户UID'," +
                    "    bank_name VARCHAR(100) NOT NULL COMMENT '银行名称'," +
                    "    branch_name VARCHAR(100) COMMENT '分行名称'," +
                    "    contact_person VARCHAR(50) COMMENT '联系人'," +
                    "    contact_phone VARCHAR(20) COMMENT '联系电话'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用银行功能'," +
                    "    UNIQUE KEY uk_uid (uid)," +
                    "    INDEX idx_enable (enable)," +
                    "    INDEX idx_bank_name (bank_name)," +
                    "    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行用户扩展信息表';";
            dbStatement.executeUpdate(createUserBanksTable);

            // 创建管理员扩展表
            String createUserAdminsTable = "CREATE TABLE IF NOT EXISTS user_admins (" +
                    "    admin_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    uid VARCHAR(36) NOT NULL COMMENT '用户UID'," +
                    "    admin_level ENUM('super', 'normal', 'auditor') DEFAULT 'normal' COMMENT '管理员级别'," +
                    "    department VARCHAR(100) COMMENT '所属部门'," +
                    "    permissions JSON COMMENT '权限配置'," +
                    "    last_login_ip VARCHAR(45) COMMENT '最后登录IP'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用管理员功能'," +
                    "    UNIQUE KEY uk_uid (uid)," +
                    "    INDEX idx_enable (enable)," +
                    "    INDEX idx_admin_level (admin_level)," +
                    "    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员用户扩展信息表';";
            dbStatement.executeUpdate(createUserAdminsTable);

            // 创建商品表
            String createProductsTable = "CREATE TABLE IF NOT EXISTS products (" +
                    "    product_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    farmer_id BIGINT NOT NULL COMMENT '农户ID'," +
                    "    category ENUM('vegetables', 'fruits', 'grains', 'livestock', 'aquatic') " +
                    "        NOT NULL COMMENT '商品分类'," +
                    "    title VARCHAR(100) NOT NULL COMMENT '商品标题'," +
                    "    detailed_description VARCHAR(200) NOT NULL COMMENT '商品详细介绍'," +
                    "    price DECIMAL(10,2) NOT NULL COMMENT '价格(元)'," +
                    "    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量'," +
                    "    description TEXT COMMENT '商品图文详细描述(HTML格式)'," +
                    "    origin VARCHAR(200) COMMENT '产地信息'," +
                    "    status ENUM('pending_review', 'on_shelf', 'off_shelf', 'review_rejected') " +
                    "        NOT NULL DEFAULT 'pending_review' COMMENT '商品状态'," +
                    "    view_count INT DEFAULT 0 COMMENT '浏览量'," +
                    "    sales_count INT DEFAULT 0 COMMENT '销量'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "    INDEX idx_farmer_id (farmer_id)," +
                    "    INDEX idx_category (category)," +
                    "    INDEX idx_status (status)," +
                    "    INDEX idx_enable (enable)," +
                    "    INDEX idx_created_at (created_at)," +
                    "    INDEX idx_price (price)," +
                    "    FOREIGN KEY (farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';";
            dbStatement.executeUpdate(createProductsTable);

            // 创建商品图片表
            String createProductImagesTable = "CREATE TABLE IF NOT EXISTS product_images (" +
                    "    image_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    product_id BIGINT NOT NULL COMMENT '商品ID'," +
                    "    image_url VARCHAR(500) NOT NULL COMMENT '图片URL'," +
                    "    sort_order INT DEFAULT 0 COMMENT '排序序号'," +
                    "    is_main BOOLEAN DEFAULT FALSE COMMENT '是否主图'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用'," +
                    "    INDEX idx_product_id (product_id)," +
                    "    INDEX idx_sort_order (sort_order)," +
                    "    INDEX idx_enable (enable)," +
                    "    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';";
            dbStatement.executeUpdate(createProductImagesTable);

            // 创建商品审核记录表
            String createProductReviewsTable = "CREATE TABLE IF NOT EXISTS product_reviews (" +
                    "    review_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    product_id BIGINT NOT NULL COMMENT '商品ID'," +
                    "    reviewer_id BIGINT COMMENT '审核员ID(关联users表)'," +
                    "    old_status ENUM('pending_review', 'on_shelf', 'off_shelf', 'review_rejected') NOT NULL COMMENT '原状态',"
                    +
                    "    new_status ENUM('pending_review', 'on_shelf', 'off_shelf', 'review_rejected') NOT NULL COMMENT '新状态',"
                    +
                    "    review_comment TEXT COMMENT '审核意见'," +
                    "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "    INDEX idx_product_id (product_id)," +
                    "    INDEX idx_reviewer_id (reviewer_id)," +
                    "    INDEX idx_enable (enable)," +
                    "    INDEX idx_created_at (created_at)," +
                    "    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品审核记录表';";
            dbStatement.executeUpdate(createProductReviewsTable);

            // 更新表结构：将specification列改为detailed_description
            try {
                // 首先检查列是否存在
                String checkColumnSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = '" + config.getDatabaseName()
                        + "' AND TABLE_NAME = 'products' AND COLUMN_NAME = 'specification'";
                ResultSet rsCheck = dbStatement.executeQuery(checkColumnSql);

                if (rsCheck.next()) {
                    // specification 列存在，需要改名
                    String alterTableSql = "ALTER TABLE products CHANGE COLUMN specification detailed_description VARCHAR(200) NOT NULL COMMENT '商品详细介绍'";
                    dbStatement.executeUpdate(alterTableSql);
                    System.out.println("表结构更新成功：specification -> detailed_description");
                } else {
                    // specification 列不存在，检查 detailed_description 是否存在
                    String checkNewColumnSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = '" + config.getDatabaseName()
                            + "' AND TABLE_NAME = 'products' AND COLUMN_NAME = 'detailed_description'";
                    ResultSet rsCheckNew = dbStatement.executeQuery(checkNewColumnSql);

                    if (!rsCheckNew.next()) {
                        // detailed_description 列也不存在，添加它
                        String addColumnSql = "ALTER TABLE products ADD COLUMN detailed_description VARCHAR(200) NOT NULL COMMENT '商品详细介绍' AFTER title";
                        dbStatement.executeUpdate(addColumnSql);
                        System.out.println("表结构更新成功：添加 detailed_description 列");
                    } else {
                        System.out.println("表结构检查：detailed_description 列已存在，无需更新");
                    }
                    rsCheckNew.close();
                }
                rsCheck.close();
            } catch (SQLException e) {
                // 如果更新失败，记录错误但不中断程序
                System.err.println("表结构更新失败：" + e.getMessage());
            }
            // 创建社区内容表
            String createContentsTable = "CREATE TABLE IF NOT EXISTS contents (" +
                    "    content_id VARCHAR(50) PRIMARY KEY COMMENT '内容ID'," +
                    "    title VARCHAR(200) NOT NULL COMMENT '标题'," +
                    "    content TEXT NOT NULL COMMENT '内容'," +
                    "    content_type ENUM('articles', 'questions', 'experiences') NOT NULL COMMENT '内容类型'," +
                    "    author_user_id VARCHAR(36) NOT NULL COMMENT '作者用户ID'," +
                    "    author_nickname VARCHAR(30) NOT NULL COMMENT '作者昵称'," +
                    "    author_role VARCHAR(20) NOT NULL COMMENT '作者角色'," +
                    "    view_count INT DEFAULT 0 COMMENT '浏览量'," +
                    "    comment_count INT DEFAULT 0 COMMENT '评论数'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "    INDEX idx_content_type (content_type)," +
                    "    INDEX idx_author_user_id (author_user_id)," +
                    "    INDEX idx_created_at (created_at)," +
                    "    INDEX idx_view_count (view_count)," +
                    "    INDEX idx_comment_count (comment_count)," +
                    "    FOREIGN KEY (author_user_id) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区内容表';";
            dbStatement.executeUpdate(createContentsTable);

            // 创建社区内容图片表
            String createContentImagesTable = "CREATE TABLE IF NOT EXISTS content_images (" +
                    "    image_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    content_id VARCHAR(50) NOT NULL COMMENT '内容ID'," +
                    "    image_url VARCHAR(500) NOT NULL COMMENT '图片URL'," +
                    "    sort_order INT DEFAULT 0 COMMENT '排序序号'," +
                    "    INDEX idx_content_id (content_id)," +
                    "    FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区内容图片表';";
            dbStatement.executeUpdate(createContentImagesTable);

            // 创建社区评论表
            String createCommentsTable = "CREATE TABLE IF NOT EXISTS comments (" +
                    "    comment_id VARCHAR(50) PRIMARY KEY COMMENT '评论ID'," +
                    "    content_id VARCHAR(50) NOT NULL COMMENT '所属内容ID'," +
                    "    parent_comment_id VARCHAR(50) COMMENT '父评论ID，NULL表示一级评论'," +
                    "    author_user_id VARCHAR(36) NOT NULL COMMENT '评论者用户ID'," +
                    "    author_nickname VARCHAR(30) NOT NULL COMMENT '评论者昵称'," +
                    "    author_role VARCHAR(20) NOT NULL COMMENT '评论者角色'," +
                    "    reply_to_user_id VARCHAR(36) COMMENT '回复的用户ID（二级评论）'," +
                    "    reply_to_nickname VARCHAR(30) COMMENT '回复的用户昵称（二级评论）'," +
                    "    content TEXT NOT NULL COMMENT '评论内容'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "    INDEX idx_content_id (content_id)," +
                    "    INDEX idx_parent_comment_id (parent_comment_id)," +
                    "    INDEX idx_author_user_id (author_user_id)," +
                    "    INDEX idx_created_at (created_at)," +
                    "    FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (author_user_id) REFERENCES users(uid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区评论表';";
            dbStatement.executeUpdate(createCommentsTable);

            // 创建订单表
            String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                    "    order_id VARCHAR(50) PRIMARY KEY COMMENT '订单唯一ID'," +
                    "    buyer_uid VARCHAR(36) NOT NULL COMMENT '买家UID'," +
                    "    farmer_uid VARCHAR(36) NOT NULL COMMENT '农户UID'," +
                    "    product_id BIGINT NOT NULL COMMENT '商品ID'," +
                    "    product_title VARCHAR(100) NOT NULL COMMENT '下单时的商品标题'," +
                    "    product_specification VARCHAR(200) NOT NULL COMMENT '下单时的商品规格'," +
                    "    product_price DECIMAL(10,2) NOT NULL COMMENT '下单时的商品单价'," +
                    "    quantity INT NOT NULL COMMENT '购买数量'," +
                    "    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额'," +
                    "    buyer_name VARCHAR(50) NOT NULL COMMENT '收货人姓名'," +
                    "    buyer_address VARCHAR(200) NOT NULL COMMENT '收货地址'," +
                    "    buyer_phone VARCHAR(11) NOT NULL COMMENT '收货人手机号'," +
                    "    remark VARCHAR(500) COMMENT '订单备注'," +
                    "    status ENUM('shipped', 'completed', 'cancelled', 'refunded') " +
                    "        DEFAULT 'shipped' COMMENT '订单状态'," +
                    "    refund_reason VARCHAR(200) COMMENT '退款原因'," +
                    "    refund_type ENUM('only_refund', 'return_and_refund') COMMENT '退款类型'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "    shipped_at TIMESTAMP NULL COMMENT '发货时间'," +
                    "    completed_at TIMESTAMP NULL COMMENT '完成时间'," +
                    "    cancelled_at TIMESTAMP NULL COMMENT '取消时间'," +
                    "    refunded_at TIMESTAMP NULL COMMENT '退款时间'," +
                    "    INDEX idx_buyer_uid (buyer_uid)," +
                    "    INDEX idx_farmer_uid (farmer_uid)," +
                    "    INDEX idx_product_id (product_id)," +
                    "    INDEX idx_status (status)," +
                    "    INDEX idx_created_at (created_at)," +
                    "    INDEX idx_buyer_status (buyer_uid, status)," +
                    "    INDEX idx_farmer_status (farmer_uid, status)," +
                    "    FOREIGN KEY (buyer_uid) REFERENCES users(uid) ON DELETE CASCADE," +
                    "    FOREIGN KEY (farmer_uid) REFERENCES users(uid) ON DELETE CASCADE," +
                    "    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';";
            dbStatement.executeUpdate(createOrdersTable);

            // 创建信用额度表
            String createCreditLimitsTable = "CREATE TABLE IF NOT EXISTS credit_limits (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    farmer_id BIGINT NOT NULL COMMENT '农户ID'," +
                    "    total_limit DECIMAL(15,2) DEFAULT 0 COMMENT '总额度'," +
                    "    used_limit DECIMAL(15,2) DEFAULT 0 COMMENT '已用额度'," +
                    "    available_limit DECIMAL(15,2) DEFAULT 0 COMMENT '可用额度'," +
                    "    currency VARCHAR(10) DEFAULT 'CNY'," +
                    "    status ENUM('active', 'no_limit', 'frozen') DEFAULT 'active'," +
                    "    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE," +
                    "    INDEX idx_farmer_id (farmer_id)," +
                    "    INDEX idx_status (status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用额度表';";
            dbStatement.executeUpdate(createCreditLimitsTable);

            // 创建额度申请表
            String createCreditApplicationsTable = "CREATE TABLE IF NOT EXISTS credit_applications (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    application_id VARCHAR(20) UNIQUE NOT NULL COMMENT '申请ID'," +
                    "    farmer_id BIGINT NOT NULL COMMENT '申请人农户ID'," +
                    "    proof_type ENUM('land_certificate', 'property_certificate', 'income_proof', 'business_license', 'other') NOT NULL,"
                    +
                    "    proof_images JSON COMMENT '证明材料图片URL数组'," +
                    "    apply_amount DECIMAL(15,2) NOT NULL," +
                    "    description VARCHAR(500)," +
                    "    status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending'," +
                    "    approved_amount DECIMAL(15,2) COMMENT '批准的额度'," +
                    "    reject_reason VARCHAR(200)," +
                    "    approved_by BIGINT COMMENT '审批人银行ID'," +
                    "    approved_at TIMESTAMP NULL," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (approved_by) REFERENCES user_banks(bank_id) ON DELETE SET NULL," +
                    "    INDEX idx_farmer_id (farmer_id)," +
                    "    INDEX idx_status (status)," +
                    "    INDEX idx_application_id (application_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用额度申请表';";
            dbStatement.executeUpdate(createCreditApplicationsTable);

            // 创建贷款产品表
            String createLoanProductsTable = "CREATE TABLE IF NOT EXISTS loan_products (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    product_id VARCHAR(20) UNIQUE NOT NULL COMMENT '产品ID'," +
                    "    product_code VARCHAR(50) UNIQUE COMMENT '产品编号'," +
                    "    product_name VARCHAR(50) NOT NULL," +
                    "    min_credit_limit DECIMAL(15,2) NOT NULL COMMENT '最低贷款额度要求'," +
                    "    max_amount DECIMAL(15,2) NOT NULL COMMENT '最高贷款额度'," +
                    "    interest_rate DECIMAL(5,2) NOT NULL COMMENT '年利率'," +
                    "    term_months INT NOT NULL COMMENT '贷款期限(月)'," +
                    "    repayment_method ENUM('equal_installment', 'interest_first', 'bullet_repayment') NOT NULL," +
                    "    description VARCHAR(500) NOT NULL," +
                    "    status ENUM('active', 'inactive') DEFAULT 'active'," +
                    "    bank_id BIGINT NOT NULL COMMENT '发布银行ID'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (bank_id) REFERENCES user_banks(bank_id) ON DELETE CASCADE," +
                    "    INDEX idx_product_id (product_id)," +
                    "    INDEX idx_status (status)," +
                    "    INDEX idx_bank_id (bank_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款产品表';";
            dbStatement.executeUpdate(createLoanProductsTable);

            // 创建贷款申请表
            String createLoanApplicationsTable = "CREATE TABLE IF NOT EXISTS loan_applications (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    loan_application_id VARCHAR(20) UNIQUE NOT NULL COMMENT '贷款申请ID'," +
                    "    farmer_id BIGINT NOT NULL COMMENT '申请人农户ID'," +
                    "    product_id BIGINT NOT NULL," +
                    "    application_type ENUM('single', 'joint') NOT NULL COMMENT '申请类型：单人、联合'," +
                    "    apply_amount DECIMAL(15,2) NOT NULL," +
                    "    purpose VARCHAR(200) NOT NULL COMMENT '贷款用途'," +
                    "    repayment_source VARCHAR(200) NOT NULL COMMENT '还款来源'," +
                    "    status ENUM('pending', 'pending_partners', 'approved', 'rejected', 'disbursed') DEFAULT 'pending',"
                    +
                    "    approved_amount DECIMAL(15,2) COMMENT '批准的金额'," +
                    "    reject_reason VARCHAR(200) COMMENT '拒绝原因'," +
                    "    approved_by BIGINT COMMENT '审批人银行ID'," +
                    "    approved_at TIMESTAMP NULL," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (product_id) REFERENCES loan_products(id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (approved_by) REFERENCES user_banks(bank_id) ON DELETE SET NULL," +
                    "    INDEX idx_loan_application_id (loan_application_id)," +
                    "    INDEX idx_farmer_id (farmer_id)," +
                    "    INDEX idx_status (status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款申请表';";
            dbStatement.executeUpdate(createLoanApplicationsTable);

            // 创建联合贷款申请表
            String createJointLoanApplicationsTable = "CREATE TABLE IF NOT EXISTS joint_loan_applications (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    loan_application_id BIGINT NOT NULL COMMENT '主贷款申请ID'," +
                    "    partner_farmer_id BIGINT NOT NULL COMMENT '伙伴农户ID'," +
                    "    partner_share_ratio DECIMAL(5,2) NOT NULL COMMENT '伙伴份额比例(%)'," +
                    "    partner_share_amount DECIMAL(15,2) NOT NULL COMMENT '伙伴份额金额'," +
                    "    status ENUM('pending_invitation', 'accepted', 'rejected') DEFAULT 'pending_invitation' COMMENT '伙伴状态',"
                    +
                    "    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '邀请时间'," +
                    "    responded_at TIMESTAMP NULL COMMENT '响应时间'," +
                    "    reject_reason VARCHAR(200) COMMENT '拒绝原因'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (loan_application_id) REFERENCES loan_applications(id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (partner_farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE," +
                    "    INDEX idx_loan_application_id (loan_application_id)," +
                    "    INDEX idx_partner_farmer_id (partner_farmer_id)," +
                    "    INDEX idx_status (status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联合贷款申请表';";
            dbStatement.executeUpdate(createJointLoanApplicationsTable);

            // 创建贷款表
            String createLoansTable = "CREATE TABLE IF NOT EXISTS loans (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    loan_id VARCHAR(20) UNIQUE NOT NULL COMMENT '贷款ID，如LOAN202511140001'," +
                    "    farmer_id BIGINT NOT NULL COMMENT '农户ID'," +
                    "    product_id BIGINT NOT NULL COMMENT '贷款产品ID'," +
                    "    loan_amount DECIMAL(15,2) NOT NULL COMMENT '贷款金额'," +
                    "    interest_rate DECIMAL(5,2) NOT NULL COMMENT '年利率'," +
                    "    term_months INT NOT NULL COMMENT '贷款期限(月)'," +
                    "    repayment_method ENUM('equal_installment', 'interest_first', 'bullet_repayment') NOT NULL COMMENT '还款方式',"
                    +
                    "    disburse_amount DECIMAL(15,2) NOT NULL COMMENT '实际放款金额'," +
                    "    disburse_method ENUM('bank_transfer', 'cash', 'check') NOT NULL COMMENT '放款方式'," +
                    "    disburse_date TIMESTAMP NOT NULL COMMENT '放款日期'," +
                    "    first_repayment_date DATE NOT NULL COMMENT '首次还款日期'," +
                    "    loan_account VARCHAR(50) COMMENT '贷款发放账户'," +
                    "    disburse_remarks VARCHAR(200) COMMENT '放款备注'," +
                    "    loan_status ENUM('pending', 'approved', 'rejected', 'active', 'closed', 'frozen', 'overdue') DEFAULT 'pending' COMMENT '贷款状态',"
                    +
                    "    approved_by BIGINT COMMENT '审批人银行ID'," +
                    "    approved_at TIMESTAMP NULL COMMENT '审批时间'," +
                    "    reject_reason VARCHAR(200) COMMENT '拒绝原因'," +
                    "    closed_date TIMESTAMP NULL COMMENT '结清日期'," +
                    "    total_repayment_amount DECIMAL(15,2) COMMENT '总应还款金额'," +
                    "    total_paid_amount DECIMAL(15,2) DEFAULT 0 COMMENT '累计已还款金额'," +
                    "    total_paid_principal DECIMAL(15,2) DEFAULT 0 COMMENT '累计已还本金'," +
                    "    total_paid_interest DECIMAL(15,2) DEFAULT 0 COMMENT '累计已还利息'," +
                    "    remaining_principal DECIMAL(15,2) NOT NULL COMMENT '剩余本金'," +
                    "    current_period INT DEFAULT 1 COMMENT '当前期数'," +
                    "    next_payment_date DATE COMMENT '下次还款日期'," +
                    "    next_payment_amount DECIMAL(15,2) COMMENT '下次应还金额'," +
                    "    overdue_days INT DEFAULT 0 COMMENT '当前逾期天数'," +
                    "    overdue_amount DECIMAL(15,2) DEFAULT 0 COMMENT '当前逾期金额'," +
                    "    repayment_schedule JSON COMMENT '还款计划数据'," +
                    "    purpose VARCHAR(200) NOT NULL COMMENT '贷款用途'," +
                    "    repayment_source VARCHAR(200) NOT NULL COMMENT '还款来源'," +
                    "    is_joint_loan BOOLEAN DEFAULT FALSE COMMENT '是否为联合贷款'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (product_id) REFERENCES loan_products(id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (approved_by) REFERENCES user_banks(bank_id) ON DELETE SET NULL," +
                    "    INDEX idx_loan_id (loan_id)," +
                    "    INDEX idx_farmer_id (farmer_id)," +
                    "    INDEX idx_loan_status (loan_status)," +
                    "    INDEX idx_next_payment_date (next_payment_date)," +
                    "    INDEX idx_created_at (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款表';";
            dbStatement.executeUpdate(createLoansTable);

            // 创建联合贷款表
            String createJointLoansTable = "CREATE TABLE IF NOT EXISTS joint_loans (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    loan_id BIGINT NOT NULL COMMENT '主贷款ID'," +
                    "    partner_farmer_id BIGINT NOT NULL COMMENT '伙伴农户ID'," +
                    "    partner_share_ratio DECIMAL(5,2) NOT NULL COMMENT '伙伴份额比例(%)'," +
                    "    partner_share_amount DECIMAL(15,2) NOT NULL COMMENT '伙伴份额金额'," +
                    "    partner_principal DECIMAL(15,2) NOT NULL COMMENT '伙伴承担本金'," +
                    "    partner_interest DECIMAL(15,2) NOT NULL COMMENT '伙伴承担利息'," +
                    "    partner_total_repayment DECIMAL(15,2) NOT NULL COMMENT '伙伴总还款额'," +
                    "    partner_paid_amount DECIMAL(15,2) DEFAULT 0 COMMENT '伙伴已还款金额'," +
                    "    partner_remaining_principal DECIMAL(15,2) NOT NULL COMMENT '伙伴剩余本金'," +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "    FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE," +
                    "    FOREIGN KEY (partner_farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE," +
                    "    INDEX idx_loan_id (loan_id)," +
                    "    INDEX idx_partner_farmer_id (partner_farmer_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联合贷款表';";
            dbStatement.executeUpdate(createJointLoansTable);

            dbStatement.close();
            dbConnection.close();

            // 初始化测试数据
            initializeTestLoanData();

            System.out.println("数据库初始化完成");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 关闭连接
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // ============= 社区相关方法 =============

    /**
     * 保存内容到数据库
     */
    public void saveContent(Content content) throws SQLException {
        Connection conn = getConnection();
        try {
            // 插入内容基本信息
            String sql = "INSERT INTO contents (content_id, title, content, content_type, " +
                    "author_user_id, author_nickname, author_role, view_count, comment_count) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, content.getContentId());
            stmt.setString(2, content.getTitle());
            stmt.setString(3, content.getContent());
            stmt.setString(4, content.getContentType());
            stmt.setString(5, content.getAuthorUserId());
            stmt.setString(6, content.getAuthorNickname());
            stmt.setString(7, content.getAuthorRole());
            stmt.setInt(8, content.getViewCount());
            stmt.setInt(9, content.getCommentCount());
            stmt.executeUpdate();
            stmt.close();

            // 插入图片
            if (content.getImages() != null && !content.getImages().isEmpty()) {
                String imgSql = "INSERT INTO content_images (content_id, image_url, sort_order) VALUES (?, ?, ?)";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                for (int i = 0; i < content.getImages().size(); i++) {
                    imgStmt.setString(1, content.getContentId());
                    imgStmt.setString(2, content.getImages().get(i));
                    imgStmt.setInt(3, i);
                    imgStmt.executeUpdate();
                }
                imgStmt.close();
            }
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据贷款申请ID获取贷款申请信息
     */
    public entity.financing.LoanApplication getLoanApplicationById(String applicationId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.LoanApplication loanApplication = null;
        try {
            String sql = "SELECT * FROM loan_applications WHERE loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, applicationId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loanApplication = new entity.financing.LoanApplication();
                loanApplication.setId(rs.getLong("id"));
                loanApplication.setLoanApplicationId(rs.getString("loan_application_id"));
                loanApplication.setFarmerId(rs.getLong("farmer_id"));
                loanApplication.setProductId(rs.getLong("product_id"));
                loanApplication.setApplicationType(rs.getString("application_type"));
                loanApplication.setApplyAmount(rs.getBigDecimal("apply_amount"));
                loanApplication.setPurpose(rs.getString("purpose"));
                loanApplication.setRepaymentSource(rs.getString("repayment_source"));
                loanApplication.setStatus(rs.getString("status"));
                loanApplication.setApprovedAmount(rs.getBigDecimal("approved_amount"));
                loanApplication.setRejectReason(rs.getString("reject_reason"));
                loanApplication.setApprovedBy(rs.getLong("approved_by"));
                loanApplication.setApprovedAt(rs.getTimestamp("approved_at"));
                // 根据 application_type 判断是否为联合贷款
                loanApplication.setJointAgreement("joint".equals(rs.getString("application_type")));
                loanApplication.setRepaymentPlan(null);
                loanApplication.setPartnerPhones(null);
                loanApplication.setCreatedAt(rs.getTimestamp("created_at"));
                loanApplication.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return loanApplication;
    }

    /**
     * 根据贷款申请ID获取所有联合贷款伙伴的手机号
     */
    public List<String> getJointLoanPartnerPhonesByApplicationId(long loanApplicationId) throws SQLException {
        Connection conn = getConnection();
        List<String> partnerPhones = new ArrayList<>();
        try {
            String sql = "SELECT u.phone FROM joint_loan_applications jla " +
                    "JOIN user_farmers uf ON jla.partner_farmer_id = uf.farmer_id " +
                    "JOIN users u ON uf.uid = u.uid " +
                    "WHERE jla.loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, loanApplicationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                partnerPhones.add(rs.getString("phone"));
            }

            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return partnerPhones;
    }

    /**
     * 更新贷款申请状态
     */
    public void updateLoanApplicationStatus(String applicationId, String status, Long approvedBy,
            Timestamp approvedAt, BigDecimal approvedAmount) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE loan_applications SET status = ?, approved_by = ?, approved_at = ?, approved_amount = ? WHERE loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            if (approvedBy != null) {
                stmt.setLong(2, approvedBy);
            } else {
                stmt.setNull(2, java.sql.Types.BIGINT);
            }
            if (approvedAt != null) {
                stmt.setTimestamp(3, approvedAt);
            } else {
                stmt.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (approvedAmount != null) {
                stmt.setBigDecimal(4, approvedAmount);
            } else {
                stmt.setNull(4, java.sql.Types.DECIMAL);
            }
            stmt.setString(5, applicationId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新贷款申请拒绝信息
     */
    public void updateLoanApplicationRejection(String applicationId, String status, Long approvedBy,
            Timestamp approvedAt, String rejectReason) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE loan_applications SET status = ?, approved_by = ?, approved_at = ?, reject_reason = ? WHERE loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            if (approvedBy != null) {
                stmt.setLong(2, approvedBy);
            } else {
                stmt.setNull(2, java.sql.Types.BIGINT);
            }
            if (approvedAt != null) {
                stmt.setTimestamp(3, approvedAt);
            } else {
                stmt.setNull(3, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(4, rejectReason);
            stmt.setString(5, applicationId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 保存贷款记录
     */
    public long saveLoan(entity.financing.Loan loan) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO loans (loan_id, farmer_id, product_id, loan_amount, interest_rate, term_months, "
                    +
                    "repayment_method, disburse_amount, disburse_method, disburse_date, first_repayment_date, " +
                    "loan_account, disburse_remarks, loan_status, approved_by, approved_at, reject_reason, " +
                    "closed_date, total_repayment_amount, total_paid_amount, total_paid_principal, " +
                    "total_paid_interest, remaining_principal, current_period, next_payment_date, " +
                    "next_payment_amount, overdue_days, overdue_amount, repayment_schedule, purpose, " +
                    "repayment_source, is_joint_loan, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, loan.getLoanId());
            stmt.setLong(2, loan.getFarmerId());
            stmt.setLong(3, loan.getProductId());
            stmt.setBigDecimal(4, loan.getLoanAmount());
            stmt.setBigDecimal(5, loan.getInterestRate());
            stmt.setInt(6, loan.getTermMonths());
            stmt.setString(7, loan.getRepaymentMethod());
            stmt.setBigDecimal(8, loan.getDisburseAmount());
            stmt.setString(9, loan.getDisburseMethod());
            stmt.setTimestamp(10, loan.getDisburseDate());
            stmt.setDate(11, loan.getFirstRepaymentDate());
            stmt.setString(12, loan.getLoanAccount());
            stmt.setString(13, loan.getDisburseRemarks());
            stmt.setString(14, loan.getLoanStatus());
            if (loan.getApprovedBy() != null) {
                stmt.setLong(15, loan.getApprovedBy());
            } else {
                stmt.setNull(15, java.sql.Types.BIGINT);
            }
            if (loan.getApprovedAt() != null) {
                stmt.setTimestamp(16, loan.getApprovedAt());
            } else {
                stmt.setNull(16, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(17, loan.getRejectReason());
            if (loan.getClosedDate() != null) {
                stmt.setTimestamp(18, loan.getClosedDate());
            } else {
                stmt.setNull(18, java.sql.Types.TIMESTAMP);
            }
            stmt.setBigDecimal(19, loan.getTotalRepaymentAmount());
            stmt.setBigDecimal(20, loan.getTotalPaidAmount());
            stmt.setBigDecimal(21, loan.getTotalPaidPrincipal());
            stmt.setBigDecimal(22, loan.getTotalPaidInterest());
            stmt.setBigDecimal(23, loan.getRemainingPrincipal());
            stmt.setInt(24, loan.getCurrentPeriod());
            if (loan.getNextPaymentDate() != null) {
                stmt.setDate(25, loan.getNextPaymentDate());
            } else {
                stmt.setNull(25, java.sql.Types.DATE);
            }
            stmt.setBigDecimal(26, loan.getNextPaymentAmount());
            stmt.setInt(27, loan.getOverdueDays());
            stmt.setBigDecimal(28, loan.getOverdueAmount());
            stmt.setString(29, loan.getRepaymentSchedule());
            stmt.setString(30, loan.getPurpose());
            stmt.setString(31, loan.getRepaymentSource());
            stmt.setBoolean(32, loan.getIsJointLoan());
            stmt.setTimestamp(33, loan.getCreatedAt());
            stmt.setTimestamp(34, loan.getUpdatedAt());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("创建贷款记录失败，没有行受到影响");
            }

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            long id = 0;
            if (generatedKeys.next()) {
                id = generatedKeys.getLong(1);
            }
            generatedKeys.close();
            stmt.close();
            return id;
        } finally {
            closeConnection();
        }
    }

    /**
     * 保存联合贷款记录
     */
    public void saveJointLoan(entity.financing.JointLoan jointLoan) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO joint_loans (loan_id, partner_farmer_id, partner_share_ratio, partner_share_amount, "
                    +
                    "partner_principal, partner_interest, partner_total_repayment, partner_paid_amount, " +
                    "partner_remaining_principal, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, jointLoan.getLoanId());
            stmt.setLong(2, jointLoan.getPartnerFarmerId());
            stmt.setBigDecimal(3, jointLoan.getPartnerShareRatio());
            stmt.setBigDecimal(4, jointLoan.getPartnerShareAmount());
            stmt.setBigDecimal(5, jointLoan.getPartnerPrincipal());
            stmt.setBigDecimal(6, jointLoan.getPartnerInterest());
            stmt.setBigDecimal(7, jointLoan.getPartnerTotalRepayment());
            stmt.setBigDecimal(8, jointLoan.getPartnerPaidAmount());
            stmt.setBigDecimal(9, jointLoan.getPartnerRemainingPrincipal());
            stmt.setTimestamp(10, jointLoan.getCreatedAt());
            stmt.setTimestamp(11, jointLoan.getUpdatedAt());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新用户余额
     */
    public void updateUserBalance(String uid, BigDecimal amount) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE users SET money = money + ? WHERE uid = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, uid);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新信用额度
     */
    public void updateCreditLimitUsed(Long farmerId, BigDecimal usedAmount) throws SQLException {
        Connection conn = getConnection();
        try {
            // 先获取当前信用额度信息
            String selectSql = "SELECT total_limit, used_limit FROM credit_limits WHERE farmer_id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setLong(1, farmerId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                BigDecimal totalLimit = rs.getBigDecimal("total_limit");
                BigDecimal currentUsedLimit = rs.getBigDecimal("used_limit");
                BigDecimal newUsedLimit = currentUsedLimit.add(usedAmount);
                BigDecimal newAvailableLimit = totalLimit.subtract(newUsedLimit);

                // 更新信用额度
                String updateSql = "UPDATE credit_limits SET used_limit = ?, available_limit = ?, last_updated = ? WHERE farmer_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setBigDecimal(1, newUsedLimit);
                updateStmt.setBigDecimal(2, newAvailableLimit);
                updateStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                updateStmt.setLong(4, farmerId);
                updateStmt.executeUpdate();
                updateStmt.close();
            }
            rs.close();
            selectStmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 保存联合贷款申请的伙伴记录
     */
    public void saveJointLoanApplicationPartners(long loanApplicationId, List<Map<String, Object>> partners)
            throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO joint_loan_applications (loan_application_id, partner_farmer_id, partner_share_ratio, partner_share_amount, status) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            for (Map<String, Object> partner : partners) {
                stmt.setLong(1, loanApplicationId);
                stmt.setLong(2, (Long) partner.get("partner_farmer_id"));
                stmt.setBigDecimal(3, (BigDecimal) partner.get("partner_share_ratio"));
                stmt.setBigDecimal(4, (BigDecimal) partner.get("partner_share_amount"));
                stmt.setString(5, "pending_invitation");
                stmt.executeUpdate();
            }

            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据贷款申请ID获取联合贷款伙伴信息
     */
    public List<Map<String, Object>> getJointLoanPartnersByApplicationId(long loanApplicationId) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> partners = new ArrayList<>();
        try {
            String sql = "SELECT jla.*, uf.uid, u.phone FROM joint_loan_applications jla " +
                    "JOIN user_farmers uf ON jla.partner_farmer_id = uf.farmer_id " +
                    "JOIN users u ON uf.uid = u.uid " +
                    "WHERE jla.loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, loanApplicationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> partner = new HashMap<>();
                partner.put("id", rs.getLong("id"));
                partner.put("loan_application_id", rs.getLong("loan_application_id"));
                partner.put("partner_farmer_id", rs.getLong("partner_farmer_id"));
                partner.put("partner_share_ratio", rs.getBigDecimal("partner_share_ratio"));
                partner.put("partner_share_amount", rs.getBigDecimal("partner_share_amount"));
                partner.put("status", rs.getString("status"));
                partner.put("invited_at", rs.getTimestamp("invited_at"));
                partner.put("responded_at", rs.getTimestamp("responded_at"));
                partner.put("reject_reason", rs.getString("reject_reason"));
                partner.put("phone", rs.getString("phone"));
                partners.add(partner);
            }

            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return partners;
    }

    /**
     * 根据ID查找内容
     */
    public Content findContentById(String contentId) throws SQLException {
        Connection conn = getConnection();
        Content content = null;
        try {
            String sql = "SELECT * FROM contents WHERE content_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, contentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                content = new Content();
                content.setContentId(rs.getString("content_id"));
                content.setTitle(rs.getString("title"));
                content.setContent(rs.getString("content"));
                content.setContentType(rs.getString("content_type"));
                content.setAuthorUserId(rs.getString("author_user_id"));
                content.setAuthorNickname(rs.getString("author_nickname"));
                content.setAuthorRole(rs.getString("author_role"));
                content.setViewCount(rs.getInt("view_count"));
                content.setCommentCount(rs.getInt("comment_count"));
                content.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                content.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                // 查询图片
                String imgSql = "SELECT image_url FROM content_images WHERE content_id = ? ORDER BY sort_order";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setString(1, contentId);
                ResultSet imgRs = imgStmt.executeQuery();
                List<String> images = new ArrayList<>();
                while (imgRs.next()) {
                    images.add(imgRs.getString("image_url"));
                }
                content.setImages(images);
                imgRs.close();
                imgStmt.close();
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return content;
    }

    /**
     * 查找内容列表（带过滤和排序）
     */
    public List<Content> findContents(String contentType, String keyword, String sort) throws SQLException {
        Connection conn = getConnection();
        List<Content> contents = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM contents WHERE 1=1");
            List<Object> params = new ArrayList<>();

            // 添加内容类型过滤
            if (contentType != null && !contentType.trim().isEmpty()) {
                sql.append(" AND content_type = ?");
                params.add(contentType);
            }

            // 添加关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND (title LIKE ? OR content LIKE ?)");
                String searchPattern = "%" + keyword + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            // 添加排序
            if (sort != null && !sort.trim().isEmpty()) {
                switch (sort) {
                    case "hottest":
                        sql.append(" ORDER BY view_count DESC");
                        break;
                    case "commented":
                        sql.append(" ORDER BY comment_count DESC");
                        break;
                    case "newest":
                    default:
                        sql.append(" ORDER BY created_at DESC");
                        break;
                }
            } else {
                sql.append(" ORDER BY created_at DESC");
            }

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Content content = new Content();
                content.setContentId(rs.getString("content_id"));
                content.setTitle(rs.getString("title"));
                content.setContent(rs.getString("content"));
                content.setContentType(rs.getString("content_type"));
                content.setAuthorUserId(rs.getString("author_user_id"));
                content.setAuthorNickname(rs.getString("author_nickname"));
                content.setAuthorRole(rs.getString("author_role"));
                content.setViewCount(rs.getInt("view_count"));
                content.setCommentCount(rs.getInt("comment_count"));
                content.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                content.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                // 查询图片
                String imgSql = "SELECT image_url FROM content_images WHERE content_id = ? ORDER BY sort_order";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setString(1, content.getContentId());
                ResultSet imgRs = imgStmt.executeQuery();
                List<String> images = new ArrayList<>();
                while (imgRs.next()) {
                    images.add(imgRs.getString("image_url"));
                }
                content.setImages(images);
                imgRs.close();
                imgStmt.close();

                contents.add(content);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return contents;
    }

    /**
     * 增加浏览量
     */
    public void incrementViewCount(String contentId) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE contents SET view_count = view_count + 1 WHERE content_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, contentId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 增加评论数
     */
    public void incrementCommentCount(String contentId) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE contents SET comment_count = comment_count + 1 WHERE content_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, contentId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 保存评论到数据库
     */
    public void saveComment(Comment comment) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO comments (comment_id, content_id, parent_comment_id, " +
                    "author_user_id, author_nickname, author_role, reply_to_user_id, " +
                    "reply_to_nickname, content) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, comment.getCommentId());
            stmt.setString(2, comment.getContentId());
            stmt.setString(3, comment.getParentCommentId());
            stmt.setString(4, comment.getAuthorUserId());
            stmt.setString(5, comment.getAuthorNickname());
            stmt.setString(6, comment.getAuthorRole());
            stmt.setString(7, comment.getReplyToUserId());
            stmt.setString(8, comment.getReplyToNickname());
            stmt.setString(9, comment.getContent());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据ID查找评论
     */
    public Comment findCommentById(String commentId) throws SQLException {
        Connection conn = getConnection();
        Comment comment = null;
        try {
            String sql = "SELECT * FROM comments WHERE comment_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, commentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                comment = new Comment();
                comment.setCommentId(rs.getString("comment_id"));
                comment.setContentId(rs.getString("content_id"));
                comment.setParentCommentId(rs.getString("parent_comment_id"));
                comment.setAuthorUserId(rs.getString("author_user_id"));
                comment.setAuthorNickname(rs.getString("author_nickname"));
                comment.setAuthorRole(rs.getString("author_role"));
                comment.setReplyToUserId(rs.getString("reply_to_user_id"));
                comment.setReplyToNickname(rs.getString("reply_to_nickname"));
                comment.setContent(rs.getString("content"));
                comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return comment;
    }

    /**
     * 根据内容ID查找所有评论
     */
    public List<Comment> findCommentsByContentId(String contentId) throws SQLException {
        Connection conn = getConnection();
        List<Comment> comments = new ArrayList<>();
        try {
            String sql = "SELECT * FROM comments WHERE content_id = ? ORDER BY created_at ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, contentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Comment comment = new Comment();
                comment.setCommentId(rs.getString("comment_id"));
                comment.setContentId(rs.getString("content_id"));
                comment.setParentCommentId(rs.getString("parent_comment_id"));
                comment.setAuthorUserId(rs.getString("author_user_id"));
                comment.setAuthorNickname(rs.getString("author_nickname"));
                comment.setAuthorRole(rs.getString("author_role"));
                comment.setReplyToUserId(rs.getString("reply_to_user_id"));
                comment.setReplyToNickname(rs.getString("reply_to_nickname"));
                comment.setContent(rs.getString("content"));
                comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                comments.add(comment);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return comments;
    }

    /**
     * 获取用户角色（支持多重身份）
     */
    public List<String> getUserRole(String uid) throws SQLException {
        Connection conn = getConnection();
        List<String> roles = new ArrayList<>();
        try {
            // 检查是否是农户
            String farmerSql = "SELECT 1 FROM user_farmers WHERE uid = ? AND enable = TRUE";
            PreparedStatement farmerStmt = conn.prepareStatement(farmerSql);
            farmerStmt.setString(1, uid);
            ResultSet farmerRs = farmerStmt.executeQuery();
            if (farmerRs.next()) {
                roles.add("farmer");
            }
            farmerRs.close();
            farmerStmt.close();

            // 检查是否是专家
            String expertSql = "SELECT 1 FROM user_experts WHERE uid = ? AND enable = TRUE";
            PreparedStatement expertStmt = conn.prepareStatement(expertSql);
            expertStmt.setString(1, uid);
            ResultSet expertRs = expertStmt.executeQuery();
            if (expertRs.next()) {
                roles.add("expert");
            }
            expertRs.close();
            expertStmt.close();

            // 检查是否是买家
            String buyerSql = "SELECT 1 FROM user_buyers WHERE uid = ? AND enable = TRUE";
            PreparedStatement buyerStmt = conn.prepareStatement(buyerSql);
            buyerStmt.setString(1, uid);
            ResultSet buyerRs = buyerStmt.executeQuery();
            if (buyerRs.next()) {
                roles.add("buyer");
            }
            buyerRs.close();
            buyerStmt.close();

            // 检查是否是银行用户
            String bankSql = "SELECT 1 FROM user_banks WHERE uid = ? AND enable = TRUE";
            PreparedStatement bankStmt = conn.prepareStatement(bankSql);
            bankStmt.setString(1, uid);
            ResultSet bankRs = bankStmt.executeQuery();
            if (bankRs.next()) {
                roles.add("bank");
            }
            bankRs.close();
            bankStmt.close();
        } finally {
            closeConnection();
        }
        return roles;
    }

    // ============= 订单相关方法 =============

    /**
     * 创建订单
     */
    public void createOrder(entity.Order order) throws SQLException {
        Connection conn = getConnection();
        try {
            // 由于订单直接创建为 shipped 状态，需要设置 shipped_at 时间
            String sql = "INSERT INTO orders (order_id, buyer_uid, farmer_uid, product_id, " +
                    "product_title, product_specification, product_price, quantity, total_amount, " +
                    "buyer_name, buyer_address, buyer_phone, remark, status, shipped_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, order.getOrderId());
            stmt.setString(2, order.getBuyerUid());
            stmt.setString(3, order.getFarmerUid());
            stmt.setLong(4, order.getProductId());
            stmt.setString(5, order.getProductTitle());
            stmt.setString(6, order.getProductSpecification());
            stmt.setBigDecimal(7, order.getProductPrice());
            stmt.setInt(8, order.getQuantity());
            stmt.setBigDecimal(9, order.getTotalAmount());
            stmt.setString(10, order.getBuyerName());
            stmt.setString(11, order.getBuyerAddress());
            stmt.setString(12, order.getBuyerPhone());
            stmt.setString(13, order.getRemark());
            stmt.setString(14, order.getStatus());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据订单ID查找订单
     */
    public entity.Order findOrderById(String orderId) throws SQLException {
        Connection conn = getConnection();
        entity.Order order = null;
        try {
            String sql = "SELECT * FROM orders WHERE order_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                order = new entity.Order();
                order.setOrderId(rs.getString("order_id"));
                order.setBuyerUid(rs.getString("buyer_uid"));
                order.setFarmerUid(rs.getString("farmer_uid"));
                order.setProductId(rs.getLong("product_id"));
                order.setProductTitle(rs.getString("product_title"));
                order.setProductSpecification(rs.getString("product_specification"));
                order.setProductPrice(rs.getBigDecimal("product_price"));
                order.setQuantity(rs.getInt("quantity"));
                order.setTotalAmount(rs.getBigDecimal("total_amount"));
                order.setBuyerName(rs.getString("buyer_name"));
                order.setBuyerAddress(rs.getString("buyer_address"));
                order.setBuyerPhone(rs.getString("buyer_phone"));
                order.setRemark(rs.getString("remark"));
                order.setStatus(rs.getString("status"));
                order.setRefundReason(rs.getString("refund_reason"));
                order.setRefundType(rs.getString("refund_type"));
                order.setCreatedAt(rs.getTimestamp("created_at"));
                order.setUpdatedAt(rs.getTimestamp("updated_at"));
                order.setShippedAt(rs.getTimestamp("shipped_at"));
                order.setCompletedAt(rs.getTimestamp("completed_at"));
                order.setCancelledAt(rs.getTimestamp("cancelled_at"));
                order.setRefundedAt(rs.getTimestamp("refunded_at"));

                // 查询商品图片
                String imgSql = "SELECT image_url FROM product_images WHERE product_id = ? ORDER BY sort_order LIMIT 3";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setLong(1, order.getProductId());
                ResultSet imgRs = imgStmt.executeQuery();
                List<String> images = new ArrayList<>();
                while (imgRs.next()) {
                    images.add(imgRs.getString("image_url"));
                }
                order.setImages(images);
                imgRs.close();
                imgStmt.close();
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return order;
    }

    /**
     * 更新订单信息
     */
    public void updateOrder(String orderId, String buyerName, String buyerAddress, String remark) throws SQLException {
        Connection conn = getConnection();
        try {
            StringBuilder sql = new StringBuilder("UPDATE orders SET ");
            List<Object> params = new ArrayList<>();

            if (buyerName != null && !buyerName.trim().isEmpty()) {
                sql.append("buyer_name = ?, ");
                params.add(buyerName);
            }
            if (buyerAddress != null && !buyerAddress.trim().isEmpty()) {
                sql.append("buyer_address = ?, ");
                params.add(buyerAddress);
            }
            if (remark != null) {
                sql.append("remark = ?, ");
                params.add(remark);
            }

            // 移除最后的逗号和空格
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE order_id = ?");
            params.add(orderId);

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新订单状态
     */
    public void updateOrderStatus(String orderId, String status, Timestamp timestamp) throws SQLException {
        Connection conn = getConnection();
        try {
            String timestampField = null;
            switch (status) {
                case "shipped":
                    timestampField = "shipped_at";
                    break;
                case "completed":
                    timestampField = "completed_at";
                    break;
                case "cancelled":
                    timestampField = "cancelled_at";
                    break;
                case "refunded":
                    timestampField = "refunded_at";
                    break;
            }

            String sql;
            if (timestampField != null) {
                sql = "UPDATE orders SET status = ?, " + timestampField + " = ? WHERE order_id = ?";
            } else {
                sql = "UPDATE orders SET status = ? WHERE order_id = ?";
            }

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            if (timestampField != null) {
                stmt.setTimestamp(2, timestamp);
                stmt.setString(3, orderId);
            } else {
                stmt.setString(2, orderId);
            }
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新订单退款信息（不修改订单状态，状态由调用方通过 updateOrderStatus 设置）
     */
    public void updateOrderRefund(String orderId, String refundReason, String refundType) throws SQLException {
        Connection conn = getConnection();
        try {
            // 关闭自动提交以使用事务
            conn.setAutoCommit(false);

            String sql = "UPDATE orders SET refund_reason = ?, refund_type = ? WHERE order_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, refundReason);
            stmt.setString(2, refundType);
            stmt.setString(3, orderId);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("updateOrderRefund - 更新行数: " + rowsAffected + ", orderId: " + orderId
                    + ", refundReason: " + refundReason + ", refundType: " + refundType);

            stmt.close();

            // 提交事务
            conn.commit();
        } catch (SQLException e) {
            // 回滚事务
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw e;
        } finally {
            try {
                // 恢复自动提交
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            closeConnection();
        }
    }

    /**
     * 获取买家订单列表
     */
    public List<entity.Order> findOrdersByBuyer(String buyerUid, String status, String title) throws SQLException {
        Connection conn = getConnection();
        List<entity.Order> orders = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE buyer_uid = ?");
            List<Object> params = new ArrayList<>();
            params.add(buyerUid);

            if (status != null && !status.trim().isEmpty()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (title != null && !title.trim().isEmpty()) {
                sql.append(" AND product_title LIKE ?");
                params.add("%" + title + "%");
            }

            sql.append(" ORDER BY created_at DESC");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                entity.Order order = new entity.Order();
                order.setOrderId(rs.getString("order_id"));
                order.setBuyerUid(rs.getString("buyer_uid"));
                order.setFarmerUid(rs.getString("farmer_uid"));
                order.setProductId(rs.getLong("product_id"));
                order.setProductTitle(rs.getString("product_title"));
                order.setProductSpecification(rs.getString("product_specification"));
                order.setProductPrice(rs.getBigDecimal("product_price"));
                order.setQuantity(rs.getInt("quantity"));
                order.setTotalAmount(rs.getBigDecimal("total_amount"));
                order.setBuyerName(rs.getString("buyer_name"));
                order.setBuyerAddress(rs.getString("buyer_address"));
                order.setBuyerPhone(rs.getString("buyer_phone"));
                order.setRemark(rs.getString("remark"));
                order.setStatus(rs.getString("status"));
                order.setCreatedAt(rs.getTimestamp("created_at"));

                // 查询商品主图
                String imgSql = "SELECT image_url FROM product_images WHERE product_id = ? AND is_main = TRUE LIMIT 1";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setLong(1, order.getProductId());
                ResultSet imgRs = imgStmt.executeQuery();
                List<String> images = new ArrayList<>();
                if (imgRs.next()) {
                    images.add(imgRs.getString("image_url"));
                }
                order.setImages(images);
                imgRs.close();
                imgStmt.close();

                orders.add(order);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return orders;
    }

    /**
     * 根据买家手机号获取买家UID
     */
    public String getBuyerUidByPhone(String phone) throws SQLException {
        Connection conn = getConnection();
        String uid = null;
        try {
            String sql = "SELECT u.uid FROM users u " +
                    "JOIN user_buyers ub ON u.uid = ub.uid " +
                    "WHERE u.phone = ? AND ub.enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                uid = rs.getString("uid");
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return uid;
    }

    /**
     * 根据买家手机号获取买家余额
     */
    public java.math.BigDecimal getBuyerBalance(String phone) throws SQLException {
        Connection conn = getConnection();
        java.math.BigDecimal balance = null;
        try {
            String sql = "SELECT u.money FROM users u " +
                    "JOIN user_buyers ub ON u.uid = ub.uid " +
                    "WHERE u.phone = ? AND ub.enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                balance = rs.getBigDecimal("money");
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return balance;
    }

    /**
     * 根据农户手机号获取农户余额
     */
    public java.math.BigDecimal getFarmerBalance(String phone) throws SQLException {
        Connection conn = getConnection();
        java.math.BigDecimal balance = null;
        try {
            String sql = "SELECT u.money FROM users u " +
                    "JOIN user_farmers uf ON u.uid = uf.uid " +
                    "WHERE u.phone = ? AND uf.enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                balance = rs.getBigDecimal("money");
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return balance;
    }

    /**
     * 根据银行手机号获取银行余额
     */
    public java.math.BigDecimal getBankBalance(String phone) throws SQLException {
        Connection conn = getConnection();
        java.math.BigDecimal balance = null;
        try {
            String sql = "SELECT u.money FROM users u " +
                    "JOIN user_banks ub ON u.uid = ub.uid " +
                    "WHERE u.phone = ? AND ub.enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                balance = rs.getBigDecimal("money");
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return balance;
    }

    /**
     * 更新买家余额
     */
    public void updateBuyerBalance(String buyerUid, java.math.BigDecimal amount) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE users SET money = money + ? WHERE uid = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, buyerUid);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新农户余额
     */
    public void updateFarmerBalance(String farmerUid, java.math.BigDecimal amount) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE users SET money = money + ? WHERE uid = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, farmerUid);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新商品库存
     */
    public void updateProductStock(Long productId, int change) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE products SET stock = stock + ? WHERE product_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, change);
            stmt.setLong(2, productId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新商品销量
     */
    public void updateProductSalesCount(Long productId, int change) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE products SET sales_count = sales_count + ? WHERE product_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, change);
            stmt.setLong(2, productId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据商品ID获取商品信息
     */
    public entity.Product getProductById(Long productId) throws SQLException {
        Connection conn = getConnection();
        entity.Product product = null;
        try {
            String sql = "SELECT * FROM products WHERE product_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                product = new entity.Product();
                product.setProductId(rs.getLong("product_id"));
                product.setFarmerId(rs.getLong("farmer_id"));
                product.setCategory(rs.getString("category"));
                product.setTitle(rs.getString("title"));
                product.setDetailedDescription(rs.getString("detailed_description"));
                product.setPrice(rs.getDouble("price"));
                product.setStock(rs.getInt("stock"));
                product.setDescription(rs.getString("description"));
                product.setOrigin(rs.getString("origin"));
                product.setStatus(rs.getString("status"));
                product.setViewCount(rs.getInt("view_count"));
                product.setSalesCount(rs.getInt("sales_count"));
                product.setEnable(rs.getBoolean("enable"));
                product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                // 查询商品图片
                String imgSql = "SELECT image_url FROM product_images WHERE product_id = ? ORDER BY sort_order";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setLong(1, productId);
                ResultSet imgRs = imgStmt.executeQuery();
                List<String> images = new ArrayList<>();
                while (imgRs.next()) {
                    images.add(imgRs.getString("image_url"));
                }
                product.setImages(images);
                imgRs.close();
                imgStmt.close();
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return product;
    }

    /**
     * 根据农户ID获取农户UID
     */
    public String getFarmerUidByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        String farmerUid = null;
        try {
            String sql = "SELECT uid FROM user_farmers WHERE farmer_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                farmerUid = rs.getString("uid");
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return farmerUid;
    }

    /**
     * 根据农户手机号获取农户UID
     */
    public String getFarmerUidByPhone(String phone) throws SQLException {
        Connection conn = getConnection();
        String uid = null;
        try {
            String sql = "SELECT u.uid FROM users u " +
                    "JOIN user_farmers uf ON u.uid = uf.uid " +
                    "WHERE u.phone = ? AND uf.enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                uid = rs.getString("uid");
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return uid;
    }

    /**
     * 获取农户订单列表
     */
    public List<entity.Order> findOrdersByFarmer(String farmerUid, String status, String title) throws SQLException {
        Connection conn = getConnection();
        List<entity.Order> orders = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE farmer_uid = ?");
            List<Object> params = new ArrayList<>();
            params.add(farmerUid);

            if (status != null && !status.trim().isEmpty()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (title != null && !title.trim().isEmpty()) {
                sql.append(" AND product_title LIKE ?");
                params.add("%" + title + "%");
            }

            sql.append(" ORDER BY created_at DESC");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                entity.Order order = new entity.Order();
                order.setOrderId(rs.getString("order_id"));
                order.setBuyerUid(rs.getString("buyer_uid"));
                order.setFarmerUid(rs.getString("farmer_uid"));
                order.setProductId(rs.getLong("product_id"));
                order.setProductTitle(rs.getString("product_title"));
                order.setProductSpecification(rs.getString("product_specification"));
                order.setProductPrice(rs.getBigDecimal("product_price"));
                order.setQuantity(rs.getInt("quantity"));
                order.setTotalAmount(rs.getBigDecimal("total_amount"));
                order.setBuyerName(rs.getString("buyer_name"));
                order.setBuyerAddress(rs.getString("buyer_address"));
                order.setBuyerPhone(rs.getString("buyer_phone"));
                order.setRemark(rs.getString("remark"));
                order.setStatus(rs.getString("status"));
                order.setCreatedAt(rs.getTimestamp("created_at"));

                // 查询商品主图
                String imgSql = "SELECT image_url FROM product_images WHERE product_id = ? AND is_main = TRUE LIMIT 1";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setLong(1, order.getProductId());
                ResultSet imgRs = imgStmt.executeQuery();
                List<String> images = new ArrayList<>();
                if (imgRs.next()) {
                    images.add(imgRs.getString("image_url"));
                }
                order.setImages(images);
                imgRs.close();
                imgStmt.close();

                orders.add(order);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return orders;
    }

    // 在 DatabaseManager 类中添加以下方法

    /**
     * 根据手机号查找用户
     */
    public entity.User findUserByPhone(String phone) throws SQLException {
        Connection conn = getConnection();
        entity.User user = null;
        try {
            String sql = "SELECT * FROM users WHERE phone = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                user = new entity.User();
                user.setUid(rs.getString("uid"));
                user.setPhone(rs.getString("phone"));
                user.setPassword(rs.getString("password"));
                user.setNickname(rs.getString("nickname"));
                user.setMoney(rs.getBigDecimal("money"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return user;
    }

    public List<Map<String, Object>> getQualifiedPartners(BigDecimal minCreditLimit, List<String> excludePhones,
            int maxPartners) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> partners = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT u.phone, u.nickname, cl.available_limit, cl.total_limit ");
            sql.append("FROM users u ");
            sql.append("JOIN user_farmers uf ON u.uid = uf.uid ");
            sql.append("JOIN credit_limits cl ON uf.farmer_id = cl.farmer_id ");
            sql.append("WHERE uf.enable = TRUE ");
            sql.append("AND cl.status = 'active' ");
            sql.append("AND cl.available_limit >= ? ");

            List<Object> params = new ArrayList<>();
            params.add(minCreditLimit);

            if (excludePhones != null && !excludePhones.isEmpty()) {
                sql.append("AND u.phone NOT IN (");
                for (int i = 0; i < excludePhones.size(); i++) {
                    sql.append("?");
                    if (i < excludePhones.size() - 1) {
                        sql.append(",");
                    }
                    params.add(excludePhones.get(i));
                }
                sql.append(") ");
            }

            sql.append("ORDER BY cl.available_limit DESC ");
            sql.append("LIMIT ? ");
            params.add(maxPartners);

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> partner = new HashMap<>();
                partner.put("phone", rs.getString("phone"));
                partner.put("nickname", rs.getString("nickname"));
                partner.put("available_limit", rs.getBigDecimal("available_limit"));
                partner.put("total_limit", rs.getBigDecimal("total_limit"));
                partners.add(partner);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return partners;
    }

    /**
     * 更新合作伙伴确认状态
     */
    public void updatePartnerConfirmationStatus(Long loanApplicationId, Long partnerFarmerId, String status) throws SQLException {
        Connection conn = getConnection();
        try {
            // 将确认状态映射到现有的枚举值
            String dbStatus = "confirmed".equals(status) ? "accepted" : "rejected";
            String sql = "UPDATE joint_loan_applications SET status = ?, responded_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE loan_application_id = ? AND partner_farmer_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, dbStatus);
            stmt.setLong(2, loanApplicationId);
            stmt.setLong(3, partnerFarmerId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 检查所有伙伴是否都已确认
     */
    public boolean areAllPartnersConfirmed(Long loanApplicationId) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "SELECT COUNT(*) as total_partners, " +
                    "SUM(CASE WHEN status = 'accepted' THEN 1 ELSE 0 END) as confirmed_partners " +
                    "FROM joint_loan_applications WHERE loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, loanApplicationId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int totalPartners = rs.getInt("total_partners");
                int confirmedPartners = rs.getInt("confirmed_partners");
                rs.close();
                stmt.close();
                return totalPartners > 0 && totalPartners == confirmedPartners;
            }
            
            rs.close();
            stmt.close();
            return false;
        } finally {
            closeConnection();
        }
    }

    /**
     * 获取用户待确认的联合贷款申请
     */
    public List<Map<String, Object>> getPendingJointLoanApplicationsByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> applications = new ArrayList<>();
        try {
            String sql = "SELECT la.*, lp.product_name, lp.interest_rate, lp.term_months, " +
                    "jla.partner_share_amount, jla.status as partner_status, " +
                    "initiator.phone as initiator_phone, initiator.nickname as initiator_nickname " +
                    "FROM loan_applications la " +
                    "JOIN joint_loan_applications jla ON la.id = jla.loan_application_id " +
                    "JOIN loan_products lp ON la.product_id = lp.id " +
                    "JOIN user_farmers uf ON la.farmer_id = uf.farmer_id " +
                    "JOIN users initiator ON uf.uid = initiator.uid " +
                    "WHERE jla.partner_farmer_id = ? " +
                    "AND la.status = 'pending_partners' " +
                    "AND jla.status = 'pending_invitation' " +
                    "ORDER BY la.created_at DESC";
                    
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> application = new HashMap<>();
                application.put("loan_application_id", rs.getString("loan_application_id"));
                application.put("product_name", rs.getString("product_name"));
                application.put("apply_amount", rs.getBigDecimal("apply_amount"));
                application.put("partner_share_amount", rs.getBigDecimal("partner_share_amount"));
                application.put("purpose", rs.getString("purpose"));
                application.put("repayment_source", rs.getString("repayment_source"));
                application.put("interest_rate", rs.getBigDecimal("interest_rate"));
                application.put("term_months", rs.getInt("term_months"));
                application.put("initiator_phone", rs.getString("initiator_phone"));
                application.put("initiator_nickname", rs.getString("initiator_nickname"));
                application.put("partner_status", rs.getString("partner_status"));
                application.put("created_at", rs.getTimestamp("created_at"));
                applications.add(application);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return applications;
    }

    /**
     * 根据特定金额需求获取符合条件的伙伴列表
     */
    public List<Map<String, Object>> getQualifiedPartnersForAmount(BigDecimal requiredAmount, List<String> excludePhones,
            int maxPartners) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> partners = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT u.phone, u.nickname, cl.available_limit, cl.total_limit ");
            sql.append("FROM users u ");
            sql.append("JOIN user_farmers uf ON u.uid = uf.uid ");
            sql.append("JOIN credit_limits cl ON uf.farmer_id = cl.farmer_id ");
            sql.append("WHERE uf.enable = TRUE ");
            sql.append("AND cl.status = 'active' ");
            sql.append("AND cl.available_limit >= ? "); // 至少能承担所需的金额

            List<Object> params = new ArrayList<>();
            params.add(requiredAmount);

            if (excludePhones != null && !excludePhones.isEmpty()) {
                sql.append("AND u.phone NOT IN (");
                for (int i = 0; i < excludePhones.size(); i++) {
                    sql.append("?");
                    if (i < excludePhones.size() - 1) {
                        sql.append(",");
                    }
                    params.add(excludePhones.get(i));
                }
                sql.append(") ");
            }

            // 按照可用额度降序排列，优先推荐额度高的伙伴
            sql.append("ORDER BY cl.available_limit DESC ");
            sql.append("LIMIT ? ");
            params.add(maxPartners);

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> partner = new HashMap<>();
                partner.put("phone", rs.getString("phone"));
                partner.put("nickname", rs.getString("nickname"));
                partner.put("available_limit", rs.getBigDecimal("available_limit"));
                partner.put("total_limit", rs.getBigDecimal("total_limit"));
                partners.add(partner);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return partners;
    }

    /**
     * 根据 UID 查找银行用户信息
     */
    public Map<String, Object> getBankInfoByUid(String uid) throws SQLException {
        Connection conn = getConnection();
        Map<String, Object> bankInfo = null;
        try {
            String sql = "SELECT * FROM user_banks WHERE uid = ? AND enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                bankInfo = new HashMap<>();
                bankInfo.put("bank_id", rs.getLong("bank_id"));
                bankInfo.put("uid", rs.getString("uid"));
                bankInfo.put("bank_name", rs.getString("bank_name"));
                bankInfo.put("branch_name", rs.getString("branch_name"));
                bankInfo.put("contact_person", rs.getString("contact_person"));
                bankInfo.put("contact_phone", rs.getString("contact_phone"));
                bankInfo.put("enable", rs.getBoolean("enable"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return bankInfo;
    }

    /**
     * 根据贷款申请ID获取贷款申请详情（包括联合贷款伙伴信息）
     */
    public Map<String, Object> getLoanApplicationDetails(String applicationId) throws SQLException {
        Connection conn = getConnection();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> partners = new ArrayList<>();

        try {
            // 获取贷款申请基本信息
            String sql = "SELECT la.*, lp.product_name, lp.interest_rate, lp.term_months, lp.repayment_method " +
                    "FROM loan_applications la " +
                    "JOIN loan_products lp ON la.product_id = lp.id " +
                    "WHERE la.loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, applicationId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                result.put("id", rs.getLong("id"));
                result.put("loan_application_id", rs.getString("loan_application_id"));
                result.put("farmer_id", rs.getLong("farmer_id"));
                result.put("product_id", rs.getLong("product_id"));
                result.put("application_type", rs.getString("application_type"));
                result.put("apply_amount", rs.getBigDecimal("apply_amount"));
                result.put("purpose", rs.getString("purpose"));
                result.put("repayment_source", rs.getString("repayment_source"));
                result.put("status", rs.getString("status"));
                result.put("approved_amount", rs.getBigDecimal("approved_amount"));
                result.put("approved_by", rs.getLong("approved_by"));
                result.put("approved_at", rs.getTimestamp("approved_at"));
                result.put("product_name", rs.getString("product_name"));
                result.put("interest_rate", rs.getBigDecimal("interest_rate"));
                result.put("term_months", rs.getInt("term_months"));
                result.put("repayment_method", rs.getString("repayment_method"));
            }
            rs.close();
            stmt.close();

            // 如果是联合贷款，获取合作伙伴信息
            if ("joint".equals(result.get("application_type"))) {
                String partnerSql = "SELECT jla.*, uf.uid, u.phone " +
                        "FROM joint_loan_applications jla " +
                        "JOIN user_farmers uf ON jla.partner_farmer_id = uf.farmer_id " +
                        "JOIN users u ON uf.uid = u.uid " +
                        "WHERE jla.loan_application_id = ?";
                PreparedStatement partnerStmt = conn.prepareStatement(partnerSql);
                partnerStmt.setLong(1, (Long) result.get("id"));
                ResultSet partnerRs = partnerStmt.executeQuery();

                while (partnerRs.next()) {
                    Map<String, Object> partner = new HashMap<>();
                    partner.put("id", partnerRs.getLong("id"));
                    partner.put("partner_farmer_id", partnerRs.getLong("partner_farmer_id"));
                    partner.put("partner_share_ratio", partnerRs.getBigDecimal("partner_share_ratio"));
                    partner.put("partner_share_amount", partnerRs.getBigDecimal("partner_share_amount"));
                    partner.put("uid", partnerRs.getString("uid"));
                    partner.put("phone", partnerRs.getString("phone"));
                    partners.add(partner);
                }
                partnerRs.close();
                partnerStmt.close();
            }

            result.put("partners", partners);
        } finally {
            closeConnection();
        }

        return result;
    }

    /**
     * 检查贷款申请是否已批准
     */
    public boolean isLoanApplicationApproved(String applicationId) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "SELECT status FROM loan_applications WHERE loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, applicationId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return "approved".equals(rs.getString("status"));
            }
            return false;
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据贷款申请ID获取产品ID
     */
    public Long getProductIdByApplicationId(String applicationId) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "SELECT product_id FROM loan_applications WHERE loan_application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, applicationId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getLong("product_id");
            }
            return null;
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新信用额度记录
     */
    public void updateCreditLimit(entity.financing.CreditLimit creditLimit) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE credit_limits SET total_limit = ?, available_limit = ?, last_updated = ?, updated_at = ? "
                    +
                    "WHERE farmer_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, creditLimit.getTotalLimit());
            stmt.setBigDecimal(2, creditLimit.getAvailableLimit());
            stmt.setTimestamp(3, creditLimit.getLastUpdated());
            stmt.setTimestamp(4, creditLimit.getUpdatedAt());
            stmt.setLong(5, creditLimit.getFarmerId());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 保存新的信用额度记录
     */
    public void saveCreditLimit(entity.financing.CreditLimit creditLimit) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO credit_limits (farmer_id, total_limit, used_limit, available_limit, currency, status, last_updated, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, creditLimit.getFarmerId());
            stmt.setBigDecimal(2, creditLimit.getTotalLimit());
            stmt.setBigDecimal(3, creditLimit.getUsedLimit());
            stmt.setBigDecimal(4, creditLimit.getAvailableLimit());
            stmt.setString(5, creditLimit.getCurrency());
            stmt.setString(6, creditLimit.getStatus());
            stmt.setTimestamp(7, creditLimit.getLastUpdated());
            stmt.setTimestamp(8, creditLimit.getCreatedAt());
            stmt.setTimestamp(9, creditLimit.getUpdatedAt());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据 UID 查找农户用户信息
     */
    public Map<String, Object> getFarmerInfoByUid(String uid) throws SQLException {
        Connection conn = getConnection();
        Map<String, Object> farmerInfo = null;
        try {
            String sql = "SELECT * FROM user_farmers WHERE uid = ? AND enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                farmerInfo = new HashMap<>();
                farmerInfo.put("farmer_id", rs.getLong("farmer_id"));
                farmerInfo.put("uid", rs.getString("uid"));
                farmerInfo.put("farm_name", rs.getString("farm_name"));
                farmerInfo.put("farm_address", rs.getString("farm_address"));
                farmerInfo.put("farm_size", rs.getBigDecimal("farm_size"));
                farmerInfo.put("enable", rs.getBoolean("enable"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return farmerInfo;
    }

    /**
     * 根据产品ID获取贷款产品
     */
    public entity.financing.LoanProduct getLoanProductById(Long productId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.LoanProduct loanProduct = null;
        try {
            // 修改SQL查询，同时匹配id和product_id字段
            String sql = "SELECT * FROM loan_products WHERE (id = ? OR product_id = ?) AND status = 'active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, productId);
            stmt.setString(2, String.valueOf(productId));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loanProduct = new entity.financing.LoanProduct();
                loanProduct.setId(rs.getLong("id"));
                loanProduct.setProductId(rs.getString("product_id"));
                loanProduct.setProductCode(rs.getString("product_code"));
                loanProduct.setProductName(rs.getString("product_name"));
                loanProduct.setMinCreditLimit(rs.getBigDecimal("min_credit_limit"));
                loanProduct.setMaxAmount(rs.getBigDecimal("max_amount"));
                loanProduct.setInterestRate(rs.getBigDecimal("interest_rate"));
                loanProduct.setTermMonths(rs.getInt("term_months"));
                loanProduct.setRepaymentMethod(rs.getString("repayment_method"));
                loanProduct.setDescription(rs.getString("description"));
                loanProduct.setStatus(rs.getString("status"));
                loanProduct.setBankId(rs.getLong("bank_id"));
                loanProduct.setCreatedAt(rs.getTimestamp("created_at"));
                loanProduct.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return loanProduct;
    }

    public entity.financing.LoanProduct getLoanProductById(String productId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.LoanProduct loanProduct = null;
        try {
            // 修改SQL查询，优先匹配product_id字段
            String sql = "SELECT * FROM loan_products WHERE product_id = ? AND status = 'active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loanProduct = new entity.financing.LoanProduct();
                loanProduct.setId(rs.getLong("id"));
                loanProduct.setProductId(rs.getString("product_id"));
                loanProduct.setProductCode(rs.getString("product_code"));
                loanProduct.setProductName(rs.getString("product_name"));
                loanProduct.setMinCreditLimit(rs.getBigDecimal("min_credit_limit"));
                loanProduct.setMaxAmount(rs.getBigDecimal("max_amount"));
                loanProduct.setInterestRate(rs.getBigDecimal("interest_rate"));
                loanProduct.setTermMonths(rs.getInt("term_months"));
                loanProduct.setRepaymentMethod(rs.getString("repayment_method"));
                loanProduct.setDescription(rs.getString("description"));
                loanProduct.setStatus(rs.getString("status"));
                loanProduct.setBankId(rs.getLong("bank_id"));
                loanProduct.setCreatedAt(rs.getTimestamp("created_at"));
                loanProduct.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return loanProduct;
    }

    /**
     * 检查贷款产品名称是否已存在
     */
    public boolean isProductNameExists(String productName) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "SELECT COUNT(*) FROM loan_products WHERE product_name = ? AND status = 'active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, productName);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            stmt.close();
            return count > 0;
        } finally {
            closeConnection();
        }
    }

    public long createLoanApplication(String loanApplicationId, Long farmerId, Long productId, String applicationType,
            BigDecimal applyAmount, String purpose, String repaymentSource) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO loan_applications (loan_application_id, farmer_id, product_id, application_type, apply_amount, purpose, repayment_source, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, loanApplicationId);
            stmt.setLong(2, farmerId);
            stmt.setLong(3, productId);
            stmt.setString(4, applicationType);
            stmt.setBigDecimal(5, applyAmount);
            stmt.setString(6, purpose);
            stmt.setString(7, repaymentSource);
            stmt.setString(8, "pending");

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("创建贷款申请记录失败，没有行受到影响");
            }

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            long id = 0;
            if (generatedKeys.next()) {
                id = generatedKeys.getLong(1);
            }
            generatedKeys.close();
            stmt.close();
            return id;
        } finally {
            closeConnection();
        }
    }

    /**
     * 预扣农户信用额度
     */
    public void preDeductCreditLimit(Long farmerId, BigDecimal amount) throws SQLException {
        Connection conn = getConnection();
        try {
            // 先获取当前信用额度信息
            String selectSql = "SELECT total_limit, used_limit, available_limit FROM credit_limits WHERE farmer_id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setLong(1, farmerId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                BigDecimal totalLimit = rs.getBigDecimal("total_limit");
                BigDecimal currentUsedLimit = rs.getBigDecimal("used_limit");
                BigDecimal currentAvailableLimit = rs.getBigDecimal("available_limit");

                // 检查可用额度是否足够
                if (currentAvailableLimit.compareTo(amount) < 0) {
                    throw new SQLException("可用额度不足");
                }

                // 更新信用额度：增加已用额度，减少可用额度
                BigDecimal newUsedLimit = currentUsedLimit.add(amount);
                BigDecimal newAvailableLimit = currentAvailableLimit.subtract(amount);

                String updateSql = "UPDATE credit_limits SET used_limit = ?, available_limit = ?, last_updated = ? WHERE farmer_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setBigDecimal(1, newUsedLimit);
                updateStmt.setBigDecimal(2, newAvailableLimit);
                updateStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                updateStmt.setLong(4, farmerId);
                updateStmt.executeUpdate();
                updateStmt.close();
            } else {
                throw new SQLException("未找到农户信用额度记录");
            }
            rs.close();
            selectStmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 还原农户信用额度（贷款申请被拒绝时调用）
     */
    public void restoreCreditLimit(Long farmerId, BigDecimal amount) throws SQLException {
        Connection conn = getConnection();
        try {
            // 先获取当前信用额度信息
            String selectSql = "SELECT used_limit, available_limit FROM credit_limits WHERE farmer_id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            selectStmt.setLong(1, farmerId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                BigDecimal currentUsedLimit = rs.getBigDecimal("used_limit");
                BigDecimal currentAvailableLimit = rs.getBigDecimal("available_limit");

                // 更新信用额度：减少已用额度，增加可用额度
                BigDecimal newUsedLimit = currentUsedLimit.subtract(amount);
                BigDecimal newAvailableLimit = currentAvailableLimit.add(amount);

                String updateSql = "UPDATE credit_limits SET used_limit = ?, available_limit = ?, last_updated = ? WHERE farmer_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setBigDecimal(1, newUsedLimit);
                updateStmt.setBigDecimal(2, newAvailableLimit);
                updateStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                updateStmt.setLong(4, farmerId);
                updateStmt.executeUpdate();
                updateStmt.close();
            }
            rs.close();
            selectStmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 确认扣除农户信用额度（贷款批准时调用）
     */
    public void confirmDeductCreditLimit(Long farmerId, BigDecimal amount) throws SQLException {
        // 实际上预扣时已经处理了，这里可以留空或做其他处理
        // 或者可以添加日志记录等操作
    }

    /**
     * 根据贷款ID获取贷款信息
     */
    public entity.financing.Loan getLoanById(String loanId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.Loan loan = null;
        try {
            String sql = "SELECT * FROM loans WHERE loan_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, loanId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loan = new entity.financing.Loan();
                loan.setId(rs.getLong("id"));
                loan.setLoanId(rs.getString("loan_id"));
                loan.setFarmerId(rs.getLong("farmer_id"));
                loan.setProductId(rs.getLong("product_id"));
                loan.setLoanAmount(rs.getBigDecimal("loan_amount"));
                loan.setInterestRate(rs.getBigDecimal("interest_rate"));
                loan.setTermMonths(rs.getInt("term_months"));
                loan.setRepaymentMethod(rs.getString("repayment_method"));
                loan.setDisburseAmount(rs.getBigDecimal("disburse_amount"));
                loan.setDisburseMethod(rs.getString("disburse_method"));
                loan.setDisburseDate(rs.getTimestamp("disburse_date"));
                loan.setFirstRepaymentDate(rs.getDate("first_repayment_date"));
                loan.setLoanAccount(rs.getString("loan_account"));
                loan.setDisburseRemarks(rs.getString("disburse_remarks"));
                loan.setLoanStatus(rs.getString("loan_status"));
                loan.setApprovedBy(rs.getLong("approved_by"));
                loan.setApprovedAt(rs.getTimestamp("approved_at"));
                loan.setRejectReason(rs.getString("reject_reason"));
                loan.setClosedDate(rs.getTimestamp("closed_date"));
                loan.setTotalRepaymentAmount(rs.getBigDecimal("total_repayment_amount"));
                loan.setTotalPaidAmount(rs.getBigDecimal("total_paid_amount"));
                loan.setTotalPaidPrincipal(rs.getBigDecimal("total_paid_principal"));
                loan.setTotalPaidInterest(rs.getBigDecimal("total_paid_interest"));
                loan.setRemainingPrincipal(rs.getBigDecimal("remaining_principal"));
                loan.setCurrentPeriod(rs.getInt("current_period"));
                loan.setNextPaymentDate(rs.getDate("next_payment_date"));
                loan.setNextPaymentAmount(rs.getBigDecimal("next_payment_amount"));
                loan.setOverdueDays(rs.getInt("overdue_days"));
                loan.setOverdueAmount(rs.getBigDecimal("overdue_amount"));
                loan.setRepaymentSchedule(rs.getString("repayment_schedule"));
                loan.setPurpose(rs.getString("purpose"));
                loan.setRepaymentSource(rs.getString("repayment_source"));
                loan.setIsJointLoan(rs.getBoolean("is_joint_loan"));
                loan.setCreatedAt(rs.getTimestamp("created_at"));
                loan.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return loan;
    }



    /**
     * 更新联合贷款合作伙伴的已还款金额
     */
    public void updateJointLoanPartnerRepayment(Long loanId, Long partnerFarmerId, BigDecimal repaymentAmount) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE joint_loans SET partner_paid_amount = partner_paid_amount + ? " +
                    "WHERE loan_id = ? AND partner_farmer_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBigDecimal(1, repaymentAmount);
            stmt.setLong(2, loanId);
            stmt.setLong(3, partnerFarmerId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }




    /**
     * 根据贷款ID获取联合贷款伙伴信息
     */
    public List<Map<String, Object>> getJointLoanPartnersByLoanId(long loanId) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> partners = new ArrayList<>();
        try {
            String sql = "SELECT * FROM joint_loans WHERE loan_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, loanId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> partner = new HashMap<>();
                partner.put("id", rs.getLong("id"));
                partner.put("loan_id", rs.getLong("loan_id"));
                partner.put("partner_farmer_id", rs.getLong("partner_farmer_id"));
                partner.put("partner_share_ratio", rs.getBigDecimal("partner_share_ratio"));
                partner.put("partner_share_amount", rs.getBigDecimal("partner_share_amount"));
                partner.put("partner_principal", rs.getBigDecimal("partner_principal"));
                partner.put("partner_interest", rs.getBigDecimal("partner_interest"));
                partner.put("partner_total_repayment", rs.getBigDecimal("partner_total_repayment"));
                partner.put("partner_paid_amount", rs.getBigDecimal("partner_paid_amount"));
                partner.put("partner_remaining_principal", rs.getBigDecimal("partner_remaining_principal"));
                partners.add(partner);
            }

            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return partners;
    }

    /**
     * 保存还款记录
     */
    public void saveRepayment(entity.financing.Repayment repayment) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO repayments (repayment_id, loan_id, farmer_id, repayment_amount, " +
                    "principal_amount, interest_amount, repayment_method, payment_account, remarks, " +
                    "repayment_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, repayment.getRepaymentId());
            stmt.setLong(2, repayment.getLoanId());
            stmt.setLong(3, repayment.getFarmerId());
            stmt.setBigDecimal(4, repayment.getRepaymentAmount());
            stmt.setBigDecimal(5, repayment.getPrincipalAmount());
            stmt.setBigDecimal(6, repayment.getInterestAmount());
            stmt.setString(7, repayment.getRepaymentMethod());
            stmt.setString(8, repayment.getPaymentAccount());
            stmt.setString(9, repayment.getRemarks());
            stmt.setTimestamp(10, repayment.getRepaymentDate());
            stmt.setTimestamp(11, repayment.getCreatedAt());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 还款后更新贷款状态
     */
    public void updateLoanAfterRepayment(entity.financing.Loan loan) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE loans SET loan_status = ?, total_paid_amount = ?, " +
                    "overdue_days = ?, overdue_amount = ?, total_repayment_amount = ?, " +
                    "next_payment_date = ?, current_period = ?, closed_date = ?, updated_at = ? " +
                    "WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, loan.getLoanStatus());
            stmt.setBigDecimal(2, loan.getTotalPaidAmount() != null ? loan.getTotalPaidAmount() : BigDecimal.ZERO);
            // 修复：正确处理 Integer 对象类型的 null 值检查
            Integer currentOverdueDaysObj = loan.getOverdueDays();
            stmt.setInt(3, currentOverdueDaysObj != null ? loan.getOverdueDays() : 0);
            stmt.setBigDecimal(4, loan.getOverdueAmount() != null ? loan.getOverdueAmount() : BigDecimal.ZERO);
            stmt.setBigDecimal(5, loan.getTotalRepaymentAmount() != null ? loan.getTotalRepaymentAmount() : BigDecimal.ZERO);

            if (loan.getNextPaymentDate() != null) {
                stmt.setDate(6, loan.getNextPaymentDate());
            } else {
                stmt.setNull(6, java.sql.Types.DATE);
            }
            Integer currentOverdueDaysObj1 = loan.getOverdueDays();
            // 修复：正确处理 Integer 对象类型的 null 值检查
            stmt.setInt(7, currentOverdueDaysObj1 != null ? loan.getCurrentPeriod() : 0);

            if (loan.getClosedDate() != null) {
                stmt.setTimestamp(8, loan.getClosedDate());
            } else {
                stmt.setNull(8, java.sql.Types.TIMESTAMP);
            }

            stmt.setTimestamp(9, loan.getUpdatedAt());
            stmt.setLong(10, loan.getId());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }





    /**
     * 根据产品名称获取贷款产品
     */
    public entity.financing.LoanProduct getLoanProductByName(String productName) throws SQLException {
        Connection conn = getConnection();
        entity.financing.LoanProduct loanProduct = null;
        try {
            String sql = "SELECT * FROM loan_products WHERE product_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, productName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loanProduct = new entity.financing.LoanProduct();
                loanProduct.setId(rs.getLong("id"));
                loanProduct.setProductId(rs.getString("product_id"));
                loanProduct.setProductCode(rs.getString("product_code"));
                loanProduct.setProductName(rs.getString("product_name"));
                loanProduct.setMinCreditLimit(rs.getBigDecimal("min_credit_limit"));
                loanProduct.setMaxAmount(rs.getBigDecimal("max_amount"));
                loanProduct.setInterestRate(rs.getBigDecimal("interest_rate"));
                loanProduct.setTermMonths(rs.getInt("term_months"));
                loanProduct.setRepaymentMethod(rs.getString("repayment_method"));
                loanProduct.setDescription(rs.getString("description"));
                loanProduct.setStatus(rs.getString("status"));
                loanProduct.setBankId(rs.getLong("bank_id"));
                loanProduct.setCreatedAt(rs.getTimestamp("created_at"));
                loanProduct.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return loanProduct;
    }

    /**
     * 保存贷款产品到数据库
     */
    public void saveLoanProduct(entity.financing.LoanProduct loanProduct) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO loan_products (product_id, product_code, product_name, min_credit_limit, " +
                    "max_amount, interest_rate, term_months, repayment_method, description, status, bank_id, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, loanProduct.getProductId());
            stmt.setString(2, loanProduct.getProductCode());
            stmt.setString(3, loanProduct.getProductName());
            stmt.setBigDecimal(4, loanProduct.getMinCreditLimit());
            stmt.setBigDecimal(5, loanProduct.getMaxAmount());
            stmt.setBigDecimal(6, loanProduct.getInterestRate());
            stmt.setInt(7, loanProduct.getTermMonths());
            stmt.setString(8, loanProduct.getRepaymentMethod());
            stmt.setString(9, loanProduct.getDescription());
            stmt.setString(10, loanProduct.getStatus());
            stmt.setLong(11, loanProduct.getBankId());
            stmt.setTimestamp(12, loanProduct.getCreatedAt());
            stmt.setTimestamp(13, loanProduct.getUpdatedAt());
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 初始化测试贷款数据
     */
    public void initializeTestLoanData() throws SQLException {
        Connection conn = getConnection();
        try {
            // 检查是否已经有贷款数据
            String checkSql = "SELECT COUNT(*) FROM loans WHERE loan_status IN ('active', 'overdue')";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            checkStmt.close();
            
            // 如果已经有活跃贷款，则不重复创建
            if (count > 0) {
                return;
            }
            
            // 获取第一个农户ID（假设农户ID为1）
            String farmerSql = "SELECT farmer_id FROM farmers LIMIT 1";
            PreparedStatement farmerStmt = conn.prepareStatement(farmerSql);
            ResultSet farmerRs = farmerStmt.executeQuery();
            if (!farmerRs.next()) {
                // 没有农户，先返回
                farmerRs.close();
                farmerStmt.close();
                return;
            }
            Long farmerId = farmerRs.getLong("farmer_id");
            farmerRs.close();
            farmerStmt.close();
            
            // 获取第一个贷款产品ID
            String productSql = "SELECT id FROM loan_products LIMIT 1";
            PreparedStatement productStmt = conn.prepareStatement(productSql);
            ResultSet productRs = productStmt.executeQuery();
            if (!productRs.next()) {
                // 没有贷款产品，先返回
                productRs.close();
                productStmt.close();
                return;
            }
            Long productId = productRs.getLong("id");
            productRs.close();
            productStmt.close();
            
            // 创建测试贷款数据
            String loanId = "LOAN" + System.currentTimeMillis();
            BigDecimal loanAmount = new BigDecimal("50000.00");
            BigDecimal interestRate = new BigDecimal("12.00"); // 12%年利率
            int termMonths = 12;
            BigDecimal totalRepaymentAmount = new BigDecimal("56000.00");
            BigDecimal monthlyPayment = totalRepaymentAmount.divide(
                new BigDecimal(termMonths), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal totalPaidAmount = new BigDecimal("10000.00"); // 已还1万元
            BigDecimal remainingAmount = totalRepaymentAmount.subtract(totalPaidAmount); // 剩余4.6万元
            
            // 当前日期
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Date disburseDate = new Date(now.getTime());
            
            // 首次还款日期（放款后一个月）
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(disburseDate);
            calendar.add(Calendar.MONTH, 1);
            Date firstRepaymentDate = new Date(calendar.getTimeInMillis());
            
            // 下次还款日期（当前期数的还款日）
            calendar.setTime(firstRepaymentDate);
            calendar.add(Calendar.MONTH, 2); // 假设已还款2期，现在是第3期
            Date nextPaymentDate = new Date(calendar.getTimeInMillis());
            
            String insertSql = "INSERT INTO loans (loan_id, farmer_id, product_id, loan_amount, interest_rate, term_months, "
                    + "repayment_method, disburse_amount, disburse_method, disburse_date, first_repayment_date, "
                    + "loan_account, disburse_remarks, loan_status, total_repayment_amount, total_paid_amount, "
                    + "total_paid_principal, total_paid_interest, remaining_principal, current_period, "
                    + "next_payment_date, next_payment_amount, overdue_days, overdue_amount, purpose, "
                    + "repayment_source, is_joint_loan, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
            PreparedStatement stmt = conn.prepareStatement(insertSql);
            stmt.setString(1, loanId);
            stmt.setLong(2, farmerId);
            stmt.setLong(3, productId);
            stmt.setBigDecimal(4, loanAmount);
            stmt.setBigDecimal(5, interestRate);
            stmt.setInt(6, termMonths);
            stmt.setString(7, "equal_installment");
            stmt.setBigDecimal(8, loanAmount);
            stmt.setString(9, "bank_transfer");
            stmt.setDate(10, disburseDate);
            stmt.setDate(11, firstRepaymentDate);
            stmt.setString(12, "6222021234567890123");
            stmt.setString(13, "测试贷款数据");
            stmt.setString(14, "active");
            stmt.setBigDecimal(15, totalRepaymentAmount);
            stmt.setBigDecimal(16, totalPaidAmount); // 已还1万元
            stmt.setBigDecimal(17, new BigDecimal("8500.00")); // 已还本金
            stmt.setBigDecimal(18, new BigDecimal("1500.00")); // 已还利息
            stmt.setBigDecimal(19, loanAmount.subtract(new BigDecimal("8500.00"))); // 剩余本金
            stmt.setInt(20, 3); // 当前期数
            stmt.setDate(21, nextPaymentDate);
            stmt.setBigDecimal(22, monthlyPayment);
            stmt.setInt(23, 0);
            stmt.setBigDecimal(24, new BigDecimal("0.00"));
            stmt.setString(25, "农业生产资金需求");
            stmt.setString(26, "农产品销售收入");
            stmt.setBoolean(27, false);
            stmt.setTimestamp(28, now);
            stmt.setTimestamp(29, now);
            
            stmt.executeUpdate();
            stmt.close();
            
            System.out.println("测试贷款数据初始化完成：" + loanId);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("初始化测试贷款数据失败：" + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * 获取所有激活的贷款产品
     */
    public List<entity.financing.LoanProduct> getAllActiveLoanProducts() throws SQLException {
        Connection conn = getConnection();
        List<entity.financing.LoanProduct> products = new ArrayList<>();
        try {
            String sql = "SELECT * FROM loan_products WHERE status = 'active' ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                entity.financing.LoanProduct loanProduct = new entity.financing.LoanProduct();
                loanProduct.setId(rs.getLong("id"));
                loanProduct.setProductId(rs.getString("product_id"));
                loanProduct.setProductCode(rs.getString("product_code"));
                loanProduct.setProductName(rs.getString("product_name"));
                loanProduct.setMinCreditLimit(rs.getBigDecimal("min_credit_limit"));
                loanProduct.setMaxAmount(rs.getBigDecimal("max_amount"));
                loanProduct.setInterestRate(rs.getBigDecimal("interest_rate"));
                loanProduct.setTermMonths(rs.getInt("term_months"));
                loanProduct.setRepaymentMethod(rs.getString("repayment_method"));
                loanProduct.setDescription(rs.getString("description"));
                loanProduct.setStatus(rs.getString("status"));
                loanProduct.setBankId(rs.getLong("bank_id"));
                loanProduct.setCreatedAt(rs.getTimestamp("created_at"));
                loanProduct.setUpdatedAt(rs.getTimestamp("updated_at"));
                products.add(loanProduct);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return products;
    }

    /**
     * 根据农户ID获取信用额度
     */
    public entity.financing.CreditLimit getCreditLimitByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.CreditLimit creditLimit = null;
        try {
            String sql = "SELECT * FROM credit_limits WHERE farmer_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                creditLimit = new entity.financing.CreditLimit();
                creditLimit.setId(rs.getLong("id"));
                creditLimit.setFarmerId(rs.getLong("farmer_id"));
                creditLimit.setTotalLimit(rs.getBigDecimal("total_limit"));
                creditLimit.setUsedLimit(rs.getBigDecimal("used_limit"));
                creditLimit.setAvailableLimit(rs.getBigDecimal("available_limit"));
                creditLimit.setCurrency(rs.getString("currency"));
                creditLimit.setStatus(rs.getString("status"));
                creditLimit.setLastUpdated(rs.getTimestamp("last_updated"));
                creditLimit.setCreatedAt(rs.getTimestamp("created_at"));
                creditLimit.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return creditLimit;
    }

    /**
     * 根据农户ID获取待审批的额度申请
     */
    public entity.financing.CreditApplication getPendingApplicationByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.CreditApplication application = null;
        try {
            String sql = "SELECT * FROM credit_applications WHERE farmer_id = ? AND status = 'pending' ORDER BY created_at DESC LIMIT 1";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                application = new entity.financing.CreditApplication();
                application.setId(rs.getLong("id"));
                application.setApplicationId(rs.getString("application_id"));
                application.setFarmerId(rs.getLong("farmer_id"));
                application.setProofType(rs.getString("proof_type"));
                application.setProofImages(rs.getString("proof_images"));
                application.setApplyAmount(rs.getBigDecimal("apply_amount"));
                application.setDescription(rs.getString("description"));
                application.setStatus(rs.getString("status"));
                application.setApprovedAmount(rs.getBigDecimal("approved_amount"));
                application.setRejectReason(rs.getString("reject_reason"));
                application.setApprovedBy(rs.getLong("approved_by"));
                application.setApprovedAt(rs.getTimestamp("approved_at"));
                application.setCreatedAt(rs.getTimestamp("created_at"));
                application.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return application;
    }

    /**
     * 保存额度申请到数据库
     */
    public void saveCreditApplication(entity.financing.CreditApplication application) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "INSERT INTO credit_applications (application_id, farmer_id, proof_type, proof_images, " +
                    "apply_amount, description, status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            System.out.println("=== DEBUG: 保存信贷申请到数据库 ===");
            System.out.println("申请ID: " + application.getApplicationId());
            System.out.println("农户ID: " + application.getFarmerId());
            System.out.println("申请金额: " + application.getApplyAmount());
            System.out.println("状态: " + application.getStatus());
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, application.getApplicationId());
            stmt.setLong(2, application.getFarmerId());
            stmt.setString(3, application.getProofType());
            stmt.setString(4, application.getProofImages());
            stmt.setBigDecimal(5, application.getApplyAmount());
            stmt.setString(6, application.getDescription());
            stmt.setString(7, application.getStatus());
            stmt.setTimestamp(8, application.getCreatedAt());
            stmt.setTimestamp(9, application.getUpdatedAt());
            
            int affectedRows = stmt.executeUpdate();
            System.out.println("插入成功，影响行数: " + affectedRows);
            System.out.println("=== DEBUG END ===");
            
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据申请ID获取信贷额度申请
     */
    public entity.financing.CreditApplication getCreditApplicationById(String applicationId) throws SQLException {
        Connection conn = getConnection();
        entity.financing.CreditApplication application = null;
        try {
            String sql = "SELECT * FROM credit_applications WHERE application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, applicationId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                application = new entity.financing.CreditApplication();
                application.setId(rs.getLong("id"));
                application.setApplicationId(rs.getString("application_id"));
                application.setFarmerId(rs.getLong("farmer_id"));
                application.setProofType(rs.getString("proof_type"));
                application.setProofImages(rs.getString("proof_images"));
                application.setApplyAmount(rs.getBigDecimal("apply_amount"));
                application.setDescription(rs.getString("description"));
                application.setStatus(rs.getString("status"));
                application.setApprovedAmount(rs.getBigDecimal("approved_amount"));
                application.setRejectReason(rs.getString("reject_reason"));
                application.setApprovedBy(rs.getLong("approved_by"));
                application.setApprovedAt(rs.getTimestamp("approved_at"));
                application.setCreatedAt(rs.getTimestamp("created_at"));
                application.setUpdatedAt(rs.getTimestamp("updated_at"));
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return application;
    }

    /**
     * 更新信贷额度申请状态（批准）
     */
    public void updateCreditApplicationStatus(String applicationId, String status, Long approvedBy, 
                                            Timestamp approvedAt, BigDecimal approvedAmount) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE credit_applications SET status = ?, approved_by = ?, approved_at = ?, " +
                        "approved_amount = ?, updated_at = CURRENT_TIMESTAMP WHERE application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setLong(2, approvedBy);
            stmt.setTimestamp(3, approvedAt);
            stmt.setBigDecimal(4, approvedAmount);
            stmt.setString(5, applicationId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 更新信贷额度申请状态（拒绝）
     */
    public void updateCreditApplicationRejection(String applicationId, String status, Long approvedBy,
                                               Timestamp approvedAt, String rejectReason) throws SQLException {
        Connection conn = getConnection();
        try {
            String sql = "UPDATE credit_applications SET status = ?, approved_by = ?, approved_at = ?, " +
                        "reject_reason = ?, updated_at = CURRENT_TIMESTAMP WHERE application_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setLong(2, approvedBy);
            stmt.setTimestamp(3, approvedAt);
            stmt.setString(4, rejectReason);
            stmt.setString(5, applicationId);
            stmt.executeUpdate();
            stmt.close();
        } finally {
            closeConnection();
        }
    }

    /**
     * 根据农户ID获取申请记录列表
     */
    public List<Map<String, Object>> getCreditApplicationsByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> applications = new ArrayList<>();
        try {
            String sql = "SELECT ca.*, u.nickname as farmer_name, u.phone as farmer_phone, " +
                        "bu.bank_name as approver_name " +
                        "FROM credit_applications ca " +
                        "JOIN user_farmers uf ON ca.farmer_id = uf.farmer_id " +
                        "JOIN users u ON uf.uid = u.uid " +
                        "LEFT JOIN user_banks bu ON ca.approved_by = bu.bank_id " +
                        "WHERE ca.farmer_id = ? " +
                        "ORDER BY ca.created_at DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> application = new HashMap<>();
                application.put("application_id", rs.getString("application_id"));
                application.put("apply_amount", rs.getBigDecimal("apply_amount"));
                application.put("proof_type", rs.getString("proof_type"));
                application.put("proof_images", rs.getString("proof_images"));
                application.put("description", rs.getString("description"));
                application.put("status", rs.getString("status"));
                application.put("approved_amount", rs.getBigDecimal("approved_amount"));
                application.put("reject_reason", rs.getString("reject_reason"));
                application.put("created_at", rs.getTimestamp("created_at"));
                application.put("updated_at", rs.getTimestamp("updated_at"));
                application.put("approved_at", rs.getTimestamp("approved_at"));
                application.put("farmer_name", rs.getString("farmer_name"));
                application.put("farmer_phone", rs.getString("farmer_phone"));
                application.put("approver_name", rs.getString("approver_name"));
                applications.add(application);
            }
            
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return applications;
    }

    /**
     * 获取待审批的信贷额度申请列表
     */
    public List<Map<String, Object>> getPendingCreditApplicationsList() throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> applications = new ArrayList<>();
        try {
            String sql = "SELECT ca.application_id, ca.apply_amount, ca.proof_type, ca.proof_images, ca.description, " +
                        "ca.created_at, u.nickname as farmer_name, u.phone as farmer_phone " +
                        "FROM credit_applications ca " +
                        "JOIN user_farmers uf ON ca.farmer_id = uf.farmer_id " +
                        "JOIN users u ON uf.uid = u.uid " +
                        "WHERE ca.status = 'pending' " +
                        "ORDER BY ca.created_at DESC";
            
            System.out.println("=== DEBUG: 执行查询待审批申请SQL ===");
            System.out.println("SQL: " + sql);
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            int count = 0;

            while (rs.next()) {
                count++;
                Map<String, Object> application = new HashMap<>();
                application.put("application_id", rs.getString("application_id"));
                application.put("apply_amount", rs.getBigDecimal("apply_amount"));
                application.put("proof_type", rs.getString("proof_type"));
                application.put("proof_images", rs.getString("proof_images"));
                application.put("description", rs.getString("description"));
                application.put("created_at", rs.getTimestamp("created_at"));
                application.put("farmer_name", rs.getString("farmer_name"));
                application.put("farmer_phone", rs.getString("farmer_phone"));
                applications.add(application);
                
                System.out.println("查询到申请 #" + count + ": " + rs.getString("application_id") + 
                                 " - " + rs.getString("farmer_name"));
                System.out.println("  证明图片: " + rs.getString("proof_images"));
            }
            
            System.out.println("总共查询到 " + count + " 条待审批申请");
            System.out.println("=== DEBUG END ===");
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return applications;
    }

    /**
     * 获取待审批的贷款申请列表
     */
    public List<Map<String, Object>> getPendingLoanApplicationsList() throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> applications = new ArrayList<>();
        try {
            String sql = "SELECT la.loan_application_id, la.apply_amount, la.purpose, la.application_type, " +
                        "la.created_at, lp.product_name, u.nickname as farmer_name, u.phone as farmer_phone " +
                        "FROM loan_applications la " +
                        "JOIN user_farmers uf ON la.farmer_id = uf.farmer_id " +
                        "JOIN users u ON uf.uid = u.uid " +
                        "JOIN loan_products lp ON la.product_id = lp.id " +
                        "WHERE la.status IN ('pending', 'pending_partners') " +
                        "ORDER BY la.created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> application = new HashMap<>();
                application.put("loan_application_id", rs.getString("loan_application_id"));
                application.put("apply_amount", rs.getBigDecimal("apply_amount"));
                application.put("purpose", rs.getString("purpose"));
                application.put("application_type", rs.getString("application_type"));
                application.put("created_at", rs.getTimestamp("created_at"));
                application.put("product_name", rs.getString("product_name"));
                application.put("farmer_name", rs.getString("farmer_name"));
                application.put("farmer_phone", rs.getString("farmer_phone"));
                applications.add(application);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return applications;
    }

    /**
     * 获取已审批待放款的贷款申请列表
     */
    public List<Map<String, Object>> getApprovedLoanApplicationsList() throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> applications = new ArrayList<>();
        try {
            String sql = "SELECT la.loan_application_id, la.apply_amount, la.approved_amount, la.purpose, " +
                        "la.application_type, la.created_at, la.approved_at, lp.product_name, lp.interest_rate, " +
                        "lp.term_months, lp.repayment_method, u.nickname as farmer_name, u.phone as farmer_phone, " +
                        "approver.nickname as approver_name " +
                        "FROM loan_applications la " +
                        "JOIN user_farmers uf ON la.farmer_id = uf.farmer_id " +
                        "JOIN users u ON uf.uid = u.uid " +
                        "JOIN loan_products lp ON la.product_id = lp.id " +
                        "LEFT JOIN user_banks ub ON la.approved_by = ub.bank_id " +
                        "LEFT JOIN users approver ON ub.uid = approver.uid " +
                        "WHERE la.status = 'approved' " +
                        "ORDER BY la.approved_at DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> application = new HashMap<>();
                application.put("loan_application_id", rs.getString("loan_application_id"));
                application.put("apply_amount", rs.getBigDecimal("apply_amount"));
                application.put("approved_amount", rs.getBigDecimal("approved_amount"));
                application.put("purpose", rs.getString("purpose"));
                application.put("application_type", rs.getString("application_type"));
                application.put("created_at", rs.getTimestamp("created_at"));
                application.put("approved_at", rs.getTimestamp("approved_at"));
                application.put("product_name", rs.getString("product_name"));
                application.put("interest_rate", rs.getBigDecimal("interest_rate"));
                application.put("term_months", rs.getInt("term_months"));
                application.put("repayment_method", rs.getString("repayment_method"));
                application.put("farmer_name", rs.getString("farmer_name"));
                application.put("farmer_phone", rs.getString("farmer_phone"));
                application.put("approver_name", rs.getString("approver_name"));
                applications.add(application);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return applications;
    }

    /**
     * 根据农户ID获取已放款的贷款记录列表
     */
    public List<Map<String, Object>> getLoansByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> loans = new ArrayList<>();
        try {
            String sql = "SELECT l.id, l.loan_id, l.farmer_id, l.loan_amount, l.interest_rate, " +
                        "l.term_months, l.repayment_method, l.disburse_amount, l.disburse_method, " +
                        "l.disburse_date, l.first_repayment_date, l.loan_account, l.disburse_remarks, " +
                        "l.loan_status, l.total_repayment_amount, l.total_paid_amount, l.remaining_principal, " +
                        "l.current_period, l.next_payment_date, l.next_payment_amount, l.purpose, " +
                        "l.repayment_source, l.is_joint_loan, l.created_at, l.updated_at, " +
                        "lp.product_name, lp.max_amount as product_max_amount " +
                        "FROM loans l " +
                        "JOIN loan_products lp ON l.product_id = lp.id " +
                        "WHERE l.farmer_id = ? AND l.loan_status IN ('active', 'overdue', 'closed') " +
                        "ORDER BY l.disburse_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> loan = new HashMap<>();
                loan.put("id", rs.getLong("id"));
                loan.put("loan_id", rs.getString("loan_id"));
                loan.put("farmer_id", rs.getLong("farmer_id"));
                loan.put("loan_amount", rs.getBigDecimal("loan_amount"));
                loan.put("interest_rate", rs.getBigDecimal("interest_rate"));
                loan.put("term_months", rs.getInt("term_months"));
                loan.put("repayment_method", rs.getString("repayment_method"));
                loan.put("disburse_amount", rs.getBigDecimal("disburse_amount"));
                loan.put("disburse_method", rs.getString("disburse_method"));
                loan.put("disburse_date", rs.getTimestamp("disburse_date"));
                loan.put("first_repayment_date", rs.getDate("first_repayment_date"));
                loan.put("loan_account", rs.getString("loan_account"));
                loan.put("disburse_remarks", rs.getString("disburse_remarks"));
                loan.put("loan_status", rs.getString("loan_status"));
                loan.put("total_repayment_amount", rs.getBigDecimal("total_repayment_amount"));
                loan.put("total_paid_amount", rs.getBigDecimal("total_paid_amount"));
                loan.put("remaining_principal", rs.getBigDecimal("remaining_principal"));
                loan.put("current_period", rs.getInt("current_period"));
                loan.put("next_payment_date", rs.getDate("next_payment_date"));
                loan.put("next_payment_amount", rs.getBigDecimal("next_payment_amount"));
                loan.put("purpose", rs.getString("purpose"));
                loan.put("repayment_source", rs.getString("repayment_source"));
                loan.put("is_joint_loan", rs.getBoolean("is_joint_loan"));
                loan.put("created_at", rs.getTimestamp("created_at"));
                loan.put("updated_at", rs.getTimestamp("updated_at"));
                loan.put("product_name", rs.getString("product_name"));
                loan.put("product_max_amount", rs.getBigDecimal("product_max_amount"));
                loans.add(loan);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw e;
        } finally {
            conn.close();
        }

        return loans;
    }

    /**
     * 根据农户ID获取贷款申请记录列表
     */
    public List<Map<String, Object>> getLoanApplicationsByFarmerId(Long farmerId) throws SQLException {
        Connection conn = getConnection();
        List<Map<String, Object>> applications = new ArrayList<>();
        try {
            String sql = "SELECT la.loan_application_id, la.apply_amount, la.approved_amount, la.purpose, " +
                        "la.repayment_source, la.application_type, la.status, la.reject_reason, " +
                        "la.created_at, la.approved_at, la.updated_at, lp.product_name, lp.interest_rate, " +
                        "lp.term_months, lp.repayment_method, approver.nickname as approver_name, " +
                        "ub.bank_name as approver_bank " +
                        "FROM loan_applications la " +
                        "JOIN loan_products lp ON la.product_id = lp.id " +
                        "LEFT JOIN user_banks ub ON la.approved_by = ub.bank_id " +
                        "LEFT JOIN users approver ON ub.uid = approver.uid " +
                        "WHERE la.farmer_id = ? " +
                        "ORDER BY la.created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, farmerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> application = new HashMap<>();
                application.put("loan_application_id", rs.getString("loan_application_id"));
                application.put("apply_amount", rs.getBigDecimal("apply_amount"));
                application.put("approved_amount", rs.getBigDecimal("approved_amount"));
                application.put("purpose", rs.getString("purpose"));
                application.put("repayment_source", rs.getString("repayment_source"));
                application.put("application_type", rs.getString("application_type"));
                application.put("status", rs.getString("status"));
                application.put("reject_reason", rs.getString("reject_reason"));
                application.put("created_at", rs.getTimestamp("created_at"));
                application.put("approved_at", rs.getTimestamp("approved_at"));
                application.put("updated_at", rs.getTimestamp("updated_at"));
                application.put("product_name", rs.getString("product_name"));
                application.put("interest_rate", rs.getBigDecimal("interest_rate"));
                application.put("term_months", rs.getInt("term_months"));
                application.put("repayment_method", rs.getString("repayment_method"));
                application.put("approver_name", rs.getString("approver_name"));
                application.put("approver_bank", rs.getString("approver_bank"));
                applications.add(application);
            }
            rs.close();
            stmt.close();
        } finally {
            closeConnection();
        }
        return applications;
    }

}
