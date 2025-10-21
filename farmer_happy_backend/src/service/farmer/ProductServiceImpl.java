// src/service/farmer/ProductServiceImpl.java
package service.farmer;

import dto.ProductCreateRequestDTO;
import dto.ProductResponseDTO;
import entity.Product;
import repository.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ProductServiceImpl implements ProductService {
    private DatabaseManager databaseManager;

    public ProductServiceImpl() {
        this.databaseManager = new DatabaseManager();
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
        String sql = "INSERT INTO products (farmer_id, title, specification, price, stock, description, origin, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, product.getFarmerId());
        stmt.setString(2, product.getTitle());
        stmt.setString(3, product.getSpecification());
        stmt.setDouble(4, product.getPrice());
        stmt.setInt(5, product.getStock());
        stmt.setString(6, product.getDescription());
        stmt.setString(7, product.getOrigin());
        stmt.setString(8, product.getStatus());
        stmt.setTimestamp(9, Timestamp.valueOf(product.getCreatedAt()));

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
}
