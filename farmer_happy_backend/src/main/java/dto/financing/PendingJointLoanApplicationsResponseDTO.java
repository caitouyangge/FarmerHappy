// src/main/java/dto/financing/PendingJointLoanApplicationsResponseDTO.java
package dto.financing;

import java.util.List;
import java.util.Map;

public class PendingJointLoanApplicationsResponseDTO {
    private int total;
    private List<Map<String, Object>> applications;

    // 构造函数
    public PendingJointLoanApplicationsResponseDTO() {}

    // Getter和Setter方法
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<Map<String, Object>> getApplications() {
        return applications;
    }

    public void setApplications(List<Map<String, Object>> applications) {
        this.applications = applications;
    }
}
