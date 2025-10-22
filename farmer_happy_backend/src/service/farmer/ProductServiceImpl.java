// src/service/farmer/ProductServiceImpl.java
package service.farmer;

import dto.farmer.ProductCreateRequestDTO;
import dto.farmer.ProductResponseDTO;
import dto.farmer.ProductStatusUpdateResponseDTO;
import entity.Product;
import repository.DatabaseManager;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ProductServiceImpl implements ProductService {
    private DatabaseManager databaseManager;
    private AuthService authService;

    public ProductServiceImpl() {
        this.databaseManager = new DatabaseManager();
        this.authService = new AuthServiceImpl();
    }

    @Override
    public ProductResponseDTO createProduct(ProductCreateRequestDTO request, String userId) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 根据用户ID获取农户ID
            Long farmerIdLong = getFarmerIdByUserId(conn, userId);

            // 插入产品信息
            Product product = new Product();
            product.setFarmerId(farmerIdLong);
            product.setCategory(request.getCategory()); // 设置分类
            product.setTitle(request.getTitle());
            product.setSpecification(request.getSpecification());
            product.setPrice(request.getPrice());
            product.setStock(request.getStock());
            product.setDescription(request.getDescription());
            product.setOrigin(request.getOrigin() != null ? request.getOrigin() : getDefaultOrigin(conn, farmerIdLong));
            product.setStatus("pending_review");
            product.setCreatedAt(LocalDateTime.now());

            Long productId = insertProduct(conn, product);
            product.setProductId(productId);

            // 插入图片信息
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                insertProductImages(conn, productId, request.getImages());
            }

            conn.commit();

            // 构建响应对象
            ProductResponseDTO response = new ProductResponseDTO();
            response.setProduct_id("prod-" + productId);
            response.setTitle(product.getTitle());
            response.setSpecification(product.getSpecification());
            response.setPrice(product.getPrice());
            response.setStock(product.getStock());
            response.setImages(request.getImages());
            response.setStatus(product.getStatus());
            response.setCreated_at(product.getCreatedAt());

            Map<String, String> links = new HashMap<>();
            links.put("self", "/api/v1/farmer/products/prod-" + productId);
            response.set_links(links);

            return response;
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // 商品上架
    @Override
    public ProductStatusUpdateResponseDTO onShelfProduct(String productId, String phone) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 验证用户
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户类型
            if (!"farmer".equals(user.getUserType())) {
                throw new IllegalArgumentException("只有农户可以操作商品");
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 获取商品当前状态
            long prodId = Long.parseLong(productId);
            String currentStatus = getProductStatus(conn, prodId, farmerId);

            // 检查状态是否允许上架
            if (!"off_shelf".equals(currentStatus) && !"review_rejected".equals(currentStatus)) {
                throw new IllegalStateException("当前商品状态无法执行上架操作");
            }

            // 更新商品状态为已上架
            updateProductStatus(conn, prodId, farmerId, "on_shelf");

            conn.commit();

            // 构建响应对象
            ProductStatusUpdateResponseDTO response = new ProductStatusUpdateResponseDTO();
            response.setProduct_id(String.valueOf(prodId));
            response.setStatus("on_shelf");

            return response;
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // 商品下架
    @Override
    public ProductStatusUpdateResponseDTO offShelfProduct(String productId, String phone) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 验证用户
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户类型
            if (!"farmer".equals(user.getUserType())) {
                throw new IllegalArgumentException("只有农户可以操作商品");
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 获取商品当前状态
            long prodId = Long.parseLong(productId);
            String currentStatus = getProductStatus(conn, prodId, farmerId);

            // 检查状态是否允许下架
            if (!"on_shelf".equals(currentStatus)) {
                throw new IllegalStateException("当前商品状态无法执行下架操作");
            }

            // 更新商品状态为已下架
            updateProductStatus(conn, prodId, farmerId, "off_shelf");

            conn.commit();

            // 构建响应对象
            ProductStatusUpdateResponseDTO response = new ProductStatusUpdateResponseDTO();
            response.setProduct_id(String.valueOf(prodId));
            response.setStatus("off_shelf");

            return response;
        } catch (Exception e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    // 根据用户ID获取农户ID
    private Long getFarmerIdByUserId(Connection conn, String userId) throws SQLException {
        String sql = "SELECT farmer_id FROM user_farmers WHERE uid = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, userId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getLong("farmer_id");
        }

        throw new SQLException("农户信息不存在");
    }

    private String getDefaultOrigin(Connection conn, Long farmerId) throws SQLException {
        String sql = "SELECT farm_address FROM user_farmers WHERE farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, farmerId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getString("farm_address");
        }

        return "";
    }

    private Long insertProduct(Connection conn, Product product) throws SQLException {
        String sql = "INSERT INTO products (farmer_id, category, title, specification, price, stock, description, origin, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, product.getFarmerId());
        stmt.setString(2, product.getCategory()); // 插入分类
        stmt.setString(3, product.getTitle());
        stmt.setString(4, product.getSpecification());
        stmt.setDouble(5, product.getPrice());
        stmt.setInt(6, product.getStock());
        stmt.setString(7, product.getDescription());
        stmt.setString(8, product.getOrigin());
        stmt.setString(9, product.getStatus());
        stmt.setTimestamp(10, Timestamp.valueOf(product.getCreatedAt()));

        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            return rs.getLong(1);
        }

        throw new SQLException("创建产品失败");
    }

    private void insertProductImages(Connection conn, Long productId, List<String> images) throws SQLException {
        String sql = "INSERT INTO product_images (product_id, image_url, sort_order) VALUES (?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);

        for (int i = 0; i < images.size(); i++) {
            stmt.setLong(1, productId);
            stmt.setString(2, images.get(i));
            stmt.setInt(3, i);
            stmt.addBatch();
        }

        stmt.executeBatch();
    }

    // 获取商品状态
    private String getProductStatus(Connection conn, long productId, Long farmerId) throws SQLException {
        String sql = "SELECT status FROM products WHERE product_id = ? AND farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.setLong(2, farmerId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getString("status");
        }

        throw new SQLException("商品不存在");
    }

    // 更新商品状态
    private void updateProductStatus(Connection conn, long productId, Long farmerId, String status) throws SQLException {
        String sql = "UPDATE products SET status = ? WHERE product_id = ? AND farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, status);
        stmt.setLong(2, productId);
        stmt.setLong(3, farmerId);
        int rowsAffected = stmt.executeUpdate();

        if (rowsAffected == 0) {
            throw new SQLException("商品不存在或不属于该农户");
        }
    }
}
