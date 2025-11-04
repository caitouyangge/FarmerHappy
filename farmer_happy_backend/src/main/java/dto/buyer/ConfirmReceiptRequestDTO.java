package dto.buyer;

/**
 * 确认收货请求DTO
 */
public class ConfirmReceiptRequestDTO {
    private String buyerPhone;      // 买家手机号（必填）

    // Getters and Setters
    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }
}

