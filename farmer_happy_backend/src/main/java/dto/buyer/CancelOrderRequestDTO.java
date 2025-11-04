package dto.buyer;

/**
 * 取消订单请求DTO
 */
public class CancelOrderRequestDTO {
    private String cancelReason;    // 取消原因（可选）
    private String buyerPhone;      // 买家手机号（必填）

    // Getters and Setters
    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }
}

