// service/community/ContentService.java
package service.community;

import dto.community.*;
import entity.Content;
import java.sql.SQLException;

public interface ContentService {
    /**
     * 发布内容
     */
    PublishContentResponseDTO publishContent(PublishContentRequestDTO request) throws SQLException, IllegalArgumentException;
    
    /**
     * 获取内容列表
     * @param contentType 内容类型（可选）
     * @param keyword 搜索关键词（可选）
     * @param sort 排序方式（newest, hottest, commented）
     */
    ContentListResponseDTO getContentList(String contentType, String keyword, String sort) throws SQLException;
    
    /**
     * 获取内容详情（同时增加浏览量）
     */
    ContentDetailResponseDTO getContentDetail(String contentId) throws SQLException;
    
    /**
     * 验证用户权限
     * @param userRole 用户角色
     * @param contentType 内容类型
     * @return 是否有权限
     */
    boolean validatePermission(String userRole, String contentType);
    
    /**
     * 增加评论数
     */
    void incrementCommentCount(String contentId) throws SQLException;
    
    /**
     * 保存内容
     */
    void saveContent(Content content) throws SQLException;
    
    /**
     * 根据ID查找内容
     */
    Content findContentById(String contentId) throws SQLException;
}

