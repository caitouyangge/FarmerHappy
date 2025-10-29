// dto/community/CommentListResponseDTO.java
package dto.community;

import java.util.List;

public class CommentListResponseDTO {
    private int totalComments;
    private List<CommentItemDTO> list;

    public CommentListResponseDTO() {
    }

    public CommentListResponseDTO(int totalComments, List<CommentItemDTO> list) {
        this.totalComments = totalComments;
        this.list = list;
    }

    // Getters and Setters
    public int getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(int totalComments) {
        this.totalComments = totalComments;
    }

    public List<CommentItemDTO> getList() {
        return list;
    }

    public void setList(List<CommentItemDTO> list) {
        this.list = list;
    }
}

