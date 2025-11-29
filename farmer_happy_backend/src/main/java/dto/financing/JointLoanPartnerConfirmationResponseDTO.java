// src/main/java/dto/financing/JointLoanPartnerConfirmationResponseDTO.java
package dto.financing;

public class JointLoanPartnerConfirmationResponseDTO {
    private String application_id;
    private String partner_phone;
    private String action_result; // "confirmed" 或 "rejected"
    private String message;
    private String next_step;

    // 构造函数
    public JointLoanPartnerConfirmationResponseDTO() {}

    // Getter和Setter方法
    public String getApplication_id() {
        return application_id;
    }

    public void setApplication_id(String application_id) {
        this.application_id = application_id;
    }

    public String getPartner_phone() {
        return partner_phone;
    }

    public void setPartner_phone(String partner_phone) {
        this.partner_phone = partner_phone;
    }

    public String getAction_result() {
        return action_result;
    }

    public void setAction_result(String action_result) {
        this.action_result = action_result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNext_step() {
        return next_step;
    }

    public void setNext_step(String next_step) {
        this.next_step = next_step;
    }
}
