// src/main/java/dto/financing/GetJointLoanMessagesRequestDTO.java
package dto.financing;

public class GetJointLoanMessagesRequestDTO {
    private String phone;
    private String application_id;

    // 构造函数
    public GetJointLoanMessagesRequestDTO() {}

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
}

