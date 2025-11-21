// src/main/java/dto/financing/BankLoanProductResponseDTO.java
package dto.financing;

import java.sql.Timestamp;

public class BankLoanProductResponseDTO {
    private String product_id;
    private String product_code;
    private String status;
    private Timestamp created_at;
    private String created_by;

    // 构造函数
    public BankLoanProductResponseDTO() {}

    // Getter和Setter方法
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public String getProduct_code() { return product_code; }
    public void setProduct_code(String product_code) { this.product_code = product_code; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public String getCreated_by() { return created_by; }
    public void setCreated_by(String created_by) { this.created_by = created_by; }
}
