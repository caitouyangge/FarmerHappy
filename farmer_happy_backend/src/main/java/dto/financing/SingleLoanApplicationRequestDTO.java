// src/main/java/dto/financing/SingleLoanApplicationRequestDTO.java
package dto.financing;

import java.math.BigDecimal;

public class SingleLoanApplicationRequestDTO {
    private String phone;
    private String product_id;
    private BigDecimal apply_amount;
    private String purpose;
    private String repayment_source;

    // 构造函数
    public SingleLoanApplicationRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public BigDecimal getApply_amount() { return apply_amount; }
    public void setApply_amount(BigDecimal apply_amount) { this.apply_amount = apply_amount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRepayment_source() { return repayment_source; }
    public void setRepayment_source(String repayment_source) { this.repayment_source = repayment_source; }
}
