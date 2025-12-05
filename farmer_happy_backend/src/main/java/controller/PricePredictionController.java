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
     * 上传Excel文件
     */
    public Map<String, Object> uploadExcel(InputStream inputStream, String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件类型
            if (fileName == null || (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx"))) {
                response.put("code", 400);
                response.put("message", "不支持的文件格式，仅支持.xls和.xlsx文件");
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
            // 统一使用时间序列模型，忽略用户指定的modelType
            String modelType = "timeseries";
            
            PricePredictionResponseDTO result = pricePredictionService.predict(
                request.getFileId(),
                predictionDays,
                modelType
            );
            
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

