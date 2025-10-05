import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import config.RouterConfig;
import repository.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

public class application {

    public static void main(String[] args) {
        System.out.println("农乐助农平台后端服务启动中...");

        // 初始化数据库管理器
        DatabaseManager dbManager = new DatabaseManager();

        // 初始化数据库和表
        dbManager.initializeDatabase();

        try {
            // 创建HTTP服务器，监听8080端口
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // 创建路由器配置实例
            RouterConfig routerConfig = new RouterConfig();

            // 设置请求处理器
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    // 解析请求信息
                    String path = exchange.getRequestURI().getPath();
                    String method = exchange.getRequestMethod();

                    // 处理请求并获取响应
                    Map<String, Object> response = routerConfig.handleRequest(path, method, parseRequestBody(exchange));

                    // 发送响应
                    String jsonResponse = toJson(response);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

                    OutputStream os = exchange.getResponseBody();
                    os.write(jsonResponse.getBytes());
                    os.close();
                }

                // 替换原有的 parseRequestBody 方法
                private Map<String, Object> parseRequestBody(HttpExchange exchange) {
                    try {
                        if ("GET".equals(exchange.getRequestMethod()) ||
                                "HEAD".equals(exchange.getRequestMethod())) {
                            return new java.util.HashMap<>();
                        }

                        StringBuilder requestBody = new StringBuilder();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(exchange.getRequestBody(), "UTF-8")
                        );
                        String line;
                        while ((line = reader.readLine()) != null) {
                            requestBody.append(line);
                        }
                        reader.close();

                        System.out.println("Received request body: " + requestBody.toString()); // 添加日志便于调试

                        if (requestBody.length() == 0) {
                            return new java.util.HashMap<>();
                        }

                        return parseJsonString(requestBody.toString());
                    } catch (Exception e) {
                        System.err.println("解析请求体失败: " + e.getMessage());
                        e.printStackTrace(); // 打印完整的堆栈跟踪
                        return new java.util.HashMap<>();
                    }
                }

