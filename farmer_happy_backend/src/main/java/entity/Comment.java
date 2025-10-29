// entity/Comment.java
package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Comment {
    private String commentId;
    private String contentId; // 所属帖子ID
    private String parentCommentId; // 父评论ID，null表示一级评论
    private String authorUserId;
    private String authorNickname;
    private String authorRole; // farmer, expert
    private String replyToUserId; // 回复的用户ID（用于二级评论）
    private String replyToNickname; // 回复的用户昵称（用于二级评论）
    private String content; // 评论内容
    private LocalDateTime createdAt;

    // Constructors
    public Comment() {
        this.createdAt = LocalDateTime.now();
    }

    public Comment(String contentId, String parentCommentId, String authorUserId, 
                   String authorNickname, String authorRole, String content,
                   String replyToUserId, String replyToNickname) {
        this();
        this.contentId = contentId;
        this.parentCommentId = parentCommentId;
        this.authorUserId = authorUserId;
        this.authorNickname = authorNickname;
        this.authorRole = authorRole;
        this.content = content;
        this.replyToUserId = replyToUserId;
        this.replyToNickname = replyToNickname;
        this.commentId = generateCommentId();
    }

    // 生成评论ID: CMT + 时间戳
    private String generateCommentId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "CMT" + LocalDateTime.now().format(formatter);
    }

    // 判断是否为一级评论
    public boolean isTopLevelComment() {
        return parentCommentId == null || parentCommentId.isEmpty();
    }

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public String getReplyToUserId() {
        return replyToUserId;
    }

    public void setReplyToUserId(String replyToUserId) {
        this.replyToUserId = replyToUserId;
    }

    public String getReplyToNickname() {
        return replyToNickname;
    }

    public void setReplyToNickname(String replyToNickname) {
        this.replyToNickname = replyToNickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

