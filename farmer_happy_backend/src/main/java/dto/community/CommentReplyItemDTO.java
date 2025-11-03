// dto/community/CommentReplyItemDTO.java
package dto.community;

public class CommentReplyItemDTO {
    private String commentId;
    private String authorUserId;
    private String authorNickname;
    private String authorRole;
    private String replyToUserId;
    private String replyToNickname;
    private String content;
    private String createdAt;

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

