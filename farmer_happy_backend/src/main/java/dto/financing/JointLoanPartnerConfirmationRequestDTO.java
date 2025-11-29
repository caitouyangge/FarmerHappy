// src/main/java/dto/financing/JointLoanPartnerConfirmationRequestDTO.java
package dto.financing;

public class JointLoanPartnerConfirmationRequestDTO {
    private String phone;
    private String application_id;
    private String action; // "confirm" 或 "reject"

    // 构造函数
    public JointLoanPartnerConfirmationRequestDTO() {}

    // Getter和Setter方法
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
}
