// src/main/java/dto/bank/PendingCreditApplicationsResponseDTO.java
package dto.bank;

import java.util.List;
import java.util.Map;

public class PendingCreditApplicationsResponseDTO {
    private int total;
    private List<Map<String, Object>> applications;

    // 构造函数
    public PendingCreditApplicationsResponseDTO() {}

    // Getter和Setter方法
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<Map<String, Object>> getApplications() { return applications; }
    public void setApplications(List<Map<String, Object>> applications) { this.applications = applications; }

    @Override
    public String toString() {
        return "PendingCreditApplicationsResponseDTO{" +
                "total=" + total +
                ", applications=" + applications +
                '}';
    }
}
