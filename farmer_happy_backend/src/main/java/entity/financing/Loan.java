// src/main/java/entity/financing/Loan.java
package entity.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Date;

public class Loan {
    private Long id;
    private String loanId;
    private Long farmerId;
    private Long productId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private int termMonths;
    private String repaymentMethod;
    private BigDecimal disburseAmount;
    private String disburseMethod;
    private Timestamp disburseDate;
    private Date firstRepaymentDate;
    private String loanAccount;
    private String disburseRemarks;
    private String loanStatus;
    private Long approvedBy;
    private Timestamp approvedAt;
    private String rejectReason;
    private Timestamp closedDate;
    private BigDecimal totalRepaymentAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalPaidPrincipal;
    private BigDecimal totalPaidInterest;
    private BigDecimal remainingPrincipal;
    private int currentPeriod;
    private Date nextPaymentDate;
    private BigDecimal nextPaymentAmount;
    private int overdueDays;
    private BigDecimal overdueAmount;
    private String repaymentSchedule;
    private String purpose;
    private String repaymentSource;
    private Boolean isJointLoan;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public Long getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(Long farmerId) {
        this.farmerId = farmerId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(int termMonths) {
        this.termMonths = termMonths;
    }

    public String getRepaymentMethod() {
        return repaymentMethod;
    }

    public void setRepaymentMethod(String repaymentMethod) {
        this.repaymentMethod = repaymentMethod;
    }

    public BigDecimal getDisburseAmount() {
        return disburseAmount;
    }

    public void setDisburseAmount(BigDecimal disburseAmount) {
        this.disburseAmount = disburseAmount;
    }

    public String getDisburseMethod() {
        return disburseMethod;
    }

    public void setDisburseMethod(String disburseMethod) {
        this.disburseMethod = disburseMethod;
    }

    public Timestamp getDisburseDate() {
        return disburseDate;
    }

    public void setDisburseDate(Timestamp disburseDate) {
        this.disburseDate = disburseDate;
    }

    public Date getFirstRepaymentDate() {
        return firstRepaymentDate;
    }

    public void setFirstRepaymentDate(Date firstRepaymentDate) {
        this.firstRepaymentDate = firstRepaymentDate;
    }

    public String getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(String loanAccount) {
        this.loanAccount = loanAccount;
    }

    public String getDisburseRemarks() {
        return disburseRemarks;
    }

    public void setDisburseRemarks(String disburseRemarks) {
        this.disburseRemarks = disburseRemarks;
    }

    public String getLoanStatus() {
        return loanStatus;
    }

    public void setLoanStatus(String loanStatus) {
        this.loanStatus = loanStatus;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Timestamp approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Timestamp getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Timestamp closedDate) {
        this.closedDate = closedDate;
    }

    public BigDecimal getTotalRepaymentAmount() {
        return totalRepaymentAmount;
    }

    public void setTotalRepaymentAmount(BigDecimal totalRepaymentAmount) {
        this.totalRepaymentAmount = totalRepaymentAmount;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public BigDecimal getTotalPaidPrincipal() {
        return totalPaidPrincipal;
    }

    public void setTotalPaidPrincipal(BigDecimal totalPaidPrincipal) {
        this.totalPaidPrincipal = totalPaidPrincipal;
    }

    public BigDecimal getTotalPaidInterest() {
        return totalPaidInterest;
    }

    public void setTotalPaidInterest(BigDecimal totalPaidInterest) {
        this.totalPaidInterest = totalPaidInterest;
    }

    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(BigDecimal remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public int getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(int currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public Date getNextPaymentDate() {
        return nextPaymentDate;
    }

    public void setNextPaymentDate(Date nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public BigDecimal getNextPaymentAmount() {
        return nextPaymentAmount;
    }

    public void setNextPaymentAmount(BigDecimal nextPaymentAmount) {
        this.nextPaymentAmount = nextPaymentAmount;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public String getRepaymentSchedule() {
        return repaymentSchedule;
    }

    public void setRepaymentSchedule(String repaymentSchedule) {
        this.repaymentSchedule = repaymentSchedule;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getRepaymentSource() {
        return repaymentSource;
    }

    public void setRepaymentSource(String repaymentSource) {
        this.repaymentSource = repaymentSource;
    }

    public Boolean getIsJointLoan() {
        return isJointLoan;
    }

    public void setIsJointLoan(Boolean isJointLoan) {
        this.isJointLoan = isJointLoan;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
