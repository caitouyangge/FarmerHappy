// src/main/java/dto/bank/LoanDisbursementRequestDTO.java
package dto.bank;

import java.math.BigDecimal;

public class LoanDisbursementRequestDTO {
    private String phone;
    private String application_id;
    private BigDecimal disburse_amount;
    private String remarks;

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

    public BigDecimal getDisburse_amount() {
        return disburse_amount;
    }

    public void setDisburse_amount(BigDecimal disburse_amount) {
        this.disburse_amount = disburse_amount;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
