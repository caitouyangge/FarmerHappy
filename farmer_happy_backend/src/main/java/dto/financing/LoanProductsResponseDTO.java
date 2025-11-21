// src/main/java/dto/financing/LoanProductsResponseDTO.java
package dto.financing;

import java.util.List;

public class LoanProductsResponseDTO {
    private Integer total;
    private List<LoanProductDTO> available_products;

    // 构造函数
    public LoanProductsResponseDTO() {}

    // Getter和Setter方法
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public List<LoanProductDTO> getAvailable_products() { return available_products; }
    public void setAvailable_products(List<LoanProductDTO> available_products) { this.available_products = available_products; }
}
