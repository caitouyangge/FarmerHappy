// src/main/java/dto/financing/PartnerItemDTO.java
package dto.financing;

import java.math.BigDecimal;

public class PartnerItemDTO {
    private String phone;
    private String nickname;
    private BigDecimal available_credit_limit;
    private BigDecimal total_credit_limit;

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public BigDecimal getAvailable_credit_limit() {
        return available_credit_limit;
    }

    public void setAvailable_credit_limit(BigDecimal available_credit_limit) {
        this.available_credit_limit = available_credit_limit;
    }

    public BigDecimal getTotal_credit_limit() {
        return total_credit_limit;
    }

    public void setTotal_credit_limit(BigDecimal total_credit_limit) {
        this.total_credit_limit = total_credit_limit;
    }
}
