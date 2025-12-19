// src/main/java/service/farmer/PricePredictionService.java
package service.farmer;

import dto.farmer.PricePredictionResponseDTO;
import util.ExcelParser;
import util.PriceFileParser;
import util.HoltWintersModel;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 价格预测服务
 */
public class PricePredictionService {
    
    // 存储上传的文件数据（实际项目中应使用数据库或缓存）
    private static final Map<String, Map<String, List<ExcelParser.DataPoint>>> fileSeriesCache = new HashMap<>();
    
    /**
     * 上传并解析Excel文件
     */
    public Map<String, Object> uploadAndParse(InputStream inputStream, String fileName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            PriceFileParser parser = new PriceFileParser();
            Map<String, List<ExcelParser.DataPoint>> seriesMap = parser.parse(inputStream, fileName);
            
            // 生成文件ID
            String fileId = UUID.randomUUID().toString();
            
            // 缓存数据
            fileSeriesCache.put(fileId, seriesMap);
            
            // 构建预览数据
            // 注意：预览仅显示前10条用于UI展示，但预测时会处理所有数据，不会遗漏任何一条
            List<Map<String, Object>> previewData = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            
            // 统计所有数据（所有规格/类型的所有数据点都会被处理）
            List<Map<String, Object>> flat = new ArrayList<>();
            int totalRecords = 0;
            for (Map.Entry<String, List<ExcelParser.DataPoint>> entry : seriesMap.entrySet()) {
                String spec = entry.getKey();
                List<ExcelParser.DataPoint> points = entry.getValue();
                if (points == null) continue;
                totalRecords += points.size(); // 累计所有规格的数据点总数
                for (ExcelParser.DataPoint point : points) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("type", spec); // 规格/类型
                    item.put("date", sdf.format(point.getDate()));
                    item.put("price", point.getPrice());
                    flat.add(item);
                }
            }
            // 按日期排序，但只返回前10条作为预览（UI限制）
            flat.sort(Comparator.comparing(m -> (String) m.get("date")));
            int previewSize = Math.min(10, flat.size());
            for (int i = 0; i < previewSize; i++) {
                previewData.add(flat.get(i));
            }
            // 重要：totalRecords 包含所有数据点，预测时会全部使用
            
