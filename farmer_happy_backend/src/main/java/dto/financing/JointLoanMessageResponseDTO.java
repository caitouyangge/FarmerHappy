// src/main/java/dto/financing/JointLoanMessageResponseDTO.java
package dto.financing;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class JointLoanMessageResponseDTO {
    private String application_id;
    private List<Map<String, Object>> messages;

    // 构造函数
    public JointLoanMessageResponseDTO() {}

    // Getter和Setter方法
    public String getApplication_id() {
        return application_id;
    }

    public void setApplication_id(String application_id) {
        this.application_id = application_id;
    }

    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    public void setMessages(List<Map<String, Object>> messages) {
        this.messages = messages;
    }
}

