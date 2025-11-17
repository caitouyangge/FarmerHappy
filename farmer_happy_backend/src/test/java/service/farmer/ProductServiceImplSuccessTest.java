package service.farmer;

import dto.farmer.ProductUpdateRequestDTO;
import dto.farmer.ProductDetailResponseDTO;
import dto.farmer.ProductListResponseDTO;
import dto.farmer.ProductResponseDTO;
import dto.farmer.ProductStatusUpdateResponseDTO;
import dto.farmer.ProductBatchActionRequestDTO;
import dto.farmer.ProductBatchActionResultDTO;
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
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceImplSuccessTest {
    @Mock
    DatabaseManager databaseManager;
    @Mock
    AuthService authService;
    @Mock
    Connection conn;

    ProductServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProductServiceImpl();
        Field fDb = ProductServiceImpl.class.getDeclaredField("databaseManager");
        fDb.setAccessible(true);
        fDb.set(service, databaseManager);
        Field fAuth = ProductServiceImpl.class.getDeclaredField("authService");
        fAuth.setAccessible(true);
        fAuth.set(service, authService);
        when(databaseManager.getConnection()).thenReturn(conn);
    }

    @Test
    void partialUpdateProduct_success() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        req.setTitle("新标题");
        req.setPrice(12.5);
        req.setStock(7);
        req.setImages(java.util.Arrays.asList("img1", "img2"));

        User user = new User("Abcdef12", "n", "13800138000");
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
        when(rsProduct.next()).thenReturn(true);
        when(rsProduct.getLong("product_id")).thenReturn(100L);
        when(rsProduct.getLong("farmer_id")).thenReturn(77L);
        when(rsProduct.getString("category")).thenReturn("cat");
        when(rsProduct.getString("title")).thenReturn("旧标题");
        when(rsProduct.getString("detailed_description")).thenReturn("desc");
        when(rsProduct.getDouble("price")).thenReturn(10.0);
        when(rsProduct.getInt("stock")).thenReturn(5);
        when(rsProduct.getString("description")).thenReturn("d");
        when(rsProduct.getString("origin")).thenReturn("o");
        when(rsProduct.getString("status")).thenReturn("pending_review");
        when(rsProduct.getBoolean("enable")).thenReturn(true);
        when(rsProduct.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

        PreparedStatement stmtUpdate = mock(PreparedStatement.class);
        when(conn.prepareStatement(startsWith("UPDATE products SET"))).thenReturn(stmtUpdate);
        when(stmtUpdate.executeUpdate()).thenReturn(1);

        PreparedStatement stmtDeleteImgs = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("DELETE FROM product_images WHERE product_id = ?"))).thenReturn(stmtDeleteImgs);

        PreparedStatement stmtInsertImgs = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("INSERT INTO product_images (product_id, image_url, sort_order) VALUES (?, ?, ?)"))).thenReturn(stmtInsertImgs);

        ProductResponseDTO resp = service.partialUpdateProduct("100", req);
        assertEquals("100", resp.getProduct_id());
        assertEquals("新标题", resp.getTitle());
        assertEquals(12.5, resp.getPrice());
        assertEquals(7, resp.getStock());
        assertEquals("pending_review", resp.getStatus());
        assertEquals(java.util.Arrays.asList("img1", "img2"), resp.getImages());
    }

    @Test
    void getProductDetail_success() throws Exception {
        User user = new User("Abcdef12", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtDetail = mock(PreparedStatement.class);
        ResultSet rsDetail = mock(ResultSet.class);
        when(conn.prepareStatement(startsWith("SELECT p.*, pi.image_url FROM products"))).thenReturn(stmtDetail);
        when(stmtDetail.executeQuery()).thenReturn(rsDetail);
        when(rsDetail.next()).thenReturn(true, false);
        when(rsDetail.getLong("product_id")).thenReturn(100L);
        when(rsDetail.getString("title")).thenReturn("T");
        when(rsDetail.getString("detailed_description")).thenReturn("D");
        when(rsDetail.getDouble("price")).thenReturn(10.0);
        when(rsDetail.getInt("stock")).thenReturn(5);
        when(rsDetail.getString("description")).thenReturn("d");
        when(rsDetail.getString("origin")).thenReturn("o");
        when(rsDetail.getString("status")).thenReturn("pending_review");
        when(rsDetail.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(rsDetail.getString("image_url")).thenReturn("img1");

        ProductDetailResponseDTO detail = service.getProductDetail("100", "13800138000");
        assertEquals("100", detail.getProduct_id());
        assertEquals("T", detail.getTitle());
        assertEquals("pending_review", detail.getStatus());
        assertEquals(List.of("img1"), detail.getImages());
    }

    @Test
    void getProductList_success() throws Exception {
        User user = new User("Abcdef12", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtList = mock(PreparedStatement.class);
        ResultSet rsList = mock(ResultSet.class);
        when(conn.prepareStatement(startsWith("SELECT p.product_id, p.title, p.price"))).thenReturn(stmtList);
        when(stmtList.executeQuery()).thenReturn(rsList);
        when(rsList.next()).thenReturn(true, false);
        when(rsList.getLong("product_id")).thenReturn(100L);
        when(rsList.getString("title")).thenReturn("T");
        when(rsList.getDouble("price")).thenReturn(10.0);
        when(rsList.getInt("stock")).thenReturn(5);
        when(rsList.getString("status")).thenReturn("pending_review");
        when(rsList.getString("main_image_url")).thenReturn("img0");

        List<ProductListResponseDTO> list = service.getProductList("13800138000", null, null);
        assertEquals(1, list.size());
        assertEquals("100", list.get(0).getProduct_id());
        assertEquals("T", list.get(0).getTitle());
    }

    @Test
    void onShelfProduct_success() throws Exception {
        User user = new User("Abcdef12", "n", "13800138000");
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
        when(rsStatus.getString("status")).thenReturn("off_shelf");

        PreparedStatement stmtUpdateStatus = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("UPDATE products SET status = ? WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtUpdateStatus);
        when(stmtUpdateStatus.executeUpdate()).thenReturn(1);

        ProductStatusUpdateResponseDTO r = service.onShelfProduct("100", "13800138000");
        assertEquals("100", r.getProduct_id());
        assertEquals("on_shelf", r.getStatus());
    }

    @Test
    void offShelfProduct_success() throws Exception {
        User user = new User("Abcdef12", "n", "13800138000");
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

        PreparedStatement stmtUpdateStatus = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("UPDATE products SET status = ? WHERE product_id = ? AND farmer_id = ?"))).thenReturn(stmtUpdateStatus);
        when(stmtUpdateStatus.executeUpdate()).thenReturn(1);

        ProductStatusUpdateResponseDTO r = service.offShelfProduct("100", "13800138000");
        assertEquals("100", r.getProduct_id());
        assertEquals("off_shelf", r.getStatus());
    }

    @Test
    void deleteProduct_success() throws Exception {
        User user = new User("Abcdef12", "n", "13800138000");
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
        when(stmtDelProduct.executeUpdate()).thenReturn(1);

        Assertions.assertDoesNotThrow(() -> service.deleteProduct("100", "13800138000"));
    }

    @Test
    void updateProduct_images_null_no_image_update() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        req.setTitle("新标题");
        req.setDetailedDescription("新详情");
        req.setPrice(99.9);
        req.setStock(20);
        req.setImages(null);

        User user = new User("Abcdef12", "n", "13800138000");
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
        when(rsProduct.next()).thenReturn(true);
        when(rsProduct.getLong("product_id")).thenReturn(100L);
        when(rsProduct.getLong("farmer_id")).thenReturn(77L);
        when(rsProduct.getString("category")).thenReturn("cat");
        when(rsProduct.getString("title")).thenReturn("旧标题");
        when(rsProduct.getString("detailed_description")).thenReturn("旧详情");
        when(rsProduct.getDouble("price")).thenReturn(10.0);
        when(rsProduct.getInt("stock")).thenReturn(5);
        when(rsProduct.getString("description")).thenReturn("d");
        when(rsProduct.getString("origin")).thenReturn("o");
        when(rsProduct.getString("status")).thenReturn("pending_review");
        when(rsProduct.getBoolean("enable")).thenReturn(true);
        when(rsProduct.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

        PreparedStatement stmtUpdate = mock(PreparedStatement.class);
        when(conn.prepareStatement(startsWith("UPDATE products SET category ="))).thenReturn(stmtUpdate);
        when(stmtUpdate.executeUpdate()).thenReturn(1);

        PreparedStatement stmtDeleteImgs = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("DELETE FROM product_images WHERE product_id = ?"))).thenReturn(stmtDeleteImgs);

        ProductResponseDTO r = service.updateProduct("100", req);
        assertEquals("100", r.getProduct_id());
        verify(conn, times(1)).prepareStatement(eq("DELETE FROM product_images WHERE product_id = ?"));
        verify(conn, never()).prepareStatement(eq("INSERT INTO product_images (product_id, image_url, sort_order) VALUES (?, ?, ?)"));
    }

    @Test
    void updateProduct_success() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138000");
        req.setTitle("新标题");
        req.setDetailedDescription("新详情");
        req.setPrice(99.9);
        req.setStock(20);
        req.setImages(java.util.Arrays.asList("a.png", "b.png"));

        User user = new User("Abcdef12", "n", "13800138000");
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
        when(rsProduct.next()).thenReturn(true);
        when(rsProduct.getLong("product_id")).thenReturn(100L);
        when(rsProduct.getLong("farmer_id")).thenReturn(77L);
        when(rsProduct.getString("category")).thenReturn("cat");
        when(rsProduct.getString("title")).thenReturn("旧标题");
        when(rsProduct.getString("detailed_description")).thenReturn("旧详情");
        when(rsProduct.getDouble("price")).thenReturn(10.0);
        when(rsProduct.getInt("stock")).thenReturn(5);
        when(rsProduct.getString("description")).thenReturn("d");
        when(rsProduct.getString("origin")).thenReturn("o");
        when(rsProduct.getString("status")).thenReturn("pending_review");
        when(rsProduct.getBoolean("enable")).thenReturn(true);
        when(rsProduct.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

        PreparedStatement stmtUpdate = mock(PreparedStatement.class);
        when(conn.prepareStatement(startsWith("UPDATE products SET category ="))).thenReturn(stmtUpdate);
        when(stmtUpdate.executeUpdate()).thenReturn(1);

        PreparedStatement stmtDeleteImgs = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("DELETE FROM product_images WHERE product_id = ?"))).thenReturn(stmtDeleteImgs);

        PreparedStatement stmtInsertImgs = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("INSERT INTO product_images (product_id, image_url, sort_order) VALUES (?, ?, ?)"))).thenReturn(stmtInsertImgs);

        ProductResponseDTO r = service.updateProduct("100", req);
        assertEquals("100", r.getProduct_id());
        assertEquals("新标题", r.getTitle());
        assertEquals("新详情", r.getDetailedDescription());
        assertEquals(99.9, r.getPrice());
        assertEquals(20, r.getStock());
        assertEquals(java.util.Arrays.asList("a.png", "b.png"), r.getImages());
    }

    @Test
    void createProduct_success() throws Exception {
        ProductCreateRequestDTO req = new ProductCreateRequestDTO();
        req.setCategory("cat");
        req.setTitle("标题");
        req.setDetailedDescription("详情");
        req.setPrice(9.9);
        req.setStock(3);
        req.setDescription("描述");
        req.setOrigin("产地");
        req.setImages(java.util.Arrays.asList("i0", "i1"));

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtInsertProduct = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);
        when(conn.prepareStatement(startsWith("INSERT INTO products"), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(stmtInsertProduct);
        when(stmtInsertProduct.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(true);
        when(rsKeys.getLong(1)).thenReturn(100L);

        PreparedStatement stmtInsertImgs = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("INSERT INTO product_images (product_id, image_url, sort_order) VALUES (?, ?, ?)"))).thenReturn(stmtInsertImgs);

        ProductResponseDTO r = service.createProduct(req, "U1");
        assertEquals("100", r.getProduct_id());
        assertEquals("标题", r.getTitle());
        assertEquals(9.9, r.getPrice());
        assertEquals(3, r.getStock());
        assertEquals(java.util.Arrays.asList("i0", "i1"), r.getImages());
        assertEquals("on_shelf", r.getStatus());
    }

    @Test
    void batchActionProducts_off_shelf_all_success() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Arrays.asList("300", "301"));

        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtOwnership1 = mock(PreparedStatement.class);
        PreparedStatement stmtOwnership2 = mock(PreparedStatement.class);
        ResultSet rsOwn1 = mock(ResultSet.class);
        ResultSet rsOwn2 = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT COUNT(*) as count FROM products p JOIN user_farmers f ON p.farmer_id = f.farmer_id WHERE p.product_id = ? AND f.uid = ?")))
                .thenReturn(stmtOwnership1, stmtOwnership2);
        when(stmtOwnership1.executeQuery()).thenReturn(rsOwn1);
        when(stmtOwnership2.executeQuery()).thenReturn(rsOwn2);
        when(rsOwn1.next()).thenReturn(true);
        when(rsOwn1.getInt("count")).thenReturn(1);
        when(rsOwn2.next()).thenReturn(true);
        when(rsOwn2.getInt("count")).thenReturn(1);

        PreparedStatement stmtOff = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("UPDATE products SET status = 'off_shelf' WHERE product_id = ?"))).thenReturn(stmtOff);
        when(stmtOff.executeUpdate()).thenReturn(1);

        ProductBatchActionResultDTO r = service.batchActionProducts(req);
        assertEquals(2, r.getSuccess_count());
        assertEquals(0, r.getFailure_count());
        assertEquals("下架成功", r.getResults().get(0).getMessage());
        assertEquals("下架成功", r.getResults().get(1).getMessage());
    }

    @Test
    void getProductList_with_filters_success() throws Exception {
        User user = new User("Abcdef12", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtFarmer = mock(PreparedStatement.class);
        ResultSet rsFarmer = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT farmer_id FROM user_farmers WHERE uid = ?"))).thenReturn(stmtFarmer);
        when(stmtFarmer.executeQuery()).thenReturn(rsFarmer);
        when(rsFarmer.next()).thenReturn(true);
        when(rsFarmer.getLong("farmer_id")).thenReturn(77L);

        PreparedStatement stmtList = mock(PreparedStatement.class);
        ResultSet rsList = mock(ResultSet.class);
        when(conn.prepareStatement(startsWith("SELECT p.product_id, p.title, p.price"))).thenReturn(stmtList);
        when(stmtList.executeQuery()).thenReturn(rsList);
        when(rsList.next()).thenReturn(true, false);
        when(rsList.getLong("product_id")).thenReturn(100L);
        when(rsList.getString("title")).thenReturn("T");
        when(rsList.getDouble("price")).thenReturn(10.0);
        when(rsList.getInt("stock")).thenReturn(5);
        when(rsList.getString("status")).thenReturn("on_shelf");
        when(rsList.getString("main_image_url")).thenReturn("img0");

        List<ProductListResponseDTO> list = service.getProductList("13800138000", "on_shelf", "T");
        assertEquals(1, list.size());
        assertEquals("100", list.get(0).getProduct_id());
        assertEquals("T", list.get(0).getTitle());
        assertEquals("on_shelf", list.get(0).getStatus());
        assertEquals("img0", list.get(0).getMain_image_url());
    }

    @Test
    void batchActionProducts_mixed_results_on_shelf() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("on-shelf");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Arrays.asList("100", "101"));

        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtOwnership1 = mock(PreparedStatement.class);
        PreparedStatement stmtOwnership2 = mock(PreparedStatement.class);
        ResultSet rsOwn1 = mock(ResultSet.class);
        ResultSet rsOwn2 = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT COUNT(*) as count FROM products p JOIN user_farmers f ON p.farmer_id = f.farmer_id WHERE p.product_id = ? AND f.uid = ?")))
                .thenReturn(stmtOwnership1, stmtOwnership2);
        when(stmtOwnership1.executeQuery()).thenReturn(rsOwn1);
        when(stmtOwnership2.executeQuery()).thenReturn(rsOwn2);
        when(rsOwn1.next()).thenReturn(true);
        when(rsOwn1.getInt("count")).thenReturn(1);
        when(rsOwn2.next()).thenReturn(true);
        when(rsOwn2.getInt("count")).thenReturn(0);

        PreparedStatement stmtOn = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("UPDATE products SET status = 'on_shelf' WHERE product_id = ?"))).thenReturn(stmtOn);
        when(stmtOn.executeUpdate()).thenReturn(1);

        ProductBatchActionResultDTO r = service.batchActionProducts(req);
        assertEquals(1, r.getSuccess_count());
        assertEquals(1, r.getFailure_count());
        assertEquals(2, r.getResults().size());
        assertTrue(r.getResults().get(0).isSuccess());
        assertEquals("上架成功", r.getResults().get(0).getMessage());
        assertNotNull(r.getResults().get(0).get_links().get("self"));
        assertFalse(r.getResults().get(1).isSuccess());
        assertEquals("商品不存在或不属于该农户", r.getResults().get(1).getMessage());
    }

    @Test
    void batchActionProducts_delete_success() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("delete");
        req.setPhone("13800138000");
        req.setProduct_ids(java.util.Arrays.asList("200", "201"));

        User user = new User("U1", "n", "13800138000");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists(user.getUid(), "farmer")).thenReturn(true);

        PreparedStatement stmtOwnership1 = mock(PreparedStatement.class);
        PreparedStatement stmtOwnership2 = mock(PreparedStatement.class);
        ResultSet rsOwn1 = mock(ResultSet.class);
        ResultSet rsOwn2 = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT COUNT(*) as count FROM products p JOIN user_farmers f ON p.farmer_id = f.farmer_id WHERE p.product_id = ? AND f.uid = ?")))
                .thenReturn(stmtOwnership1, stmtOwnership2);
        when(stmtOwnership1.executeQuery()).thenReturn(rsOwn1);
        when(stmtOwnership2.executeQuery()).thenReturn(rsOwn2);
        when(rsOwn1.next()).thenReturn(true);
        when(rsOwn1.getInt("count")).thenReturn(1);
        when(rsOwn2.next()).thenReturn(true);
        when(rsOwn2.getInt("count")).thenReturn(1);

        PreparedStatement stmtDelImgs = mock(PreparedStatement.class);
        PreparedStatement stmtDelProd = mock(PreparedStatement.class);
        when(conn.prepareStatement(eq("DELETE FROM product_images WHERE product_id = ?"))).thenReturn(stmtDelImgs);
        when(conn.prepareStatement(eq("DELETE FROM products WHERE product_id = ?"))).thenReturn(stmtDelProd);
        when(stmtDelProd.executeUpdate()).thenReturn(1);

        ProductBatchActionResultDTO r = service.batchActionProducts(req);
        assertEquals(2, r.getSuccess_count());
        assertEquals(0, r.getFailure_count());
        assertEquals(2, r.getResults().size());
        assertNull(r.getResults().get(0).get_links().get("self"));
        assertNull(r.getResults().get(1).get_links().get("self"));
    }
}