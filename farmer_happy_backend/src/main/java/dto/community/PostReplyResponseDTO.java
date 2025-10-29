// dto/community/PostReplyResponseDTO.java
package dto.community;

public class PostReplyResponseDTO {
    private String commentId;
    private String parentCommentId;
    private String createdAt;

    public PostReplyResponseDTO() {
    }

    public PostReplyResponseDTO(String commentId, String parentCommentId, String createdAt) {
        this.commentId = commentId;
        this.parentCommentId = parentCommentId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

