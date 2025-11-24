// src/main/java/dto/financing/JointLoanApplicationRequestDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.util.List;

public class JointLoanApplicationRequestDTO {
    private String phone;
    private String product_id;
    private BigDecimal apply_amount;
    private List<String> partner_phones;
    private String purpose;
    private String repayment_plan;
    private Boolean joint_agreement;

    // 构造函数
    public JointLoanApplicationRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProduct_id() { return product_id; }
    public void setProduct_id(String product_id) { this.product_id = product_id; }

    public BigDecimal getApply_amount() { return apply_amount; }
    public void setApply_amount(BigDecimal apply_amount) { this.apply_amount = apply_amount; }

    public List<String> getPartner_phones() { return partner_phones; }
    public void setPartner_phones(List<String> partner_phones) { this.partner_phones = partner_phones; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRepayment_plan() { return repayment_plan; }
    public void setRepayment_plan(String repayment_plan) { this.repayment_plan = repayment_plan; }

    public Boolean getJoint_agreement() { return joint_agreement; }
    public void setJoint_agreement(Boolean joint_agreement) { this.joint_agreement = joint_agreement; }
}
