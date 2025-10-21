// src/controller/ProductController.java
package controller;

import dto.ProductCreateRequestDTO;
import dto.ProductResponseDTO;
import service.farmer.ProductService;
import service.farmer.ProductServiceImpl;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.util.*;

public class ProductController {
    private ProductService productService;
    private AuthService authService;

    public ProductController() {
        this.productService = new ProductServiceImpl();
        this.authService = new AuthServiceImpl();
    }

    public Map<String, Object> createProduct(ProductCreateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            List<String> errors = validateProductRequest(request);
            if (!errors.isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");

                List<Map<String, String>> errorDetails = new ArrayList<>();
                for (String error : errors) {
                    Map<String, String> errorDetail = new HashMap<>();
                    errorDetail.put("message", error);
                    errorDetails.add(errorDetail);
                }
                response.put("errors", errorDetails);
                return response;
            }

            // 验证手机号并获取用户ID
            String phone = request.getPhone();
            if (phone == null || phone.isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");

                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("message", "手机号不能为空");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            // 查找用户
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                response.put("code", 400);
                response.put("message", "参数验证失败");

                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("message", "用户不存在");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            // 验证用户类型是否为农户
            if (!"farmer".equals(user.getUserType())) {
                response.put("code", 403);
                response.put("message", "只有农户可以发布商品");
                return response;
            }

            // 创建产品
            ProductResponseDTO product = productService.createProduct(request, user.getUid());

            response.put("code", 201);
            response.put("message", "商品发布成功，等待审核");
            response.put("data", product);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    private List<String> validateProductRequest(ProductCreateRequestDTO request) {
        List<String> errors = new ArrayList<>();

        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            errors.add("商品标题不能为空");
        } else if (request.getTitle().length() > 100) {
            errors.add("商品标题长度不能超过100个字符");
        }

        if (request.getSpecification() == null || request.getSpecification().isEmpty()) {
            errors.add("商品规格描述不能为空");
        }

        if (request.getPrice() <= 0) {
            errors.add("价格必须是大于0的数字");
        }

        if (request.getStock() < 0) {
            errors.add("库存数量必须大于等于0");
        }

        if (request.getDescription() != null && request.getDescription().length() > 5000) {
            errors.add("商品描述长度不能超过5000个字符");
        }

        if (request.getImages() != null && request.getImages().size() > 9) {
            errors.add("最多支持9张图片");
        }

        return errors;
    }
}
