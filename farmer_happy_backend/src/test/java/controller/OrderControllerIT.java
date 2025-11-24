package controller;

import dto.buyer.*;
import exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.buyer.OrderService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderControllerIT {

    @Mock
    OrderService orderService;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderController(orderService);
    }

    @Test
    void create_order_maps_balance_insufficient_to_402() throws Exception {
        CreateOrderRequestDTO req = new CreateOrderRequestDTO();
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenThrow(new RuntimeException("余额不足"));
        Map<String, Object> resp = controller.createOrder(req);
        assertThat(resp.get("code")).isEqualTo(402);
    }

    @Test
    void create_order_maps_out_of_stock_to_409() throws Exception {
        CreateOrderRequestDTO req = new CreateOrderRequestDTO();
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenThrow(new RuntimeException("库存不足"));
        Map<String, Object> resp = controller.createOrder(req);
        assertThat(resp.get("code")).isEqualTo(409);
    }

    @Test
    void create_order_maps_buyer_not_found_to_404() throws Exception {
        CreateOrderRequestDTO req = new CreateOrderRequestDTO();
        when(orderService.createOrder(any(CreateOrderRequestDTO.class))).thenThrow(new RuntimeException("买家账户不存在"));
        Map<String, Object> resp = controller.createOrder(req);
        assertThat(resp.get("code")).isEqualTo(404);
    }

    @Test
    void update_order_maps_validation_errors_to_400_with_errors() throws Exception {
        UpdateOrderRequestDTO req = new UpdateOrderRequestDTO();
        java.util.List<java.util.Map<String, String>> errors = new java.util.ArrayList<>();
        java.util.Map<String, String> e = new java.util.HashMap<>();
        e.put("field", "buyer_phone");
        e.put("message", "手机号格式不正确");
        errors.add(e);
        when(orderService.updateOrder("ORD-1", req)).thenThrow(new ValidationException("参数验证失败", errors));
        Map<String, Object> resp = controller.updateOrder("ORD-1", req);
        assertThat(resp.get("code")).isEqualTo(400);
        assertThat(((java.util.List<?>) resp.get("errors")).size()).isEqualTo(1);
    }

    @Test
    void get_order_detail_maps_invalid_phone_to_400() throws Exception {
        QueryOrderRequestDTO req = new QueryOrderRequestDTO();
        when(orderService.getOrderDetail("ORD-2", req)).thenThrow(new RuntimeException("手机号格式不正确"));
        Map<String, Object> resp = controller.getOrderDetail("ORD-2", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void apply_refund_maps_not_allowed_to_409() throws Exception {
        RefundRequestDTO req = new RefundRequestDTO();
        when(orderService.applyRefund("ORD-3", req)).thenThrow(new RuntimeException("当前订单状态不允许申请退款"));
        Map<String, Object> resp = controller.applyRefund("ORD-3", req);
        assertThat(resp.get("code")).isEqualTo(409);
    }

    @Test
    void confirm_receipt_maps_not_shipped_to_409() throws Exception {
        ConfirmReceiptRequestDTO req = new ConfirmReceiptRequestDTO();
        when(orderService.confirmReceipt("ORD-4", req)).thenThrow(new RuntimeException("当前订单状态不允许确认收货"));
        Map<String, Object> resp = controller.confirmReceipt("ORD-4", req);
        assertThat(resp.get("code")).isEqualTo(409);
    }

}
