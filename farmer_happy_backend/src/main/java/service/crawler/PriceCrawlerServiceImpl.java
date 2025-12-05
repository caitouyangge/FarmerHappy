// service/crawler/PriceCrawlerServiceImpl.java
package service.crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Paths;

public class PriceCrawlerServiceImpl implements PriceCrawlerService {

    @Override
    public Map<String, Object> crawlAgriculturalPrices(String startTime, String endTime, String productName) throws Exception {
        Map<String, Object> response = new HashMap<>();

        // 构建Python脚本路径 - 保持原路径不变
        String projectRoot = System.getProperty("user.dir");
        String pythonScriptPath = Paths.get(projectRoot, "python","python", "xinfadi_crawler.py").toString();

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

            // 更稳健的JSON解析方法
            if (outputStr.startsWith("{") && outputStr.endsWith("}")) {
                try {
                    // 查找file_name字段，使用更精确的匹配方式
                    int fileNameStart = outputStr.indexOf("\"file_name\"");
                    if (fileNameStart != -1) {
                        // 找到冒号位置
                        int colonIndex = outputStr.indexOf(":", fileNameStart);
                        if (colonIndex != -1) {
                            // 找到第一个引号
                            int firstQuote = outputStr.indexOf("\"", colonIndex);
                            if (firstQuote != -1) {
                                // 找到第二个引号
                                int secondQuote = outputStr.indexOf("\"", firstQuote + 1);
                                if (secondQuote != -1) {
                                    String fileName = outputStr.substring(firstQuote + 1, secondQuote);
                                    response.put("file_name", fileName);
                                    return response;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("解析JSON时出错: " + e.getMessage());
                }
            }

            // 如果无法解析JSON，只返回最后一行可能的JSON输出
            String[] lines = outputStr.split("\n");
            if (lines.length > 0) {
                String lastLine = lines[lines.length - 1].trim();
                if (lastLine.startsWith("{") && lastLine.endsWith("}")) {
                    try {
                        // 尝试解析最后一行作为JSON
                        int fileNameStart = lastLine.indexOf("\"file_name\"");
                        if (fileNameStart != -1) {
                            int colonIndex = lastLine.indexOf(":", fileNameStart);
                            if (colonIndex != -1) {
                                int firstQuote = lastLine.indexOf("\"", colonIndex);
                                if (firstQuote != -1) {
                                    int secondQuote = lastLine.indexOf("\"", firstQuote + 1);
                                    if (secondQuote != -1) {
                                        String fileName = lastLine.substring(firstQuote + 1, secondQuote);
                                        response.put("file_name", fileName);
                                        return response;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("解析最后一行JSON时出错: " + e.getMessage());
                    }
                }
            }

            // 如果仍然无法解析JSON，返回原始输出
            response.put("raw_output", outputStr);
            return response;
        } else {
            throw new Exception("Python脚本执行失败: " + errorOutput.toString());
        }
    }
}
