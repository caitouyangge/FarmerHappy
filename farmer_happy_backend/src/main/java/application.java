// src/application.java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import config.RouterConfig;
import dto.auth.AuthResponseDTO;
import dto.bank.LoanApprovalResponseDTO;
import dto.bank.LoanDisbursementResponseDTO;
import dto.farmer.ProductListResponseDTO;
import dto.farmer.ProductResponseDTO;
import dto.farmer.ProductStatusUpdateResponseDTO;
import dto.farmer.ProductDetailResponseDTO;
import dto.farmer.ProductBatchActionResultDTO;
import dto.farmer.PricePredictionResponseDTO;
import dto.community.*;
import dto.buyer.*;
import dto.financing.*;
import dto.crawler.*;
import dto.expert.*;
import repository.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class application {

    public static void main(String[] args) {
        System.out.println("农乐助农平台后端服务启动中...");

        // 初始化数据库管理器
        DatabaseManager dbManager = DatabaseManager.getInstance();

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
                    try {
                        // 解析请求信息
                        String path = exchange.getRequestURI().getPath();
                        String method = exchange.getRequestMethod();
                        String query = exchange.getRequestURI().getQuery();

                        System.out.println("处理请求: " + method + " " + path + (query != null ? "?" + query : ""));

                        // 解析URL查询参数（需要在处理文件下载之前）
                        Map<String, String> queryParams = parseQueryParams(query);

                        // 处理CSV文件下载
                        if ("GET".equals(method) && "/api/v1/agriculture/price/download".equals(path)) {
                            String fileName = queryParams != null ? queryParams.get("file_name") : null;
                            String scope = queryParams != null ? queryParams.getOrDefault("scope", "root") : "root";
                            String location = queryParams != null ? queryParams.get("location") : null;
                            String dir = queryParams != null ? queryParams.get("dir") : null;
                            if (fileName != null && !fileName.isEmpty()) {
                                String projectRoot = System.getProperty("user.dir");
                                
                                // 安全校验，防止路径穿越
                                if (!isSafeFileName(fileName)) {
                                    exchange.sendResponseHeaders(400, -1);
                                    return;
                                }

                                java.nio.file.Path filePath;
                                if ("split".equalsIgnoreCase(scope)) {
                                    filePath = java.nio.file.Paths.get(projectRoot, "result", "split", fileName);
                                } else if ("dir".equalsIgnoreCase(scope)) {
                                    if (!isSafeRelativeDir(dir)) {
                                        exchange.sendResponseHeaders(400, -1);
                                        return;
                                    }
                                    java.nio.file.Path root = java.nio.file.Paths.get(projectRoot).toAbsolutePath().normalize();
                                    filePath = root.resolve(dir).resolve(fileName).normalize();
                                    if (!filePath.startsWith(root)) {
                                        exchange.sendResponseHeaders(400, -1);
                                        return;
                                    }
                                } else if ("placed".equalsIgnoreCase(scope)) {
                                    if (!isSafePathSegment(location)) {
                                        exchange.sendResponseHeaders(400, -1);
                                        return;
                                    }
                                    filePath = java.nio.file.Paths.get(projectRoot, "result", "placed", location, fileName);
                                } else {
                                    // scope=root（默认）：兼容旧逻辑，文件在 result 目录
                                    filePath = java.nio.file.Paths.get(projectRoot, "result", fileName);
                                }
                                
                                System.out.println("查找CSV文件 - 项目根目录: " + projectRoot);
                                System.out.println("查找CSV文件 - 文件名: " + fileName);
                                System.out.println("查找CSV文件 - 路径1: " + filePath.toString());
                                System.out.println("查找CSV文件 - 路径1存在: " + java.nio.file.Files.exists(filePath));
                                
                                // 如果文件不存在，尝试在python/python/result目录查找（兼容旧路径）
                                if ("root".equalsIgnoreCase(scope) && !java.nio.file.Files.exists(filePath)) {
                                    filePath = java.nio.file.Paths.get(projectRoot, "python", "python", "result", fileName);
                                    System.out.println("查找CSV文件 - 路径2: " + filePath.toString());
                                    System.out.println("查找CSV文件 - 路径2存在: " + java.nio.file.Files.exists(filePath));
                                }
                                
                                if (java.nio.file.Files.exists(filePath)) {
                                    System.out.println("找到CSV文件: " + filePath.toString());
                                    try {
                                        // 设置响应头（按扩展名返回更准确的 Content-Type）
                                        exchange.getResponseHeaders().set("Content-Type", guessDownloadContentType(fileName));
                                        // 使用URL编码的文件名，确保中文字符正确显示
                                        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
                                        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
                                        
                                        byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
                                        exchange.sendResponseHeaders(200, bytes.length);
                                        OutputStream os = exchange.getResponseBody();
                                        os.write(bytes);
                                        os.close();
                                        System.out.println("文件下载成功");
                                        return;
                                    } catch (Exception e) {
                                        System.err.println("下载文件时出错: " + e.getMessage());
                                        e.printStackTrace();
                                        exchange.sendResponseHeaders(500, -1);
                                        return;
                                    }
                                } else {
                                    System.out.println("文件不存在: " + filePath.toString());
                                    exchange.sendResponseHeaders(404, -1);
                                    return;
                                }
                            } else {
                                exchange.sendResponseHeaders(400, -1);
                                return;
                            }
                        }

                        if ("GET".equals(method) && path.startsWith("/uploads/")) {
                            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads").resolve(path.substring("/uploads/".length()));
                            if (java.nio.file.Files.exists(filePath)) {
                                String contentType;
                                String p = path.toLowerCase();
                                if (p.endsWith(".png")) contentType = "image/png";
                                else if (p.endsWith(".jpg") || p.endsWith(".jpeg")) contentType = "image/jpeg";
                                else if (p.endsWith(".gif")) contentType = "image/gif";
                                else contentType = "application/octet-stream";
                                exchange.getResponseHeaders().set("Content-Type", contentType);
                                byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
                                exchange.sendResponseHeaders(200, bytes.length);
                                OutputStream os = exchange.getResponseBody();
                                os.write(bytes);
                                os.close();
                                return;
                            } else {
                                exchange.sendResponseHeaders(404, -1);
                                return;
                            }
                        }

                        // 解析请求体
                        Map<String, Object> requestBody = parseRequestBody(exchange);

                        // 解析请求头
                        Map<String, String> headers = new HashMap<>();
                        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                            if (!entry.getValue().isEmpty()) {
                                headers.put(entry.getKey(), entry.getValue().get(0));
                            }
                        }

                        // 处理请求并获取响应（传递查询参数，queryParams已在前面定义）
                        Map<String, Object> response = routerConfig.handleRequest(path, method, requestBody, headers, queryParams);

                        System.out.println("生成响应: code=" + response.get("code"));

                        // 发送响应
                        String jsonResponse = toJson(response);
                        System.out.println("JSON响应: " + jsonResponse);
                        
                        // 设置正确的Content-Type和字符编码
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        
                        // 根据响应中的code字段返回对应的HTTP状态码
                        int httpStatusCode = 200;
                        if (response.containsKey("code")) {
                            Object codeObj = response.get("code");
                            if (codeObj instanceof Number) {
                                httpStatusCode = ((Number) codeObj).intValue();
                            }
                        }
                        
                        // 对于204状态码，不发送响应体
                        if (httpStatusCode == 204) {
                            exchange.sendResponseHeaders(httpStatusCode, -1);
                        } else {
                            exchange.sendResponseHeaders(httpStatusCode, jsonResponse.getBytes("UTF-8").length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(jsonResponse.getBytes("UTF-8"));
                            os.close();
                        }
                        
                        System.out.println("响应发送完成");
                    } catch (Exception e) {
                        System.err.println("处理请求时发生错误: " + e.getMessage());
                        e.printStackTrace();

                        // 发送500错误响应
                        try {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("code", 500);
                            errorResponse.put("message", "服务器内部错误: " + e.getMessage());

                            String jsonResponse = toJson(errorResponse);
                            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                            exchange.sendResponseHeaders(500, jsonResponse.getBytes("UTF-8").length);

                            OutputStream os = exchange.getResponseBody();
                            os.write(jsonResponse.getBytes("UTF-8"));
                            os.close();
                        } catch (Exception ex) {
                            System.err.println("发送错误响应失败: " + ex.getMessage());
                            // 如果发送错误响应也失败，尝试发送基本的错误状态码
                            try {
                                exchange.sendResponseHeaders(500, -1);
                            } catch (Exception ex2) {
                                System.err.println("发送基本错误状态码也失败: " + ex2.getMessage());
                            }
                        }
                    }

                }

                // 解析URL查询参数
                private Map<String, String> parseQueryParams(String query) {
                    Map<String, String> params = new HashMap<>();
                    if (query == null || query.trim().isEmpty()) {
                        return params;
                    }
                    
                    try {
                        String[] pairs = query.split("&");
                        for (String pair : pairs) {
                            int idx = pair.indexOf("=");
                            if (idx > 0) {
                                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                                params.put(key, value);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("解析查询参数失败: " + e.getMessage());
                    }
                    
                    return params;
                }

                // 防止路径穿越：仅允许“文件名”，禁止任何分隔符/冒号/..
                private boolean isSafeFileName(String fileName) {
                    if (fileName == null) return false;
                    String f = fileName.trim();
                    if (f.isEmpty()) return false;
                    if (f.contains("/") || f.contains("\\") || f.contains(":")) return false;
                    if (f.contains("..")) return false;
                    return true;
                }

                private String guessDownloadContentType(String fileName) {
                    if (fileName == null) return "application/octet-stream";
                    String f = fileName.toLowerCase();
                    if (f.endsWith(".csv")) {
                        return "text/csv; charset=UTF-8";
                    }
                    if (f.endsWith(".xlsx")) {
                        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    }
                    if (f.endsWith(".xls")) {
                        return "application/vnd.ms-excel";
                    }
                    return "application/octet-stream";
                }

                // 防止路径穿越：仅允许单段目录名
                private boolean isSafePathSegment(String segment) {
                    if (segment == null) return false;
                    String s = segment.trim();
                    if (s.isEmpty()) return false;
                    if (s.contains("/") || s.contains("\\") || s.contains(":")) return false;
                    if (s.contains("..")) return false;
                    return true;
                }

                // 允许相对目录（可包含/或\），但必须是相对路径且不能包含 .. 或冒号
                private boolean isSafeRelativeDir(String dir) {
                    if (dir == null) return false;
                    String d = dir.trim();
                    if (d.isEmpty()) return false;
                    if (d.contains(":")) return false;
                    if (d.startsWith("/") || d.startsWith("\\\\")) return false;
                    if (d.contains("..")) return false;
                    return true;
                }

                // 替换原有的 parseRequestBody 方法
                private Map<String, Object> parseRequestBody(HttpExchange exchange) {
                    try {
                        if ("GET".equals(exchange.getRequestMethod()) ||
                                "HEAD".equals(exchange.getRequestMethod())) {
                            // 对于GET请求，返回空Map（查询参数已经单独解析）
                            return new HashMap<>();
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
                            return new HashMap<>();
                        }

                        return parseJsonString(requestBody.toString());
                    } catch (Exception e) {
                        System.err.println("解析请求体失败: " + e.getMessage());
                        e.printStackTrace(); // 打印完整的堆栈跟踪
                        return new HashMap<>();
                    }
                }

                // 改进 parseJsonString 方法
                private Map<String, Object> parseJsonString(String jsonString) {
                    Map<String, Object> result = new HashMap<>();
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
                                break;
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
                                break;
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
                            ParseResult parseResult = parseJsonValue(cleanJson, i);
                            Object value = parseResult.value;
                            i = parseResult.nextIndex;

                            // 添加到结果中
                            String key = keyBuilder.toString();
                            result.put(key, value);
                            System.out.println("Parsed key-value: " + key + " = " + value); // 调试日志

                            // 跳过空白字符并找到下一个逗号或结束位置
                            while (i < cleanJson.length() && Character.isWhitespace(cleanJson.charAt(i))) {
                                i++;
                            }

                            if (i < cleanJson.length() && cleanJson.charAt(i) == ',') {
                                i++; // 跳过逗号
                            } else if (i < cleanJson.length() && cleanJson.charAt(i) != '}') {
                                // 如果不是结束括号也不是逗号，可能是格式错误
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("解析JSON失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return result;
                }

                // 新增类：封装解析结果和下一个位置
                class ParseResult {
                    Object value;
                    int nextIndex;

                    ParseResult(Object value, int nextIndex) {
                        this.value = value;
                        this.nextIndex = nextIndex;
                    }
                }

                // 改进方法：解析JSON值（字符串、数字、布尔值、null、对象、数组）
                private ParseResult parseJsonValue(String json, int startIndex) {
                    int i = startIndex;

                    // 跳过前导空白字符
                    while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                        i++;
                    }

                    if (i >= json.length()) {
                        return new ParseResult(null, i);
                    }

                    char ch = json.charAt(i);

                    // 解析字符串值
                    if (ch == '"') {
                        i++; // 跳过开始引号
                        StringBuilder valueBuilder = new StringBuilder();
                        while (i < json.length() && json.charAt(i) != '"') {
                            if (json.charAt(i) == '\\') {
                                // 处理转义字符
                                i++;
                                if (i < json.length()) {
                                    // 处理常见的转义字符
                                    switch (json.charAt(i)) {
                                        case '"': valueBuilder.append('"'); break;
                                        case '\\': valueBuilder.append('\\'); break;
                                        case '/': valueBuilder.append('/'); break;
                                        case 'b': valueBuilder.append('\b'); break;
                                        case 'f': valueBuilder.append('\f'); break;
                                        case 'n': valueBuilder.append('\n'); break;
                                        case 'r': valueBuilder.append('\r'); break;
                                        case 't': valueBuilder.append('\t'); break;
                                        default: valueBuilder.append(json.charAt(i)); break;
                                    }
                                }
                            } else {
                                valueBuilder.append(json.charAt(i));
                            }
                            i++;
                        }
                        i++; // 跳过结束引号
                        return new ParseResult(valueBuilder.toString(), i);
                    }
                    // 解析布尔值 true
                    else if (ch == 't' && i + 3 < json.length() && json.substring(i, i + 4).equals("true")) {
                        i += 4;
                        return new ParseResult(true, i);
                    }
                    // 解析布尔值 false
                    else if (ch == 'f' && i + 4 < json.length() && json.substring(i, i + 5).equals("false")) {
                        i += 5;
                        return new ParseResult(false, i);
                    }
                    // 解析 null 值
                    else if (ch == 'n' && i + 3 < json.length() && json.substring(i, i + 4).equals("null")) {
                        i += 4;
                        return new ParseResult(null, i);
                    }
                    // 解析数字值
                    else if (Character.isDigit(ch) || ch == '-') {
                        StringBuilder numBuilder = new StringBuilder();
                        while (i < json.length() && (Character.isDigit(json.charAt(i)) ||
                                json.charAt(i) == '.' || json.charAt(i) == '-' ||
                                json.charAt(i) == 'e' || json.charAt(i) == 'E')) {
                            numBuilder.append(json.charAt(i));
                            i++;
                        }

                        String numStr = numBuilder.toString();
                        Object value;
                        if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                            value = Double.parseDouble(numStr);
                        } else {
                            try {
                                value = Integer.parseInt(numStr);
                            } catch (NumberFormatException e) {
                                value = Long.parseLong(numStr);
                            }
                        }
                        return new ParseResult(value, i);
                    }
                    // 解析数组
                    else if (ch == '[') {
                        return parseJsonArray(json, i);
                    }
                    // 解析对象
                    else if (ch == '{') {
                        // 递归解析对象
                        int start = i;
                        int braceCount = 1;
                        i++;
                        while (i < json.length() && braceCount > 0) {
                            if (json.charAt(i) == '{') {
                                braceCount++;
                            } else if (json.charAt(i) == '}') {
                                braceCount--;
                            } else if (json.charAt(i) == '"' && i > 0) {
                                // 跳过字符串中的大括号
                                i++;
                                while (i < json.length() && json.charAt(i) != '"') {
                                    if (json.charAt(i) == '\\') {
                                        i++;
                                    }
                                    i++;
                                }
                            }
                            i++;
                        }
                        String objStr = json.substring(start, i);
                        return new ParseResult(objStr, i); // 简化处理，返回原始字符串
                    }

                    return new ParseResult(null, i);
                }


                // 改进方法：解析JSON数组
                private ParseResult parseJsonArray(String json, int startIndex) {
                    List<Object> result = new ArrayList<>();
                    int i = startIndex + 1; // 跳过开始的 '['

                    // 跳过前导空白字符
                    while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                        i++;
                    }

                    // 如果是空数组
                    if (i < json.length() && json.charAt(i) == ']') {
                        return new ParseResult(result, i + 1);
                    }

                    // 解析数组元素
                    while (i < json.length() && json.charAt(i) != ']') {
                        // 跳过空白字符
                        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                            i++;
                        }

                        if (i >= json.length() || json.charAt(i) == ']') break;

                        // 解析数组元素值
                        ParseResult parseResult = parseJsonValue(json, i);
                        Object value = parseResult.value;
                        i = parseResult.nextIndex;

                        // 添加值到结果中
                        result.add(value);

                        // 跳过空白字符
                        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                            i++;
                        }

                        // 跳过逗号
                        if (i < json.length() && json.charAt(i) == ',') {
                            i++;
                        }
                    }

                    // 跳过结束的 ']'
                    if (i < json.length() && json.charAt(i) == ']') {
                        i++;
                    }

                    return new ParseResult(result, i);
                }

                private String toJson(Map<String, Object> map) {
                    if (map == null) {
                        return "null";
                    }
                    StringBuilder json = new StringBuilder("{");
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        try {
                            json.append("\"").append(entry.getKey()).append("\":");
                            json.append(serializeValue(entry.getValue()));
                            json.append(",");
                        } catch (Exception e) {
                            System.err.println("序列化键 '" + entry.getKey() + "' 时出错: " + e.getMessage());
                            e.printStackTrace();
                            // 跳过这个键值对，继续处理其他的
                        }
                    }
                    if (json.length() > 1) json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    json.append("}");
                    return json.toString();
                }

                // 序列化各种类型的值
                private String serializeValue(Object value) {
                    if (value == null) {
                        return "null";
                    } else if (value instanceof String) {
                        return "\"" + escapeJsonString(value.toString()) + "\"";
                    } else if (value instanceof Number) {
                        return value.toString();
                    } else if (value instanceof Boolean) {
                        return value.toString();
                    } else if (value instanceof List) {
                        return serializeList((List<?>) value);
                    } else if (value instanceof Map) {
                        // 确保Map的键是String类型
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) value;
                        return toJson(map);
                    } else if (value instanceof AuthResponseDTO) {
                        return serializeAuthResponseDTO((AuthResponseDTO) value);
                    } else if (value instanceof ProductResponseDTO) {
                        return serializeProductResponseDTO((ProductResponseDTO) value);
                    } else if (value instanceof ProductStatusUpdateResponseDTO) {
                        return serializeProductStatusUpdateResponseDTO((ProductStatusUpdateResponseDTO) value);
                    } else if (value instanceof ProductDetailResponseDTO) {
                        return serializeProductDetailResponseDTO((ProductDetailResponseDTO) value);
                    } else if (value instanceof ProductListResponseDTO) {
                        return serializeProductListResponseDTO((ProductListResponseDTO) value);
                    } else if (value instanceof ProductBatchActionResultDTO) {
                        return serializeProductBatchActionResultDTO((ProductBatchActionResultDTO) value);
                    } else if (value instanceof PublishContentResponseDTO) {
                        return serializePublishContentResponseDTO((PublishContentResponseDTO) value);
                    } else if (value instanceof ContentListResponseDTO) {
                        return serializeContentListResponseDTO((ContentListResponseDTO) value);
                    } else if (value instanceof ContentDetailResponseDTO) {
                        return serializeContentDetailResponseDTO((ContentDetailResponseDTO) value);
                    } else if (value instanceof PostCommentResponseDTO) {
                        return serializePostCommentResponseDTO((PostCommentResponseDTO) value);
                    } else if (value instanceof PostReplyResponseDTO) {
                        return serializePostReplyResponseDTO((PostReplyResponseDTO) value);
                    } else if (value instanceof CommentListResponseDTO) {
                        return serializeCommentListResponseDTO((CommentListResponseDTO) value);
                    } else if (value instanceof ContentListItemDTO) {
                        return serializeContentListItemDTO((ContentListItemDTO) value);
                    } else if (value instanceof CommentItemDTO) {
                        return serializeCommentItemDTO((CommentItemDTO) value);
                    } else if (value instanceof CommentReplyItemDTO) {
                        return serializeCommentReplyItemDTO((CommentReplyItemDTO) value);
                    } else if (value instanceof CreateOrderResponseDTO) {
                        return serializeCreateOrderResponseDTO((CreateOrderResponseDTO) value);
                    } else if (value instanceof UpdateOrderResponseDTO) {
                        return serializeUpdateOrderResponseDTO((UpdateOrderResponseDTO) value);
                    } else if (value instanceof OrderDetailResponseDTO) {
                        return serializeOrderDetailResponseDTO((OrderDetailResponseDTO) value);
                    } else if (value instanceof OrderListResponseDTO) {
                        return serializeOrderListResponseDTO((OrderListResponseDTO) value);
                    } else if (value instanceof OrderListItemDTO) {
                        return serializeOrderListItemDTO((OrderListItemDTO) value);
                    } else if (value instanceof RefundResponseDTO) {
                        return serializeRefundResponseDTO((RefundResponseDTO) value);
                    } else if (value instanceof ConfirmReceiptResponseDTO) {
                        return serializeConfirmReceiptResponseDTO((ConfirmReceiptResponseDTO) value);
                    }else if (value instanceof BankLoanProductResponseDTO) {
                        return serializeBankLoanProductResponseDTO((BankLoanProductResponseDTO) value);
                    } else if (value instanceof LoanProductsResponseDTO) {
                        return serializeLoanProductsResponseDTO((LoanProductsResponseDTO) value);
                    } else if (value instanceof CreditLimitDTO) {
                        return serializeCreditLimitDTO((CreditLimitDTO) value);
                    } else if (value instanceof CreditApplicationDTO) {
                        return serializeCreditApplicationDTO((CreditApplicationDTO) value);
                    } else if (value instanceof LoanProductDTO) {
                        return serializeLoanProductDTO((LoanProductDTO) value);
                    }else if (value instanceof SingleLoanApplicationResponseDTO) {
                        return serializeSingleLoanApplicationResponseDTO((SingleLoanApplicationResponseDTO) value);
                    } else if (value instanceof JointLoanApplicationResponseDTO) {
                        return serializeJointLoanApplicationResponseDTO((JointLoanApplicationResponseDTO) value);
                    } else if (value instanceof JointPartnerDTO) {
                        return serializeJointPartnerDTO((JointPartnerDTO) value);
                    }else if (value instanceof PartnersResponseDTO) {
                        return serializePartnersResponseDTO((PartnersResponseDTO) value);
                    }else if (value instanceof PartnerItemDTO) {
                        return serializePartnerItemDTO((PartnerItemDTO) value);
                    }else if (value instanceof SmartLoanRecommendationResponseDTO) {
                        return serializeSmartLoanRecommendationResponseDTO((SmartLoanRecommendationResponseDTO) value);
                    }else if (value instanceof LoanApprovalResponseDTO) {
                        return serializeLoanApprovalResponseDTO((LoanApprovalResponseDTO) value);
                    }else if (value instanceof LoanDisbursementResponseDTO) {
                        return serializeLoanDisbursementResponseDTO((LoanDisbursementResponseDTO) value);
                    }else if (value instanceof RepaymentScheduleResponseDTO) {
                        return serializeRepaymentScheduleResponseDTO((RepaymentScheduleResponseDTO) value);
                    }else if (value instanceof RepaymentResponseDTO) {
                        return serializeRepaymentResponseDTO((RepaymentResponseDTO) value);
                    } else if (value instanceof dto.farmer.PricePredictionResponseDTO) {
                        return serializePricePredictionResponseDTO((dto.farmer.PricePredictionResponseDTO) value);
                    }else if (value instanceof  PriceCrawlResponseDTO) {
                        return  serializePriceCrawlResponseDTO((PriceCrawlResponseDTO) value);
                    } else if (value instanceof AppointmentCreateResponseDTO) {
                        return serializeAppointmentCreateResponseDTO((AppointmentCreateResponseDTO) value);
                    } else if (value instanceof AppointmentDecisionResponseDTO) {
                        return serializeAppointmentDecisionResponseDTO((AppointmentDecisionResponseDTO) value);
                    }

                    else {
                        return "\"" + escapeJsonString(value.toString()) + "\"";
                    }
                }

                // 序列化List类型
                private String serializeList(List<?> list) {
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        // 特殊处理 BatchActionResultItem
                        if (item instanceof ProductBatchActionResultDTO.BatchActionResultItem) {
                            json.append(serializeBatchActionResultItem((ProductBatchActionResultDTO.BatchActionResultItem) item));
                        } else {
                            json.append(serializeValue(item));
                        }
                        if (i < list.size() - 1) {
                            json.append(",");
                        }
                    }
                    json.append("]");
                    return json.toString();
                }

                // 序列化 RepaymentResponseDTO
                private String serializeRepaymentResponseDTO(RepaymentResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");

                    if (dto.getRepayment_id() != null) {
                        json.append("\"repayment_id\":\"").append(escapeJsonString(dto.getRepayment_id())).append("\",");
                    }

                    if (dto.getLoan_id() != null) {
                        json.append("\"loan_id\":\"").append(escapeJsonString(dto.getLoan_id())).append("\",");
                    }

                    if (dto.getRepayment_amount() != null) {
                        json.append("\"repayment_amount\":").append(dto.getRepayment_amount()).append(",");
                    }

                    if (dto.getPrincipal_amount() != null) {
                        json.append("\"principal_amount\":").append(dto.getPrincipal_amount()).append(",");
                    }

                    if (dto.getInterest_amount() != null) {
                        json.append("\"interest_amount\":").append(dto.getInterest_amount()).append(",");
                    }

                    if (dto.getRemaining_principal() != null) {
                        json.append("\"remaining_principal\":").append(dto.getRemaining_principal()).append(",");
                    }

                    if (dto.getRepayment_method() != null) {
                        json.append("\"repayment_method\":\"").append(escapeJsonString(dto.getRepayment_method())).append("\",");
                    }

                    if (dto.getRepayment_date() != null) {
                        json.append("\"repayment_date\":\"").append(escapeJsonString(dto.getRepayment_date().toString())).append("\",");
                    }

                    if (dto.getNext_payment_date() != null) {
                        json.append("\"next_payment_date\":\"").append(escapeJsonString(dto.getNext_payment_date().toString())).append("\",");
                    }

                    if (dto.getNext_payment_amount() != null) {
                        json.append("\"next_payment_amount\":").append(dto.getNext_payment_amount()).append(",");
                    }

                    if (dto.getLoan_status() != null) {
                        json.append("\"loan_status\":\"").append(escapeJsonString(dto.getLoan_status())).append("\",");
                    }

                    if (dto.getClosed_date() != null) {
                        json.append("\"closed_date\":\"").append(escapeJsonString(dto.getClosed_date().toString())).append("\",");
                    }

                    if (dto.getTotal_interest_saved() != null) {
                        json.append("\"total_interest_saved\":").append(dto.getTotal_interest_saved()).append(",");
                    }

                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }


                // 序列化 RepaymentScheduleResponseDTO
                private String serializeRepaymentScheduleResponseDTO(RepaymentScheduleResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");

                    if (dto.getLoan_id() != null) {
                        json.append("\"loan_id\":\"").append(escapeJsonString(dto.getLoan_id())).append("\",");
                    }

                    if (dto.getLoan_status() != null) {
                        json.append("\"loan_status\":\"").append(escapeJsonString(dto.getLoan_status())).append("\",");
                    }

                    if (dto.getLoan_amount() != null) {
                        json.append("\"loan_amount\":").append(dto.getLoan_amount()).append(",");
                    }

                    if (dto.getInterest_rate() != null) {
                        json.append("\"interest_rate\":").append(dto.getInterest_rate()).append(",");
                    }

                    json.append("\"term_months\":").append(dto.getTerm_months()).append(",");

                    if (dto.getRepayment_method() != null) {
                        json.append("\"repayment_method\":\"").append(escapeJsonString(dto.getRepayment_method())).append("\",");
                    }

                    if (dto.getDisburse_date() != null) {
                        json.append("\"disburse_date\":\"").append(dto.getDisburse_date().toString()).append("\",");
                    } else {
                        json.append("\"disburse_date\":null,");
                    }

                    if (dto.getMaturity_date() != null) {
                        json.append("\"maturity_date\":\"").append(dto.getMaturity_date().toString()).append("\",");
                    } else {
                        json.append("\"maturity_date\":null,");
                    }

                    if (dto.getClosed_date() != null) {
                        json.append("\"closed_date\":\"").append(dto.getClosed_date().toString()).append("\",");
                    } else {
                        json.append("\"closed_date\":null,");
                    }

                    json.append("\"current_period\":").append(dto.getCurrent_period()).append(",");
                    json.append("\"total_periods\":").append(dto.getTotal_periods()).append(",");

                    if (dto.getRemaining_principal() != null) {
                        json.append("\"remaining_principal\":").append(dto.getRemaining_principal()).append(",");
                    }

                    // 序列化 current_due 对象
                    if (dto.getCurrent_due() != null) {
                        json.append("\"current_due\":").append(serializeDueInfo(dto.getCurrent_due())).append(",");
                    } else {
                        json.append("\"current_due\":null,");
                    }

                    // 序列化 next_payment 对象
                    if (dto.getNext_payment() != null) {
                        json.append("\"next_payment\":").append(serializePaymentInfo(dto.getNext_payment())).append(",");
                    } else {
                        json.append("\"next_payment\":null,");
                    }

                    // 序列化 summary 对象
                    if (dto.getSummary() != null) {
                        json.append("\"summary\":").append(serializeSummaryInfo(dto.getSummary())).append(",");
                    }

                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 DueInfo 对象
                private String serializeDueInfo(RepaymentScheduleResponseDTO.DueInfo dueInfo) {
                    if (dueInfo == null) return "null";

                    StringBuilder json = new StringBuilder("{");

                    if (dueInfo.getDue_date() != null) {
                        json.append("\"due_date\":\"").append(dueInfo.getDue_date().toString()).append("\",");
                    }

                    if (dueInfo.getDue_amount() != null) {
                        json.append("\"due_amount\":").append(dueInfo.getDue_amount()).append(",");
                    }

                    if (dueInfo.getPrincipal_amount() != null) {
                        json.append("\"principal_amount\":").append(dueInfo.getPrincipal_amount()).append(",");
                    }

                    if (dueInfo.getInterest_amount() != null) {
                        json.append("\"interest_amount\":").append(dueInfo.getInterest_amount()).append(",");
                    }

                    json.append("\"days_overdue\":").append(dueInfo.getDays_overdue()).append(",");

                    if (dueInfo.getOverdue_interest() != null) {
                        json.append("\"overdue_interest\":").append(dueInfo.getOverdue_interest()).append(",");
                    }

                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PaymentInfo 对象
                private String serializePaymentInfo(RepaymentScheduleResponseDTO.PaymentInfo paymentInfo) {
                    if (paymentInfo == null) return "null";

                    StringBuilder json = new StringBuilder("{");

                    if (paymentInfo.getPayment_date() != null) {
                        json.append("\"payment_date\":\"").append(paymentInfo.getPayment_date().toString()).append("\",");
                    }

                    if (paymentInfo.getPayment_amount() != null) {
                        json.append("\"payment_amount\":").append(paymentInfo.getPayment_amount()).append(",");
                    }

                    if (paymentInfo.getPrincipal_amount() != null) {
                        json.append("\"principal_amount\":").append(paymentInfo.getPrincipal_amount()).append(",");
                    }

                    if (paymentInfo.getInterest_amount() != null) {
                        json.append("\"interest_amount\":").append(paymentInfo.getInterest_amount()).append(",");
                    }

                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 SummaryInfo 对象
                private String serializeSummaryInfo(RepaymentScheduleResponseDTO.SummaryInfo summaryInfo) {
                    if (summaryInfo == null) return "null";

                    StringBuilder json = new StringBuilder("{");

                    if (summaryInfo.getTotal_paid() != null) {
                        json.append("\"total_paid\":").append(summaryInfo.getTotal_paid()).append(",");
                    }

                    if (summaryInfo.getPrincipal_paid() != null) {
                        json.append("\"principal_paid\":").append(summaryInfo.getPrincipal_paid()).append(",");
                    }

                    if (summaryInfo.getInterest_paid() != null) {
                        json.append("\"interest_paid\":").append(summaryInfo.getInterest_paid()).append(",");
                    }

                    if (summaryInfo.getRemaining_total() != null) {
                        json.append("\"remaining_total\":").append(summaryInfo.getRemaining_total()).append(",");
                    }

                    if (summaryInfo.getRemaining_principal() != null) {
                        json.append("\"remaining_principal\":").append(summaryInfo.getRemaining_principal()).append(",");
                    }

                    if (summaryInfo.getRemaining_interest() != null) {
                        json.append("\"remaining_interest\":").append(summaryInfo.getRemaining_interest()).append(",");
                    }

                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }


                // 添加序列化 LoanApprovalResponseDTO
                private String serializeLoanApprovalResponseDTO(LoanApprovalResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getApplication_id() != null) {
                        json.append("\"application_id\":\"").append(escapeJsonString(dto.getApplication_id())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getApproved_amount() != null) {
                        json.append("\"approved_amount\":").append(dto.getApproved_amount()).append(",");
                    }
                    if (dto.getApproved_by() != null) {
                        json.append("\"approved_by\":\"").append(escapeJsonString(dto.getApproved_by())).append("\",");
                    }
                    if (dto.getApproved_at() != null) {
                        json.append("\"approved_at\":\"").append(escapeJsonString(dto.getApproved_at().toString())).append("\",");
                    }
                    if (dto.getNext_step() != null) {
                        json.append("\"next_step\":\"").append(escapeJsonString(dto.getNext_step())).append("\",");
                    }
                    if (dto.getReject_reason() != null) {
                        json.append("\"reject_reason\":\"").append(escapeJsonString(dto.getReject_reason())).append("\",");
                    }
                    if (dto.getRejected_by() != null) {
                        json.append("\"rejected_by\":\"").append(escapeJsonString(dto.getRejected_by())).append("\",");
                    }
                    if (dto.getRejected_at() != null) {
                        json.append("\"rejected_at\":\"").append(escapeJsonString(dto.getRejected_at().toString())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 LoanDisbursementResponseDTO
                private String serializeLoanDisbursementResponseDTO(LoanDisbursementResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getLoan_id() != null) {
                        json.append("\"loan_id\":\"").append(escapeJsonString(dto.getLoan_id())).append("\",");
                    }
                    if (dto.getDisbursement_id() != null) {
                        json.append("\"disbursement_id\":\"").append(escapeJsonString(dto.getDisbursement_id())).append("\",");
                    }
                    if (dto.getApplication_id() != null) {
                        json.append("\"application_id\":\"").append(escapeJsonString(dto.getApplication_id())).append("\",");
                    }
                    if (dto.getDisburse_amount() != null) {
                        json.append("\"disburse_amount\":").append(dto.getDisburse_amount()).append(",");
                    }
                    if (dto.getDisburse_method() != null) {
                        json.append("\"disburse_method\":\"").append(escapeJsonString(dto.getDisburse_method())).append("\",");
                    }
                    if (dto.getDisburse_date() != null) {
                        json.append("\"disburse_date\":\"").append(escapeJsonString(dto.getDisburse_date().toString())).append("\",");
                    }
                    if (dto.getFirst_repayment_date() != null) {
                        json.append("\"first_repayment_date\":\"").append(escapeJsonString(dto.getFirst_repayment_date().toString())).append("\",");
                    }
                    if (dto.getLoan_status() != null) {
                        json.append("\"loan_status\":\"").append(escapeJsonString(dto.getLoan_status())).append("\",");
                    }
                    if (dto.getTotal_repayment_amount() != null) {
                        json.append("\"total_repayment_amount\":").append(dto.getTotal_repayment_amount()).append(",");
                    }
                    if (dto.getMonthly_payment() != null) {
                        json.append("\"monthly_payment\":").append(dto.getMonthly_payment()).append(",");
                    }
                    if (dto.getNext_payment_date() != null) {
                        json.append("\"next_payment_date\":\"").append(escapeJsonString(dto.getNext_payment_date().toString())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PartnersResponseDTO
                private String serializePartnersResponseDTO(PartnersResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"total\":").append(dto.getTotal()).append(",");
                    if (dto.getPartners() != null) {
                        json.append("\"partners\":").append(serializeList(dto.getPartners())).append(",");
                    }
                    if (dto.getRecommendation_reason() != null) {
                        json.append("\"recommendation_reason\":\"").append(escapeJsonString(dto.getRecommendation_reason())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 AuthResponseDTO 对象的方法
                private String serializeAuthResponseDTO(AuthResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getUid() != null) {
                        json.append("\"uid\":\"").append(escapeJsonString(dto.getUid())).append("\",");
                    }
                    if (dto.getNickname() != null) {
                        json.append("\"nickname\":\"").append(escapeJsonString(dto.getNickname())).append("\",");
                    }
                    if (dto.getPhone() != null) {
                        json.append("\"phone\":\"").append(escapeJsonString(dto.getPhone())).append("\",");
                    }
                    if (dto.getUserType() != null) {
                        json.append("\"userType\":\"").append(escapeJsonString(dto.getUserType())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 ProductListResponseDTO 对象的方法
                private String serializeProductListResponseDTO(ProductListResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    json.append("\"price\":").append(dto.getPrice()).append(",");
                    json.append("\"stock\":").append(dto.getStock()).append(",");
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getMain_image_url() != null) {
                        json.append("\"main_image_url\":\"").append(escapeJsonString(dto.getMain_image_url())).append("\",");
                    }
                    if (dto.getDetailed_description() != null) {
                        json.append("\"detailed_description\":\"").append(escapeJsonString(dto.getDetailed_description())).append("\",");
                    }
                    if (dto.getImages() != null) {
                        json.append("\"images\":").append(serializeList(dto.getImages())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 ProductResponseDTO 对象的方法
                private String serializeProductResponseDTO(ProductResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getDetailedDescription() != null) {
                        json.append("\"detailed_description\":\"").append(escapeJsonString(dto.getDetailedDescription())).append("\",");
                    }
                    json.append("\"price\":").append(dto.getPrice()).append(",");
                    json.append("\"stock\":").append(dto.getStock()).append(",");
                    if (dto.getImages() != null) {
                        json.append("\"images\":").append(serializeList(dto.getImages())).append(",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCreated_at() != null) {
                        json.append("\"created_at\":\"").append(dto.getCreated_at().toString()).append("\",");
                    }
                    if (dto.get_links() != null) {
                        // 修复：将Map<String, String>转换为Map<String, Object>
                        Map<String, Object> linksObject = new HashMap<>();
                        for (Map.Entry<String, String> entry : dto.get_links().entrySet()) {
                            linksObject.put(entry.getKey(), entry.getValue());
                        }
                        json.append("\"_links\":").append(toJson(linksObject)).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 ProductStatusUpdateResponseDTO 对象的方法
                private String serializeProductStatusUpdateResponseDTO(ProductStatusUpdateResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                private String serializeSingleLoanApplicationResponseDTO(SingleLoanApplicationResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getLoan_application_id() != null) {
                        json.append("\"loan_application_id\":\"").append(escapeJsonString(dto.getLoan_application_id())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getProduct_name() != null) {
                        json.append("\"product_name\":\"").append(escapeJsonString(dto.getProduct_name())).append("\",");
                    }
                    if (dto.getApply_amount() != null) {
                        json.append("\"apply_amount\":").append(dto.getApply_amount()).append(",");
                    }
                    if (dto.getEstimated_monthly_payment() != null) {
                        json.append("\"estimated_monthly_payment\":").append(dto.getEstimated_monthly_payment()).append(",");
                    }
                    if (dto.getCreated_at() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreated_at().toString())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 JointLoanApplicationResponseDTO
                private String serializeJointLoanApplicationResponseDTO(JointLoanApplicationResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getLoan_application_id() != null) {
                        json.append("\"loan_application_id\":\"").append(escapeJsonString(dto.getLoan_application_id())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getProduct_name() != null) {
                        json.append("\"product_name\":\"").append(escapeJsonString(dto.getProduct_name())).append("\",");
                    }
                    if (dto.getApply_amount() != null) {
                        json.append("\"apply_amount\":").append(dto.getApply_amount()).append(",");
                    }
                    if (dto.getInitiator_phone() != null) {
                        json.append("\"initiator_phone\":\"").append(escapeJsonString(dto.getInitiator_phone())).append("\",");
                    }
                    if (dto.getPartners() != null) {
                        json.append("\"partners\":").append(serializeList(dto.getPartners())).append(",");
                    }
                    if (dto.getCreated_at() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreated_at().toString())).append("\",");
                    }
                    if (dto.getNext_step() != null) {
                        json.append("\"next_step\":\"").append(escapeJsonString(dto.getNext_step())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 JointPartnerDTO
                private String serializeJointPartnerDTO(JointPartnerDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getPhone() != null) {
                        json.append("\"phone\":\"").append(escapeJsonString(dto.getPhone())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getInvited_at() != null) {
                        json.append("\"invited_at\":\"").append(escapeJsonString(dto.getInvited_at().toString())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 ProductDetailResponseDTO 对象的方法
                private String serializeProductDetailResponseDTO(ProductDetailResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getDetailedDescription() != null) {
                        json.append("\"detailed_description\":\"").append(escapeJsonString(dto.getDetailedDescription())).append("\",");
                    }
                    json.append("\"price\":").append(dto.getPrice()).append(",");
                    json.append("\"stock\":").append(dto.getStock()).append(",");
                    if (dto.getDescription() != null) {
                        json.append("\"description\":\"").append(escapeJsonString(dto.getDescription())).append("\",");
                    }
                    if (dto.getImages() != null) {
                        json.append("\"images\":").append(serializeList(dto.getImages())).append(",");
                    }
                    if (dto.getOrigin() != null) {
                        json.append("\"origin\":\"").append(escapeJsonString(dto.getOrigin())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCreated_at() != null) {
                        json.append("\"created_at\":\"").append(dto.getCreated_at().toString()).append("\",");
                    }
                    if (dto.getUpdated_at() != null) {
                        json.append("\"updated_at\":\"").append(dto.getUpdated_at().toString()).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 ProductBatchActionResultDTO 对象的方法
                private String serializeProductBatchActionResultDTO(ProductBatchActionResultDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"success_count\":").append(dto.getSuccess_count()).append(",");
                    json.append("\"failure_count\":").append(dto.getFailure_count()).append(",");
                    if (dto.getResults() != null) {
                        json.append("\"results\":").append(serializeList(dto.getResults()));
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加序列化 BatchActionResultItem 对象的方法
                private String serializeBatchActionResultItem(ProductBatchActionResultDTO.BatchActionResultItem dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    json.append("\"success\":").append(dto.isSuccess()).append(",");
                    if (dto.getMessage() != null) {
                        json.append("\"message\":\"").append(escapeJsonString(dto.getMessage())).append("\",");
                    }
                    if (dto.get_links() != null) {
                        Map<String, Object> linksObject = new HashMap<>();
                        for (Map.Entry<String, String> entry : dto.get_links().entrySet()) {
                            linksObject.put(entry.getKey(), entry.getValue());
                        }
                        json.append("\"_links\":").append(toJson(linksObject)).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                // ============= 社区DTO序列化方法 =============
                
                // 序列化 PublishContentResponseDTO
                private String serializePublishContentResponseDTO(PublishContentResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getContentId() != null) {
                        json.append("\"content_id\":\"").append(escapeJsonString(dto.getContentId())).append("\",");
                    }
                    if (dto.getContentType() != null) {
                        json.append("\"content_type\":\"").append(escapeJsonString(dto.getContentType())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 ContentListItemDTO
                private String serializeContentListItemDTO(ContentListItemDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getContentId() != null) {
                        json.append("\"content_id\":\"").append(escapeJsonString(dto.getContentId())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getContent() != null) {
                        json.append("\"content\":\"").append(escapeJsonString(dto.getContent())).append("\",");
                    }
                    if (dto.getContentType() != null) {
                        json.append("\"content_type\":\"").append(escapeJsonString(dto.getContentType())).append("\",");
                    }
                    if (dto.getImages() != null) {
                        json.append("\"images\":").append(serializeList(dto.getImages())).append(",");
                    }
                    if (dto.getAuthorName() != null) {
                        json.append("\"author_name\":\"").append(escapeJsonString(dto.getAuthorName())).append("\",");
                    }
                    if (dto.getAuthorRole() != null) {
                        json.append("\"author_role\":\"").append(escapeJsonString(dto.getAuthorRole())).append("\",");
                    }
                    json.append("\"view_count\":").append(dto.getViewCount()).append(",");
                    json.append("\"comment_count\":").append(dto.getCommentCount()).append(",");
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 ContentListResponseDTO
                private String serializeContentListResponseDTO(ContentListResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"total\":").append(dto.getTotal()).append(",");
                    if (dto.getList() != null) {
                        json.append("\"list\":").append(serializeList(dto.getList())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 ContentDetailResponseDTO
                private String serializeContentDetailResponseDTO(ContentDetailResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getContentId() != null) {
                        json.append("\"content_id\":\"").append(escapeJsonString(dto.getContentId())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getContent() != null) {
                        json.append("\"content\":\"").append(escapeJsonString(dto.getContent())).append("\",");
                    }
                    if (dto.getContentType() != null) {
                        json.append("\"content_type\":\"").append(escapeJsonString(dto.getContentType())).append("\",");
                    }
                    if (dto.getImages() != null) {
                        json.append("\"images\":").append(serializeList(dto.getImages())).append(",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (dto.getAuthorUserId() != null) {
                        json.append("\"author_user_id\":\"").append(escapeJsonString(dto.getAuthorUserId())).append("\",");
                    }
                    if (dto.getAuthorNickname() != null) {
                        json.append("\"author_nickname\":\"").append(escapeJsonString(dto.getAuthorNickname())).append("\",");
                    }
                    if (dto.getAuthorRole() != null) {
                        json.append("\"author_role\":\"").append(escapeJsonString(dto.getAuthorRole())).append("\",");
                    }
                    json.append("\"view_count\":").append(dto.getViewCount()).append(",");
                    json.append("\"comment_count\":").append(dto.getCommentCount()).append(",");
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PostCommentResponseDTO
                private String serializePostCommentResponseDTO(PostCommentResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getCommentId() != null) {
                        json.append("\"comment_id\":\"").append(escapeJsonString(dto.getCommentId())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PostReplyResponseDTO
                private String serializePostReplyResponseDTO(PostReplyResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getCommentId() != null) {
                        json.append("\"comment_id\":\"").append(escapeJsonString(dto.getCommentId())).append("\",");
                    }
                    if (dto.getParentCommentId() != null) {
                        json.append("\"parent_comment_id\":\"").append(escapeJsonString(dto.getParentCommentId())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PriceCrawlResponseDTO
                private String serializePriceCrawlResponseDTO(PriceCrawlResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getFile_name() != null) {
                        json.append("\"file_name\":\"").append(escapeJsonString(dto.getFile_name())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }


                // 序列化 CommentReplyItemDTO
                private String serializeCommentReplyItemDTO(CommentReplyItemDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getCommentId() != null) {
                        json.append("\"comment_id\":\"").append(escapeJsonString(dto.getCommentId())).append("\",");
                    }
                    if (dto.getAuthorUserId() != null) {
                        json.append("\"author_user_id\":\"").append(escapeJsonString(dto.getAuthorUserId())).append("\",");
                    }
                    if (dto.getAuthorNickname() != null) {
                        json.append("\"author_nickname\":\"").append(escapeJsonString(dto.getAuthorNickname())).append("\",");
                    }
                    if (dto.getAuthorRole() != null) {
                        json.append("\"author_role\":\"").append(escapeJsonString(dto.getAuthorRole())).append("\",");
                    }
                    if (dto.getReplyToUserId() != null) {
                        json.append("\"reply_to_user_id\":\"").append(escapeJsonString(dto.getReplyToUserId())).append("\",");
                    }
                    if (dto.getReplyToNickname() != null) {
                        json.append("\"reply_to_nickname\":\"").append(escapeJsonString(dto.getReplyToNickname())).append("\",");
                    }
                    if (dto.getContent() != null) {
                        json.append("\"content\":\"").append(escapeJsonString(dto.getContent())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 CommentItemDTO
                private String serializeCommentItemDTO(CommentItemDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getCommentId() != null) {
                        json.append("\"comment_id\":\"").append(escapeJsonString(dto.getCommentId())).append("\",");
                    }
                    if (dto.getAuthorUserId() != null) {
                        json.append("\"author_user_id\":\"").append(escapeJsonString(dto.getAuthorUserId())).append("\",");
                    }
                    if (dto.getAuthorNickname() != null) {
                        json.append("\"author_nickname\":\"").append(escapeJsonString(dto.getAuthorNickname())).append("\",");
                    }
                    if (dto.getAuthorRole() != null) {
                        json.append("\"author_role\":\"").append(escapeJsonString(dto.getAuthorRole())).append("\",");
                    }
                    if (dto.getContent() != null) {
                        json.append("\"content\":\"").append(escapeJsonString(dto.getContent())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (dto.getReplies() != null) {
                        json.append("\"replies\":").append(serializeList(dto.getReplies())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 CommentListResponseDTO
                private String serializeCommentListResponseDTO(CommentListResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"total_comments\":").append(dto.getTotalComments()).append(",");
                    if (dto.getList() != null) {
                        json.append("\"list\":").append(serializeList(dto.getList())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // ============= 订单相关DTO序列化方法 =============
                
                // 序列化 CreateOrderResponseDTO
                private String serializeCreateOrderResponseDTO(CreateOrderResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getOrderId() != null) {
                        json.append("\"order_id\":\"").append(escapeJsonString(dto.getOrderId())).append("\",");
                    }
                    if (dto.getProductId() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProductId())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getPrice() != null) {
                        json.append("\"price\":").append(dto.getPrice()).append(",");
                    }
                    if (dto.getQuantity() != null) {
                        json.append("\"quantity\":").append(dto.getQuantity()).append(",");
                    }
                    if (dto.getTotalAmount() != null) {
                        json.append("\"total_amount\":").append(dto.getTotalAmount()).append(",");
                    }
                    if (dto.getBuyerName() != null) {
                        json.append("\"buyer_name\":\"").append(escapeJsonString(dto.getBuyerName())).append("\",");
                    }
                    if (dto.getBuyerAddress() != null) {
                        json.append("\"buyer_address\":\"").append(escapeJsonString(dto.getBuyerAddress())).append("\",");
                    }
                    if (dto.getBuyerPhone() != null) {
                        json.append("\"buyer_phone\":\"").append(escapeJsonString(dto.getBuyerPhone())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (dto.getLinks() != null) {
                        json.append("\"_links\":").append(serializeValue(dto.getLinks())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }
                
                // 序列化 UpdateOrderResponseDTO
                private String serializeUpdateOrderResponseDTO(UpdateOrderResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getOrderId() != null) {
                        json.append("\"order_id\":\"").append(escapeJsonString(dto.getOrderId())).append("\",");
                    }
                    if (dto.getBuyerName() != null) {
                        json.append("\"buyer_name\":\"").append(escapeJsonString(dto.getBuyerName())).append("\",");
                    }
                    if (dto.getBuyerAddress() != null) {
                        json.append("\"buyer_address\":\"").append(escapeJsonString(dto.getBuyerAddress())).append("\",");
                    }
                    if (dto.getBuyerPhone() != null) {
                        json.append("\"buyer_phone\":\"").append(escapeJsonString(dto.getBuyerPhone())).append("\",");
                    }
                    if (dto.getRemark() != null) {
                        json.append("\"remark\":\"").append(escapeJsonString(dto.getRemark())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getUpdatedAt() != null) {
                        json.append("\"updated_at\":\"").append(escapeJsonString(dto.getUpdatedAt())).append("\",");
                    }
                    if (dto.getLinks() != null) {
                        json.append("\"_links\":").append(serializeValue(dto.getLinks())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }
                
                // 序列化 OrderDetailResponseDTO
                private String serializeOrderDetailResponseDTO(OrderDetailResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getOrderId() != null) {
                        json.append("\"order_id\":\"").append(escapeJsonString(dto.getOrderId())).append("\",");
                    }
                    if (dto.getProductId() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProductId())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getSpecification() != null) {
                        json.append("\"specification\":\"").append(escapeJsonString(dto.getSpecification())).append("\",");
                    }
                    if (dto.getPrice() != null) {
                        json.append("\"price\":").append(dto.getPrice()).append(",");
                    }
                    if (dto.getQuantity() != null) {
                        json.append("\"quantity\":").append(dto.getQuantity()).append(",");
                    }
                    if (dto.getTotalAmount() != null) {
                        json.append("\"total_amount\":").append(dto.getTotalAmount()).append(",");
                    }
                    if (dto.getBuyerName() != null) {
                        json.append("\"buyer_name\":\"").append(escapeJsonString(dto.getBuyerName())).append("\",");
                    }
                    if (dto.getBuyerAddress() != null) {
                        json.append("\"buyer_address\":\"").append(escapeJsonString(dto.getBuyerAddress())).append("\",");
                    }
                    if (dto.getBuyerPhone() != null) {
                        json.append("\"buyer_phone\":\"").append(escapeJsonString(dto.getBuyerPhone())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getRemark() != null) {
                        json.append("\"remark\":\"").append(escapeJsonString(dto.getRemark())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (dto.getShippedAt() != null) {
                        json.append("\"shipped_at\":\"").append(escapeJsonString(dto.getShippedAt())).append("\",");
                    } else {
                        json.append("\"shipped_at\":null,");
                    }
                    if (dto.getCompletedAt() != null) {
                        json.append("\"completed_at\":\"").append(escapeJsonString(dto.getCompletedAt())).append("\",");
                    } else {
                        json.append("\"completed_at\":null,");
                    }
                    if (dto.getCancelledAt() != null) {
                        json.append("\"cancelled_at\":\"").append(escapeJsonString(dto.getCancelledAt())).append("\",");
                    } else {
                        json.append("\"cancelled_at\":null,");
                    }
                    if (dto.getRefundedAt() != null) {
                        json.append("\"refunded_at\":\"").append(escapeJsonString(dto.getRefundedAt())).append("\",");
                    } else {
                        json.append("\"refunded_at\":null,");
                    }
                    if (dto.getImages() != null) {
                        json.append("\"images\":").append(serializeList(dto.getImages())).append(",");
                    }
                    if (dto.getLinks() != null) {
                        json.append("\"_links\":").append(serializeValue(dto.getLinks())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }
                
                // 序列化 OrderListItemDTO
                private String serializeOrderListItemDTO(OrderListItemDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getOrderId() != null) {
                        json.append("\"order_id\":\"").append(escapeJsonString(dto.getOrderId())).append("\",");
                    }
                    if (dto.getProductId() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProductId())).append("\",");
                    }
                    if (dto.getTitle() != null) {
                        json.append("\"title\":\"").append(escapeJsonString(dto.getTitle())).append("\",");
                    }
                    if (dto.getQuantity() != null) {
                        json.append("\"quantity\":").append(dto.getQuantity()).append(",");
                    }
                    if (dto.getTotalAmount() != null) {
                        json.append("\"total_amount\":").append(dto.getTotalAmount()).append(",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCreatedAt() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreatedAt())).append("\",");
                    }
                    if (dto.getMainImageUrl() != null) {
                        json.append("\"main_image_url\":\"").append(escapeJsonString(dto.getMainImageUrl())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PartnerItemDTO
                private String serializePartnerItemDTO(PartnerItemDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getPhone() != null) {
                        json.append("\"phone\":\"").append(escapeJsonString(dto.getPhone())).append("\",");
                    }
                    if (dto.getNickname() != null) {
                        json.append("\"nickname\":\"").append(escapeJsonString(dto.getNickname())).append("\",");
                    }
                    if (dto.getAvailable_credit_limit() != null) {
                        json.append("\"available_credit_limit\":").append(dto.getAvailable_credit_limit()).append(",");
                    }
                    if (dto.getTotal_credit_limit() != null) {
                        json.append("\"total_credit_limit\":").append(dto.getTotal_credit_limit()).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 SmartLoanRecommendationResponseDTO
                private String serializeSmartLoanRecommendationResponseDTO(SmartLoanRecommendationResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getRecommendation_type() != null) {
                        json.append("\"recommendation_type\":\"").append(escapeJsonString(dto.getRecommendation_type())).append("\",");
                    }
                    if (dto.getRecommendation_reason() != null) {
                        json.append("\"recommendation_reason\":\"").append(escapeJsonString(dto.getRecommendation_reason())).append("\",");
                    }
                    if (dto.getUser_available_limit() != null) {
                        json.append("\"user_available_limit\":").append(dto.getUser_available_limit()).append(",");
                    }
                    if (dto.getApply_amount() != null) {
                        json.append("\"apply_amount\":").append(dto.getApply_amount()).append(",");
                    }
                    json.append("\"can_apply_single\":").append(dto.isCan_apply_single()).append(",");
                    json.append("\"can_apply_joint\":").append(dto.isCan_apply_joint()).append(",");
                    if (dto.getRecommended_partners() != null) {
                        json.append("\"recommended_partners\":").append(serializeList(dto.getRecommended_partners())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }
                
                // 序列化 OrderListResponseDTO
                private String serializeOrderListResponseDTO(OrderListResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getList() != null) {
                        json.append("\"list\":").append(serializeList(dto.getList())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }
                
                // 序列化 RefundResponseDTO
                private String serializeRefundResponseDTO(RefundResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getOrderId() != null) {
                        json.append("\"order_id\":\"").append(escapeJsonString(dto.getOrderId())).append("\",");
                    }
                    if (dto.getRefundType() != null) {
                        json.append("\"refund_type\":\"").append(escapeJsonString(dto.getRefundType())).append("\",");
                    }
                    if (dto.getRefundAmount() != null) {
                        json.append("\"refund_amount\":").append(dto.getRefundAmount()).append(",");
                    }
                    if (dto.getRefundReason() != null) {
                        json.append("\"refund_reason\":\"").append(escapeJsonString(dto.getRefundReason())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getAppliedAt() != null) {
                        json.append("\"applied_at\":\"").append(escapeJsonString(dto.getAppliedAt())).append("\",");
                    }
                    if (dto.getLinks() != null) {
                        json.append("\"_links\":").append(serializeValue(dto.getLinks())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 BankLoanProductResponseDTO
                private String serializeBankLoanProductResponseDTO(BankLoanProductResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    if (dto.getProduct_code() != null) {
                        json.append("\"product_code\":\"").append(escapeJsonString(dto.getProduct_code())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCreated_at() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreated_at().toString())).append("\",");
                    }
                    if (dto.getCreated_by() != null) {
                        json.append("\"created_by\":\"").append(escapeJsonString(dto.getCreated_by())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 LoanProductsResponseDTO
                private String serializeLoanProductsResponseDTO(LoanProductsResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    json.append("\"total\":").append(dto.getTotal()).append(",");
                    if (dto.getAvailable_products() != null) {
                        json.append("\"available_products\":").append(serializeList(dto.getAvailable_products())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 CreditLimitDTO
                private String serializeCreditLimitDTO(CreditLimitDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getTotal_limit() != null) {
                        json.append("\"total_limit\":").append(dto.getTotal_limit()).append(",");
                    }
                    if (dto.getUsed_limit() != null) {
                        json.append("\"used_limit\":").append(dto.getUsed_limit()).append(",");
                    }
                    if (dto.getAvailable_limit() != null) {
                        json.append("\"available_limit\":").append(dto.getAvailable_limit()).append(",");
                    }
                    if (dto.getCurrency() != null) {
                        json.append("\"currency\":\"").append(escapeJsonString(dto.getCurrency())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getLast_updated() != null) {
                        json.append("\"last_updated\":\"").append(escapeJsonString(dto.getLast_updated().toString())).append("\",");
                    } else {
                        json.append("\"last_updated\":null,");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 CreditApplicationDTO
                private String serializeCreditApplicationDTO(CreditApplicationDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getApplication_id() != null) {
                        json.append("\"application_id\":\"").append(escapeJsonString(dto.getApplication_id())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCreated_at() != null) {
                        json.append("\"created_at\":\"").append(escapeJsonString(dto.getCreated_at().toString())).append("\",");
                    }
                    if (dto.getApply_amount() != null) {
                        json.append("\"apply_amount\":").append(dto.getApply_amount()).append(",");
                    }
                    if (dto.getProof_type() != null) {
                        json.append("\"proof_type\":\"").append(escapeJsonString(dto.getProof_type())).append("\",");
                    }
                    if (dto.getProof_images() != null) {
                        json.append("\"proof_images\":").append(serializeList(dto.getProof_images())).append(",");
                    }
                    if (dto.getDescription() != null) {
                        json.append("\"description\":\"").append(escapeJsonString(dto.getDescription())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 LoanProductDTO
                private String serializeLoanProductDTO(LoanProductDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getProduct_id() != null) {
                        json.append("\"product_id\":\"").append(escapeJsonString(dto.getProduct_id())).append("\",");
                    }
                    if (dto.getProduct_name() != null) {
                        json.append("\"product_name\":\"").append(escapeJsonString(dto.getProduct_name())).append("\",");
                    }
                    if (dto.getProduct_code() != null) {
                        json.append("\"product_code\":\"").append(escapeJsonString(dto.getProduct_code())).append("\",");
                    }
                    if (dto.getMin_credit_limit() != null) {
                        json.append("\"min_credit_limit\":").append(dto.getMin_credit_limit()).append(",");
                    }
                    if (dto.getMax_amount() != null) {
                        json.append("\"max_amount\":").append(dto.getMax_amount()).append(",");
                    }
                    if (dto.getInterest_rate() != null) {
                        json.append("\"interest_rate\":").append(dto.getInterest_rate()).append(",");
                    }
                    if (dto.getTerm_months() != null) {
                        json.append("\"term_months\":").append(dto.getTerm_months()).append(",");
                    }
                    if (dto.getRepayment_method() != null) {
                        json.append("\"repayment_method\":\"").append(escapeJsonString(dto.getRepayment_method())).append("\",");
                    }
                    if (dto.getRepayment_method_name() != null) {
                        json.append("\"repayment_method_name\":\"").append(escapeJsonString(dto.getRepayment_method_name())).append("\",");
                    }
                    if (dto.getDescription() != null) {
                        json.append("\"description\":\"").append(escapeJsonString(dto.getDescription())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCan_apply() != null) {
                        json.append("\"can_apply\":").append(dto.getCan_apply().toString().toLowerCase()).append(",");
                    }
                    if (dto.getReason() != null) {
                        json.append("\"reason\":\"").append(escapeJsonString(dto.getReason())).append("\",");
                    }
                    if (dto.getMax_apply_amount() != null) {
                        json.append("\"max_apply_amount\":").append(dto.getMax_apply_amount()).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }
                
                // 序列化 ConfirmReceiptResponseDTO
                private String serializeConfirmReceiptResponseDTO(ConfirmReceiptResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getOrderId() != null) {
                        json.append("\"order_id\":\"").append(escapeJsonString(dto.getOrderId())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (dto.getCompletedAt() != null) {
                        json.append("\"completed_at\":\"").append(escapeJsonString(dto.getCompletedAt())).append("\",");
                    }
                    if (dto.getLinks() != null) {
                        json.append("\"_links\":").append(serializeValue(dto.getLinks())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 序列化 PricePredictionResponseDTO
                private String serializePricePredictionResponseDTO(PricePredictionResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    
                    if (dto.getHistoricalData() != null) {
                        json.append("\"historical_data\":").append(serializeList(dto.getHistoricalData())).append(",");
                    }
                    
                    if (dto.getPredictedData() != null) {
                        json.append("\"predicted_data\":").append(serializeList(dto.getPredictedData())).append(",");
                    }

                    if (dto.getSeriesData() != null) {
                        json.append("\"series_data\":").append(serializeList(dto.getSeriesData())).append(",");
                    }
                    
                    if (dto.getModelMetrics() != null) {
                        // 将Map<String, Double>转换为Map<String, Object>
                        Map<String, Object> metricsMap = new HashMap<>();
                        for (Map.Entry<String, Double> entry : dto.getModelMetrics().entrySet()) {
                            metricsMap.put(entry.getKey(), entry.getValue());
                        }
                        json.append("\"model_metrics\":").append(toJson(metricsMap)).append(",");
                    }
                    
                    if (dto.getTrend() != null) {
                        json.append("\"trend\":\"").append(escapeJsonString(dto.getTrend())).append("\",");
                    }
                    
                    // 始终包含calculation_details字段，即使为null也序列化为空对象
                    if (dto.getCalculationDetails() != null) {
                        System.out.println("序列化calculationDetails，包含键: " + dto.getCalculationDetails().keySet());
                        System.out.println("calculationDetails大小: " + dto.getCalculationDetails().size());
                        try {
                            String calcDetailsJson = toJson(dto.getCalculationDetails());
                            System.out.println("calculationDetails JSON长度: " + calcDetailsJson.length());
                            System.out.println("calculationDetails JSON前100字符: " + (calcDetailsJson.length() > 100 ? calcDetailsJson.substring(0, 100) : calcDetailsJson));
                            json.append("\"calculation_details\":").append(calcDetailsJson).append(",");
                        } catch (Exception e) {
                            System.err.println("序列化calculationDetails时出错: " + e.getMessage());
                            e.printStackTrace();
                            // 即使出错也添加一个空对象，确保字段存在
                            json.append("\"calculation_details\":{},");
                        }
                    } else {
                        System.out.println("警告: calculationDetails为null，将序列化为空对象");
                        json.append("\"calculation_details\":{},");
                    }
                    
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1); // 删除最后一个逗号
                    }
                    json.append("}");
                    return json.toString();
                }

                private String serializeAppointmentCreateResponseDTO(AppointmentCreateResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getGroup_id() != null) {
                        json.append("\"group_id\":\"").append(escapeJsonString(dto.getGroup_id())).append("\",");
                    }
                    if (dto.getAppointment_ids() != null) {
                        json.append("\"appointment_ids\":").append(serializeList(dto.getAppointment_ids())).append(",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                private String serializeAppointmentDecisionResponseDTO(AppointmentDecisionResponseDTO dto) {
                    StringBuilder json = new StringBuilder("{");
                    if (dto.getAppointment_id() != null) {
                        json.append("\"appointment_id\":\"").append(escapeJsonString(dto.getAppointment_id())).append("\",");
                    }
                    if (dto.getStatus() != null) {
                        json.append("\"status\":\"").append(escapeJsonString(dto.getStatus())).append("\",");
                    }
                    if (json.length() > 1) {
                        json.deleteCharAt(json.length() - 1);
                    }
                    json.append("}");
                    return json.toString();
                }

                // 添加JSON字符串转义方法
                private String escapeJsonString(String str) {
                    if (str == null) return "";
                    return str.replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\b", "\\b")
                            .replace("\f", "\\f")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t");
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
