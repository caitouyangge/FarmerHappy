// src/main/java/service/farmer/PricePredictionService.java
package service.farmer;

import dto.farmer.PricePredictionResponseDTO;
import util.ExcelParser;
import util.RegressionModel;
import util.TimeSeriesModel;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 价格预测服务
 */
public class PricePredictionService {
    
    // 存储上传的文件数据（实际项目中应使用数据库或缓存）
    private static final Map<String, List<ExcelParser.DataPoint>> fileDataCache = new HashMap<>();
    
    /**
     * 上传并解析Excel文件
     */
    public Map<String, Object> uploadAndParse(InputStream inputStream, String fileName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            ExcelParser parser = new ExcelParser();
            List<ExcelParser.DataPoint> dataPoints = parser.parse(inputStream, fileName);
            
            // 生成文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 缓存数据
            fileDataCache.put(fileId, dataPoints);
            
            // 构建预览数据
            List<Map<String, Object>> previewData = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            
            // 只返回前10条作为预览
            int previewSize = Math.min(10, dataPoints.size());
            for (int i = 0; i < previewSize; i++) {
                ExcelParser.DataPoint point = dataPoints.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("date", sdf.format(point.getDate()));
                item.put("price", point.getPrice());
                previewData.add(item);
            }
            
            result.put("file_id", fileId);
            result.put("preview_data", previewData);
            result.put("total_records", dataPoints.size());
            
        } catch (Exception e) {
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 预测价格
     */
    public PricePredictionResponseDTO predict(String fileId, int predictionDays, String modelType) {
        // 从缓存获取数据
        List<ExcelParser.DataPoint> dataPoints = fileDataCache.get(fileId);
        if (dataPoints == null || dataPoints.isEmpty()) {
            throw new IllegalArgumentException("文件数据不存在或已过期，请重新上传");
        }
        
        // 限制预测天数
        if (predictionDays < 1 || predictionDays > 90) {
            throw new IllegalArgumentException("预测天数必须在1-90天之间");
        }
        
        // 数据预处理：去除异常值
        List<ExcelParser.DataPoint> cleanedData = removeOutliers(dataPoints);
        
        // 准备训练数据（将日期转换为数值）
        List<RegressionModel.Point> trainingData = new ArrayList<>();
        Date firstDate = cleanedData.get(0).getDate();
        long oneDay = 24 * 60 * 60 * 1000; // 一天的毫秒数
        
        for (ExcelParser.DataPoint point : cleanedData) {
            long daysDiff = (point.getDate().getTime() - firstDate.getTime()) / oneDay;
            trainingData.add(new RegressionModel.Point(daysDiff, point.getPrice()));
        }
        
        // 统一使用时间序列模型（准确率最高）
        List<TimeSeriesModel.Point> timeSeriesData = new ArrayList<>();
        for (RegressionModel.Point point : trainingData) {
            timeSeriesData.add(new TimeSeriesModel.Point(point.x, point.y));
        }
        
        // 自动选择最佳alpha参数
        double bestAlpha = TimeSeriesModel.findBestAlpha(timeSeriesData);
        TimeSeriesModel timeSeriesModel = new TimeSeriesModel();
        timeSeriesModel.trainExponentialSmoothing(timeSeriesData, bestAlpha);
        TimeSeriesModel.Metrics timeSeriesMetrics = timeSeriesModel.evaluate();
        
        // 构建历史数据
        List<Map<String, Object>> historicalData = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (ExcelParser.DataPoint point : dataPoints) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", sdf.format(point.getDate()));
            item.put("price", point.getPrice());
            historicalData.add(item);
        }
        
        // 预测未来数据
        List<Map<String, Object>> predictedData = new ArrayList<>();
        Date lastDate = dataPoints.get(dataPoints.size() - 1).getDate();
        long lastDays = (lastDate.getTime() - firstDate.getTime()) / oneDay;
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        
        for (int i = 1; i <= predictionDays; i++) {
            long futureDays = lastDays + i;
            double predictedPrice = timeSeriesModel.predict(futureDays);
            
            // 确保价格不为负
            if (predictedPrice < 0) {
                predictedPrice = 0;
            }
            
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date futureDate = cal.getTime();
            
            Map<String, Object> item = new HashMap<>();
            item.put("date", sdf.format(futureDate));
            item.put("price", Math.round(predictedPrice * 100.0) / 100.0); // 保留两位小数
            predictedData.add(item);
        }
        
        // 构建响应
        PricePredictionResponseDTO response = new PricePredictionResponseDTO();
        response.setHistoricalData(historicalData);
        response.setPredictedData(predictedData);
        
        Map<String, Double> metricsMap = new HashMap<>();
        metricsMap.put("r_squared", Math.round(timeSeriesMetrics.rSquared * 10000.0) / 10000.0);
        metricsMap.put("mae", Math.round(timeSeriesMetrics.mae * 100.0) / 100.0);
        metricsMap.put("rmse", Math.round(timeSeriesMetrics.rmse * 100.0) / 100.0);
        response.setModelMetrics(metricsMap);
        
        // 根据趋势判断价格走势
        String trend = determineTrend(timeSeriesData);
        response.setTrend(trend);
        
        return response;
    }
    
    /**
     * 数据预处理：去除异常值
     */
    private List<ExcelParser.DataPoint> removeOutliers(List<ExcelParser.DataPoint> dataPoints) {
        if (dataPoints.size() < 10) {
            return dataPoints; // 数据太少，不处理异常值
        }
        
        // 计算均值和标准差
        double mean = dataPoints.stream().mapToDouble(ExcelParser.DataPoint::getPrice).average().orElse(0);
        double variance = dataPoints.stream()
            .mapToDouble(p -> Math.pow(p.getPrice() - mean, 2))
            .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // 使用3倍标准差规则去除异常值
        double lowerBound = mean - 3 * stdDev;
        double upperBound = mean + 3 * stdDev;
        
        List<ExcelParser.DataPoint> filtered = new ArrayList<>();
        for (ExcelParser.DataPoint point : dataPoints) {
            if (point.getPrice() >= lowerBound && point.getPrice() <= upperBound) {
                filtered.add(point);
            }
        }
        
        // 如果过滤后数据太少，返回原始数据
        if (filtered.size() < dataPoints.size() * 0.7) {
            return dataPoints;
        }
        
        return filtered;
    }
    
    /**
     * 根据时间序列数据判断趋势
     */
    private String determineTrend(List<TimeSeriesModel.Point> data) {
        if (data == null || data.size() < 2) {
            return "波动";
        }
        
        // 计算最后几个点的平均变化率
        int lookback = Math.min(5, data.size() - 1);
        double sumChange = 0;
        int count = 0;
        
        for (int i = data.size() - lookback; i < data.size() - 1; i++) {
            double xDiff = data.get(i + 1).x - data.get(i).x;
            if (xDiff > 0) {
                double yDiff = data.get(i + 1).y - data.get(i).y;
                sumChange += yDiff / xDiff;
                count++;
            }
        }
        
        if (count == 0) {
            return "波动";
        }
        
        double avgChange = sumChange / count;
        
        if (avgChange > 0.01) {
            return "上升";
        } else if (avgChange < -0.01) {
            return "下降";
        } else {
            return "平稳";
        }
    }
}

