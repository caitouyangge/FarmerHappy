// src/dto/farmer/ProductStatusUpdateResponseDTO.java
package dto.farmer;

public class ProductStatusUpdateResponseDTO {
    private String product_id;
    private String status;

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
