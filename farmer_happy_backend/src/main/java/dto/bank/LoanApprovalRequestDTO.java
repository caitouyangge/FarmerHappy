// src/main/java/dto/bank/LoanApprovalRequestDTO.java
package dto.bank;

import java.math.BigDecimal;

public class LoanApprovalRequestDTO {
    private String phone;
    private String application_id;
    private String action;
    private BigDecimal approved_amount;
    private String reject_reason;

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApplication_id() {
        return application_id;
    }

    public void setApplication_id(String application_id) {
        this.application_id = application_id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getApproved_amount() {
        return approved_amount;
    }

    public void setApproved_amount(BigDecimal approved_amount) {
        this.approved_amount = approved_amount;
    }

    public String getReject_reason() {
        return reject_reason;
    }

    public void setReject_reason(String reject_reason) {
        this.reject_reason = reject_reason;
    }
}
