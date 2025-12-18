// src/main/java/service/farmer/PricePredictionService.java
package service.farmer;

import dto.farmer.PricePredictionResponseDTO;
import util.ExcelParser;
import util.RegressionModel;
import util.ARIMAModel;

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
        
        // 使用ARIMA模型
        List<ARIMAModel.Point> arimaData = new ArrayList<>();
        for (RegressionModel.Point point : trainingData) {
            arimaData.add(new ARIMAModel.Point(point.x, point.y));
        }
        
        // 自动选择ARIMA参数
        ARIMAModel.ARIMAParams arimaParams = ARIMAModel.autoSelectParams(arimaData);
        ARIMAModel arimaModel = new ARIMAModel();
        arimaModel.train(arimaData, arimaParams);
        ARIMAModel.Metrics arimaMetrics = arimaModel.evaluate();
        
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
        List<Double> futureXValues = new ArrayList<>();
        Date lastDate = dataPoints.get(dataPoints.size() - 1).getDate();
        long lastDays = (lastDate.getTime() - firstDate.getTime()) / oneDay;
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        
        for (int i = 1; i <= predictionDays; i++) {
            long futureDays = lastDays + i;
            futureXValues.add((double)futureDays);
            double predictedPrice = arimaModel.predict(i);
            
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
        
        // 构建详细计算过程
        Map<String, Object> calculationDetails = new HashMap<>();
        
        // 数据预处理详情
        Map<String, Object> preprocessingDetails = new HashMap<>();
        preprocessingDetails.put("original_count", dataPoints.size());
        preprocessingDetails.put("cleaned_count", cleanedData.size());
        preprocessingDetails.put("removed_count", dataPoints.size() - cleanedData.size());
        if (dataPoints.size() != cleanedData.size()) {
            double mean = dataPoints.stream().mapToDouble(ExcelParser.DataPoint::getPrice).average().orElse(0);
            double variance = dataPoints.stream()
                .mapToDouble(p -> Math.pow(p.getPrice() - mean, 2))
                .average().orElse(0);
            double stdDev = Math.sqrt(variance);
            preprocessingDetails.put("mean", Math.round(mean * 100.0) / 100.0);
            preprocessingDetails.put("std_dev", Math.round(stdDev * 100.0) / 100.0);
            preprocessingDetails.put("lower_bound", Math.round((mean - 3 * stdDev) * 100.0) / 100.0);
            preprocessingDetails.put("upper_bound", Math.round((mean + 3 * stdDev) * 100.0) / 100.0);
            preprocessingDetails.put("method", "3倍标准差规则");
        } else {
            preprocessingDetails.put("method", "数据点少于10个，未进行异常值过滤");
        }
        calculationDetails.put("preprocessing", preprocessingDetails);
        
        // ARIMA模型参数详情
        Map<String, Object> arimaParamsDetails = new HashMap<>();
        arimaParamsDetails.put("model_type", arimaParams.toString());
        arimaParamsDetails.put("p", arimaParams.p);
        arimaParamsDetails.put("d", arimaParams.d);
        arimaParamsDetails.put("q", arimaParams.q);
        arimaParamsDetails.put("is_seasonal", arimaParams.isSeasonal());
        if (arimaParams.isSeasonal()) {
            arimaParamsDetails.put("P", arimaParams.P);
            arimaParamsDetails.put("D", arimaParams.D);
            arimaParamsDetails.put("Q", arimaParams.Q);
            arimaParamsDetails.put("s", arimaParams.s);
        }
        arimaParamsDetails.put("method", "自动选择：基于数据特征自动确定最优参数");
        calculationDetails.put("arima_params", arimaParamsDetails);
        
        // ARIMA模型计算详情
        Map<String, Object> modelDetails = getARIMACalculationDetails(arimaModel, arimaData, arimaParams);
        calculationDetails.put("model_calculation", modelDetails);
        
        // 预测过程详情
        List<Map<String, Object>> predictionDetails = new ArrayList<>();
        cal.setTime(lastDate);
        for (int i = 0; i < predictionDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            double predictedPrice = arimaModel.predict(i + 1);
            
            Map<String, Object> detail = new HashMap<>();
            detail.put("step", i + 1);
            detail.put("date", sdf.format(cal.getTime()));
            detail.put("predicted_price", Math.round(predictedPrice * 100.0) / 100.0);
            detail.put("formula", String.format("ARIMA预测：使用AR(%d)和MA(%d)模型，差分次数d=%d", 
                arimaParams.p, arimaParams.q, arimaParams.d));
            predictionDetails.add(detail);
        }
        calculationDetails.put("prediction_steps", predictionDetails);
        
        // 构建响应
        PricePredictionResponseDTO response = new PricePredictionResponseDTO();
        response.setHistoricalData(historicalData);
        response.setPredictedData(predictedData);
        
        Map<String, Double> metricsMap = new HashMap<>();
        metricsMap.put("r_squared", Math.round(arimaMetrics.rSquared * 10000.0) / 10000.0);
        metricsMap.put("mae", Math.round(arimaMetrics.mae * 100.0) / 100.0);
        metricsMap.put("rmse", Math.round(arimaMetrics.rmse * 100.0) / 100.0);
        metricsMap.put("aic", Math.round(arimaMetrics.aic * 100.0) / 100.0);
        response.setModelMetrics(metricsMap);
        
        // 根据趋势判断价格走势
        String trend = determineTrendFromARIMA(arimaData);
        response.setTrend(trend);
        
        // 设置详细计算过程
        response.setCalculationDetails(calculationDetails);
        
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
     * 获取ARIMA模型的计算详情
     */
    private Map<String, Object> getARIMACalculationDetails(ARIMAModel model, 
                                                             List<ARIMAModel.Point> data,
                                                             ARIMAModel.ARIMAParams params) {
        Map<String, Object> details = new HashMap<>();
        
        // 模型参数
        details.put("model_type", params.toString());
        details.put("p", params.p);
        details.put("d", params.d);
        details.put("q", params.q);
        
        // AR系数
        double[] arCoeffs = model.getARCoefficients();
        List<Map<String, Object>> arCoeffsList = new ArrayList<>();
        for (int i = 0; i < arCoeffs.length; i++) {
            Map<String, Object> coeff = new HashMap<>();
            coeff.put("order", i + 1);
            coeff.put("value", Math.round(arCoeffs[i] * 10000.0) / 10000.0);
            coeff.put("description", String.format("AR(%d)系数 φ%d", i + 1, i + 1));
            arCoeffsList.add(coeff);
        }
        details.put("ar_coefficients", arCoeffsList);
        
        // MA系数
        double[] maCoeffs = model.getMACoefficients();
        List<Map<String, Object>> maCoeffsList = new ArrayList<>();
        for (int i = 0; i < maCoeffs.length; i++) {
            Map<String, Object> coeff = new HashMap<>();
            coeff.put("order", i + 1);
            coeff.put("value", Math.round(maCoeffs[i] * 10000.0) / 10000.0);
            coeff.put("description", String.format("MA(%d)系数 θ%d", i + 1, i + 1));
            maCoeffsList.add(coeff);
        }
        details.put("ma_coefficients", maCoeffsList);
        
        // 差分过程
        List<Double> differencedData = model.getDifferencedData();
        List<Map<String, Object>> differencingSteps = new ArrayList<>();
        if (params.d > 0) {
            Map<String, Object> step = new HashMap<>();
            step.put("step", 1);
            step.put("description", String.format("进行%d阶差分，消除趋势", params.d));
            step.put("original_count", data.size());
            step.put("differenced_count", differencedData.size());
            differencingSteps.add(step);
        }
        details.put("differencing_steps", differencingSteps);
        
        // 残差信息
        double[] residuals = model.getResiduals();
        if (residuals != null && residuals.length > 0) {
            double residualMean = 0;
            double residualStd = 0;
            for (double res : residuals) {
                residualMean += res;
            }
            residualMean /= residuals.length;
            for (double res : residuals) {
                residualStd += Math.pow(res - residualMean, 2);
            }
            residualStd = Math.sqrt(residualStd / residuals.length);
            
            Map<String, Object> residualInfo = new HashMap<>();
            residualInfo.put("count", residuals.length);
            residualInfo.put("mean", Math.round(residualMean * 10000.0) / 10000.0);
            residualInfo.put("std", Math.round(residualStd * 100.0) / 100.0);
            details.put("residual_info", residualInfo);
        }
        
        return details;
    }
    
    /**
     * 根据ARIMA数据判断趋势
     */
    private String determineTrendFromARIMA(List<ARIMAModel.Point> data) {
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

