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
    private List<Map<String, Object>> seriesData; // 多条规格/类型曲线数据
    private Map<String, Double> modelMetrics;
    private String trend;
    private Map<String, Object> calculationDetails; // 详细计算过程
    
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

    public List<Map<String, Object>> getSeriesData() {
        return seriesData;
    }

    public void setSeriesData(List<Map<String, Object>> seriesData) {
        this.seriesData = seriesData;
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
    
    public Map<String, Object> getCalculationDetails() {
        return calculationDetails;
    }
    
    public void setCalculationDetails(Map<String, Object> calculationDetails) {
        this.calculationDetails = calculationDetails;
    }
}



