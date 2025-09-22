package service.auth;

import dto.AuthResponseDTO;
import dto.LoginRequestDTO;
import dto.RegisterRequestDTO;
import entity.User;
import java.sql.SQLException;
import java.util.List;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO registerRequest) throws SQLException, IllegalArgumentException;
    AuthResponseDTO login(LoginRequestDTO loginRequest) throws SQLException, SecurityException;
    boolean validateRegisterRequest(RegisterRequestDTO registerRequest, List<String> errors);
    User findUserByPhone(String phone) throws SQLException;
    void saveUser(User user) throws SQLException;
}
