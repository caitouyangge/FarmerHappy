// src/config/RouterConfig.java
package config;

import controller.AuthController;
import controller.ProductController;
import dto.auth.*;
import dto.farmer.FarmerRegisterRequestDTO;
import dto.farmer.ProductBatchActionRequestDTO; // 新增导入
import dto.farmer.ProductCreateRequestDTO;
import dto.farmer.ProductStatusUpdateRequestDTO;
import dto.farmer.ProductUpdateRequestDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterConfig {
    private AuthController authController;
    private ProductController productController;

    public RouterConfig() {
        this.authController = new AuthController();
        this.productController = new ProductController();
    }

    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody, Map<String, String> headers, String sessionId) {
        // 处理商品上架请求
        Pattern onShelfPattern = Pattern.compile("/api/v1/farmer/products/([^/]+)/on-shelf");
        Matcher onShelfMatcher = onShelfPattern.matcher(path);

        if (onShelfMatcher.matches() && "POST".equals(method)) {
            String productId = onShelfMatcher.group(1);
            // 移除可能存在的花括号
            if (productId.startsWith("{") && productId.endsWith("}")) {
                productId = productId.substring(1, productId.length() - 1);
            }
            return productController.onShelfProduct(productId, parseProductStatusUpdateRequest(requestBody));
        }

        // 处理商品下架请求
        Pattern offShelfPattern = Pattern.compile("/api/v1/farmer/products/([^/]+)/off-shelf");
        Matcher offShelfMatcher = offShelfPattern.matcher(path);

        if (offShelfMatcher.matches() && "POST".equals(method)) {
            String productId = offShelfMatcher.group(1);
            // 移除可能存在的花括号
            if (productId.startsWith("{") && productId.endsWith("}")) {
                productId = productId.substring(1, productId.length() - 1);
            }
            return productController.offShelfProduct(productId, parseProductStatusUpdateRequest(requestBody));
        }

        // 处理商品删除请求
        Pattern deleteProductPattern = Pattern.compile("/api/v1/farmer/products/([^/]+)");
        Matcher deleteProductMatcher = deleteProductPattern.matcher(path);

        if (deleteProductMatcher.matches() && "DELETE".equals(method)) {
            String productId = deleteProductMatcher.group(1);
            // 移除可能存在的花括号
            if (productId.startsWith("{") && productId.endsWith("}")) {
                productId = productId.substring(1, productId.length() - 1);
            }
            return productController.deleteProduct(productId, parseProductStatusUpdateRequest(requestBody));
        }

        // 处理获取单个商品详情请求 (修改路径为 /api/v1/farmer/products/query/{productId})
        Pattern getProductDetailPattern = Pattern.compile("/api/v1/farmer/products/query/([^/]+)");
        Matcher getProductDetailMatcher = getProductDetailPattern.matcher(path);

        if (getProductDetailMatcher.matches() && "POST".equals(method)) {
            String productId = getProductDetailMatcher.group(1);
            // 移除可能存在的花括号
            if (productId.startsWith("{") && productId.endsWith("}")) {
                productId = productId.substring(1, productId.length() - 1);
            }
            return productController.getProductDetail(productId, parseProductStatusUpdateRequest(requestBody));
        }

        // 处理更新商品请求
        Pattern updateProductPattern = Pattern.compile("/api/v1/farmer/products/([^/]+)");
        Matcher updateProductMatcher = updateProductPattern.matcher(path);

        if (updateProductMatcher.matches() && "PUT".equals(method)) {
            String productId = updateProductMatcher.group(1);
            // 移除可能存在的花括号
            if (productId.startsWith("{") && productId.endsWith("}")) {
                productId = productId.substring(1, productId.length() - 1);
            }
            return productController.updateProduct(productId, parseProductUpdateRequest(requestBody));
        }

        // 处理获取商品列表请求
        if ("/api/v1/farmer/products/list_query".equals(path) && "POST".equals(method)) {
            return productController.getProductList(requestBody);
        }

        // 处理批量操作商品请求
        if ("/api/v1/farmer/products/batch-actions".equals(path) && "POST".equals(method)) {
            return productController.batchActionProducts(parseProductBatchActionRequest(requestBody));
        }

        switch (path) {
            case "/api/v1/auth/register":
                if ("POST".equals(method)) {
                    return handleRegister(requestBody);
                }
                break;
            case "/api/v1/auth/login":
                System.out.println("RouterConfig.handleRequest - 匹配到登录路径");
                if ("POST".equals(method)) {
                    System.out.println("RouterConfig.handleRequest - 调用 handleLogin");
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
        System.out.println("RouterConfig.handleRegister - 开始处理注册请求");
        String userType = (String) requestBody.get("user_type");

        if (userType == null) {
            userType = (String) requestBody.get("userType");
        }
        System.out.println("RouterConfig.handleRegister - 用户类型: " + userType);

        switch (userType) {
            case "farmer":
                System.out.println("RouterConfig.handleRegister - 处理农户注册");
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
                System.out.println("RouterConfig.handleRegister - 调用 authController.register");
                Map<String, Object> result = authController.register(farmerRequest);
                System.out.println("RouterConfig.handleRegister - authController.register 返回: " + result);
                return result;

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
        System.out.println("RouterConfig.handleLogin - 开始处理登录请求");
        System.out.println("请求体: " + requestBody);

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setPhone((String) requestBody.get("phone"));
        loginRequest.setPassword((String) requestBody.get("password"));
        // 同时支持 userType 和 user_type 两种字段名
        String userType = (String) requestBody.get("userType");
        if (userType == null) {
            userType = (String) requestBody.get("user_type");
        }
        loginRequest.setUserType(userType);

        System.out.println("RouterConfig.handleLogin - LoginRequestDTO 创建完成");
        System.out.println("LoginRequestDTO - phone: " + loginRequest.getPhone() +
                ", password: " + loginRequest.getPassword() +
                ", userType: " + loginRequest.getUserType());

        Map<String, Object> loginResult = authController.login(loginRequest);
        System.out.println("RouterConfig.handleLogin - authController.login 返回: " + loginResult);

        return loginResult;
    }


    private ProductCreateRequestDTO parseProductRequest(Map<String, Object> requestBody) {
        ProductCreateRequestDTO request = new ProductCreateRequestDTO();
        request.setTitle((String) requestBody.get("title"));
        request.setSpecification((String) requestBody.get("specification"));

        // 处理 price 字段
        Object priceObj = requestBody.get("price");
        if (priceObj instanceof Number) {
            request.setPrice(((Number) priceObj).doubleValue());
        } else if (priceObj instanceof String) {
            try {
                request.setPrice(Double.parseDouble((String) priceObj));
            } catch (NumberFormatException e) {
                // 保持为 null
            }
        }

        // 处理 stock 字段
        Object stockObj = requestBody.get("stock");
        if (stockObj instanceof Number) {
            request.setStock(((Number) stockObj).intValue());
        } else if (stockObj instanceof String) {
            try {
                request.setStock(Integer.parseInt((String) stockObj));
            } catch (NumberFormatException e) {
                // 保持为 null
            }
        }

        request.setDescription((String) requestBody.get("description"));
        request.setOrigin((String) requestBody.get("origin"));
        request.setPhone((String) requestBody.get("phone"));
        request.setCategory((String) requestBody.get("category"));

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

    private ProductStatusUpdateRequestDTO parseProductStatusUpdateRequest(Map<String, Object> requestBody) {
        ProductStatusUpdateRequestDTO request = new ProductStatusUpdateRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        return request;
    }

    private ProductUpdateRequestDTO parseProductUpdateRequest(Map<String, Object> requestBody) {
        ProductUpdateRequestDTO request = new ProductUpdateRequestDTO();
        request.setTitle((String) requestBody.get("title"));
        request.setSpecification((String) requestBody.get("specification"));

        // 处理 price 字段
        Object priceObj = requestBody.get("price");
        if (priceObj instanceof Number) {
            request.setPrice(((Number) priceObj).doubleValue());
        } else if (priceObj instanceof String) {
            try {
                request.setPrice(Double.parseDouble((String) priceObj));
            } catch (NumberFormatException e) {
                // 保持为 null
            }
        }

        // 处理 stock 字段
        Object stockObj = requestBody.get("stock");
        if (stockObj instanceof Number) {
            request.setStock(((Number) stockObj).intValue());
        } else if (stockObj instanceof String) {
            try {
                request.setStock(Integer.parseInt((String) stockObj));
            } catch (NumberFormatException e) {
                // 保持为 null
            }
        }

        request.setDescription((String) requestBody.get("description"));
        request.setOrigin((String) requestBody.get("origin"));
        request.setPhone((String) requestBody.get("phone"));
        request.setCategory((String) requestBody.get("category"));

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

    // 添加解析批量操作请求的方法
    private ProductBatchActionRequestDTO parseProductBatchActionRequest(Map<String, Object> requestBody) {
        ProductBatchActionRequestDTO request = new ProductBatchActionRequestDTO();

        request.setAction((String) requestBody.get("action"));
        request.setPhone((String) requestBody.get("phone"));

        // 处理 product_ids 字段
        if (requestBody.get("product_ids") instanceof List) {
            List<?> idsObj = (List<?>) requestBody.get("product_ids");
            List<String> productIds = new ArrayList<>();
            for (Object id : idsObj) {
                if (id instanceof String) {
                    productIds.add((String) id);
                }
            }
            request.setProduct_ids(productIds);
        }

        return request;
    }

}
