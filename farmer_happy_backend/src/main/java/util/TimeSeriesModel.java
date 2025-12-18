// src/main/java/util/TimeSeriesModel.java
package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间序列模型
 * 使用移动平均和指数平滑方法
 */
public class TimeSeriesModel {
    
    /**
     * 数据点类
     */
    public static class Point {
        public double x;
        public double y;
        
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * 模型评估指标
     */
    public static class Metrics {
        public double rSquared;
        public double mae;
        public double rmse;
        
        public Metrics(double rSquared, double mae, double rmse) {
            this.rSquared = rSquared;
            this.mae = mae;
            this.rmse = rmse;
        }
    }
    
    private List<Point> data;
    private double alpha; // 指数平滑参数
    private double[] smoothedValues;
    private double trend;
    
    /**
     * 训练指数平滑模型
     */
    public void trainExponentialSmoothing(List<Point> data, double alpha) {
        this.data = data;
        this.alpha = alpha;
        this.smoothedValues = new double[data.size()];
        
        if (data.isEmpty()) {
            return;
        }
        
        // 初始化：第一个平滑值等于第一个实际值
        smoothedValues[0] = data.get(0).y;
        
        // 计算指数平滑值
        for (int i = 1; i < data.size(); i++) {
            smoothedValues[i] = alpha * data.get(i).y + (1 - alpha) * smoothedValues[i - 1];
        }
        
        // 计算趋势（使用最后几个点的平均变化率）
        if (data.size() >= 5) {
            double sumTrend = 0;
            int count = 0;
            for (int i = data.size() - 5; i < data.size() - 1; i++) {
                double xDiff = data.get(i + 1).x - data.get(i).x;
                if (xDiff > 0) {
                    double yDiff = data.get(i + 1).y - data.get(i).y;
                    sumTrend += yDiff / xDiff;
                    count++;
                }
            }
            this.trend = count > 0 ? sumTrend / count : 0;
        } else {
            this.trend = 0;
        }
    }
    
    /**
     * 预测未来值
     */
    public double predict(double futureX) {
        if (data == null || data.isEmpty()) {
            return 0;
        }
        
        double lastX = data.get(data.size() - 1).x;
        double lastSmoothed = smoothedValues[data.size() - 1];
        double steps = futureX - lastX;
        
        // 使用指数平滑值 + 趋势外推
        return lastSmoothed + trend * steps;
    }
    
    /**
     * 评估模型
     */
    public Metrics evaluate() {
        if (data == null || data.isEmpty()) {
            return new Metrics(0, 0, 0);
        }
        
        double ssRes = 0;
        double ssTot = 0;
        double mae = 0;
        double rmse = 0;
        
        double yMean = data.stream().mapToDouble(p -> p.y).average().orElse(0);
        
        for (int i = 0; i < data.size(); i++) {
            double predicted = smoothedValues[i];
            double actual = data.get(i).y;
            double error = actual - predicted;
            
            ssRes += error * error;
            ssTot += Math.pow(actual - yMean, 2);
            mae += Math.abs(error);
            rmse += error * error;
        }
        
        mae /= data.size();
        rmse = Math.sqrt(rmse / data.size());
        
        double rSquared = 1 - (ssRes / ssTot);
        if (Double.isNaN(rSquared) || Double.isInfinite(rSquared)) {
            rSquared = 0;
        }
        
        return new Metrics(rSquared, mae, rmse);
    }
    
    /**
     * 获取详细的计算过程信息
     */
    public Map<String, Object> getCalculationDetails() {
        Map<String, Object> details = new HashMap<>();
        
        if (data == null || data.isEmpty()) {
            return details;
        }
        
        // 指数平滑计算过程
        List<Map<String, Object>> smoothingSteps = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> step = new HashMap<>();
            step.put("index", i + 1);
            step.put("days", (long)data.get(i).x);
            step.put("actual_price", Math.round(data.get(i).y * 100.0) / 100.0);
            
            if (i == 0) {
                step.put("formula", "S(0) = Y(0) = " + Math.round(data.get(i).y * 100.0) / 100.0);
                step.put("smoothed_value", Math.round(smoothedValues[i] * 100.0) / 100.0);
            } else {
                double prevSmoothed = smoothedValues[i - 1];
                double actual = data.get(i).y;
                double smoothed = smoothedValues[i];
                step.put("formula", String.format("S(%d) = %.4f × %.2f + (1-%.4f) × %.2f = %.2f", 
                    i, alpha, actual, alpha, prevSmoothed, smoothed));
                step.put("smoothed_value", Math.round(smoothed * 100.0) / 100.0);
                step.put("alpha", Math.round(alpha * 10000.0) / 10000.0);
            }
            smoothingSteps.add(step);
        }
        details.put("smoothing_steps", smoothingSteps);
        details.put("alpha", Math.round(alpha * 10000.0) / 10000.0);
        
