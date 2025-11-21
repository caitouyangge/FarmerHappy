// src/main/java/dto/financing/BankLoanProductRequestDTO.java
package dto.financing;

import java.math.BigDecimal;

public class BankLoanProductRequestDTO {
    private String phone;
    private String product_name;
    private BigDecimal min_credit_limit;
    private BigDecimal max_amount;
    private BigDecimal interest_rate;
    private Integer term_months;
    private String repayment_method;
    private String description;
    private String product_code;

    // 构造函数
    public BankLoanProductRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone=phone; }

    public String getProduct_name() { return product_name; }
    public void setProduct_name(String product_name) { this.product_name = product_name; }

    public BigDecimal getMin_credit_limit() { return min_credit_limit; }
    public void setMin_credit_limit(BigDecimal min_credit_limit) { this.min_credit_limit = min_credit_limit; }

    public BigDecimal getMax_amount() { return max_amount; }
    public void setMax_amount(BigDecimal max_amount) { this.max_amount = max_amount; }

    public BigDecimal getInterest_rate() { return interest_rate; }
    public void setInterest_rate(BigDecimal interest_rate) { this.interest_rate = interest_rate; }

    public Integer getTerm_months() { return term_months; }
    public void setTerm_months(Integer term_months) { this.term_months = term_months; }

    public String getRepayment_method() { return repayment_method; }
    public void setRepayment_method(String repayment_method) { this.repayment_method = repayment_method; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProduct_code() { return product_code; }
    public void setProduct_code(String product_code) { this.product_code = product_code; }
}
