// src/main/java/util/CsvPriceParser.java
package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * CSV价格数据解析器
 *
 * 支持两类CSV表头（顺序不限）：
 * - 规格, 平均价, 发布日期
 * - 日期, 价格
 *
 * 也兼容爬虫导出的完整CSV（只要包含“平均价/发布日期/规格”列即可，其他列会被忽略）。
 */
public class CsvPriceParser {

    public Map<String, List<ExcelParser.DataPoint>> parse(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV文件为空");
            }

            // 去掉UTF-8 BOM
            headerLine = stripBom(headerLine).trim();

            List<String> headers = splitCsvLine(headerLine);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                idx.put(normalizeHeader(headers.get(i)), i);
            }

            Integer specIdx = firstMatchIndex(idx, "规格");
            Integer avgPriceIdx = firstMatchIndex(idx, "平均价");
            Integer pubDateIdx = firstMatchIndex(idx, "发布日期");

            Integer dateIdx = firstMatchIndex(idx, "日期");
            Integer priceIdx = firstMatchIndex(idx, "价格");

            boolean modeSpecAvgDate = (avgPriceIdx != null && pubDateIdx != null);
            boolean modeDatePrice = (dateIdx != null && priceIdx != null);

            if (!modeSpecAvgDate && !modeDatePrice) {
                throw new IllegalArgumentException("CSV表头不符合要求：需要包含（规格/平均价/发布日期）或（日期/价格）列");
            }

            Map<String, List<ExcelParser.DataPoint>> result = new HashMap<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                List<String> cells = splitCsvLine(line);

                String spec = "默认";
                String dateStr;
                String priceStr;

                if (modeSpecAvgDate) {
                    if (pubDateIdx >= cells.size() || avgPriceIdx >= cells.size()) continue;
                    dateStr = cells.get(pubDateIdx);
                    priceStr = cells.get(avgPriceIdx);
                    if (specIdx != null && specIdx < cells.size()) {
                        String s = cells.get(specIdx);
                        if (s != null && !s.trim().isEmpty()) spec = s.trim();
                    }
                } else {
                    if (dateIdx >= cells.size() || priceIdx >= cells.size()) continue;
                    dateStr = cells.get(dateIdx);
                    priceStr = cells.get(priceIdx);
                }

                Date date = parseDateString(dateStr);
                if (date == null) continue;

                Double price = parseDouble(priceStr);
                if (price == null || price < 0) continue;

                result.computeIfAbsent(spec, k -> new ArrayList<>())
                    .add(new ExcelParser.DataPoint(date, price));
            }

            if (result.isEmpty()) {
                throw new IllegalArgumentException("CSV未解析到有效数据（请检查日期/价格列是否为空或格式是否正确）");
            }

            // 每个规格按日期排序
            for (List<ExcelParser.DataPoint> series : result.values()) {
                series.sort(Comparator.comparing(ExcelParser.DataPoint::getDate));
            }

            return result;
        }
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static String normalizeHeader(String h) {
        if (h == null) return "";
        return stripBom(h).trim();
    }

    private static Integer firstMatchIndex(Map<String, Integer> idx, String key) {
        if (idx.containsKey(key)) return idx.get(key);
        // 兼容可能的空白/不同写法
        for (Map.Entry<String, Integer> e : idx.entrySet()) {
            if (e.getKey() != null && e.getKey().replace(" ", "").equals(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static Double parseDouble(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        v = v.replaceAll("[¥$€£,，\\s]", "");
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static Date parseDateString(String dateStr) {
        if (dateStr == null) return null;
        String v = dateStr.trim();
        if (v.isEmpty()) return null;
        // 支持的日期格式
        String[] patterns = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "MM/dd/yyyy",
            "dd/MM/yyyy"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(v);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 简易CSV行切分：支持双引号包裹，双引号内可包含逗号，双引号用""转义
     */
    private static List<String> splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // escaped quote
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }
}


