// src/main/java/dto/financing/SingleLoanApplicationResponseDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class SingleLoanApplicationResponseDTO {
    private String loan_application_id;
    private String status;
    private String product_name;
    private BigDecimal apply_amount;
    private BigDecimal estimated_monthly_payment;
    private Timestamp created_at;

    // 构造函数
    public SingleLoanApplicationResponseDTO() {}

    // Getter和Setter方法
    public String getLoan_application_id() { return loan_application_id; }
    public void setLoan_application_id(String loan_application_id) { this.loan_application_id = loan_application_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProduct_name() { return product_name; }
    public void setProduct_name(String product_name) { this.product_name = product_name; }

    public BigDecimal getApply_amount() { return apply_amount; }
    public void setApply_amount(BigDecimal apply_amount) { this.apply_amount = apply_amount; }

    public BigDecimal getEstimated_monthly_payment() { return estimated_monthly_payment; }
    public void setEstimated_monthly_payment(BigDecimal estimated_monthly_payment) { this.estimated_monthly_payment = estimated_monthly_payment; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }
}
