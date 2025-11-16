// src/config/RouterConfig.java
package config;

import controller.AuthController;
import controller.ProductController;
import controller.ContentController;
import controller.CommentController;
import controller.OrderController;
import dto.auth.*;
import dto.farmer.FarmerRegisterRequestDTO;
import dto.farmer.ProductBatchActionRequestDTO;
import dto.farmer.ProductCreateRequestDTO;
import dto.farmer.ProductStatusUpdateRequestDTO;
import dto.farmer.ProductUpdateRequestDTO;
import dto.community.*;
import dto.buyer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterConfig {
    private AuthController authController;
    private ProductController productController;
    private ContentController contentController;
    private CommentController commentController;
    private OrderController orderController;

    public RouterConfig() {
        this.authController = new AuthController();
        this.productController = new ProductController();
        this.contentController = new ContentController();
        this.commentController = new CommentController();
        this.orderController = new OrderController();
    }

    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody, Map<String, String> headers, Map<String, String> queryParams) {
        // ============= 买家订单相关路由 =============
        
        // 创建订单 - /api/v1/buyer/orders
        if ("/api/v1/buyer/orders".equals(path) && "POST".equals(method)) {
            CreateOrderRequestDTO request = parseCreateOrderRequest(requestBody);
            return orderController.createOrder(request);
        }
        
        // 更新订单信息 - /api/v1/buyer/orders/{order_id}
        Pattern updateOrderPattern = Pattern.compile("/api/v1/buyer/orders/([^/]+)");
        Matcher updateOrderMatcher = updateOrderPattern.matcher(path);
        if (updateOrderMatcher.matches() && "PUT".equals(method)) {
            String orderId = updateOrderMatcher.group(1);
            UpdateOrderRequestDTO request = parseUpdateOrderRequest(requestBody);
            return orderController.updateOrder(orderId, request);
        }
        
        // 获取订单详情 - /api/v1/buyer/orders/query/{order_id}
        Pattern getOrderDetailPattern = Pattern.compile("/api/v1/buyer/orders/query/([^/]+)");
        Matcher getOrderDetailMatcher = getOrderDetailPattern.matcher(path);
        if (getOrderDetailMatcher.matches() && "POST".equals(method)) {
            String orderId = getOrderDetailMatcher.group(1);
            QueryOrderRequestDTO request = parseQueryOrderRequest(requestBody);
            return orderController.getOrderDetail(orderId, request);
        }
        
        // 获取订单列表 - /api/v1/buyer/orders/list_query
        if ("/api/v1/buyer/orders/list_query".equals(path) && "POST".equals(method)) {
            String buyerPhone = queryParams.get("buyer_phone");
            String status = queryParams.get("status");
            String title = queryParams.get("title");
            return orderController.getOrderList(buyerPhone, status, title);
        }
        
        // ============= 农户订单相关路由 =============
        
        // 获取农户订单列表 - /api/v1/farmer/orders/list_query
        if ("/api/v1/farmer/orders/list_query".equals(path) && "POST".equals(method)) {
            String farmerPhone = queryParams.get("farmer_phone");
            String status = queryParams.get("status");
            String title = queryParams.get("title");
            return orderController.getFarmerOrderList(farmerPhone, status, title);
        }
        
        // 获取农户订单详情 - /api/v1/farmer/orders/query/{order_id}
        Pattern getFarmerOrderDetailPattern = Pattern.compile("/api/v1/farmer/orders/query/([^/]+)");
        Matcher getFarmerOrderDetailMatcher = getFarmerOrderDetailPattern.matcher(path);
        if (getFarmerOrderDetailMatcher.matches() && "POST".equals(method)) {
            String orderId = getFarmerOrderDetailMatcher.group(1);
            String farmerPhone = null;
            if (requestBody != null) {
                farmerPhone = (String) requestBody.get("farmer_phone");
            }
            // 如果没有从请求体中获取到，尝试从查询参数获取
            if (farmerPhone == null && queryParams != null) {
                farmerPhone = queryParams.get("farmer_phone");
            }
            return orderController.getFarmerOrderDetail(orderId, farmerPhone);
        }
        
        // 申请退货退款 - /api/v1/buyer/orders/{order_id}/refund
        Pattern refundOrderPattern = Pattern.compile("/api/v1/buyer/orders/([^/]+)/refund");
        Matcher refundOrderMatcher = refundOrderPattern.matcher(path);
        if (refundOrderMatcher.matches() && "POST".equals(method)) {
            String orderId = refundOrderMatcher.group(1);
            RefundRequestDTO request = parseRefundRequest(requestBody);
            return orderController.applyRefund(orderId, request);
        }
        
        // 确认收货 - /api/v1/buyer/orders/{order_id}/confirm_receipt
        Pattern confirmReceiptPattern = Pattern.compile("/api/v1/buyer/orders/([^/]+)/confirm_receipt");
        Matcher confirmReceiptMatcher = confirmReceiptPattern.matcher(path);
        if (confirmReceiptMatcher.matches() && "POST".equals(method)) {
            String orderId = confirmReceiptMatcher.group(1);
            ConfirmReceiptRequestDTO request = parseConfirmReceiptRequest(requestBody);
            return orderController.confirmReceipt(orderId, request);
        }
        
        // ============= 社区相关路由 =============
        
        // 处理发布内容请求
        if ("/api/v1/content/publish".equals(path) && "POST".equals(method)) {
            return contentController.publishContent(parsePublishContentRequest(requestBody));
        }
        
        // 处理获取内容列表请求
        if ("/api/v1/content/list".equals(path) && "GET".equals(method)) {
            // 从查询参数中获取筛选条件
            String contentType = queryParams != null ? queryParams.get("content_type") : null;
            String keyword = queryParams != null ? queryParams.get("keyword") : null;
            String sort = queryParams != null ? queryParams.get("sort") : null;
            return contentController.getContentList(contentType, keyword, sort);
        }
        
        // 处理获取内容详情请求 - /api/v1/content/{content_id}
        Pattern contentDetailPattern = Pattern.compile("/api/v1/content/([^/]+)");
        Matcher contentDetailMatcher = contentDetailPattern.matcher(path);
        if (contentDetailMatcher.matches() && "GET".equals(method)) {
            String contentId = contentDetailMatcher.group(1);
            // 确保不是 /api/v1/content/list 或 /api/v1/content/publish
            if (!contentId.equals("list") && !contentId.equals("publish")) {
                return contentController.getContentDetail(contentId);
            }
        }
        
        // 处理发表评论请求 - /api/v1/content/{content_id}/comments
        Pattern postCommentPattern = Pattern.compile("/api/v1/content/([^/]+)/comments");
        Matcher postCommentMatcher = postCommentPattern.matcher(path);
        if (postCommentMatcher.matches() && "POST".equals(method)) {
            String contentId = postCommentMatcher.group(1);
            return commentController.postComment(contentId, parsePostCommentRequest(requestBody));
        }
        
        // 处理获取评论列表请求 - /api/v1/content/{content_id}/comments
        if (postCommentPattern.matcher(path).matches() && "GET".equals(method)) {
            Matcher matcher = postCommentPattern.matcher(path);
            if (matcher.matches()) {
                String contentId = matcher.group(1);
                return commentController.getCommentList(contentId);
            }
        }
        
        // 处理回复评论请求 - /api/v1/comment/{comment_id}/replies
        Pattern postReplyPattern = Pattern.compile("/api/v1/comment/([^/]+)/replies");
        Matcher postReplyMatcher = postReplyPattern.matcher(path);
        if (postReplyMatcher.matches() && "POST".equals(method)) {
            String commentId = postReplyMatcher.group(1);
            return commentController.postReply(commentId, parsePostReplyRequest(requestBody));
        }
        
        // ============= 商品相关路由 =============
        
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

        // 获取用户余额 - /api/v1/auth/balance
        if ("/api/v1/auth/balance".equals(path) && "GET".equals(method)) {
            String phone = queryParams != null ? queryParams.get("phone") : null;
            String userType = queryParams != null ? queryParams.get("user_type") : null;
            if (phone == null && requestBody != null) {
                phone = (String) requestBody.get("phone");
            }
            if (userType == null && requestBody != null) {
                userType = (String) requestBody.get("user_type");
            }
            return authController.getBalance(phone, userType);
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
        return handleRequest(path, method, requestBody, new HashMap<>(), new HashMap<>());
    }

    // 重载方法以保持向后兼容
    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody, Map<String, String> headers) {
        return handleRequest(path, method, requestBody, headers, new HashMap<>());
    }
    
    // 旧的sessionId版本兼容
    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody, Map<String, String> headers, String sessionId) {
        return handleRequest(path, method, requestBody, headers, new HashMap<>());
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
        request.setDetailedDescription((String) requestBody.get("detailed_description"));

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
        request.setDetailedDescription((String) requestBody.get("detailed_description"));

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

    // ============= 社区相关DTO解析方法 =============
    
    private PublishContentRequestDTO parsePublishContentRequest(Map<String, Object> requestBody) {
        PublishContentRequestDTO request = new PublishContentRequestDTO();
        request.setTitle((String) requestBody.get("title"));
        request.setContent((String) requestBody.get("content"));
        request.setContentType((String) requestBody.get("content_type"));
        request.setPhone((String) requestBody.get("phone"));
        
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
    
    private PostCommentRequestDTO parsePostCommentRequest(Map<String, Object> requestBody) {
        PostCommentRequestDTO request = new PostCommentRequestDTO();
        request.setComment((String) requestBody.get("comment"));
        request.setPhone((String) requestBody.get("phone"));
        return request;
    }
    
    private PostReplyRequestDTO parsePostReplyRequest(Map<String, Object> requestBody) {
        PostReplyRequestDTO request = new PostReplyRequestDTO();
        request.setComment((String) requestBody.get("comment"));
        request.setPhone((String) requestBody.get("phone"));
        return request;
    }
    
    // ============= 订单相关DTO解析方法 =============
    
    private CreateOrderRequestDTO parseCreateOrderRequest(Map<String, Object> requestBody) {
        CreateOrderRequestDTO request = new CreateOrderRequestDTO();
        request.setProductId((String) requestBody.get("product_id"));
        
        Object quantityObj = requestBody.get("quantity");
        if (quantityObj instanceof Number) {
            request.setQuantity(((Number) quantityObj).intValue());
        }
        
        request.setBuyerName((String) requestBody.get("buyer_name"));
        request.setBuyerAddress((String) requestBody.get("buyer_address"));
        request.setBuyerPhone((String) requestBody.get("buyer_phone"));
        request.setRemark((String) requestBody.get("remark"));
        
        return request;
    }
    
    private UpdateOrderRequestDTO parseUpdateOrderRequest(Map<String, Object> requestBody) {
        UpdateOrderRequestDTO request = new UpdateOrderRequestDTO();
        request.setBuyerName((String) requestBody.get("buyer_name"));
        request.setBuyerAddress((String) requestBody.get("buyer_address"));
        request.setBuyerPhone((String) requestBody.get("buyer_phone"));
        request.setRemark((String) requestBody.get("remark"));
        return request;
    }
    
    private QueryOrderRequestDTO parseQueryOrderRequest(Map<String, Object> requestBody) {
        QueryOrderRequestDTO request = new QueryOrderRequestDTO();
        request.setBuyerPhone((String) requestBody.get("buyer_phone"));
        return request;
    }
    
    private RefundRequestDTO parseRefundRequest(Map<String, Object> requestBody) {
        RefundRequestDTO request = new RefundRequestDTO();
        request.setRefundReason((String) requestBody.get("refund_reason"));
        request.setRefundType((String) requestBody.get("refund_type"));
        request.setBuyerPhone((String) requestBody.get("buyer_phone"));
        return request;
    }
    
    private ConfirmReceiptRequestDTO parseConfirmReceiptRequest(Map<String, Object> requestBody) {
        ConfirmReceiptRequestDTO request = new ConfirmReceiptRequestDTO();
        request.setBuyerPhone((String) requestBody.get("buyer_phone"));
        return request;
    }

}