        // 趋势计算过程
        Map<String, Object> trendDetails = new HashMap<>();
        if (data.size() >= 5) {
            List<Map<String, Object>> trendSteps = new ArrayList<>();
            double sumTrend = 0;
            int count = 0;
            for (int i = data.size() - 5; i < data.size() - 1; i++) {
                Map<String, Object> step = new HashMap<>();
                double xDiff = data.get(i + 1).x - data.get(i).x;
                if (xDiff > 0) {
                    double yDiff = data.get(i + 1).y - data.get(i).y;
                    double changeRate = yDiff / xDiff;
                    sumTrend += changeRate;
                    count++;
                    step.put("from_index", i + 1);
                    step.put("to_index", i + 2);
                    step.put("price_change", Math.round(yDiff * 100.0) / 100.0);
                    step.put("time_change", (long)xDiff);
                    step.put("change_rate", Math.round(changeRate * 10000.0) / 10000.0);
                    step.put("formula", String.format("(%.2f - %.2f) / %.0f = %.4f", 
                        data.get(i + 1).y, data.get(i).y, xDiff, changeRate));
                    trendSteps.add(step);
                }
            }
            trendDetails.put("steps", trendSteps);
            trendDetails.put("formula", String.format("趋势 = (%.4f + ...) / %d = %.4f", 
                sumTrend, count, trend));
            trendDetails.put("value", Math.round(trend * 10000.0) / 10000.0);
        } else {
            trendDetails.put("value", 0.0);
            trendDetails.put("formula", "数据点少于5个，趋势设为0");
        }
        details.put("trend_calculation", trendDetails);
        
        // 评估指标计算过程
        Map<String, Object> evaluationDetails = new HashMap<>();
        double yMean = data.stream().mapToDouble(p -> p.y).average().orElse(0);
        double ssRes = 0;
        double ssTot = 0;
        double mae = 0;
        double rmse = 0;
        
        List<Map<String, Object>> evaluationSteps = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            double predicted = smoothedValues[i];
            double actual = data.get(i).y;
            double error = actual - predicted;
            double errorSquared = error * error;
            double totalSquared = Math.pow(actual - yMean, 2);
            
            ssRes += errorSquared;
            ssTot += totalSquared;
            mae += Math.abs(error);
            rmse += errorSquared;
            
            Map<String, Object> step = new HashMap<>();
            step.put("index", i + 1);
            step.put("actual", Math.round(actual * 100.0) / 100.0);
            step.put("predicted", Math.round(predicted * 100.0) / 100.0);
            step.put("error", Math.round(error * 100.0) / 100.0);
            step.put("error_squared", Math.round(errorSquared * 10000.0) / 10000.0);
            step.put("total_squared", Math.round(totalSquared * 10000.0) / 10000.0);
            evaluationSteps.add(step);
        }
        
        mae /= data.size();
        rmse = Math.sqrt(rmse / data.size());
        double rSquared = 1 - (ssRes / ssTot);
        if (Double.isNaN(rSquared) || Double.isInfinite(rSquared)) {
            rSquared = 0;
        }
        
        evaluationDetails.put("steps", evaluationSteps);
        evaluationDetails.put("y_mean", Math.round(yMean * 100.0) / 100.0);
        evaluationDetails.put("ss_res", Math.round(ssRes * 10000.0) / 10000.0);
        evaluationDetails.put("ss_tot", Math.round(ssTot * 10000.0) / 10000.0);
        evaluationDetails.put("r_squared_formula", String.format("R² = 1 - (%.4f / %.4f) = %.4f", 
            ssRes, ssTot, rSquared));
        evaluationDetails.put("mae_formula", String.format("MAE = %.4f / %d = %.4f", 
            mae * data.size(), data.size(), mae));
        evaluationDetails.put("rmse_formula", String.format("RMSE = √(%.4f / %d) = %.4f", 
            rmse * rmse * data.size(), data.size(), rmse));
        
