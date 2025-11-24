// controller/ContentController.java
package controller;

import dto.community.*;
import service.community.ContentService;
import service.community.ContentServiceImpl;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;
import repository.DatabaseManager;

import java.sql.SQLException;
import java.util.*;

public class ContentController {
    private ContentService contentService;

    public ContentController() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        AuthService authService = new AuthServiceImpl();
        this.contentService = new ContentServiceImpl(databaseManager, authService);
    }

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * 发布内容
     * POST /api/v1/content/publish
     */
    public Map<String, Object> publishContent(PublishContentRequestDTO request) {
        System.out.println("ContentController.publishContent - 开始处理发布内容请求");
        Map<String, Object> response = new HashMap<>();

        try {
            PublishContentResponseDTO result = contentService.publishContent(request);
            response.put("code", 201);
            response.put("message", "内容发布成功");
            response.put("data", result);
            System.out.println("ContentController.publishContent - 内容发布成功");
        } catch (IllegalArgumentException e) {
            System.out.println("ContentController.publishContent - 参数验证失败: " + e.getMessage());
            response.put("code", 400);
            response.put("message", "参数验证失败");
            
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", extractFieldName(e.getMessage()));
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (SecurityException e) {
            System.out.println("ContentController.publishContent - 安全异常: " + e.getMessage());
            if (e.getMessage().contains("用户认证失败")) {
                response.put("code", 401);
                response.put("message", e.getMessage());
            } else if (e.getMessage().contains("权限不足")) {
                response.put("code", 403);
                response.put("message", e.getMessage());
            } else {
                response.put("code", 401);
                response.put("message", e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("ContentController.publishContent - 数据库错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("ContentController.publishContent - 未知错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 获取内容列表
     * GET /api/v1/content/list
     */
    public Map<String, Object> getContentList(String contentType, String keyword, String sort) {
        System.out.println("ContentController.getContentList - 开始获取内容列表");
        Map<String, Object> response = new HashMap<>();

        try {
            // 验证sort参数
            if (sort != null && !sort.isEmpty()) {
                if (!sort.equals("newest") && !sort.equals("hottest") && !sort.equals("commented")) {
                    response.put("code", 400);
                    response.put("message", "参数验证失败");
                    
                    List<Map<String, String>> errors = new ArrayList<>();
                    Map<String, String> error = new HashMap<>();
                    error.put("field", "sort");
                    error.put("message", "sort 的值 '" + sort + "' 不被支持");
                    errors.add(error);
                    response.put("errors", errors);
                    return response;
                }
            }

            ContentListResponseDTO result = contentService.getContentList(contentType, keyword, sort);
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", result);
            System.out.println("ContentController.getContentList - 获取内容列表成功");
        } catch (SQLException e) {
            System.out.println("ContentController.getContentList - 数据库错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("ContentController.getContentList - 未知错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 获取内容详情
     * GET /api/v1/content/{content_id}
     */
    public Map<String, Object> getContentDetail(String contentId) {
        System.out.println("ContentController.getContentDetail - 开始获取内容详情: " + contentId);
        Map<String, Object> response = new HashMap<>();

        try {
            if (contentId == null || contentId.trim().isEmpty()) {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "content_id");
                error.put("message", "无效的内容ID格式");
                errors.add(error);
                response.put("errors", errors);
                return response;
            }

            ContentDetailResponseDTO result = contentService.getContentDetail(contentId);
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", result);
            System.out.println("ContentController.getContentDetail - 获取内容详情成功");
        } catch (IllegalArgumentException e) {
            System.out.println("ContentController.getContentDetail - 内容不存在: " + e.getMessage());
            response.put("code", 404);
            response.put("message", "内容不存在或已被删除");
        } catch (SQLException e) {
            System.out.println("ContentController.getContentDetail - 数据库错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("ContentController.getContentDetail - 未知错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 从错误消息中提取字段名
     */
    private String extractFieldName(String errorMessage) {
        if (errorMessage.contains("标题")) return "title";
        if (errorMessage.contains("内容")) return "content";
        if (errorMessage.contains("类型")) return "content_type";
        if (errorMessage.contains("手机号")) return "phone";
        return "unknown";
    }
}

