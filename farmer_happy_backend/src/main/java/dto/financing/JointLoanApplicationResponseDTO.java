// src/main/java/dto/financing/JointLoanApplicationResponseDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class JointLoanApplicationResponseDTO {
    private String loan_application_id;
    private String status;
    private String product_name;
    private BigDecimal apply_amount;
    private String initiator_phone;
    private List<JointPartnerDTO> partners;
    private Timestamp created_at;
    private String next_step;

    // 构造函数
    public JointLoanApplicationResponseDTO() {}

    // Getter和Setter方法
    public String getLoan_application_id() { return loan_application_id; }
    public void setLoan_application_id(String loan_application_id) { this.loan_application_id = loan_application_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProduct_name() { return product_name; }
    public void setProduct_name(String product_name) { this.product_name = product_name; }

    public BigDecimal getApply_amount() { return apply_amount; }
    public void setApply_amount(BigDecimal apply_amount) { this.apply_amount = apply_amount; }

    public String getInitiator_phone() { return initiator_phone; }
    public void setInitiator_phone(String initiator_phone) { this.initiator_phone = initiator_phone; }

    public List<JointPartnerDTO> getPartners() { return partners; }
    public void setPartners(List<JointPartnerDTO> partners) { this.partners = partners; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public String getNext_step() { return next_step; }
    public void setNext_step(String next_step) { this.next_step = next_step; }
}
