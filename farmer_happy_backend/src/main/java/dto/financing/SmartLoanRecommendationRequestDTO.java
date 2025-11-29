// src/main/java/dto/financing/SmartLoanRecommendationRequestDTO.java
package dto.financing;

import java.math.BigDecimal;

public class SmartLoanRecommendationRequestDTO {
    private String phone;
    private String product_id;  // 改为String类型，与数据库保持一致
    private BigDecimal apply_amount;

    // 构造函数
    public SmartLoanRecommendationRequestDTO() {}

    // Getter和Setter方法
    public String getPhone() { 
        return phone; 
    }
    
    public void setPhone(String phone) { 
        this.phone = phone; 
    }

    public String getProduct_id() { 
        return product_id; 
    }
    
    public void setProduct_id(String product_id) { 
        this.product_id = product_id; 
    }

    public BigDecimal getApply_amount() { 
        return apply_amount; 
    }
    
    public void setApply_amount(BigDecimal apply_amount) { 
        this.apply_amount = apply_amount; 
    }
}
