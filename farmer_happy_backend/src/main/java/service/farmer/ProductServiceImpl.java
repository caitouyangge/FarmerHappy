// src/service/farmer/ProductServiceImpl.java
package service.farmer;

import dto.farmer.*;
import entity.Product;
import repository.DatabaseManager;
import service.auth.AuthService;
import service.auth.AuthServiceImpl;

import java.util.ArrayList;
import java.util.List;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ProductServiceImpl implements ProductService {
    private DatabaseManager databaseManager;
    private AuthService authService;

    public ProductServiceImpl() {
        this.databaseManager = DatabaseManager.getInstance();
        this.authService = new AuthServiceImpl();
    }

    public ProductServiceImpl(DatabaseManager databaseManager, AuthService authService) {
        this.databaseManager = databaseManager;
        this.authService = authService;
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
            product.setCategory(request.getCategory());
            product.setTitle(request.getTitle());
            product.setDetailedDescription(request.getDetailedDescription());
            product.setPrice(request.getPrice());
            product.setStock(request.getStock());
            product.setDescription(request.getDescription());
            product.setOrigin(request.getOrigin());
            product.setStatus("on_shelf"); // 初始状态为已上架
            product.setEnable(true);
            product.setCreatedAt(LocalDateTime.now());

            long productId = insertProduct(conn, product);

            // 插入图片信息
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                insertProductImages(conn, productId, request.getImages());
            }

            conn.commit();

            // 构建响应对象
            ProductResponseDTO response = new ProductResponseDTO();
            response.setProduct_id(String.valueOf(productId));
            response.setTitle(product.getTitle());
            response.setDetailedDescription(product.getDetailedDescription());
            response.setPrice(product.getPrice());
            response.setStock(product.getStock());
            response.setImages(request.getImages());
            response.setStatus(product.getStatus());
            response.setCreated_at(product.getCreatedAt());

            // 设置链接
            Map<String, String> links = new HashMap<>();
            links.put("self", "/api/v1/farmer/products/" + productId);
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

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以操作商品");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 获取商品当前状态
            long prodId = Long.parseLong(productId);
            String currentStatus = getProductStatus(conn, prodId, farmerId);

            // 检查状态是否允许上架
            if (!"off_shelf".equals(currentStatus) && !"pending_review".equals(currentStatus)) {
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

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以操作商品");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
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

    // 删除商品
    @Override
    public void deleteProduct(String productId, String phone) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 验证用户
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以操作商品");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 删除商品
            long prodId = Long.parseLong(productId);
            deleteProductById(conn, prodId, farmerId);

            conn.commit();
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

    // 获取单个商品详情
    @Override
    public ProductDetailResponseDTO getProductDetail(String productId, String phone) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();

            // 验证用户
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 检查用户类型
            boolean isFarmer = false;
            Long farmerId = null;
            try {
                isFarmer = authService.checkUserTypeExists(user.getUid(), "farmer");
                if (isFarmer) {
                    // 如果是农户，获取农户ID，用于验证商品是否属于该农户
                    farmerId = getFarmerIdByUserId(conn, user.getUid());
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 获取商品详情
            long prodId = Long.parseLong(productId);
            ProductDetailResponseDTO productDetail;
            
            if (isFarmer && farmerId != null) {
                // 农户只能查看自己的商品
                productDetail = getProductDetailById(conn, prodId, farmerId);
            } else {
                // 买家可以查看所有在售商品（不限制农户）
                productDetail = getProductDetailByIdForBuyer(conn, prodId);
            }

            return productDetail;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    // 获取商品列表
    @Override
    public List<ProductListResponseDTO> getProductList(String phone, String status, String title) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();

            // 验证用户 - 直接使用手机号查询
            entity.User user = authService.findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以操作商品");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 构建查询语句
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder
                    .append("SELECT p.product_id, p.title, p.price, p.stock, p.status, pi.image_url as main_image_url ")
                    .append("FROM products p ")
                    .append("LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.sort_order = 0 ")
                    .append("WHERE p.farmer_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add(farmerId);

            // 添加状态筛选条件
            if (status != null && !status.isEmpty()) {
                sqlBuilder.append("AND p.status = ? ");
                params.add(status);
            }

            // 添加标题搜索条件
            if (title != null && !title.isEmpty()) {
                sqlBuilder.append("AND p.title LIKE ? ");
                params.add("%" + title + "%");
            }

            sqlBuilder.append("ORDER BY p.created_at DESC");

            PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString());

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();

            List<ProductListResponseDTO> productList = new ArrayList<>();
            while (rs.next()) {
                ProductListResponseDTO product = new ProductListResponseDTO();
                product.setProduct_id(String.valueOf(rs.getLong("product_id")));
                product.setTitle(rs.getString("title"));
                product.setPrice(rs.getDouble("price"));
                product.setStock(rs.getInt("stock"));
                product.setStatus(rs.getString("status"));
                product.setMain_image_url(rs.getString("main_image_url"));
                productList.add(product);
            }

            return productList;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    // 获取所有在售商品（用于广告）
    @Override
    public List<ProductListResponseDTO> getAllOnShelfProducts() throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();

            // 构建查询语句 - 查询所有在售商品的基本信息
            String sql = "SELECT p.product_id, p.title, p.price, p.stock, p.status, p.detailed_description " +
                    "FROM products p " +
                    "WHERE p.status = 'on_shelf' AND p.stock > 0 AND p.enable = TRUE " +
                    "ORDER BY p.created_at DESC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            List<ProductListResponseDTO> productList = new ArrayList<>();
            while (rs.next()) {
                ProductListResponseDTO product = new ProductListResponseDTO();
                long productId = rs.getLong("product_id");
                product.setProduct_id(String.valueOf(productId));
                product.setTitle(rs.getString("title"));
                product.setPrice(rs.getDouble("price"));
                product.setStock(rs.getInt("stock"));
                product.setStatus(rs.getString("status"));
                
                // 获取商品的所有图片
                String imgSql = "SELECT image_url FROM product_images WHERE product_id = ? ORDER BY sort_order";
                PreparedStatement imgStmt = conn.prepareStatement(imgSql);
                imgStmt.setLong(1, productId);
                ResultSet imgRs = imgStmt.executeQuery();
                
                List<String> images = new ArrayList<>();
                int imageCount = 0;
                while (imgRs.next()) {
                    String imageUrl = imgRs.getString("image_url");
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        images.add(imageUrl);
                        imageCount++;
                    }
                }
                imgRs.close();
                imgStmt.close();
                
                // 调试日志
                System.out.println("商品ID: " + productId + ", 查询到的图片数量: " + imageCount + ", 图片列表: " + images);
                
                // 设置主图和所有图片
                if (!images.isEmpty()) {
                    product.setMain_image_url(images.get(0));
                    product.setImages(images);
                } else {
                    // 如果没有图片，设置空列表
                    product.setImages(new ArrayList<>());
                }
                
                // 设置详细介绍
                String detailedDesc = rs.getString("detailed_description");
                product.setDetailed_description(detailedDesc);
                
                productList.add(product);
            }

            rs.close();
            stmt.close();
            return productList;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    // 完整更新商品
    @Override
    public ProductResponseDTO updateProduct(String productId, ProductUpdateRequestDTO request) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 验证用户
            entity.User user = authService.findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以操作商品");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 获取商品当前信息
            long prodId = Long.parseLong(productId);
            Product existingProduct = getProductById(conn, prodId, farmerId);

            // 更新商品信息
            Product product = new Product();
            product.setProductId(prodId);
            product.setFarmerId(farmerId);
            product.setCategory(request.getCategory() != null ? request.getCategory() : existingProduct.getCategory());
            product.setTitle(request.getTitle() != null ? request.getTitle() : existingProduct.getTitle());
            product.setDetailedDescription(request.getDetailedDescription() != null ? request.getDetailedDescription()
                    : existingProduct.getDetailedDescription());
            product.setPrice(request.getPrice() != null ? request.getPrice() : existingProduct.getPrice());
            product.setStock(request.getStock() != null ? request.getStock() : existingProduct.getStock());
            product.setDescription(
                    request.getDescription() != null ? request.getDescription() : existingProduct.getDescription());
            product.setOrigin(request.getOrigin() != null ? request.getOrigin() : existingProduct.getOrigin());
            product.setStatus(existingProduct.getStatus()); // 状态不通过此接口修改
            product.setEnable(existingProduct.isEnable());
            product.setCreatedAt(existingProduct.getCreatedAt());

            // 更新商品
            updateProduct(conn, product);

            // 更新图片信息
            deleteProductImages(conn, prodId);
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                insertProductImages(conn, prodId, request.getImages());
            }

            conn.commit();

            // 构建响应对象
            ProductResponseDTO response = new ProductResponseDTO();
            response.setProduct_id(String.valueOf(prodId));
            response.setTitle(product.getTitle());
            response.setDetailedDescription(product.getDetailedDescription());
            response.setPrice(product.getPrice());
            response.setStock(product.getStock());
            response.setImages(request.getImages());
            response.setStatus(product.getStatus());
            response.setCreated_at(product.getCreatedAt());

            // 设置链接
            Map<String, String> links = new HashMap<>();
            links.put("self", "/api/v1/farmer/products/" + prodId);
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

    @Override
    public ProductBatchActionResultDTO batchActionProducts(ProductBatchActionRequestDTO request) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 验证用户
            entity.User user = authService.findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以执行商品操作");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 验证参数
            if (request.getProduct_ids() == null || request.getProduct_ids().isEmpty()) {
                throw new IllegalArgumentException("商品ID列表不能为空");
            }

            if (request.getProduct_ids().size() > 100) {
                throw new IllegalArgumentException("一次最多只能操作100个商品");
            }

            String normalizedAction = request.getAction() == null ? null : request.getAction().replace('-', '_');
            if (normalizedAction == null ||
                    (!"on_shelf".equals(normalizedAction) &&
                            !"off_shelf".equals(normalizedAction) &&
                            !"delete".equals(normalizedAction))) {
                throw new IllegalArgumentException("无效的操作类型");
            }
            request.setAction(normalizedAction);

            ProductBatchActionResultDTO result = new ProductBatchActionResultDTO();
            List<ProductBatchActionResultDTO.BatchActionResultItem> items = new ArrayList<>();

            int successCount = 0;
            int failureCount = 0;

            for (String productId : request.getProduct_ids()) {
                ProductBatchActionResultDTO.BatchActionResultItem item = new ProductBatchActionResultDTO.BatchActionResultItem();
                item.setProduct_id(productId);

                try {
                    // 解析产品ID
                    long prodId = Long.parseLong(productId);

                    // 验证商品是否属于该农户
                    if (!isProductOwnedByFarmer(conn, prodId, user.getUid())) {
                        throw new IllegalArgumentException("商品不存在或不属于该农户");
                    }

                    // 根据操作类型执行不同操作
                    switch (request.getAction()) {
                        case "on_shelf":
                            onShelfProductInternal(conn, prodId);
                            item.setMessage("上架成功");
                            break;
                        case "off_shelf":
                            offShelfProductInternal(conn, prodId);
                            item.setMessage("下架成功");
                            break;
                        case "delete":
                            deleteProductInternal(conn, prodId);
                            item.setMessage("删除成功");
                            break;
                    }

                    item.setSuccess(true);
                    successCount++;

                    // 设置链接
                    Map<String, String> links = new HashMap<>();
                    if (!"delete".equals(request.getAction())) {
                        links.put("self", "/api/v1/farmer/products/" + productId);
                    } else {
                        links.put("self", null);
                    }
                    item.set_links(links);

                } catch (Exception e) {
                    item.setSuccess(false);
                    item.setMessage(e.getMessage());

                    Map<String, String> links = new HashMap<>();
                    links.put("self", null);
                    item.set_links(links);

                    failureCount++;
                }

                items.add(item);
            }

            result.setSuccess_count(successCount);
            result.setFailure_count(failureCount);
            result.setResults(items);

            conn.commit();
            return result;

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

    // 辅助方法：检查商品是否属于指定农户
    private boolean isProductOwnedByFarmer(Connection conn, long productId, String uid) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM products p " +
                "JOIN user_farmers f ON p.farmer_id = f.farmer_id " +
                "WHERE p.product_id = ? AND f.uid = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.setString(2, uid);

        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("count") > 0;
        }
        return false;
    }

    // 下架
    private void offShelfProductInternal(Connection conn, long productId) throws SQLException {
        String sql = "UPDATE products SET status = 'off_shelf' WHERE product_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        int rowsAffected = stmt.executeUpdate();

        if (rowsAffected == 0) {
            throw new SQLException("商品不存在");
        }
    }

    // 上架
    private void onShelfProductInternal(Connection conn, long productId) throws SQLException {
        String sql = "UPDATE products SET status = 'on_shelf' WHERE product_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        int rowsAffected = stmt.executeUpdate();

        if (rowsAffected == 0) {
            throw new SQLException("商品不存在");
        }
    }

    // 辅助方法：删除商品
    private void deleteProductInternal(Connection conn, long productId) throws SQLException {
        // 删除商品图片
        String deleteImagesSql = "DELETE FROM product_images WHERE product_id = ?";
        PreparedStatement deleteImagesStmt = conn.prepareStatement(deleteImagesSql);
        deleteImagesStmt.setLong(1, productId);
        deleteImagesStmt.executeUpdate();

        // 删除商品
        String deleteProductSql = "DELETE FROM products WHERE product_id = ?";
        PreparedStatement deleteProductStmt = conn.prepareStatement(deleteProductSql);
        deleteProductStmt.setLong(1, productId);
        int rowsAffected = deleteProductStmt.executeUpdate();

        if (rowsAffected == 0) {
            throw new SQLException("商品不存在");
        }
    }

    // 部分更新商品
    @Override
    public ProductResponseDTO partialUpdateProduct(String productId, ProductUpdateRequestDTO request) throws Exception {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            // 验证用户
            entity.User user = authService.findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 验证用户是否具有农户身份
            try {
                if (!authService.checkUserTypeExists(user.getUid(), "farmer")) {
                    throw new IllegalArgumentException("只有农户可以操作商品");
                }
            } catch (SQLException e) {
                throw new SQLException("验证用户身份失败: " + e.getMessage());
            }

            // 获取农户ID
            Long farmerId = getFarmerIdByUserId(conn, user.getUid());

            // 获取商品当前信息
            long prodId = Long.parseLong(productId);
            Product existingProduct = getProductById(conn, prodId, farmerId);

            // 创建要更新的商品对象，只更新请求中包含的字段
            Product product = new Product();
            product.setProductId(prodId);
            product.setFarmerId(farmerId);

            // 只有当请求中的字段不为null时才更新
            product.setCategory(request.getCategory() != null ? request.getCategory() : existingProduct.getCategory());
            product.setTitle(request.getTitle() != null ? request.getTitle() : existingProduct.getTitle());
            product.setDetailedDescription(request.getDetailedDescription() != null ? request.getDetailedDescription()
                    : existingProduct.getDetailedDescription());
            product.setPrice(request.getPrice() != null ? request.getPrice() : existingProduct.getPrice());
            product.setStock(request.getStock() != null ? request.getStock() : existingProduct.getStock());
            product.setDescription(
                    request.getDescription() != null ? request.getDescription() : existingProduct.getDescription());
            product.setOrigin(request.getOrigin() != null ? request.getOrigin() : existingProduct.getOrigin());
            product.setStatus(existingProduct.getStatus()); // 状态不通过此接口修改
            product.setEnable(existingProduct.isEnable());
            product.setCreatedAt(existingProduct.getCreatedAt());

            // 更新商品
            updateProduct(conn, product);

            // 如果请求中包含图片，则更新图片信息
            if (request.getImages() != null) {
                deleteProductImages(conn, prodId);
                if (!request.getImages().isEmpty()) {
                    insertProductImages(conn, prodId, request.getImages());
                }
            }

            conn.commit();

            // 构建响应对象
            ProductResponseDTO response = new ProductResponseDTO();
            response.setProduct_id(String.valueOf(prodId));
            response.setTitle(product.getTitle());
            response.setDetailedDescription(product.getDetailedDescription());
            response.setPrice(product.getPrice());
            response.setStock(product.getStock());

            // 获取最新的图片列表
            List<String> images = request.getImages() != null ? request.getImages() : getProductImages(conn, prodId);
            response.setImages(images);

            response.setStatus(product.getStatus());
            response.setCreated_at(product.getCreatedAt());

            // 设置链接
            Map<String, String> links = new HashMap<>();
            links.put("self", "/api/v1/farmer/products/" + prodId);
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

    // 插入产品信息
    private long insertProduct(Connection conn, Product product) throws SQLException {
        String sql = "INSERT INTO products (farmer_id, category, title, detailed_description, price, stock, description, origin, status, enable, created_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, product.getFarmerId());
        stmt.setString(2, product.getCategory());
        stmt.setString(3, product.getTitle());
        stmt.setString(4, product.getDetailedDescription());
        stmt.setDouble(5, product.getPrice());
        stmt.setInt(6, product.getStock());
        stmt.setString(7, product.getDescription());
        stmt.setString(8, product.getOrigin());
        stmt.setString(9, product.getStatus());
        stmt.setBoolean(10, product.isEnable());
        stmt.setTimestamp(11, Timestamp.valueOf(product.getCreatedAt()));

        stmt.executeUpdate();

        // 获取生成的产品ID
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getLong(1);
        } else {
            throw new SQLException("创建产品失败，无法获取产品ID");
        }
    }

    // 插入产品图片信息
    private void insertProductImages(Connection conn, long productId, List<String> images) throws SQLException {
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
    private void updateProductStatus(Connection conn, long productId, Long farmerId, String status)
            throws SQLException {
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

    // 删除商品
    private void deleteProductById(Connection conn, long productId, Long farmerId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ? AND farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.setLong(2, farmerId);
        int rowsAffected = stmt.executeUpdate();

        if (rowsAffected == 0) {
            throw new SQLException("商品不存在或不属于该农户");
        }
    }

    // 获取商品详情（农户版本：只能查看自己的商品）
    private ProductDetailResponseDTO getProductDetailById(Connection conn, long productId, Long farmerId)
            throws SQLException {
        String sql = "SELECT p.*, pi.image_url FROM products p " +
                "LEFT JOIN product_images pi ON p.product_id = pi.product_id " +
                "WHERE p.product_id = ? AND p.farmer_id = ? " +
                "ORDER BY pi.sort_order";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.setLong(2, farmerId);
        ResultSet rs = stmt.executeQuery();

        ProductDetailResponseDTO productDetail = null;
        List<String> images = new ArrayList<>();

        while (rs.next()) {
            if (productDetail == null) {
                productDetail = new ProductDetailResponseDTO();
                productDetail.setProduct_id(String.valueOf(rs.getLong("product_id")));
                productDetail.setTitle(rs.getString("title"));
                productDetail.setDetailedDescription(rs.getString("detailed_description"));
                productDetail.setPrice(rs.getDouble("price"));
                productDetail.setStock(rs.getInt("stock"));
                productDetail.setDescription(rs.getString("description"));
                productDetail.setOrigin(rs.getString("origin"));
                productDetail.setStatus(rs.getString("status"));
                productDetail.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
            }

            String imageUrl = rs.getString("image_url");
            if (imageUrl != null) {
                images.add(imageUrl);
            }
        }

        if (productDetail == null) {
            throw new SQLException("商品不存在或不属于该农户");
        }

        productDetail.setImages(images);
        return productDetail;
    }

    // 获取商品详情（买家版本：可以查看所有在售商品）
    private ProductDetailResponseDTO getProductDetailByIdForBuyer(Connection conn, long productId)
            throws SQLException {
        String sql = "SELECT p.*, pi.image_url FROM products p " +
                "LEFT JOIN product_images pi ON p.product_id = pi.product_id " +
                "WHERE p.product_id = ? " +
                "ORDER BY pi.sort_order";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        ResultSet rs = stmt.executeQuery();

        ProductDetailResponseDTO productDetail = null;
        List<String> images = new ArrayList<>();

        while (rs.next()) {
            if (productDetail == null) {
                productDetail = new ProductDetailResponseDTO();
                productDetail.setProduct_id(String.valueOf(rs.getLong("product_id")));
                productDetail.setTitle(rs.getString("title"));
                productDetail.setDetailedDescription(rs.getString("detailed_description"));
                productDetail.setPrice(rs.getDouble("price"));
                productDetail.setStock(rs.getInt("stock"));
                productDetail.setDescription(rs.getString("description"));
                productDetail.setOrigin(rs.getString("origin"));
                productDetail.setStatus(rs.getString("status"));
                productDetail.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
            }

            String imageUrl = rs.getString("image_url");
            if (imageUrl != null) {
                images.add(imageUrl);
            }
        }

        if (productDetail == null) {
            throw new SQLException("商品不存在");
        }

        productDetail.setImages(images);
        return productDetail;
    }

    // 更新商品信息
    private void updateProduct(Connection conn, Product product) throws SQLException {
        String sql = "UPDATE products SET category = ?, title = ?, detailed_description = ?, price = ?, stock = ?, " +
                "description = ?, origin = ? WHERE product_id = ? AND farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, product.getCategory());
        stmt.setString(2, product.getTitle());
        stmt.setString(3, product.getDetailedDescription());
        stmt.setDouble(4, product.getPrice());
        stmt.setInt(5, product.getStock());
        stmt.setString(6, product.getDescription());
        stmt.setString(7, product.getOrigin());
        stmt.setLong(8, product.getProductId());
        stmt.setLong(9, product.getFarmerId());

        int rowsAffected = stmt.executeUpdate();
        if (rowsAffected == 0) {
            throw new SQLException("商品不存在或不属于该农户");
        }
    }

    // 删除商品图片
    private void deleteProductImages(Connection conn, long productId) throws SQLException {
        String sql = "DELETE FROM product_images WHERE product_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.executeUpdate();
    }

    // 获取商品创建时间
    private LocalDateTime getProductCreatedAt(Connection conn, long productId, Long farmerId) throws SQLException {
        String sql = "SELECT created_at FROM products WHERE product_id = ? AND farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.setLong(2, farmerId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getTimestamp("created_at").toLocalDateTime();
        }

        throw new SQLException("商品不存在");
    }

    // 获取商品信息
    private Product getProductById(Connection conn, long productId, Long farmerId) throws SQLException {
        String sql = "SELECT * FROM products WHERE product_id = ? AND farmer_id = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        stmt.setLong(2, farmerId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            Product product = new Product();
            product.setProductId(rs.getLong("product_id"));
            product.setFarmerId(rs.getLong("farmer_id"));
            product.setCategory(rs.getString("category"));
            product.setTitle(rs.getString("title"));
            product.setDetailedDescription(rs.getString("detailed_description"));
            product.setPrice(rs.getDouble("price"));
            product.setStock(rs.getInt("stock"));
            product.setDescription(rs.getString("description"));
            product.setOrigin(rs.getString("origin"));
            product.setStatus(rs.getString("status"));
            product.setEnable(rs.getBoolean("enable"));
            product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return product;
        }

        throw new SQLException("商品不存在或不属于该农户");
    }

    // 获取商品图片列表
    private List<String> getProductImages(Connection conn, long productId) throws SQLException {
        String sql = "SELECT image_url FROM product_images WHERE product_id = ? ORDER BY sort_order";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, productId);
        ResultSet rs = stmt.executeQuery();

        List<String> images = new ArrayList<>();
        while (rs.next()) {
            images.add(rs.getString("image_url"));
        }
        return images;
    }
}
