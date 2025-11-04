package dto.buyer;

/**
 * 查询订单请求DTO（用于获取订单详情）
 */
public class QueryOrderRequestDTO {
    private String buyerPhone;      // 买家手机号（必填）

    // Getters and Setters
    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }
}

