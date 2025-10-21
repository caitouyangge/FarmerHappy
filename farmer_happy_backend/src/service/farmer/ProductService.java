// src/service/farmer/ProductService.java
package service.farmer;

import dto.farmer.ProductCreateRequestDTO;
import dto.farmer.ProductResponseDTO;

public interface ProductService {
    ProductResponseDTO createProduct(ProductCreateRequestDTO request, String userId) throws Exception;
}
