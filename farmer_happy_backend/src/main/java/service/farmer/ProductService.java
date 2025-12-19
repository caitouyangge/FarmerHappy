// src/service/farmer/ProductService.java
package service.farmer;

import dto.farmer.*;
import java.util.List;

public interface ProductService {
    ProductResponseDTO createProduct(ProductCreateRequestDTO request, String userId) throws Exception;

    // 添加商品状态变更方法
    ProductStatusUpdateResponseDTO onShelfProduct(String productId, String phone) throws Exception;
    ProductStatusUpdateResponseDTO offShelfProduct(String productId, String phone) throws Exception;

    // 添加删除商品方法
    void deleteProduct(String productId, String phone) throws Exception;

    // 添加获取单个商品详情方法
    ProductDetailResponseDTO getProductDetail(String productId, String phone) throws Exception;

    // 添加更新商品方法
    ProductResponseDTO updateProduct(String productId, ProductUpdateRequestDTO request) throws Exception;

    // 添加部分更新商品方法
    ProductResponseDTO partialUpdateProduct(String productId, ProductUpdateRequestDTO request) throws Exception;

    // 获取商品列表
    List<ProductListResponseDTO> getProductList(String phone, String status, String title) throws Exception;

    // 批量操作方法
    ProductBatchActionResultDTO batchActionProducts(ProductBatchActionRequestDTO request) throws Exception;

    // 获取所有在售商品（用于广告）
    List<ProductListResponseDTO> getAllOnShelfProducts() throws Exception;

}
