package controller;

import dto.buyer.*;
import exception.ValidationException;
import service.buyer.OrderService;
import service.buyer.OrderServiceImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单控制器
 */
public class OrderController {

    private final OrderService orderService;

    public OrderController() {
        this.orderService = new OrderServiceImpl();
    }

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单
     */
    public Map<String, Object> createOrder(CreateOrderRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层创建订单
            CreateOrderResponseDTO result = orderService.createOrder(request);

            response.put("code", 201);
            response.put("message", "订单创建成功");
            response.put("data", result);
            response.put("location", "/api/v1/buyer/orders/" + result.getOrderId());

        } catch (ValidationException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("errors", e.getErrors());
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("买家账户不存在") || message.contains("商品不存在")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("已下架") || message.contains("库存")) {
                response.put("code", 409);
                response.put("message", message);
            } else if (message.contains("余额不足")) {
                response.put("code", 402);
                response.put("message", message);
            } else if (message.contains("单次购买数量")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 更新订单信息
     */
    public Map<String, Object> updateOrder(String orderId, UpdateOrderRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层更新订单
            UpdateOrderResponseDTO result = orderService.updateOrder(orderId, request);

            response.put("code", 200);
            response.put("message", "更新成功");
            response.put("data", result);

        } catch (ValidationException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("errors", e.getErrors());
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无权限")) {
                response.put("code", 403);
                response.put("message", message);
            } else if (message.contains("不允许") || message.contains("已发货")) {
                response.put("code", 409);
                response.put("message", message);
            } else if (message.contains("至少需要提供")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 获取订单详情
     */
    public Map<String, Object> getOrderDetail(String orderId, QueryOrderRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层获取订单详情
            OrderDetailResponseDTO result = orderService.getOrderDetail(orderId, request);

            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", result);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无权限")) {
                response.put("code", 403);
                response.put("message", message);
            } else if (message.contains("手机号格式")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 获取订单列表
     */
    public Map<String, Object> getOrderList(String buyerPhone, String status, String title) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层获取订单列表
            OrderListResponseDTO result = orderService.getOrderList(buyerPhone, status, title);

            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", result);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无效的订单状态") || message.contains("手机号格式")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 申请退货退款
     */
    public Map<String, Object> applyRefund(String orderId, RefundRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层申请退款
            RefundResponseDTO result = orderService.applyRefund(orderId, request);

            response.put("code", 200);
            response.put("message", "退款申请已提交，等待农户审核");
            response.put("data", result);

        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无权限")) {
                response.put("code", 403);
                response.put("message", message);
            } else if (message.contains("不允许") || message.contains("已完成") || message.contains("正在退款")
                    || message.contains("未发货")) {
                response.put("code", 409);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 确认收货
     */
    public Map<String, Object> confirmReceipt(String orderId, ConfirmReceiptRequestDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层确认收货
            ConfirmReceiptResponseDTO result = orderService.confirmReceipt(orderId, request);

            response.put("code", 200);
            response.put("message", "确认收货成功");
            response.put("data", result);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无权限")) {
                response.put("code", 403);
                response.put("message", message);
            } else if (message.contains("不允许") || message.contains("未发货") || message.contains("已完成")) {
                response.put("code", 409);
                response.put("message", message);
            } else if (message.contains("手机号格式")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 获取农户订单列表
     */
    public Map<String, Object> getFarmerOrderList(String farmerPhone, String status, String title) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层获取订单列表
            OrderListResponseDTO result = ((OrderServiceImpl) orderService).getFarmerOrderList(farmerPhone, status,
                    title);

            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", result);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无效的订单状态") || message.contains("手机号格式")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }

    /**
     * 获取农户订单详情
     */
    public Map<String, Object> getFarmerOrderDetail(String orderId, String farmerPhone) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 调用服务层获取订单详情
            OrderDetailResponseDTO result = ((OrderServiceImpl) orderService).getFarmerOrderDetail(orderId,
                    farmerPhone);

            response.put("code", 200);
            response.put("message", "成功");
            response.put("data", result);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("不存在") || message.contains("未启用")) {
                response.put("code", 404);
                response.put("message", message);
            } else if (message.contains("无权限")) {
                response.put("code", 403);
                response.put("message", message);
            } else if (message.contains("手机号格式")) {
                response.put("code", 400);
                response.put("message", message);
            } else {
                response.put("code", 500);
                response.put("message", "服务器内部错误：" + message);
            }
        }

        return response;
    }
}
