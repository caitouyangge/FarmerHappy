// src/main/java/dto/financing/PartnerInfoDTO.java
package dto.financing;

import java.math.BigDecimal;
import java.util.List;

public class PartnerInfoDTO {
    private String phone;
    private String nickname;
    private String avatar;
    private BigDecimal credit_limit;
    private BigDecimal available_limit;
    private Integer loan_count;
    private Integer repayment_rate;
    private String location;
    private List<String> crops;
    private Integer cooperation_score;
    private String status;

    // 构造函数
    public PartnerInfoDTO() {}

    // Getter和Setter方法
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public BigDecimal getCredit_limit() { return credit_limit; }
    public void setCredit_limit(BigDecimal credit_limit) { this.credit_limit = credit_limit; }

    public BigDecimal getAvailable_limit() { return available_limit; }
    public void setAvailable_limit(BigDecimal available_limit) { this.available_limit = available_limit; }

    public Integer getLoan_count() { return loan_count; }
    public void setLoan_count(Integer loan_count) { this.loan_count = loan_count; }

    public Integer getRepayment_rate() { return repayment_rate; }
    public void setRepayment_rate(Integer repayment_rate) { this.repayment_rate = repayment_rate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<String> getCrops() { return crops; }
    public void setCrops(List<String> crops) { this.crops = crops; }

    public Integer getCooperation_score() { return cooperation_score; }
    public void setCooperation_score(Integer cooperation_score) { this.cooperation_score = cooperation_score; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
