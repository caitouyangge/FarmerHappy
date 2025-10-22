// src/service/farmer/ProductService.java
package service.farmer;

import dto.farmer.ProductCreateRequestDTO;
import dto.farmer.ProductResponseDTO;
import dto.farmer.ProductStatusUpdateResponseDTO;

public interface ProductService {
    ProductResponseDTO createProduct(ProductCreateRequestDTO request, String userId) throws Exception;

    // 添加商品状态变更方法
    ProductStatusUpdateResponseDTO onShelfProduct(String productId, String phone) throws Exception;
    ProductStatusUpdateResponseDTO offShelfProduct(String productId, String phone) throws Exception;
}
