package controller;

import dto.farmer.*;
import entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.auth.AuthService;
import service.farmer.ProductService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerIT {

    @Mock
    ProductService productService;
    @Mock
    AuthService authService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productService, authService);
    }

    @Test
    void get_product_detail_missing_phone_returns_400() {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        Map<String, Object> resp = controller.getProductDetail("1", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void get_product_list_missing_phone_returns_400() {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> resp = controller.getProductList(body);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void get_product_list_non_farmer_returns_403() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("phone", "13800138000");
        User user = new User();
        user.setUid("uid-1");
        when(authService.findUserByPhone("13800138000")).thenReturn(user);
        when(authService.checkUserTypeExists("uid-1", "farmer")).thenReturn(false);
        Map<String, Object> resp = controller.getProductList(body);
        assertThat(resp.get("code")).isEqualTo(403);
    }

    @Test
    void get_product_detail_sql_error_returns_500() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        when(productService.getProductDetail("9", "13800138000")).thenThrow(new SQLException("错误"));
        Map<String, Object> resp = controller.getProductDetail("9", req);
        assertThat(resp.get("code")).isEqualTo(500);
    }

    @Test
    void update_product_missing_phone_returns_400() {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        Map<String, Object> resp = controller.updateProduct("9", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void create_product_user_not_found_returns_400() throws Exception {
        ProductCreateRequestDTO req = new ProductCreateRequestDTO();
        req.setTitle("苹果E");
        req.setDetailedDescription("规格E");
        req.setPrice(10.0);
        req.setStock(5);
        req.setDescription("desc");
        req.setOrigin("河北");
        req.setImages(java.util.Arrays.asList("http://img/e1.jpg"));
        req.setCategory("fruits");
        req.setPhone("13900138000");

        when(authService.findUserByPhone("13900138000")).thenReturn(null);

        Map<String, Object> resp = controller.createProduct(req);
        assertThat(resp.get("code")).isEqualTo(400);
        assertThat(resp.get("message")).isEqualTo("参数验证失败");
        java.util.List<java.util.Map<String, String>> errors = (java.util.List<java.util.Map<String, String>>) resp
                .get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get(0).get("message")).isEqualTo("用户不存在");
    }

    @Test
    void create_product_missing_phone_returns_400() {
        ProductCreateRequestDTO req = new ProductCreateRequestDTO();
        req.setTitle("标题");
        req.setDetailedDescription("详细介绍");
        req.setPrice(12.5);
        req.setStock(10);
        req.setDescription("描述");
        req.setOrigin("产地");
        req.setImages(java.util.Arrays.asList("http://img/1.jpg"));
        req.setCategory("fruits");
        Map<String, Object> resp = controller.createProduct(req);
        assertThat(resp.get("code")).isEqualTo(400);
        java.util.List<java.util.Map<String, String>> errors = (java.util.List<java.util.Map<String, String>>) resp
                .get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get(0).get("message")).isEqualTo("手机号不能为空");
    }

    @Test
    void create_product_non_farmer_returns_403() throws Exception {
        ProductCreateRequestDTO req = new ProductCreateRequestDTO();
        req.setTitle("标题");
        req.setDetailedDescription("详细介绍");
        req.setPrice(12.5);
        req.setStock(10);
        req.setCategory("fruits");
        req.setPhone("13800138023");
        entity.User user = new entity.User();
        user.setUid("uid-x");
        when(authService.findUserByPhone("13800138023")).thenReturn(user);
        when(authService.checkUserTypeExists("uid-x", "farmer")).thenReturn(false);
        Map<String, Object> resp = controller.createProduct(req);
        assertThat(resp.get("code")).isEqualTo(403);
        assertThat(resp.get("message")).isEqualTo("只有农户可以发布商品");
    }

    @Test
    void update_product_non_farmer_returns_403() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138024");
        req.setTitle("新标题");
        req.setCategory("fruits");
        entity.User user = new entity.User();
        user.setUid("uid-y");
        when(authService.findUserByPhone("13800138024")).thenReturn(user);
        when(authService.checkUserTypeExists("uid-y", "farmer")).thenReturn(false);
        Map<String, Object> resp = controller.updateProduct("P-1", req);
        assertThat(resp.get("code")).isEqualTo(403);
        assertThat(resp.get("message")).isEqualTo("只有农户可以修改商品");
    }

    @Test
    void update_product_not_exist_returns_404() throws Exception {
        ProductUpdateRequestDTO req = new ProductUpdateRequestDTO();
        req.setPhone("13800138025");
        req.setTitle("新标题");
        req.setCategory("fruits");
        entity.User user = new entity.User();
        user.setUid("uid-z");
        when(authService.findUserByPhone("13800138025")).thenReturn(user);
        when(authService.checkUserTypeExists("uid-z", "farmer")).thenReturn(true);
        when(productService.partialUpdateProduct("P-404", req))
                .thenThrow(new java.sql.SQLException("商品不存在"));
        Map<String, Object> resp = controller.updateProduct("P-404", req);
        assertThat(resp.get("code")).isEqualTo(404);
        assertThat(resp.get("message")).isEqualTo("商品不存在");
    }

    @Test
    void on_shelf_not_exist_returns_404() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        when(productService.onShelfProduct("P-404", "13800138000"))
                .thenThrow(new java.sql.SQLException("商品不存在"));
        Map<String, Object> resp = controller.onShelfProduct("P-404", req);
        assertThat(resp.get("code")).isEqualTo(404);
        assertThat(resp.get("message")).isEqualTo("商品不存在");
    }

    @Test
    void off_shelf_not_exist_returns_404() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        when(productService.offShelfProduct("P-404", "13800138000"))
                .thenThrow(new java.sql.SQLException("商品不存在"));
        Map<String, Object> resp = controller.offShelfProduct("P-404", req);
        assertThat(resp.get("code")).isEqualTo(404);
        assertThat(resp.get("message")).isEqualTo("商品不存在");
    }

    @Test
    void batch_actions_missing_phone_returns_400() {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("delete");
        req.setProduct_ids(java.util.Arrays.asList("1", "2"));
        Map<String, Object> resp = controller.batchActionProducts(req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void batch_actions_non_farmer_returns_403() throws Exception {
        ProductBatchActionRequestDTO req = new ProductBatchActionRequestDTO();
        req.setAction("off-shelf");
        req.setProduct_ids(java.util.Arrays.asList("1"));
        req.setPhone("13800138026");
        entity.User user = new entity.User();
        user.setUid("uid-w");
        when(authService.findUserByPhone("13800138026")).thenReturn(user);
        when(authService.checkUserTypeExists("uid-w", "farmer")).thenReturn(false);
        Map<String, Object> resp = controller.batchActionProducts(req);
        assertThat(resp.get("code")).isEqualTo(403);
        assertThat(resp.get("message")).isEqualTo("只有农户可以执行商品操作");
    }

    @Test
    void on_shelf_missing_phone_returns_400() {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        Map<String, Object> resp = controller.onShelfProduct("9", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void on_shelf_illegal_state_returns_409() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        when(productService.onShelfProduct("9", "13800138000"))
                .thenThrow(new IllegalStateException("无法执行上架"));
        Map<String, Object> resp = controller.onShelfProduct("9", req);
        assertThat(resp.get("code")).isEqualTo(409);
    }

    @Test
    void off_shelf_illegal_argument_returns_400() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        when(productService.offShelfProduct("9", "13800138000"))
                .thenThrow(new IllegalArgumentException("只有农户可以操作商品"));
        Map<String, Object> resp = controller.offShelfProduct("9", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void delete_product_missing_phone_returns_400() {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        Map<String, Object> resp = controller.deleteProduct("9", req);
        assertThat(resp.get("code")).isEqualTo(400);
    }

    @Test
    void delete_product_not_exist_returns_404() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        org.mockito.Mockito.doThrow(new java.sql.SQLException("商品不存在"))
                .when(productService).deleteProduct("9", "13800138000");
        Map<String, Object> resp = controller.deleteProduct("9", req);
        assertThat(resp.get("code")).isEqualTo(404);
    }

    @Test
    void get_product_detail_not_exist_returns_404() throws Exception {
        ProductStatusUpdateRequestDTO req = new ProductStatusUpdateRequestDTO();
        req.setPhone("13800138000");
        when(productService.getProductDetail("999", "13800138000"))
                .thenThrow(new java.sql.SQLException("商品不存在"));
        Map<String, Object> resp = controller.getProductDetail("999", req);
        assertThat(resp.get("code")).isEqualTo(404);
    }
}
