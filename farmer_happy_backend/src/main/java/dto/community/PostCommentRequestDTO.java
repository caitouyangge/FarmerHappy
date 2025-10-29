// dto/community/PostCommentRequestDTO.java
package dto.community;

public class PostCommentRequestDTO {
    private String comment;
    private String phone; // 用户手机号

    // Getters and Setters
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

