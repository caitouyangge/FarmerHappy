// src/main/java/util/RegressionModel.java
package util;

import java.util.List;

/**
 * 回归模型类
 * 实现线性回归和多项式回归
 */
public class RegressionModel {
    
    private double slope;      // 斜率
    private double intercept;   // 截距
    private double[] polyCoeffs; // 多项式系数（用于多项式回归）
    private ModelType modelType;
    private double normalizedXMin = 0; // 标准化x的最小值
    private double normalizedXMax = 1; // 标准化x的最大值
    
    public enum ModelType {
        LINEAR,      // 线性回归
        POLYNOMIAL_2, // 二次多项式
        POLYNOMIAL_3  // 三次多项式
    }
    
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
        public double rSquared;  // R²决定系数
        public double mae;       // 平均绝对误差
        public double rmse;      // 均方根误差
        
        public Metrics(double rSquared, double mae, double rmse) {
            this.rSquared = rSquared;
            this.mae = mae;
            this.rmse = rmse;
        }
    }
    
    /**
     * 训练线性回归模型（支持加权回归）
     */
    public void trainLinear(List<Point> data) {
        trainLinear(data, false);
    }
    
    /**
     * 训练线性回归模型（支持加权回归，最近的数据权重更高）
     */
    public void trainLinear(List<Point> data, boolean useWeighted) {
        this.modelType = ModelType.LINEAR;
        
        if (data == null || data.size() < 2) {
            throw new IllegalArgumentException("至少需要2个数据点");
        }
        
        if (useWeighted && data.size() > 5) {
            // 使用加权回归，最近的数据权重更高
            trainWeightedLinear(data);
            return;
        }
        
        // 计算均值
        double xMean = data.stream().mapToDouble(p -> p.x).average().orElse(0);
        double yMean = data.stream().mapToDouble(p -> p.y).average().orElse(0);
        
        // 计算斜率和截距（最小二乘法）
        double numerator = 0;
        double denominator = 0;
        
        for (Point point : data) {
            double xDiff = point.x - xMean;
            double yDiff = point.y - yMean;
            numerator += xDiff * yDiff;
            denominator += xDiff * xDiff;
        }
        
        if (Math.abs(denominator) < 1e-10) {
            // 如果分母为0，说明所有x值相同，使用平均值
            this.slope = 0;
            this.intercept = yMean;
        } else {
            this.slope = numerator / denominator;
            this.intercept = yMean - this.slope * xMean;
        }
    }
    
    /**
     * 加权线性回归（最近的数据权重更高）
     */
    private void trainWeightedLinear(List<Point> data) {
        int n = data.size();
        double totalWeight = 0;
        double weightedXMean = 0;
        double weightedYMean = 0;
        
        // 计算权重（最近的数据权重更高，使用指数衰减）
        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            // 权重从1.0（最新）到0.3（最旧）
            weights[i] = 0.3 + 0.7 * (i + 1.0) / n;
            totalWeight += weights[i];
            weightedXMean += weights[i] * data.get(i).x;
            weightedYMean += weights[i] * data.get(i).y;
        }
        
        weightedXMean /= totalWeight;
        weightedYMean /= totalWeight;
        
        // 计算加权斜率和截距
        double numerator = 0;
        double denominator = 0;
        
        for (int i = 0; i < n; i++) {
            Point point = data.get(i);
            double xDiff = point.x - weightedXMean;
            double yDiff = point.y - weightedYMean;
            numerator += weights[i] * xDiff * yDiff;
            denominator += weights[i] * xDiff * xDiff;
        }
        
        if (Math.abs(denominator) < 1e-10) {
            this.slope = 0;
            this.intercept = weightedYMean;
        } else {
            this.slope = numerator / denominator;
            this.intercept = weightedYMean - this.slope * weightedXMean;
        }
    }
    
    /**
     * 训练多项式回归模型（二次）
     */
    public void trainPolynomial2(List<Point> data) {
        this.modelType = ModelType.POLYNOMIAL_2;
        this.polyCoeffs = fitPolynomial(data, 2);
    }
    
    /**
     * 训练多项式回归模型（三次）
     */
    public void trainPolynomial3(List<Point> data) {
        this.modelType = ModelType.POLYNOMIAL_3;
        this.polyCoeffs = fitPolynomial(data, 3);
    }
    
    /**
     * 拟合多项式（使用最小二乘法，改进数值稳定性）
     */
    private double[] fitPolynomial(List<Point> data, int degree) {
        int n = data.size();
        int m = degree + 1;
        
        // 数据标准化：将x值标准化到[-1, 1]范围，提高数值稳定性
        double xMin = data.stream().mapToDouble(p -> p.x).min().orElse(0);
        double xMax = data.stream().mapToDouble(p -> p.x).max().orElse(1);
        double xRange = xMax - xMin;
        if (xRange < 1e-10) {
            xRange = 1.0;
        }
        
        // 构建矩阵 A (Vandermonde矩阵，使用标准化后的x)
        double[][] A = new double[n][m];
        double[] b = new double[n];
        
        for (int i = 0; i < n; i++) {
            // 标准化x值到[-1, 1]
            double xNormalized = 2.0 * (data.get(i).x - xMin) / xRange - 1.0;
            b[i] = data.get(i).y;
            for (int j = 0; j < m; j++) {
                A[i][j] = Math.pow(xNormalized, j);
            }
        }
        
        // 求解线性方程组 A^T * A * x = A^T * b
        double[] coeffsNormalized = solveNormalEquations(A, b);
        
        // 保存标准化参数，用于预测时转换
        this.normalizedXMin = xMin;
        this.normalizedXMax = xMax;
        
        return coeffsNormalized;
    }
    
    /**
     * 求解正规方程
     */
    private double[] solveNormalEquations(double[][] A, double[] b) {
        int n = A.length;
        int m = A[0].length;
        
        // 计算 A^T * A
        double[][] AtA = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < n; k++) {
                    AtA[i][j] += A[k][i] * A[k][j];
                }
            }
        }
        
        // 计算 A^T * b
        double[] Atb = new double[m];
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                Atb[i] += A[k][i] * b[k];
            }
        }
        
        // 求解线性方程组（高斯消元法）
        return gaussianElimination(AtA, Atb);
    }
    
    /**
     * 高斯消元法求解线性方程组
     */
    private double[] gaussianElimination(double[][] A, double[] b) {
        int n = A.length;
        double[][] augmented = new double[n][n + 1];
        
        // 构建增广矩阵
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = A[i][j];
            }
            augmented[i][n] = b[i];
        }
        
        // 前向消元
        for (int i = 0; i < n; i++) {
            // 找到主元
            int maxRow = i;
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                    maxRow = k;
                }
            }
            
            // 交换行
            double[] temp = augmented[i];
            augmented[i] = augmented[maxRow];
            augmented[maxRow] = temp;
            
            // 消元
            for (int k = i + 1; k < n; k++) {
                double factor = augmented[k][i] / augmented[i][i];
                for (int j = i; j <= n; j++) {
                    augmented[k][j] -= factor * augmented[i][j];
                }
            }
        }
        
        // 回代
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = augmented[i][n];
            for (int j = i + 1; j < n; j++) {
                x[i] -= augmented[i][j] * x[j];
            }
            x[i] /= augmented[i][i];
        }
        
        return x;
    }
    
    /**
     * 预测单个值
     */
    public double predict(double x) {
        if (modelType == ModelType.LINEAR) {
            return slope * x + intercept;
        } else if (modelType == ModelType.POLYNOMIAL_2 || modelType == ModelType.POLYNOMIAL_3) {
            // 对于多项式，需要标准化x值
            double xRange = normalizedXMax - normalizedXMin;
            if (xRange < 1e-10) {
                xRange = 1.0;
            }
            double xNormalized = 2.0 * (x - normalizedXMin) / xRange - 1.0;
            
            double result = 0;
            for (int i = 0; i < polyCoeffs.length; i++) {
                result += polyCoeffs[i] * Math.pow(xNormalized, i);
            }
            return result;
        }
        return 0;
    }
    
    /**
     * 评估模型
     */
    public Metrics evaluate(List<Point> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("评估数据不能为空");
        }
        
        double ssRes = 0; // 残差平方和
        double ssTot = 0; // 总平方和
        double mae = 0;
        double rmse = 0;
        
        // 计算y的均值
        double yMean = data.stream().mapToDouble(p -> p.y).average().orElse(0);
        
        for (Point point : data) {
            double predicted = predict(point.x);
            double error = point.y - predicted;
            
            ssRes += error * error;
            ssTot += Math.pow(point.y - yMean, 2);
            mae += Math.abs(error);
            rmse += error * error;
        }
        
        mae /= data.size();
        rmse = Math.sqrt(rmse / data.size());
        
        // 计算R²
        double rSquared = 1 - (ssRes / ssTot);
        if (Double.isNaN(rSquared) || Double.isInfinite(rSquared)) {
            rSquared = 0;
        }
        
        return new Metrics(rSquared, mae, rmse);
    }
    
    /**
     * 获取趋势描述
     */
    public String getTrendDescription() {
        if (modelType == ModelType.LINEAR) {
            if (slope > 0.01) {
                return "上升";
            } else if (slope < -0.01) {
                return "下降";
            } else {
                return "平稳";
            }
        } else {
            // 对于多项式，使用最后几个点的趋势
            return "波动";
        }
    }
}

