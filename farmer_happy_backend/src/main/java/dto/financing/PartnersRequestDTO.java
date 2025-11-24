// src/main/java/dto/financing/PartnersRequestDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.util.List;

public class PartnersRequestDTO {
    private String phone;
    private BigDecimal min_credit_limit;
    private Integer max_partners;
    private List<String> exclude_phones;

    // 构造函数
    public PartnersRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getMin_credit_limit() { return min_credit_limit; }
    public void setMin_credit_limit(BigDecimal min_credit_limit) { this.min_credit_limit = min_credit_limit; }

    public Integer getMax_partners() { return max_partners; }
    public void setMax_partners(Integer max_partners) { this.max_partners = max_partners; }

    public List<String> getExclude_phones() { return exclude_phones; }
    public void setExclude_phones(List<String> exclude_phones) { this.exclude_phones = exclude_phones; }
}
