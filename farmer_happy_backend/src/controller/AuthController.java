package controller;

import dto.AuthResponseDTO;
import dto.LoginRequestDTO;
import dto.RegisterRequestDTO;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthController {
    private AuthService authService;

    public AuthController() {
        this.authService = new AuthServiceImpl();
    }

    public Map<String, Object> register(RegisterRequestDTO request) {
        System.out.println("进入注册控制器，请求参数: " + request.getPhone() + ", " + request.getPassword());

        Map<String, Object> response = new HashMap<>();

        try {
            AuthResponseDTO authResponse = authService.register(request);
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", authResponse);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", "参数验证失败");

            List<Map<String, String>> errors = new ArrayList<>();
            String[] errorMessages = e.getMessage().split("; ");
            for (String errorMsg : errorMessages) {
                Map<String, String> error = new HashMap<>();
                error.put("field", "unknown"); // 简化处理
                error.put("message", errorMsg);
                errors.add(error);
            }
            response.put("errors", errors);
        } catch (SQLException e) {
            if (e.getMessage().contains("该手机号已被注册")) {
                response.put("code", 409);
                response.put("message", "该手机号已被注册");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误");
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误");
        }

        return response;
    }

    public Map<String, Object> login(LoginRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            AuthResponseDTO authResponse = authService.login(request);
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", authResponse);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", "参数验证失败");

            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "unknown"); // 简化处理
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (SecurityException e) {
            response.put("code", 401);
            response.put("message", "用户名或密码错误");
        } catch (SQLException e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误");
        }

        return response;
    }
}
