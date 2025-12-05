// src/main/java/dto/farmer/PricePredictionResponseDTO.java
package dto.farmer;

import java.util.List;
import java.util.Map;

/**
 * 价格预测响应DTO
 */
public class PricePredictionResponseDTO {
    private List<Map<String, Object>> historicalData;
    private List<Map<String, Object>> predictedData;
    private Map<String, Double> modelMetrics;
    private String trend;
    
    public List<Map<String, Object>> getHistoricalData() {
        return historicalData;
    }
    
    public void setHistoricalData(List<Map<String, Object>> historicalData) {
        this.historicalData = historicalData;
    }
    
    public List<Map<String, Object>> getPredictedData() {
        return predictedData;
    }
    
    public void setPredictedData(List<Map<String, Object>> predictedData) {
        this.predictedData = predictedData;
    }
    
    public Map<String, Double> getModelMetrics() {
        return modelMetrics;
    }
    
    public void setModelMetrics(Map<String, Double> modelMetrics) {
        this.modelMetrics = modelMetrics;
    }
    
    public String getTrend() {
        return trend;
    }
    
    public void setTrend(String trend) {
        this.trend = trend;
    }
}

