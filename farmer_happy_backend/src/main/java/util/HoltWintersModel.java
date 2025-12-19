// src/main/java/util/HoltWintersModel.java
package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ETS(Holt-Winters) 加性模型（level + trend + seasonality）
 *
 * 说明：
 * - 适用于等间隔时间序列（本项目按“天”聚合后使用）
 * - 当 seasonLength <= 1 或数据不足时，会自动退化为 Holt 线性趋势模型（无季节项）
 *
 * 参考公式（Additive）：
 * 预测(一步)： \hat{y}_t = (l_{t-1} + b_{t-1}) + s_{t-m}
 * 更新：
 *  l_t = α (y_t - s_{t-m}) + (1-α)(l_{t-1}+b_{t-1})
 *  b_t = β (l_t - l_{t-1}) + (1-β)b_{t-1}
 *  s_t = γ (y_t - l_t) + (1-γ)s_{t-m}
 */
public class HoltWintersModel {

    public static class FitResult {
        public final List<Double> fitted;   // 与训练序列同长度的拟合值（可用于误差计算）
        public final double level;
        public final double trend;
        public final double phi; // 阻尼趋势系数（damped trend），phi=1 退化为线性趋势
        public final int seasonLength;
        public final double alpha;
        public final double beta;
        public final double gamma;

        private FitResult(List<Double> fitted,
                          double level,
                          double trend,
                          double phi,
                          int seasonLength,
                          double alpha,
                          double beta,
                          double gamma) {
            this.fitted = fitted;
            this.level = level;
            this.trend = trend;
            this.phi = phi;
            this.seasonLength = seasonLength;
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
        }
    }

    private List<Double> y = Collections.emptyList();
    private List<Double> fitted = Collections.emptyList();
    private List<Double> seasonals = Collections.emptyList();
    private double level = 0.0;
    private double trend = 0.0;
    private int m = 1;
    private double alpha = 0.3;
    private double beta = 0.1;
    private double gamma = 0.1;
    private double phi = 1.0; // 阻尼趋势（damped trend）系数，默认1（不阻尼）
    private double psi = 1.0; // 季节项预测期衰减系数（0,1]，仅作用于 forecast；1表示不衰减

    public FitResult fit(List<Double> y, int seasonLength, double alpha, double beta, double gamma) {
        return fit(y, seasonLength, alpha, beta, gamma, 1.0);
    }

    /**
     * 带阻尼趋势（damped trend）的 Holt-Winters 加性模型拟合
     * phi ∈ (0,1]，phi 越小趋势衰减越强；phi=1 等价于原实现（线性趋势外推）。
     */
    public FitResult fit(List<Double> y, int seasonLength, double alpha, double beta, double gamma, double phi) {
        return fit(y, seasonLength, alpha, beta, gamma, phi, 1.0);
    }

