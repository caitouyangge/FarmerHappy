// src/main/java/dto/financing/PartnersResponseDTO.java
package dto.financing;

import java.util.List;

public class PartnersResponseDTO {
    private int total;
    private List<PartnerItemDTO> partners;
    private String recommendation_reason;

    // Getters and Setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<PartnerItemDTO> getPartners() {
        return partners;
    }

    public void setPartners(List<PartnerItemDTO> partners) {
        this.partners = partners;
    }

    public String getRecommendation_reason() {
        return recommendation_reason;
    }

    public void setRecommendation_reason(String recommendation_reason) {
        this.recommendation_reason = recommendation_reason;
    }
}
