// service/auth/AuthServiceImpl.java
package service.auth;

import dto.auth.*;
import dto.farmer.FarmerRegisterRequestDTO;
import entity.User;
import repository.DatabaseManager;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AuthServiceImpl implements AuthService {
    private DatabaseManager databaseManager;

    public AuthServiceImpl() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO registerRequest) throws SQLException, IllegalArgumentException {
        System.out.println("进入注册服务，请求参数: " + registerRequest.getPhone() + ", " + registerRequest.getPassword());
        List<String> errors = new ArrayList<>();
        if (!validateRegisterRequest(registerRequest, errors)) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 查找用户是否存在
            User user = findUserByPhoneWithConnection(conn, registerRequest.getPhone());

            if (user == null) {
                // 用户不存在，创建新用户
                user = new User(
                        registerRequest.getPassword(),
                        registerRequest.getNickname() != null ? registerRequest.getNickname() : "",
                        registerRequest.getPhone()
                );

                // 保存用户到数据库
                saveUserWithConnection(conn, user);
                System.out.println("用户保存成功");

                // 根据用户类型保存扩展信息
                System.out.println("开始保存扩展信息，用户类型: " + registerRequest.getUserType());
                saveUserExtensionByType(conn, user.getUid(), registerRequest);
            } else {
                // 用户已存在，验证密码
                if (!user.getPassword().equals(registerRequest.getPassword())) {
                    throw new IllegalArgumentException("密码错误");
                }

                // 检查该用户类型是否已经存在
                if (checkUserTypeExistsWithConnection(conn, user.getUid(), registerRequest.getUserType())) {
                    throw new IllegalArgumentException("该手机号已注册此用户类型");
                }

                // 保存扩展信息
                saveUserExtensionByType(conn, user.getUid(), registerRequest);
            }

            conn.commit();

            // 生成认证响应
            System.out.println("开始生成认证响应");
            AuthResponseDTO response = new AuthResponseDTO();
            response.setUid(user.getUid());
            response.setNickname(user.getNickname());
            response.setPhone(user.getPhone());
            response.setUserType(registerRequest.getUserType());
            System.out.println("认证响应生成完成");

            return response;
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private void saveUserExtensionByType(Connection conn, String uid, RegisterRequestDTO registerRequest) throws SQLException {
        if (registerRequest instanceof FarmerRegisterRequestDTO) {
            System.out.println("保存农户扩展信息...");
            saveFarmerExtensionWithConnection(conn, uid, (FarmerRegisterRequestDTO) registerRequest);
            System.out.println("农户扩展信息保存完成");
        } else if (registerRequest instanceof BuyerRegisterRequestDTO) {
            System.out.println("保存买家扩展信息...");
            saveBuyerExtensionWithConnection(conn, uid, (BuyerRegisterRequestDTO) registerRequest);
            System.out.println("买家扩展信息保存完成");
        } else if (registerRequest instanceof ExpertRegisterRequestDTO) {
            System.out.println("保存专家扩展信息...");
            saveExpertExtensionWithConnection(conn, uid, (ExpertRegisterRequestDTO) registerRequest);
            System.out.println("专家扩展信息保存完成");
        } else if (registerRequest instanceof BankRegisterRequestDTO) {
            System.out.println("保存银行扩展信息...");
            saveBankExtensionWithConnection(conn, uid, (BankRegisterRequestDTO) registerRequest);
            System.out.println("银行扩展信息保存完成");
        }
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

        if (loginRequest.getUserType() == null || loginRequest.getUserType().isEmpty()) {
            throw new IllegalArgumentException("用户类型不能为空");
        }

        Connection conn = null;
        try {
            conn = databaseManager.getConnection();

            // 查找用户
            User user = findUserByPhoneWithConnection(conn, loginRequest.getPhone());
            if (user == null) {
                throw new SecurityException("用户名或密码错误");
            }

            // 验证密码
            if (!user.getPassword().equals(loginRequest.getPassword())) {
                throw new SecurityException("用户名或密码错误");
            }

            // 验证用户类型是否存在
            if (!checkUserTypeExistsWithConnection(conn, user.getUid(), loginRequest.getUserType())) {
                throw new SecurityException("该用户类型未注册");
            }

            // 获取用户余额
            java.math.BigDecimal balance = null;
            String userType = loginRequest.getUserType();
            if ("buyer".equals(userType)) {
                balance = databaseManager.getBuyerBalance(loginRequest.getPhone());
            } else if ("farmer".equals(userType)) {
                balance = databaseManager.getFarmerBalance(loginRequest.getPhone());
            } else if ("bank".equals(userType)) {
                balance = databaseManager.getBankBalance(loginRequest.getPhone());
            }
            // 如果余额为null，设置为0
            if (balance == null) {
                balance = java.math.BigDecimal.ZERO;
            }

            // 生成认证响应
            AuthResponseDTO response = new AuthResponseDTO();
            response.setUid(user.getUid());
            response.setNickname(user.getNickname());
            response.setPhone(user.getPhone());
            response.setUserType(loginRequest.getUserType());
            response.setMoney(balance);

            return response;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
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
        try {
            Connection conn = databaseManager.getConnection();
            User user = findUserByPhoneWithConnection(conn, phone);
            conn.close();
            return user;
        } catch (SQLException e) {
            System.err.println("查询用户失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("查询用户失败: " + e.getMessage());
        }
    }

    private User findUserByPhoneWithConnection(Connection conn, String phone) throws SQLException {
        System.out.println("开始查询用户: " + phone);
        try {
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
                // 读取money字段，如果为null则设为0
                java.math.BigDecimal money = rs.getBigDecimal("money");
                user.setMoney(money != null ? money : java.math.BigDecimal.ZERO);
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                user.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                        rs.getTimestamp("updated_at").toLocalDateTime() : null);
                System.out.println("找到用户: " + user.getPhone());
            } else {
                System.out.println("未找到用户: " + phone);
            }

            rs.close();
            stmt.close();
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
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            saveUserWithConnection(conn, user);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void saveUserWithConnection(Connection conn, User user) throws SQLException {
        System.out.println("开始保存用户: " + user.getPhone());
        try {
            System.out.println("数据库连接成功");
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (uid, password, nickname, phone, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, user.getUid());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getNickname());
            stmt.setString(4, user.getPhone());
            stmt.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(user.getUpdatedAt()));

            System.out.println("执行插入操作");
            int result = stmt.executeUpdate();
            System.out.println("插入结果: " + result);
            stmt.close();
            System.out.println("数据库资源已关闭");
        } catch (SQLException e) {
            System.err.println("保存用户失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存用户失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkUserTypeExists(String uid, String userType) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            boolean exists = checkUserTypeExistsWithConnection(conn, uid, userType);
            return exists;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private boolean checkUserTypeExistsWithConnection(Connection conn, String uid, String userType) throws SQLException {
        String tableName = "";
        switch (userType) {
            case "farmer":
                tableName = "user_farmers";
                break;
            case "buyer":
                tableName = "user_buyers";
                break;
            case "expert":
                tableName = "user_experts";
                break;
            case "bank":
                tableName = "user_banks";
                break;
            default:
                return false;
        }

        String query = "SELECT COUNT(*) as count FROM " + tableName + " WHERE uid = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, uid);
        ResultSet rs = stmt.executeQuery();

        boolean exists = false;
        if (rs.next()) {
            exists = rs.getInt("count") > 0;
        }

        rs.close();
        stmt.close();
        return exists;
    }

    // 农户扩展信息保存方法
    @Override
    public void saveFarmerExtension(String uid, FarmerRegisterRequestDTO farmerRequest) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            saveFarmerExtensionWithConnection(conn, uid, farmerRequest);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void saveFarmerExtensionWithConnection(Connection conn, String uid, FarmerRegisterRequestDTO farmerRequest) throws SQLException {
        System.out.println("saveFarmerExtension - 开始");
        System.out.println("UID: " + uid);
        System.out.println("农场名称: " + farmerRequest.getFarmName());
        System.out.println("农场地址: " + farmerRequest.getFarmAddress());
        System.out.println("农场规模: " + farmerRequest.getFarmSize());

        try {
            System.out.println("saveFarmerExtension - 数据库连接成功");

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

            System.out.println("saveFarmerExtension - 执行插入操作");
            int result = stmt.executeUpdate();
            System.out.println("saveFarmerExtension - 插入结果: " + result);

            stmt.close();
            System.out.println("saveFarmerExtension - 数据库资源已关闭");
        } catch (SQLException e) {
            System.err.println("保存农户扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存农户扩展信息失败: " + e.getMessage());
        }
    }

    // 买家扩展信息保存方法
    @Override
    public void saveBuyerExtension(String uid, BuyerRegisterRequestDTO buyerRequest) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            saveBuyerExtensionWithConnection(conn, uid, buyerRequest);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void saveBuyerExtensionWithConnection(Connection conn, String uid, BuyerRegisterRequestDTO buyerRequest) throws SQLException {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_buyers (uid, shipping_address) VALUES (?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, buyerRequest.getShippingAddress());

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("保存买家扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存买家扩展信息失败: " + e.getMessage());
        }
    }

    // 专家扩展信息保存方法
    @Override
    public void saveExpertExtension(String uid, ExpertRegisterRequestDTO expertRequest) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            saveExpertExtensionWithConnection(conn, uid, expertRequest);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void saveExpertExtensionWithConnection(Connection conn, String uid, ExpertRegisterRequestDTO expertRequest) throws SQLException {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_experts (uid, expertise_field, work_experience, service_area, consultation_fee) VALUES (?, ?, ?, ?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, expertRequest.getExpertiseField());
            if (expertRequest.getWorkExperience() != null) {
                stmt.setInt(3, expertRequest.getWorkExperience());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, expertRequest.getServiceArea());
            if (expertRequest.getConsultationFee() != null) {
                stmt.setDouble(5, expertRequest.getConsultationFee());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("保存专家扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存专家扩展信息失败: " + e.getMessage());
        }
    }

    // 银行扩展信息保存方法
    @Override
    public void saveBankExtension(String uid, BankRegisterRequestDTO bankRequest) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            saveBankExtensionWithConnection(conn, uid, bankRequest);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void saveBankExtensionWithConnection(Connection conn, String uid, BankRegisterRequestDTO bankRequest) throws SQLException {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_banks (uid, bank_name, branch_name, contact_person, contact_phone) VALUES (?, ?, ?, ?, ?)"
            );
            stmt.setString(1, uid);
            stmt.setString(2, bankRequest.getBankName());
            stmt.setString(3, bankRequest.getBranchName());
            stmt.setString(4, bankRequest.getContactPerson());
            stmt.setString(5, bankRequest.getContactPhone());

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("保存银行扩展信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("保存银行扩展信息失败: " + e.getMessage());
        }
    }

    @Override
    public java.math.BigDecimal getBalance(String phone, String userType) throws SQLException {
        java.math.BigDecimal balance = null;
        if ("buyer".equals(userType)) {
            balance = databaseManager.getBuyerBalance(phone);
        } else if ("farmer".equals(userType)) {
            balance = databaseManager.getFarmerBalance(phone);
        } else if ("bank".equals(userType)) {
            balance = databaseManager.getBankBalance(phone);
        }
        // 如果余额为null，返回0
        return balance != null ? balance : java.math.BigDecimal.ZERO;
    }

    @Override
    public void updateProfile(UpdateProfileRequestDTO request) throws SQLException, IllegalArgumentException {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        
        if (request.getNickname().length() > 30) {
            throw new IllegalArgumentException("昵称长度不能超过30个字符");
        }
        
        // 验证用户是否存在
        User user = findUserByPhone(request.getPhone());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        // 更新昵称
        databaseManager.updateUserNickname(request.getPhone(), request.getNickname());
    }

    @Override
    public void recharge(RechargeRequestDTO request) throws SQLException, IllegalArgumentException {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        
        if (request.getUserType() == null || request.getUserType().trim().isEmpty()) {
            throw new IllegalArgumentException("用户类型不能为空");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("充值金额必须大于0");
        }
        
        // 验证用户是否存在
        User user = findUserByPhone(request.getPhone());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        // 验证用户类型是否匹配（需要检查用户是否有该类型的角色）
        // 这里简化处理，直接根据userType更新对应的用户余额
        if ("buyer".equals(request.getUserType())) {
            String buyerUid = databaseManager.getBuyerUidByPhone(request.getPhone());
            if (buyerUid == null) {
                throw new IllegalArgumentException("该用户不是买家");
            }
            databaseManager.updateBuyerBalance(buyerUid, request.getAmount());
        } else if ("farmer".equals(request.getUserType())) {
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("该用户不是农户");
            }
            databaseManager.updateFarmerBalance(user.getUid(), request.getAmount());
        } else if ("bank".equals(request.getUserType())) {
            // 检查银行角色
            Connection conn = databaseManager.getConnection();
            try {
                String sql = "SELECT COUNT(*) as count FROM user_banks WHERE uid = ? AND enable = TRUE";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, user.getUid());
                ResultSet rs = stmt.executeQuery();
                boolean isBank = false;
                if (rs.next() && rs.getInt("count") > 0) {
                    isBank = true;
                }
                rs.close();
                stmt.close();
                if (!isBank) {
                    throw new IllegalArgumentException("该用户不是银行");
                }
                databaseManager.updateUserBalance(user.getUid(), request.getAmount());
            } finally {
                databaseManager.closeConnection();
            }
        } else {
            throw new IllegalArgumentException("不支持的用户类型");
        }
    }

    private Map<String, Object> checkUserFarmerRole(String uid) throws SQLException {
        Connection conn = databaseManager.getConnection();
        try {
            String sql = "SELECT farmer_id FROM user_farmers WHERE uid = ? AND enable = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, uid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                result.put("farmer_id", rs.getLong("farmer_id"));
                rs.close();
                stmt.close();
                return result;
            }
            rs.close();
            stmt.close();
            return null;
        } finally {
            databaseManager.closeConnection();
        }
    }

    @Override
    public UserProfileResponseDTO getUserProfile(String phone, String userType) throws SQLException, IllegalArgumentException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("用户类型不能为空");
        }

        // 查找用户
        User user = findUserByPhone(phone);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 构建基础用户信息
        UserProfileResponseDTO profile = new UserProfileResponseDTO();
        profile.setUid(user.getUid());
        profile.setPhone(user.getPhone());
        profile.setNickname(user.getNickname());
        profile.setUserType(userType);
        profile.setMoney(user.getMoney());

        // 根据用户类型获取扩展信息
        if ("buyer".equals(userType)) {
            Map<String, Object> buyerInfo = databaseManager.getBuyerInfoByPhone(phone);
            if (buyerInfo != null) {
                profile.setShippingAddress((String) buyerInfo.get("shipping_address"));
                profile.setMemberLevel((String) buyerInfo.get("member_level"));
            }
        } else if ("farmer".equals(userType)) {
            Map<String, Object> farmerInfo = databaseManager.getFarmerInfoByUid(user.getUid());
            if (farmerInfo != null) {
                profile.setFarmName((String) farmerInfo.get("farm_name"));
                profile.setFarmAddress((String) farmerInfo.get("farm_address"));
                profile.setFarmSize((java.math.BigDecimal) farmerInfo.get("farm_size"));
            }
        }
        // 注意：专家和银行的扩展信息获取可以根据需要添加

        return profile;
    }

    @Override
    public void updateShippingAddress(UpdateShippingAddressRequestDTO request) throws SQLException, IllegalArgumentException {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        // 验证收货地址
        if (request.getShippingAddress() != null && request.getShippingAddress().length() > 500) {
            throw new IllegalArgumentException("收货地址长度不能超过500个字符");
        }

        // 验证用户是否存在
        User user = findUserByPhone(request.getPhone());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 验证用户是否为买家
        String buyerUid = databaseManager.getBuyerUidByPhone(request.getPhone());
        if (buyerUid == null) {
            throw new IllegalArgumentException("该用户不是买家");
        }

        // 更新收货地址
        databaseManager.updateBuyerShippingAddress(request.getPhone(), request.getShippingAddress());
    }
}
