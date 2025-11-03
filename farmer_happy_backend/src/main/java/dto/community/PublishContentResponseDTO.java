// dto/community/PublishContentResponseDTO.java
package dto.community;

public class PublishContentResponseDTO {
    private String contentId;
    private String contentType;
    private String createdAt;

    public PublishContentResponseDTO() {
    }

    public PublishContentResponseDTO(String contentId, String contentType, String createdAt) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

