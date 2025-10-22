// src/service/farmer/ProductService.java
package service.farmer;

import dto.farmer.ProductCreateRequestDTO;
import dto.farmer.ProductResponseDTO;
import dto.farmer.ProductStatusUpdateResponseDTO;
import dto.farmer.ProductDetailResponseDTO;
import dto.farmer.ProductUpdateRequestDTO;

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
}
