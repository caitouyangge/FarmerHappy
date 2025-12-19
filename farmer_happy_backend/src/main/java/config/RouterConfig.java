// src/config/RouterConfig.java
package config;

import controller.AiController;
import controller.AuthController;
import controller.CommentController;
import controller.ContentController;
import controller.FinancingController;
import controller.OrderController;
import controller.PriceCrawlerController;
import controller.PricePredictionController;
import controller.ProductController;
import dto.auth.*;
import dto.bank.*;
import dto.buyer.*;
import dto.community.*;
import dto.farmer.*;
import dto.financing.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterConfig {
    private AuthController authController;
    private ProductController productController;
    private ContentController contentController;
    private CommentController commentController;
    private OrderController orderController;
    private FinancingController financingController;
    private AiController aiController;
    private controller.ExpertAppointmentController expertAppointmentController;
    private PricePredictionController pricePredictionController;
    private PriceCrawlerController priceCrawlerController;


    public RouterConfig() {
        this.authController = new AuthController();
        this.productController = new ProductController();
        this.contentController = new ContentController();
        this.commentController = new CommentController();
        this.orderController = new OrderController();
        this.financingController = new FinancingController();
        this.aiController = new AiController();
        this.pricePredictionController = new PricePredictionController();
        this.priceCrawlerController = new PriceCrawlerController();
        this.expertAppointmentController = new controller.ExpertAppointmentController();
    }

    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody,
            Map<String, String> headers, Map<String, String> queryParams) {

        // ============= 农产品价格爬虫相关路由 =============

        // 获取爬虫数据
        if ("/api/v1/agriculture/price".equals(path) && "POST".equals(method)) {
            return priceCrawlerController.crawlAgriculturalPrices(requestBody);
        }

        // 获取 split 文件列表（动态勾选品种）
        if ("/api/v1/agriculture/price/split/list".equals(path) && "GET".equals(method)) {
            return priceCrawlerController.listSplitFiles(queryParams);
        }

        // 用户勾选品种/文件并选择位置后放置
        if ("/api/v1/agriculture/price/split/place".equals(path) && "POST".equals(method)) {
            return priceCrawlerController.placeSplitFiles(requestBody);
        }

        // 将 split 下的 CSV 批量导出为 XLSX（不需要重新爬取）
        if ("/api/v1/agriculture/price/split/export_xlsx".equals(path) && "POST".equals(method)) {
            return priceCrawlerController.exportSplitXlsx(requestBody);
        }
        // ============= AI 农业专家相关路由 =============

        // 与 AI 农业专家对话
        if ("/api/v1/ai/expert-chat".equals(path) && "POST".equals(method)) {
            String question = requestBody != null ? (String) requestBody.get("question") : null;
            return aiController.chatWithAiExpert(question);
        }

        // ============= 专家预约相关路由 =============

        // 获取专家列表
        if ("/api/v1/expert/experts/list".equals(path) && "POST".equals(method)) {
            return expertAppointmentController.listExperts();
        }

        // 农户发起预约
        if ("/api/v1/expert/appointments/apply".equals(path) && "POST".equals(method)) {
            dto.expert.AppointmentCreateRequestDTO req = parseAppointmentCreateRequest(requestBody);
            return expertAppointmentController.apply(req);
        }

        // 专家查看预约请求
        if ("/api/v1/expert/appointments/expert/list".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return expertAppointmentController.getExpertAppointments(phone);
        }

        // 农户查看预约结果
        if ("/api/v1/expert/appointments/farmer/list".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return expertAppointmentController.getFarmerAppointments(phone);
        }

        // 获取预约详情 - /api/v1/expert/appointments/query/{appointment_id}
        Pattern apptDetailPattern = Pattern.compile("/api/v1/expert/appointments/query/([^/]+)");
        Matcher apptDetailMatcher = apptDetailPattern.matcher(path);
        if (apptDetailMatcher.matches() && "POST".equals(method)) {
            String appointmentId = apptDetailMatcher.group(1);
            return expertAppointmentController.getDetail(appointmentId);
        }

        // 专家处理预约 - /api/v1/expert/appointments/{appointment_id}/decision
        Pattern apptDecisionPattern = Pattern.compile("/api/v1/expert/appointments/([^/]+)/decision");
        Matcher apptDecisionMatcher = apptDecisionPattern.matcher(path);
        if (apptDecisionMatcher.matches() && "POST".equals(method)) {
            String appointmentId = apptDecisionMatcher.group(1);
            dto.expert.AppointmentDecisionRequestDTO req = parseAppointmentDecisionRequest(requestBody);
            return expertAppointmentController.decide(appointmentId, req);
        }

        // ============= 融资相关路由 =============

        // 在处理请求的方法中添加
        if ("/api/v1/financing/repayment".equals(path) && "POST".equals(method)) {
            return financingController.makeRepayment(requestBody, headers);
        }


        // 获取还款列表
        if ("/api/v1/financing/repayment/schedule".equals(path) && "POST".equals(method)) {
            return financingController.getRepaymentSchedule(requestBody, headers);
        }
        // 银行审批贷款申请
        if ("/api/v1/bank/loans/approve".equals(path) && "POST".equals(method)) {
            LoanApprovalRequestDTO request = parseLoanApprovalRequest(requestBody);
            return financingController.approveLoan(request);
        }

        // 银行放款操作
        if ("/api/v1/bank/loans/disburse".equals(path) && "POST".equals(method)) {
            LoanDisbursementRequestDTO request = parseLoanDisbursementRequest(requestBody);
            return financingController.disburseLoan(request);
        }

        // 银行审批信贷额度申请
        if ("/api/v1/bank/credit/approve".equals(path) && "POST".equals(method)) {
            CreditApprovalRequestDTO request = parseCreditApprovalRequest(requestBody);
            return financingController.approveCreditApplication(request);
        }

        // 获取待审批的信贷额度申请列表
        if ("/api/v1/bank/credit/pending".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return financingController.getPendingCreditApplications(phone);
        }

        // 获取待审批的贷款申请列表
        if ("/api/v1/bank/loans/pending".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return financingController.getPendingLoanApplications(phone);
        }

        // 获取已审批待放款的贷款申请列表
        if ("/api/v1/bank/loans/approved".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return financingController.getApprovedLoanApplications(phone);
        }

        // 查询可用贷款额度
        if ("/api/v1/financing/credit/limit".equals(path) && "POST".equals(method)) {
            CreditLimitRequestDTO request = parseCreditLimitRequest(requestBody);
            return financingController.getCreditLimit(request);
        }

        // 申请贷款额度
        if ("/api/v1/financing/credit/apply".equals(path) && "POST".equals(method)) {
            CreditApplicationRequestDTO request = parseCreditApplicationRequest(requestBody);
            return financingController.applyForCreditLimit(request);
        }

        // 获取农户申请记录
        if ("/api/v1/financing/credit/applications".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return financingController.getFarmerCreditApplications(phone);
        }

        // 获取农户已放款的贷款列表
        if ("/api/v1/financing/loans/list".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return financingController.getFarmerLoans(phone);
        }

        // 获取农户贷款申请记录
        if ("/api/v1/financing/loans/applications".equals(path) && "POST".equals(method)) {
            String phone = (String) requestBody.get("phone");
            return financingController.getFarmerLoanApplications(phone);
        }

        // 查询可申请的贷款产品
        if ("/api/v1/financing/loans/products".equals(path) && "POST".equals(method)) {
            LoanProductsRequestDTO request = parseLoanProductsRequest(requestBody);
            return financingController.getAvailableLoanProducts(request);
        }

        // 银行发布贷款产品
        if ("/api/v1/bank/loans/products".equals(path) && "POST".equals(method)) {
            BankLoanProductRequestDTO request = parseBankLoanProductRequest(requestBody);
            return financingController.publishLoanProduct(request);
        }

        // 申请单人贷款
        if ("/api/v1/financing/loans/single".equals(path) && "POST".equals(method)) {
            SingleLoanApplicationRequestDTO request = parseSingleLoanApplicationRequest(requestBody);
            return financingController.applyForSingleLoan(request);
        }

        // 申请联合贷款
        if ("/api/v1/financing/loans/joint".equals(path) && "POST".equals(method)) {
            JointLoanApplicationRequestDTO request = parseJointLoanApplicationRequest(requestBody);
            return financingController.applyForJointLoan(request);
        }

        // 浏览可联合农户
        if ("/api/v1/financing/partners".equals(path) && "POST".equals(method)) {
            PartnersRequestDTO request = parsePartnersRequest(requestBody);
            return financingController.getJointPartners(request);
        }

        // 智能贷款推荐
        if ("/api/v1/financing/smart-recommendation".equals(path) && "POST".equals(method)) {
            SmartLoanRecommendationRequestDTO request = parseSmartLoanRecommendationRequest(requestBody);
            return financingController.getSmartLoanRecommendation(request);
        }

        // 联合贷款伙伴确认
        if ("/api/v1/financing/joint-loan-confirmation".equals(path) && "POST".equals(method)) {
            JointLoanPartnerConfirmationRequestDTO request = parseJointLoanPartnerConfirmationRequest(requestBody);
            return financingController.confirmJointLoanApplication(request);
        }

        // 获取待确认的联合贷款申请
        if ("/api/v1/financing/pending-joint-loans".equals(path) && "POST".equals(method)) {
            PendingJointLoanApplicationsRequestDTO request = parsePendingJointLoanApplicationsRequest(requestBody);
            return financingController.getPendingJointLoanApplications(request);
        }

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

        // 处理获取所有在售商品请求（用于广告）
        if ("/api/v1/farmer/products/on-shelf/all".equals(path) && "GET".equals(method)) {
            return productController.getAllOnShelfProducts();
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

        if ("/api/v1/storage/upload".equals(path) && "POST".equals(method)) {
            return handleImageUpload(requestBody);
        }

        // ============= 价格预测相关路由 =============

        // 上传Excel文件
        if ("/api/v1/farmer/price-prediction/upload".equals(path) && "POST".equals(method)) {
            return handleExcelUpload(requestBody);
        }

        // 预测价格
        if ("/api/v1/farmer/price-prediction/predict".equals(path) && "POST".equals(method)) {
            PricePredictionRequestDTO request = parsePricePredictionRequest(requestBody);
            return pricePredictionController.predictPrice(request);
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
    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody,
            Map<String, String> headers) {
        return handleRequest(path, method, requestBody, headers, new HashMap<>());
    }

    // 旧的sessionId版本兼容
    public Map<String, Object> handleRequest(String path, String method, Map<String, Object> requestBody,
            Map<String, String> headers, String sessionId) {
        return handleRequest(path, method, requestBody, headers, new HashMap<>());
    }

    private Map<String, Object> handleImageUpload(Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        Object imagesObj = requestBody != null ? requestBody.get("images") : null;
        if (!(imagesObj instanceof List)) {
            response.put("code", 400);
            response.put("message", "参数验证失败");
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "images");
            error.put("message", "images 必须为数组");
            errors.add(error);
            response.put("errors", errors);
            return response;
        }

        List<?> images = (List<?>) imagesObj;
        List<String> urls = new ArrayList<>();
        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            for (Object obj : images) {
                if (!(obj instanceof String)) {
                    continue;
                }
                String dataUrl = (String) obj;
                String prefix;
                String base64Part;
                int commaIndex = dataUrl.indexOf(',');
                if (commaIndex > 0) {
                    prefix = dataUrl.substring(0, commaIndex);
                    base64Part = dataUrl.substring(commaIndex + 1);
                } else {
                    prefix = "";
                    base64Part = dataUrl;
                }

                String ext;
                if (prefix.contains("image/png")) {
                    ext = ".png";
                } else if (prefix.contains("image/jpeg") || prefix.contains("image/jpg")) {
                    ext = ".jpg";
                } else if (prefix.contains("image/gif")) {
                    ext = ".gif";
                } else {
                    ext = ".bin";
                }

                byte[] bytes = Base64.getDecoder().decode(base64Part);
                String fileName = UUID.randomUUID().toString() + ext;
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, bytes);
                urls.add("http://localhost:8080/uploads/" + fileName);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("urls", urls);
            response.put("code", 201);
            response.put("message", "上传成功");
            response.put("data", data);
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            return response;
        }
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

    /**
     * 解析贷款审批请求
     */
    private LoanApprovalRequestDTO parseLoanApprovalRequest(Map<String, Object> requestBody) {
        LoanApprovalRequestDTO request = new LoanApprovalRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setApplication_id((String) requestBody.get("application_id"));
        request.setAction((String) requestBody.get("action"));
        request.setReject_reason((String) requestBody.get("reject_reason"));

        // 处理数值类型字段
        if (requestBody.get("approved_amount") instanceof Number) {
            request.setApproved_amount(BigDecimal.valueOf(((Number) requestBody.get("approved_amount")).doubleValue()));
        }

        return request;
    }

    /**
     * 解析信贷额度审批请求
     */
    private CreditApprovalRequestDTO parseCreditApprovalRequest(Map<String, Object> requestBody) {
        CreditApprovalRequestDTO request = new CreditApprovalRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setApplication_id((String) requestBody.get("application_id"));
        request.setAction((String) requestBody.get("action"));
        request.setReject_reason((String) requestBody.get("reject_reason"));

        // 处理数值类型字段
        if (requestBody.get("approved_amount") instanceof Number) {
            request.setApproved_amount(BigDecimal.valueOf(((Number) requestBody.get("approved_amount")).doubleValue()));
        }

        return request;
    }

    /**
     * 解析贷款放款请求
     */
    private LoanDisbursementRequestDTO parseLoanDisbursementRequest(Map<String, Object> requestBody) {
        LoanDisbursementRequestDTO request = new LoanDisbursementRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setApplication_id((String) requestBody.get("application_id"));
        request.setDisburse_method((String) requestBody.get("disburse_method"));
        request.setLoan_account((String) requestBody.get("loan_account"));
        request.setRemarks((String) requestBody.get("remarks"));

        // 处理数值类型字段
        if (requestBody.get("disburse_amount") instanceof Number) {
            request.setDisburse_amount(BigDecimal.valueOf(((Number) requestBody.get("disburse_amount")).doubleValue()));
        }

        // 处理日期字段
        if (requestBody.get("first_repayment_date") instanceof String) {
            try {
                // 将字符串转换为Date对象
                String dateStr = (String) requestBody.get("first_repayment_date");
                // 简化处理，实际项目中应使用适当的日期格式化
                request.setFirst_repayment_date(java.sql.Date.valueOf(dateStr));
            } catch (Exception e) {
                // 日期格式不正确时保持为null
            }
        }

        return request;
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

    private dto.expert.AppointmentCreateRequestDTO parseAppointmentCreateRequest(Map<String, Object> requestBody) {
        dto.expert.AppointmentCreateRequestDTO req = new dto.expert.AppointmentCreateRequestDTO();
        req.setFarmer_phone((String) requestBody.get("farmer_phone"));
        req.setMode((String) requestBody.get("mode"));
        if (requestBody.get("expert_ids") instanceof List) {
            List<?> ids = (List<?>) requestBody.get("expert_ids");
            List<Long> longIds = new ArrayList<>();
            for (Object o : ids) {
                if (o instanceof Number) {
                    longIds.add(((Number) o).longValue());
                } else if (o instanceof String) {
                    try { longIds.add(Long.parseLong((String) o)); } catch (Exception ignored) {}
                }
            }
            req.setExpert_ids(longIds);
        }
        req.setMessage((String) requestBody.get("message"));
        return req;
    }

    private dto.expert.AppointmentDecisionRequestDTO parseAppointmentDecisionRequest(Map<String, Object> requestBody) {
        dto.expert.AppointmentDecisionRequestDTO req = new dto.expert.AppointmentDecisionRequestDTO();
        req.setExpert_phone((String) requestBody.get("expert_phone"));
        req.setAction((String) requestBody.get("action"));
        req.setExpert_note((String) requestBody.get("expert_note"));
        Object tsObj = requestBody.get("scheduled_time");
        if (tsObj instanceof String) {
            try {
                req.setScheduled_time(java.sql.Timestamp.valueOf((String) tsObj));
            } catch (Exception e) {
                req.setScheduled_time(null);
            }
        }
        req.setLocation((String) requestBody.get("location"));
        return req;
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

    // ============= 融资相关DTO解析方法 =============

    private BankLoanProductRequestDTO parseBankLoanProductRequest(Map<String, Object> requestBody) {
        BankLoanProductRequestDTO request = new BankLoanProductRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setProduct_name((String) requestBody.get("product_name"));
        request.setProduct_code((String) requestBody.get("product_code"));
        request.setDescription((String) requestBody.get("description"));
        request.setRepayment_method((String) requestBody.get("repayment_method"));

        // 处理数值类型字段，使用BigDecimal.valueOf()进行转换
        if (requestBody.get("min_credit_limit") instanceof Number) {
            request.setMin_credit_limit(
                    BigDecimal.valueOf(((Number) requestBody.get("min_credit_limit")).doubleValue()));
        }

        if (requestBody.get("max_amount") instanceof Number) {
            request.setMax_amount(BigDecimal.valueOf(((Number) requestBody.get("max_amount")).doubleValue()));
        }

        if (requestBody.get("interest_rate") instanceof Number) {
            request.setInterest_rate(BigDecimal.valueOf(((Number) requestBody.get("interest_rate")).doubleValue()));
        }

        if (requestBody.get("term_months") instanceof Number) {
            request.setTerm_months(((Number) requestBody.get("term_months")).intValue());
        }

        return request;
    }

    private LoanProductsRequestDTO parseLoanProductsRequest(Map<String, Object> requestBody) {
        LoanProductsRequestDTO request = new LoanProductsRequestDTO();
        request.setPhone((String) requestBody.get("phone"));

        // 处理数值类型字段，使用BigDecimal.valueOf()进行转换
        if (requestBody.get("credit_limit") instanceof Number) {
            request.setCredit_limit(BigDecimal.valueOf(((Number) requestBody.get("credit_limit")).doubleValue()));
        }

        return request;
    }

    private CreditLimitRequestDTO parseCreditLimitRequest(Map<String, Object> requestBody) {
        CreditLimitRequestDTO request = new CreditLimitRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        return request;
    }

    private CreditApplicationRequestDTO parseCreditApplicationRequest(Map<String, Object> requestBody) {
        CreditApplicationRequestDTO request = new CreditApplicationRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setProof_type((String) requestBody.get("proof_type"));
        request.setDescription((String) requestBody.get("description"));

        // 处理数值类型字段，使用BigDecimal.valueOf()进行转换
        if (requestBody.get("apply_amount") instanceof Number) {
            request.setApply_amount(BigDecimal.valueOf(((Number) requestBody.get("apply_amount")).doubleValue()));
        }

        // 处理图片数组
        if (requestBody.get("proof_images") instanceof List) {
            List<?> imagesObj = (List<?>) requestBody.get("proof_images");
            List<String> images = new ArrayList<>();
            for (Object img : imagesObj) {
                if (img instanceof String) {
                    images.add((String) img);
                }
            }
            request.setProof_images(images);
        }

        return request;
    }

    private SingleLoanApplicationRequestDTO parseSingleLoanApplicationRequest(Map<String, Object> requestBody) {
        SingleLoanApplicationRequestDTO request = new SingleLoanApplicationRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setProduct_id((String) requestBody.get("product_id"));
        request.setPurpose((String) requestBody.get("purpose"));
        request.setRepayment_source((String) requestBody.get("repayment_source"));

        // 处理数值类型字段
        if (requestBody.get("apply_amount") instanceof Number) {
            request.setApply_amount(BigDecimal.valueOf(((Number) requestBody.get("apply_amount")).doubleValue()));
        }

        return request;
    }

    private JointLoanApplicationRequestDTO parseJointLoanApplicationRequest(Map<String, Object> requestBody) {
        JointLoanApplicationRequestDTO request = new JointLoanApplicationRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setProduct_id((String) requestBody.get("product_id"));
        request.setPurpose((String) requestBody.get("purpose"));
        request.setRepayment_plan((String) requestBody.get("repayment_plan"));
        request.setJoint_agreement((Boolean) requestBody.get("joint_agreement"));

        // 处理数值类型字段
        if (requestBody.get("apply_amount") instanceof Number) {
            request.setApply_amount(BigDecimal.valueOf(((Number) requestBody.get("apply_amount")).doubleValue()));
        }

        // 处理伙伴手机号数组
        if (requestBody.get("partner_phones") instanceof List) {
            List<?> phonesObj = (List<?>) requestBody.get("partner_phones");
            List<String> phones = new ArrayList<>();
            for (Object phone : phonesObj) {
                if (phone instanceof String) {
                    phones.add((String) phone);
                }
            }
            request.setPartner_phones(phones);
        }

        return request;
    }

    private PartnersRequestDTO parsePartnersRequest(Map<String, Object> requestBody) {
        PartnersRequestDTO request = new PartnersRequestDTO();
        request.setPhone((String) requestBody.get("phone"));

        // 处理数值类型字段
        if (requestBody.get("min_credit_limit") instanceof Number) {
            request.setMin_credit_limit(
                    BigDecimal.valueOf(((Number) requestBody.get("min_credit_limit")).doubleValue()));
        }

        if (requestBody.get("max_partners") instanceof Number) {
            request.setMax_partners(((Number) requestBody.get("max_partners")).intValue());
        }

        // 处理排除手机号数组
        if (requestBody.get("exclude_phones") instanceof List) {
            List<?> phonesObj = (List<?>) requestBody.get("exclude_phones");
            List<String> phones = new ArrayList<>();
            for (Object phone : phonesObj) {
                if (phone instanceof String) {
                    phones.add((String) phone);
                }
            }
            request.setExclude_phones(phones);
        }

        return request;
    }

    /**
     * 解析智能贷款推荐请求
     */
    private SmartLoanRecommendationRequestDTO parseSmartLoanRecommendationRequest(Map<String, Object> requestBody) {
        SmartLoanRecommendationRequestDTO request = new SmartLoanRecommendationRequestDTO();
        request.setPhone((String) requestBody.get("phone"));

        // 处理产品ID
        Object productIdObj = requestBody.get("product_id");
        if (productIdObj != null) {
            request.setProduct_id(productIdObj.toString());
        }

        // 处理申请金额
        if (requestBody.get("apply_amount") instanceof Number) {
            request.setApply_amount(
                    BigDecimal.valueOf(((Number) requestBody.get("apply_amount")).doubleValue()));
        }

        return request;
    }

    /**
     * 解析联合贷款伙伴确认请求
     */
    private JointLoanPartnerConfirmationRequestDTO parseJointLoanPartnerConfirmationRequest(Map<String, Object> requestBody) {
        JointLoanPartnerConfirmationRequestDTO request = new JointLoanPartnerConfirmationRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        request.setApplication_id((String) requestBody.get("application_id"));
        request.setAction((String) requestBody.get("action"));
        return request;
    }

    /**
     * 解析获取待确认联合贷款申请请求
     */
    private PendingJointLoanApplicationsRequestDTO parsePendingJointLoanApplicationsRequest(Map<String, Object> requestBody) {
        PendingJointLoanApplicationsRequestDTO request = new PendingJointLoanApplicationsRequestDTO();
        request.setPhone((String) requestBody.get("phone"));
        return request;
    }

    /**
     * 处理价格文件上传（Excel/CSV）
     */
    private Map<String, Object> handleExcelUpload(Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object fileObj = requestBody != null ? requestBody.get("file") : null;
            Object fileNameObj = requestBody != null ? requestBody.get("fileName") : null;
            
            if (!(fileObj instanceof String)) {
                response.put("code", 400);
                response.put("message", "file参数必须为Base64编码的字符串");
                return response;
            }
            
            String fileBase64 = (String) fileObj;
            String fileName = fileNameObj != null ? fileNameObj.toString() : "upload.xlsx";
            
            // 验证文件类型
            String lower = fileName != null ? fileName.toLowerCase() : null;
            if (lower == null || (!lower.endsWith(".xls") && !lower.endsWith(".xlsx") && !lower.endsWith(".csv"))) {
                response.put("code", 400);
                response.put("message", "不支持的文件格式，仅支持.xls、.xlsx、.csv文件");
                return response;
            }
            
            // 解析Base64
            String base64Data = fileBase64;
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            
            byte[] fileBytes = Base64.getDecoder().decode(base64Data);
            
            // 验证文件大小（限制10MB）
            if (fileBytes.length > 10 * 1024 * 1024) {
                response.put("code", 400);
                response.put("message", "文件大小不能超过10MB");
                return response;
            }
            
            // 创建输入流并调用控制器
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            return pricePredictionController.uploadExcel(inputStream, fileName);
            
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

    /**
     * 解析价格预测请求
     */
    private PricePredictionRequestDTO parsePricePredictionRequest(Map<String, Object> requestBody) {
        PricePredictionRequestDTO request = new PricePredictionRequestDTO();
        request.setFileId((String) requestBody.get("file_id"));
        
        Object predictionDaysObj = requestBody.get("prediction_days");
        if (predictionDaysObj instanceof Number) {
            request.setPredictionDays(((Number) predictionDaysObj).intValue());
        }
        
        request.setModelType((String) requestBody.get("model_type"));
        return request;
    }

}
