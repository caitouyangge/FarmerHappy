package repository;

import entity.Content;
import entity.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "farmer_happy";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static DatabaseManager instance;
    private Connection connection;

    // 私有构造函数（单例模式）
    private DatabaseManager() {
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
                            "    money DECIMAL(10,2) DEFAULT 0 COMMENT '账户余额（元）'," +
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
                            "    money DECIMAL(10,2) DEFAULT 0 COMMENT '账户余额（元）'," +
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
                            "    money DECIMAL(10,2) DEFAULT 0 COMMENT '账户余额（元）'," +
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

            // 创建社区内容表
            String createContentsTable =
                    "CREATE TABLE IF NOT EXISTS contents (" +
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
            String createContentImagesTable =
                    "CREATE TABLE IF NOT EXISTS content_images (" +
                            "    image_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "    content_id VARCHAR(50) NOT NULL COMMENT '内容ID'," +
                            "    image_url VARCHAR(500) NOT NULL COMMENT '图片URL'," +
                            "    sort_order INT DEFAULT 0 COMMENT '排序序号'," +
                            "    INDEX idx_content_id (content_id)," +
                            "    FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区内容图片表';";
            dbStatement.executeUpdate(createContentImagesTable);

            // 创建社区评论表
            String createCommentsTable =
                    "CREATE TABLE IF NOT EXISTS comments (" +
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
     * 获取用户角色
     */
    public String getUserRole(String uid) throws SQLException {
        Connection conn = getConnection();
        String role = null;
        try {
            // 检查是否是农户
            String farmerSql = "SELECT 1 FROM user_farmers WHERE uid = ? AND enable = TRUE";
            PreparedStatement farmerStmt = conn.prepareStatement(farmerSql);
            farmerStmt.setString(1, uid);
            ResultSet farmerRs = farmerStmt.executeQuery();
            if (farmerRs.next()) {
                role = "farmer";
            }
            farmerRs.close();
            farmerStmt.close();

            if (role == null) {
                // 检查是否是专家
                String expertSql = "SELECT 1 FROM user_experts WHERE uid = ? AND enable = TRUE";
                PreparedStatement expertStmt = conn.prepareStatement(expertSql);
                expertStmt.setString(1, uid);
                ResultSet expertRs = expertStmt.executeQuery();
                if (expertRs.next()) {
                    role = "expert";
                }
                expertRs.close();
                expertStmt.close();
            }

            if (role == null) {
                // 检查是否是买家
                String buyerSql = "SELECT 1 FROM user_buyers WHERE uid = ? AND enable = TRUE";
                PreparedStatement buyerStmt = conn.prepareStatement(buyerSql);
                buyerStmt.setString(1, uid);
                ResultSet buyerRs = buyerStmt.executeQuery();
                if (buyerRs.next()) {
                    role = "buyer";
                }
                buyerRs.close();
                buyerStmt.close();
            }
        } finally {
            closeConnection();
        }
        return role;
    }
}
