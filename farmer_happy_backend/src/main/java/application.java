// src/application.java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import config.RouterConfig;
import dto.auth.AuthResponseDTO;
import dto.farmer.ProductListResponseDTO;
import dto.farmer.ProductResponseDTO;
import dto.farmer.ProductStatusUpdateResponseDTO;
import dto.farmer.ProductDetailResponseDTO;
import dto.farmer.ProductBatchActionResultDTO;
import dto.community.*;
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

                        // 解析请求体
                        Map<String, Object> requestBody = parseRequestBody(exchange);

                        // 解析请求头
                        Map<String, String> headers = new HashMap<>();
                        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                            if (!entry.getValue().isEmpty()) {
                                headers.put(entry.getKey(), entry.getValue().get(0));
                            }
                        }

                        // 解析URL查询参数
                        Map<String, String> queryParams = parseQueryParams(query);

                        // 处理请求并获取响应（传递查询参数）
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
                    StringBuilder json = new StringBuilder("{");
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        json.append("\"").append(entry.getKey()).append("\":");
                        json.append(serializeValue(entry.getValue()));
                        json.append(",");
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
                        return toJson((Map<String, Object>) value);
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
                    } else {
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
                    if (dto.getShipping_template_id() != null) {
                        json.append("\"shipping_template_id\":\"").append(escapeJsonString(dto.getShipping_template_id())).append("\",");
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