            result.put("file_id", fileId);
            result.put("preview_data", previewData);
            result.put("total_records", totalRecords);
            result.put("types", new ArrayList<>(seriesMap.keySet()));
            
        } catch (Exception e) {
            throw new RuntimeException("解析文件失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 预测价格
     */
    public PricePredictionResponseDTO predict(String fileId, int predictionDays, String modelType) {
        // 从缓存获取数据
        Map<String, List<ExcelParser.DataPoint>> seriesMap = fileSeriesCache.get(fileId);
        if (seriesMap == null || seriesMap.isEmpty()) {
            throw new IllegalArgumentException("文件数据不存在或已过期，请重新上传");
        }
        
        // 限制预测天数
        if (predictionDays < 1 || predictionDays > 90) {
            throw new IllegalArgumentException("预测天数必须在1-90天之间");
        }

        // 按"规格/类型"分别预测，并在响应里返回 series_data
        // 重要：会处理所有规格/类型的所有数据点，不会遗漏任何一条
        List<Map<String, Object>> seriesData = new ArrayList<>();

        // 为保持旧UI兼容，仍填充 historical_data / predicted_data / metrics / trend / calculation_details（取第一条规格）
        String primaryType = seriesMap.keySet().stream().sorted().findFirst().orElse("默认");
        SeriesPrediction primary = null;

        // 遍历所有规格/类型，每个规格的所有数据点都会被处理
        for (Map.Entry<String, List<ExcelParser.DataPoint>> entry : seriesMap.entrySet()) {
            String type = entry.getKey() != null ? entry.getKey() : "默认";
            List<ExcelParser.DataPoint> points = entry.getValue();
            if (points == null || points.isEmpty()) continue;

            // 对当前规格的所有数据点进行预测（不会限制数据量）
            SeriesPrediction sp = predictOneSeries(points, predictionDays);
            if (type.equals(primaryType)) {
                primary = sp;
            }

            Map<String, Object> one = new HashMap<>();
            one.put("type", type);
            one.put("historical_data", sp.historicalData);
            one.put("predicted_data", sp.predictedData);
            one.put("trend", sp.trend);
            // 这里不返回每条规格的 calculation_details，避免体积过大（主规格仍会返回）
            one.put("model_metrics", sp.metricsMapAsObject());
            seriesData.add(one);
        }

        seriesData.sort(Comparator.comparing(m -> (String) m.get("type")));

        if (primary == null) {
            // 理论上不会发生（seriesMap非空），兜底
            throw new IllegalArgumentException("文件中没有可用的数据序列");
        }

        PricePredictionResponseDTO response = new PricePredictionResponseDTO();
        response.setSeriesData(seriesData);

        // 旧字段（主规格）
        response.setHistoricalData(primary.historicalData);
        response.setPredictedData(primary.predictedData);
        response.setModelMetrics(primary.metrics);
        response.setTrend(primary.trend);
        response.setCalculationDetails(primary.calculationDetails);

        return response;
    }

    private static class SeriesPrediction {
        List<Map<String, Object>> historicalData;
        List<Map<String, Object>> predictedData;
        Map<String, Double> metrics;
        String trend;
        Map<String, Object> calculationDetails;

        Map<String, Object> metricsMapAsObject() {
            Map<String, Object> m = new HashMap<>();
            if (metrics != null) {
                for (Map.Entry<String, Double> e : metrics.entrySet()) {
                    m.put(e.getKey(), e.getValue());
                }
            }
            return m;
        }
    }

    /**
     * 预测单条规格序列
     */
    private SeriesPrediction predictOneSeries(List<ExcelParser.DataPoint> rawPoints, int predictionDays) {
        // 统一按天聚合（同一天多条记录取平均）
        List<ExcelParser.DataPoint> dataPoints = normalizeDaily(rawPoints);
        if (dataPoints.size() < 2) {
            // 数据太少：返回“持平外推”
            return naivePredict(dataPoints, predictionDays);
        }

        // 数据预处理：去除异常值
        List<ExcelParser.DataPoint> cleanedData = removeOutliers(dataPoints);

        // 补齐缺失日期（保证等间隔“按天”序列，ETS/HW 更稳定）
        List<ExcelParser.DataPoint> filledData = fillMissingDays(cleanedData);
        if (filledData.size() < 2) {
            return naivePredict(filledData, predictionDays);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 构建历史数据
        List<Map<String, Object>> historicalData = new ArrayList<>();
        for (ExcelParser.DataPoint point : filledData) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", sdf.format(point.getDate()));
            item.put("price", point.getPrice());
            historicalData.add(item);
        }

        // ========= ETS(Holt-Winters) + 留出集回测自动选参 =========
        List<Double> y = new ArrayList<>();
        for (ExcelParser.DataPoint p : filledData) y.add(p.getPrice());

        // 缺失日期补齐比例：补齐（carry-forward）容易制造“伪周周期”，季节性判定需惩罚
        double imputedFraction = 0.0;
        if (filledData.size() > 0) {
            imputedFraction = Math.max(0.0, (filledData.size() - cleanedData.size()) / (double) filledData.size());
        }

        ModelSelection selection = selectBestModel(y, imputedFraction, predictionDays);

        List<Double> forecast;
        if ("naive".equals(selection.modelName)) {
            forecast = forecastNaive(y, predictionDays);
        } else if ("drift".equals(selection.modelName)) {
            forecast = forecastNaiveDrift(y, predictionDays, selection.driftLookback, selection.driftPhi);
        } else if ("theta".equals(selection.modelName)) {
            forecast = forecastTheta(y, predictionDays, selection.thetaAlpha, selection.thetaPhi);
        } else {
            HoltWintersModel model = new HoltWintersModel();
            model.fit(y, selection.seasonLength, selection.alpha, selection.beta, selection.gamma, selection.phi, selection.psi);
            // 中期优先使用“多步外推”(direct)；短期可用递推(rolling)来更贴近短期波动
            forecast = "rolling_recursive".equals(selection.forecastStrategy)
                ? model.forecastIterative(predictionDays)
                : model.forecast(predictionDays);
        }

        // 预测未来数据（按天递增日期）
        List<Map<String, Object>> predictedData = new ArrayList<>();
        Date lastDate = filledData.get(filledData.size() - 1).getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        for (int i = 0; i < predictionDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date futureDate = cal.getTime();
            double predictedPrice = (i < forecast.size()) ? forecast.get(i) : 0.0;
            if (Double.isNaN(predictedPrice) || Double.isInfinite(predictedPrice)) predictedPrice = 0.0;
            if (predictedPrice < 0) predictedPrice = 0.0;

            Map<String, Object> item = new HashMap<>();
            item.put("date", sdf.format(futureDate));
            item.put("price", Math.round(predictedPrice * 100.0) / 100.0);
            predictedData.add(item);
        }

        // 构建详细计算过程（仅用于主规格）
        Map<String, Object> calculationDetails = new HashMap<>();

        // 数据预处理详情
        Map<String, Object> preprocessingDetails = new HashMap<>();
        preprocessingDetails.put("original_count", dataPoints.size());
        preprocessingDetails.put("cleaned_count", cleanedData.size());
        preprocessingDetails.put("removed_count", dataPoints.size() - cleanedData.size());
        preprocessingDetails.put("filled_count", filledData.size());
        preprocessingDetails.put("imputed_fraction", round4(imputedFraction));
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

        // 模型选择与参数详情（ETS/HW + 回测）
        Map<String, Object> modelDetails = new HashMap<>();
        modelDetails.put("model_name", selection.modelDisplayName);
        modelDetails.put("selection_method", "多折中期回测（CV）最小RMSE优先，其次MAE；并与最佳基线(naive/drift)对比");
        modelDetails.put("baseline_name", selection.baselineName);
        modelDetails.put("drift_lookback", selection.driftLookback);
        modelDetails.put("drift_phi", round4(selection.driftPhi));
        modelDetails.put("theta_alpha", round4(selection.thetaAlpha));
        modelDetails.put("theta_phi", round4(selection.thetaPhi));
        modelDetails.put("season_length", selection.seasonLength);
        modelDetails.put("alpha", round4(selection.alpha));
        modelDetails.put("beta", round4(selection.beta));
        modelDetails.put("gamma", round4(selection.gamma));
        modelDetails.put("phi", round4(selection.phi));
        modelDetails.put("psi", round4(selection.psi));
        modelDetails.put("seasonality_strength", round4(
            selection.seasonLength > 1 ? seasonalStrength(y, selection.seasonLength, imputedFraction) : 0.0
        ));
        modelDetails.put("forecast_strategy", selection.forecastStrategy);
        modelDetails.put("cv_folds", selection.cvFolds);
        modelDetails.put("holdout_size", selection.holdoutSize);
        modelDetails.put("holdout_metrics", selection.holdoutMetricsAsObject());
        modelDetails.put("baseline_metrics", selection.baselineMetricsAsObject());
        calculationDetails.put("model_selection", modelDetails);

        // 预测过程详情
        List<Map<String, Object>> predictionDetails = new ArrayList<>();
        cal.setTime(lastDate);
        for (int i = 0; i < predictionDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            double predictedPrice = (i < forecast.size()) ? forecast.get(i) : 0.0;
            Map<String, Object> detail = new HashMap<>();
            detail.put("step", i + 1);
            detail.put("date", sdf.format(cal.getTime()));
            detail.put("predicted_price", Math.round(predictedPrice * 100.0) / 100.0);
            detail.put("formula", selection.formulaHint);
            predictionDetails.add(detail);
        }
        calculationDetails.put("prediction_steps", predictionDetails);

        // 指标与趋势
        Map<String, Double> metricsMap = new HashMap<>();
        // 以“回测holdout”指标作为更可靠的参考；若无holdout，则退化使用全序列的简单误差
        metricsMap.put("r_squared", round4(selection.r2));
        metricsMap.put("mae", round2(selection.mae));
        metricsMap.put("rmse", round2(selection.rmse));
        metricsMap.put("mape", round4(selection.mape));
        metricsMap.put("aic", 0.0); // ETS不提供AIC（此处保留字段以兼容旧UI）

        String trend = determineTrendFromForecast(forecast);

        SeriesPrediction sp = new SeriesPrediction();
        sp.historicalData = historicalData;
        sp.predictedData = predictedData;
        sp.metrics = metricsMap;
        sp.trend = trend;
        sp.calculationDetails = calculationDetails;
        return sp;
    }

    private SeriesPrediction naivePredict(List<ExcelParser.DataPoint> dataPoints, int predictionDays) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> historicalData = new ArrayList<>();
        for (ExcelParser.DataPoint p : dataPoints) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", sdf.format(p.getDate()));
            item.put("price", p.getPrice());
            historicalData.add(item);
        }

        double lastPrice = dataPoints.isEmpty() ? 0.0 : dataPoints.get(dataPoints.size() - 1).getPrice();
        Date lastDate = dataPoints.isEmpty() ? new Date() : dataPoints.get(dataPoints.size() - 1).getDate();

        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        List<Map<String, Object>> predictedData = new ArrayList<>();
        for (int i = 0; i < predictionDays; i++) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Map<String, Object> item = new HashMap<>();
            item.put("date", sdf.format(cal.getTime()));
            item.put("price", Math.round(lastPrice * 100.0) / 100.0);
            predictedData.add(item);
        }

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("r_squared", 0.0);
        metrics.put("mae", 0.0);
        metrics.put("rmse", 0.0);
        metrics.put("aic", 0.0);

        Map<String, Object> calc = new HashMap<>();
        calc.put("note", "数据量不足，使用持平外推（naive）作为预测");

        SeriesPrediction sp = new SeriesPrediction();
        sp.historicalData = historicalData;
        sp.predictedData = predictedData;
        sp.metrics = metrics;
        sp.trend = "平稳";
        sp.calculationDetails = calc;
        return sp;
    }

    private List<ExcelParser.DataPoint> normalizeDaily(List<ExcelParser.DataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.isEmpty()) return Collections.emptyList();

        Map<Long, List<Double>> byDay = new HashMap<>();
        for (ExcelParser.DataPoint p : dataPoints) {
            if (p == null || p.getDate() == null) continue;
            long dayKey = truncateToDay(p.getDate()).getTime();
            byDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(p.getPrice());
        }

        List<ExcelParser.DataPoint> normalized = new ArrayList<>();
        for (Map.Entry<Long, List<Double>> e : byDay.entrySet()) {
            List<Double> prices = e.getValue();
            if (prices == null || prices.isEmpty()) continue;
            double avg = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            normalized.add(new ExcelParser.DataPoint(new Date(e.getKey()), avg));
        }

        normalized.sort(Comparator.comparing(ExcelParser.DataPoint::getDate));
        return normalized;
    }

    private Date truncateToDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 补齐缺失日期（按天等间隔）：缺失天采用“线性插值”，避免前值填充导致阶梯型序列，
     * 进而诱发模型识别出“伪季节性”，使预测呈现机械锯齿。
     *
     * 规则：
     * - 两个已知点之间的缺失天：线性插值
     * - 序列尾部（没有下一已知点）：沿用最后一个值（carry-forward）
     */
    private List<ExcelParser.DataPoint> fillMissingDays(List<ExcelParser.DataPoint> points) {
        if (points == null || points.size() < 2) return points == null ? Collections.emptyList() : points;

        List<ExcelParser.DataPoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparing(ExcelParser.DataPoint::getDate));

        // 先把日期截断到天，确保严格按天对齐
        List<ExcelParser.DataPoint> daily = new ArrayList<>();
        for (ExcelParser.DataPoint p : sorted) {
            if (p == null || p.getDate() == null) continue;
            daily.add(new ExcelParser.DataPoint(truncateToDay(p.getDate()), p.getPrice()));
        }
        if (daily.size() < 2) return daily;

        List<ExcelParser.DataPoint> out = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < daily.size() - 1; i++) {
            ExcelParser.DataPoint a = daily.get(i);
            ExcelParser.DataPoint b = daily.get(i + 1);
            Date da = a.getDate();
            Date db = b.getDate();
            double pa = a.getPrice();
            double pb = b.getPrice();

            // 写入起点（避免重复，只有第一段写a）
            if (out.isEmpty()) out.add(new ExcelParser.DataPoint(da, pa));

            long daysGap = (db.getTime() - da.getTime()) / (24L * 60 * 60 * 1000);
            if (daysGap <= 0) continue;

            // 中间缺失天：线性插值
            for (int d = 1; d < daysGap; d++) {
                cal.setTime(da);
                cal.add(Calendar.DAY_OF_MONTH, d);
                Date cur = cal.getTime();
                double frac = (double) d / (double) daysGap;
                double interp = pa + frac * (pb - pa);
                out.add(new ExcelParser.DataPoint(cur, interp));
            }

            // 写入终点
            out.add(new ExcelParser.DataPoint(db, pb));
        }

        // 如果需要，补齐到最后一天之后的缺失（通常不会发生；这里保持语义：尾部 carry-forward）
        return out;
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

    // ===================== 模型选择 / 指标工具 =====================

    private static class ModelSelection {
        String modelName; // "hw" | "naive" | "drift" | "theta"
        String modelDisplayName;
        String formulaHint;
        int seasonLength;
        double alpha;
        double beta;
        double gamma;
        double phi; // 阻尼趋势参数（damped trend），phi=1 为原线性趋势
        double psi; // 季节项预测衰减参数（0,1]，psi=1 不衰减
        String forecastStrategy; // "direct_multistep" | "rolling_recursive"
        int holdoutSize;
        int cvFolds;

        // 最佳基线（用于对比/展示）
        String baselineName; // "naive" | "drift"

        // drift 参数
        int driftLookback;
        double driftPhi;

        // theta 参数
        double thetaAlpha;
        double thetaPhi;

        // 选中模型的 holdout 指标
        double mae;
        double rmse;
        double mape;
        double r2;

        // naive 基线指标（同一 holdout）
        double baselineMae;
        double baselineRmse;
        double baselineMape;
        double baselineR2;

        Map<String, Object> holdoutMetricsAsObject() {
            Map<String, Object> m = new HashMap<>();
            m.put("mae", round2(mae));
            m.put("rmse", round2(rmse));
            m.put("mape", round4(mape));
            m.put("r_squared", round4(r2));
            return m;
        }

        Map<String, Object> baselineMetricsAsObject() {
            Map<String, Object> m = new HashMap<>();
            m.put("mae", round2(baselineMae));
            m.put("rmse", round2(baselineRmse));
            m.put("mape", round4(baselineMape));
            m.put("r_squared", round4(baselineR2));
            return m;
        }
    }

    private ModelSelection selectBestModel(List<Double> y, double imputedFraction, int horizonWanted) {
        ModelSelection sel = new ModelSelection();
        sel.modelName = "naive";
        sel.modelDisplayName = "Naive(持平外推)";
        sel.formulaHint = "Naive：预测=训练集最后一个价格（持平外推）";
        sel.seasonLength = 1;
        sel.alpha = 0.0;
        sel.beta = 0.0;
        sel.gamma = 0.0;
        sel.phi = 1.0;
        sel.psi = 1.0;
        sel.forecastStrategy = "direct_multistep";
        sel.cvFolds = 1;
        sel.baselineName = "naive";
        sel.driftLookback = 30;
        sel.driftPhi = 1.0;
        sel.thetaAlpha = 0.3;
        sel.thetaPhi = 1.0;

        if (y == null || y.size() < 2) {
            sel.holdoutSize = 0;
            sel.mae = sel.rmse = sel.mape = sel.r2 = 0.0;
            sel.baselineMae = sel.baselineRmse = sel.baselineMape = sel.baselineR2 = 0.0;
            return sel;
        }

        int n = y.size();
        int horizon = chooseCvHorizon(n, horizonWanted);
        int folds = chooseCvFolds(n, horizon);
        sel.holdoutSize = horizon;
        sel.cvFolds = folds;

        // 中期（horizon较大）默认用 direct_multistep；短期用 rolling_recursive
        // 解释：把预测值当“真实观测”会压平趋势并放大累积误差，对中期一般更差
        String defaultStrategy = (horizon >= 21) ? "direct_multistep" : "rolling_recursive";
        sel.forecastStrategy = defaultStrategy;

        // 基线：naive vs drift（中期价格更常见的是“慢漂移”，drift 通常显著优于持平外推）
        MetricsAgg naiveAgg = backtestNaiveCv(y, horizon, folds);
        MetricsAgg bestBaseline = naiveAgg;
        String bestBaselineName = "naive";
        int bestLb = 30;
        double bestDriftPhi = 1.0;

        int[] lbGrid = new int[]{14, 30, 60, 120};
        double[] driftPhiGrid = new double[]{0.85, 0.9, 0.95, 1.0};
        double bestBaseRmse = naiveAgg.rmse;
        double bestBaseMae = naiveAgg.mae;
        for (int lb : lbGrid) {
            for (double dphi : driftPhiGrid) {
                MetricsAgg agg = backtestNaiveDriftCv(y, horizon, folds, lb, dphi);
                if (isBetterAgg(agg, bestBaseRmse, bestBaseMae)) {
                    bestBaseRmse = agg.rmse;
                    bestBaseMae = agg.mae;
                    bestBaseline = agg;
                    bestBaselineName = "drift";
                    bestLb = lb;
                    bestDriftPhi = dphi;
                }
            }
        }

        sel.baselineName = bestBaselineName;
        sel.driftLookback = bestLb;
        sel.driftPhi = bestDriftPhi;
        sel.baselineMae = bestBaseline.mae;
        sel.baselineRmse = bestBaseline.rmse;
        sel.baselineMape = bestBaseline.mape;
        sel.baselineR2 = bestBaseline.r2;

        // 默认先用“最佳基线”的表现
        sel.modelName = bestBaselineName;
        if ("drift".equals(bestBaselineName)) {
            sel.modelDisplayName = String.format("Naive‑Drift(漂移外推, lookback=%d, φ=%.2f)", bestLb, bestDriftPhi);
            sel.formulaHint = "Naive‑Drift：预测=最后值 + 漂移×(φ+…+φ^h)";
        } else {
            sel.modelDisplayName = "Naive(持平外推)";
            sel.formulaHint = "Naive：预测=训练集最后一个价格（持平外推）";
        }
        sel.mae = sel.baselineMae;
        sel.rmse = sel.baselineRmse;
        sel.mape = sel.baselineMape;
        sel.r2 = sel.baselineR2;

        // Theta 方法（对中期趋势+噪声场景往往比ETS更强）：网格搜索 alpha 与趋势阻尼 phi
        double[] thetaAlphaGrid = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
        double[] thetaPhiGrid = new double[]{0.85, 0.9, 0.95, 1.0};
        double bestThetaRmse = Double.POSITIVE_INFINITY;
        double bestThetaMae = Double.POSITIVE_INFINITY;
        double bestThetaMape = Double.POSITIVE_INFINITY;
        double bestThetaR2 = Double.NEGATIVE_INFINITY;
        double bestThetaAlpha = 0.3;
        double bestThetaPhi = 1.0;
        for (double ta : thetaAlphaGrid) {
            for (double tph : thetaPhiGrid) {
                MetricsAgg agg = backtestThetaCv(y, horizon, folds, ta, tph);
                if (isBetterAgg(agg, bestThetaRmse, bestThetaMae)) {
                    bestThetaRmse = agg.rmse;
                    bestThetaMae = agg.mae;
                    bestThetaMape = agg.mape;
                    bestThetaR2 = agg.r2;
                    bestThetaAlpha = ta;
                    bestThetaPhi = tph;
                }
            }
        }
        if (Double.isFinite(bestThetaRmse) && bestThetaRmse + 1e-9 < sel.rmse) {
            sel.modelName = "theta";
            sel.modelDisplayName = String.format("Theta(中期强化, α=%.2f, φ=%.2f)", bestThetaAlpha, bestThetaPhi);
            sel.formulaHint = "Theta：0.5×(线性趋势外推) + 0.5×(对“θ=2”序列做指数平滑)";
            sel.thetaAlpha = bestThetaAlpha;
            sel.thetaPhi = bestThetaPhi;
            sel.mae = bestThetaMae;
            sel.rmse = bestThetaRmse;
            sel.mape = bestThetaMape;
            sel.r2 = bestThetaR2;
        }

        // ETS/HW：尝试不同季节周期 + 参数（含阻尼趋势phi），并用留出集回测择优
        // 关键：只有当“季节性足够强”时才允许启用季节项，避免出现“未来波浪线式抖动”。
        List<Integer> seasonCandidates = new ArrayList<>();
        seasonCandidates.add(1); // 无季节
        // 注意：季节候选基于“完整序列”的强度筛选（比只看最后一段更稳）
        addSeasonCandidateIfStrong(seasonCandidates, y, 7, imputedFraction);
        addSeasonCandidateIfStrong(seasonCandidates, y, 14, imputedFraction);
        addSeasonCandidateIfStrong(seasonCandidates, y, 30, imputedFraction);

        double bestRmse = Double.POSITIVE_INFINITY;
        double bestMae = Double.POSITIVE_INFINITY;
        double bestMape = Double.POSITIVE_INFINITY;
        double bestR2 = Double.NEGATIVE_INFINITY;
        int bestSeason = 1;
        double bestA = 0.4, bestB = 0.1, bestG = 0.1, bestPhi = 0.9, bestPsi = 1.0;

        // 记录“无季节”最佳，用于抑制误判7天（季节模型必须显著优于无季节才放行）
        double bestRmseNoSeason = Double.POSITIVE_INFINITY;
        double bestMaeNoSeason = Double.POSITIVE_INFINITY;
        double bestMapeNoSeason = Double.POSITIVE_INFINITY;
        double bestR2NoSeason = Double.NEGATIVE_INFINITY;
        double bestANoSeason = 0.4, bestBNoSeason = 0.1, bestPhiNoSeason = 0.9;

        // 更小、更稳的参数网格（避免耗时/过拟合）
        double[] gridA = new double[]{0.2, 0.4, 0.6, 0.8};
        double[] gridB = new double[]{0.05, 0.1, 0.2, 0.3};
        double[] gridG = new double[]{0.1, 0.2, 0.3, 0.5};
        double[] gridPhi = new double[]{0.6, 0.7, 0.8, 0.9, 0.95, 1.0};
        double[] gridPsi = new double[]{0.85, 0.9, 0.95, 1.0};

        for (int seasonLength : seasonCandidates) {
            // 需要至少 2*m 才能稳定估计季节项；不足则跳过该季节候选
            if (seasonLength > 1 && y.size() < 2 * seasonLength) continue;

            for (double a : gridA) {
                for (double b : gridB) {
                    for (double phi : gridPhi) {
                        if (seasonLength == 1) {
                            // 无季节项时 gamma 固定为0
                            try {
                                MetricsAgg agg = backtestEtsCv(y, horizon, folds, 1, a, b, 0.0, phi, 1.0, defaultStrategy);
                                if (isBetterAgg(agg, bestRmse, bestMae)) {
                                    bestRmse = agg.rmse;
                                    bestMae = agg.mae;
                                    bestMape = agg.mape;
                                    bestR2 = agg.r2;
                                    bestSeason = 1;
                                    bestA = a;
                                    bestB = b;
                                    bestG = 0.0;
                                    bestPhi = phi;
                                    bestPsi = 1.0;
                                }
                                if (isBetterAgg(agg, bestRmseNoSeason, bestMaeNoSeason)) {
                                    bestRmseNoSeason = agg.rmse;
                                    bestMaeNoSeason = agg.mae;
                                    bestMapeNoSeason = agg.mape;
                                    bestR2NoSeason = agg.r2;
                                    bestANoSeason = a;
                                    bestBNoSeason = b;
                                    bestPhiNoSeason = phi;
                                }
                            } catch (Exception ignored) {
                            }
                        } else {
                            for (double g : gridG) {
                                for (double psi : gridPsi) {
                                    try {
                                        MetricsAgg agg = backtestEtsCv(y, horizon, folds, seasonLength, a, b, g, phi, psi, defaultStrategy);
                                        if (isBetterAgg(agg, bestRmse, bestMae)) {
                                            bestRmse = agg.rmse;
                                            bestMae = agg.mae;
                                            bestMape = agg.mape;
                                            bestR2 = agg.r2;
                                            bestSeason = seasonLength;
                                            bestA = a;
                                            bestB = b;
                                            bestG = g;
                                            bestPhi = phi;
                                            bestPsi = psi;
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 若 HW 明显优于 naive，则选 HW；否则仍用 naive（避免“看似高级但更差”）
        if (Double.isFinite(bestRmse) && bestRmse + 1e-9 < sel.rmse) {
            // 额外门槛：如果最佳是季节模型，则必须“季节性足够强”且显著优于无季节
            if (bestSeason > 1 && Double.isFinite(bestRmseNoSeason) && bestRmseNoSeason > 1e-12) {
                double strength = seasonalStrength(y, bestSeason, imputedFraction);
                double improvementRatio = 1.0 - (bestRmse / bestRmseNoSeason);
                // 经验阈值：强度>=0.10 且 RMSE 相对提升 >=6%
                boolean allowSeasonal = (strength >= 0.10) && (improvementRatio >= 0.06);
                if (!allowSeasonal && bestRmseNoSeason + 1e-9 < sel.rmse) {
                    // 回退到无季节最佳（解决“误判7天”导致未来波浪线）
                    bestSeason = 1;
                    bestA = bestANoSeason;
                    bestB = bestBNoSeason;
                    bestG = 0.0;
                    bestPhi = bestPhiNoSeason;
                    bestPsi = 1.0;
                    bestRmse = bestRmseNoSeason;
                    bestMae = bestMaeNoSeason;
                    bestMape = bestMapeNoSeason;
                    bestR2 = bestR2NoSeason;
                }
            }
            sel.modelName = "hw";
            boolean damped = bestPhi < 0.9999;
            sel.modelDisplayName = bestSeason > 1
                ? (damped ? String.format("ETS(Holt-Winters, 阻尼趋势φ=%.2f, 加性季节)", bestPhi)
                          : "ETS(Holt-Winters, 加性季节)")
                : (damped ? String.format("ETS(Holt, 阻尼趋势φ=%.2f)", bestPhi)
                          : "ETS(Holt, 线性趋势)");
            sel.forecastStrategy = defaultStrategy;
            sel.formulaHint = "direct_multistep".equals(defaultStrategy)
                ? (bestSeason > 1
                    ? (damped
                        ? "ETS(Holt-Winters, 阻尼趋势, 多步外推)：预测=level + (φ+…+φ^h)×trend + seasonal×ψ^h"
                        : "ETS(Holt-Winters, 多步外推)：预测=(level + h×trend) + seasonal×ψ^h")
                    : (damped
                        ? "ETS(Holt, 阻尼趋势, 多步外推)：预测=level + (φ+…+φ^h)×trend"
                        : "ETS(Holt, 多步外推)：预测=(level + h×trend)"))
                : (bestSeason > 1
                    ? (damped
                        ? "ETS(Holt-Winters, 阻尼趋势, 滚动递推)：每步预测值加入下一步；预测≈(level + φ×trend) + seasonal"
                        : "ETS(Holt-Winters, 滚动递推)：每步预测值加入下一步；预测≈(level + trend) + seasonal")
                    : (damped
                        ? "ETS(Holt, 阻尼趋势, 滚动递推)：每步预测值加入下一步；预测≈(level + φ×trend)"
                        : "ETS(Holt, 滚动递推)：每步预测值加入下一步；预测≈(level + trend)"));
            sel.seasonLength = bestSeason;
            sel.alpha = bestA;
            sel.beta = bestB;
            sel.gamma = bestG;
            sel.phi = bestPhi;
            sel.psi = bestPsi;
            sel.mae = bestMae;
            sel.rmse = bestRmse;
            sel.mape = bestMape;
            sel.r2 = bestR2;
        }

        return sel;
    }

    private static int chooseCvHorizon(int n, int horizonWanted) {
        int hw = (horizonWanted <= 0) ? 30 : horizonWanted;
        // 中期默认 30；上限 45（避免极端难评估），下限 7
        int horizon = Math.max(7, Math.min(hw, 45));
        // 数据太短则缩短
        return Math.min(horizon, Math.max(7, n / 4));
    }

    private static int chooseCvFolds(int n, int horizon) {
        // 至少留出 2*horizon 作为训练，才能做多折
        if (n >= horizon * 5) return 4;
        if (n >= horizon * 4) return 3;
        if (n >= horizon * 3) return 2;
        return 1;
    }

    private static class MetricsAgg {
        double mae;
        double rmse;
        double mape;
        double r2;
    }

    private static boolean isBetterAgg(MetricsAgg agg, double bestRmse, double bestMae) {
        if (agg == null) return false;
        if (!Double.isFinite(agg.rmse) || !Double.isFinite(agg.mae)) return false;
        if (agg.rmse + 1e-9 < bestRmse) return true;
        return Math.abs(agg.rmse - bestRmse) <= 1e-9 && agg.mae + 1e-9 < bestMae;
    }

    private static MetricsAgg backtestNaiveCv(List<Double> y, int horizon, int folds) {
        MetricsAgg out = new MetricsAgg();
        if (y == null || y.size() < horizon + 5) return out;
        int n = y.size();
        int used = 0;
        double sumMae = 0, sumRmse = 0, sumMape = 0, sumR2 = 0;
        for (int f = 0; f < folds; f++) {
            int testStart = n - horizon * (f + 1);
            int testEnd = testStart + horizon;
            if (testStart <= 1 || testEnd > n) break;
            List<Double> train = y.subList(0, testStart);
            List<Double> test = y.subList(testStart, testEnd);
            List<Double> pred = forecastNaive(train, horizon);
            Metrics m = computeMetrics(test, pred);
            sumMae += m.mae;
            sumRmse += m.rmse;
            sumMape += m.mape;
            sumR2 += m.r2;
            used++;
        }
        if (used == 0) return out;
        out.mae = sumMae / used;
        out.rmse = sumRmse / used;
        out.mape = sumMape / used;
        out.r2 = sumR2 / used;
        return out;
    }

    private static MetricsAgg backtestEtsCv(
        List<Double> y,
        int horizon,
        int folds,
        int seasonLength,
        double alpha,
        double beta,
        double gamma,
        double phi,
        double psi,
        String strategy
    ) {
        MetricsAgg out = new MetricsAgg();
        if (y == null || y.size() < horizon + 10) return out;
        int n = y.size();
        int used = 0;
        double sumMae = 0, sumRmse = 0, sumMape = 0, sumR2 = 0;
        for (int f = 0; f < folds; f++) {
            int testStart = n - horizon * (f + 1);
            int testEnd = testStart + horizon;
            if (testStart <= 2 || testEnd > n) break;
            List<Double> train = y.subList(0, testStart);
            List<Double> test = y.subList(testStart, testEnd);

            HoltWintersModel model = new HoltWintersModel();
            model.fit(train, seasonLength, alpha, beta, gamma, phi, psi);
            List<Double> pred = "rolling_recursive".equals(strategy)
                ? model.forecastIterative(horizon)
                : model.forecast(horizon);
            Metrics m = computeMetrics(test, pred);
            sumMae += m.mae;
            sumRmse += m.rmse;
            sumMape += m.mape;
            sumR2 += m.r2;
            used++;
        }
        if (used == 0) return out;
        out.mae = sumMae / used;
        out.rmse = sumRmse / used;
        out.mape = sumMape / used;
        out.r2 = sumR2 / used;
        return out;
    }

    private static class Metrics {
        double mae;
        double rmse;
        double mape;
        double r2;
    }

    private static Metrics computeMetrics(List<Double> actual, List<Double> pred) {
        Metrics m = new Metrics();
        if (actual == null || pred == null || actual.isEmpty() || pred.isEmpty()) {
            m.mae = m.rmse = m.mape = 0.0;
            m.r2 = 0.0;
            return m;
        }
        int n = Math.min(actual.size(), pred.size());
        double sumAbs = 0.0;
        double sumSq = 0.0;
        double sumMape = 0.0;
        int mapeCount = 0;
        double mean = 0.0;
        for (int i = 0; i < n; i++) mean += actual.get(i);
        mean /= n;
        double ssTot = 0.0;
        double ssRes = 0.0;
        for (int i = 0; i < n; i++) {
            double a = actual.get(i);
            double p = pred.get(i);
            if (Double.isNaN(p) || Double.isInfinite(p)) p = 0.0;
            if (p < 0) p = 0.0;
            double e = a - p;
            sumAbs += Math.abs(e);
            sumSq += e * e;
            ssRes += e * e;
            ssTot += (a - mean) * (a - mean);
            if (Math.abs(a) > 1e-9) {
                sumMape += Math.abs(e / a);
                mapeCount++;
            }
        }
        m.mae = sumAbs / n;
        m.rmse = Math.sqrt(sumSq / n);
        m.mape = mapeCount == 0 ? 0.0 : (sumMape / mapeCount);
        double r2 = (ssTot <= 1e-12) ? 0.0 : (1.0 - (ssRes / ssTot));
        if (Double.isNaN(r2) || Double.isInfinite(r2)) r2 = 0.0;
        m.r2 = r2;
        return m;
    }

    private static List<Double> forecastNaive(List<Double> train, int steps) {
        double last = (train == null || train.isEmpty()) ? 0.0 : train.get(train.size() - 1);
        if (Double.isNaN(last) || Double.isInfinite(last)) last = 0.0;
        if (last < 0) last = 0.0;
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < steps; i++) out.add(last);
        return out;
    }

    /**
     * Naive with drift（带漂移外推）：
     * 用最近 lookback 天的线性回归斜率作为漂移，并对漂移进行阻尼（phi）。
     */
    private static List<Double> forecastNaiveDrift(List<Double> series, int steps, int lookback, double phi) {
        if (steps <= 0) return Collections.emptyList();
        if (series == null || series.isEmpty()) return forecastNaive(series, steps);
        int n = series.size();
        int lb = Math.max(3, Math.min(lookback, n));
        int start = n - lb;
        // 线性回归：t=1..lb
        double sumT = 0, sumY = 0, sumTT = 0, sumTY = 0;
        for (int i = 0; i < lb; i++) {
            double t = i + 1;
            double y = series.get(start + i);
            sumT += t;
            sumY += y;
            sumTT += t * t;
            sumTY += t * y;
        }
        double denom = lb * sumTT - sumT * sumT;
        double slope = 0.0;
        if (Math.abs(denom) > 1e-12) {
            slope = (lb * sumTY - sumT * sumY) / denom;
        }
        if (!Double.isFinite(slope)) slope = 0.0;

        double last = series.get(n - 1);
        if (!Double.isFinite(last)) last = 0.0;
        if (last < 0) last = 0.0;
        double dphi = clamp01(phi);

        List<Double> out = new ArrayList<>(steps);
        double prev = last;
        double maxStepDelta = estimateMaxStepDeltaForService(series);
        for (int h = 1; h <= steps; h++) {
            double driftTerm;
            if (Math.abs(dphi - 1.0) < 1e-12) {
                driftTerm = slope * h;
            } else {
                driftTerm = slope * (dphi * (1.0 - Math.pow(dphi, h)) / (1.0 - dphi));
            }
            double yhat = last + driftTerm;
            if (!Double.isFinite(yhat)) yhat = last;
            if (yhat < 0) yhat = 0.0;

            // 轻量约束：防止漂移外推出现离谱跳变（比ETS更松）
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
     * Theta 方法预测（中期常用）：0.5×趋势外推 + 0.5×对 θ=2 序列做指数平滑
     */
    private static List<Double> forecastTheta(List<Double> series, int steps, double alpha, double phi) {
        if (steps <= 0) return Collections.emptyList();
        if (series == null || series.size() < 2) return forecastNaive(series, steps);
        int n = series.size();

        // 线性趋势：y = a + b*t，t=1..n
        double sumT = 0, sumY = 0, sumTT = 0, sumTY = 0;
        for (int i = 0; i < n; i++) {
            double t = i + 1;
            double y = series.get(i);
            sumT += t;
            sumY += y;
            sumTT += t * t;
            sumTY += t * y;
        }
        double denom = n * sumTT - sumT * sumT;
        double b = 0.0;
        double a = sumY / n;
        if (Math.abs(denom) > 1e-12) {
            b = (n * sumTY - sumT * sumY) / denom;
            a = (sumY - b * sumT) / n;
        }
        if (!Double.isFinite(a)) a = 0.0;
        if (!Double.isFinite(b)) b = 0.0;

        // θ=2 序列：y2_t = 2*y_t - trend_t
        double s = 0.0;
        double aa = clamp01(alpha);
        for (int i = 0; i < n; i++) {
            double t = i + 1;
            double trendT = a + b * t;
            double y2 = 2.0 * series.get(i) - trendT;
            if (!Double.isFinite(y2)) y2 = 0.0;
            if (i == 0) s = y2;
            else s = aa * y2 + (1.0 - aa) * s;
        }

        // 预测：0.5 * trendForecast + 0.5 * SES(y2)
        double dphi = clamp01(phi);
        double trendAtN = a + b * n;
        List<Double> out = new ArrayList<>(steps);
        double prev = series.get(n - 1);
        double maxStepDelta = estimateMaxStepDeltaForService(series);
        for (int h = 1; h <= steps; h++) {
            double slopeTerm;
            if (Math.abs(dphi - 1.0) < 1e-12) {
                slopeTerm = b * h;
            } else {
                slopeTerm = b * (dphi * (1.0 - Math.pow(dphi, h)) / (1.0 - dphi));
            }
            double trendF = trendAtN + slopeTerm;
            double yhat = 0.5 * trendF + 0.5 * s;
            if (!Double.isFinite(yhat)) yhat = prev;
            if (yhat < 0) yhat = 0.0;
            if (maxStepDelta > 0) {
                yhat = clamp(yhat, prev - maxStepDelta, prev + maxStepDelta);
                if (yhat < 0) yhat = 0.0;
            }
            prev = yhat;
            out.add(yhat);
        }
        return out;
    }

    private static MetricsAgg backtestNaiveDriftCv(List<Double> y, int horizon, int folds, int lookback, double phi) {
        MetricsAgg out = new MetricsAgg();
        if (y == null || y.size() < horizon + 10) return out;
        int n = y.size();
        int used = 0;
        double sumMae = 0, sumRmse = 0, sumMape = 0, sumR2 = 0;
        for (int f = 0; f < folds; f++) {
            int testStart = n - horizon * (f + 1);
            int testEnd = testStart + horizon;
            if (testStart <= 2 || testEnd > n) break;
            List<Double> train = y.subList(0, testStart);
            List<Double> test = y.subList(testStart, testEnd);
            List<Double> pred = forecastNaiveDrift(train, horizon, lookback, phi);
            Metrics m = computeMetrics(test, pred);
            sumMae += m.mae;
            sumRmse += m.rmse;
            sumMape += m.mape;
            sumR2 += m.r2;
            used++;
        }
        if (used == 0) return out;
        out.mae = sumMae / used;
        out.rmse = sumRmse / used;
        out.mape = sumMape / used;
        out.r2 = sumR2 / used;
        return out;
    }

    private static MetricsAgg backtestThetaCv(List<Double> y, int horizon, int folds, double alpha, double phi) {
        MetricsAgg out = new MetricsAgg();
        if (y == null || y.size() < horizon + 10) return out;
        int n = y.size();
        int used = 0;
        double sumMae = 0, sumRmse = 0, sumMape = 0, sumR2 = 0;
        for (int f = 0; f < folds; f++) {
            int testStart = n - horizon * (f + 1);
            int testEnd = testStart + horizon;
            if (testStart <= 2 || testEnd > n) break;
            List<Double> train = y.subList(0, testStart);
            List<Double> test = y.subList(testStart, testEnd);
            List<Double> pred = forecastTheta(train, horizon, alpha, phi);
            Metrics m = computeMetrics(test, pred);
            sumMae += m.mae;
            sumRmse += m.rmse;
            sumMape += m.mape;
            sumR2 += m.r2;
            used++;
        }
        if (used == 0) return out;
        out.mae = sumMae / used;
        out.rmse = sumRmse / used;
        out.mape = sumMape / used;
        out.r2 = sumR2 / used;
        return out;
    }

    // 服务侧轻量“单步变动”约束：放宽ETS里的限制，避免中期被压成直线
    private static double estimateMaxStepDeltaForService(List<Double> y) {
        if (y == null || y.size() < 2) return 0.0;
        int n = y.size();
        int window = Math.min(60, n - 1);
        List<Double> diffs = new ArrayList<>();
        for (int i = n - window; i < n; i++) {
            if (i <= 0) continue;
            double d = y.get(i) - y.get(i - 1);
            diffs.add(Math.abs(d));
        }
        if (diffs.isEmpty()) return 0.0;
        Collections.sort(diffs);
        double p90 = diffs.get(Math.min(diffs.size() - 1, (int) Math.floor(diffs.size() * 0.9)));
        double last = y.get(n - 1);
        double minDelta = Math.max(0.05, Math.abs(last) * 0.05); // 至少允许 ~5%/天（比ETS更松）
        double robustDelta = p90 * 2.0;
        return Math.max(minDelta, robustDelta);
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 1.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /**
     * 用预测段本身的斜率判断趋势（更符合用户看到的图），避免“趋势标签”和预测曲线相矛盾。
     */
    private static String determineTrendFromForecast(List<Double> forecast) {
        if (forecast == null || forecast.size() < 2) return "平稳";
        int look = Math.min(14, forecast.size() - 1);
        double sum = 0.0;
        for (int i = 0; i < look; i++) {
            sum += (forecast.get(i + 1) - forecast.get(i));
        }
        double avg = sum / look;
        if (avg > 0.01) return "上升";
        if (avg < -0.01) return "下降";
        return "平稳";
    }

    /**
     * 仅当季节性“强度足够高”、且周期数量足够多、且补齐比例不高时，才允许启用季节项。
     */
    private static void addSeasonCandidateIfStrong(List<Integer> out, List<Double> series, int period, double imputedFraction) {
        if (out == null || series == null) return;
        if (period <= 1) return;
        // 补齐比例高时（carry-forward）很容易制造“伪周周期”，直接不启用季节项
        if (imputedFraction > 0.12) return;
        // 至少 4 个完整周期才考虑（周季节性至少 ~1个月观察）
        if (series.size() < period * 4) return;
        double strength = seasonalStrength(series, period, imputedFraction);
        // 阈值更严格：只有明显季节性才放行
        if (strength >= 0.12) out.add(period);
    }

    /**
     * 季节性强度（0~1）：用“相位均值的方差 / 总方差”衡量周期解释力，并对补齐比例进行惩罚。
     * 相比 lag-correlation，这个指标更不容易被台阶/补齐伪周期误导。
     */
    private static double seasonalStrength(List<Double> series, int period, double imputedFraction) {
        if (series == null || series.size() < period * 2 || period <= 1) return 0.0;
        int n = series.size();
        double mean = series.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double var = 0.0;
        for (double v : series) {
            double d = v - mean;
            var += d * d;
        }
        var /= Math.max(1, n);
        if (var < 1e-12) return 0.0;

        double[] phaseSum = new double[period];
        int[] phaseCnt = new int[period];
        for (int i = 0; i < n; i++) {
            int p = i % period;
            phaseSum[p] += series.get(i);
            phaseCnt[p] += 1;
        }
        double[] phaseMean = new double[period];
        for (int p = 0; p < period; p++) {
            phaseMean[p] = phaseCnt[p] == 0 ? mean : (phaseSum[p] / phaseCnt[p]);
        }
        double pm = 0.0;
        for (double v : phaseMean) pm += v;
        pm /= period;
        double varPhase = 0.0;
        for (double v : phaseMean) {
            double d = v - pm;
            varPhase += d * d;
        }
        varPhase /= period;

        double strength = varPhase / var;
        // 对补齐比例惩罚：补齐越多，越可能是伪周期
        double penalty = 1.0 - Math.max(0.0, Math.min(0.9, imputedFraction));
        strength *= (penalty * penalty);
        if (Double.isNaN(strength) || Double.isInfinite(strength)) return 0.0;
        if (strength < 0) return 0.0;
        return Math.min(1.0, strength);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}

