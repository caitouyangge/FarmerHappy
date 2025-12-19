// src/main/java/util/PriceFileParser.java
package util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格文件解析器：统一支持 Excel(.xls/.xlsx) 与 CSV(.csv)
 * - Excel: 默认认为是两列（日期, 价格），返回单序列，规格名为“默认”
 * - CSV: 支持（规格/平均价/发布日期）或（日期/价格），返回按规格分组的多序列
 */
public class PriceFileParser {

    public Map<String, List<ExcelParser.DataPoint>> parse(InputStream inputStream, String fileName) throws Exception {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName不能为空");
        }

        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
            ExcelParser parser = new ExcelParser();
            List<ExcelParser.DataPoint> points = parser.parse(inputStream, fileName);
            Map<String, List<ExcelParser.DataPoint>> map = new HashMap<>();
            map.put("默认", points);
            return map;
        }

        if (lower.endsWith(".csv")) {
            CsvPriceParser parser = new CsvPriceParser();
            return parser.parse(inputStream);
        }

        throw new IllegalArgumentException("不支持的文件格式，仅支持 .xls / .xlsx / .csv");
    }
}


