package controller;

import dto.auth.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.auth.AuthService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerIT {

    @Mock
    AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void register_success_returns_200() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPhone("13800138010");
        req.setPassword("Abcdef12");
        req.setNickname("用户");
        req.setUserType("buyer");

        AuthResponseDTO respDto = new AuthResponseDTO();
        respDto.setUid("U-1");
        respDto.setPhone("13800138010");
        respDto.setNickname("用户");
        respDto.setUserType("buyer");
        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(respDto);

        Map<String, Object> resp = controller.register(req);
        assertThat(resp.get("code")).isEqualTo(200);
        AuthResponseDTO data = (AuthResponseDTO) resp.get("data");
        assertThat(data.getUid()).isEqualTo("U-1");
    }

    @Test
    void register_validation_error_returns_400_with_errors() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("phone:手机号不能为空; password:密码不能为空"));

        Map<String, Object> resp = controller.register(req);
        assertThat(resp.get("code")).isEqualTo(400);
        assertThat(resp.get("message")).isEqualTo("参数验证失败");
        List<Map<String, String>> errors = (List<Map<String, String>>) resp.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0).get("field")).isEqualTo("phone");
        assertThat(errors.get(1).get("field")).isEqualTo("password");
    }

    @Test
    void register_duplicate_phone_returns_409() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new SQLException("该手机号已注册此用户类型"));

        Map<String, Object> resp = controller.register(req);
        assertThat(resp.get("code")).isEqualTo(409);
        assertThat(resp.get("message")).isEqualTo("该手机号已注册此用户类型");
    }

    @Test
    void login_success_returns_200() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Abcdef12");
        req.setUserType("farmer");

        AuthResponseDTO respDto = new AuthResponseDTO();
        respDto.setUid("U-2");
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(respDto);

        Map<String, Object> resp = controller.login(req);
        assertThat(resp.get("code")).isEqualTo(200);
        AuthResponseDTO data = (AuthResponseDTO) resp.get("data");
        assertThat(data.getUid()).isEqualTo("U-2");
    }

    @Test
    void login_validation_error_returns_400() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("password:密码不能为空"));

        Map<String, Object> resp = controller.login(req);
        assertThat(resp.get("code")).isEqualTo(400);
        assertThat(resp.get("message")).isEqualTo("参数验证失败");
        List<Map<String, String>> errors = (List<Map<String, String>>) resp.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get(0).get("field")).isEqualTo("password");
    }

    @Test
    void login_security_error_returns_401() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new SecurityException("认证失败"));

        Map<String, Object> resp = controller.login(req);
        assertThat(resp.get("code")).isEqualTo(401);
        assertThat(resp.get("message")).isEqualTo("用户名或密码错误");
    }

    @Test
    void login_unknown_error_returns_500() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new RuntimeException("未知错误"));

        Map<String, Object> resp = controller.login(req);
        assertThat(resp.get("code")).isEqualTo(500);
        assertThat(resp.get("message")).isEqualTo("服务器内部错误");
    }

    @Test
    void get_balance_missing_phone_returns_400() {
        Map<String, Object> resp = controller.getBalance(null, "buyer");
        assertThat(resp.get("code")).isEqualTo(400);
        assertThat(resp.get("message")).isEqualTo("手机号不能为空");
    }

    @Test
    void get_balance_missing_user_type_returns_400() {
        Map<String, Object> resp = controller.getBalance("13800138000", null);
        assertThat(resp.get("code")).isEqualTo(400);
        assertThat(resp.get("message")).isEqualTo("用户类型不能为空");
    }

    @Test
    void get_balance_success_returns_200_and_value() throws Exception {
        when(authService.getBalance("13800138000", "buyer")).thenReturn(new BigDecimal("100.50"));
        Map<String, Object> resp = controller.getBalance("13800138000", "buyer");
        assertThat(resp.get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertThat(data.get("balance")).isEqualTo(new BigDecimal("100.50"));
    }

    @Test
    void get_balance_service_exception_returns_500() throws Exception {
        when(authService.getBalance("13800138000", "buyer")).thenThrow(new RuntimeException("失败"));
        Map<String, Object> resp = controller.getBalance("13800138000", "buyer");
        assertThat(resp.get("code")).isEqualTo(500);
        assertThat(((String) resp.get("message")).startsWith("服务器内部错误")).isTrue();
    }
}
