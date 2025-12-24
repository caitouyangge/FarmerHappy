// dto/auth/RechargeRequestDTO.java
package dto.auth;

import java.math.BigDecimal;

public class RechargeRequestDTO {
    private String phone;
    private String userType;
    private BigDecimal amount;

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

