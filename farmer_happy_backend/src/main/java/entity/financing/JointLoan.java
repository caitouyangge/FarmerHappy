// src/main/java/entity/financing/JointLoan.java
package entity.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class JointLoan {
    private Long id;
    private Long loanId;
    private Long partnerFarmerId;
    private BigDecimal partnerShareRatio;
    private BigDecimal partnerShareAmount;
    private BigDecimal partnerPrincipal;
    private BigDecimal partnerInterest;
    private BigDecimal partnerTotalRepayment;
    private BigDecimal partnerPaidAmount;
    private BigDecimal partnerRemainingPrincipal;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getPartnerFarmerId() {
        return partnerFarmerId;
    }

    public void setPartnerFarmerId(Long partnerFarmerId) {
        this.partnerFarmerId = partnerFarmerId;
    }

    public BigDecimal getPartnerShareRatio() {
        return partnerShareRatio;
    }

    public void setPartnerShareRatio(BigDecimal partnerShareRatio) {
        this.partnerShareRatio = partnerShareRatio;
    }

    public BigDecimal getPartnerShareAmount() {
        return partnerShareAmount;
    }

    public void setPartnerShareAmount(BigDecimal partnerShareAmount) {
        this.partnerShareAmount = partnerShareAmount;
    }

    public BigDecimal getPartnerPrincipal() {
        return partnerPrincipal;
    }

    public void setPartnerPrincipal(BigDecimal partnerPrincipal) {
        this.partnerPrincipal = partnerPrincipal;
    }

    public BigDecimal getPartnerInterest() {
        return partnerInterest;
    }

    public void setPartnerInterest(BigDecimal partnerInterest) {
        this.partnerInterest = partnerInterest;
    }

    public BigDecimal getPartnerTotalRepayment() {
        return partnerTotalRepayment;
    }

    public void setPartnerTotalRepayment(BigDecimal partnerTotalRepayment) {
        this.partnerTotalRepayment = partnerTotalRepayment;
    }

    public BigDecimal getPartnerPaidAmount() {
        return partnerPaidAmount;
    }

    public void setPartnerPaidAmount(BigDecimal partnerPaidAmount) {
        this.partnerPaidAmount = partnerPaidAmount;
    }

    public BigDecimal getPartnerRemainingPrincipal() {
        return partnerRemainingPrincipal;
    }

    public void setPartnerRemainingPrincipal(BigDecimal partnerRemainingPrincipal) {
        this.partnerRemainingPrincipal = partnerRemainingPrincipal;
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
