// src/main/java/dto/financing/LoanProductDTO.java
package dto.financing;

import java.math.BigDecimal;

public class LoanProductDTO {
    private String product_id;
    private String product_name;
    private String product_code;
    private BigDecimal min_credit_limit;
    private BigDecimal max_amount;
    private BigDecimal interest_rate;
    private Integer term_months;
    private String repayment_method;
    private String repayment_method_name;
    private String description;
    private String status;
    private Boolean can_apply;
    private String reason;
    private BigDecimal max_apply_amount;

    // 构造函数
    public LoanProductDTO() {}

    // Getter和Setter方法
    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public String getProduct_name() { return product_name; }
    public void setProduct_name(String product_name) { this.product_name = product_name; }

    public String getProduct_code() { return product_code; }
    public void setProduct_code(String product_code) { this.product_code = product_code; }

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

    public String getRepayment_method_name() { return repayment_method_name; }
    public void setRepayment_method_name(String repayment_method_name) { this.repayment_method_name = repayment_method_name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getCan_apply() { return can_apply; }
    public void setCan_apply(Boolean can_apply) { this.can_apply = can_apply; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public BigDecimal getMax_apply_amount() { return max_apply_amount; }
    public void setMax_apply_amount(BigDecimal max_apply_amount) { this.max_apply_amount = max_apply_amount; }
}
