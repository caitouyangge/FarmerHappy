// dto/community/ContentListResponseDTO.java
package dto.community;

import java.util.List;

public class ContentListResponseDTO {
    private int total;
    private List<ContentListItemDTO> list;

    public ContentListResponseDTO() {
    }

    public ContentListResponseDTO(int total, List<ContentListItemDTO> list) {
        this.total = total;
        this.list = list;
    }

    // Getters and Setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<ContentListItemDTO> getList() {
        return list;
    }

    public void setList(List<ContentListItemDTO> list) {
        this.list = list;
    }
}

