// src/main/java/dto/bank/LoanDisbursementRequestDTO.java
package dto.bank;

import java.math.BigDecimal;
import java.sql.Date;

public class LoanDisbursementRequestDTO {
    private String phone;
    private String application_id;
    private BigDecimal disburse_amount;
    private String disburse_method;
    private Date first_repayment_date;
    private String loan_account;
    private String remarks;

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApplication_id() {
        return application_id;
    }

    public void setApplication_id(String application_id) {
        this.application_id = application_id;
    }

    public BigDecimal getDisburse_amount() {
        return disburse_amount;
    }

    public void setDisburse_amount(BigDecimal disburse_amount) {
        this.disburse_amount = disburse_amount;
    }

    public String getDisburse_method() {
        return disburse_method;
    }

    public void setDisburse_method(String disburse_method) {
        this.disburse_method = disburse_method;
    }

    public Date getFirst_repayment_date() {
        return first_repayment_date;
    }

    public void setFirst_repayment_date(Date first_repayment_date) {
        this.first_repayment_date = first_repayment_date;
    }

    public String getLoan_account() {
        return loan_account;
    }

    public void setLoan_account(String loan_account) {
        this.loan_account = loan_account;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
