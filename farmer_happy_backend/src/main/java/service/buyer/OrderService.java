package service.buyer;

import dto.buyer.*;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     * @param request 创建订单请求
     * @return 创建订单响应
     * @throws Exception 如果创建失败
     */
    CreateOrderResponseDTO createOrder(CreateOrderRequestDTO request) throws Exception;
    
    /**
     * 更新订单信息
     * @param orderId 订单ID
     * @param request 更新订单请求
     * @return 更新订单响应
     * @throws Exception 如果更新失败
     */
    UpdateOrderResponseDTO updateOrder(String orderId, UpdateOrderRequestDTO request) throws Exception;
    
    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @param request 查询请求（包含买家手机号）
     * @return 订单详情
     * @throws Exception 如果查询失败
     */
    OrderDetailResponseDTO getOrderDetail(String orderId, QueryOrderRequestDTO request) throws Exception;
    
    /**
     * 获取买家订单列表
     * @param buyerPhone 买家手机号
     * @param status 订单状态（可选）
     * @param title 商品标题（可选）
     * @return 订单列表
     * @throws Exception 如果查询失败
     */
    OrderListResponseDTO getOrderList(String buyerPhone, String status, String title) throws Exception;
    
    /**
     * 取消订单
     * @param orderId 订单ID
     * @param request 取消订单请求
     * @return 取消订单响应
     * @throws Exception 如果取消失败
     */
    CancelOrderResponseDTO cancelOrder(String orderId, CancelOrderRequestDTO request) throws Exception;
    
    /**
     * 申请退货退款
     * @param orderId 订单ID
     * @param request 退款请求
     * @return 退款响应
     * @throws Exception 如果申请失败
     */
    RefundResponseDTO applyRefund(String orderId, RefundRequestDTO request) throws Exception;
    
    /**
     * 确认收货
     * @param orderId 订单ID
     * @param request 确认收货请求
     * @return 确认收货响应
     * @throws Exception 如果确认失败
     */
    ConfirmReceiptResponseDTO confirmReceipt(String orderId, ConfirmReceiptRequestDTO request) throws Exception;
}

