package dto.buyer;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 申请退货退款响应DTO
 */
public class RefundResponseDTO {
    private String orderId;         // 订单ID
    private String refundType;      // 退款类型
    private BigDecimal refundAmount; // 退款金额
    private String refundReason;    // 退款原因
    private String status;          // 订单状态
    private String appliedAt;       // 申请时间
    private Map<String, String> links; // 操作链接

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRefundType() {
        return refundType;
    }

    public void setRefundType(String refundType) {
        this.refundType = refundType;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(String appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }
}

