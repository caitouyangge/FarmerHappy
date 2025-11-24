// src/main/java/dto/bank/LoanDisbursementResponseDTO.java
package dto.bank;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Date;

public class LoanDisbursementResponseDTO {
    private String loan_id;
    private String disbursement_id;
    private String application_id;
    private BigDecimal disburse_amount;
    private String disburse_method;
    private Timestamp disburse_date;
    private Date first_repayment_date;
    private String loan_status;
    private BigDecimal total_repayment_amount;
    private BigDecimal monthly_payment;
    private Date next_payment_date;

    // Getters and Setters
    public String getLoan_id() {
        return loan_id;
    }

    public void setLoan_id(String loan_id) {
        this.loan_id = loan_id;
    }

    public String getDisbursement_id() {
        return disbursement_id;
    }

    public void setDisbursement_id(String disbursement_id) {
        this.disbursement_id = disbursement_id;
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

    public Timestamp getDisburse_date() {
        return disburse_date;
    }

    public void setDisburse_date(Timestamp disburse_date) {
        this.disburse_date = disburse_date;
    }

    public Date getFirst_repayment_date() {
        return first_repayment_date;
    }

    public void setFirst_repayment_date(Date first_repayment_date) {
        this.first_repayment_date = first_repayment_date;
    }

    public String getLoan_status() {
        return loan_status;
    }

    public void setLoan_status(String loan_status) {
        this.loan_status = loan_status;
    }

    public BigDecimal getTotal_repayment_amount() {
        return total_repayment_amount;
    }

    public void setTotal_repayment_amount(BigDecimal total_repayment_amount) {
        this.total_repayment_amount = total_repayment_amount;
    }

    public BigDecimal getMonthly_payment() {
        return monthly_payment;
    }

    public void setMonthly_payment(BigDecimal monthly_payment) {
        this.monthly_payment = monthly_payment;
    }

    public Date getNext_payment_date() {
        return next_payment_date;
    }

    public void setNext_payment_date(Date next_payment_date) {
        this.next_payment_date = next_payment_date;
    }
}
