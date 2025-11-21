// src/main/java/entity/financing/CreditLimit.java
package entity.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CreditLimit {
    private Long id;
    private Long farmerId;
    private BigDecimal totalLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private String currency;
    private String status;
    private Timestamp lastUpdated;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 构造函数
    public CreditLimit() {}

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }

    public BigDecimal getTotalLimit() { return totalLimit; }
    public void setTotalLimit(BigDecimal totalLimit) { this.totalLimit = totalLimit; }

    public BigDecimal getUsedLimit() { return usedLimit; }
    public void setUsedLimit(BigDecimal usedLimit) { this.usedLimit = usedLimit; }

    public BigDecimal getAvailableLimit() { return availableLimit; }
    public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
