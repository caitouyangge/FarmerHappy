// dto/community/PostReplyRequestDTO.java
package dto.community;

public class PostReplyRequestDTO {
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

