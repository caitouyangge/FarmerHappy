// src/main/java/entity/financing/LoanProduct.java
package entity.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class LoanProduct {
    private Long id;
    private String productId;
    private String productCode;
    private String productName;
    private BigDecimal minCreditLimit;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private String repaymentMethod;
    private String description;
    private String status;
    private Long bankId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 构造函数
    public LoanProduct() {}

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getMinCreditLimit() { return minCreditLimit; }
    public void setMinCreditLimit(BigDecimal minCreditLimit) { this.minCreditLimit = minCreditLimit; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String repaymentMethod) { this.repaymentMethod = repaymentMethod; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
