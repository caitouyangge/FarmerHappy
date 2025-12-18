// src/main/java/util/ARIMAModel.java
package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ARIMA (AutoRegressive Integrated Moving Average) 模型
 * 自回归综合移动平均模型
 * 支持ARIMA(p,d,q)和SARIMA(p,d,q)(P,D,Q,s)
 */
public class ARIMAModel {
    
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
        public double aic; // Akaike信息准则
        
        public Metrics(double rSquared, double mae, double rmse, double aic) {
            this.rSquared = rSquared;
            this.mae = mae;
            this.rmse = rmse;
            this.aic = aic;
        }
    }
    
    /**
     * ARIMA模型参数
     */
    public static class ARIMAParams {
        public int p; // AR阶数
        public int d; // 差分次数
        public int q; // MA阶数
        public int P; // 季节性AR阶数
        public int D; // 季节性差分次数
        public int Q; // 季节性MA阶数
        public int s; // 季节周期
        
        public ARIMAParams(int p, int d, int q) {
            this.p = p;
            this.d = d;
            this.q = q;
            this.P = 0;
            this.D = 0;
            this.Q = 0;
            this.s = 0;
        }
        
        public ARIMAParams(int p, int d, int q, int P, int D, int Q, int s) {
            this.p = p;
            this.d = d;
            this.q = q;
            this.P = P;
            this.D = D;
            this.Q = Q;
            this.s = s;
        }
        
        public boolean isSeasonal() {
            return s > 0;
        }
        
        @Override
        public String toString() {
            if (isSeasonal()) {
                return String.format("SARIMA(%d,%d,%d)(%d,%d,%d)[%d]", p, d, q, P, D, Q, s);
            } else {
                return String.format("ARIMA(%d,%d,%d)", p, d, q);
            }
        }
    }
    
    private List<Point> originalData;
    private List<Double> differencedData; // 差分后的数据
    private ARIMAParams params;
    private double[] arCoefficients; // AR系数
    private double[] maCoefficients; // MA系数
    private double[] seasonalARCoefficients; // 季节性AR系数
    private double[] seasonalMACoefficients; // 季节性MA系数
    private double constant; // 常数项
    private double[] residuals; // 残差
    private double mean; // 均值
    
    /**
     * 训练ARIMA模型
     */
    public void train(List<Point> data, ARIMAParams params) {
        this.originalData = data;
        this.params = params;
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据不能为空");
        }
        
        // 提取价格序列
        List<Double> priceSeries = new ArrayList<>();
        for (Point point : data) {
            priceSeries.add(point.y);
        }
        
        // 计算均值
        this.mean = priceSeries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        // 去中心化
        List<Double> centeredSeries = new ArrayList<>();
        for (Double price : priceSeries) {
            centeredSeries.add(price - mean);
        }
        
        // 差分处理
        List<Double> series = centeredSeries;
        for (int i = 0; i < params.d; i++) {
            series = ARIMAModel.difference(series);
        }
        
        // 季节性差分
        if (params.isSeasonal() && params.D > 0 && series.size() > params.s) {
            for (int i = 0; i < params.D; i++) {
                series = seasonalDifference(series, params.s);
            }
        }
        
        this.differencedData = series;
        
        // 估计参数（使用最小二乘法）
        estimateParameters();
        
        // 估计季节性参数（如果适用）
        if (params.isSeasonal()) {
            estimateSeasonalParameters();
        }
    }
    
    /**
     * 估计季节性参数
     */
    private void estimateSeasonalParameters() {
        if (!params.isSeasonal() || differencedData.size() < params.s * 2) {
            this.seasonalARCoefficients = new double[params.P];
            this.seasonalMACoefficients = new double[params.Q];
            return;
        }
        
        // 估计季节性AR系数（简化版）
        if (params.P > 0) {
            this.seasonalARCoefficients = new double[params.P];
            // 使用季节滞后的自相关
            for (int i = 0; i < params.P; i++) {
                int lag = params.s * (i + 1);
                if (lag < differencedData.size()) {
                    double[] acf = ARIMAModel.calculateACF(differencedData, lag);
                    seasonalARCoefficients[i] = Math.max(-0.9, Math.min(0.9, acf[lag] * 0.6));
                }
            }
        } else {
            this.seasonalARCoefficients = new double[0];
        }
        
        // 估计季节性MA系数（简化版）
        if (params.Q > 0) {
            this.seasonalMACoefficients = new double[params.Q];
            for (int i = 0; i < params.Q; i++) {
                int lag = params.s * (i + 1);
                if (lag < differencedData.size()) {
                    double[] acf = ARIMAModel.calculateACF(differencedData, lag);
                    seasonalMACoefficients[i] = Math.max(-0.9, Math.min(0.9, acf[lag] * 0.5));
                }
            }
        } else {
            this.seasonalMACoefficients = new double[0];
        }
    }
    
    /**
     * 季节性差分
     */
    private List<Double> seasonalDifference(List<Double> series, int period) {
        List<Double> diff = new ArrayList<>();
        for (int i = period; i < series.size(); i++) {
            diff.add(series.get(i) - series.get(i - period));
        }
        return diff;
    }
    
    /**
     * 估计ARIMA参数（简化版，使用最小二乘法）
     */
    private void estimateParameters() {
        int n = differencedData.size();
        if (n < Math.max(params.p, params.q) + 10) {
            // 数据太少，使用默认值
            this.arCoefficients = new double[params.p];
            this.maCoefficients = new double[params.q];
            return;
        }
        
        // 估计AR系数（使用Yule-Walker方程的简化版本）
        if (params.p > 0) {
            this.arCoefficients = estimateARCoefficients(differencedData, params.p);
        } else {
            this.arCoefficients = new double[0];
        }
        
        // 估计MA系数（简化版）
        if (params.q > 0) {
            this.maCoefficients = estimateMACoefficients(differencedData, params.q);
        } else {
            this.maCoefficients = new double[0];
        }
        
        // 计算残差
        this.residuals = calculateResiduals();
        
        // 估计常数项
        this.constant = estimateConstant();
    }
    
    /**
     * 估计AR系数（使用自相关函数和Yule-Walker方程）
     */
    private double[] estimateARCoefficients(List<Double> series, int p) {
        double[] coefficients = new double[p];
        int n = series.size();
        
        if (p == 0 || n < p + 5) {
            return coefficients;
        }
        
        // 计算自相关函数
        double[] acf = ARIMAModel.calculateACF(series, p);
        
        // 使用Yule-Walker方程
        if (p == 1) {
            // AR(1): phi1 = acf[1]
            coefficients[0] = Math.max(-0.99, Math.min(0.99, acf[1])); // 限制在合理范围内
        } else if (p == 2) {
            // AR(2): 使用Yule-Walker方程
            double denom = 1 - acf[1] * acf[1];
            if (Math.abs(denom) > 1e-10) {
                coefficients[0] = Math.max(-0.99, Math.min(0.99, acf[1] * (1 - acf[2]) / denom));
                coefficients[1] = Math.max(-0.99, Math.min(0.99, (acf[2] - acf[1] * acf[1]) / denom));
            } else {
                // 如果分母太小，使用简化方法
                coefficients[0] = Math.max(-0.99, Math.min(0.99, acf[1] * 0.9));
                coefficients[1] = 0;
            }
        } else {
            // 对于更高阶，使用简化的方法，但限制系数大小
            for (int i = 0; i < p && i < acf.length - 1; i++) {
                coefficients[i] = Math.max(-0.99, Math.min(0.99, acf[i + 1] * 0.8));
            }
        }
        
        return coefficients;
    }
    
    /**
     * 估计MA系数（使用自相关函数）
     */
    private double[] estimateMACoefficients(List<Double> series, int q) {
        double[] coefficients = new double[q];
        int n = series.size();
        
        if (q == 0 || n < q + 5) {
            return coefficients;
        }
        
        // 计算自相关函数
        double[] acf = ARIMAModel.calculateACF(series, q);
        
        // 对于MA模型，ACF在lag>q后应该截断
        // 简化估计：使用ACF值，但限制系数大小
        for (int i = 0; i < q && i < acf.length - 1; i++) {
            // MA系数通常较小，限制在合理范围内
            coefficients[i] = Math.max(-0.99, Math.min(0.99, acf[i + 1] * 0.7));
        }
        
        return coefficients;
    }
    
    /**
     * 计算残差
     */
    private double[] calculateResiduals() {
        int n = differencedData.size();
        double[] residuals = new double[n];
        
        for (int i = 0; i < n; i++) {
            double predicted = 0;
            
            // AR部分
            for (int j = 0; j < params.p && i - j - 1 >= 0; j++) {
                predicted += arCoefficients[j] * differencedData.get(i - j - 1);
            }
            
            // MA部分（使用之前的残差，简化处理）
            for (int j = 0; j < params.q && i - j - 1 >= 0; j++) {
                if (i - j - 1 >= 0) {
                    predicted += maCoefficients[j] * (i - j - 2 >= 0 ? residuals[i - j - 2] : 0);
                }
            }
            
            residuals[i] = differencedData.get(i) - predicted;
        }
        
        return residuals;
    }
    
    /**
     * 估计常数项
     */
    private double estimateConstant() {
        if (differencedData.isEmpty()) {
            return 0;
        }
        return differencedData.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
    
    /**
     * 预测未来值
     */
    public double predict(int stepsAhead) {
        if (differencedData == null || differencedData.isEmpty() || originalData == null || originalData.isEmpty()) {
            return mean;
        }
        
        int n = differencedData.size();
        List<Double> forecastDiffSeries = new ArrayList<>(differencedData);
        List<Double> forecastResiduals = new ArrayList<>();
        
        // 初始化残差（使用实际残差）
        if (residuals != null) {
            for (int i = Math.max(0, residuals.length - params.q); i < residuals.length; i++) {
                forecastResiduals.add(residuals[i]);
            }
        }
        
        // 预测差分序列的未来值
        for (int step = 0; step < stepsAhead; step++) {
            double forecast = constant;
            
            // AR部分：使用最近的差分值
            for (int i = 0; i < params.p && forecastDiffSeries.size() - i - 1 >= 0; i++) {
                forecast += arCoefficients[i] * forecastDiffSeries.get(forecastDiffSeries.size() - i - 1);
            }
            
            // MA部分：使用最近的残差
            for (int i = 0; i < params.q && forecastResiduals.size() - i - 1 >= 0; i++) {
                forecast += maCoefficients[i] * forecastResiduals.get(forecastResiduals.size() - i - 1);
            }
            
            // 季节性AR部分（如果适用）
            if (params.isSeasonal() && seasonalARCoefficients != null) {
                for (int i = 0; i < params.P; i++) {
                    int seasonalLag = params.s * (i + 1);
                    int idx = forecastDiffSeries.size() - seasonalLag - 1;
                    if (idx >= 0 && idx < forecastDiffSeries.size()) {
                        forecast += seasonalARCoefficients[i] * forecastDiffSeries.get(idx);
                    }
                }
            }
            
            // 季节性MA部分（如果适用）
            if (params.isSeasonal() && seasonalMACoefficients != null) {
                for (int i = 0; i < params.Q; i++) {
                    int seasonalLag = params.s * (i + 1);
                    int idx = forecastResiduals.size() - seasonalLag - 1;
                    if (idx >= 0 && idx < forecastResiduals.size()) {
                        forecast += seasonalMACoefficients[i] * forecastResiduals.get(idx);
                    }
                }
            }
            
            // 对于未来值，残差设为0（简化处理）
            forecastResiduals.add(0.0);
            forecastDiffSeries.add(forecast);
        }
        
        // 反向差分：从最后一个原始值开始，累加所有差分预测值
        // 对于SARIMA模型，需要正确处理季节性
        
        // 如果检测到季节性，优先使用历史周期模式（更准确）
        if (params.isSeasonal() && params.s > 0 && originalData.size() >= params.s * 2) {
            return predictUsingHistoricalPattern(stepsAhead);
        }
        
        // 非季节性模型，使用标准ARIMA预测
        double result = calculateARIMAPrediction(stepsAhead, forecastDiffSeries, n);
        
        // 如果结果不合理，使用趋势外推
        if (result < 0 || Double.isNaN(result) || Double.isInfinite(result)) {
            result = predictUsingHistoricalPattern(stepsAhead);
        }
        
        // 确保价格不为负
        if (result < 0) {
            result = Math.max(0, originalData.get(originalData.size() - 1).y * 0.9);
        }
        
        return result;
    }
    
    /**
     * 计算ARIMA预测值（不考虑季节性，用于非季节性模型或作为参考）
     */
    private double calculateARIMAPrediction(int stepsAhead, List<Double> forecastDiffSeries, int n) {
        // 反向差分：从最后一个原始值开始，累加所有差分预测值
        double result = originalData.get(originalData.size() - 1).y;
        
        // 累加所有预测的差分值
        for (int i = 0; i < stepsAhead; i++) {
            int diffIndex = n + i;
            if (diffIndex < forecastDiffSeries.size()) {
                result += forecastDiffSeries.get(diffIndex);
            }
        }
        
        return result;
    }
    
    /**
     * 使用历史周期模式进行预测（捕捉周期性规律）
     * 通过建立周期模板来更准确地复制历史周期模式
     */
    private double predictUsingHistoricalPattern(int stepsAhead) {
        // 如果没有季节性参数，使用简单趋势
        if (!params.isSeasonal() || params.s <= 0 || originalData.size() < params.s * 2) {
            if (originalData.size() >= 3) {
                // 使用最近几个点的平均趋势
                double recentTrend = 0;
                int lookback = Math.min(5, originalData.size() - 1);
                for (int i = originalData.size() - lookback; i < originalData.size() - 1; i++) {
                    recentTrend += originalData.get(i + 1).y - originalData.get(i).y;
                }
                recentTrend /= lookback;
                return originalData.get(originalData.size() - 1).y + recentTrend * stepsAhead;
            } else if (originalData.size() >= 2) {
                double trend = originalData.get(originalData.size() - 1).y - 
                              originalData.get(originalData.size() - 2).y;
                return originalData.get(originalData.size() - 1).y + trend * stepsAhead;
            }
            return originalData.get(originalData.size() - 1).y;
        }
        
        int n = originalData.size();
        int numCompleteCycles = n / params.s;
        
        if (numCompleteCycles < 2) {
            // 完整周期太少，使用简单方法
            int currentPos = (n - 1) % params.s;
            int targetPos = (currentPos + stepsAhead) % params.s;
            int lastCycleStart = Math.max(0, n - params.s);
            int targetIdx = lastCycleStart + targetPos;
            if (targetIdx >= 0 && targetIdx < n) {
                return originalData.get(targetIdx).y;
            }
            return originalData.get(n - 1).y;
        }
        
        // 计算目标位置在周期中的索引
        int currentPositionInCycle = (n - 1) % params.s;
        int targetPositionInCycle = (currentPositionInCycle + stepsAhead) % params.s;
        int cyclesAhead = (currentPositionInCycle + stepsAhead) / params.s;
        
        // 建立周期模板：分析历史周期中每个位置相对于周期平均值的相对变化
        // 周期模板存储每个位置相对于周期平均值的偏移量
        List<List<Double>> positionOffsets = new ArrayList<>(); // 每个位置的历史偏移量列表
        for (int pos = 0; pos < params.s; pos++) {
            positionOffsets.add(new ArrayList<>());
        }
        List<Double> cycleAverages = new ArrayList<>();
        
        // 计算每个完整周期的平均值和周期模板
        int maxCyclesToUse = Math.min(5, numCompleteCycles); // 使用最近5个周期
        for (int i = 0; i < maxCyclesToUse; i++) {
            int cycleIndex = numCompleteCycles - 1 - i; // 从最近的周期开始
            int cycleStart = cycleIndex * params.s;
            
            // 计算这个周期的平均值
            double cycleSum = 0;
            for (int pos = 0; pos < params.s && cycleStart + pos < n; pos++) {
                cycleSum += originalData.get(cycleStart + pos).y;
            }
            double cycleAvg = cycleSum / params.s;
            cycleAverages.add(cycleAvg);
            
            // 计算这个周期中每个位置相对于平均值的偏移
            for (int pos = 0; pos < params.s && cycleStart + pos < n; pos++) {
                double offset = originalData.get(cycleStart + pos).y - cycleAvg;
                positionOffsets.get(pos).add(offset);
            }
        }
        
        // 计算周期模板：每个位置的平均偏移（使用加权平均，最近周期权重更高）
        double[] cycleTemplate = new double[params.s];
        for (int pos = 0; pos < params.s; pos++) {
            List<Double> offsets = positionOffsets.get(pos);
            if (!offsets.isEmpty()) {
                double weightedSum = 0;
                double totalWeight = 0;
                for (int i = 0; i < offsets.size(); i++) {
                    double weight = Math.pow(0.85, i); // 最近周期权重更高
                    weightedSum += offsets.get(i) * weight;
                    totalWeight += weight;
                }
                cycleTemplate[pos] = weightedSum / totalWeight;
            }
        }
        
        // 预测目标周期的平均值（使用最近几个周期的平均值和趋势）
        double predictedCycleAvg;
        if (cycleAverages.size() >= 2) {
            // 计算周期平均值的趋势
            double trend = 0;
            for (int i = 0; i < cycleAverages.size() - 1; i++) {
                trend += cycleAverages.get(i) - cycleAverages.get(i + 1);
            }
            trend /= (cycleAverages.size() - 1);
            
            // 预测值 = 最近周期平均值 + 趋势 * 跨越的周期数
            predictedCycleAvg = cycleAverages.get(0) + trend * cyclesAhead;
        } else {
            predictedCycleAvg = cycleAverages.get(0);
        }
        
        // 如果预测还在当前周期内，需要考虑当前周期已过去的部分
        if (cyclesAhead == 0) {
            // 在同一周期内，使用当前周期的部分平均值
            int currentCycleStart = (n - 1) / params.s * params.s;
            double currentCycleSum = 0;
            int currentCycleCount = 0;
            for (int i = currentCycleStart; i < n; i++) {
                currentCycleSum += originalData.get(i).y;
                currentCycleCount++;
            }
            if (currentCycleCount > 0 && currentCycleCount < params.s) {
                // 当前周期还没结束，使用已过去部分的平均值
                double currentCycleAvg = currentCycleSum / currentCycleCount;
                // 混合使用：70%当前周期部分平均值 + 30%历史周期平均值
                predictedCycleAvg = currentCycleAvg * 0.7 + cycleAverages.get(0) * 0.3;
            }
        }
        
        // 最终预测值 = 预测周期平均值 + 周期模板中目标位置的偏移
        double result = predictedCycleAvg + cycleTemplate[targetPositionInCycle];
        
        // 如果结果不合理，使用简单方法
        if (result < 0 || Double.isNaN(result) || Double.isInfinite(result)) {
            // 回退到使用上一个周期对应位置的值
            int lastCycleStart = Math.max(0, n - params.s);
            int targetIdx = lastCycleStart + targetPositionInCycle;
            if (targetIdx >= 0 && targetIdx < n) {
                result = originalData.get(targetIdx).y;
            } else {
                result = originalData.get(n - 1).y;
            }
        }
        
        return result;
    }
    
    /**
     * 评估模型（在原始数据上计算）
     */
    public Metrics evaluate() {
        if (originalData == null || originalData.isEmpty() || differencedData == null || differencedData.isEmpty()) {
            return new Metrics(0, 0, 0, Double.MAX_VALUE);
        }
        
        // 在原始数据上计算拟合值和评估指标
        List<Double> fittedValues = new ArrayList<>();
        List<Double> originalValues = new ArrayList<>();
        
        // 计算差分数据的拟合值
        int n = differencedData.size();
        int startIdx = Math.max(params.p, params.q);
        
        for (int i = startIdx; i < n; i++) {
            double predictedDiff = constant;
            
            // AR部分
            for (int j = 0; j < params.p && i - j - 1 >= 0; j++) {
                predictedDiff += arCoefficients[j] * differencedData.get(i - j - 1);
            }
            
            // MA部分
            for (int j = 0; j < params.q && residuals != null && i - j - 1 >= 0; j++) {
                predictedDiff += maCoefficients[j] * residuals[i - j - 1];
            }
            
            // 反向差分得到原始数据的拟合值
            // 对于一阶差分：Y(t) = Y(t-1) + diff(t)
            int originalIdx = params.d + i;
            if (originalIdx > 0 && originalIdx < originalData.size()) {
                double fittedOriginal = originalData.get(originalIdx - 1).y + predictedDiff;
                fittedValues.add(fittedOriginal);
                originalValues.add(originalData.get(originalIdx).y);
            } else if (originalIdx == params.d && originalIdx < originalData.size()) {
                // 第一个点，使用第一个原始值
                double fittedOriginal = originalData.get(0).y + predictedDiff;
                fittedValues.add(fittedOriginal);
                originalValues.add(originalData.get(originalIdx).y);
            }
        }
        
        if (fittedValues.isEmpty() || originalValues.isEmpty()) {
            return new Metrics(0, 0, 0, Double.MAX_VALUE);
        }
        
        // 在原始数据上计算评估指标
        double ssRes = 0;
        double ssTot = 0;
        double mae = 0;
        double rmse = 0;
        
        double yMean = originalValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        int minSize = Math.min(fittedValues.size(), originalValues.size());
        for (int i = 0; i < minSize; i++) {
            double predicted = fittedValues.get(i);
            double actual = originalValues.get(i);
            double error = actual - predicted;
            
            ssRes += error * error;
            ssTot += Math.pow(actual - yMean, 2);
            mae += Math.abs(error);
            rmse += error * error;
        }
        
        if (minSize > 0) {
            mae /= minSize;
            rmse = Math.sqrt(rmse / minSize);
        }
        
        double rSquared = 1 - (ssRes / ssTot);
        if (Double.isNaN(rSquared) || Double.isInfinite(rSquared) || ssTot < 1e-10) {
            rSquared = 0;
        }
        
        // 计算AIC（简化版）
        double aic = minSize * Math.log(Math.max(ssRes / minSize, 1e-10)) + 2 * (params.p + params.q + 1);
        
        return new Metrics(rSquared, mae, rmse, aic);
    }
    
    /**
     * 反向差分（从差分值恢复原始值）
     */
    private double reverseDifference(double diffValue, int position) {
        if (params.d == 0) {
            return diffValue + mean;
        }
        
        // 简化处理：使用最后一个原始值作为基准
        if (originalData.size() < 2) {
            return diffValue + mean;
        }
        
        // 对于一阶差分，反向差分是累加
        double result = diffValue;
        
        // 找到对应的原始数据点
        int baseIdx = originalData.size() - differencedData.size() + position;
        if (baseIdx > 0 && baseIdx < originalData.size()) {
            result += originalData.get(baseIdx - 1).y;
        } else if (baseIdx == 0) {
            result += originalData.get(0).y;
        } else {
            // 使用最后一个值
            result += originalData.get(originalData.size() - 1).y;
        }
        
        return result;
    }
    
    /**
     * 自动选择ARIMA参数
     * 针对数据量小、序列稳定的场景进行了优化
     */
    public static ARIMAParams autoSelectParams(List<Point> data) {
        if (data == null || data.size() < 10) {
            // 数据太少（<10），使用最简单的模型
            return new ARIMAParams(1, 0, 1);
        }
        
        if (data.size() < 20) {
            // 数据较少（10-20），使用简单模型，不进行差分
            return new ARIMAParams(1, 0, 1);
        }
        
        // 提取价格序列
        List<Double> priceSeries = new ArrayList<>();
        for (Point point : data) {
            priceSeries.add(point.y);
        }
        
        // 检测是否需要差分（简化版：检查趋势）
        int d = detectDifferencingOrder(priceSeries);
        
        // 应用差分
        List<Double> differenced = priceSeries;
        for (int i = 0; i < d; i++) {
            differenced = difference(differenced);
        }
        
        // 检测季节性（简化版）
        int s = detectSeasonality(priceSeries);
        
        // 选择AR和MA的阶数（简化版：使用ACF和PACF的简化分析）
        int p = selectAROrder(differenced);
        int q = selectMAOrder(differenced);
        
        // 如果检测到季节性，使用SARIMA
        if (s > 0 && differenced.size() > s * 2) {
            return new ARIMAParams(p, d, q, 1, 1, 1, s);
        } else {
            return new ARIMAParams(p, d, q);
        }
    }
    
    /**
     * 检测差分阶数（简化版）
     * 针对小数据集：更谨慎地使用差分，避免过度差分导致数据点减少
     */
    private static int detectDifferencingOrder(List<Double> series) {
        int n = series.size();
        
        // 对于小数据集（<30），除非趋势非常明显，否则不使用差分
        if (n < 30) {
            // 检查是否有非常明显的趋势
            double firstThird = series.subList(0, n / 3)
                .stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double lastThird = series.subList(2 * n / 3, n)
                .stream().mapToDouble(Double::doubleValue).average().orElse(0);
            
            double trend = Math.abs(lastThird - firstThird) / (Math.abs(firstThird) + 1e-10);
            
            // 小数据集需要更强的趋势才进行差分
            if (trend > 0.2) {
                return 1;
            }
            return 0; // 小数据集默认不差分，保持数据点数量
        }
        
        // 对于较大数据集，使用原有逻辑
        double firstHalf = series.subList(0, series.size() / 2)
            .stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalf = series.subList(series.size() / 2, series.size())
            .stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double trend = Math.abs(secondHalf - firstHalf) / (Math.abs(firstHalf) + 1e-10);
        
        if (trend > 0.1) {
            return 1; // 有明显趋势，需要一次差分
        }
        return 0;
    }
    
    /**
     * 检测季节性（简化版）
     * 针对小数据集优化：降低检测阈值，优先检测短周期
     */
    private static int detectSeasonality(List<Double> series) {
        int n = series.size();
        
        // 对于小数据集，降低检测要求
        double correlationThreshold = n < 30 ? 0.25 : 0.3;
        int minCyclesRequired = n < 30 ? 1 : 2; // 小数据集只需要1个完整周期即可检测
        
        // 尝试检测7天、30天、90天的周期（优先检测短周期）
        int[] periods = {7, 30, 90};
        
        for (int period : periods) {
            if (n >= period * minCyclesRequired) {
                double correlation = calculatePeriodCorrelation(series, period);
                if (correlation > correlationThreshold) {
                    return period;
                }
            }
        }
        
        return 0;
    }
    
    /**
     * 计算周期相关性
     */
    private static double calculatePeriodCorrelation(List<Double> series, int period) {
        int n = series.size();
        double mean = series.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double numerator = 0;
        double denominator1 = 0;
        double denominator2 = 0;
        
        for (int i = period; i < n; i++) {
            double diff1 = series.get(i) - mean;
            double diff2 = series.get(i - period) - mean;
            numerator += diff1 * diff2;
            denominator1 += diff1 * diff1;
            denominator2 += diff2 * diff2;
        }
        
        double denominator = Math.sqrt(denominator1 * denominator2);
        if (denominator < 1e-10) {
            return 0;
        }
        
        return numerator / denominator;
    }
    
    /**
     * 选择AR阶数（简化版）
     * 针对小数据集：优先使用低阶模型，避免过拟合
     */
    private static int selectAROrder(List<Double> series) {
        int n = series.size();
        if (n < 10) {
            return 1; // 数据太少，使用AR(1)
        }
        
        // 对于小数据集，限制最大阶数
        int maxLag = n < 30 ? 3 : 5;
        double[] acf = calculateACF(series, maxLag);
        
        // 如果ACF快速衰减，使用AR(1)
        if (Math.abs(acf[1]) > 0.3 && (maxLag < 2 || Math.abs(acf[2]) < Math.abs(acf[1]) * 0.7)) {
            return 1;
        }
        // 如果ACF缓慢衰减且数据量足够，使用AR(2)
        if (n >= 20 && maxLag >= 2 && Math.abs(acf[2]) > 0.2) {
            return 2;
        }
        
        return 1; // 默认使用AR(1)，适合小数据集
    }
    
    /**
     * 选择MA阶数（简化版）
     * 针对小数据集：优先使用MA(1)，避免过拟合
     */
    private static int selectMAOrder(List<Double> series) {
        int n = series.size();
        if (n < 10) {
            return 1; // 数据太少，使用MA(1)
        }
        
        // 对于小数据集，使用MA(1)即可，避免过拟合
        // 对于较大数据集，可以考虑MA(2)，但这里保持简单
        return 1;
    }
    
    /**
     * 计算ACF（静态方法）
     */
    private static double[] calculateACF(List<Double> series, int maxLag) {
        int n = series.size();
        double mean = series.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double variance = 0;
        for (Double value : series) {
            variance += Math.pow(value - mean, 2);
        }
        variance /= n;
        
        if (variance < 1e-10) {
            return new double[maxLag + 1];
        }
        
        double[] acf = new double[maxLag + 1];
        acf[0] = 1.0;
        
        for (int lag = 1; lag <= maxLag && lag < n; lag++) {
            double covariance = 0;
            for (int i = lag; i < n; i++) {
                covariance += (series.get(i) - mean) * (series.get(i - lag) - mean);
            }
            covariance /= (n - lag);
            acf[lag] = covariance / variance;
        }
        
        return acf;
    }
    
    /**
     * 一阶差分（静态方法）
     */
    private static List<Double> difference(List<Double> series) {
        List<Double> diff = new ArrayList<>();
        for (int i = 1; i < series.size(); i++) {
            diff.add(series.get(i) - series.get(i - 1));
        }
        return diff;
    }
    
    /**
     * 获取模型参数
     */
    public ARIMAParams getParams() {
        return params;
    }
    
    /**
     * 获取AR系数
     */
    public double[] getARCoefficients() {
        return arCoefficients != null ? arCoefficients.clone() : new double[0];
    }
    
    /**
     * 获取MA系数
     */
    public double[] getMACoefficients() {
        return maCoefficients != null ? maCoefficients.clone() : new double[0];
    }
    
    /**
     * 获取残差
     */
    public double[] getResiduals() {
        return residuals != null ? residuals.clone() : new double[0];
    }
    
    /**
     * 获取差分后的数据
     */
    public List<Double> getDifferencedData() {
        return new ArrayList<>(differencedData);
    }
}

