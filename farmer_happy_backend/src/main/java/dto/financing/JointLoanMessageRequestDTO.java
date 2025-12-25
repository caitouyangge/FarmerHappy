// src/main/java/dto/financing/JointLoanMessageRequestDTO.java
package dto.financing;

public class JointLoanMessageRequestDTO {
    private String phone;
    private String application_id;
    private String receiver_phone;
    private String content;

    // 构造函数
    public JointLoanMessageRequestDTO() {}

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

    public String getReceiver_phone() {
        return receiver_phone;
    }

    public void setReceiver_phone(String receiver_phone) {
        this.receiver_phone = receiver_phone;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

