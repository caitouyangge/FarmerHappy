package controller;

import dto.community.PublishContentRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.community.ContentService;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentControllerIT {

    @Mock
    ContentService contentService;

    private ContentController controller;

    @BeforeEach
    void setUp() {
        controller = new ContentController(contentService);
    }

    @Test
    void get_content_list_invalid_sort_returns_400() {
        Map<String, Object> resp = controller.getContentList("articles", null, "invalid");
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void publish_content_param_error_returns_400() throws Exception {
        when(contentService.publishContent(any(PublishContentRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("类型不正确"));
        PublishContentRequestDTO req = new PublishContentRequestDTO();
        Map<String, Object> resp = controller.publishContent(req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void publish_content_auth_error_returns_401() throws Exception {
        when(contentService.publishContent(any(PublishContentRequestDTO.class)))
                .thenThrow(new SecurityException("用户认证失败"));
        PublishContentRequestDTO req = new PublishContentRequestDTO();
        Map<String, Object> resp = controller.publishContent(req);
        assertThat(resp.get("code")).isEqualTo(401);
    }

    @Test
    void publish_content_sql_error_returns_500() throws Exception {
        when(contentService.publishContent(any(PublishContentRequestDTO.class)))
                .thenThrow(new SQLException("错误"));
        PublishContentRequestDTO req = new PublishContentRequestDTO();
        Map<String, Object> resp = controller.publishContent(req);
        assertThat(resp.get("code")).isEqualTo(500);
    }

    @Test
    void get_content_detail_invalid_id_returns_400() {
        Map<String, Object> resp = controller.getContentDetail(" ");
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void get_content_detail_not_found_returns_404() throws Exception {
        when(contentService.getContentDetail("CNT-404")).thenThrow(new IllegalArgumentException("内容不存在"));
        Map<String, Object> resp = controller.getContentDetail("CNT-404");
        assertThat(resp.get("code")).isEqualTo(404);
    }
}