        details.put("evaluation_calculation", evaluationDetails);
        
        return details;
    }
    
    /**
     * 获取预测过程的详细信息
     */
    public List<Map<String, Object>> getPredictionDetails(List<Double> futureXValues) {
        List<Map<String, Object>> predictionDetails = new ArrayList<>();
        
        if (data == null || data.isEmpty()) {
            return predictionDetails;
        }
        
        double lastX = data.get(data.size() - 1).x;
        double lastSmoothed = smoothedValues[data.size() - 1];
        
        for (int i = 0; i < futureXValues.size(); i++) {
            double futureX = futureXValues.get(i);
            double steps = futureX - lastX;
            double predictedPrice = lastSmoothed + trend * steps;
            
            Map<String, Object> detail = new HashMap<>();
            detail.put("step", i + 1);
            detail.put("future_x", futureX);
            detail.put("steps", Math.round(steps * 100.0) / 100.0);
            detail.put("last_smoothed", Math.round(lastSmoothed * 100.0) / 100.0);
            detail.put("trend", Math.round(trend * 10000.0) / 10000.0);
            detail.put("formula", String.format("预测价格 = %.2f + %.4f × %.0f = %.2f", 
                lastSmoothed, trend, steps, predictedPrice));
            detail.put("predicted_price", Math.round(predictedPrice * 100.0) / 100.0);
            
            predictionDetails.add(detail);
        }
        
        return predictionDetails;
    }
    
    /**
     * 自动选择最佳alpha参数
     */
    public static double findBestAlpha(List<Point> data) {
        double bestAlpha = 0.3;
        double bestR2 = Double.NEGATIVE_INFINITY;
        
        // 尝试不同的alpha值
        for (double alpha = 0.1; alpha <= 0.9; alpha += 0.1) {
            try {
                TimeSeriesModel model = new TimeSeriesModel();
                model.trainExponentialSmoothing(data, alpha);
                Metrics metrics = model.evaluate();
                if (metrics.rSquared > bestR2) {
                    bestR2 = metrics.rSquared;
                    bestAlpha = alpha;
                }
            } catch (Exception e) {
                // 忽略错误，继续尝试
            }
        }
        
        return bestAlpha;
    }
    
    /**
     * 获取Alpha参数选择的详细过程
     */
    public static Map<String, Object> getAlphaSelectionDetails(List<Point> data) {
        Map<String, Object> details = new HashMap<>();
        List<Map<String, Object>> alphaTrials = new ArrayList<>();
        double bestAlpha = 0.3;
        double bestR2 = Double.NEGATIVE_INFINITY;
        
        // 尝试不同的alpha值
        for (double alpha = 0.1; alpha <= 0.9; alpha += 0.1) {
            try {
                TimeSeriesModel model = new TimeSeriesModel();
                model.trainExponentialSmoothing(data, alpha);
                Metrics metrics = model.evaluate();
                
                Map<String, Object> trial = new HashMap<>();
                trial.put("alpha", Math.round(alpha * 10000.0) / 10000.0);
                trial.put("r_squared", Math.round(metrics.rSquared * 10000.0) / 10000.0);
                trial.put("mae", Math.round(metrics.mae * 100.0) / 100.0);
                trial.put("rmse", Math.round(metrics.rmse * 100.0) / 100.0);
                trial.put("is_best", false);
                
                if (metrics.rSquared > bestR2) {
                    bestR2 = metrics.rSquared;
                    bestAlpha = alpha;
                    // 更新之前的最佳标记
                    for (Map<String, Object> prevTrial : alphaTrials) {
                        prevTrial.put("is_best", false);
                    }
                    trial.put("is_best", true);
                }
                
                alphaTrials.add(trial);
            } catch (Exception e) {
                // 忽略错误，继续尝试
            }
        }
        
        details.put("trials", alphaTrials);
        details.put("selected_alpha", Math.round(bestAlpha * 10000.0) / 10000.0);
        details.put("selected_r_squared", Math.round(bestR2 * 10000.0) / 10000.0);
        details.put("method", "遍历0.1到0.9（步长0.1），选择R²最高的alpha值");
        
        return details;
    }
}



