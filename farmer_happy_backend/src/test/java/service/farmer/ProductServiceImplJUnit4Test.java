package service.farmer;

import dto.farmer.ProductUpdateRequestDTO;
import dto.farmer.ProductBatchActionRequestDTO;
import dto.farmer.ProductCreateRequestDTO;
import entity.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import repository.DatabaseManager;
import service.auth.AuthService;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceImplJupiterTest {
    @Mock
    DatabaseManager databaseManager;

    @Mock
    AuthService authService;

    @Mock
    Connection conn;

    ProductServiceImpl productService;

    @BeforeEach
    void setUp() throws Exception {
        productService = new ProductServiceImpl();
        Field fDb = ProductServiceImpl.class.getDeclaredField("databaseManager");
        fDb.setAccessible(true);
        fDb.set(productService, databaseManager);
        Field fAuth = ProductServiceImpl.class.getDeclaredField("authService");
        fAuth.setAccessible(true);
        fAuth.set(productService, authService);
        when(databaseManager.getConnection()).thenReturn(conn);
    }

    @Test
    void partialUpdateProduct_not_farmer() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.partialUpdateProduct("100", req));
    }

    @Test
    void getProductList_user_not_found() throws Exception {
        when(authService.findUserByPhone("13800138000")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> productService.getProductList("13800138000", null, null));
    }

    @Test
    void onShelfProduct_not_farmer() throws Exception {
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.onShelfProduct("100", "13800138000"));
    }

    @Test
    void updateProduct_not_farmer() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct("100", req));
    }

    @Test
    void updateProduct_user_not_found() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct("100", req));
    }

    @Test
    void batchActionProducts_user_not_found() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("on-shelf");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Arrays.asList("100"));
        when(authService.findUserByPhone("13800138000")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> productService.batchActionProducts(req));
    }

    @Test
    void batchActionProducts_not_farmer() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Arrays.asList("100"));
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.batchActionProducts(req));
    }

    @Test
    void batchActionProducts_invalid_params_empty_ids() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("delete");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Collections.emptyList());
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> productService.batchActionProducts(req));
    }

    @Test
    void batchActionProducts_invalid_action() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("invalid");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Arrays.asList("1"));
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> productService.batchActionProducts(req));
    }

    @Test
    void batchActionProducts_too_many_ids() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++) ids.add(String.valueOf(i));
        req.setAction("on-shelf");
        req.setPhone("13800138000");
        req.setProduct_ids(ids);
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> productService.batchActionProducts(req));
    }

    @Test
    void deleteProduct_user_not_found() throws Exception {
        when(authService.findUserByPhone("13800138000")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> productService.deleteProduct("100", "13800138000"));
    }

    @Test
    void deleteProduct_not_farmer() throws Exception {
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.deleteProduct("100", "13800138000"));
    }

    @Test
    void deleteProduct_not_owned() throws Exception {
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtDelProduct = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("DELETE FROM products WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtDelProduct);
        when(stmtDelProduct.executeUpdate()).thenReturn(0);

        assertThrows(SQLException.class, () -> productService.deleteProduct("100", "13800138000"));
    }

    @Test
    void offShelfProduct_not_farmer() throws Exception {
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.offShelfProduct("100", "13800138000"));
    }

    @Test
    void offShelfProduct_illegal_state() throws Exception {
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtStatus = mock(PreparedStatement.class);
        ResultSet rsStatus = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT status FROM products WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtStatus);
        when(stmtStatus.executeQuery()).thenReturn(rsStatus);
        when(rsStatus.next()).thenReturn(true);
        when(rsStatus.getString("status")).thenReturn("pending_review");

        assertThrows(IllegalStateException.class, () -> productService.offShelfProduct("100", "13800138000"));
    }

    @Test
    void onShelfProduct_illegal_state() throws Exception {
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtStatus = mock(PreparedStatement.class);
        ResultSet rsStatus = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT status FROM products WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtStatus);
        when(stmtStatus.executeQuery()).thenReturn(rsStatus);
        when(rsStatus.next()).thenReturn(true);
        when(rsStatus.getString("status")).thenReturn("on_shelf");

        assertThrows(IllegalStateException.class, () -> productService.onShelfProduct("100", "13800138000"));
    }

    @Test
    void getProductList_checkUserType_sql_error() throws Exception {
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenThrow(new SQLException("X"));
        SQLException e = assertThrows(SQLException.class, () -> productService.getProductList("13800138000", null, null));
        assertEquals("验证用户身份失败: X", e.getMessage());
    }

    @Test
    void onShelfProduct_checkUserType_sql_error() throws Exception {
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenThrow(new SQLException("Y"));
        SQLException e = assertThrows(SQLException.class, () -> productService.onShelfProduct("100", "13800138000"));
        assertEquals("验证用户身份失败: Y", e.getMessage());
    }

    @Test
    void offShelfProduct_checkUserType_sql_error() throws Exception {
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenThrow(new SQLException("Z"));
        SQLException e = assertThrows(SQLException.class, () -> productService.offShelfProduct("100", "13800138000"));
        assertEquals("验证用户身份失败: Z", e.getMessage());
    }

    @Test
    void partialUpdateProduct_checkUserType_sql_error() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenThrow(new SQLException("E1"));
        SQLException e = assertThrows(SQLException.class, () -> productService.partialUpdateProduct("100", req));
        assertEquals("验证用户身份失败: E1", e.getMessage());
    }

    @Test
    void updateProduct_checkUserType_sql_error() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenThrow(new SQLException("E2"));
        SQLException e = assertThrows(SQLException.class, () -> productService.updateProduct("100", req));
        assertEquals("验证用户身份失败: E2", e.getMessage());
    }

    @Test
    void getProductDetail_user_not_found() throws Exception {
        when(authService.findUserByPhone("13800138000")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> productService.getProductDetail("100", "13800138000"));
    }

    @Test
    void getProductDetail_not_farmer() throws Exception {
        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> productService.getProductDetail("100", "13800138000"));
    }

    @Test
    void createProduct_farmer_not_found() throws Exception {
        ProductCreateRequestDTO req = new ProductCreateRequestDTO();
        req.setCategory("c");
        req.setTitle("t");
        req.setDetailedDescription("d");
        req.setPrice(1.0);
        req.setStock(1);
        req.setDescription("desc");
        req.setOrigin("o");

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(false);

        assertThrows(SQLException.class, () -> productService.createProduct(req, "U1"));
    }

    @Test
    void partialUpdateProduct_product_not_owned() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtProduct = mock(PreparedStatement.class);
        ResultSet rsProduct = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM products WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtProduct);
        when(stmtProduct.executeQuery()).thenReturn(rsProduct);
        when(rsProduct.next()).thenReturn(false);

        assertThrows(SQLException.class, () -> productService.partialUpdateProduct("100", req));
    }

    @Test
    void updateProduct_product_not_owned() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        User user = new User("Abcdef12", "nick", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtProduct = mock(PreparedStatement.class);
        ResultSet rsProduct = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT * FROM products WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtProduct);
        when(stmtProduct.executeQuery()).thenReturn(rsProduct);
        when(rsProduct.next()).thenReturn(false);

        assertThrows(SQLException.class, () -> productService.updateProduct("100", req));
    }

}