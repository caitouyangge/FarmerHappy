// dto/financing/RepaymentScheduleResponseDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.sql.Date;

public class RepaymentScheduleResponseDTO {
    private String loan_id;
    private String loan_status;
    private BigDecimal loan_amount;
    private BigDecimal interest_rate;
    private int term_months;
    private String repayment_method;
    private Date disburse_date;
    private Date maturity_date;
    private Date closed_date;
    private int current_period;
    private int total_periods;
    private BigDecimal remaining_principal;
    private DueInfo current_due;
    private PaymentInfo next_payment;
    private SummaryInfo summary;
    private java.util.List<RepaymentPlanItem> repayment_plan;

    // Getters and Setters
    public String getLoan_id() { return loan_id; }
    public void setLoan_id(String loan_id) { this.loan_id = loan_id; }

    public String getLoan_status() { return loan_status; }
    public void setLoan_status(String loan_status) { this.loan_status = loan_status; }

    public BigDecimal getLoan_amount() { return loan_amount; }
    public void setLoan_amount(BigDecimal loan_amount) { this.loan_amount = loan_amount; }

    public BigDecimal getInterest_rate() { return interest_rate; }
    public void setInterest_rate(BigDecimal interest_rate) { this.interest_rate = interest_rate; }

    public int getTerm_months() { return term_months; }
    public void setTerm_months(int term_months) { this.term_months = term_months; }

    public String getRepayment_method() { return repayment_method; }
    public void setRepayment_method(String repayment_method) { this.repayment_method = repayment_method; }

    public Date getDisburse_date() { return disburse_date; }
    public void setDisburse_date(Date disburse_date) { this.disburse_date = disburse_date; }

    public Date getMaturity_date() { return maturity_date; }
    public void setMaturity_date(Date maturity_date) { this.maturity_date = maturity_date; }

    public Date getClosed_date() { return closed_date; }
    public void setClosed_date(Date closed_date) { this.closed_date = closed_date; }

    public int getCurrent_period() { return current_period; }
    public void setCurrent_period(int current_period) { this.current_period = current_period; }

    public int getTotal_periods() { return total_periods; }
    public void setTotal_periods(int total_periods) { this.total_periods = total_periods; }

    public BigDecimal getRemaining_principal() { return remaining_principal; }
    public void setRemaining_principal(BigDecimal remaining_principal) { this.remaining_principal = remaining_principal; }

    public DueInfo getCurrent_due() { return current_due; }
    public void setCurrent_due(DueInfo current_due) { this.current_due = current_due; }

    public PaymentInfo getNext_payment() { return next_payment; }
    public void setNext_payment(PaymentInfo next_payment) { this.next_payment = next_payment; }

    public SummaryInfo getSummary() { return summary; }
    public void setSummary(SummaryInfo summary) { this.summary = summary; }

    public java.util.List<RepaymentPlanItem> getRepayment_plan() { return repayment_plan; }
    public void setRepayment_plan(java.util.List<RepaymentPlanItem> repayment_plan) { this.repayment_plan = repayment_plan; }

    public static class DueInfo {
        private Date due_date;
        private BigDecimal due_amount;
        private BigDecimal principal_amount;
        private BigDecimal interest_amount;
        private int days_overdue;
        private BigDecimal overdue_interest;

        // Getters and Setters
        public Date getDue_date() { return due_date; }
        public void setDue_date(Date due_date) { this.due_date = due_date; }

        public BigDecimal getDue_amount() { return due_amount; }
        public void setDue_amount(BigDecimal due_amount) { this.due_amount = due_amount; }

        public BigDecimal getPrincipal_amount() { return principal_amount; }
        public void setPrincipal_amount(BigDecimal principal_amount) { this.principal_amount = principal_amount; }

        public BigDecimal getInterest_amount() { return interest_amount; }
        public void setInterest_amount(BigDecimal interest_amount) { this.interest_amount = interest_amount; }

        public int getDays_overdue() { return days_overdue; }
        public void setDays_overdue(int days_overdue) { this.days_overdue = days_overdue; }

        public BigDecimal getOverdue_interest() { return overdue_interest; }
        public void setOverdue_interest(BigDecimal overdue_interest) { this.overdue_interest = overdue_interest; }
    }

    public static class PaymentInfo {
        private Date payment_date;
        private BigDecimal payment_amount;
        private BigDecimal principal_amount;
        private BigDecimal interest_amount;

        // Getters and Setters
        public Date getPayment_date() { return payment_date; }
        public void setPayment_date(Date payment_date) { this.payment_date = payment_date; }

        public BigDecimal getPayment_amount() { return payment_amount; }
        public void setPayment_amount(BigDecimal payment_amount) { this.payment_amount = payment_amount; }

        public BigDecimal getPrincipal_amount() { return principal_amount; }
        public void setPrincipal_amount(BigDecimal principal_amount) { this.principal_amount = principal_amount; }

        public BigDecimal getInterest_amount() { return interest_amount; }
        public void setInterest_amount(BigDecimal interest_amount) { this.interest_amount = interest_amount; }
    }

    public static class SummaryInfo {
        private BigDecimal total_paid;
        private BigDecimal principal_paid;
        private BigDecimal interest_paid;
        private BigDecimal remaining_total;
        private BigDecimal remaining_principal;
        private BigDecimal remaining_interest;

        // Getters and Setters
        public BigDecimal getTotal_paid() { return total_paid; }
        public void setTotal_paid(BigDecimal total_paid) { this.total_paid = total_paid; }

        public BigDecimal getPrincipal_paid() { return principal_paid; }
        public void setPrincipal_paid(BigDecimal principal_paid) { this.principal_paid = principal_paid; }

        public BigDecimal getInterest_paid() { return interest_paid; }
        public void setInterest_paid(BigDecimal interest_paid) { this.interest_paid = interest_paid; }

        public BigDecimal getRemaining_total() { return remaining_total; }
        public void setRemaining_total(BigDecimal remaining_total) { this.remaining_total = remaining_total; }

        public BigDecimal getRemaining_principal() { return remaining_principal; }
        public void setRemaining_principal(BigDecimal remaining_principal) { this.remaining_principal = remaining_principal; }

        public BigDecimal getRemaining_interest() { return remaining_interest; }
        public void setRemaining_interest(BigDecimal remaining_interest) { this.remaining_interest = remaining_interest; }
    }

    public static class RepaymentPlanItem {
        private int period;
        private Date due_date;
        private BigDecimal principal;
        private BigDecimal interest;
        private BigDecimal total;
        private String status; // pending, paid, overdue

        // Getters and Setters
        public int getPeriod() { return period; }
        public void setPeriod(int period) { this.period = period; }

        public Date getDue_date() { return due_date; }
        public void setDue_date(Date due_date) { this.due_date = due_date; }

        public BigDecimal getPrincipal() { return principal; }
        public void setPrincipal(BigDecimal principal) { this.principal = principal; }

        public BigDecimal getInterest() { return interest; }
        public void setInterest(BigDecimal interest) { this.interest = interest; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
