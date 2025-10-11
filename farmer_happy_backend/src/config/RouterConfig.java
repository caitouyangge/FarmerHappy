package config;

import controller.AuthController;
import dto.*;

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

        return authController.login(request);
    }
}
