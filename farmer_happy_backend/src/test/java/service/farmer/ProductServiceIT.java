package service.farmer;

import dto.farmer.ProductListResponseDTO;
import entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.DatabaseManager;
import service.auth.AuthService;

import java.sql.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceIT {
    private DatabaseManager db;
    private ProductServiceImpl service;
    private String farmerUid;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.configure("jdbc:h2:mem:", "farmer_happy;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "sa", "", "org.h2.Driver");
        db = DatabaseManager.getInstance();
        db.initializeDatabaseForTests();
        db.clearAllTablesForTests();

        farmerUid = "farmer-uid-2";

        Connection conn = db.getConnection();

        PreparedStatement insertUser = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138002', 'pwd', 'farmer', 0, TRUE)");
        insertUser.setString(1, farmerUid);
        insertUser.executeUpdate();

        PreparedStatement insertFarmer = conn.prepareStatement(
                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES (?, '好农场', TRUE)",
                Statement.RETURN_GENERATED_KEYS);
        insertFarmer.setString(1, farmerUid);
        insertFarmer.executeUpdate();
        ResultSet farmerKeys = insertFarmer.getGeneratedKeys();
        farmerKeys.next();
        long farmerId = farmerKeys.getLong(1);

        PreparedStatement insertProduct1 = conn.prepareStatement(
                "INSERT INTO products (farmer_id, category, title, detailed_description, price, stock, description, origin, status, enable, created_at) VALUES (?, 'fruits', '苹果A', '规格A', 10.00, 10, 'desc', '河北', 'on_shelf', TRUE, CURRENT_TIMESTAMP)");
        insertProduct1.setLong(1, farmerId);
        insertProduct1.executeUpdate();

        PreparedStatement insertProduct2 = conn.prepareStatement(
                "INSERT INTO products (farmer_id, category, title, detailed_description, price, stock, description, origin, status, enable, created_at) VALUES (?, 'fruits', '苹果B', '规格B', 12.00, 5, 'desc', '河北', 'on_shelf', TRUE, CURRENT_TIMESTAMP)");
        insertProduct2.setLong(1, farmerId);
        insertProduct2.executeUpdate();

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
                u.setUid(farmerUid);
                u.setPhone(phone);
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

        service = new ProductServiceImpl(db, auth);
    }

    @Test
    void list_products_for_farmer_phone() throws Exception {
        List<ProductListResponseDTO> list = service.getProductList("13800138002", "on_shelf", "苹果");
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getTitle()).contains("苹果");
    }

    @Test
    void get_product_detail_returns_images_and_fields() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        PreparedStatement addImg = conn.prepareStatement(
                "INSERT INTO product_images (product_id, image_url, sort_order, is_main, enable) VALUES (?, 'http://img/d.jpg', 0, TRUE, TRUE)");
        addImg.setLong(1, pid);
        addImg.executeUpdate();
        conn.close();

        dto.farmer.ProductDetailResponseDTO d = service.getProductDetail(String.valueOf(pid), "13800138002");
        assertThat(d.getProduct_id()).isEqualTo(String.valueOf(pid));
        assertThat(d.getImages()).isNotEmpty();
        assertThat(d.getStatus()).isEqualTo("on_shelf");
    }

    @Test
    void product_list_with_non_farmer_identity_throws_error() {
        service.farmer.ProductServiceImpl svc = new service.farmer.ProductServiceImpl(db,
                new service.auth.AuthService() {
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
                    public entity.User findUserByPhone(String phone) {
                        entity.User u = new entity.User();
                        u.setUid(farmerUid);
                        u.setPhone(phone);
                        return u;
                    }

                    @Override
                    public void saveUser(entity.User user) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean checkUserTypeExists(String uid, String userType) {
                        return false;
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
                });

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> svc.getProductList("13800138002", "on_shelf", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只有农户可以操作商品");
    }

    @Test
    void off_shelf_then_on_shelf_updates_status() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        dto.farmer.ProductStatusUpdateResponseDTO off = service.offShelfProduct(String.valueOf(pid), "13800138002");
        assertThat(off.getStatus()).isEqualTo("off_shelf");
        entity.Product p1 = db.getProductById(pid);
        assertThat(p1.getStatus()).isEqualTo("off_shelf");

        dto.farmer.ProductStatusUpdateResponseDTO on = service.onShelfProduct(String.valueOf(pid), "13800138002");
        assertThat(on.getStatus()).isEqualTo("on_shelf");
        entity.Product p2 = db.getProductById(pid);
        assertThat(p2.getStatus()).isEqualTo("on_shelf");
    }

    @Test
    void partial_update_changes_price_stock_and_images() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        dto.farmer.ProductUpdateRequestDTO req = new dto.farmer.ProductUpdateRequestDTO();
        req.setPhone("13800138002");
        req.setPrice(15.00);
        req.setStock(20);
        req.setImages(java.util.Arrays.asList("http://img/a.jpg", "http://img/b.jpg"));

        dto.farmer.ProductResponseDTO resp = service.partialUpdateProduct(String.valueOf(pid), req);
        assertThat(resp.getPrice()).isEqualTo(15.00);
        assertThat(resp.getStock()).isEqualTo(20);
        assertThat(resp.getImages().size()).isEqualTo(2);

        entity.Product p = db.getProductById(pid);
        assertThat(p.getPrice()).isEqualTo(15.00);
        assertThat(p.getStock()).isEqualTo(20);
        assertThat(p.getImages().size()).isEqualTo(2);
        assertThat(p.getImages().get(0)).isEqualTo("http://img/a.jpg");
    }

    @Test
    void delete_product_removes_product_and_images() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);

        PreparedStatement addImg = conn.prepareStatement(
                "INSERT INTO product_images (product_id, image_url, sort_order, is_main, enable) VALUES (?, 'http://img/x.jpg', 1, FALSE, TRUE)");
        addImg.setLong(1, pid);
        addImg.executeUpdate();
        conn.close();

        service.deleteProduct(String.valueOf(pid), "13800138002");

        PreparedStatement checkP = db.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM products WHERE product_id = ?");
        checkP.setLong(1, pid);
        ResultSet rp = checkP.executeQuery();
        rp.next();
        assertThat(rp.getInt(1)).isEqualTo(0);

        PreparedStatement checkI = db.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM product_images WHERE product_id = ?");
        checkI.setLong(1, pid);
        ResultSet ri = checkI.executeQuery();
        ri.next();
        assertThat(ri.getInt(1)).isEqualTo(0);
    }

    @Test
    void off_shelf_when_already_off_shelf_throws_error() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        service.offShelfProduct(String.valueOf(pid), "13800138002");
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.offShelfProduct(String.valueOf(pid), "13800138002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法执行下架");
    }

    @Test
    void on_shelf_when_already_on_shelf_throws_error() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.onShelfProduct(String.valueOf(pid), "13800138002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法执行上架");
    }

    @Test
    void create_product_inserts_and_returns_links() throws Exception {
        dto.farmer.ProductCreateRequestDTO req = new dto.farmer.ProductCreateRequestDTO();
        req.setCategory("fruits");
        req.setTitle("苹果C");
        req.setDetailedDescription("规格C");
        req.setPrice(9.99);
        req.setStock(7);
        req.setDescription("desc");
        req.setOrigin("河北");
        req.setImages(java.util.Arrays.asList("http://img/c1.jpg"));

        dto.farmer.ProductResponseDTO resp = service.createProduct(req, farmerUid);
        assertThat(resp.getProduct_id()).isNotBlank();
        assertThat(resp.getStatus()).isEqualTo("on_shelf");
        assertThat(resp.get_links().get("self")).contains("/api/v1/farmer/products/");
    }

    @Test
    void create_product_images_order_preserved() throws Exception {
        dto.farmer.ProductCreateRequestDTO req = new dto.farmer.ProductCreateRequestDTO();
        req.setCategory("fruits");
        req.setTitle("苹果D");
        req.setDetailedDescription("规格D");
        req.setPrice(10.50);
        req.setStock(8);
        req.setDescription("desc");
        req.setOrigin("河北");
        req.setImages(java.util.Arrays.asList("http://img/1.jpg", "http://img/2.jpg", "http://img/3.jpg"));

        dto.farmer.ProductResponseDTO resp = service.createProduct(req, farmerUid);
        long pid = Long.parseLong(resp.getProduct_id());
        entity.Product p = db.getProductById(pid);
        assertThat(p.getImages()).containsExactly("http://img/1.jpg", "http://img/2.jpg", "http://img/3.jpg");
    }

    @Test
    void partial_update_without_images_keeps_existing() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);

        PreparedStatement addImg = conn.prepareStatement(
                "INSERT INTO product_images (product_id, image_url, sort_order, is_main, enable) VALUES (?, 'http://img/keep.jpg', 2, FALSE, TRUE)");
        addImg.setLong(1, pid);
        addImg.executeUpdate();
        conn.close();

        dto.farmer.ProductUpdateRequestDTO req = new dto.farmer.ProductUpdateRequestDTO();
        req.setPhone("13800138002");
        req.setPrice(13.00);
        req.setImages(null);

        dto.farmer.ProductResponseDTO resp = service.partialUpdateProduct(String.valueOf(pid), req);
        assertThat(resp.getPrice()).isEqualTo(13.00);
        assertThat(resp.getImages()).isNotEmpty();
        assertThat(resp.getImages()).anyMatch(u -> u.equals("http://img/keep.jpg"));
    }

    @Test
    void get_product_detail_with_nonexistent_farmer_identity_throws_error() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        service.farmer.ProductServiceImpl svc = new service.farmer.ProductServiceImpl(db,
                new service.auth.AuthService() {
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
                    public entity.User findUserByPhone(String phone) {
                        entity.User u = new entity.User();
                        u.setUid("not-farmer-uid");
                        u.setPhone(phone);
                        return u;
                    }

                    @Override
                    public void saveUser(entity.User user) {
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
                });

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> svc.getProductDetail(String.valueOf(pid), "13900000000"))
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("农户信息不存在");
    }

    @Test
    void delete_product_not_exists_throws_error() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.deleteProduct("999999", "13800138002"))
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    void partial_update_with_null_phone_throws_user_not_exists() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        service.farmer.ProductServiceImpl svc = new service.farmer.ProductServiceImpl(db,
                new service.auth.AuthService() {
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
                    public entity.User findUserByPhone(String phone) {
                        return null;
                    }

                    @Override
                    public void saveUser(entity.User user) {
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
                });

        dto.farmer.ProductUpdateRequestDTO req = new dto.farmer.ProductUpdateRequestDTO();
        req.setPhone(null);
        req.setPrice(11.00);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> svc.partialUpdateProduct(String.valueOf(pid), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void on_shelf_with_other_farmer_throws_error() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement insertUser = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES ('farmer-uid-3', '13800138008', 'pwd', 'farmer3', 0, TRUE)");
        insertUser.executeUpdate();
        PreparedStatement insertFarmer = conn.prepareStatement(
                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES ('farmer-uid-3', '其他农场', TRUE)",
                Statement.RETURN_GENERATED_KEYS);
        insertFarmer.executeUpdate();
        ResultSet fk = insertFarmer.getGeneratedKeys();
        fk.next();
        long otherFarmerId = fk.getLong(1);

        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        service.farmer.ProductServiceImpl svc = new service.farmer.ProductServiceImpl(db,
                new service.auth.AuthService() {
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
                    public entity.User findUserByPhone(String phone) {
                        entity.User u = new entity.User();
                        u.setUid("farmer-uid-3");
                        u.setPhone(phone);
                        return u;
                    }

                    @Override
                    public void saveUser(entity.User user) {
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
                });

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> svc.onShelfProduct(String.valueOf(pid), "13800138008"))
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    void batch_off_shelf_two_products_success() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
        ResultSet rs = ps.executeQuery();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        while (rs.next())
            ids.add(String.valueOf(rs.getLong(1)));
        conn.close();

        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138002");
        req.setProduct_ids(ids);

        dto.farmer.ProductBatchActionResultDTO result = service.batchActionProducts(req);
        assertThat(result.getSuccess_count()).isEqualTo(ids.size());
        assertThat(result.getFailure_count()).isEqualTo(0);

        for (String id : ids) {
            entity.Product p = db.getProductById(Long.parseLong(id));
            assertThat(p.getStatus()).isEqualTo("off_shelf");
        }
    }

    @Test
    void batch_on_shelf_two_products_success() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
        ResultSet rs = ps.executeQuery();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        while (rs.next())
            ids.add(String.valueOf(rs.getLong(1)));
        conn.close();

        dto.farmer.ProductBatchActionRequestDTO req1 = new dto.farmer.ProductBatchActionRequestDTO();
        req1.setAction("off-shelf");
        req1.setPhone("13800138002");
        req1.setProduct_ids(ids);
        service.batchActionProducts(req1);

        dto.farmer.ProductBatchActionRequestDTO req2 = new dto.farmer.ProductBatchActionRequestDTO();
        req2.setAction("on-shelf");
        req2.setPhone("13800138002");
        req2.setProduct_ids(ids);

        dto.farmer.ProductBatchActionResultDTO result = service.batchActionProducts(req2);
        assertThat(result.getSuccess_count()).isEqualTo(ids.size());
        assertThat(result.getFailure_count()).isEqualTo(0);

        for (String id : ids) {
            entity.Product p = db.getProductById(Long.parseLong(id));
            assertThat(p.getStatus()).isEqualTo("on_shelf");
        }
    }

    @Test
    void batch_delete_products_success() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
        ResultSet rs = ps.executeQuery();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        while (rs.next())
            ids.add(String.valueOf(rs.getLong(1)));
        conn.close();

        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("delete");
        req.setPhone("13800138002");
        req.setProduct_ids(ids);

        dto.farmer.ProductBatchActionResultDTO result = service.batchActionProducts(req);
        assertThat(result.getSuccess_count()).isEqualTo(ids.size());
        assertThat(result.getFailure_count()).isEqualTo(0);

        PreparedStatement check = db.getConnection().prepareStatement("SELECT COUNT(*) FROM products");
        ResultSet rc = check.executeQuery();
        rc.next();
        assertThat(rc.getInt(1)).isEqualTo(0);
    }

    @Test
    void batch_action_invalid_action_throws_error() {
        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("invalid");
        req.setPhone("13800138002");
        req.setProduct_ids(java.util.Arrays.asList("1"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.batchActionProducts(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无效的操作类型");
    }

    @Test
    void batch_action_empty_ids_throws_error() {
        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138002");
        req.setProduct_ids(java.util.Collections.emptyList());

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.batchActionProducts(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("商品ID列表不能为空");
    }

    @Test
    void batch_action_too_many_ids_throws_error() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++)
            ids.add(String.valueOf(1000 + i));

        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138002");
        req.setProduct_ids(ids);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.batchActionProducts(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("一次最多只能操作100个商品");
    }

    @Test
    void batch_action_non_farmer_identity_throws_error() {
        service.farmer.ProductServiceImpl svc = new service.farmer.ProductServiceImpl(db,
                new service.auth.AuthService() {
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
                    public entity.User findUserByPhone(String phone) {
                        entity.User u = new entity.User();
                        u.setUid("farmer-uid-2");
                        u.setPhone(phone);
                        return u;
                    }

                    @Override
                    public void saveUser(entity.User user) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public boolean checkUserTypeExists(String uid, String userType) {
                        return false;
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
                });

        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138002");
        req.setProduct_ids(java.util.Arrays.asList("1"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> svc.batchActionProducts(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只有农户");
    }

    @Test
    void update_product_full_replaces_images_and_fields() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id LIMIT 1");
        ResultSet rs = ps.executeQuery();
        rs.next();
        long pid = rs.getLong(1);
        conn.close();

        dto.farmer.ProductUpdateRequestDTO req = new dto.farmer.ProductUpdateRequestDTO();
        req.setPhone("13800138002");
        req.setTitle("新标题");
        req.setDetailedDescription("新规格");
        req.setPrice(19.99);
        req.setStock(99);
        req.setImages(java.util.Arrays.asList("http://img/new1.jpg", "http://img/new2.jpg"));

        dto.farmer.ProductResponseDTO resp = service.updateProduct(String.valueOf(pid), req);
        assertThat(resp.getTitle()).isEqualTo("新标题");
        assertThat(resp.getDetailedDescription()).isEqualTo("新规格");
        assertThat(resp.getPrice()).isEqualTo(19.99);
        assertThat(resp.getStock()).isEqualTo(99);
        assertThat(resp.getImages().size()).isEqualTo(2);
        assertThat(resp.getImages().get(0)).isEqualTo("http://img/new1.jpg");

        entity.Product p = db.getProductById(pid);
        assertThat(p.getTitle()).isEqualTo("新标题");
        assertThat(p.getDetailedDescription()).isEqualTo("新规格");
        assertThat(p.getPrice()).isEqualTo(19.99);
        assertThat(p.getStock()).isEqualTo(99);
        assertThat(p.getImages().size()).isEqualTo(2);
    }

    @Test
    void delete_product_not_owned_throws_error() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement insertUser = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES ('farmer-uid-9', '13800138009', 'pwd', 'farmer9', 0, TRUE)");
        insertUser.executeUpdate();
        PreparedStatement insertFarmer = conn.prepareStatement(
                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES ('farmer-uid-9', '外部农场', TRUE)",
                Statement.RETURN_GENERATED_KEYS);
        insertFarmer.executeUpdate();
        ResultSet fk = insertFarmer.getGeneratedKeys();
        fk.next();
        long otherFarmerId = fk.getLong(1);

        PreparedStatement insertProduct = conn.prepareStatement(
                "INSERT INTO products (farmer_id, category, title, detailed_description, price, stock, description, origin, status, enable, created_at) VALUES (?, 'fruits', '外部苹果', '外部规格', 8.00, 3, 'desc', '河北', 'on_shelf', TRUE, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS);
        insertProduct.setLong(1, otherFarmerId);
        insertProduct.executeUpdate();
        ResultSet pk = insertProduct.getGeneratedKeys();
        pk.next();
        long pid = pk.getLong(1);
        conn.close();

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.deleteProduct(String.valueOf(pid), "13800138002"))
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageContaining("不属于该农户");
    }

    @Test
    void batch_action_mixed_results_some_failures() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
        ResultSet rs = ps.executeQuery();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        while (rs.next())
            ids.add(String.valueOf(rs.getLong(1)));

        PreparedStatement insertUser = conn.prepareStatement(
                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES ('farmer-uid-10', '13800138110', 'pwd', 'farmer10', 0, TRUE)");
        insertUser.executeUpdate();
        PreparedStatement insertFarmer = conn.prepareStatement(
                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES ('farmer-uid-10', '外部农场2', TRUE)",
                Statement.RETURN_GENERATED_KEYS);
        insertFarmer.executeUpdate();
        ResultSet fk = insertFarmer.getGeneratedKeys();
        fk.next();
        long otherFarmerId = fk.getLong(1);
        PreparedStatement insertProduct = conn.prepareStatement(
                "INSERT INTO products (farmer_id, category, title, detailed_description, price, stock, description, origin, status, enable, created_at) VALUES (?, 'fruits', '外部苹果2', '外部规格2', 11.00, 5, 'desc', '河北', 'on_shelf', TRUE, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS);
        insertProduct.setLong(1, otherFarmerId);
        insertProduct.executeUpdate();
        ResultSet pk = insertProduct.getGeneratedKeys();
        pk.next();
        String notOwnedId = String.valueOf(pk.getLong(1));
        conn.close();

        java.util.ArrayList<String> mixed = new java.util.ArrayList<>();
        mixed.add(ids.get(0));
        mixed.add(ids.get(1));
        mixed.add(notOwnedId);
        mixed.add("abc");

        dto.farmer.ProductBatchActionRequestDTO req = new dto.farmer.ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setPhone("13800138002");
        req.setProduct_ids(mixed);

        dto.farmer.ProductBatchActionResultDTO result = service.batchActionProducts(req);
        assertThat(result.getSuccess_count()).isEqualTo(2);
        assertThat(result.getFailure_count()).isEqualTo(2);

        entity.Product p1 = db.getProductById(Long.parseLong(ids.get(0)));
        entity.Product p2 = db.getProductById(Long.parseLong(ids.get(1)));
        assertThat(p1.getStatus()).isEqualTo("off_shelf");
        assertThat(p2.getStatus()).isEqualTo("off_shelf");
    }

    @Test
    void get_product_detail_non_numeric_id_throws_number_format() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.getProductDetail("abc", "13800138002"))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void product_list_status_filter_returns_off_shelf_only() throws Exception {
        Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT product_id FROM products ORDER BY product_id");
        ResultSet rs = ps.executeQuery();
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        while (rs.next())
            ids.add(rs.getLong(1));
        conn.close();

        service.offShelfProduct(String.valueOf(ids.get(0)), "13800138002");
        service.offShelfProduct(String.valueOf(ids.get(1)), "13800138002");

        java.util.List<ProductListResponseDTO> list = service.getProductList("13800138002", "off_shelf", null);
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getStatus()).isEqualTo("off_shelf");
    }
}