                // 改进 parseJsonString 方法
                private Map<String, Object> parseJsonString(String jsonString) {
                    Map<String, Object> result = new java.util.HashMap<>();
                    try {
                        // 去除首尾空格
                        String cleanJson = jsonString.trim();
                        System.out.println("Parsing JSON: " + cleanJson); // 添加调试日志

                        // 检查是否为有效的JSON对象
                        if (!cleanJson.startsWith("{") || !cleanJson.endsWith("}")) {
                            System.err.println("Invalid JSON format: not an object");
                            return result;
                        }

                        // 去除大括号
                        cleanJson = cleanJson.substring(1, cleanJson.length() - 1).trim();

                        // 如果为空对象，直接返回
                        if (cleanJson.isEmpty()) {
                            return result;
                        }

                        // 解析键值对
                        int i = 0;
                        while (i < cleanJson.length()) {
                            // 跳过空白字符
                            while (i < cleanJson.length() && Character.isWhitespace(cleanJson.charAt(i))) {
                                i++;
                            }

                            if (i >= cleanJson.length()) break;

                            // 解析键
                            if (cleanJson.charAt(i) != '"') {
                                // 键必须用双引号包围
                                System.err.println("Key must be quoted at position " + i);
                                return result;
                            }

                            i++; // 跳过开始的引号
                            StringBuilder keyBuilder = new StringBuilder();
                            while (i < cleanJson.length() && cleanJson.charAt(i) != '"') {
                                if (cleanJson.charAt(i) == '\\') {
                                    // 处理转义字符
                                    i++;
                                    if (i < cleanJson.length()) {
                                        keyBuilder.append(cleanJson.charAt(i));
                                    }
                                } else {
                                    keyBuilder.append(cleanJson.charAt(i));
                                }
                                i++;
                            }

                            if (i >= cleanJson.length()) {
                                // 未找到结束引号
                                System.err.println("Missing end quote for key");
                                return result;
                            }

                            i++; // 跳过结束引号

                            // 跳过空白字符和冒号
                            while (i < cleanJson.length() && (Character.isWhitespace(cleanJson.charAt(i)) || cleanJson.charAt(i) == ':')) {
                                i++;
                            }

                            if (i >= cleanJson.length()) {
                                break;
                            }

                            // 解析值
                            Object value;
                            if (cleanJson.charAt(i) == '"') {
                                // 字符串值
                                i++; // 跳过开始引号
                                StringBuilder valueBuilder = new StringBuilder();
                                while (i < cleanJson.length() && cleanJson.charAt(i) != '"') {
                                    if (cleanJson.charAt(i) == '\\') {
                                        // 处理转义字符
                                        i++;
                                        if (i < cleanJson.length()) {
                                            valueBuilder.append(cleanJson.charAt(i));
                                        }
                                    } else {
                                        valueBuilder.append(cleanJson.charAt(i));
                                    }
                                    i++;
                                }
                                value = valueBuilder.toString();
                                if (i < cleanJson.length()) {
                                    i++; // 跳过结束引号
                                }
                            } else if (cleanJson.charAt(i) == 't' && i + 3 < cleanJson.length() &&
                                    cleanJson.substring(i, i + 4).equals("true")) {
                                // 布尔值 true
                                value = true;
                                i += 4;
                            } else if (cleanJson.charAt(i) == 'f' && i + 4 < cleanJson.length() &&
                                    cleanJson.substring(i, i + 5).equals("false")) {
                                // 布尔值 false
                                value = false;
                                i += 5;
                            } else if (cleanJson.charAt(i) == 'n' && i + 3 < cleanJson.length() &&
                                    cleanJson.substring(i, i + 4).equals("null")) {
                                // null值
                                value = null;
                                i += 4;
                            } else if (Character.isDigit(cleanJson.charAt(i)) || cleanJson.charAt(i) == '-') {
                                // 数字值
                                StringBuilder numBuilder = new StringBuilder();
                                while (i < cleanJson.length() && (Character.isDigit(cleanJson.charAt(i)) ||
                                        cleanJson.charAt(i) == '.' || cleanJson.charAt(i) == '-' ||
                                        cleanJson.charAt(i) == 'e' || cleanJson.charAt(i) == 'E')) {
                                    numBuilder.append(cleanJson.charAt(i));
                                    i++;
                                }
                                String numStr = numBuilder.toString();
                                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                                    value = Double.parseDouble(numStr);
                                } else {
                                    try {
                                        value = Integer.parseInt(numStr);
                                    } catch (NumberFormatException e) {
                                        value = Long.parseLong(numStr);
                                    }
                                }
                            } else {
                                // 无法识别的值类型
                                System.err.println("Unrecognized value type at position " + i);
                                i++;
                                continue;
                            }

                            // 添加到结果中
                            String key = keyBuilder.toString();
                            result.put(key, value);
                            System.out.println("Parsed key-value: " + key + " = " + value); // 调试日志

                            // 跳过空白字符和逗号
                            while (i < cleanJson.length() && (Character.isWhitespace(cleanJson.charAt(i)) || cleanJson.charAt(i) == ',')) {
                                i++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("解析JSON失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return result;
                }


                private String toJson(Map<String, Object> map) {
                    StringBuilder json = new StringBuilder("{");
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        json.append("\"").append(entry.getKey()).append("\":");
                        if (entry.getValue() instanceof String) {
                            json.append("\"").append(entry.getValue()).append("\"");
                        } else {
                            json.append(entry.getValue());
                        }
                        json.append(",");
                    }
                    if (json.length() > 1) json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    json.append("}");
                    return json.toString();
                }
            });

            // 启动服务器
            server.setExecutor(null); // 使用默认executor
            server.start();

            System.out.println("服务已启动，监听端口8080...");
        } catch (IOException e) {
            System.err.println("启动服务器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
