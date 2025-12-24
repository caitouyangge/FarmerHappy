// service/auth/AuthService.java
package service.auth;

import dto.auth.*;
import dto.farmer.FarmerRegisterRequestDTO;
import entity.User;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO registerRequest) throws SQLException, IllegalArgumentException;
    AuthResponseDTO login(LoginRequestDTO loginRequest) throws SQLException, SecurityException;
    boolean validateRegisterRequest(RegisterRequestDTO registerRequest, List<String> errors);
    User findUserByPhone(String phone) throws SQLException;
    void saveUser(User user) throws SQLException;
    boolean checkUserTypeExists(String uid, String userType) throws SQLException;
    BigDecimal getBalance(String phone, String userType) throws SQLException;

    // 新增针对不同类型用户的保存方法
    void saveFarmerExtension(String uid, FarmerRegisterRequestDTO farmerRequest) throws SQLException;
    void saveBuyerExtension(String uid, BuyerRegisterRequestDTO buyerRequest) throws SQLException;
    void saveExpertExtension(String uid, ExpertRegisterRequestDTO expertRequest) throws SQLException;
    void saveBankExtension(String uid, BankRegisterRequestDTO bankRequest) throws SQLException;
    
    // 更新用户信息
    void updateProfile(UpdateProfileRequestDTO request) throws SQLException, IllegalArgumentException;
    
    // 充值
    void recharge(RechargeRequestDTO request) throws SQLException, IllegalArgumentException;
    
    // 获取用户详细信息
    UserProfileResponseDTO getUserProfile(String phone, String userType) throws SQLException, IllegalArgumentException;
    
    // 更新买家收货地址
    void updateShippingAddress(UpdateShippingAddressRequestDTO request) throws SQLException, IllegalArgumentException;
}
