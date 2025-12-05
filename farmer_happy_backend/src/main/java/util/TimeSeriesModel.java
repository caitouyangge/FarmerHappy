// src/main/java/util/TimeSeriesModel.java
package util;

import java.util.List;

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
}

