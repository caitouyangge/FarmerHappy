// src/config/RouterConfig.java
package config;

import controller.AuthController;
import controller.ProductController;
import dto.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RouterConfig {
    private AuthController authController;
    private ProductController productController;

    public RouterConfig() {
        this.authController = new AuthController();
        this.productController = new ProductController();
    }

    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody, Map<String, String> headers, String sessionId) {
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
            case "/api/v1/farmer/products":
                if ("POST".equals(method)) {
                    // 不再使用会话，直接处理请求
                    return productController.createProduct(parseProductRequest(requestBody));
                }
                break;
        }

        // 默认返回404
        Map<String, Object> response = new HashMap<>();
        response.put("code", 404);
        response.put("message", "接口不存在");
        return response;
    }

    // 重载方法以保持向后兼容
    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody) {
        return handleRequest(path, method, requestBody, new HashMap<>(), null);
    }

    // 重载方法以保持向后兼容
    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody, Map<String, String> headers) {
        return handleRequest(path, method, requestBody, headers, null);
    }

    private Map<String, Object> handleRegister(Map<String, Object> requestBody) {
        String userType = (String) requestBody.get("user_type");

        if (userType == null) {
            userType = (String) requestBody.get("userType");
        }

        switch (userType) {
            case "farmer":
                FarmerRegisterRequestDTO farmerRequest = new FarmerRegisterRequestDTO();
                farmerRequest.setPassword((String) requestBody.get("password"));
                farmerRequest.setNickname((String) requestBody.get("nickname"));
                farmerRequest.setPhone((String) requestBody.get("phone"));
                farmerRequest.setUserType(userType);
                farmerRequest.setFarmName((String) requestBody.get("farm_name"));
                farmerRequest.setFarmAddress((String) requestBody.get("farm_address"));
                if (requestBody.get("farm_size") instanceof Number) {
                    farmerRequest.setFarmSize(((Number) requestBody.get("farm_size")).doubleValue());
                }
                return authController.register(farmerRequest);

            case "buyer":
                BuyerRegisterRequestDTO buyerRequest = new BuyerRegisterRequestDTO();
                buyerRequest.setPassword((String) requestBody.get("password"));
                buyerRequest.setNickname((String) requestBody.get("nickname"));
                buyerRequest.setPhone((String) requestBody.get("phone"));
                buyerRequest.setUserType(userType);
                buyerRequest.setShippingAddress((String) requestBody.get("shipping_address"));
                return authController.register(buyerRequest);

            case "expert":
                ExpertRegisterRequestDTO expertRequest = new ExpertRegisterRequestDTO();
                expertRequest.setPassword((String) requestBody.get("password"));
                expertRequest.setNickname((String) requestBody.get("nickname"));
                expertRequest.setPhone((String) requestBody.get("phone"));
                expertRequest.setUserType(userType);
                expertRequest.setExpertiseField((String) requestBody.get("expertise_field"));
                if (requestBody.get("work_experience") instanceof Number) {
                    expertRequest.setWorkExperience(((Number) requestBody.get("work_experience")).intValue());
                }
                return authController.register(expertRequest);

            case "bank":
                BankRegisterRequestDTO bankRequest = new BankRegisterRequestDTO();
                bankRequest.setPassword((String) requestBody.get("password"));
                bankRequest.setNickname((String) requestBody.get("nickname"));
                bankRequest.setPhone((String) requestBody.get("phone"));
                bankRequest.setUserType(userType);
                bankRequest.setBankName((String) requestBody.get("bank_name"));
                bankRequest.setBranchName((String) requestBody.get("branch_name"));
                return authController.register(bankRequest);

            default:
                RegisterRequestDTO defaultRequest = new RegisterRequestDTO();
                defaultRequest.setPassword((String) requestBody.get("password"));
                defaultRequest.setNickname((String) requestBody.get("nickname"));
                defaultRequest.setPhone((String) requestBody.get("phone"));
                defaultRequest.setUserType(userType);
                return authController.register(defaultRequest);
        }
    }

    private Map<String, Object> handleLogin(Map<String, Object> requestBody) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setPassword((String) requestBody.get("password"));

        Map<String, Object> loginResult = authController.login(request);

        return loginResult;
    }

    private ProductCreateRequestDTO parseProductRequest(Map<String, Object> requestBody) {
        ProductCreateRequestDTO request = new ProductCreateRequestDTO();
        request.setTitle((String) requestBody.get("title"));
        request.setSpecification((String) requestBody.get("specification"));
        if (requestBody.get("price") instanceof Number) {
            request.setPrice(((Number) requestBody.get("price")).doubleValue());
        }
        if (requestBody.get("stock") instanceof Number) {
            request.setStock(((Number) requestBody.get("stock")).intValue());
        }
        request.setDescription((String) requestBody.get("description"));
        request.setOrigin((String) requestBody.get("origin"));
        request.setPhone((String) requestBody.get("phone")); // 添加手机号字段

        // 处理图片数组
        if (requestBody.get("images") instanceof List) {
            List<?> imagesObj = (List<?>) requestBody.get("images");
            List<String> images = new ArrayList<>();
            for (Object img : imagesObj) {
                if (img instanceof String) {
                    images.add((String) img);
                }
            }
            request.setImages(images);
        }

        return request;
    }
}
