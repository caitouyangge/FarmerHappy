package dto.buyer;

import java.util.List;

/**
 * 订单列表响应DTO
 */
public class OrderListResponseDTO {
    private List<OrderListItemDTO> list;

    public OrderListResponseDTO() {
    }

    public OrderListResponseDTO(List<OrderListItemDTO> list) {
        this.list = list;
    }

    // Getters and Setters
    public List<OrderListItemDTO> getList() {
        return list;
    }

    public void setList(List<OrderListItemDTO> list) {
        this.list = list;
    }
}

