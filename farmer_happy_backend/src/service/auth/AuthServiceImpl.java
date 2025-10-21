package service.auth;

import dto.auth.*;
import dto.farmer.FarmerRegisterRequestDTO;
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

        // 根据用户类型保存扩展信息
        if (registerRequest instanceof FarmerRegisterRequestDTO) {
            saveFarmerExtension(user.getUid(), (FarmerRegisterRequestDTO) registerRequest);
        } else if (registerRequest instanceof BuyerRegisterRequestDTO) {
            saveBuyerExtension(user.getUid(), (BuyerRegisterRequestDTO) registerRequest);
        } else if (registerRequest instanceof ExpertRegisterRequestDTO) {
            saveExpertExtension(user.getUid(), (ExpertRegisterRequestDTO) registerRequest);
        } else if (registerRequest instanceof BankRegisterRequestDTO) {
            saveBankExtension(user.getUid(), (BankRegisterRequestDTO) registerRequest);
        }

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
            String[] validTypes = {"farmer", "buyer", "expert", "bank"};
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

        // 验证特定用户类型的额外字段
        if (registerRequest instanceof FarmerRegisterRequestDTO) {
            FarmerRegisterRequestDTO farmerRequest = (FarmerRegisterRequestDTO) registerRequest;
            if (farmerRequest.getFarmName() == null || farmerRequest.getFarmName().isEmpty()) {
                errors.add("farm_name:农场名称不能为空");
                isValid = false;
            } else if (farmerRequest.getFarmName().length() > 100) {
                errors.add("farm_name:农场名称长度不能超过100个字符");
                isValid = false;
            }

            if (farmerRequest.getFarmAddress() != null && farmerRequest.getFarmAddress().length() > 200) {
                errors.add("farm_address:农场地址长度不能超过200个字符");
                isValid = false;
            }
        } else if (registerRequest instanceof BuyerRegisterRequestDTO) {
            BuyerRegisterRequestDTO buyerRequest = (BuyerRegisterRequestDTO) registerRequest;
            if (buyerRequest.getShippingAddress() != null && buyerRequest.getShippingAddress().length() > 500) {
                errors.add("shipping_address:收货地址长度不能超过500个字符");
                isValid = false;
            }
        } else if (registerRequest instanceof ExpertRegisterRequestDTO) {
            ExpertRegisterRequestDTO expertRequest = (ExpertRegisterRequestDTO) registerRequest;
            if (expertRequest.getExpertiseField() == null || expertRequest.getExpertiseField().isEmpty()) {
                errors.add("expertise_field:专业领域不能为空");
                isValid = false;
            } else if (expertRequest.getExpertiseField().length() > 100) {
                errors.add("expertise_field:专业领域长度不能超过100个字符");
                isValid = false;
            }
        } else if (registerRequest instanceof BankRegisterRequestDTO) {
            BankRegisterRequestDTO bankRequest = (BankRegisterRequestDTO) registerRequest;
            if (bankRequest.getBankName() == null || bankRequest.getBankName().isEmpty()) {
                errors.add("bank_name:银行名称不能为空");
                isValid = false;
            } else if (bankRequest.getBankName().length() > 100) {
                errors.add("bank_name:银行名称长度不能超过100个字符");
                isValid = false;
            }

            if (bankRequest.getBranchName() != null && bankRequest.getBranchName().length() > 100) {
                errors.add("branch_name:分行名称长度不能超过100个字符");
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

    @Override
    public void saveFarmerExtension(String uid, FarmerRegisterRequestDTO farmerRequest) throws SQLException {
        try {
            Connection conn = databaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_farmers (uid, farm_name, farm_address, farm_size) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, farmerRequest.getFarmName());
            stmt.setString(3, farmerRequest.getFarmAddress());
            if (farmerRequest.getFarmSize() != null) {
                stmt.setDouble(4, farmerRequest.getFarmSize());
            } else {
                stmt.setNull(4, Types.DOUBLE);
            }

            stmt.executeUpdate();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("保存农户扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存农户扩展信息失败: " + e.getMessage());
        }
    }

    @Override
    public void saveBuyerExtension(String uid, BuyerRegisterRequestDTO buyerRequest) throws SQLException {
        try {
            Connection conn = databaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_buyers (uid, shipping_address) VALUES (?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, buyerRequest.getShippingAddress());

            stmt.executeUpdate();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("保存买家扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存买家扩展信息失败: " + e.getMessage());
        }
    }

    @Override
    public void saveExpertExtension(String uid, ExpertRegisterRequestDTO expertRequest) throws SQLException {
        try {
            Connection conn = databaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_experts (uid, expertise_field, work_experience) VALUES (?, ?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, expertRequest.getExpertiseField());
            if (expertRequest.getWorkExperience() != null) {
                stmt.setInt(3, expertRequest.getWorkExperience());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.executeUpdate();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("保存专家扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存专家扩展信息失败: " + e.getMessage());
        }
    }

    @Override
    public void saveBankExtension(String uid, BankRegisterRequestDTO bankRequest) throws SQLException {
        try {
            Connection conn = databaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_banks (uid, bank_name, branch_name) VALUES (?, ?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, bankRequest.getBankName());
            stmt.setString(3, bankRequest.getBranchName());

            stmt.executeUpdate();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println("保存银行扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存银行扩展信息失败: " + e.getMessage());
        }
    }
}
