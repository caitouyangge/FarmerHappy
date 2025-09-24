package config;

import controller.AuthController;
import dto.LoginRequestDTO;
import dto.RegisterRequestDTO;

import java.util.Map;

public class RouterConfig {
    private AuthController authController;

    public RouterConfig() {
        this.authController = new AuthController();
    }

    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody) {
        switch (path) {
            case "/api/v1/auth/register":
                if ("POST".equals(method)) {
                    return handleRegister(requestBody);
                }
                break;
            case "/api/v1/auth/login":
                if ("POST".equals(method)) {
                    return handleLogin(requestBody);
                }
                break;
        }

        // 默认返回404
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("code", 404);
        response.put("message", "接口不存在");
        return response;
    }

    private Map<String, Object> handleRegister(Map<String, Object> requestBody) {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setPassword((String) requestBody.get("password"));
        request.setNickname((String) requestBody.get("nickname"));
        request.setPhone((String) requestBody.get("phone"));
        request.setUserType((String) requestBody.get("user_type"));

        return authController.register(request);
    }

    private Map<String, Object> handleLogin(Map<String, Object> requestBody) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setPassword((String) requestBody.get("password"));

        return authController.login(request);
    }
}
