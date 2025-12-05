// src/main/java/dto/farmer/PricePredictionRequestDTO.java
package dto.farmer;

/**
 * 价格预测请求DTO
 */
public class PricePredictionRequestDTO {
    private String fileId;
    private Integer predictionDays;
    private String modelType; // linear, polynomial_2, polynomial_3
    
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public Integer getPredictionDays() {
        return predictionDays;
    }
    
    public void setPredictionDays(Integer predictionDays) {
        this.predictionDays = predictionDays;
    }
    
    public String getModelType() {
        return modelType;
    }
    
    public void setModelType(String modelType) {
        this.modelType = modelType;
    }
}

