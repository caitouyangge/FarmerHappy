package dto.buyer;

import java.util.Map;

/**
 * 确认收货响应DTO
 */
public class ConfirmReceiptResponseDTO {
    private String orderId;         // 订单ID
    private String status;          // 订单状态
    private String completedAt;     // 完成时间
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

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }
}

