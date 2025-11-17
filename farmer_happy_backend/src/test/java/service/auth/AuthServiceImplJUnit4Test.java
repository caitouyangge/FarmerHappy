package service.auth;

import dto.auth.*;
import dto.farmer.FarmerRegisterRequestDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import repository.DatabaseManager;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplJUnit4Test {
    @Mock
    DatabaseManager databaseManager;

    @Mock
    Connection conn;

    AuthServiceImpl authService;

    @BeforeAll
    static void beforeAll() {}

    @AfterAll
    static void afterAll() {}

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthServiceImpl();
        Field f = AuthServiceImpl.class.getDeclaredField("databaseManager");
        f.setAccessible(true);
        f.set(authService, databaseManager);
        when(databaseManager.getConnection()).thenReturn(conn);
    }

    @Test
    void validateRegisterRequest_valid_farmer() {
        FarmerRegisterRequestDTO req = new FarmerRegisterRequestDTO();
        req.setPassword("Abcdef12");
        req.setNickname("昵称");
        req.setPhone("13800138000");
        req.setUserType("farmer");
        req.setFarmName("好农场");
        req.setFarmAddress("地址");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertTrue(ok);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateRegisterRequest_invalid_password_length() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPassword("Ab1");
        req.setNickname("a");
        req.setPhone("13800138000");
        req.setUserType("buyer");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertFalse(ok);
        assertTrue(errors.stream().anyMatch(s -> s.contains("长度必须在8-32")));
    }

    @Test
    void validateRegisterRequest_invalid_phone() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPassword("Abcdef12");
        req.setNickname("a");
        req.setPhone("12345");
        req.setUserType("buyer");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertFalse(ok);
        assertTrue(errors.stream().anyMatch(s -> s.contains("手机号格式不正确")));
    }

    @Test
    void validateRegisterRequest_invalid_user_type() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPassword("Abcdef12");
        req.setNickname("a");
        req.setPhone("13800138000");
        req.setUserType("unknown");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertFalse(ok);
        assertTrue(errors.stream().anyMatch(s -> s.contains("用户类型不正确")));
    }

    @Test
    void login_missing_phone_throws() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone(null);
        req.setPassword("abc");
        req.setUserType("buyer");
        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void login_user_not_found() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Abcdef12");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(false);

        assertThrows(SecurityException.class, () -> authService.login(req));
    }

    @Test
    void login_wrong_password() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Wrong123");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString("uid")).thenReturn("uid-1");
        when(rsUser.getString("password")).thenReturn("Abcdef12");
        when(rsUser.getString("nickname")).thenReturn("nick");
        when(rsUser.getString("phone")).thenReturn("13800138000");
        when(rsUser.getBigDecimal("money")).thenReturn(null);
        when(rsUser.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(rsUser.getTimestamp("updated_at")).thenReturn(null);

        assertThrows(SecurityException.class, () -> authService.login(req));
    }

    @Test
    void login_missing_password_throws() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone("13800138000");
        req.setPassword(null);
        req.setUserType("buyer");
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("密码不能为空");
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 33})
    void validateRegisterRequest_password_length_out_of_range(int len) {
        RegisterRequestDTO req = new RegisterRequestDTO();
        String password = "A" + "b".repeat(Math.max(0, len - 2)) + "1";
        req.setPassword(password);
        req.setNickname("n");
        req.setPhone("13800138000");
        req.setUserType("buyer");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertThat(ok).isFalse();
        assertThat(errors).anyMatch(s -> s.contains("长度必须在8-32"));
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 32})
    void validateRegisterRequest_password_length_boundary_valid(int len) {
        RegisterRequestDTO req = new RegisterRequestDTO();
        String password = "A" + "b".repeat(Math.max(0, len - 2)) + "1";
        req.setPassword(password);
        req.setNickname("n");
        req.setPhone("13800138000");
        req.setUserType("buyer");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertThat(ok).isTrue();
        assertThat(errors).isEmpty();
    }

    @Test
    void login_user_type_not_registered() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Abcdef12");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString("uid")).thenReturn("uid-1");
        when(rsUser.getString("password")).thenReturn("Abcdef12");
        when(rsUser.getString("nickname")).thenReturn("nick");
        when(rsUser.getString("phone")).thenReturn("13800138000");
        when(rsUser.getBigDecimal("money")).thenReturn(null);
        when(rsUser.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(rsUser.getTimestamp("updated_at")).thenReturn(null);

        PreparedStatement stmtCount = mock(PreparedStatement.class);
        ResultSet rsCount = mock(ResultSet.class);
        when(conn.prepareStatement(startsWith("SELECT COUNT(*) as count FROM user_buyers"))).thenReturn(stmtCount);
        when(stmtCount.executeQuery()).thenReturn(rsCount);
        when(rsCount.next()).thenReturn(true);
        when(rsCount.getInt("count")).thenReturn(0);

        assertThrows(SecurityException.class, () -> authService.login(req));
    }

    @Test
    void login_success_buyer() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Abcdef12");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString("uid")).thenReturn("uid-1");
        when(rsUser.getString("password")).thenReturn("Abcdef12");
        when(rsUser.getString("nickname")).thenReturn("nick");
        when(rsUser.getString("phone")).thenReturn("13800138000");
        when(rsUser.getBigDecimal("money")).thenReturn(null);
        when(rsUser.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(rsUser.getTimestamp("updated_at")).thenReturn(null);

        PreparedStatement stmtCount = mock(PreparedStatement.class);
        ResultSet rsCount = mock(ResultSet.class);
        when(conn.prepareStatement(startsWith("SELECT COUNT(*) as count FROM user_buyers"))).thenReturn(stmtCount);
        when(stmtCount.executeQuery()).thenReturn(rsCount);
        when(rsCount.next()).thenReturn(true);
        when(rsCount.getInt("count")).thenReturn(1);

        when(databaseManager.getBuyerBalance("13800138000")).thenReturn(new BigDecimal("100.50"));

        AuthResponseDTO resp = authService.login(req);
        Assertions.assertEquals("uid-1", resp.getUid());
        Assertions.assertEquals("nick", resp.getNickname());
        Assertions.assertEquals("13800138000", resp.getPhone());
        Assertions.assertEquals("buyer", resp.getUserType());
        Assertions.assertEquals(new BigDecimal("100.50"), resp.getMoney());
    }

    @Test
    void register_user_exists_wrong_password() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Wrong123");
        req.setNickname("nick");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString("uid")).thenReturn("uid-1");
        when(rsUser.getString("password")).thenReturn("Abcdef12");
        when(rsUser.getString("nickname")).thenReturn("nick");
        when(rsUser.getString("phone")).thenReturn("13800138000");
        when(rsUser.getBigDecimal("money")).thenReturn(null);
        when(rsUser.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(rsUser.getTimestamp("updated_at")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_new_farmer_success() throws Exception {
        FarmerRegisterRequestDTO req = new FarmerRegisterRequestDTO();
        req.setPhone("13800138001");
        req.setPassword("Abcdef12");
        req.setNickname("nick");
        req.setUserType("farmer");
        req.setFarmName("好农场");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(false);

        PreparedStatement stmtInsertUser = mock(PreparedStatement.class);
        when(conn.prepareStatement(startsWith("INSERT INTO users"))).thenReturn(stmtInsertUser);
        when(stmtInsertUser.executeUpdate()).thenReturn(1);

        PreparedStatement stmtInsertFarmer = mock(PreparedStatement.class);
        when(conn.prepareStatement(startsWith("INSERT INTO user_farmers"))).thenReturn(stmtInsertFarmer);
        when(stmtInsertFarmer.executeUpdate()).thenReturn(1);

        AuthResponseDTO resp = authService.register(req);
        Assertions.assertNotNull(resp.getUid());
        Assertions.assertEquals("nick", resp.getNickname());
        Assertions.assertEquals("13800138001", resp.getPhone());
        Assertions.assertEquals("farmer", resp.getUserType());
    }
}