package service.auth;

import dto.auth.*;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplAdditionalTest {
    @Mock
    DatabaseManager databaseManager;

    @Mock
    Connection conn;

    AuthServiceImpl authService;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthServiceImpl();
        Field f = AuthServiceImpl.class.getDeclaredField("databaseManager");
        f.setAccessible(true);
        f.set(authService, databaseManager);
        when(databaseManager.getConnection()).thenReturn(conn);
    }

    @Test
    void validate_buyer_address_too_long() {
        BuyerRegisterRequestDTO req = new BuyerRegisterRequestDTO();
        req.setPassword("Abcdef12");
        req.setPhone("13800138000");
        req.setUserType("buyer");
        req.setShippingAddress("x".repeat(501));
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertFalse(ok);
        assertTrue(errors.stream().anyMatch(s -> s.contains("收货地址长度不能超过500")));
    }

    @Test
    void validate_expert_missing_field() {
        ExpertRegisterRequestDTO req = new ExpertRegisterRequestDTO();
        req.setPassword("Abcdef12");
        req.setPhone("13800138000");
        req.setUserType("expert");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertFalse(ok);
        assertTrue(errors.stream().anyMatch(s -> s.contains("专业领域不能为空")));
    }

    @Test
    void validate_bank_missing_name() {
        BankRegisterRequestDTO req = new BankRegisterRequestDTO();
        req.setPassword("Abcdef12");
        req.setPhone("13800138000");
        req.setUserType("bank");
        java.util.List<String> errors = new java.util.ArrayList<>();
        boolean ok = authService.validateRegisterRequest(req, errors);
        assertFalse(ok);
        assertTrue(errors.stream().anyMatch(s -> s.contains("银行名称不能为空")));
    }

    @Test
    void getBalance_null_returns_zero() throws Exception {
        when(databaseManager.getBuyerBalance("13800138000")).thenReturn(null);
        BigDecimal b = authService.getBalance("13800138000", "buyer");
        assertEquals(BigDecimal.ZERO, b);
    }

    @Test
    void register_existing_type_already_registered() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Abcdef12");
        req.setNickname("n");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString("uid")).thenReturn("u1");
        when(rsUser.getString("password")).thenReturn("Abcdef12");
        when(rsUser.getString("nickname")).thenReturn("n");
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

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_existing_type_not_registered_then_save() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setPhone("13800138000");
        req.setPassword("Abcdef12");
        req.setNickname("n");
        req.setUserType("buyer");

        PreparedStatement stmtUser = mock(PreparedStatement.class);
        ResultSet rsUser = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM users WHERE phone = ?"))).thenReturn(stmtUser);
        when(stmtUser.executeQuery()).thenReturn(rsUser);
        when(rsUser.next()).thenReturn(true);
        when(rsUser.getString("uid")).thenReturn("u1");
        when(rsUser.getString("password")).thenReturn("Abcdef12");
        when(rsUser.getString("nickname")).thenReturn("n");
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

        PreparedStatement stmtInsertBuyer = mock(PreparedStatement.class);
        when(conn.prepareStatement(startsWith("INSERT INTO user_buyers"))).thenReturn(stmtInsertBuyer);
        when(stmtInsertBuyer.executeUpdate()).thenReturn(1);

        AuthResponseDTO resp = authService.register(req);
        assertEquals("u1", resp.getUid());
        assertEquals("n", resp.getNickname());
        assertEquals("13800138000", resp.getPhone());
        assertEquals("buyer", resp.getUserType());
    }
}