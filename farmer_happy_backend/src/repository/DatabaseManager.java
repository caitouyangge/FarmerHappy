package repository;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "farmer_happy";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private Connection connection;

    // 获取数据库连接
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(URL + DB_NAME, USERNAME, PASSWORD);
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }

    // 初始化数据库和表
    public void initializeDatabase() {
        try {
            // 首先连接到 MySQL 服务器（不指定数据库）
            Connection serverConnection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            Statement serverStatement = serverConnection.createStatement();

            // 创建数据库（如果不存在）
            serverStatement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            serverStatement.close();
            serverConnection.close();

            // 连接到指定数据库并创建表
            Connection dbConnection = getConnection();
            Statement dbStatement = dbConnection.createStatement();

            // 示例：创建用户表
            String createUserTable =
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "    uid VARCHAR(36) PRIMARY KEY DEFAULT (UUID())," +
                            "    phone VARCHAR(11) UNIQUE NOT NULL COMMENT '手机号，11位数字'," +
                            "    password VARCHAR(255) NOT NULL COMMENT '密码（存储基本字符串）'," +
                            "    nickname VARCHAR(30) DEFAULT '' COMMENT '用户昵称，1-30个字符'," +
                            "    login_attempts INT DEFAULT 0 COMMENT '连续登录失败次数'," +
                            "    locked_until TIMESTAMP NULL COMMENT '账号锁定截止时间'," +
                            "    is_active BOOLEAN DEFAULT TRUE COMMENT '账号是否激活'," +
                            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                            "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                            "    INDEX idx_phone (phone)," +
                            "    INDEX idx_created_at (created_at)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息表';";
            dbStatement.executeUpdate(createUserTable);

// 创建买家扩展表
            String createUserBuyersTable =
                    "CREATE TABLE IF NOT EXISTS user_buyers (" +
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
            String createUserFarmersTable =
                    "CREATE TABLE IF NOT EXISTS user_farmers (" +
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
            String createUserExpertsTable =
                    "CREATE TABLE IF NOT EXISTS user_experts (" +
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
            String createUserBanksTable =
                    "CREATE TABLE IF NOT EXISTS user_banks (" +
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
            String createUserAdminsTable =
                    "CREATE TABLE IF NOT EXISTS user_admins (" +
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

            // 创建运费模板表
            String createShippingTemplatesTable =
                    "CREATE TABLE IF NOT EXISTS shipping_templates (" +
                            "    template_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "    farmer_id BIGINT NOT NULL COMMENT '农户ID'," +
                            "    template_name VARCHAR(100) NOT NULL COMMENT '模板名称'," +
                            "    shipping_type ENUM('free', 'fixed', 'calculated') NOT NULL DEFAULT 'free' COMMENT '运费类型: free-包邮, fixed-固定运费, calculated-计算运费'," +
                            "    fixed_amount DECIMAL(10,2) DEFAULT 0 COMMENT '固定运费金额'," +
                            "    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认模板'," +
                            "    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用'," +
                            "    INDEX idx_farmer_id (farmer_id)," +
                            "    INDEX idx_is_default (is_default)," +
                            "    INDEX idx_enable (enable)," +
                            "    FOREIGN KEY (farmer_id) REFERENCES user_farmers(farmer_id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费模板表';";
            dbStatement.executeUpdate(createShippingTemplatesTable);

            // 创建商品表
            String createProductsTable =
                    "CREATE TABLE IF NOT EXISTS products (" +
                            "    product_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "    farmer_id BIGINT NOT NULL COMMENT '农户ID'," +
                            "    category ENUM('vegetables', 'fruits', 'grains', 'livestock', 'aquatic') " +
                            "        NOT NULL COMMENT '商品分类'," +
                            "    title VARCHAR(100) NOT NULL COMMENT '商品标题'," +
                            "    specification VARCHAR(200) NOT NULL COMMENT '商品规格描述'," +
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
            String createProductImagesTable =
                    "CREATE TABLE IF NOT EXISTS product_images (" +
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
            String createProductReviewsTable =
                    "CREATE TABLE IF NOT EXISTS product_reviews (" +
                            "    review_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "    product_id BIGINT NOT NULL COMMENT '商品ID'," +
                            "    reviewer_id BIGINT COMMENT '审核员ID(关联users表)'," +
                            "    old_status ENUM('pending_review', 'on_shelf', 'off_shelf', 'review_rejected') NOT NULL COMMENT '原状态'," +
                            "    new_status ENUM('pending_review', 'on_shelf', 'off_shelf', 'review_rejected') NOT NULL COMMENT '新状态'," +
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

            dbStatement.close();
            dbConnection.close();

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
}
