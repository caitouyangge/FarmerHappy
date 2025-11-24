package controller;

import dto.community.PostCommentRequestDTO;
import dto.community.PostReplyRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.community.CommentService;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerIT {

    @Mock
    CommentService commentService;

    private CommentController controller;

    @BeforeEach
    void setUp() {
        controller = new CommentController(commentService);
    }

    @Test
    void post_comment_not_found_maps_404() throws Exception {
        PostCommentRequestDTO req = new PostCommentRequestDTO();
        when(commentService.postComment("CNT-1", req)).thenThrow(new IllegalArgumentException("帖子不存在"));
        Map<String, Object> resp = controller.postComment("CNT-1", req);
        assertThat(resp.get("code")).isEqualTo(404);
    }

    @Test
    void post_comment_param_error_maps_400() throws Exception {
        PostCommentRequestDTO req = new PostCommentRequestDTO();
        when(commentService.postComment("CNT-1", req)).thenThrow(new IllegalArgumentException("手机号格式错误"));
        Map<String, Object> resp = controller.postComment("CNT-1", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void post_comment_auth_error_maps_401() throws Exception {
        PostCommentRequestDTO req = new PostCommentRequestDTO();
        when(commentService.postComment("CNT-1", req)).thenThrow(new SecurityException("用户认证失败"));
        Map<String, Object> resp = controller.postComment("CNT-1", req);
        assertThat(resp.get("code")).isEqualTo(401);
    }

    @Test
    void post_reply_comment_not_found_maps_404() throws Exception {
        PostReplyRequestDTO req = new PostReplyRequestDTO();
        when(commentService.postReply("CMT-1", req)).thenThrow(new IllegalArgumentException("评论不存在"));
        Map<String, Object> resp = controller.postReply("CMT-1", req);
        assertThat(resp.get("code")).isEqualTo(404);
    }

    @Test
    void post_reply_param_error_maps_400() throws Exception {
        PostReplyRequestDTO req = new PostReplyRequestDTO();
        when(commentService.postReply("CMT-1", req)).thenThrow(new IllegalArgumentException("手机号格式错误"));
        Map<String, Object> resp = controller.postReply("CMT-1", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void get_comment_list_invalid_id_returns_400() {
        Map<String, Object> resp = controller.getCommentList(" ");
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void post_comment_sql_error_returns_500() throws Exception {
        PostCommentRequestDTO req = new PostCommentRequestDTO();
        when(commentService.postComment(any(String.class), any(PostCommentRequestDTO.class)))
                .thenThrow(new SQLException("错误"));
        Map<String, Object> resp = controller.postComment("CNT-1", req);
        assertThat(resp.get("code")).isEqualTo(500);
    }
}

