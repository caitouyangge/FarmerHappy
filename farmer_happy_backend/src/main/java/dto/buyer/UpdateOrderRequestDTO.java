package dto.buyer;

/**
 * 更新订单请求DTO
 */
public class UpdateOrderRequestDTO {
    private String buyerName;       // 买家姓名（可选）
    private String buyerAddress;    // 收货地址（可选）
    private String remark;          // 订单备注（可选）
    private String buyerPhone;      // 买家手机号（必填，用于身份验证）

    // Getters and Setters
    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }

    /**
     * 检查是否至少提供了一个需要更新的字段
     */
    public boolean hasUpdateFields() {
        return (buyerName != null && !buyerName.trim().isEmpty()) ||
               (buyerAddress != null && !buyerAddress.trim().isEmpty()) ||
               (remark != null);
    }
}

