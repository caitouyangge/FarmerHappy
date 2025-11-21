// src/main/java/dto/financing/CreditLimitDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CreditLimitDTO {
    private BigDecimal total_limit;
    private BigDecimal used_limit;
    private BigDecimal available_limit;
    private String currency;
    private String status;
    private Timestamp last_updated;

    // 构造函数
    public CreditLimitDTO() {}

    // Getter和Setter方法
    public BigDecimal getTotal_limit() { return total_limit; }
    public void setTotal_limit(BigDecimal total_limit) { this.total_limit = total_limit; }

    public BigDecimal getUsed_limit() { return used_limit; }
    public void setUsed_limit(BigDecimal used_limit) { this.used_limit = used_limit; }

    public BigDecimal getAvailable_limit() { return available_limit; }
    public void setAvailable_limit(BigDecimal available_limit) { this.available_limit = available_limit; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getLast_updated() { return last_updated; }
    public void setLast_updated(Timestamp last_updated) { this.last_updated = last_updated; }
}
