// src/main/java/controller/PricePredictionController.java
package controller;

import dto.farmer.PricePredictionRequestDTO;
import dto.farmer.PricePredictionResponseDTO;
import service.farmer.PricePredictionService;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 价格预测控制器
 */
public class PricePredictionController {
    
    private final PricePredictionService pricePredictionService;
    
    public PricePredictionController() {
        this.pricePredictionService = new PricePredictionService();
    }
    
    /**
     * 上传价格文件（Excel/CSV）
     */
    public Map<String, Object> uploadExcel(InputStream inputStream, String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件类型
            String lower = fileName != null ? fileName.toLowerCase() : null;
            if (lower == null || (!lower.endsWith(".xls") && !lower.endsWith(".xlsx") && !lower.endsWith(".csv"))) {
                response.put("code", 400);
                response.put("message", "不支持的文件格式，仅支持.xls、.xlsx、.csv文件");
                return response;
            }
            
            // 验证文件大小（限制10MB）
            // 注意：这里无法直接获取文件大小，需要在RouterConfig中处理
            
            Map<String, Object> result = pricePredictionService.uploadAndParse(inputStream, fileName);
            
            response.put("code", 200);
            response.put("message", "文件上传成功");
            response.put("data", result);
            
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 预测价格
     */
    public Map<String, Object> predictPrice(PricePredictionRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 参数验证
            if (request.getFileId() == null || request.getFileId().trim().isEmpty()) {
                response.put("code", 400);
                response.put("message", "fileId不能为空");
                return response;
            }
            
            int predictionDays = request.getPredictionDays() != null ? request.getPredictionDays() : 30;
            // 支持timeseries和ai两种模型类型
            String modelType = request.getModelType() != null ? request.getModelType() : "timeseries";
            if (!"timeseries".equals(modelType) && !"ai".equals(modelType)) {
                modelType = "timeseries"; // 默认使用timeseries
            }
            
            PricePredictionResponseDTO result = pricePredictionService.predict(
                request.getFileId(),
                predictionDays,
                modelType
            );
            
            // 调试：检查calculationDetails是否设置
            if (result.getCalculationDetails() != null) {
                System.out.println("CalculationDetails已设置，包含 " + result.getCalculationDetails().size() + " 个键");
                System.out.println("键列表: " + result.getCalculationDetails().keySet());
            } else {
                System.out.println("警告: CalculationDetails为null!");
            }
            
            response.put("code", 200);
            response.put("message", "预测成功");
            response.put("data", result);
            
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        
        return response;
    }
}

