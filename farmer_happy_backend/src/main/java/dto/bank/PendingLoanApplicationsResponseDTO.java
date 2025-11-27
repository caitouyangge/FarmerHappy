// src/main/java/dto/bank/PendingLoanApplicationsResponseDTO.java
package dto.bank;

import java.util.List;
import java.util.Map;

public class PendingLoanApplicationsResponseDTO {
    private int total;
    private List<Map<String, Object>> applications;

    // 构造函数
    public PendingLoanApplicationsResponseDTO() {}

    // Getter和Setter方法
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<Map<String, Object>> getApplications() { return applications; }
    public void setApplications(List<Map<String, Object>> applications) { this.applications = applications; }

    @Override
    public String toString() {
        return "PendingLoanApplicationsResponseDTO{" +
                "total=" + total +
                ", applications=" + applications +
                '}';
    }
}