    /**
     * 带阻尼趋势 + 季节项预测衰减（psi）的 Holt-Winters 加性模型拟合
     * - phi ∈ (0,1]：趋势阻尼
     * - psi ∈ (0,1]：季节项在预测期的衰减（越远季节振幅越小）
     */
    public FitResult fit(List<Double> y, int seasonLength, double alpha, double beta, double gamma, double phi, double psi) {
        if (y == null || y.isEmpty()) {
            this.y = Collections.emptyList();
            this.fitted = Collections.emptyList();
            this.seasonals = Collections.emptyList();
            this.level = 0.0;
            this.trend = 0.0;
            this.m = Math.max(1, seasonLength);
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
            this.phi = clampPhi(phi);
            this.psi = clampPhi(psi);
            return new FitResult(Collections.emptyList(), 0.0, 0.0, this.phi, this.m, alpha, beta, gamma);
        }

        this.y = new ArrayList<>(y);
        this.alpha = clamp01(alpha);
        this.beta = clamp01(beta);
        this.gamma = clamp01(gamma);
        this.phi = clampPhi(phi);
        this.psi = clampPhi(psi);

        // 数据不足以估计季节项时，退化为无季节（m=1, gamma=0）
        if (seasonLength <= 1 || y.size() < 2 * seasonLength) {
            this.m = 1;
            this.gamma = 0.0;
        } else {
            this.m = seasonLength;
        }

        int n = y.size();
        List<Double> fittedLocal = new ArrayList<>(Collections.nCopies(n, 0.0));
        List<Double> seasonalsLocal = new ArrayList<>(Collections.nCopies(n, 0.0));

        // 初始化 level / trend / seasonals（加性）
        double l0;
        double b0;
        if (m == 1) {
            l0 = y.get(0);
            b0 = (n >= 2) ? (y.get(1) - y.get(0)) : 0.0;
        } else {
            double season1Avg = mean(y, 0, m);
            double season2Avg = mean(y, m, 2 * m);
            l0 = season1Avg;
            b0 = (season2Avg - season1Avg) / m;
            for (int i = 0; i < m; i++) {
                seasonalsLocal.set(i, y.get(i) - season1Avg);
            }
        }

        double lt = l0;
        double bt = b0;

        // t=0 的拟合值直接置为真实值（避免前期误差主导）
        fittedLocal.set(0, y.get(0));

        for (int t = 1; t < n; t++) {
            double st_m = (m == 1 || t - m < 0) ? 0.0 : seasonalsLocal.get(t - m);

            // 一步预测（用于拟合/评估）
            // 阻尼趋势：一步预测使用 (lt + phi * bt)
            double yhat = (lt + this.phi * bt) + st_m;
            fittedLocal.set(t, yhat);

            // 更新
            double yt = y.get(t);
            // 阻尼趋势版本更新（参考 ETS(A,Ad,A) 的常见形式）
            double newLevel = this.alpha * (yt - st_m) + (1.0 - this.alpha) * (lt + this.phi * bt);
            double newTrend = this.beta * (newLevel - lt) + (1.0 - this.beta) * (this.phi * bt);
            double newSeasonal;
            if (m == 1) {
                newSeasonal = 0.0;
            } else if (t - m >= 0) {
                newSeasonal = this.gamma * (yt - newLevel) + (1.0 - this.gamma) * st_m;
            } else {
                // 还没走满一个季节，沿用初始化季节项
                newSeasonal = seasonalsLocal.get(t);
            }

            lt = newLevel;
            bt = newTrend;
            seasonalsLocal.set(t, newSeasonal);
        }

        this.level = lt;
        this.trend = bt;
        this.fitted = fittedLocal;
        this.seasonals = seasonalsLocal;

        return new FitResult(
            new ArrayList<>(fittedLocal),
            this.level,
            this.trend,
            this.phi,
            this.m,
            this.alpha,
            this.beta,
            this.gamma
        );
    }

    public List<Double> forecast(int steps) {
        if (steps <= 0) return Collections.emptyList();
        if (y == null || y.isEmpty()) {
            List<Double> out = new ArrayList<>(steps);
            for (int i = 0; i < steps; i++) out.add(0.0);
            return out;
        }

        int n = y.size();
        List<Double> out = new ArrayList<>(steps);
        double last = y.get(n - 1);
        double maxStepDelta = estimateMaxStepDelta(y);
        double prev = last;

        for (int h = 1; h <= steps; h++) {
            double seasonal = 0.0;
            if (m > 1 && seasonals != null && !seasonals.isEmpty()) {
                int idx = (n - m) + ((h - 1) % m);
                if (idx >= 0 && idx < seasonals.size()) {
                    seasonal = seasonals.get(idx);
                }
            }
            // 季节项衰减：越远的预测，季节振幅逐步降低（避免机械重复）
            if (m > 1 && Math.abs(this.psi - 1.0) > 1e-12) {
                seasonal = seasonal * Math.pow(this.psi, h);
            }
            // 阻尼趋势预测：level + (phi + ... + phi^h) * trend + seasonal
            double trendTerm;
            if (Math.abs(this.phi - 1.0) < 1e-12) {
                trendTerm = h * trend;
            } else {
                // sum_{i=1..h} phi^i = phi * (1 - phi^h) / (1 - phi)
                trendTerm = trend * (this.phi * (1.0 - Math.pow(this.phi, h)) / (1.0 - this.phi));
            }
            double yhat = (level + trendTerm) + seasonal;
            if (Double.isNaN(yhat) || Double.isInfinite(yhat)) yhat = 0.0;
            if (yhat < 0) yhat = 0.0;

            // 业务稳健性约束：限制单步变化幅度，避免趋势项在噪声下“发散”
            if (maxStepDelta > 0) {
                yhat = clamp(yhat, prev - maxStepDelta, prev + maxStepDelta);
                if (yhat < 0) yhat = 0.0;
            }
            prev = yhat;
            out.add(yhat);
        }
        return out;
    }

