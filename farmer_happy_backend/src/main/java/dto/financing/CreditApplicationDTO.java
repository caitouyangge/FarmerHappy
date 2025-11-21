// src/main/java/dto/financing/CreditApplicationDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class CreditApplicationDTO {
    private String application_id;
    private String status;
    private Timestamp created_at;
    private BigDecimal apply_amount;
    private String proof_type;
    private List<String> proof_images;
    private String description;

    // 构造函数
    public CreditApplicationDTO() {}

    // Getter和Setter方法
    public String getApplication_id() { return application_id; }
    public void setApplication_id(String application_id) { this.application_id = application_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    public BigDecimal getApply_amount() { return apply_amount; }
    public void setApply_amount(BigDecimal apply_amount) { this.apply_amount = apply_amount; }

    public String getProof_type() { return proof_type; }
    public void setProof_type(String proof_type) { this.proof_type = proof_type; }

    public List<String> getProof_images() { return proof_images; }
    public void setProof_images(List<String> proof_images) { this.proof_images = proof_images; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
