// entity/financing/Repayment.java
package entity.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Repayment {
    private Long id;
    private String repaymentId;
    private Long loanId;
    private Long farmerId;
    private BigDecimal repaymentAmount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private String repaymentMethod;
    private String paymentAccount;
    private String remarks;
    private Timestamp repaymentDate;
    private Timestamp createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRepaymentId() { return repaymentId; }
    public void setRepaymentId(String repaymentId) { this.repaymentId = repaymentId; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }

    public BigDecimal getRepaymentAmount() { return repaymentAmount; }
    public void setRepaymentAmount(BigDecimal repaymentAmount) { this.repaymentAmount = repaymentAmount; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String repaymentMethod) { this.repaymentMethod = repaymentMethod; }

    public String getPaymentAccount() { return paymentAccount; }
    public void setPaymentAccount(String paymentAccount) { this.paymentAccount = paymentAccount; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Timestamp getRepaymentDate() { return repaymentDate; }
    public void setRepaymentDate(Timestamp repaymentDate) { this.repaymentDate = repaymentDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
