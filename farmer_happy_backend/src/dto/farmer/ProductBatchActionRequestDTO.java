// src/dto/farmer/ProductBatchActionRequestDTO.java
package dto.farmer;

import java.util.List;

public class ProductBatchActionRequestDTO {
    private String action;
    private List<String> product_ids;
    private String phone;

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getProduct_ids() {
        return product_ids;
    }

    public void setProduct_ids(List<String> product_ids) {
        this.product_ids = product_ids;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
