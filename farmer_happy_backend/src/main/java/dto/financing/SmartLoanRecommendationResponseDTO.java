// src/main/java/dto/financing/SmartLoanRecommendationResponseDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.util.List;

public class SmartLoanRecommendationResponseDTO {
    private String recommendation_type; // "single" 或 "joint"
    private String recommendation_reason;
    private BigDecimal user_available_limit;
    private BigDecimal apply_amount;
    private boolean can_apply_single;
    private boolean can_apply_joint;
    private List<PartnerItemDTO> recommended_partners; // 推荐的合作伙伴

    // 构造函数
    public SmartLoanRecommendationResponseDTO() {}

    // Getter和Setter方法
    public String getRecommendation_type() {
        return recommendation_type;
    }

    public void setRecommendation_type(String recommendation_type) {
        this.recommendation_type = recommendation_type;
    }

    public String getRecommendation_reason() {
        return recommendation_reason;
    }

    public void setRecommendation_reason(String recommendation_reason) {
        this.recommendation_reason = recommendation_reason;
    }

    public BigDecimal getUser_available_limit() {
        return user_available_limit;
    }

    public void setUser_available_limit(BigDecimal user_available_limit) {
        this.user_available_limit = user_available_limit;
    }

    public BigDecimal getApply_amount() {
        return apply_amount;
    }

    public void setApply_amount(BigDecimal apply_amount) {
        this.apply_amount = apply_amount;
    }

    public boolean isCan_apply_single() {
        return can_apply_single;
    }

    public void setCan_apply_single(boolean can_apply_single) {
        this.can_apply_single = can_apply_single;
    }

    public boolean isCan_apply_joint() {
        return can_apply_joint;
    }

    public void setCan_apply_joint(boolean can_apply_joint) {
        this.can_apply_joint = can_apply_joint;
    }

    public List<PartnerItemDTO> getRecommended_partners() {
        return recommended_partners;
    }

    public void setRecommended_partners(List<PartnerItemDTO> recommended_partners) {
        this.recommended_partners = recommended_partners;
    }
}
