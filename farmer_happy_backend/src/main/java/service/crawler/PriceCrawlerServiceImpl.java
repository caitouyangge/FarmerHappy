// service/crawler/PriceCrawlerServiceImpl.java
package service.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class PriceCrawlerServiceImpl implements PriceCrawlerService {

    // 添加CsvSplitterService实例
    private final CsvSplitterService csvSplitterService = new CsvSplitterService();

    @Override
    public Map<String, Object> crawlAgriculturalPrices(String startTime, String endTime, String productName) throws Exception {
        // 清理result目录中的旧数据
        cleanResultDirectory();

        Map<String, Object> response = new HashMap<>();

        // 构建Python脚本路径 - 保持原路径不变
        String projectRoot = System.getProperty("user.dir");
        String pythonScriptPath = Paths.get(projectRoot, "python", "xinfadi_crawler.py").toString();

        // 构建命令行参数
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                "--start_time", startTime,
                "--end_time", endTime,
                "--product_name", productName
        );

        // 设置工作目录为项目根目录
        processBuilder.directory(Paths.get(projectRoot).toFile());

        // 启动进程 - 指定UTF-8编码以避免中文乱码
        Process process = processBuilder.start();

        // 读取标准输出 - 使用UTF-8编码
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // 读取错误输出 - 使用UTF-8编码
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
        StringBuilder errorOutput = new StringBuilder();
        String errorLine;
        while ((errorLine = errorReader.readLine()) != null) {
            errorOutput.append(errorLine).append("\n");
        }

        // 等待进程结束
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            // 解析Python脚本的输出
            String outputStr = output.toString().trim();
            System.out.println("Python output: " + outputStr); // 调试日志

            // 使用更安全的方式解析文件名
            String fileName = parseFileNameFromOutput(outputStr);
            if (fileName != null && !fileName.isEmpty()) {
                response.put("file_name", fileName);

                // 调用Java方法分割CSV文件
                splitCsvFile(fileName);

                // 返回 split 文件列表（前端用于动态勾选品种）
                try {
                    response.put("split_dir", "result/split");
                    response.put("split_files", csvSplitterService.listSplitFiles());
                } catch (Exception e) {
                    // 不影响主流程
                    response.put("split_files", new java.util.ArrayList<>());
                }

                System.out.println("Python output: " + fileName);
                return response;
            }

            // 如果仍然无法解析JSON，返回原始输出
            response.put("raw_output", outputStr);
            return response;
        } else {
            throw new Exception("Python脚本执行失败: " + errorOutput.toString());
        }
    }

    /**
     * 清理result目录中的旧数据文件
     */
    private void cleanResultDirectory() {
        String projectRoot = System.getProperty("user.dir");
        Path resultDir = Paths.get(projectRoot, "result");
        Path splitDir = Paths.get(projectRoot, "result", "split");

        try {
            // 确保result目录存在
            Files.createDirectories(resultDir);

            // 删除 result 目录下的普通文件（保留子目录，例如 placed）
            try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(resultDir)) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            System.err.println("删除文件失败: " + p + ", 错误: " + e.getMessage());
                        }
                    }
                }
            }

            // 清空 split 目录（每次爬取重新生成），但不删除其他目录
            if (Files.exists(splitDir)) {
                deleteDirectoryRecursively(splitDir);
            }
            Files.createDirectories(splitDir);
        } catch (IOException e) {
            System.err.println("清理或创建result目录失败: " + e.getMessage());
        }
    }

    /**
     * 递归删除目录及其内容
     *
     * @param directory 目录路径
     * @throws IOException IO异常
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // 先删除子文件再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("删除文件/目录失败: " + path.toString() + ", 错误: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * 使用字符串方式安全解析文件名
     */
    private String parseFileNameFromOutput(String outputStr) {
        // 查找最后出现的完整JSON对象
        int lastBraceStart = outputStr.lastIndexOf("{");
        int lastBraceEnd = outputStr.lastIndexOf("}");

        if (lastBraceStart != -1 && lastBraceEnd != -1 && lastBraceEnd > lastBraceStart) {
            String lastJsonObject = outputStr.substring(lastBraceStart, lastBraceEnd + 1);

            // 在这个JSON对象中查找file_name
            int fileNameIndex = lastJsonObject.indexOf("\"file_name\"");
            if (fileNameIndex != -1) {
                int colonIndex = lastJsonObject.indexOf(":", fileNameIndex);
                if (colonIndex != -1) {
                    int firstQuote = lastJsonObject.indexOf("\"", colonIndex);
                    if (firstQuote != -1) {
                        int secondQuote = lastJsonObject.indexOf("\"", firstQuote + 1);
                        if (secondQuote != -1) {
                            return lastJsonObject.substring(firstQuote + 1, secondQuote);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 分割CSV文件
     *
     * @param fileName 文件名
     * @throws Exception 异常
     */
    private void splitCsvFile(String fileName) throws Exception {
        csvSplitterService.splitCsvFile(fileName);
    }
}
