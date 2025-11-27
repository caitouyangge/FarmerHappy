// src/main/java/dto/bank/CreditApprovalResponseDTO.java
package dto.bank;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CreditApprovalResponseDTO {
    private String application_id;
    private String status;
    private BigDecimal approved_amount;
    private String approved_by;
    private Timestamp approved_at;
    private String reject_reason;
    private String rejected_by;
    private Timestamp rejected_at;

    // 构造函数
    public CreditApprovalResponseDTO() {}

    // Getter和Setter方法
    public String getApplication_id() { return application_id; }
    public void setApplication_id(String application_id) { this.application_id = application_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getApproved_amount() { return approved_amount; }
    public void setApproved_amount(BigDecimal approved_amount) { this.approved_amount = approved_amount; }

    public String getApproved_by() { return approved_by; }
    public void setApproved_by(String approved_by) { this.approved_by = approved_by; }

    public Timestamp getApproved_at() { return approved_at; }
    public void setApproved_at(Timestamp approved_at) { this.approved_at = approved_at; }

    public String getReject_reason() { return reject_reason; }
    public void setReject_reason(String reject_reason) { this.reject_reason = reject_reason; }

    public String getRejected_by() { return rejected_by; }
    public void setRejected_by(String rejected_by) { this.rejected_by = rejected_by; }

    public Timestamp getRejected_at() { return rejected_at; }
    public void setRejected_at(Timestamp rejected_at) { this.rejected_at = rejected_at; }
}
