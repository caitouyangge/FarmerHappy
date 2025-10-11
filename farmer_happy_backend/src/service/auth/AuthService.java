package service.auth;

import dto.*;
import entity.User;
import java.sql.SQLException;
import java.util.List;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO registerRequest) throws SQLException, IllegalArgumentException;
    AuthResponseDTO login(LoginRequestDTO loginRequest) throws SQLException, SecurityException;
    boolean validateRegisterRequest(RegisterRequestDTO registerRequest, List<String> errors);
    User findUserByPhone(String phone) throws SQLException;
    void saveUser(User user) throws SQLException;

    // 新增针对不同类型用户的保存方法
    void saveFarmerExtension(String uid, FarmerRegisterRequestDTO farmerRequest) throws SQLException;
    void saveBuyerExtension(String uid, BuyerRegisterRequestDTO buyerRequest) throws SQLException;
    void saveExpertExtension(String uid, ExpertRegisterRequestDTO expertRequest) throws SQLException;
    void saveBankExtension(String uid, BankRegisterRequestDTO bankRequest) throws SQLException;
}
