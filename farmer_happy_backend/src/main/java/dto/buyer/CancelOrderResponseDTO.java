package dto.buyer;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 取消订单响应DTO
 */
public class CancelOrderResponseDTO {
    private String orderId;         // 订单ID
    private String status;          // 订单状态
    private BigDecimal refundAmount; // 退款金额
    private String cancelReason;    // 取消原因
    private String cancelledAt;     // 取消时间
    private Map<String, String> links; // 操作链接

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(String cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }
}

