// src/service/farmer/ProductService.java
package service.farmer;

import dto.ProductCreateRequestDTO;
import dto.ProductResponseDTO;

public interface ProductService {
    ProductResponseDTO createProduct(ProductCreateRequestDTO request, String userId) throws Exception;
}