    /**
     * 滚动递推预测（Recursive / Rolling Forecast）：
     * - 每一步只做“一步预测”，并把该预测值当作“下一步的观测值”继续更新 level/trend/seasonal
     * - 相比一次性 multi-step 公式外推，更贴合“每个新预测点加入下一步考虑范围”的直觉
     *
     * 注意：这种方法会累积误差，但在“业务展示”场景通常更平滑、更少出现机械重复。
     */
    public List<Double> forecastIterative(int steps) {
        if (steps <= 0) return Collections.emptyList();
        if (y == null || y.isEmpty()) {
            List<Double> out = new ArrayList<>(steps);
            for (int i = 0; i < steps; i++) out.add(0.0);
            return out;
        }

        int n = y.size();
        List<Double> out = new ArrayList<>(steps);
        double maxStepDelta = estimateMaxStepDelta(y);
        double prev = y.get(n - 1);

        // 本地状态（不污染模型本体，便于同一个模型多次调用）
        double lt = this.level;
        double bt = this.trend;

        // 取最后一个完整季节周期的季节项作为未来起点（加性）
        double[] seasonalBase = new double[Math.max(1, this.m)];
        if (this.m > 1 && this.seasonals != null && !this.seasonals.isEmpty()) {
            int start = Math.max(0, n - this.m);
            for (int i = 0; i < this.m; i++) {
                int idx = start + i;
                if (idx >= 0 && idx < this.seasonals.size()) {
                    seasonalBase[i] = this.seasonals.get(idx);
                } else {
                    seasonalBase[i] = 0.0;
                }
            }
        } else {
            seasonalBase[0] = 0.0;
        }

        for (int h = 1; h <= steps; h++) {
            double sBase = (this.m > 1) ? seasonalBase[(h - 1) % this.m] : 0.0;

            // 预测期季节项衰减（psi）：越远季节振幅越小
            double sUsed = sBase;
            if (this.m > 1 && Math.abs(this.psi - 1.0) > 1e-12) {
                sUsed = sUsed * Math.pow(this.psi, h);
            }

            // 一步预测（阻尼趋势）：lt + phi*bt + seasonal
            double yhat = (lt + this.phi * bt) + sUsed;
            if (Double.isNaN(yhat) || Double.isInfinite(yhat)) yhat = 0.0;
            if (yhat < 0) yhat = 0.0;

            // 单步变动钳制：基于历史波动范围控制每一步变化，避免发散或异常跳变
            if (maxStepDelta > 0) {
                yhat = clamp(yhat, prev - maxStepDelta, prev + maxStepDelta);
                if (yhat < 0) yhat = 0.0;
            }

            // 把该预测值作为“下一步的观测值”更新状态（recursive）
            double newLevel = this.alpha * (yhat - sUsed) + (1.0 - this.alpha) * (lt + this.phi * bt);
            double newTrend = this.beta * (newLevel - lt) + (1.0 - this.beta) * (this.phi * bt);
            if (this.m > 1) {
                double newSeasonal = this.gamma * (yhat - newLevel) + (1.0 - this.gamma) * sBase;
                seasonalBase[(h - 1) % this.m] = newSeasonal;
            }

            lt = newLevel;
            bt = newTrend;
            prev = yhat;
            out.add(yhat);
        }

        return out;
    }

    public List<Double> getFitted() {
        return fitted;
    }

    private static double mean(List<Double> y, int start, int endExclusive) {
        int n = y.size();
        int a = Math.max(0, start);
        int b = Math.min(n, endExclusive);
        if (a >= b) return 0.0;
        double sum = 0.0;
        for (int i = a; i < b; i++) sum += y.get(i);
        return sum / (b - a);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double clampPhi(double v) {
        // phi ∈ (0,1]；过小会导致趋势项几乎消失，过大会变成线性外推
        if (Double.isNaN(v) || Double.isInfinite(v)) return 1.0;
        if (v <= 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /**
     * 从历史序列估计“单日最大合理变动”，用于限制预测发散。
     * 采用最近窗口的一阶差分绝对值的中位数（robust），再乘以倍数；并叠加一个最小比例阈值。
     */
    private static double estimateMaxStepDelta(List<Double> y) {
        if (y == null || y.size() < 2) return 0.0;
        int n = y.size();
        int window = Math.min(30, n - 1);
        List<Double> diffs = new ArrayList<>();
        for (int i = n - window; i < n; i++) {
            if (i <= 0) continue;
            double d = y.get(i) - y.get(i - 1);
            diffs.add(Math.abs(d));
        }
        if (diffs.isEmpty()) return 0.0;
        Collections.sort(diffs);
        double med = diffs.get(diffs.size() / 2);
        double last = y.get(n - 1);
        double minDelta = Math.max(0.02, Math.abs(last) * 0.02); // 至少允许 ~2%/天
        double robustDelta = med * 8.0; // 允许约 8×中位差分（可抵抗偶发噪声）
        return Math.max(minDelta, robustDelta);
    }
}


