// service/community/CommentServiceImpl.java
package service.community;

import dto.community.*;
import entity.Comment;
import entity.Content;
import entity.User;
import repository.DatabaseManager;
import service.auth.AuthService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentServiceImpl implements CommentService {
    private final DatabaseManager databaseManager;
    private final AuthService authService;
    private final ContentService contentService;

    public CommentServiceImpl(DatabaseManager databaseManager, AuthService authService, ContentService contentService) {
        this.databaseManager = databaseManager;
        this.authService = authService;
        this.contentService = contentService;
    }

    @Override
    public PostCommentResponseDTO postComment(String contentId, PostCommentRequestDTO request) throws SQLException, IllegalArgumentException {
        // 1. 参数验证
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        // 2. 验证用户是否存在
        User user = authService.findUserByPhone(request.getPhone());
        if (user == null) {
            throw new SecurityException("用户认证失败，请检查手机号或重新登录");
        }

        // 3. 验证帖子是否存在
        Content content = contentService.findContentById(contentId);
        if (content == null) {
            throw new IllegalArgumentException("评论失败，目标帖子不存在或已被删除");
        }

        // 4. 获取用户角色（支持多重身份）
        List<String> userRoles = databaseManager.getUserRole(user.getUid());
        
        // 5. 选择主要角色用于显示（优先显示专家身份）
        String displayRole = selectPrimaryRole(userRoles);

        // 6. 创建评论对象（一级评论，parentCommentId为null）
        Comment comment = new Comment(
            contentId,
            null, // 一级评论
            user.getUid(),
            user.getNickname(),
            displayRole,
            request.getComment(),
            null, // 一级评论不需要replyToUserId
            null  // 一级评论不需要replyToNickname
        );

        // 6. 保存评论
        saveComment(comment);

        // 7. 增加帖子的评论数
        contentService.incrementCommentCount(contentId);

        // 8. 构建响应
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return new PostCommentResponseDTO(
            comment.getCommentId(),
            comment.getCreatedAt().format(formatter)
        );
    }

    @Override
    public PostReplyResponseDTO postReply(String commentId, PostReplyRequestDTO request) throws SQLException, IllegalArgumentException {
        // 1. 参数验证
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        // 2. 验证用户是否存在
        User user = authService.findUserByPhone(request.getPhone());
        if (user == null) {
            throw new SecurityException("用户认证失败，请检查手机号或重新登录");
        }

        // 3. 验证父评论是否存在
        Comment parentComment = findCommentById(commentId);
        if (parentComment == null) {
            throw new IllegalArgumentException("回复失败，目标评论不存在或已被删除");
        }

        // 4. 获取用户角色（支持多重身份）
        List<String> userRoles = databaseManager.getUserRole(user.getUid());
        
        // 5. 选择主要角色用于显示（优先显示专家身份）
        String displayRole = selectPrimaryRole(userRoles);

        // 5. 确定真正的父评论ID（如果当前评论是二级评论，需要找到一级评论）
        String topLevelCommentId = parentComment.isTopLevelComment() ? 
            parentComment.getCommentId() : parentComment.getParentCommentId();

        // 6. 创建回复评论对象（二级评论）
        Comment reply = new Comment(
            parentComment.getContentId(),
            topLevelCommentId, // 保存一级评论ID
            user.getUid(),
            user.getNickname(),
            displayRole,
            request.getComment(),
            parentComment.getAuthorUserId(), // 被回复的用户ID
            parentComment.getAuthorNickname() // 被回复的用户昵称
        );

        // 7. 保存回复
        saveComment(reply);

        // 8. 增加帖子的评论数
        contentService.incrementCommentCount(parentComment.getContentId());

        // 9. 构建响应
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return new PostReplyResponseDTO(
            reply.getCommentId(),
            topLevelCommentId,
            reply.getCreatedAt().format(formatter)
        );
    }

    @Override
    public CommentListResponseDTO getCommentList(String contentId) throws SQLException {
        // 1. 获取该帖子的所有评论
        List<Comment> allComments = databaseManager.findCommentsByContentId(contentId);
        
        // 2. 分离一级评论和二级回复
        List<Comment> topLevelComments = new ArrayList<>();
        Map<String, List<Comment>> repliesMap = new HashMap<>();
        
        for (Comment comment : allComments) {
            if (comment.isTopLevelComment()) {
                topLevelComments.add(comment);
                // 初始化该一级评论的回复列表
                repliesMap.put(comment.getCommentId(), new ArrayList<>());
            } else {
                // 将二级回复添加到对应的一级评论下
                String parentId = comment.getParentCommentId();
                repliesMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(comment);
            }
        }
        
        // 3. 构建响应DTO
        List<CommentItemDTO> commentItems = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        
        for (Comment topComment : topLevelComments) {
            CommentItemDTO itemDTO = new CommentItemDTO();
            itemDTO.setCommentId(topComment.getCommentId());
            itemDTO.setAuthorUserId(topComment.getAuthorUserId());
            itemDTO.setAuthorNickname(topComment.getAuthorNickname());
            itemDTO.setAuthorRole(topComment.getAuthorRole());
            itemDTO.setContent(topComment.getContent());
            itemDTO.setCreatedAt(topComment.getCreatedAt().format(formatter));
            
            // 添加该评论的所有回复
            List<CommentReplyItemDTO> replyDTOs = new ArrayList<>();
            List<Comment> replies = repliesMap.get(topComment.getCommentId());
            if (replies != null) {
                for (Comment reply : replies) {
                    CommentReplyItemDTO replyDTO = new CommentReplyItemDTO();
                    replyDTO.setCommentId(reply.getCommentId());
                    replyDTO.setAuthorUserId(reply.getAuthorUserId());
                    replyDTO.setAuthorNickname(reply.getAuthorNickname());
                    replyDTO.setAuthorRole(reply.getAuthorRole());
                    replyDTO.setReplyToUserId(reply.getReplyToUserId());
                    replyDTO.setReplyToNickname(reply.getReplyToNickname());
                    replyDTO.setContent(reply.getContent());
                    replyDTO.setCreatedAt(reply.getCreatedAt().format(formatter));
                    replyDTOs.add(replyDTO);
                }
            }
            itemDTO.setReplies(replyDTOs);
            
            commentItems.add(itemDTO);
        }
        
        return new CommentListResponseDTO(allComments.size(), commentItems);
    }

    @Override
    public void saveComment(Comment comment) throws SQLException {
        databaseManager.saveComment(comment);
    }

    @Override
    public Comment findCommentById(String commentId) throws SQLException {
        return databaseManager.findCommentById(commentId);
    }
    
    /**
     * 从多重身份中选择主要角色用于显示
     * 优先级：expert > farmer > buyer > bank
     */
    private String selectPrimaryRole(List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return "user"; // 默认角色
        }
        
        // 按优先级返回角色
        if (userRoles.contains("expert")) {
            return "expert";
        }
        if (userRoles.contains("farmer")) {
            return "farmer";
        }
        if (userRoles.contains("buyer")) {
            return "buyer";
        }
        if (userRoles.contains("bank")) {
            return "bank";
        }
        
        // 如果都不匹配，返回第一个角色
        return userRoles.get(0);
    }
}

