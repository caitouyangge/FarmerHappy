// service/community/ContentServiceImpl.java
package service.community;

import dto.community.*;
import entity.Content;
import entity.User;
import repository.DatabaseManager;
import service.auth.AuthService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ContentServiceImpl implements ContentService {
    private final DatabaseManager databaseManager;
    private final AuthService authService;

    public ContentServiceImpl(DatabaseManager databaseManager, AuthService authService) {
        this.databaseManager = databaseManager;
        this.authService = authService;
    }

    @Override
    public PublishContentResponseDTO publishContent(PublishContentRequestDTO request) throws SQLException, IllegalArgumentException {
        // 1. 参数验证
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        if (request.getContentType() == null || request.getContentType().trim().isEmpty()) {
            throw new IllegalArgumentException("内容类型不能为空");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        // 2. 验证用户是否存在
        User user = authService.findUserByPhone(request.getPhone());
        if (user == null) {
            throw new SecurityException("用户认证失败，请检查手机号或重新登录");
        }

        // 3. 获取用户角色（支持多重身份）
        List<String> userRoles = databaseManager.getUserRole(user.getUid());
        if (userRoles == null || userRoles.isEmpty()) {
            throw new SecurityException("无法获取用户角色信息");
        }

        // 4. 验证权限（多重身份支持）
        if (!validatePermissionMultiRole(userRoles, request.getContentType())) {
            String errorMsg = "权限不足";
            if (userRoles.contains("farmer") && request.getContentType().equals("articles")) {
                errorMsg = "权限不足，农户角色不能发布技术文章";
            } else if (userRoles.contains("expert") && !request.getContentType().equals("articles")) {
                errorMsg = "权限不足，专家角色只能发布技术文章";
            }
            throw new SecurityException(errorMsg);
        }

        // 5. 选择最适合的角色用于创建内容
        String selectedRole = selectBestRoleForContent(userRoles, request.getContentType());

        // 6. 创建内容对象
        Content content = new Content(
            request.getTitle(),
            request.getContent(),
            request.getContentType(),
            request.getImages(),
            user.getUid(),
            user.getNickname(),
            selectedRole
        );

        // 7. 保存到数据库
        saveContent(content);

        // 7. 构建响应
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        return new PublishContentResponseDTO(
            content.getContentId(),
            content.getContentType(),
            content.getCreatedAt().format(formatter)
        );
    }

    @Override
    public ContentListResponseDTO getContentList(String contentType, String keyword, String sort) throws SQLException {
        // 获取内容列表
        List<Content> contents = databaseManager.findContents(contentType, keyword, sort);
        
        // 转换为DTO
        List<ContentListItemDTO> items = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        
        for (Content content : contents) {
            ContentListItemDTO item = new ContentListItemDTO();
            item.setContentId(content.getContentId());
            item.setTitle(content.getTitle());
            
            // 内容摘要（前100个字符）
            String contentText = content.getContent();
            if (contentText.length() > 100) {
                contentText = contentText.substring(0, 100) + "...";
            }
            item.setContent(contentText);
            
            item.setContentType(content.getContentType());
            item.setImages(content.getImages());
            item.setAuthorName(content.getAuthorNickname());
            item.setAuthorRole(content.getAuthorRole());
            item.setViewCount(content.getViewCount());
            item.setCommentCount(content.getCommentCount());
            item.setCreatedAt(content.getCreatedAt().format(formatter));
            
            items.add(item);
        }
        
        return new ContentListResponseDTO(items.size(), items);
    }

    @Override
    public ContentDetailResponseDTO getContentDetail(String contentId) throws SQLException {
        // 查找内容
        Content content = findContentById(contentId);
        if (content == null) {
            throw new IllegalArgumentException("内容不存在或已被删除");
        }
        
        // 增加浏览量
        databaseManager.incrementViewCount(contentId);
        content.setViewCount(content.getViewCount() + 1);
        
        // 转换为DTO
        ContentDetailResponseDTO dto = new ContentDetailResponseDTO();
        dto.setContentId(content.getContentId());
        dto.setTitle(content.getTitle());
        dto.setContent(content.getContent());
        dto.setContentType(content.getContentType());
        dto.setImages(content.getImages());
        dto.setAuthorUserId(content.getAuthorUserId());
        dto.setAuthorNickname(content.getAuthorNickname());
        dto.setAuthorRole(content.getAuthorRole());
        dto.setViewCount(content.getViewCount());
        dto.setCommentCount(content.getCommentCount());
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        dto.setCreatedAt(content.getCreatedAt().format(formatter));
        
        return dto;
    }

    @Override
    public boolean validatePermission(String userRole, String contentType) {
        // 专家只能发布技术文章
        if ("expert".equals(userRole)) {
            return "articles".equals(contentType);
        }
        
        // 农户可以发布问题和经验分享，但不能发布技术文章
        if ("farmer".equals(userRole)) {
            return "questions".equals(contentType) || "experiences".equals(contentType);
        }
        
        return false;
    }

    /**
     * 支持多重身份的权限验证
     */
    public boolean validatePermissionMultiRole(List<String> userRoles, String contentType) {
        // 如果用户有多重身份，只要其中一个身份有权限就可以发布
        for (String role : userRoles) {
            if (validatePermission(role, contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据内容类型确定最适合的用户角色
     */
    public String selectBestRoleForContent(List<String> userRoles, String contentType) {
        // 优先级：专家发布技术文章，农户发布问题和经验
        if ("articles".equals(contentType) && userRoles.contains("expert")) {
            return "expert";
        }
        if (("questions".equals(contentType) || "experiences".equals(contentType)) 
            && userRoles.contains("farmer")) {
            return "farmer";
        }
        
        // 如果没有完美匹配，返回第一个有权限的角色
        for (String role : userRoles) {
            if (validatePermission(role, contentType)) {
                return role;
            }
        }
        
        // 如果都没有权限，返回第一个角色用于错误提示
        return userRoles.isEmpty() ? null : userRoles.get(0);
    }

    @Override
    public void incrementCommentCount(String contentId) throws SQLException {
        databaseManager.incrementCommentCount(contentId);
    }

    @Override
    public void saveContent(Content content) throws SQLException {
        databaseManager.saveContent(content);
    }

    @Override
    public Content findContentById(String contentId) throws SQLException {
        return databaseManager.findContentById(contentId);
    }
}

