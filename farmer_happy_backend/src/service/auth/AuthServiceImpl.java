package service.auth;

import dto.AuthResponseDTO;
import dto.LoginRequestDTO;
import dto.RegisterRequestDTO;
import entity.User;
import repository.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class AuthServiceImpl implements AuthService {
    private DatabaseManager databaseManager;

    public AuthServiceImpl() {
        this.databaseManager = new DatabaseManager();
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO registerRequest) throws SQLException, IllegalArgumentException {
        System.out.println("进入注册服务，请求参数: " + registerRequest.getPhone() + ", " + registerRequest.getPassword());
        List<String> errors = new ArrayList<>();
        if (!validateRegisterRequest(registerRequest, errors)) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        // 检查手机号是否已存在
        if (findUserByPhone(registerRequest.getPhone()) != null) {
            throw new SQLException("该手机号已被注册");
        }

        // 创建用户
        User user = new User(
                registerRequest.getPassword(),
                registerRequest.getNickname() != null ? registerRequest.getNickname() : "",
                registerRequest.getPhone(),
                registerRequest.getUserType()
        );

        // 保存用户到数据库
        saveUser(user);

        // 生成认证响应
        AuthResponseDTO response = new AuthResponseDTO();
        response.setUid(user.getUid());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setUserType(user.getUserType());
        response.setToken(UUID.randomUUID().toString()); // 简化处理，实际应使用JWT
        response.setExpiresAt(LocalDateTime.now().plusHours(1));

        return response;
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO loginRequest) throws SQLException, SecurityException {
        // 参数验证
        if (loginRequest.getPhone() == null || loginRequest.getPhone().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 查找用户
        User user = findUserByPhone(loginRequest.getPhone());
        if (user == null) {
            throw new SecurityException("用户名或密码错误");
        }

        // 验证密码
        if (!user.getPassword().equals(loginRequest.getPassword())) { // 简化处理，实际应使用加密
            throw new SecurityException("用户名或密码错误");
        }

        // 生成认证响应
        AuthResponseDTO response = new AuthResponseDTO();
        response.setUid(user.getUid());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setUserType(user.getUserType());
        response.setToken(UUID.randomUUID().toString());
        response.setExpiresAt(LocalDateTime.now().plusHours(1));

        return response;
    }

    @Override
    public boolean validateRegisterRequest(RegisterRequestDTO registerRequest, List<String> errors) {
        System.out.println("开始验证注册请求参数");
        System.out.println("密码: " + registerRequest.getPassword());
        System.out.println("手机号: " + registerRequest.getPhone());
        System.out.println("用户类型: " + registerRequest.getUserType());
        System.out.println("昵称: " + registerRequest.getNickname());

        boolean isValid = true;

        // 验证密码
        if (registerRequest.getPassword() == null || registerRequest.getPassword().isEmpty()) {
            errors.add("password:密码不能为空");
            isValid = false;
        } else if (registerRequest.getPassword().length() < 8 || registerRequest.getPassword().length() > 32) {
            errors.add("password: '" + registerRequest.getPassword() + "' 长度必须在8-32个字符之间");
            isValid = false;
        } else if (!registerRequest.getPassword().matches(".*[a-z].*") ||
                !registerRequest.getPassword().matches(".*[A-Z].*") ||
                !registerRequest.getPassword().matches(".*[0-9].*")) {
            errors.add("密码： '" + registerRequest.getPassword() + "' 必须包含大小写字母和数字");
            isValid = false;
        }

        // 验证昵称（可选）
        if (registerRequest.getNickname() != null && registerRequest.getNickname().length() > 30) {
            errors.add("nickname:昵称长度不能超过30个字符");
            isValid = false;
        }

        // 验证手机号
        if (registerRequest.getPhone() == null || registerRequest.getPhone().isEmpty()) {
            errors.add("phone:手机号不能为空");
            isValid = false;
        } else if (!registerRequest.getPhone().matches("1[3-9]\\d{9}")) {
            errors.add("phone:手机号格式不正确");
            isValid = false;
        }

        // 验证用户类型
        if (registerRequest.getUserType() == null || registerRequest.getUserType().isEmpty()) {
            errors.add("user_type:用户类型不能为空");
            isValid = false;
        } else {
            String[] validTypes = {"farmer", "buyer", "expert", "bank", "admin"};
            boolean validType = false;
            for (String type : validTypes) {
                if (type.equals(registerRequest.getUserType())) {
                    validType = true;
                    break;
                }
            }
            if (!validType) {
                errors.add("user_type:用户类型不正确");
                isValid = false;
            }
        }

        return isValid;
    }


    @Override
    public User findUserByPhone(String phone) throws SQLException {
        System.out.println("开始查询用户: " + phone);
        try {
            Connection conn = databaseManager.getConnection();
            System.out.println("数据库连接成功");
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE phone = ?");
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            System.out.println("执行查询完成");

            User user = null;
            if (rs.next()) {
                user = new User();
                user.setUid(rs.getString("uid"));
                user.setPassword(rs.getString("password"));
                user.setNickname(rs.getString("nickname"));
                user.setPhone(rs.getString("phone"));
                user.setUserType(rs.getString("user_type"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                user.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                        rs.getTimestamp("updated_at").toLocalDateTime() : null);
                System.out.println("找到用户: " + user.getPhone());
            } else {
                System.out.println("未找到用户: " + phone);
            }

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("数据库资源已关闭");

            return user;
        } catch (SQLException e) {
            System.err.println("查询用户失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("查询用户失败: " + e.getMessage());
        }
    }


    @Override
    public void saveUser(User user) throws SQLException {
        System.out.println("开始保存用户: " + user.getPhone());
        try {
            Connection conn = databaseManager.getConnection();
            System.out.println("数据库连接成功");
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (uid, password, nickname, phone, user_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, user.getUid());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getNickname());
            stmt.setString(4, user.getPhone());
            stmt.setString(5, user.getUserType());
            stmt.setTimestamp(6, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(user.getUpdatedAt()));

            System.out.println("执行插入操作");
            int result = stmt.executeUpdate();
            System.out.println("插入结果: " + result);
            stmt.close();
            conn.close();
            System.out.println("数据库资源已关闭");
        } catch (SQLException e) {
            System.err.println("保存用户失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存用户失败: " + e.getMessage());
        }
    }

}
