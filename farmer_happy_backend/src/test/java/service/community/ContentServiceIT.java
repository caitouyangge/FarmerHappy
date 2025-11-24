package service.community;

import dto.community.ContentDetailResponseDTO;
import dto.community.ContentListResponseDTO;
import dto.community.PublishContentRequestDTO;
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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ContentServiceIT {
    private DatabaseManager db;
    private ContentServiceImpl service;
    private String expertUid;
    private String farmerUid;

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
        st.executeUpdate("DELETE FROM user_farmers");
        st.executeUpdate("DELETE FROM users");
        st.executeUpdate("DELETE FROM users");
        st.close();

        expertUid = "expert-uid-1";
        farmerUid = "farmer-uid-1";

        PreparedStatement iu1 = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138020', 'pwd', '专家A', 0, TRUE)");
        iu1.setString(1, expertUid);
        iu1.executeUpdate();
        PreparedStatement ie = conn.prepareStatement(
                "INSERT INTO user_experts (uid, expertise_field, work_experience, enable) VALUES (?, '果树', 10, TRUE)");
        ie.setString(1, expertUid);
        ie.executeUpdate();

        PreparedStatement iu2 = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138021', 'pwd', '农户A', 0, TRUE)");
        iu2.setString(1, farmerUid);
        iu2.executeUpdate();
        PreparedStatement ifa = conn.prepareStatement(
                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES (?, '好农场', TRUE)");
        ifa.setString(1, farmerUid);
        ifa.executeUpdate();
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
                User u = new User();
                if ("13800138020".equals(phone)) {
                    u.setUid(expertUid);
                    u.setPhone(phone);
                    u.setNickname("专家A");
                } else if ("13800138021".equals(phone)) {
                    u.setUid(farmerUid);
                    u.setPhone(phone);
                    u.setNickname("农户A");
                } else
                    return null;
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

        service = new ContentServiceImpl(db, auth);
    }

    @Test
    void publish_content_expert_articles_success_and_persist() throws Exception {
        PublishContentRequestDTO req = new PublishContentRequestDTO();
        req.setTitle("技术分享");
        req.setContent("苹果树修剪技巧");
        req.setContentType("articles");
        req.setImages(Arrays.asList("http://img/c1.jpg"));
        req.setPhone("13800138020");

        dto.community.PublishContentResponseDTO resp = service.publishContent(req);
        assertThat(resp.getContentType()).isEqualTo("articles");

        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM contents WHERE content_id = ?");
        ps.setString(1, resp.getContentId());
        ResultSet rs = ps.executeQuery();
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(1);
        conn.close();
    }

    @Test
    void publish_content_farmer_articles_forbidden() {
        PublishContentRequestDTO req = new PublishContentRequestDTO();
        req.setTitle("技术分享");
        req.setContent("不该允许的文章");
        req.setContentType("articles");
        req.setPhone("13800138021");

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.publishContent(req))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("权限不足");
    }

    @Test
    void content_list_sorting_and_summary() throws Exception {
        Content a = new Content("A", new String(new char[120]).replace('\0', 'a'), "questions", null, expertUid, "专家A",
                "expert");
        a.setViewCount(10);
        a.setCommentCount(5);
        Content b = new Content("B", "短文B", "articles", null, expertUid, "专家A", "expert");
        b.setViewCount(40);
        b.setCommentCount(1);
        Content c = new Content("C", "短文C", "experiences", null, farmerUid, "农户A", "farmer");
        c.setViewCount(5);
        c.setCommentCount(20);
        a.setContentId("CNT-A");
        b.setContentId("CNT-B");
        c.setContentId("CNT-C");
        db.saveContent(a);
        db.saveContent(b);
        db.saveContent(c);

        Connection conn = db.getConnection();
        Statement st = conn.createStatement();
        st.executeUpdate(
                "UPDATE contents SET created_at = TIMESTAMPADD(SECOND, -30, CURRENT_TIMESTAMP) WHERE content_id = '"
                        + a.getContentId() + "'");
        st.executeUpdate(
                "UPDATE contents SET created_at = TIMESTAMPADD(SECOND, -20, CURRENT_TIMESTAMP) WHERE content_id = '"
                        + b.getContentId() + "'");
        st.executeUpdate(
                "UPDATE contents SET created_at = TIMESTAMPADD(SECOND, -10, CURRENT_TIMESTAMP) WHERE content_id = '"
                        + c.getContentId() + "'");
        conn.close();

        ContentListResponseDTO newest = service.getContentList(null, null, "newest");
        assertThat(newest.getList().get(0).getTitle()).isEqualTo("C");

        ContentListResponseDTO hottest = service.getContentList(null, null, "hottest");
        assertThat(hottest.getList().get(0).getTitle()).isEqualTo("B");

        ContentListResponseDTO commented = service.getContentList(null, null, "commented");
        assertThat(commented.getList().get(0).getTitle()).isEqualTo("C");

        ContentListResponseDTO list = service.getContentList(null, null, null);
        String summary = list.getList().stream().filter(i -> "A".equals(i.getTitle())).findFirst().get().getContent();
        assertThat(summary.length()).isGreaterThan(100);
        assertThat(summary).endsWith("...");
    }

    @Test
    void content_detail_increments_view_count() throws Exception {
        Content a = new Content("D", "正文D", "articles", null, expertUid, "专家A", "expert");
        a.setViewCount(5);
        db.saveContent(a);

        ContentDetailResponseDTO d = service.getContentDetail(a.getContentId());
        assertThat(d.getViewCount()).isEqualTo(6);
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT view_count FROM contents WHERE content_id = ?");
        ps.setString(1, a.getContentId());
        ResultSet rs = ps.executeQuery();
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(6);
        conn.close();
    }
}
