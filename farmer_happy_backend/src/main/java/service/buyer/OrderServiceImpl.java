package service.buyer;

import dto.buyer.*;
import entity.Order;
import entity.Product;
import exception.ValidationException;
import repository.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订单服务实现类
 */
public class OrderServiceImpl implements OrderService {

    private final DatabaseManager dbManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public OrderServiceImpl() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public OrderServiceImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request) throws Exception {
        // 1. 参数验证
        validateCreateOrderRequest(request);

        // 2. 验证买家是否存在
        String buyerUid = dbManager.getBuyerUidByPhone(request.getBuyerPhone());
        if (buyerUid == null) {
            throw new RuntimeException("买家账户不存在或未启用");
        }

        // 3. 验证商品是否存在
        Long productId = Long.parseLong(request.getProductId());
        Product product = dbManager.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        // 4. 验证商品状态
        if (!"on_shelf".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架，无法购买");
        }

        // 5. 验证库存
        if (product.getStock() == 0) {
            throw new RuntimeException("商品已售罄，当前库存：0");
        }
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("库存不足，当前可用库存：" + product.getStock() + "，请求数量：" + request.getQuantity());
        }

        // 6. 验证购买数量限制
        if (request.getQuantity() > 100) {
            throw new RuntimeException("单次购买数量不能超过100件");
        }

        // 7. 计算订单总金额
        BigDecimal productPrice = new BigDecimal(product.getPrice());
        BigDecimal totalAmount = productPrice.multiply(new BigDecimal(request.getQuantity()));

        // 8. 验证买家余额
        BigDecimal buyerBalance = dbManager.getBuyerBalance(request.getBuyerPhone());
        if (buyerBalance == null || buyerBalance.compareTo(totalAmount) < 0) {
            throw new RuntimeException("余额不足，订单金额：" + String.format("%.2f", totalAmount) + "元，当前余额：" +
                    String.format("%.2f", buyerBalance != null ? buyerBalance : BigDecimal.ZERO) + "元");
        }

        // 获取农户UID
        String farmerUid = dbManager.getFarmerUidByFarmerId(product.getFarmerId());
        if (farmerUid == null) {
            throw new RuntimeException("农户信息异常");
        }

        // 9. 创建订单
        Order order = new Order();
        order.setOrderId("order-" + UUID.randomUUID().toString());
        order.setBuyerUid(buyerUid);
        order.setFarmerUid(farmerUid);
        order.setProductId(productId);
        order.setProductTitle(product.getTitle());
        order.setProductSpecification(product.getDetailedDescription());
        order.setProductPrice(productPrice);
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(totalAmount);
        order.setBuyerName(request.getBuyerName());
        order.setBuyerAddress(request.getBuyerAddress());
        order.setBuyerPhone(request.getBuyerPhone());
        order.setRemark(request.getRemark());
        order.setStatus("shipped");

        // 10. 保存订单到数据库
        dbManager.createOrder(order);

        // 11. 扣减库存
        dbManager.updateProductStock(productId, -request.getQuantity());

        // 12. 扣减买家余额
        dbManager.updateBuyerBalance(buyerUid, totalAmount.negate());

        // 13. 增加销量
        dbManager.updateProductSalesCount(productId, request.getQuantity());

        // 14. 构建响应
        CreateOrderResponseDTO response = new CreateOrderResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setProductId(request.getProductId());
        response.setTitle(product.getTitle());
        response.setPrice(productPrice);
        response.setQuantity(request.getQuantity());
        response.setTotalAmount(totalAmount);
        response.setBuyerName(request.getBuyerName());
        response.setBuyerAddress(request.getBuyerAddress());
        response.setBuyerPhone(request.getBuyerPhone());
        response.setStatus("shipped");
        response.setCreatedAt(dateFormat.format(new Timestamp(System.currentTimeMillis())));

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/buyer/orders/" + order.getOrderId());
        links.put("cancel", "/api/v1/buyer/orders/" + order.getOrderId() + "/cancel");
        response.setLinks(links);

        return response;
    }

    @Override
    public UpdateOrderResponseDTO updateOrder(String orderId, UpdateOrderRequestDTO request) throws Exception {
        // 1. 参数验证
        validateUpdateOrderRequest(request);

        if (!request.hasUpdateFields()) {
            throw new RuntimeException("至少需要提供一个需要修改的字段");
        }

        // 2. 验证买家是否存在
        String buyerUid = dbManager.getBuyerUidByPhone(request.getBuyerPhone());
        if (buyerUid == null) {
            throw new RuntimeException("买家账户不存在或未启用");
        }

        // 3. 验证订单是否存在
        Order order = dbManager.findOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 4. 验证订单是否属于该买家
        if (!order.getBuyerUid().equals(buyerUid)) {
            throw new RuntimeException("无权限访问该订单");
        }

        // 5. 验证订单状态 - 只有已发货状态才能修改订单信息
        if (!"shipped".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许修改");
        }

        // 6. 更新订单
        dbManager.updateOrder(orderId, request.getBuyerName(), request.getBuyerAddress(), request.getRemark());

        // 7. 重新查询订单获取最新数据
        order = dbManager.findOrderById(orderId);

        // 8. 构建响应
        UpdateOrderResponseDTO response = new UpdateOrderResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setBuyerName(order.getBuyerName());
        response.setBuyerAddress(order.getBuyerAddress());
        response.setBuyerPhone(order.getBuyerPhone());
        response.setRemark(order.getRemark());
        response.setStatus(order.getStatus());
        response.setUpdatedAt(dateFormat.format(order.getUpdatedAt()));

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/buyer/orders/" + order.getOrderId());
        response.setLinks(links);

        return response;
    }

    @Override
    public OrderDetailResponseDTO getOrderDetail(String orderId, QueryOrderRequestDTO request) throws Exception {
        // 1. 参数验证
        if (request.getBuyerPhone() == null || !request.getBuyerPhone().matches("\\d{11}")) {
            throw new RuntimeException("手机号格式不正确，必须是11位数字");
        }

        // 2. 验证买家是否存在
        String buyerUid = dbManager.getBuyerUidByPhone(request.getBuyerPhone());
        if (buyerUid == null) {
            throw new RuntimeException("买家账户不存在或未启用");
        }

        // 3. 验证订单是否存在
        Order order = dbManager.findOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 4. 验证订单是否属于该买家
        if (!order.getBuyerUid().equals(buyerUid)) {
            throw new RuntimeException("无权限访问该订单");
        }

        // 5. 构建响应
        OrderDetailResponseDTO response = new OrderDetailResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setProductId(String.valueOf(order.getProductId()));
        response.setTitle(order.getProductTitle());
        response.setSpecification(order.getProductSpecification());
        response.setPrice(order.getProductPrice());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setBuyerName(order.getBuyerName());
        response.setBuyerAddress(order.getBuyerAddress());
        response.setBuyerPhone(order.getBuyerPhone());
        response.setStatus(order.getStatus());
        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt() != null ? dateFormat.format(order.getCreatedAt()) : null);
        response.setShippedAt(order.getShippedAt() != null ? dateFormat.format(order.getShippedAt()) : null);
        response.setCompletedAt(order.getCompletedAt() != null ? dateFormat.format(order.getCompletedAt()) : null);
        response.setCancelledAt(order.getCancelledAt() != null ? dateFormat.format(order.getCancelledAt()) : null);
        response.setRefundedAt(order.getRefundedAt() != null ? dateFormat.format(order.getRefundedAt()) : null);
        response.setImages(order.getImages());

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/buyer/orders/" + order.getOrderId());

        // 根据订单状态添加可用操作链接
        if ("shipped".equals(order.getStatus())) {
            links.put("cancel", "/api/v1/buyer/orders/" + order.getOrderId() + "/cancel");
            links.put("confirm_receipt", "/api/v1/buyer/orders/" + order.getOrderId() + "/confirm-receipt");
            links.put("refund", "/api/v1/buyer/orders/" + order.getOrderId() + "/refund");
        }

        response.setLinks(links);

        return response;
    }

    @Override
    public OrderListResponseDTO getOrderList(String buyerPhone, String status, String title) throws Exception {
        // 1. 参数验证
        if (buyerPhone == null || !buyerPhone.matches("\\d{11}")) {
            throw new RuntimeException("手机号格式不正确，必须是11位数字");
        }

        // 2. 验证状态值（如果提供）
        if (status != null && !status.trim().isEmpty()) {
            String[] validStatuses = { "shipped", "completed", "cancelled", "refunded" };
            boolean isValid = false;
            for (String validStatus : validStatuses) {
                if (validStatus.equals(status)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new RuntimeException("无效的订单状态，允许的值：shipped, completed, cancelled, refunded");
            }
        }

        // 3. 验证买家是否存在
        String buyerUid = dbManager.getBuyerUidByPhone(buyerPhone);
        if (buyerUid == null) {
            throw new RuntimeException("买家账户不存在或未启用");
        }

        // 4. 查询订单列表
        List<Order> orders = dbManager.findOrdersByBuyer(buyerUid, status, title);

        // 5. 构建响应
        List<OrderListItemDTO> list = new ArrayList<>();
        for (Order order : orders) {
            OrderListItemDTO item = new OrderListItemDTO();
            item.setOrderId(order.getOrderId());
            item.setProductId(String.valueOf(order.getProductId()));
            item.setTitle(order.getProductTitle());
            item.setQuantity(order.getQuantity());
            item.setTotalAmount(order.getTotalAmount());
            item.setStatus(order.getStatus());
            item.setCreatedAt(dateFormat.format(order.getCreatedAt()));

            // 设置商品主图
            if (order.getImages() != null && !order.getImages().isEmpty()) {
                item.setMainImageUrl(order.getImages().get(0));
            }

            list.add(item);
        }

        return new OrderListResponseDTO(list);
    }

    /**
     * 获取农户订单列表
     * 
     * @param farmerPhone 农户手机号
     * @param status      订单状态（可选）
     * @param title       商品标题（可选）
     * @return 订单列表
     * @throws Exception 如果查询失败
     */
    public OrderListResponseDTO getFarmerOrderList(String farmerPhone, String status, String title) throws Exception {
        // 1. 参数验证
        if (farmerPhone == null || !farmerPhone.matches("\\d{11}")) {
            throw new RuntimeException("手机号格式不正确，必须是11位数字");
        }

        // 2. 验证状态值（如果提供）
        if (status != null && !status.trim().isEmpty()) {
            String[] validStatuses = { "shipped", "completed", "cancelled", "refunded" };
            boolean isValid = false;
            for (String validStatus : validStatuses) {
                if (validStatus.equals(status)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new RuntimeException("无效的订单状态，允许的值：shipped, completed, cancelled, refunded");
            }
        }

        // 3. 验证农户是否存在
        String farmerUid = dbManager.getFarmerUidByPhone(farmerPhone);
        if (farmerUid == null) {
            throw new RuntimeException("农户账户不存在或未启用");
        }

        // 4. 查询订单列表
        List<Order> orders = dbManager.findOrdersByFarmer(farmerUid, status, title);

        // 5. 构建响应
        List<OrderListItemDTO> list = new ArrayList<>();
        for (Order order : orders) {
            OrderListItemDTO item = new OrderListItemDTO();
            item.setOrderId(order.getOrderId());
            item.setProductId(String.valueOf(order.getProductId()));
            item.setTitle(order.getProductTitle());
            item.setQuantity(order.getQuantity());
            item.setTotalAmount(order.getTotalAmount());
            item.setStatus(order.getStatus());
            item.setCreatedAt(dateFormat.format(order.getCreatedAt()));

            // 设置商品主图
            if (order.getImages() != null && !order.getImages().isEmpty()) {
                item.setMainImageUrl(order.getImages().get(0));
            }

            list.add(item);
        }

        return new OrderListResponseDTO(list);
    }

    /**
     * 获取农户订单详情
     * 
     * @param orderId     订单ID
     * @param farmerPhone 农户手机号
     * @return 订单详情
     * @throws Exception 如果查询失败
     */
    public OrderDetailResponseDTO getFarmerOrderDetail(String orderId, String farmerPhone) throws Exception {
        // 1. 参数验证
        if (farmerPhone == null || !farmerPhone.matches("\\d{11}")) {
            throw new RuntimeException("手机号格式不正确，必须是11位数字");
        }

        // 2. 验证农户是否存在
        String farmerUid = dbManager.getFarmerUidByPhone(farmerPhone);
        if (farmerUid == null) {
            throw new RuntimeException("农户账户不存在或未启用");
        }

        // 3. 验证订单是否存在
        Order order = dbManager.findOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 4. 验证订单是否属于该农户
        if (!order.getFarmerUid().equals(farmerUid)) {
            throw new RuntimeException("无权限访问该订单");
        }

        // 5. 构建响应
        OrderDetailResponseDTO response = new OrderDetailResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setProductId(String.valueOf(order.getProductId()));
        response.setTitle(order.getProductTitle());
        response.setSpecification(order.getProductSpecification());
        response.setPrice(order.getProductPrice());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setBuyerName(order.getBuyerName());
        response.setBuyerAddress(order.getBuyerAddress());
        response.setBuyerPhone(order.getBuyerPhone());
        response.setStatus(order.getStatus());
        response.setRemark(order.getRemark());
        response.setCreatedAt(dateFormat.format(order.getCreatedAt()));
        response.setShippedAt(order.getShippedAt() != null ? dateFormat.format(order.getShippedAt()) : null);
        response.setCompletedAt(order.getCompletedAt() != null ? dateFormat.format(order.getCompletedAt()) : null);
        response.setCancelledAt(order.getCancelledAt() != null ? dateFormat.format(order.getCancelledAt()) : null);
        response.setRefundedAt(order.getRefundedAt() != null ? dateFormat.format(order.getRefundedAt()) : null);
        response.setImages(order.getImages());

        // 农户不需要操作链接
        response.setLinks(new HashMap<>());

        return response;
    }

    @Override
    public RefundResponseDTO applyRefund(String orderId, RefundRequestDTO request) throws Exception {
        // 1. 参数验证
        validateRefundRequest(request);

        // 2. 验证买家是否存在
        String buyerUid = dbManager.getBuyerUidByPhone(request.getBuyerPhone());
        if (buyerUid == null) {
            throw new RuntimeException("买家账户不存在或未启用");
        }

        // 3. 验证订单是否存在
        Order order = dbManager.findOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 4. 验证订单是否属于该买家
        if (!order.getBuyerUid().equals(buyerUid)) {
            throw new RuntimeException("无权限访问该订单");
        }

        // 5. 验证订单状态
        if ("completed".equals(order.getStatus())) {
            throw new RuntimeException("订单已完成，不能申请退款");
        } else if ("refunded".equals(order.getStatus())) {
            throw new RuntimeException("订单已退款，不能重复申请");
        } else if (!"shipped".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许申请退款");
        }

        // 6. 验证退款类型 - 由于订单直接是已发货状态，只支持退货退款
        if ("only_refund".equals(request.getRefundType())) {
            throw new RuntimeException("订单已发货，请选择退货退款类型");
        }

        // 7. 处理退款
        System.out.println("applyRefund - 准备更新订单: orderId=" + orderId +
                ", refundReason=" + request.getRefundReason() +
                ", refundType=" + request.getRefundType());

        // 7.1 更新订单状态为已退款
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dbManager.updateOrderStatus(orderId, "refunded", now);
        dbManager.updateOrderRefund(orderId, request.getRefundReason(), request.getRefundType());

        // 7.2 恢复库存
        dbManager.updateProductStock(order.getProductId(), order.getQuantity());

        // 7.3 退款给买家
        dbManager.updateBuyerBalance(order.getBuyerUid(), order.getTotalAmount());

        // 7.4 减少销量
        dbManager.updateProductSalesCount(order.getProductId(), -order.getQuantity());

        System.out.println("applyRefund - 订单更新完成，已退款给买家");

        // 8. 构建响应
        RefundResponseDTO response = new RefundResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setRefundType(request.getRefundType());
        response.setRefundAmount(order.getTotalAmount());
        response.setRefundReason(request.getRefundReason());
        response.setStatus("refunded");
        response.setAppliedAt(dateFormat.format(now));

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/buyer/orders/" + order.getOrderId());
        response.setLinks(links);

        return response;
    }

    @Override
    public ConfirmReceiptResponseDTO confirmReceipt(String orderId, ConfirmReceiptRequestDTO request) throws Exception {
        // 1. 参数验证
        if (request.getBuyerPhone() == null || !request.getBuyerPhone().matches("\\d{11}")) {
            throw new RuntimeException("手机号格式不正确，必须是11位数字");
        }

        // 2. 验证买家是否存在
        String buyerUid = dbManager.getBuyerUidByPhone(request.getBuyerPhone());
        if (buyerUid == null) {
            throw new RuntimeException("买家账户不存在或未启用");
        }

        // 3. 验证订单是否存在
        Order order = dbManager.findOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 4. 验证订单是否属于该买家
        if (!order.getBuyerUid().equals(buyerUid)) {
            throw new RuntimeException("无权限访问该订单");
        }

        // 5. 验证订单状态 - 只有已发货状态才能确认收货
        if ("completed".equals(order.getStatus())) {
            throw new RuntimeException("订单已完成，不能重复确认收货");
        } else if (!"shipped".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许确认收货");
        }

        // 6. 更新订单状态为已完成
        Timestamp now = new Timestamp(System.currentTimeMillis());
        dbManager.updateOrderStatus(orderId, "completed", now);

        // 7. 农户收款（增加农户余额）
        dbManager.updateFarmerBalance(order.getFarmerUid(), order.getTotalAmount());

        // 8. 构建响应
        ConfirmReceiptResponseDTO response = new ConfirmReceiptResponseDTO();
        response.setOrderId(order.getOrderId());
        response.setStatus("completed");
        response.setCompletedAt(dateFormat.format(now));

        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1/buyer/orders/" + order.getOrderId());
        response.setLinks(links);

        return response;
    }

    // ============= 私有辅助方法 =============

    private void validateCreateOrderRequest(CreateOrderRequestDTO request) throws Exception {
        List<Map<String, String>> errors = new ArrayList<>();

        if (request.getProductId() == null || request.getProductId().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "product_id");
            error.put("message", "商品ID不能为空");
            errors.add(error);
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "quantity");
            error.put("message", "购买数量必须大于0");
            errors.add(error);
        }

        if (request.getBuyerName() == null || request.getBuyerName().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_name");
            error.put("message", "买家姓名不能为空");
            errors.add(error);
        } else if (request.getBuyerName().length() > 50) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_name");
            error.put("message", "买家姓名长度不能超过50个字符");
            errors.add(error);
        }

        if (request.getBuyerAddress() == null || request.getBuyerAddress().length() < 5
                || request.getBuyerAddress().length() > 200) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_address");
            error.put("message", "收货地址长度必须在5-200个字符之间");
            errors.add(error);
        }

        if (request.getBuyerPhone() == null || !request.getBuyerPhone().matches("\\d{11}")) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_phone");
            error.put("message", "手机号格式不正确，必须是11位数字");
            errors.add(error);
        }

        if (request.getRemark() != null && request.getRemark().length() > 500) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "remark");
            error.put("message", "订单备注长度不能超过500个字符");
            errors.add(error);
        }

        if (!errors.isEmpty()) {
            // 构建详细的错误消息
            StringBuilder errorMessage = new StringBuilder("参数验证失败：");
            for (int i = 0; i < errors.size(); i++) {
                Map<String, String> error = errors.get(i);
                errorMessage.append(error.get("message"));
                if (i < errors.size() - 1) {
                    errorMessage.append("；");
                }
            }
            throw new ValidationException(errorMessage.toString(), errors);
        }
    }

    private void validateUpdateOrderRequest(UpdateOrderRequestDTO request) throws Exception {
        List<Map<String, String>> errors = new ArrayList<>();

        if (request.getBuyerName() != null && request.getBuyerName().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_name");
            error.put("message", "买家姓名不能为空");
            errors.add(error);
        } else if (request.getBuyerName() != null && request.getBuyerName().length() > 50) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_name");
            error.put("message", "买家姓名长度不能超过50个字符");
            errors.add(error);
        }

        if (request.getBuyerAddress() != null
                && (request.getBuyerAddress().length() < 5 || request.getBuyerAddress().length() > 200)) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_address");
            error.put("message", "收货地址长度必须在5-200个字符之间");
            errors.add(error);
        }

        if (request.getBuyerPhone() == null || !request.getBuyerPhone().matches("\\d{11}")) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_phone");
            error.put("message", "手机号格式不正确，必须是11位数字");
            errors.add(error);
        }

        if (request.getRemark() != null && request.getRemark().length() > 500) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "remark");
            error.put("message", "订单备注长度不能超过500个字符");
            errors.add(error);
        }

        if (!errors.isEmpty()) {
            // 构建详细的错误消息
            StringBuilder errorMessage = new StringBuilder("参数验证失败：");
            for (int i = 0; i < errors.size(); i++) {
                Map<String, String> error = errors.get(i);
                errorMessage.append(error.get("message"));
                if (i < errors.size() - 1) {
                    errorMessage.append("；");
                }
            }
            throw new ValidationException(errorMessage.toString(), errors);
        }
    }

    private void validateRefundRequest(RefundRequestDTO request) throws Exception {
        List<Map<String, String>> errors = new ArrayList<>();

        if (request.getRefundReason() == null || request.getRefundReason().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "refund_reason");
            error.put("message", "退款原因不能为空");
            errors.add(error);
        } else if (request.getRefundReason().length() > 200) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "refund_reason");
            error.put("message", "退款原因长度不能超过200个字符");
            errors.add(error);
        }

        if (request.getRefundType() == null || (!request.getRefundType().equals("only_refund")
                && !request.getRefundType().equals("return_and_refund"))) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "refund_type");
            error.put("message", "无效的退款类型，允许的值：only_refund, return_and_refund");
            errors.add(error);
        }

        if (request.getBuyerPhone() == null || !request.getBuyerPhone().matches("\\d{11}")) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "buyer_phone");
            error.put("message", "手机号格式不正确，必须是11位数字");
            errors.add(error);
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("参数验证失败");
        }
    }
}
