// src/main/java/dto/financing/JointPartnerDTO.java
package dto.financing;

import java.sql.Timestamp;

public class JointPartnerDTO {
    private String phone;
    private String status;
    private Timestamp invited_at;

    // 构造函数
    public JointPartnerDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getInvited_at() { return invited_at; }
    public void setInvited_at(Timestamp invited_at) { this.invited_at = invited_at; }
}
