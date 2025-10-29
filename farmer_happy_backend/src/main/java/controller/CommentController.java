// controller/CommentController.java
package controller;

import dto.community.*;
import service.community.CommentService;
import service.community.CommentServiceImpl;
import service.community.ContentService;
import service.community.ContentServiceImpl;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;
import repository.DatabaseManager;

import java.sql.SQLException;
import java.util.*;

public class CommentController {
    private CommentService commentService;

    public CommentController() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        AuthService authService = new AuthServiceImpl();
        ContentService contentService = new ContentServiceImpl(databaseManager, authService);
        this.commentService = new CommentServiceImpl(databaseManager, authService, contentService);
    }

    /**
     * 对帖子发表评论
     * POST /api/v1/content/{content_id}/comments
     */
    public Map<String, Object> postComment(String contentId, PostCommentRequestDTO request) {
        System.out.println("CommentController.postComment - 开始处理评论请求");
        Map<String, Object> response = new HashMap<>();

        try {
            PostCommentResponseDTO result = commentService.postComment(contentId, request);
            response.put("code", 201);
            response.put("message", "评论成功");
            response.put("data", result);
            System.out.println("CommentController.postComment - 评论发布成功");
        } catch (IllegalArgumentException e) {
            System.out.println("CommentController.postComment - 参数验证失败: " + e.getMessage());
            if (e.getMessage().contains("帖子不存在")) {
                response.put("code", 404);
                response.put("message", e.getMessage());
            } else {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", extractFieldName(e.getMessage()));
                error.put("message", e.getMessage());
                errors.add(error);
                response.put("errors", errors);
            }
        } catch (SecurityException e) {
            System.out.println("CommentController.postComment - 用户认证失败: " + e.getMessage());
            response.put("code", 401);
            response.put("message", e.getMessage());
        } catch (SQLException e) {
            System.out.println("CommentController.postComment - 数据库错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("CommentController.postComment - 未知错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 回复评论
     * POST /api/v1/comment/{comment_id}/replies
     */
    public Map<String, Object> postReply(String commentId, PostReplyRequestDTO request) {
        System.out.println("CommentController.postReply - 开始处理回复请求");
        Map<String, Object> response = new HashMap<>();

        try {
            PostReplyResponseDTO result = commentService.postReply(commentId, request);
            response.put("code", 201);
            response.put("message", "回复评论成功");
            response.put("data", result);
            System.out.println("CommentController.postReply - 回复发布成功");
        } catch (IllegalArgumentException e) {
            System.out.println("CommentController.postReply - 参数验证失败: " + e.getMessage());
            if (e.getMessage().contains("评论不存在")) {
                response.put("code", 404);
                response.put("message", "回复失败，目标评论不存在或已被删除");
            } else {
                response.put("code", 400);
                response.put("message", "参数验证失败");
                
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", extractFieldName(e.getMessage()));
                error.put("message", e.getMessage());
                errors.add(error);
                response.put("errors", errors);
            }
        } catch (SecurityException e) {
            System.out.println("CommentController.postReply - 用户认证失败: " + e.getMessage());
            response.put("code", 401);
            response.put("message", e.getMessage());
        } catch (SQLException e) {
            System.out.println("CommentController.postReply - 数据库错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("CommentController.postReply - 未知错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 获取评论列表
     * GET /api/v1/content/{content_id}/comments
     */
    public Map<String, Object> getCommentList(String contentId) {
        System.out.println("CommentController.getCommentList - 开始获取评论列表");
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

            CommentListResponseDTO result = commentService.getCommentList(contentId);
            response.put("code", 200);
            response.put("message", "获取评论列表成功");
            response.put("data", result);
            System.out.println("CommentController.getCommentList - 获取评论列表成功");
        } catch (SQLException e) {
            System.out.println("CommentController.getCommentList - 数据库错误: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "服务器内部错误");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("CommentController.getCommentList - 未知错误: " + e.getMessage());
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
        if (errorMessage.contains("评论内容")) return "comment";
        if (errorMessage.contains("手机号")) return "phone";
        return "unknown";
    }
}

