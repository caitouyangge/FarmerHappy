// dto/financing/RepaymentRequestDTO.java
package dto.financing;

import java.math.BigDecimal;

public class RepaymentRequestDTO {
    private String phone;
    private String loan_id;
    private BigDecimal repayment_amount;
    private String repayment_method;
    private String payment_account;
    private String remarks;

    // Getters and Setters
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLoan_id() { return loan_id; }
    public void setLoan_id(String loan_id) { this.loan_id = loan_id; }

    public BigDecimal getRepayment_amount() { return repayment_amount; }
    public void setRepayment_amount(BigDecimal repayment_amount) { this.repayment_amount = repayment_amount; }

    public String getRepayment_method() { return repayment_method; }
    public void setRepayment_method(String repayment_method) { this.repayment_method = repayment_method; }

    public String getPayment_account() { return payment_account; }
    public void setPayment_account(String payment_account) { this.payment_account = payment_account; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
