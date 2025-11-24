package service.community;

import dto.community.PostCommentRequestDTO;
import dto.community.PostReplyRequestDTO;
import entity.Content;
import entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.DatabaseManager;
import service.auth.AuthService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class CommentServiceIT {
    private DatabaseManager db;
    private CommentServiceImpl service;
    private ContentServiceImpl contentService;
    private String buyerUid;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.configure("jdbc:h2:mem:", "farmer_happy;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "sa", "", "org.h2.Driver");
        db = DatabaseManager.getInstance();
        db.initializeDatabaseForTests();

        Connection conn = db.getConnection();
        Statement st = conn.createStatement();
        st.executeUpdate("CREATE TABLE IF NOT EXISTS contents (\n" +
                "    content_id VARCHAR(50) PRIMARY KEY,\n" +
                "    title VARCHAR(200) NOT NULL,\n" +
                "    content TEXT NOT NULL,\n" +
                "    content_type VARCHAR(20) NOT NULL,\n" +
                "    author_user_id VARCHAR(36) NOT NULL,\n" +
                "    author_nickname VARCHAR(30) NOT NULL,\n" +
                "    author_role VARCHAR(20) NOT NULL,\n" +
                "    view_count INT DEFAULT 0,\n" +
                "    comment_count INT DEFAULT 0,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                ")");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS content_images (\n" +
                "    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                "    content_id VARCHAR(50) NOT NULL,\n" +
                "    image_url VARCHAR(500) NOT NULL,\n" +
                "    sort_order INT DEFAULT 0\n" +
                ")");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS comments (\n" +
                "    comment_id VARCHAR(50) PRIMARY KEY,\n" +
                "    content_id VARCHAR(50) NOT NULL,\n" +
                "    parent_comment_id VARCHAR(50),\n" +
                "    author_user_id VARCHAR(36) NOT NULL,\n" +
                "    author_nickname VARCHAR(30) NOT NULL,\n" +
                "    author_role VARCHAR(20) NOT NULL,\n" +
                "    reply_to_user_id VARCHAR(36),\n" +
                "    reply_to_nickname VARCHAR(30),\n" +
                "    content TEXT NOT NULL,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                ")");
        st.executeUpdate("DELETE FROM comments");
        st.executeUpdate("DELETE FROM content_images");
        st.executeUpdate("DELETE FROM contents");
        st.executeUpdate("CREATE TABLE IF NOT EXISTS user_experts (\n" +
                "    expert_id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                "    uid VARCHAR(36) UNIQUE,\n" +
                "    expertise_field VARCHAR(100),\n" +
                "    work_experience INT DEFAULT 0,\n" +
                "    enable BOOLEAN DEFAULT TRUE\n" +
                ")");
        st.executeUpdate("DELETE FROM user_experts");
        st.executeUpdate("DELETE FROM user_buyers");
        st.executeUpdate("DELETE FROM users");
        st.close();

        buyerUid = "buyer-uid-1";

        PreparedStatement iu = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138030', 'pwd', '买家A', 0, TRUE)");
        iu.setString(1, buyerUid);
        iu.executeUpdate();
        PreparedStatement ib = conn.prepareStatement(
                "INSERT INTO user_buyers (uid, enable) VALUES (?, TRUE)");
        ib.setString(1, buyerUid);
        ib.executeUpdate();
        conn.close();

        AuthService auth = new AuthService() {
            @Override
            public dto.auth.AuthResponseDTO register(dto.auth.RegisterRequestDTO registerRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public dto.auth.AuthResponseDTO login(dto.auth.LoginRequestDTO loginRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean validateRegisterRequest(dto.auth.RegisterRequestDTO registerRequest,
                    java.util.List<String> errors) {
                throw new UnsupportedOperationException();
            }

            @Override
            public User findUserByPhone(String phone) {
                if (!"13800138030".equals(phone))
                    return null;
                User u = new User();
                u.setUid(buyerUid);
                u.setPhone(phone);
                u.setNickname("买家A");
                return u;
            }

            @Override
            public void saveUser(User user) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean checkUserTypeExists(String uid, String userType) {
                return true;
            }

            @Override
            public java.math.BigDecimal getBalance(String phone, String userType) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveFarmerExtension(String uid, dto.farmer.FarmerRegisterRequestDTO farmerRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveBuyerExtension(String uid, dto.auth.BuyerRegisterRequestDTO buyerRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveExpertExtension(String uid, dto.auth.ExpertRegisterRequestDTO expertRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveBankExtension(String uid, dto.auth.BankRegisterRequestDTO bankRequest) {
                throw new UnsupportedOperationException();
            }
        };

        contentService = new ContentServiceImpl(db, auth);
        service = new CommentServiceImpl(db, auth, contentService);
    }

    @Test
    void post_comment_success_and_increment_count() throws Exception {
        Content post = new Content("帖子", "内容", "questions", null, buyerUid, "买家A", "buyer");
        db.saveContent(post);

        PostCommentRequestDTO req = new PostCommentRequestDTO();
        req.setComment("很好");
        req.setPhone("13800138030");
        dto.community.PostCommentResponseDTO resp = service.postComment(post.getContentId(), req);
        assertThat(resp.getCommentId()).isNotBlank();

        Connection conn = db.getConnection();
        PreparedStatement pc = conn.prepareStatement("SELECT comment_count FROM contents WHERE content_id = ?");
        pc.setString(1, post.getContentId());
        ResultSet rc = pc.executeQuery();
        rc.next();
        assertThat(rc.getInt(1)).isEqualTo(1);

        PreparedStatement list = conn.prepareStatement("SELECT COUNT(*) FROM comments WHERE content_id = ?");
        list.setString(1, post.getContentId());
        ResultSet rs = list.executeQuery();
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(1);
        conn.close();
    }

    @Test
    void post_reply_success_and_increment_count() throws Exception {
        Content post = new Content("帖子2", "内容2", "questions", null, buyerUid, "买家A", "buyer");
        db.saveContent(post);

        PostCommentRequestDTO cReq = new PostCommentRequestDTO();
        cReq.setComment("一级评论");
        cReq.setPhone("13800138030");
        dto.community.PostCommentResponseDTO cResp = service.postComment(post.getContentId(), cReq);

        PostReplyRequestDTO rReq = new PostReplyRequestDTO();
        rReq.setComment("二级回复");
        rReq.setPhone("13800138030");
        Thread.sleep(1100);
        dto.community.PostReplyResponseDTO rResp = service.postReply(cResp.getCommentId(), rReq);
        assertThat(rResp.getParentCommentId()).isEqualTo(cResp.getCommentId());

        Connection conn = db.getConnection();
        PreparedStatement pc = conn.prepareStatement("SELECT comment_count FROM contents WHERE content_id = ?");
        pc.setString(1, post.getContentId());
        ResultSet rc = pc.executeQuery();
        rc.next();
        assertThat(rc.getInt(1)).isEqualTo(2);
        conn.close();
    }

    @Test
    void get_comment_list_returns_nested_replies() throws Exception {
        Content post = new Content("帖子4", "内容4", "questions", null, buyerUid, "买家A", "buyer");
        db.saveContent(post);

        PostCommentRequestDTO cReq = new PostCommentRequestDTO();
        cReq.setComment("一级");
        cReq.setPhone("13800138030");
        dto.community.PostCommentResponseDTO cResp = service.postComment(post.getContentId(), cReq);
        Thread.sleep(2100);

        PostReplyRequestDTO r1 = new PostReplyRequestDTO();
        r1.setComment("二级-1");
        r1.setPhone("13800138030");
        dto.community.PostReplyResponseDTO pr1 = service.postReply(cResp.getCommentId(), r1);

        Thread.sleep(2100);
        PostReplyRequestDTO r2 = new PostReplyRequestDTO();
        r2.setComment("二级-2(回复二级-1)");
        r2.setPhone("13800138030");
        Thread.sleep(1200);
        dto.community.PostReplyResponseDTO pr2 = service.postReply(pr1.getCommentId(), r2);

        dto.community.CommentListResponseDTO list = service.getCommentList(post.getContentId());
        assertThat(list.getTotalComments()).isEqualTo(3);
        assertThat(list.getList().size()).isEqualTo(1);
        dto.community.CommentItemDTO item = list.getList().get(0);
        assertThat(item.getReplies().size()).isEqualTo(2);
        assertThat(item.getReplies().get(0).getReplyToUserId()).isNotBlank();
        assertThat(item.getReplies().get(1).getReplyToUserId()).isNotBlank();
    }

    @Test
    void reply_to_reply_attaches_to_top_level_parent() throws Exception {
        Content post = new Content("帖子5", "内容5", "questions", null, buyerUid, "买家A", "buyer");
        db.saveContent(post);

        PostCommentRequestDTO cReq = new PostCommentRequestDTO();
        cReq.setComment("Top");
        cReq.setPhone("13800138030");
        dto.community.PostCommentResponseDTO top = service.postComment(post.getContentId(), cReq);
        Thread.sleep(2100);

        PostReplyRequestDTO r1 = new PostReplyRequestDTO();
        r1.setComment("R1");
        r1.setPhone("13800138030");
        dto.community.PostReplyResponseDTO rr1 = service.postReply(top.getCommentId(), r1);

        Thread.sleep(2100);
        PostReplyRequestDTO r2 = new PostReplyRequestDTO();
        r2.setComment("R2 to R1");
        r2.setPhone("13800138030");
        Thread.sleep(1200);
        dto.community.PostReplyResponseDTO rr2 = service.postReply(rr1.getCommentId(), r2);

        assertThat(rr2.getParentCommentId()).isEqualTo(top.getCommentId());

        Connection conn = db.getConnection();
        PreparedStatement pc = conn.prepareStatement("SELECT comment_count FROM contents WHERE content_id = ?");
        pc.setString(1, post.getContentId());
        ResultSet rc = pc.executeQuery();
        rc.next();
        assertThat(rc.getInt(1)).isEqualTo(3);
        conn.close();
    }

    @Test
    void post_comment_nonexistent_content_throws() {
        PostCommentRequestDTO req = new PostCommentRequestDTO();
        req.setComment("x");
        req.setPhone("13800138030");
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.postComment("CNT-not-exists", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void post_reply_nonexistent_comment_throws() {
        PostReplyRequestDTO req = new PostReplyRequestDTO();
        req.setComment("x");
        req.setPhone("13800138030");
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.postReply("CMT-not-exists", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void post_comment_auth_failure_throws() {
        Content post = new Content("帖子3", "内容3", "questions", null, buyerUid, "买家A", "buyer");
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> {
                    PostCommentRequestDTO req = new PostCommentRequestDTO();
                    req.setComment("x");
                    req.setPhone("13900000000");
                    service.postComment(post.getContentId(), req);
                })
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("认证失败");
    }
}
