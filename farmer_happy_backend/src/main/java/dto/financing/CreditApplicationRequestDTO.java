// src/main/java/dto/financing/CreditApplicationRequestDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.util.List;

public class CreditApplicationRequestDTO {
    private String phone;
    private String proof_type;
    private List<String> proof_images;
    private BigDecimal apply_amount;
    private String description;

    // 构造函数
    public CreditApplicationRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProof_type() { return proof_type; }
    public void setProof_type(String proof_type) { this.proof_type = proof_type; }

    public List<String> getProof_images() { return proof_images; }
    public void setProof_images(List<String> proof_images) { this.proof_images = proof_images; }

    public BigDecimal getApply_amount() { return apply_amount; }
    public void setApply_amount(BigDecimal apply_amount) { this.apply_amount = apply_amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
