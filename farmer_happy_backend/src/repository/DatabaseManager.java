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
                            "  UID      CHAR(36) PRIMARY KEY DEFAULT (UUID())," +
                            "  password VARCHAR(32)  NOT NULL COMMENT '8-32位，含大小写字母+数字'," +
                            "  nickname VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '1-30字符'," +
                            "  phone    CHAR(11)     NOT NULL COMMENT '11位中国手机号'," +
                            "  user_type ENUM('farmer','buyer','expert','bank','admin') NOT NULL," +
                            "  CONSTRAINT chk_pwd CHECK (CHAR_LENGTH(password) BETWEEN 8 AND 32)," +
                            "  CONSTRAINT chk_phone CHECK (phone REGEXP '^1[3-9]\\\\d{9}$')," +
                            "  UNIQUE KEY uk_phone (phone)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';";
            dbStatement.executeUpdate(createUserTable);

            // 可以在这里添加更多表的创建语句

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
