// controller/AuthController.java
package controller;

import dto.auth.*;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthController {
    private AuthService authService;

    public AuthController() {
        this.authService = new AuthServiceImpl();
    }

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public Map<String, Object> register(RegisterRequestDTO request) {
        System.out.println("AuthController.register - 开始处理注册请求");
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("AuthController.register - 调用 authService.register");
            AuthResponseDTO authResponse = authService.register(request);
            System.out.println("AuthController.register - authService.register 返回成功");
            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", authResponse);
            System.out.println("AuthController.register - 响应构建完成");
        } catch (IllegalArgumentException e) {
            System.out.println("AuthController.register - 捕获 IllegalArgumentException: " + e.getMessage());
            response.put("code", 400);
            response.put("message", "参数验证失败");

            List<Map<String, String>> errors = new ArrayList<>();
            String[] errorMessages = e.getMessage().split("; ");
            for (String errorMsg : errorMessages) {
                Map<String, String> error = new HashMap<>();
                error.put("field", extractFieldName(errorMsg)); // 提取字段名
                error.put("message", errorMsg);
                errors.add(error);
            }
            response.put("errors", errors);
        } catch (SQLException e) {
            if (e.getMessage().contains("该手机号已注册此用户类型")) {
                response.put("code", 409);
                response.put("message", "该手机号已注册此用户类型");
            } else if (e.getMessage().contains("密码错误")) {
                response.put("code", 400);
                response.put("message", "密码错误");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误");
            }
        } catch (Exception e) {
            System.out.println("AuthController.register - 捕获异常: " + e.getMessage());
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误");
        }

        System.out.println("AuthController.register - 返回响应，code=" + response.get("code"));
        return response;
    }

    public Map<String, Object> login(LoginRequestDTO request) {
        System.out.println("AuthController.login - 开始处理登录请求");
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("AuthController.login - 调用 authService.login");
            AuthResponseDTO authResponse = authService.login(request);
            System.out.println("AuthController.login - authService.login 返回成功");
            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", authResponse);
            System.out.println("AuthController.login - 响应构建完成");
        } catch (IllegalArgumentException e) {
            System.out.println("AuthController.login - 捕获 IllegalArgumentException: " + e.getMessage());
            response.put("code", 400);
            response.put("message", "参数验证失败");

            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", extractFieldName(e.getMessage())); // 提取字段名
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (SecurityException e) {
            System.out.println("AuthController.login - 捕获 SecurityException: " + e.getMessage());
            response.put("code", 401);
            response.put("message", "用户名或密码错误");
        } catch (Exception e) {
            System.out.println("AuthController.login - 捕获异常: " + e.getMessage());
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误");
        }

        System.out.println("AuthController.login - 返回响应，code=" + response.get("code"));
        return response;
    }

    public Map<String, Object> getBalance(String phone, String userType) {
        System.out.println("AuthController.getBalance - 开始处理获取余额请求");
        Map<String, Object> response = new HashMap<>();

        try {
            if (phone == null || phone.isEmpty()) {
                response.put("code", 400);
                response.put("message", "手机号不能为空");
                return response;
            }

            if (userType == null || userType.isEmpty()) {
                response.put("code", 400);
                response.put("message", "用户类型不能为空");
                return response;
            }

            java.math.BigDecimal balance = authService.getBalance(phone, userType);
            if (balance == null) {
                balance = java.math.BigDecimal.ZERO;
            }

            response.put("code", 200);
            response.put("message", "成功");
            Map<String, Object> data = new HashMap<>();
            data.put("balance", balance);
            response.put("data", data);
            System.out.println("AuthController.getBalance - 返回余额: " + balance);
        } catch (Exception e) {
            System.out.println("AuthController.getBalance - 捕获异常: " + e.getMessage());
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }

        return response;
    }

    private String extractFieldName(String errorMessage) {
        System.out.println("AuthController.extractFieldName - 处理错误消息: [" + errorMessage + "]");
        
        // 匹配 "field:message" 或 "field: message" 格式的错误消息
        Pattern pattern = Pattern.compile("^([\\w_]+):.*");
        Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            String field = matcher.group(1);
            System.out.println("AuthController.extractFieldName - 通过正则匹配到字段: " + field);
            return field;
        }
        
        // 特殊处理已知错误消息
        if (errorMessage.contains("该手机号已注册此用户类型")) {
            System.out.println("AuthController.extractFieldName - 匹配到：该手机号已注册此用户类型");
            return "phone";
        }
        if (errorMessage.contains("密码错误")) {
            System.out.println("AuthController.extractFieldName - 匹配到：密码错误");
            return "password";
        }
        if (errorMessage.contains("用户类型不能为空")) {
            System.out.println("AuthController.extractFieldName - 匹配到：用户类型不能为空");
            return "user_type";
        }
        if (errorMessage.contains("手机号不能为空")) {
            System.out.println("AuthController.extractFieldName - 匹配到：手机号不能为空");
            return "phone";
        }
        if (errorMessage.contains("密码不能为空")) {
            System.out.println("AuthController.extractFieldName - 匹配到：密码不能为空");
            return "password";
        }
        if (errorMessage.contains("农场名称不能为空")) {
            System.out.println("AuthController.extractFieldName - 匹配到：农场名称不能为空");
            return "farm_name";
        }
        
        System.out.println("AuthController.extractFieldName - 未匹配到任何已知错误，返回 unknown");
        return "unknown";
    }

}
