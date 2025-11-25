// dto/financing/RepaymentResponseDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Date;

public class RepaymentResponseDTO {
    private String repayment_id;
    private String loan_id;
    private BigDecimal repayment_amount;
    private BigDecimal principal_amount;
    private BigDecimal interest_amount;
    private BigDecimal remaining_principal;
    private String repayment_method;
    private Timestamp repayment_date;
    private Date next_payment_date;
    private BigDecimal next_payment_amount;
    private String loan_status;
    private Timestamp closed_date;
    private BigDecimal total_interest_saved;

    // Getters and Setters
    public String getRepayment_id() { return repayment_id; }
    public void setRepayment_id(String repayment_id) { this.repayment_id = repayment_id; }

    public String getLoan_id() { return loan_id; }
    public void setLoan_id(String loan_id) { this.loan_id = loan_id; }

    public BigDecimal getRepayment_amount() { return repayment_amount; }
    public void setRepayment_amount(BigDecimal repayment_amount) { this.repayment_amount = repayment_amount; }

    public BigDecimal getPrincipal_amount() { return principal_amount; }
    public void setPrincipal_amount(BigDecimal principal_amount) { this.principal_amount = principal_amount; }

    public BigDecimal getInterest_amount() { return interest_amount; }
    public void setInterest_amount(BigDecimal interest_amount) { this.interest_amount = interest_amount; }

    public BigDecimal getRemaining_principal() { return remaining_principal; }
    public void setRemaining_principal(BigDecimal remaining_principal) { this.remaining_principal = remaining_principal; }

    public String getRepayment_method() { return repayment_method; }
    public void setRepayment_method(String repayment_method) { this.repayment_method = repayment_method; }

    public Timestamp getRepayment_date() { return repayment_date; }
    public void setRepayment_date(Timestamp repayment_date) { this.repayment_date = repayment_date; }

    public Date getNext_payment_date() { return next_payment_date; }
    public void setNext_payment_date(Date next_payment_date) { this.next_payment_date = next_payment_date; }

    public BigDecimal getNext_payment_amount() { return next_payment_amount; }
    public void setNext_payment_amount(BigDecimal next_payment_amount) { this.next_payment_amount = next_payment_amount; }

    public String getLoan_status() { return loan_status; }
    public void setLoan_status(String loan_status) { this.loan_status = loan_status; }

    public Timestamp getClosed_date() { return closed_date; }
    public void setClosed_date(Timestamp closed_date) { this.closed_date = closed_date; }

    public BigDecimal getTotal_interest_saved() { return total_interest_saved; }
    public void setTotal_interest_saved(BigDecimal total_interest_saved) { this.total_interest_saved = total_interest_saved; }
}
