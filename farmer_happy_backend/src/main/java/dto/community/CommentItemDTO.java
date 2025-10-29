// dto/community/CommentItemDTO.java
package dto.community;

import java.util.List;

public class CommentItemDTO {
    private String commentId;
    private String authorUserId;
    private String authorNickname;
    private String authorRole;
    private String content;
    private String createdAt;
    private List<CommentReplyItemDTO> replies;

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

    public List<CommentReplyItemDTO> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentReplyItemDTO> replies) {
        this.replies = replies;
    }
}

