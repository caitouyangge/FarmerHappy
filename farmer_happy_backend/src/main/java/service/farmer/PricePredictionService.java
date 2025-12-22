// src/main/java/service/farmer/PricePredictionService.java
package service.farmer;

import dto.farmer.PricePredictionResponseDTO;
import util.ExcelParser;
import util.PriceFileParser;
import util.ARIMAModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

        // 如果使用AI预测
        if ("ai".equals(modelType)) {
            return predictWithAI(seriesMap, predictionDays);
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
        Map<String, Object> historicalFeatures; // 历史价格特征分析
        String predictionReason; // 预测理由

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

        // ========= ARIMA模型 + 留出集回测自动选参 =========
        List<Double> y = new ArrayList<>();
        for (ExcelParser.DataPoint p : filledData) y.add(p.getPrice());

        // 缺失日期补齐比例：补齐（carry-forward）容易制造"伪周周期"，季节性判定需惩罚
        double imputedFraction = 0.0;
        if (filledData.size() > 0) {
            imputedFraction = Math.max(0.0, (filledData.size() - cleanedData.size()) / (double) filledData.size());
        }

        ModelSelection selection = selectBestARIMAModel(y, filledData, imputedFraction, predictionDays);

        // 使用ARIMA模型进行预测
        List<Double> forecast;
        List<ARIMAModel.Point> arimaPoints = new ArrayList<>();
        for (int i = 0; i < filledData.size(); i++) {
            arimaPoints.add(new ARIMAModel.Point(i + 1, y.get(i)));
        }
        ARIMAModel model = new ARIMAModel();
        model.train(arimaPoints, selection.arimaParams);
        forecast = new ArrayList<>();
        for (int i = 1; i <= predictionDays; i++) {
            double pred = model.predict(i);
            if (Double.isNaN(pred) || Double.isInfinite(pred) || pred < 0) {
                // 如果预测值不合理，使用最近几个值的平均值作为兜底
                int lookback = Math.min(5, y.size());
                double avg = y.subList(y.size() - lookback, y.size()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(y.get(y.size() - 1));
                pred = avg;
            }
            forecast.add(pred);
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

        // 模型选择与参数详情（ARIMA + 回测）
        Map<String, Object> modelDetails = new HashMap<>();
        modelDetails.put("model_name", selection.modelDisplayName);
        modelDetails.put("selection_method", "多折中期回测（CV）最小RMSE优先，其次MAE；自动选择最优ARIMA参数，只选择R² > 0的模型");
        if (selection.arimaParams != null) {
            modelDetails.put("arima_params", selection.arimaParams.toString());
            modelDetails.put("ar_p", selection.arimaParams.p);
            modelDetails.put("ar_d", selection.arimaParams.d);
            modelDetails.put("ar_q", selection.arimaParams.q);
            if (selection.arimaParams.isSeasonal()) {
                modelDetails.put("sarima_P", selection.arimaParams.P);
                modelDetails.put("sarima_D", selection.arimaParams.D);
                modelDetails.put("sarima_Q", selection.arimaParams.Q);
                modelDetails.put("sarima_s", selection.arimaParams.s);
            }
        }
        modelDetails.put("cv_folds", selection.cvFolds);
        modelDetails.put("holdout_size", selection.holdoutSize);
        modelDetails.put("holdout_metrics", selection.holdoutMetricsAsObject());
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
        metricsMap.put("aic", round2(selection.aic)); // ARIMA提供AIC

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
        double aic;

        // naive 基线指标（同一 holdout）
        double baselineMae;
        double baselineRmse;
        double baselineMape;
        double baselineR2;

        // ARIMA参数
        ARIMAModel.ARIMAParams arimaParams;

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

    /**
     * 选择最佳ARIMA模型（使用回测选择最优参数）
     * 通过网格搜索找到表现最好的参数组合，只选择R² > 0的模型
     */
    private ModelSelection selectBestARIMAModel(List<Double> y, List<ExcelParser.DataPoint> filledData, double imputedFraction, int horizonWanted) {
        ModelSelection sel = new ModelSelection();
        sel.modelName = "arima";
        sel.baselineName = "none";

        if (y == null || y.size() < 10) {
            throw new IllegalArgumentException("数据量不足，至少需要10条数据才能使用ARIMA模型");
        }

        int n = y.size();
        int horizon = chooseCvHorizon(n, horizonWanted);
        int folds = chooseCvFolds(n, horizon);
        sel.holdoutSize = horizon;
        sel.cvFolds = folds;

        // 构建ARIMA数据点
        List<ARIMAModel.Point> arimaPoints = new ArrayList<>();
        for (int i = 0; i < filledData.size(); i++) {
            arimaPoints.add(new ARIMAModel.Point(i + 1, y.get(i)));
        }

        // 网格搜索最佳ARIMA参数
        // 优先尝试简单的模型，避免过度复杂导致负R²
        ARIMAModel.ARIMAParams bestParams = null;
        MetricsAgg bestAgg = null;
        double bestScore = Double.POSITIVE_INFINITY;

        // 定义参数网格（限制复杂度，优先简单模型）
        int[] pGrid = {0, 1, 2}; // AR阶数
        int[] dGrid = {0, 1}; // 差分阶数（最多1次，避免过度差分）
        int[] qGrid = {0, 1, 2}; // MA阶数

        // 首先尝试非季节性ARIMA模型
        for (int p : pGrid) {
            for (int d : dGrid) {
                for (int q : qGrid) {
                    // 跳过过于复杂的组合（p+q+d > 4时跳过）
                    if (p + q + d > 4) continue;
                    
                    // 对于小数据集，进一步限制复杂度
                    if (n < 100 && p + q + d > 3) continue;
                    
                    try {
                        ARIMAModel.ARIMAParams params = new ARIMAModel.ARIMAParams(p, d, q);
                        MetricsAgg agg = backtestARIMACv(arimaPoints, horizon, folds, params);
                        
                        // 只考虑R² > 0的模型（模型必须比简单平均值好）
                        if (agg != null && agg.r2 > 0 && Double.isFinite(agg.rmse) && agg.rmse > 0) {
                            // 评分：优先RMSE，其次R²（希望RMSE小，R²大）
                            double score = agg.rmse * (1.0 - Math.min(agg.r2, 0.99));
                            
                            // 对于相同评分，优先选择更简单的模型（参数总和小的）
                            if (bestParams == null || score < bestScore || 
                                (Math.abs(score - bestScore) < 0.01 && (p + q + d) < (bestParams.p + bestParams.q + bestParams.d))) {
                                bestScore = score;
                                bestParams = params;
                                bestAgg = agg;
                            }
                        }
                    } catch (Exception e) {
                        // 跳过无法训练的模型
                        continue;
                    }
                }
            }
        }

        // 如果非季节性模型都不好（R² < 0.3），尝试简单的季节性模型（但更谨慎）
        if (bestAgg == null || bestAgg.r2 < 0.3) {
            // 只尝试7天和30天的季节性（更保守）
            int[] seasonalPeriods = {7, 30};
            
            for (int s : seasonalPeriods) {
                // 需要至少3个完整周期才考虑季节性
                if (n < s * 3) continue;
                
                // 只尝试简单的季节性模型：SARIMA(1,d,1)(0,1,0)[s] 避免复杂的(1,1,1)(1,1,1)
                for (int d : dGrid) {
                    try {
                        ARIMAModel.ARIMAParams params1 = new ARIMAModel.ARIMAParams(1, d, 1, 0, 1, 0, s);
                        MetricsAgg agg1 = backtestARIMACv(arimaPoints, horizon, folds, params1);
                        
                        if (agg1 != null && agg1.r2 > 0 && Double.isFinite(agg1.rmse) && agg1.rmse > 0) {
                            double score = agg1.rmse * (1.0 - Math.min(agg1.r2, 0.99));
                            // 季节性模型需要有明显优势（R²至少0.1以上）才采用
                            if (agg1.r2 > 0.1 && (bestAgg == null || score < bestScore || 
                                (Math.abs(score - bestScore) < 0.05 && agg1.r2 > bestAgg.r2))) {
                                bestScore = score;
                                bestParams = params1;
                                bestAgg = agg1;
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }

        // 如果所有模型都失败，使用最简单的ARIMA(1,0,1)作为兜底
        if (bestParams == null || bestAgg == null) {
            bestParams = new ARIMAModel.ARIMAParams(1, 0, 1);
            bestAgg = backtestARIMACv(arimaPoints, horizon, folds, bestParams);
            if (bestAgg == null || bestAgg.rmse <= 0) {
                // 如果回测仍然失败，使用默认值
                bestAgg = new MetricsAgg();
                bestAgg.mae = 0.0;
                bestAgg.rmse = 0.0;
                bestAgg.mape = 0.0;
                bestAgg.r2 = 0.0;
                bestAgg.aic = Double.MAX_VALUE;
            }
        }

        sel.arimaParams = bestParams;
        sel.modelDisplayName = bestParams.toString();
        sel.formulaHint = "ARIMA模型：使用自回归(AR)、差分(I)、移动平均(MA)的组合进行预测";
        sel.mae = bestAgg.mae;
        sel.rmse = bestAgg.rmse;
        sel.mape = bestAgg.mape;
        sel.r2 = bestAgg.r2;
        sel.aic = bestAgg.aic;

        // 基线指标设为0（不使用基线）
        sel.baselineMae = 0.0;
        sel.baselineRmse = 0.0;
        sel.baselineMape = 0.0;
        sel.baselineR2 = 0.0;

        return sel;
    }

    /**
     * ARIMA模型交叉验证回测
     */
    private MetricsAgg backtestARIMACv(List<ARIMAModel.Point> data, int horizon, int folds, ARIMAModel.ARIMAParams params) {
        MetricsAgg out = new MetricsAgg();
        out.aic = Double.MAX_VALUE;
        
        if (data == null || data.size() < horizon + 10) return out;
        
        int n = data.size();
        int used = 0;
        double sumMae = 0, sumRmse = 0, sumMape = 0, sumR2 = 0, sumAic = 0;
        
        for (int f = 0; f < folds; f++) {
            int testStart = n - horizon * (f + 1);
            int testEnd = testStart + horizon;
            if (testStart <= 5 || testEnd > n) break;
            
            List<ARIMAModel.Point> train = data.subList(0, testStart);
            List<ARIMAModel.Point> test = data.subList(testStart, testEnd);
            
            try {
                ARIMAModel model = new ARIMAModel();
                model.train(train, params);
                
                // 预测
                List<Double> pred = new ArrayList<>();
                for (int i = 1; i <= horizon; i++) {
                    double p = model.predict(i);
                    if (Double.isNaN(p) || Double.isInfinite(p) || p < 0) {
                        // 使用最后一个训练值作为兜底
                        p = train.get(train.size() - 1).y;
                    }
                    pred.add(p);
                }
                
                // 提取实际值
                List<Double> actual = new ArrayList<>();
                for (ARIMAModel.Point pt : test) {
                    actual.add(pt.y);
                }
                
                Metrics m = computeMetrics(actual, pred);
                sumMae += m.mae;
                sumRmse += m.rmse;
                sumMape += m.mape;
                sumR2 += m.r2;
                
                // 评估模型以获取AIC
                ARIMAModel.Metrics evalMetrics = model.evaluate();
                sumAic += evalMetrics.aic;
                used++;
            } catch (Exception e) {
                // 忽略错误，继续下一个fold
                continue;
            }
        }
        
        if (used == 0) return out;
        out.mae = sumMae / used;
        out.rmse = sumRmse / used;
        out.mape = sumMape / used;
        out.r2 = sumR2 / used;
        out.aic = sumAic / used;
        return out;
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
        double aic = Double.MAX_VALUE;
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

    /**
     * 分析历史价格特征（增强版）
     */
    private Map<String, Object> analyzeHistoricalPriceFeatures(List<Double> prices, List<Map<String, Object>> historicalData) {
        Map<String, Object> features = new HashMap<>();
        
        if (prices == null || prices.isEmpty()) {
            return features;
        }
        
        // 基础统计
        double minPrice = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxPrice = prices.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double avgPrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double priceRange = maxPrice - minPrice;
        
        // 计算标准差和变异系数
        double variance = 0.0;
        for (double price : prices) {
            variance += Math.pow(price - avgPrice, 2);
        }
        variance /= prices.size();
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = avgPrice > 0 ? stdDev / avgPrice : 0.0;
        
        // 计算中位数和四分位数
        List<Double> sortedPrices = new ArrayList<>(prices);
        Collections.sort(sortedPrices);
        double medianPrice = sortedPrices.get(sortedPrices.size() / 2);
        double q25Price = sortedPrices.get(sortedPrices.size() / 4);
        double q75Price = sortedPrices.get(sortedPrices.size() * 3 / 4);
        
        // 趋势分析（线性回归斜率）
        double trendSlope = 0.0;
        if (prices.size() >= 2) {
            double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
            for (int i = 0; i < prices.size(); i++) {
                double x = i;
                double y = prices.get(i);
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumXX += x * x;
            }
            double n = prices.size();
            double denominator = n * sumXX - sumX * sumX;
            if (Math.abs(denominator) > 1e-12) {
                trendSlope = (n * sumXY - sumX * sumY) / denominator;
            }
        }
        
        // 趋势强度和方向
        double overallTrendValue = prices.get(prices.size() - 1) - prices.get(0);
        String overallTrend;
        if (overallTrendValue > avgPrice * 0.05) {
            overallTrend = "上升";
        } else if (overallTrendValue < -avgPrice * 0.05) {
            overallTrend = "下降";
        } else {
            overallTrend = "平稳";
        }
        
        // 趋势强度（标准化）
        double trendStrength = avgPrice > 0 ? Math.abs(overallTrendValue) / avgPrice : 0.0;
        trendStrength = Math.min(1.0, trendStrength);
        
        // 近期趋势（最近30%的数据）
        String recentTrend = null;
        if (prices.size() >= 3) {
            int recentStart = (int) (prices.size() * 0.7);
            double recentTrendValue = prices.get(prices.size() - 1) - prices.get(recentStart);
            if (recentTrendValue > avgPrice * 0.05) {
                recentTrend = "上升";
            } else if (recentTrendValue < -avgPrice * 0.05) {
                recentTrend = "下降";
            } else {
                recentTrend = "平稳";
            }
        }
        
        // 波动性评级
        String volatilityLevel;
        if (coefficientOfVariation < 0.05) {
            volatilityLevel = "低波动";
        } else if (coefficientOfVariation < 0.15) {
            volatilityLevel = "中等波动";
        } else {
            volatilityLevel = "高波动";
        }
        
        // 峰值和谷值
        int peakIndex = 0, troughIndex = 0;
        double peakPrice = prices.get(0), troughPrice = prices.get(0);
        for (int i = 1; i < prices.size(); i++) {
            if (prices.get(i) > peakPrice) {
                peakPrice = prices.get(i);
                peakIndex = i;
            }
            if (prices.get(i) < troughPrice) {
                troughPrice = prices.get(i);
                troughIndex = i;
            }
        }
        
        String peakDate = historicalData.size() > peakIndex ? (String) historicalData.get(peakIndex).get("date") : null;
        String troughDate = historicalData.size() > troughIndex ? (String) historicalData.get(troughIndex).get("date") : null;
        
        // 季节性检测（简单检测）
        boolean hasSeasonality = false;
        int seasonalPeriod = 0;
        if (prices.size() >= 14) {
            // 检测7天和30天周期
            for (int period : new int[]{7, 30}) {
                if (prices.size() >= period * 2) {
                    double strength = seasonalStrength(prices, period, 0.0);
                    if (strength > 0.1) {
                        hasSeasonality = true;
                        seasonalPeriod = period;
                        break;
                    }
                }
            }
        }
        
        // 填充特征映射
        features.put("min_price", round2(minPrice));
        features.put("max_price", round2(maxPrice));
        features.put("avg_price", round2(avgPrice));
        features.put("median_price", round2(medianPrice));
        features.put("price_range", round2(priceRange));
        features.put("std_dev", round2(stdDev));
        features.put("coefficient_of_variation", round4(coefficientOfVariation));
        features.put("q25_price", round2(q25Price));
        features.put("q75_price", round2(q75Price));
        features.put("trend_slope", round4(trendSlope));
        features.put("overall_trend", overallTrend);
        features.put("trend_strength", round4(trendStrength));
        if (recentTrend != null) {
            features.put("recent_trend", recentTrend);
        }
        features.put("volatility_level", volatilityLevel);
        features.put("peak_price", round2(peakPrice));
        features.put("peak_date", peakDate);
        features.put("trough_price", round2(troughPrice));
        features.put("trough_date", troughDate);
        features.put("has_seasonality", hasSeasonality);
        if (hasSeasonality) {
            features.put("seasonal_period", seasonalPeriod);
        }
        
        return features;
    }

    /**
     * 使用AI进行价格预测
     */
    private PricePredictionResponseDTO predictWithAI(Map<String, List<ExcelParser.DataPoint>> seriesMap, int predictionDays) {
        // AI API配置
        String API_KEY = "sk-QfccpUybEFZ3iGB9rzzukWekBgb0fkaS8Skcy4tyuM8TY5Yf";
        String BASE_URL = "https://chatapi.zjt66.top/v1";
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> seriesData = new ArrayList<>();
        String primaryType = seriesMap.keySet().stream().sorted().findFirst().orElse("默认");
        SeriesPrediction primary = null;

        // 遍历所有规格/类型
        for (Map.Entry<String, List<ExcelParser.DataPoint>> entry : seriesMap.entrySet()) {
            String type = entry.getKey() != null ? entry.getKey() : "默认";
            List<ExcelParser.DataPoint> points = entry.getValue();
            if (points == null || points.isEmpty()) continue;

            // 统一按天聚合
            List<ExcelParser.DataPoint> dataPoints = normalizeDaily(points);
            if (dataPoints.size() < 2) {
                // 数据太少，使用naive预测
                SeriesPrediction sp = naivePredict(dataPoints, predictionDays);
                if (type.equals(primaryType)) {
                    primary = sp;
                }
                Map<String, Object> one = new HashMap<>();
                one.put("type", type);
                one.put("historical_data", sp.historicalData);
                one.put("predicted_data", sp.predictedData);
                one.put("trend", sp.trend);
                one.put("model_metrics", sp.metricsMapAsObject());
                seriesData.add(one);
                continue;
            }

            // 构建历史数据
            List<Map<String, Object>> historicalData = new ArrayList<>();
            for (ExcelParser.DataPoint point : dataPoints) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", sdf.format(point.getDate()));
                item.put("price", point.getPrice());
                historicalData.add(item);
            }

            // 准备AI提示词：格式化时间-价格数据
            StringBuilder dataPrompt = new StringBuilder();
            dataPrompt.append("你是一名商品价格预测专家，擅长分析时间序列数据并预测未来价格。\n\n");
            dataPrompt.append("以下是历史价格数据（日期-价格）：\n");
            for (Map<String, Object> item : historicalData) {
                dataPrompt.append(item.get("date")).append(": ").append(item.get("price")).append("\n");
            }
            
            // 分析历史数据的特征（增强版）
            Map<String, Object> historicalFeatures = null;
            if (historicalData.size() >= 3) {
                List<Double> prices = new ArrayList<>();
                for (Map<String, Object> item : historicalData) {
                    Object priceObj = item.get("price");
                    if (priceObj instanceof Number) {
                        prices.add(((Number) priceObj).doubleValue());
                    }
                }
                
                // 执行详细的特征分析
                historicalFeatures = analyzeHistoricalPriceFeatures(prices, historicalData);
                
                // 构建特征分析文本用于AI prompt
                dataPrompt.append("\n========== 历史价格特征深度分析 ==========\n");
                dataPrompt.append("【基础统计信息】\n");
                dataPrompt.append("- 价格范围：").append(String.format("%.2f", historicalFeatures.get("min_price"))).append(" ~ ").append(String.format("%.2f", historicalFeatures.get("max_price"))).append("\n");
                dataPrompt.append("- 平均价格：").append(String.format("%.2f", historicalFeatures.get("avg_price"))).append("\n");
                dataPrompt.append("- 中位数价格：").append(String.format("%.2f", historicalFeatures.get("median_price"))).append("\n");
                dataPrompt.append("- 价格波动幅度：").append(String.format("%.2f", historicalFeatures.get("price_range"))).append("\n");
                dataPrompt.append("- 变异系数（CV）：").append(String.format("%.4f", historicalFeatures.get("coefficient_of_variation"))).append("（值越大表示波动越大）\n");
                
                dataPrompt.append("\n【趋势分析】\n");
                dataPrompt.append("- 整体趋势：").append(historicalFeatures.get("overall_trend")).append("\n");
                dataPrompt.append("- 趋势强度：").append(String.format("%.2f", historicalFeatures.get("trend_strength"))).append("（0-1之间，值越大趋势越明显）\n");
                if (historicalFeatures.get("recent_trend") != null) {
                    dataPrompt.append("- 近期趋势（最近30%数据）：").append(historicalFeatures.get("recent_trend")).append("\n");
                }
                
                dataPrompt.append("\n【波动性分析】\n");
                dataPrompt.append("- 标准差：").append(String.format("%.2f", historicalFeatures.get("std_dev"))).append("\n");
                dataPrompt.append("- 波动性评级：").append(historicalFeatures.get("volatility_level")).append("\n");
                if (historicalFeatures.get("peak_price") != null && historicalFeatures.get("peak_date") != null) {
                    dataPrompt.append("- 历史最高价：").append(String.format("%.2f", historicalFeatures.get("peak_price"))).append("（日期：").append(historicalFeatures.get("peak_date")).append("）\n");
                }
                if (historicalFeatures.get("trough_price") != null && historicalFeatures.get("trough_date") != null) {
                    dataPrompt.append("- 历史最低价：").append(String.format("%.2f", historicalFeatures.get("trough_price"))).append("（日期：").append(historicalFeatures.get("trough_date")).append("）\n");
                }
                
                // 季节性检测
                if (historicalFeatures.get("has_seasonality") != null && (Boolean) historicalFeatures.get("has_seasonality")) {
                    dataPrompt.append("\n【季节性特征】\n");
                    dataPrompt.append("- 检测到季节性模式，周期长度：").append(historicalFeatures.get("seasonal_period")).append("天\n");
                }
                
                dataPrompt.append("\n【价格分布特征】\n");
                dataPrompt.append("- 价格主要集中在：").append(String.format("%.2f", historicalFeatures.get("q25_price"))).append(" ~ ").append(String.format("%.2f", historicalFeatures.get("q75_price"))).append("之间（四分位距）\n");
                
                dataPrompt.append("\n========================================\n");
                dataPrompt.append("\n【预测指导原则】\n");
                dataPrompt.append("基于以上历史特征分析，请在进行价格预测时：\n");
                dataPrompt.append("1. 充分考虑历史价格的波动模式和趋势特征\n");
                dataPrompt.append("2. 如果历史数据显示明显的波动性，预测结果应该保持类似的波动幅度\n");
                dataPrompt.append("3. 趋势方向应该与历史数据保持一致，但要考虑市场的不确定性\n");
                dataPrompt.append("4. 预测价格应该在历史价格范围内合理波动，避免极端值\n");
                dataPrompt.append("5. 不要生成单调递增、递减或完全持平的价格序列\n");
            }
            
            dataPrompt.append("\n请预测未来").append(predictionDays).append("天的商品价格。");
            dataPrompt.append("请以JSON格式返回预测结果，格式如下：\n");
            dataPrompt.append("{\n");
            dataPrompt.append("  \"predicted_data\": [\n");
            dataPrompt.append("    {\"date\": \"YYYY-MM-DD\", \"price\": 数值},\n");
            dataPrompt.append("    ...\n");
            dataPrompt.append("  ],\n");
            dataPrompt.append("  \"trend\": \"上升/下降/平稳/波动\",\n");
            dataPrompt.append("  \"prediction_reason\": \"详细的预测理由，说明你做出这些预测的原因和依据，包括对历史特征的分析、趋势判断、波动性考虑等，至少200字\",\n");
            dataPrompt.append("  \"model_metrics\": {\n");
            dataPrompt.append("    \"r_squared\": 数值,\n");
            dataPrompt.append("    \"mae\": 数值,\n");
            dataPrompt.append("    \"rmse\": 数值\n");
            dataPrompt.append("  }\n");
            dataPrompt.append("}\n");
            dataPrompt.append("\n关键要求：\n");
            dataPrompt.append("1. 日期从历史数据的最后一天开始连续递增。\n");
            dataPrompt.append("2. 价格数值必须为合理的正数。\n");
            dataPrompt.append("3. 如果历史数据是波动的，预测结果也必须是波动的，不要生成单调递增、递减或完全持平的价格序列。\n");
            dataPrompt.append("4. 预测价格应该反映历史数据的波动特征，包括合理的价格起伏和不确定性。\n");
            dataPrompt.append("5. 预测价格应该在历史价格范围内或合理延伸范围内，保持真实性和可信度。\n");
            dataPrompt.append("6. prediction_reason字段必须提供详细的预测理由，说明你是如何基于历史数据特征做出预测的，这是增强预测信服力的关键。\n");

            String promptText = dataPrompt.toString();
            System.out.println("========== AI预测输入日志 ==========");
            System.out.println("规格/类型: " + type);
            System.out.println("历史数据点数: " + historicalData.size());
            System.out.println("预测天数: " + predictionDays);
            System.out.println("历史数据:");
            for (Map<String, Object> item : historicalData) {
                System.out.println("  " + item.get("date") + ": " + item.get("price"));
            }
            System.out.println("发送给AI的提示词:");
            System.out.println(promptText);
            System.out.println("====================================");

            // 调用AI接口
            try {
                String url = BASE_URL + "/chat/completions";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);

                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

                // 构造请求体
                StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append("{");
                bodyBuilder.append("\"model\":\"gpt-4o-mini\",");
                bodyBuilder.append("\"temperature\":0.3,");
                bodyBuilder.append("\"messages\":[");
                bodyBuilder.append("{\"role\":\"system\",\"content\":\"你是一名商品价格预测专家，擅长分析时间序列数据并预测未来价格。你需要根据历史数据的真实波动模式进行预测，而不是简单的趋势外推。如果历史数据是波动的，预测结果也必须是波动的，体现市场的真实不确定性和价格起伏。预测结果应该真实可信，反映商品价格的实际变化规律，不要生成单调递增、递减或完全持平的价格序列。你必须为每次预测提供详细的预测理由（prediction_reason字段），说明你是如何基于历史数据特征、趋势分析、波动性等因素做出预测判断的，以增强预测结果的可信度和说服力。\"},");
                bodyBuilder.append("{\"role\":\"user\",\"content\":");
                bodyBuilder.append("\"").append(escapeJson(dataPrompt.toString())).append("\"");
                bodyBuilder.append("}");
                bodyBuilder.append("]");
                bodyBuilder.append("}");

                String requestBody = bodyBuilder.toString();
                System.out.println("========== AI API请求日志 ==========");
                System.out.println("请求URL: " + url);
                System.out.println("请求体长度: " + requestBody.length() + " 字符");
                System.out.println("请求体内容:");
                System.out.println(requestBody);
                System.out.println("====================================");
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int statusCode = conn.getResponseCode();
                System.out.println("========== AI API响应日志 ==========");
                System.out.println("HTTP状态码: " + statusCode);
                BufferedReader reader;
                if (statusCode >= 200 && statusCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                }

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String apiResp = sb.toString();
                System.out.println("API原始响应长度: " + apiResp.length() + " 字符");
                System.out.println("API原始响应内容:");
                System.out.println(apiResp);
                System.out.println("====================================");
                
                if (statusCode >= 200 && statusCode < 300) {
                    String aiResponse = extractFirstMessageContent(apiResp);
                    System.out.println("========== AI响应解析日志 ==========");
                    System.out.println("提取的AI响应内容:");
                    System.out.println(aiResponse);
                    System.out.println("====================================");
                    
                    // 解析AI返回的JSON数据
                    SeriesPrediction sp = parseAIResponse(aiResponse, historicalData, dataPoints, predictionDays, historicalFeatures);
                    
                    System.out.println("========== AI解析结果日志 ==========");
                    System.out.println("解析后的预测数据点数: " + (sp.predictedData != null ? sp.predictedData.size() : 0));
                    if (sp.predictedData != null && !sp.predictedData.isEmpty()) {
                        System.out.println("前5条预测数据:");
                        for (int i = 0; i < Math.min(5, sp.predictedData.size()); i++) {
                            Map<String, Object> pred = sp.predictedData.get(i);
                            System.out.println("  " + pred.get("date") + ": " + pred.get("price"));
                        }
                    }
                    System.out.println("趋势: " + sp.trend);
                    System.out.println("指标: " + sp.metrics);
                    System.out.println("====================================");
                    if (type.equals(primaryType)) {
                        primary = sp;
                    }

                    Map<String, Object> one = new HashMap<>();
                    one.put("type", type);
                    one.put("historical_data", sp.historicalData);
                    one.put("predicted_data", sp.predictedData);
                    one.put("trend", sp.trend);
                    one.put("model_metrics", sp.metricsMapAsObject());
                    seriesData.add(one);
                } else {
                    // AI调用失败，使用naive预测作为兜底
                    System.err.println("AI预测失败，使用naive预测: " + apiResp);
                    SeriesPrediction sp = naivePredict(dataPoints, predictionDays);
                    if (type.equals(primaryType)) {
                        primary = sp;
                    }
                    Map<String, Object> one = new HashMap<>();
                    one.put("type", type);
                    one.put("historical_data", sp.historicalData);
                    one.put("predicted_data", sp.predictedData);
                    one.put("trend", sp.trend);
                    one.put("model_metrics", sp.metricsMapAsObject());
                    seriesData.add(one);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // AI调用异常，使用naive预测作为兜底
                SeriesPrediction sp = naivePredict(dataPoints, predictionDays);
                if (type.equals(primaryType)) {
                    primary = sp;
                }
                Map<String, Object> one = new HashMap<>();
                one.put("type", type);
                one.put("historical_data", sp.historicalData);
                one.put("predicted_data", sp.predictedData);
                one.put("trend", sp.trend);
                one.put("model_metrics", sp.metricsMapAsObject());
                seriesData.add(one);
            }
        }

        seriesData.sort(Comparator.comparing(m -> (String) m.get("type")));

        if (primary == null) {
            throw new IllegalArgumentException("文件中没有可用的数据序列");
        }

        PricePredictionResponseDTO response = new PricePredictionResponseDTO();
        response.setSeriesData(seriesData);
        response.setHistoricalData(primary.historicalData);
        response.setPredictedData(primary.predictedData);
        response.setModelMetrics(primary.metrics);
        response.setTrend(primary.trend);
        response.setCalculationDetails(primary.calculationDetails);

        return response;
    }

    /**
     * 解析AI返回的JSON响应
     */
    private SeriesPrediction parseAIResponse(String aiResponse, List<Map<String, Object>> historicalData, 
                                             List<ExcelParser.DataPoint> dataPoints, int predictionDays,
                                             Map<String, Object> historicalFeatures) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SeriesPrediction sp = new SeriesPrediction();
        sp.historicalData = historicalData;
        sp.historicalFeatures = historicalFeatures;

        System.out.println("========== 开始解析AI响应 ==========");
        System.out.println("AI响应内容长度: " + aiResponse.length());
        System.out.println("AI响应前500字符: " + (aiResponse.length() > 500 ? aiResponse.substring(0, 500) : aiResponse));

        // 尝试从AI响应中提取JSON数据
        // 查找predicted_data数组
        List<Map<String, Object>> predictedData = new ArrayList<>();
        String trend = "平稳";
        String predictionReason = "基于历史价格数据的分析进行预测。";
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("r_squared", 0.0);
        metrics.put("mae", 0.0);
        metrics.put("rmse", 0.0);
        metrics.put("mape", 0.0);
        metrics.put("aic", 0.0);

        try {
            // 查找predicted_data数组
            int dataIndex = aiResponse.indexOf("\"predicted_data\"");
            System.out.println("查找predicted_data位置: " + dataIndex);
            if (dataIndex != -1) {
                int arrayStart = aiResponse.indexOf("[", dataIndex);
                System.out.println("找到数组开始位置: " + arrayStart);
                if (arrayStart != -1) {
                    int arrayEnd = findMatchingBracket(aiResponse, arrayStart);
                    System.out.println("找到数组结束位置: " + arrayEnd);
                    if (arrayEnd != -1) {
                        String arrayContent = aiResponse.substring(arrayStart + 1, arrayEnd);
                        System.out.println("数组内容长度: " + arrayContent.length());
                        System.out.println("数组内容前200字符: " + (arrayContent.length() > 200 ? arrayContent.substring(0, 200) : arrayContent));
                        // 解析数组中的对象
                        predictedData = parsePredictedDataArray(arrayContent, dataPoints, predictionDays, sdf);
                        System.out.println("解析后预测数据数量: " + predictedData.size());
                    } else {
                        System.out.println("警告: 未找到数组结束位置");
                    }
                } else {
                    System.out.println("警告: 未找到数组开始位置");
                }
            } else {
                System.out.println("警告: 未找到predicted_data字段");
            }

            // 查找trend
            int trendIndex = aiResponse.indexOf("\"trend\"");
            if (trendIndex != -1) {
                int colonIndex = aiResponse.indexOf(":", trendIndex);
                if (colonIndex != -1) {
                    int quoteStart = aiResponse.indexOf("\"", colonIndex);
                    if (quoteStart != -1) {
                        int quoteEnd = aiResponse.indexOf("\"", quoteStart + 1);
                        if (quoteEnd != -1) {
                            trend = aiResponse.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
            }

            // 查找prediction_reason
            int reasonIndex = aiResponse.indexOf("\"prediction_reason\"");
            if (reasonIndex == -1) {
                reasonIndex = aiResponse.indexOf("\"predictionReason\"");
            }
            if (reasonIndex != -1) {
                int colonIndex = aiResponse.indexOf(":", reasonIndex);
                if (colonIndex != -1) {
                    // 跳过空白字符
                    int valueStart = colonIndex + 1;
                    while (valueStart < aiResponse.length() && Character.isWhitespace(aiResponse.charAt(valueStart))) {
                        valueStart++;
                    }
                    if (valueStart < aiResponse.length() && aiResponse.charAt(valueStart) == '"') {
                        // 提取字符串值（支持转义字符）
                        int quoteStart = valueStart + 1;
                        int quoteEnd = quoteStart;
                        boolean escape = false;
                        while (quoteEnd < aiResponse.length()) {
                            char c = aiResponse.charAt(quoteEnd);
                            if (escape) {
                                escape = false;
                            } else if (c == '\\') {
                                escape = true;
                            } else if (c == '"') {
                                break;
                            }
                            quoteEnd++;
                        }
                        if (quoteEnd < aiResponse.length()) {
                            predictionReason = aiResponse.substring(quoteStart, quoteEnd);
                            // 处理转义字符
                            predictionReason = predictionReason.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                        }
                    }
                }
            }

            // 查找model_metrics
            int metricsIndex = aiResponse.indexOf("\"model_metrics\"");
            if (metricsIndex != -1) {
                int objStart = aiResponse.indexOf("{", metricsIndex);
                if (objStart != -1) {
                    int objEnd = findMatchingBrace(aiResponse, objStart);
                    if (objEnd != -1) {
                        String metricsContent = aiResponse.substring(objStart, objEnd + 1);
                        metrics = parseMetricsObject(metricsContent);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析AI响应时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("解析完成 - 预测数据数量: " + predictedData.size() + ", 需要: " + predictionDays);

        // 如果AI没有返回足够的预测数据，使用naive预测补充
        if (predictedData.size() < predictionDays) {
            System.out.println("警告: AI返回的预测数据不足，使用naive预测补充");
            Date lastDate = dataPoints.get(dataPoints.size() - 1).getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(lastDate);
            double lastPrice = dataPoints.get(dataPoints.size() - 1).getPrice();
            
            for (int i = predictedData.size(); i < predictionDays; i++) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                Map<String, Object> item = new HashMap<>();
                item.put("date", sdf.format(cal.getTime()));
                item.put("price", Math.round(lastPrice * 100.0) / 100.0);
                predictedData.add(item);
            }
        }

        sp.predictedData = predictedData;
        sp.trend = trend;
        sp.metrics = metrics;
        sp.predictionReason = predictionReason;

        // 构建计算详情（包含AI输入输出信息，便于前端显示和排查问题）
        Map<String, Object> calculationDetails = new HashMap<>();
        calculationDetails.put("model_name", "AI预测模型");
        calculationDetails.put("prediction_method", "AI商品价格预测专家");
        
        // 添加历史价格特征分析
        if (historicalFeatures != null && !historicalFeatures.isEmpty()) {
            calculationDetails.put("historical_features", historicalFeatures);
        }
        
        // 添加预测理由
        calculationDetails.put("prediction_reason", predictionReason);
        
        // 添加AI输入输出信息
        Map<String, Object> aiInfo = new HashMap<>();
        aiInfo.put("historical_data_count", historicalData.size());
        aiInfo.put("prediction_days", predictionDays);
        if (historicalData.size() > 0) {
            aiInfo.put("first_date", historicalData.get(0).get("date"));
            aiInfo.put("last_date", historicalData.get(historicalData.size() - 1).get("date"));
            aiInfo.put("first_price", historicalData.get(0).get("price"));
            aiInfo.put("last_price", historicalData.get(historicalData.size() - 1).get("price"));
        }
        aiInfo.put("parsed_predicted_count", predictedData.size());
        calculationDetails.put("ai_info", aiInfo);
        
        sp.calculationDetails = calculationDetails;

        return sp;
    }

    /**
     * 解析predicted_data数组
     */
    private List<Map<String, Object>> parsePredictedDataArray(String arrayContent, 
                                                              List<ExcelParser.DataPoint> dataPoints, 
                                                              int predictionDays, 
                                                              SimpleDateFormat sdf) {
        System.out.println("========== 解析predicted_data数组 ==========");
        System.out.println("数组内容长度: " + arrayContent.length());
        
        List<Map<String, Object>> result = new ArrayList<>();
        Date lastDate = dataPoints.get(dataPoints.size() - 1).getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);

        // 查找所有对象
        int objStart = 0;
        int count = 0;
        while (count < predictionDays && objStart < arrayContent.length()) {
            int braceStart = arrayContent.indexOf("{", objStart);
            if (braceStart == -1) {
                System.out.println("未找到更多对象，已解析 " + count + " 个");
                break;
            }
            
            int braceEnd = findMatchingBrace(arrayContent, braceStart);
            if (braceEnd == -1) {
                System.out.println("未找到对象结束位置");
                break;
            }

            String objContent = arrayContent.substring(braceStart + 1, braceEnd);
            System.out.println("对象 " + (count + 1) + " 内容: " + objContent);
            
            // 提取date和price
            String date = extractStringValue(objContent, "date");
            String priceStr = extractStringValue(objContent, "price");
            
            System.out.println("  提取的date: " + date);
            System.out.println("  提取的price: " + priceStr);
            
            if (date == null || priceStr == null) {
                System.out.println("  警告: date或price为空，使用默认值");
                // 如果解析失败，使用递增日期和最后一个价格
                cal.add(Calendar.DAY_OF_MONTH, 1);
                date = sdf.format(cal.getTime());
                priceStr = String.valueOf(dataPoints.get(dataPoints.size() - 1).getPrice());
            }

            try {
                double price = Double.parseDouble(priceStr);
                if (price < 0) {
                    System.out.println("  警告: 价格为负数，使用最后一个价格");
                    price = dataPoints.get(dataPoints.size() - 1).getPrice();
                }
                
                Map<String, Object> item = new HashMap<>();
                item.put("date", date);
                item.put("price", Math.round(price * 100.0) / 100.0);
                result.add(item);
                System.out.println("  成功添加: " + date + " -> " + item.get("price"));
                
                // 更新日期
                try {
                    Date parsedDate = sdf.parse(date);
                    cal.setTime(parsedDate);
                } catch (Exception e) {
                    System.out.println("  日期解析失败，使用递增日期");
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
            } catch (NumberFormatException e) {
                System.out.println("  价格解析失败: " + e.getMessage() + "，使用最后一个价格");
                // 价格解析失败，使用最后一个价格
                cal.add(Calendar.DAY_OF_MONTH, 1);
                Map<String, Object> item = new HashMap<>();
                item.put("date", sdf.format(cal.getTime()));
                item.put("price", Math.round(dataPoints.get(dataPoints.size() - 1).getPrice() * 100.0) / 100.0);
                result.add(item);
            }

            objStart = braceEnd + 1;
            count++;
        }

        System.out.println("解析完成，共解析 " + result.size() + " 条数据");
        System.out.println("====================================");
        return result;
    }

    /**
     * 从对象字符串中提取字符串值
     */
    private String extractStringValue(String objContent, String key) {
        int keyIndex = objContent.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return null;
        
        int colonIndex = objContent.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        // 跳过空白字符
        int valueStart = colonIndex + 1;
        while (valueStart < objContent.length() && Character.isWhitespace(objContent.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= objContent.length()) return null;
        
        // 检查是否是字符串值（以引号开始）
        if (objContent.charAt(valueStart) == '"') {
            int quoteStart = valueStart + 1;
            int quoteEnd = quoteStart;
            boolean escape = false;
            while (quoteEnd < objContent.length()) {
                char c = objContent.charAt(quoteEnd);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    break;
                }
                quoteEnd++;
            }
            if (quoteEnd < objContent.length()) {
                return objContent.substring(quoteStart, quoteEnd);
            }
        } else {
            // 可能是数字值，提取到逗号或结束
            int valueEnd = valueStart;
            while (valueEnd < objContent.length() && 
                   objContent.charAt(valueEnd) != ',' && 
                   objContent.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return objContent.substring(valueStart, valueEnd).trim();
        }
        
        return null;
    }

    /**
     * 解析metrics对象
     */
    private Map<String, Double> parseMetricsObject(String metricsContent) {
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("r_squared", 0.0);
        metrics.put("mae", 0.0);
        metrics.put("rmse", 0.0);
        metrics.put("mape", 0.0);
        metrics.put("aic", 0.0);

        String[] keys = {"r_squared", "mae", "rmse", "mape", "aic"};
        for (String key : keys) {
            String valueStr = extractStringValue(metricsContent, key);
            if (valueStr != null) {
                try {
                    double value = Double.parseDouble(valueStr);
                    metrics.put(key, value);
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }

        return metrics;
    }

    /**
     * 查找匹配的右括号
     */
    private int findMatchingBracket(String str, int start) {
        int depth = 0;
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 查找匹配的右花括号
     */
    private int findMatchingBrace(String str, int start) {
        int depth = 0;
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 提取AI响应中的第一条消息内容
     */
    private String extractFirstMessageContent(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        try {
            int contentIndex = json.indexOf("\"content\"");
            if (contentIndex == -1) {
                return json;
            }

            int firstQuote = json.indexOf('"', contentIndex + 9);
            if (firstQuote == -1) {
                return json;
            }
            StringBuilder sb = new StringBuilder();
            boolean escape = false;
            for (int i = firstQuote + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escape) {
                    switch (c) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                    escape = false;
                } else {
                    if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
            }
            String result = sb.toString().trim();
            if (result.isEmpty()) {
                return json;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return json;
        }
    }

    /**
     * JSON转义
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

