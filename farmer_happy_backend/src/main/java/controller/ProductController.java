// src/controller/ProductController.java
package controller;

import dto.farmer.*;
import service.farmer.ProductService;
import service.farmer.ProductServiceImpl;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.sql.SQLException;
import java.util.*;

public class ProductController {
    private ProductService productService;
    private AuthService authService;

    public ProductController() {
        this.productService = new ProductServiceImpl();
        this.authService = new AuthServiceImpl();
    }

    public ProductController(ProductService productService, AuthService authService) {
        this.productService = productService;
        this.authService = authService;
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

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    response.put("code", 403);
                    response.put("message", "只有农户可以发布商品");
                    return response;
                }
            } catch (SQLException e) {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
                return response;
            }

            // 创建产品
            ProductResponseDTO product = productService.createProduct(request, user.getUid());

            response.put("code", 201);
            response.put("message", "商品发布成功");
            response.put("data", product);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 商品上架
    public Map<String, Object> onShelfProduct(String productId, ProductStatusUpdateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("message", "手机号不能为空");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            ProductStatusUpdateResponseDTO result = productService.onShelfProduct(productId, request.getPhone());

            response.put("code", 200);
            response.put("message", "商品上架成功");
            response.put("data", result);

            return response;
        } catch (SQLException e) {
            if (e.getMessage().contains("商品不存在")) {
                response.put("code", 404);
                response.put("message", "商品不存在");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
            }
            return response;
        } catch (IllegalStateException e) {
            response.put("code", 409);
            response.put("message", e.getMessage());
            return response;
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 商品下架
    public Map<String, Object> offShelfProduct(String productId, ProductStatusUpdateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("message", "手机号不能为空");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            ProductStatusUpdateResponseDTO result = productService.offShelfProduct(productId, request.getPhone());

            response.put("code", 200);
            response.put("message", "商品下架成功");
            response.put("data", result);

            return response;
        } catch (SQLException e) {
            if (e.getMessage().contains("商品不存在")) {
                response.put("code", 404);
                response.put("message", "商品不存在");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
            }
            return response;
        } catch (IllegalStateException e) {
            response.put("code", 409);
            response.put("message", e.getMessage());
            return response;
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 删除商品
    public Map<String, Object> deleteProduct(String productId, ProductStatusUpdateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("message", "手机号不能为空");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            // 删除商品
            productService.deleteProduct(productId, request.getPhone());

            // 返回204状态码
            response.put("code", 204);
            response.put("message", "商品删除成功");

            return response;
        } catch (SQLException e) {
            if (e.getMessage().contains("商品不存在")) {
                response.put("code", 404);
                response.put("message", "商品不存在");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
            }
            return response;
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 获取单个商品详情
    public Map<String, Object> getProductDetail(String productId, ProductStatusUpdateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("message", "手机号不能为空");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            // 获取商品详情
            ProductDetailResponseDTO productDetail = productService.getProductDetail(productId, request.getPhone());

            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", productDetail);

            return response;
        } catch (SQLException e) {
            if (e.getMessage().contains("商品不存在")) {
                response.put("code", 404);
                response.put("message", "商品不存在");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
            }
            return response;
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 获取商品列表
    public Map<String, Object> getProductList(Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            String phone = (String) requestBody.get("phone");
            if (phone == null || phone.isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("field", "phone");
                errorDetail.put("message", "手机号错误");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            // 验证用户
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                List<Map<String, String>> errorDetails = new ArrayList<>();
                Map<String, String> errorDetail = new HashMap<>();
                errorDetail.put("field", "phone");
                errorDetail.put("message", "手机号错误");
                errorDetails.add(errorDetail);
                response.put("errors", errorDetails);
                return response;
            }

            // 获取商品列表，注意这里传入的是手机号而不是user.getUid()
            // 允许买家和农户都能查看商品列表
            String status = (String) requestBody.get("status");
            String title = (String) requestBody.get("title");

            List<ProductListResponseDTO> productList = productService.getProductList(phone, status, title);

            response.put("code", 200);
            response.put("message", "成功");

            Map<String, Object> data = new HashMap<>();
            data.put("list", productList);
            response.put("data", data);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 更新商品
    public Map<String, Object> updateProduct(String productId, ProductUpdateRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            List<String> errors = validateProductRequestForUpdate(request);
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

            // 验证手机号
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

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    response.put("code", 403);
                    response.put("message", "只有农户可以修改商品");
                    return response;
                }
            } catch (SQLException e) {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
                return response;
            }

            // 更新产品 - 使用部分更新方法
            ProductResponseDTO product = productService.partialUpdateProduct(productId, request);

            response.put("code", 200);
            response.put("message", "商品更新成功");
            response.put("data", product);

            return response;
        } catch (SQLException e) {
            if (e.getMessage().contains("商品不存在")) {
                response.put("code", 404);
                response.put("message", "商品不存在");
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
            }
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

        if (request.getDetailedDescription() == null || request.getDetailedDescription().isEmpty()) {
            errors.add("商品详细介绍不能为空");
        }

        if (request.getPrice() == null || request.getPrice() <= 0) {
            errors.add("价格必须是大于0的数字");
        }

        if (request.getStock() == null || request.getStock() < 0) {
            errors.add("库存数量必须大于等于0");
        }

        if (request.getDescription() != null && request.getDescription().length() > 5000) {
            errors.add("商品描述长度不能超过5000个字符");
        }

        if (request.getImages() != null && request.getImages().size() > 9) {
            errors.add("最多支持9张图片");
        }

        // 验证分类
        if (request.getCategory() == null || request.getCategory().isEmpty()) {
            errors.add("商品分类不能为空");
        } else {
            Set<String> validCategories = new HashSet<>(Arrays.asList("vegetables", "fruits", "grains"));
            if (!validCategories.contains(request.getCategory())) {
                errors.add("商品分类无效，必须是以下值之一: vegetables, fruits, grains");
            }
        }

        return errors;
    }

    private List<String> validateProductRequestForUpdate(ProductUpdateRequestDTO request) {
        List<String> errors = new ArrayList<>();

        if (request.getTitle() != null && request.getTitle().isEmpty()) {
            errors.add("商品标题不能为空");
        } else if (request.getTitle() != null && request.getTitle().length() > 100) {
            errors.add("商品标题长度不能超过100个字符");
        }

        if (request.getDetailedDescription() != null && request.getDetailedDescription().isEmpty()) {
            errors.add("商品详细介绍不能为空");
        }

        if (request.getPrice() != null && request.getPrice() <= 0) {
            errors.add("价格必须是大于0的数字");
        }

        if (request.getStock() != null && request.getStock() < 0) {
            errors.add("库存数量必须大于等于0");
        }

        if (request.getDescription() != null && request.getDescription().length() > 5000) {
            errors.add("商品描述长度不能超过5000个字符");
        }

        if (request.getImages() != null && request.getImages().size() > 9) {
            errors.add("最多支持9张图片");
        }

        // 验证分类
        if (request.getCategory() != null && request.getCategory().isEmpty()) {
            errors.add("商品分类不能为空");
        } else if (request.getCategory() != null) {
            Set<String> validCategories = new HashSet<>(
                    Arrays.asList("vegetables", "fruits", "grains", "livestock", "aquatic"));
            if (!validCategories.contains(request.getCategory())) {
                errors.add("商品分类无效，必须是以下值之一: vegetables, fruits, grains, livestock, aquatic");
            }
        }

        return errors;
    }

    // 批量操作商品
    public Map<String, Object> batchActionProducts(ProductBatchActionRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证参数
            List<String> errors = validateBatchActionRequest(request);
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

            // 验证手机号
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

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    response.put("code", 403);
                    response.put("message", "只有农户可以执行商品操作");
                    return response;
                }
            } catch (SQLException e) {
                response.put("code", 500);
                response.put("message", "服务器内部错误: " + e.getMessage());
                return response;
            }

            // 执行批量操作
            ProductBatchActionResultDTO result = productService.batchActionProducts(request);

            response.put("code", 200);
            response.put("message", "批量操作处理完成");
            Map<String, Object> data = new HashMap<>();
            data.put("success_count", result.getSuccess_count());
            data.put("failure_count", result.getFailure_count());
            data.put("results", result.getResults());
            response.put("data", data);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

    // 验证批量操作请求参数
    private List<String> validateBatchActionRequest(ProductBatchActionRequestDTO request) {
        List<String> errors = new ArrayList<>();

        if (request.getAction() == null || request.getAction().isEmpty()) {
            errors.add("操作类型不能为空");
        } else {
            String normalized = request.getAction().replace('-', '_');
            if (!"on_shelf".equals(normalized) &&
                    !"off_shelf".equals(normalized) &&
                    !"delete".equals(normalized)) {
                errors.add("无效的操作类型");
            } else {
                request.setAction(normalized);
            }
        }

        if (request.getProduct_ids() == null) {
            errors.add("商品ID列表不能为空");
        } else if (request.getProduct_ids().isEmpty()) {
            errors.add("商品ID列表不能为空");
        } else if (request.getProduct_ids().size() > 100) {
            errors.add("一次最多只能操作100个商品");
        }

        return errors;
    }

    // 获取所有在售商品（用于广告）
    public Map<String, Object> getAllOnShelfProducts() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取所有在售商品
            List<ProductListResponseDTO> productList = productService.getAllOnShelfProducts();

            response.put("code", 200);
            response.put("message", "成功");

            Map<String, Object> data = new HashMap<>();
            data.put("list", productList);
            response.put("data", data);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return response;
        }
    }

}
