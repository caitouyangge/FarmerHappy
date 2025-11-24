// src/main/java/dto/financing/LoanProductsRequestDTO.java
package dto.financing;

import java.math.BigDecimal;

public class LoanProductsRequestDTO {
    private String phone;
    private BigDecimal credit_limit;

    // 构造函数
    public LoanProductsRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getCredit_limit() { return credit_limit; }
    public void setCredit_limit(BigDecimal credit_limit) { this.credit_limit = credit_limit; }
}
