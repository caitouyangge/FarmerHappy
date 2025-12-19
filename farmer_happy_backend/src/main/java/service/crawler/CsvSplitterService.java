// service/crawler/CsvSplitterService.java
package service.crawler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * CSV文件分割服务类
 */
public class CsvSplitterService {
    private static final String UTF8_BOM = "\uFEFF";

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
     * 扫描 result/split 目录，返回可用的拆分文件列表（用于前端动态勾选品种）
     */
    public List<Map<String, Object>> listSplitFiles() throws IOException {
        String projectRoot = System.getProperty("user.dir");
        Path splitDir = Paths.get(projectRoot, "result", "split");

        List<Map<String, Object>> files = new ArrayList<>();
        if (!Files.exists(splitDir) || !Files.isDirectory(splitDir)) {
            return files;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitDir, "*.csv")) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                String csvFileName = p.getFileName().toString();

                Map<String, Object> info = parseSplitFileName(csvFileName);

                // 默认仍以“一个品种+时间戳”为一个条目，但优先返回 xlsx（这样前端不改代码也会下载到 xlsx）
                info.put("csv_file_name", csvFileName);
                info.put("csv_file_size", Files.size(p));
                info.put("download_csv_url", "http://localhost:8080/api/v1/agriculture/price/download?scope=split&file_name=" + urlEncode(csvFileName));

