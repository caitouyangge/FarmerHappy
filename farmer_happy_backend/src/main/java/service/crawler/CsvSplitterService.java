// service/crawler/CsvSplitterService.java
package service.crawler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CSV文件分割服务类
 */
public class CsvSplitterService {

    /**
     * 分割CSV文件，根据品名将数据分别存储
     *
     * @param fileName 文件名
     * @throws IOException IO异常
     */
    public void splitCsvFile(String fileName) throws IOException {
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");

        // 构建输入文件路径
        Path inputFilePath = Paths.get(projectRoot, "result", fileName);

        // 检查文件是否存在
        if (!Files.exists(inputFilePath)) {
            throw new FileNotFoundException("文件不存在: " + inputFilePath.toString());
        }

        // 创建输出目录
        Path outputDir = Paths.get(projectRoot, "result", "split");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // 读取并处理CSV文件
        processCsvFile(inputFilePath, outputDir);
    }

    /**
     * 处理CSV文件，按品名分割数据
     *
     * @param inputFilePath 输入文件路径
     * @param outputDir 输出目录
     * @throws IOException IO异常
     */
    private void processCsvFile(Path inputFilePath, Path outputDir) throws IOException {
        // 存储每个品种的数据
        Map<String, List<Map<String, String>>> productDataMap = new HashMap<>();

        // 读取文件所有行
        List<String> lines = Files.readAllLines(inputFilePath, StandardCharsets.UTF_8);

        // 检查是否有数据
        if (lines.isEmpty()) {
            return;
        }

        // 获取表头
        String header = lines.get(0);

        // 找到各列的索引
        String[] headers = header.split(",");
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            columnIndexMap.put(headers[i].trim(), i);
        }

        // 检查必需的列是否存在
        if (!columnIndexMap.containsKey("品名") ||
                !columnIndexMap.containsKey("规格") ||
                !columnIndexMap.containsKey("平均价") ||
                !columnIndexMap.containsKey("发布日期")) {
            throw new IllegalArgumentException("CSV文件缺少必需的列：品名、规格、平均价、发布日期");
        }

        int productNameIndex = columnIndexMap.get("品名");
        int specIndex = columnIndexMap.get("规格");
        int avgPriceIndex = columnIndexMap.get("平均价");
        int dateIndex = columnIndexMap.get("发布日期");

        // 处理每一行数据
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] values = parseCsvLine(line);

            if (values.length > Math.max(Math.max(productNameIndex, specIndex),
                    Math.max(avgPriceIndex, dateIndex))) {
                String productName = values[productNameIndex].trim();

                // 处理发布日期，只保留日期部分
                String fullDate = values[dateIndex].trim();
                String dateOnly = extractDateOnly(fullDate);

                // 创建行数据映射
                Map<String, String> rowData = new HashMap<>();
                rowData.put("规格", values[specIndex].trim());
                rowData.put("平均价", values[avgPriceIndex].trim());
                rowData.put("发布日期", dateOnly);

                // 将数据添加到对应品名的列表中
                productDataMap.computeIfAbsent(productName, k -> new ArrayList<>()).add(rowData);
            }
        }

        // 获取当前时间戳用于文件命名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // 为每个品名创建单独的CSV文件
        for (Map.Entry<String, List<Map<String, String>>> entry : productDataMap.entrySet()) {
            String productName = entry.getKey();
            List<Map<String, String>> dataRows = entry.getValue();

            // 创建输出文件路径，文件名格式为"品名_时间"
            String safeFileName = productName.replaceAll("[\\\\/:*?\"<>|]", "_"); // 替换非法字符
            Path outputFile = outputDir.resolve(safeFileName + "_" + timestamp + ".csv");

            // 写入数据
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                // 写入表头（只包含规格、平均价、发布日期）
                writer.write("规格,平均价,发布日期");
                writer.newLine();

                // 写入数据行
                for (Map<String, String> row : dataRows) {
                    writer.write(String.join(",",
                            row.get("规格"),
                            row.get("平均价"),
                            row.get("发布日期")));
                    writer.newLine();
                }
            }
        }
    }

    /**
     * 从完整的日期时间字符串中提取日期部分
     *
     * @param dateTime 完整的日期时间字符串
     * @return 只包含日期的部分
     */
    private String extractDateOnly(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return dateTime;
        }

        // 如果包含空格，只取空格前的部分（日期部分）
        int spaceIndex = dateTime.indexOf(" ");
        if (spaceIndex > 0) {
            return dateTime.substring(0, spaceIndex);
        }

        // 如果没有空格，返回原字符串
        return dateTime;
    }

    /**
     * 解析CSV行，正确处理包含逗号的字段（被引号包围的情况）
     *
     * @param line CSV行
     * @return 字段数组
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && (i == 0 || line.charAt(i-1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }
}
