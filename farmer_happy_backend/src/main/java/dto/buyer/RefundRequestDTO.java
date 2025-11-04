package dto.buyer;

/**
 * 申请退货退款请求DTO
 */
public class RefundRequestDTO {
    private String refundReason;    // 退款原因（必填）
    private String refundType;      // 退款类型（必填）
    private String buyerPhone;      // 买家手机号（必填）

    // Getters and Setters
    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getRefundType() {
        return refundType;
    }

    public void setRefundType(String refundType) {
        this.refundType = refundType;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }
}

