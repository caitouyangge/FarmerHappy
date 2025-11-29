// src/main/java/controller/AiController.java
package controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AiController {

    // 注意：真实环境中不建议把密钥写死在代码里，这里按题目要求直接使用
    private static final String API_KEY = "sk-QfccpUybEFZ3iGB9rzzukWekBgb0fkaS8Skcy4tyuM8TY5Yf";
    private static final String BASE_URL = "https://chatapi.zjt66.top/v1";

    /**
     * 与 AI 农业专家对话
     * POST /api/v1/ai/expert-chat
     *
     * 请求体示例：
     * {
     *   "question": "请问如何防治水稻稻瘟病？"
     * }
     */
    public Map<String, Object> chatWithAiExpert(String question) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (question == null || question.trim().isEmpty()) {
                response.put("code", 400);
                response.put("message", "问题内容不能为空");
                return response;
            }

            String url = BASE_URL + "/chat/completions";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

            // 构造与 OpenAI 兼容的聊天请求体
            // 这里直接手动拼接 JSON，避免引入第三方库
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("{");
            bodyBuilder.append("\"model\":\"gpt-4o-mini\",");
            bodyBuilder.append("\"temperature\":0.7,");
            bodyBuilder.append("\"messages\":[");
            bodyBuilder.append("{\"role\":\"system\",\"content\":\"你是一名资深农业专家，熟悉种植管理、病虫害防治、农业气象、农资使用等，使用简体中文回答农户的问题，语言通俗易懂，给出可操作的建议。\"},");
            bodyBuilder.append("{\"role\":\"user\",\"content\":");
            bodyBuilder.append("\"").append(escapeJson(question)).append("\"");
            bodyBuilder.append("}");
            bodyBuilder.append("]");
            bodyBuilder.append("}");

            String requestBody = bodyBuilder.toString();
            System.out.println("AI 请求体: " + requestBody);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int statusCode = conn.getResponseCode();
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
            System.out.println("AI 原始响应: " + apiResp);

            if (statusCode >= 200 && statusCode < 300) {
                String answer = extractFirstMessageContent(apiResp);

                Map<String, Object> data = new HashMap<>();
                data.put("answer", answer);
                data.put("raw_response", apiResp);

                response.put("code", 200);
                response.put("message", "success");
                response.put("data", data);
            } else {
                // 解析错误信息
                String errorMessage = extractErrorMessage(apiResp);
                
                // 针对429错误（负载饱和）提供更友好的提示
                if (statusCode == 429) {
                    response.put("code", 429);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        response.put("message", errorMessage);
                    } else {
                        response.put("message", "AI服务当前负载较高，请稍后再试");
                    }
                    response.put("error_type", "rate_limit");
                } else if (statusCode == 400 || statusCode == 404) {
                    // 400/404可能是模型不存在或其他参数错误
                    response.put("code", 400);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        response.put("message", errorMessage);
                    } else {
                        response.put("message", "AI服务请求参数错误，请检查模型配置");
                    }
                    response.put("error_type", "bad_request");
                } else {
                    // 其他错误
                    response.put("code", 502);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        response.put("message", errorMessage);
                    } else {
                        response.put("message", "调用AI服务失败，HTTP状态码: " + statusCode);
                    }
                    response.put("error_type", "server_error");
                }
                response.put("error_detail", apiResp);
                response.put("http_status", statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }

        return response;
    }

    /**
     * 简单提取 OpenAI ChatCompletions 响应中的第一条 message.content
     * 由于项目没有引入 JSON 库，这里使用非常简单的字符串查找方式，
     * 在大多数正常响应场景下可以工作。
     */
    private String extractFirstMessageContent(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        try {
            int contentIndex = json.indexOf("\"content\"");
            if (contentIndex == -1) {
                return json; // 如果找不到字段，直接返回原始响应
            }

            int firstQuote = json.indexOf('"', contentIndex + 9); // 跳过 "content"
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
     * 从错误响应中提取错误消息
     */
    private String extractErrorMessage(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            // 查找 "message" 字段
            int messageIndex = json.indexOf("\"message\"");
            if (messageIndex == -1) {
                return null;
            }
            
            // 找到 message 后面的冒号和引号
            int colonIndex = json.indexOf(':', messageIndex);
            if (colonIndex == -1) {
                return null;
            }
            
            // 找到第一个引号（可能是转义的）
            int firstQuote = -1;
            for (int i = colonIndex + 1; i < json.length(); i++) {
                if (json.charAt(i) == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    firstQuote = i;
                    break;
                }
            }
            
            if (firstQuote == -1) {
                return null;
            }
            
            // 提取消息内容
            StringBuilder sb = new StringBuilder();
            boolean escape = false;
            for (int i = firstQuote + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escape) {
                    if (c == 'n') {
                        sb.append('\n');
                    } else if (c == 'r') {
                        sb.append('\r');
                    } else if (c == 't') {
                        sb.append('\t');
                    } else if (c == '"') {
                        sb.append('"');
                    } else if (c == '\\') {
                        sb.append('\\');
                    } else {
                        sb.append(c);
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
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对简单字符串进行 JSON 转义，避免请求体格式错误
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