                // 如果同名 xlsx 存在，则附带 xlsx 下载信息（避免列表重复）
                String base = stripExtension(csvFileName);
                if (base != null && !base.isEmpty()) {
                    Path xlsxPath = splitDir.resolve(base + ".xlsx");
                    if (Files.exists(xlsxPath) && Files.isRegularFile(xlsxPath)) {
                        String xlsxName = xlsxPath.getFileName().toString();
                        info.put("xlsx_file_name", xlsxName);
                        info.put("xlsx_file_size", Files.size(xlsxPath));
                        info.put("download_xlsx_url", "http://localhost:8080/api/v1/agriculture/price/download?scope=split&file_name=" + urlEncode(xlsxName));

                        // 关键：让老前端直接用 file_name/download_url 就能拿到 xlsx
                        info.put("file_name", xlsxName);
                        info.put("file_size", Files.size(xlsxPath));
                        info.put("last_modified", Files.getLastModifiedTime(xlsxPath).toMillis());
                        info.put("download_url", info.get("download_xlsx_url"));
                    } else {
                        // 没有 xlsx，就退化为 csv
                        info.put("file_name", csvFileName);
                        info.put("file_size", Files.size(p));
                        info.put("last_modified", Files.getLastModifiedTime(p).toMillis());
                        info.put("download_url", info.get("download_csv_url"));
                    }
                }
                files.add(info);
            }
        }

        // 按最后修改时间倒序，便于取“最新”
        files.sort((a, b) -> {
            long am = a.get("last_modified") instanceof Number ? ((Number) a.get("last_modified")).longValue() : 0L;
            long bm = b.get("last_modified") instanceof Number ? ((Number) b.get("last_modified")).longValue() : 0L;
            return Long.compare(bm, am);
        });
        return files;
    }

    /**
     * 用户勾选品种/文件后，放置到指定位置（位置被映射为 result/placed/{location}/）
     *
     * 支持两种输入：
     * - fileNames：精确指定要放置的 split 文件名（推荐，前端从 listSplitFiles() 获取后勾选）
     * - varieties：只给品种名，则会自动选择该品种“最新”的 split 文件进行放置
     */
    public Map<String, Object> placeSplitFiles(List<String> fileNames, List<String> varieties, String targetDir) throws IOException {
        String projectRoot = System.getProperty("user.dir");
        Path root = Paths.get(projectRoot).toAbsolutePath().normalize();
        Path splitDir = Paths.get(projectRoot, "result", "split");
        if (!Files.exists(splitDir) || !Files.isDirectory(splitDir)) {
            throw new FileNotFoundException("split 目录不存在: " + splitDir.toString());
        }

        // 将 varieties 转为具体 fileNames（取最新）
        List<String> resolvedFileNames = new ArrayList<>();
        if (fileNames != null) {
            for (String fn : fileNames) {
                if (fn == null || fn.trim().isEmpty()) continue;
                resolvedFileNames.add(fn.trim());
            }
        }
        if ((resolvedFileNames == null || resolvedFileNames.isEmpty()) && varieties != null && !varieties.isEmpty()) {
            Map<String, String> newestByVariety = pickNewestFileByVariety(splitDir);
            for (String v : varieties) {
                if (v == null || v.trim().isEmpty()) continue;
                String key = v.trim();
                if (newestByVariety.containsKey(key)) {
                    resolvedFileNames.add(newestByVariety.get(key));
                }
            }
        }

        if (resolvedFileNames == null || resolvedFileNames.isEmpty()) {
            throw new IllegalArgumentException("必须提供 file_names 或 varieties（至少一个）");
        }

        // 目标目录：允许用户指定相对项目根目录的任意子目录（会做normalize并限制不能跳出项目目录）
        Path placedDir = resolveSafeTargetDir(root, targetDir);
        Files.createDirectories(placedDir);

        List<Map<String, Object>> placedFiles = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (String fn : resolvedFileNames) {
            if (!isSafeFileName(fn)) {
                Map<String, Object> err = new HashMap<>();
                err.put("file_name", fn);
                err.put("message", "文件名不合法");
                errors.add(err);
                continue;
            }

            Path src = splitDir.resolve(fn);
            if (!Files.exists(src) || !Files.isRegularFile(src)) {
                Map<String, Object> err = new HashMap<>();
                err.put("file_name", fn);
                err.put("message", "文件不存在");
                errors.add(err);
                continue;
            }

            Path dst = placedDir.resolve(fn);
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> info = parseSplitFileName(fn);
            info.put("file_name", fn);
            String relPlacedDir = safeRelativize(root, placedDir);
            info.put("target_dir", relPlacedDir);
            info.put("placed_path", (relPlacedDir + "/" + fn).replace("\\", "/"));
            info.put("download_url", "http://localhost:8080/api/v1/agriculture/price/download?scope=dir&dir=" + urlEncode(relPlacedDir) + "&file_name=" + urlEncode(fn));
            placedFiles.add(info);
        }

        Map<String, Object> result = new HashMap<>();
        String relPlacedDir = safeRelativize(root, placedDir);
        result.put("target_dir", relPlacedDir);
        result.put("placed_files", placedFiles);
        result.put("placed_count", placedFiles.size());
        result.put("errors", errors);
        result.put("error_count", errors.size());
        return result;
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
            columnIndexMap.put(normalizeHeader(headers[i]), i);
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
            Path outputXlsxFile = outputDir.resolve(safeFileName + "_" + timestamp + ".xlsx");

            // 写入数据
            try (OutputStream os = Files.newOutputStream(outputFile);
                 OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {
                // 写入 UTF-8 BOM，提升 Windows/Excel 对中文的兼容性
                writer.write(UTF8_BOM);

                // 写入表头（只包含规格、平均价、发布日期）
                writer.write("规格,平均价,发布日期");
                writer.newLine();

                // 写入数据行（避免最后多一个空行：最后一行不再额外 newLine）
                for (int i = 0; i < dataRows.size(); i++) {
                    Map<String, String> row = dataRows.get(i);
                    writer.write(String.join(",",
                            safeCell(row.get("规格")),
                            safeCell(row.get("平均价")),
                            safeCell(row.get("发布日期"))));
                    if (i < dataRows.size() - 1) {
                        writer.newLine();
                    }
                }
            }

            // 同时写出 xlsx（日期列放第一列，便于系统后续解析/预测时直接上传使用）
            writeSplitXlsx(outputXlsxFile, dataRows);
        }
    }

    /**
     * 将已有的 result/split/*.csv 批量导出为同名 .xlsx（不会重新爬取）
     */
    public Map<String, Object> exportExistingSplitCsvToXlsx() throws IOException {
        String projectRoot = System.getProperty("user.dir");
        Path splitDir = Paths.get(projectRoot, "result", "split");

        Map<String, Object> result = new HashMap<>();
        int converted = 0;
        int skipped = 0;
        int failed = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        if (!Files.exists(splitDir) || !Files.isDirectory(splitDir)) {
            result.put("converted", 0);
            result.put("skipped", 0);
            result.put("failed", 0);
            result.put("errors", errors);
            return result;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitDir, "*.csv")) {
            for (Path csvPath : stream) {
                if (!Files.isRegularFile(csvPath)) continue;

                String csvName = csvPath.getFileName().toString();
                String base = stripExtension(csvName);
                if (base == null || base.isEmpty()) {
                    skipped++;
                    continue;
                }

                Path xlsxPath = splitDir.resolve(base + ".xlsx");
                if (Files.exists(xlsxPath)) {
                    skipped++;
                    continue;
                }

                try {
                    List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
                    if (lines.size() < 2) {
                        skipped++;
                        continue;
                    }

                    // 从第二行开始解析（第一行表头）
                    List<Map<String, String>> dataRows = new ArrayList<>();
                    for (int i = 1; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line == null) continue;
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        String[] values = parseCsvLine(line);
                        if (values.length < 3) continue;
                        Map<String, String> rowData = new HashMap<>();
                        rowData.put("规格", safeCell(values[0]));
                        rowData.put("平均价", safeCell(values[1]));
                        rowData.put("发布日期", safeCell(values[2]));
                        dataRows.add(rowData);
                    }

                    if (dataRows.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    writeSplitXlsx(xlsxPath, dataRows);
                    converted++;
                } catch (Exception e) {
                    failed++;
                    Map<String, Object> err = new HashMap<>();
                    err.put("file_name", csvName);
                    err.put("message", e.getMessage());
                    errors.add(err);
                }
            }
        }

        result.put("converted", converted);
        result.put("skipped", skipped);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    /**
     * 写出 split 的 xlsx：
     * - A列：发布日期（yyyy-MM-dd）
     * - B列：平均价（数字）
     * - C列：规格（字符串）
     */
    private void writeSplitXlsx(Path outputXlsxFile, List<Map<String, String>> dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("data");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("发布日期");
            header.createCell(1).setCellValue("平均价");
            header.createCell(2).setCellValue("规格");

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            int rowIdx = 1;
            for (Map<String, String> row : dataRows) {
                Row r = sheet.createRow(rowIdx++);

                String dateStr = safeCell(row.get("发布日期"));
                Cell dateCell = r.createCell(0);
                // 尝试按 yyyy-MM-dd 写入为日期；失败则写字符串
                try {
                    LocalDate ld = LocalDate.parse(dateStr);
                    java.util.Date d = java.sql.Date.valueOf(ld);
                    dateCell.setCellValue(d);
                    dateCell.setCellStyle(dateStyle);
                } catch (Exception e) {
                    dateCell.setCellValue(dateStr);
                }

                String priceStr = safeCell(row.get("平均价"));
                Cell priceCell = r.createCell(1);
                try {
                    priceCell.setCellValue(Double.parseDouble(priceStr));
                } catch (Exception e) {
                    priceCell.setCellValue(priceStr);
                }

                r.createCell(2).setCellValue(safeCell(row.get("规格")));
            }

            // 简单设置列宽（避免太窄）
            sheet.setColumnWidth(0, 14 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 18 * 256);

            try (OutputStream os = Files.newOutputStream(outputXlsxFile)) {
                workbook.write(os);
            }
        }
    }

    /**
     * 规范化表头单元格：去掉可能存在的 UTF-8 BOM，并 trim。
     */
    private String normalizeHeader(String h) {
        if (h == null) return "";
        String s = h;
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.trim();
    }

    private String safeCell(String s) {
        return s == null ? "" : s.trim();
    }

    private String stripExtension(String fileName) {
        if (fileName == null) return null;
        String f = fileName.trim();
        if (f.toLowerCase().endsWith(".csv")) {
            return f.substring(0, f.length() - 4);
        }
        if (f.toLowerCase().endsWith(".xlsx")) {
            return f.substring(0, f.length() - 5);
        }
        return f;
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

    private Map<String, Object> parseSplitFileName(String fileName) {
        Map<String, Object> info = new HashMap<>();
        if (fileName == null) return info;

        String base = stripExtension(fileName);

        String[] parts = base.split("_");
        if (parts.length >= 3) {
            String timestamp = parts[parts.length - 2] + "_" + parts[parts.length - 1];
            StringBuilder varietyBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) {
                if (i > 0) varietyBuilder.append("_");
                varietyBuilder.append(parts[i]);
            }
            info.put("variety", varietyBuilder.toString());
            info.put("timestamp", timestamp);
        } else if (parts.length >= 1) {
            info.put("variety", parts[0]);
        }
        return info;
    }

    private Map<String, String> pickNewestFileByVariety(Path splitDir) throws IOException {
        Map<String, String> newest = new HashMap<>();
        Map<String, Long> newestMtime = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitDir, "*.csv")) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                String csvFileName = p.getFileName().toString();
                Map<String, Object> parsed = parseSplitFileName(csvFileName);
                String variety = parsed.get("variety") != null ? parsed.get("variety").toString() : null;
                if (variety == null || variety.trim().isEmpty()) continue;

                // 如果同名 xlsx 存在，则优先选择 xlsx（并以 xlsx mtime 为准）
                long mtime = Files.getLastModifiedTime(p).toMillis();
                String chosenName = csvFileName;
                String base = stripExtension(csvFileName);
                if (base != null && !base.isEmpty()) {
                    Path xlsxPath = splitDir.resolve(base + ".xlsx");
                    if (Files.exists(xlsxPath) && Files.isRegularFile(xlsxPath)) {
                        long xmtime = Files.getLastModifiedTime(xlsxPath).toMillis();
                        if (xmtime >= mtime) {
                            mtime = xmtime;
                            chosenName = xlsxPath.getFileName().toString();
                        }
                    }
                }

                if (!newestMtime.containsKey(variety) || mtime > newestMtime.get(variety)) {
                    newestMtime.put(variety, mtime);
                    newest.put(variety, chosenName);
                }
            }
        }
        return newest;
    }

    private boolean isSafeFileName(String fileName) {
        if (fileName == null) return false;
        String f = fileName.trim();
        if (f.isEmpty()) return false;
        if (f.contains("/") || f.contains("\\") || f.contains(":")) return false;
        if (f.contains("..")) return false;
        String lower = f.toLowerCase();
        return lower.endsWith(".csv") || lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    private boolean isSafePathSegment(String segment) {
        if (segment == null) return false;
        String s = segment.trim();
        if (s.isEmpty()) return false;
        if (s.contains("/") || s.contains("\\") || s.contains(":")) return false;
        if (s.contains("..")) return false;
        return true;
    }

    /**
     * 解析/校验用户指定的目标目录：
     * - 允许形如 result/placed/xxx 或 result/custom/xxx
     * - normalize 后必须在项目根目录内，防止路径穿越
     * - 为空则默认 result/placed/<timestamp>
     */
    private Path resolveSafeTargetDir(Path projectRoot, String targetDir) {
        Path root = projectRoot.toAbsolutePath().normalize();
        if (targetDir == null || targetDir.trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            return root.resolve(Paths.get("result", "placed", timestamp)).normalize();
        }

        String td = targetDir.trim();
        if (td.contains(":")) {
            throw new IllegalArgumentException("target_dir 不合法（禁止包含冒号）");
        }

        Path resolved = root.resolve(td).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("target_dir 不允许跳出项目目录");
        }
        return resolved;
    }

    private String safeRelativize(Path projectRoot, Path absolutePath) {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path abs = absolutePath.toAbsolutePath().normalize();
        if (!abs.startsWith(root)) {
            return abs.toString().replace("\\", "/");
        }
        return root.relativize(abs).toString().replace("\\", "/");
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
}
