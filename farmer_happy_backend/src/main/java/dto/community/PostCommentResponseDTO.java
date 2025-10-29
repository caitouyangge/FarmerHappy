// dto/community/PostCommentResponseDTO.java
package dto.community;

public class PostCommentResponseDTO {
    private String commentId;
    private String createdAt;

    public PostCommentResponseDTO() {
    }

    public PostCommentResponseDTO(String commentId, String createdAt) {
        this.commentId = commentId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

