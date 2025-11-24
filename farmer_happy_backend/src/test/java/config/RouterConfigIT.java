package config;

import controller.AuthController;
import controller.ProductController;
import controller.ContentController;
import controller.CommentController;
import controller.OrderController;
import dto.auth.LoginRequestDTO;
import dto.farmer.ProductStatusUpdateRequestDTO;
import dto.buyer.CreateOrderRequestDTO;
import dto.buyer.UpdateOrderRequestDTO;
import dto.buyer.QueryOrderRequestDTO;
import dto.buyer.RefundRequestDTO;
import dto.buyer.ConfirmReceiptRequestDTO;
import dto.farmer.ProductUpdateRequestDTO;
import dto.farmer.ProductBatchActionRequestDTO;
import dto.community.PublishContentRequestDTO;
import dto.community.PostCommentRequestDTO;
import dto.community.PostReplyRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouterConfigIT {

        @Mock
        AuthController authController;
        @Mock
        ProductController productController;
        @Mock
        ContentController contentController;
        @Mock
        CommentController commentController;
        @Mock
        OrderController orderController;

        private RouterConfig router;

        @BeforeEach
        void setUp() {
                router = new RouterConfig(authController, productController, contentController, commentController,
                                orderController);
        }

        @Test
        void login_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138000");
                body.put("password", "Abcdef12");
                body.put("user_type", "farmer");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(authController.login(any(LoginRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/auth/login", "POST", body, new HashMap<>(),
                                new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<LoginRequestDTO> captor = ArgumentCaptor.forClass(LoginRequestDTO.class);
                verify(authController).login(captor.capture());
                LoginRequestDTO dto = captor.getValue();
                assertThat(dto.getPhone()).isEqualTo("13800138000");
                assertThat(dto.getPassword()).isEqualTo("Abcdef12");
                assertThat(dto.getUserType()).isEqualTo("farmer");
        }

        @Test
        void product_on_shelf_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138000");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(productController.onShelfProduct(any(String.class), any(ProductStatusUpdateRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/100/on-shelf", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<ProductStatusUpdateRequestDTO> reqCaptor = ArgumentCaptor
                                .forClass(ProductStatusUpdateRequestDTO.class);
                verify(productController).onShelfProduct(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("100");
                assertThat(reqCaptor.getValue().getPhone()).isEqualTo("13800138000");
        }

        @Test
        void product_off_shelf_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138000");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(productController.offShelfProduct(any(String.class), any(ProductStatusUpdateRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/100/off-shelf", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<ProductStatusUpdateRequestDTO> reqCaptor = ArgumentCaptor
                                .forClass(ProductStatusUpdateRequestDTO.class);
                verify(productController).offShelfProduct(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("100");
                assertThat(reqCaptor.getValue().getPhone()).isEqualTo("13800138000");
        }

        @Test
        void content_list_query_param_parsing() {
                Map<String, String> query = new HashMap<>();
                query.put("content_type", "articles");
                query.put("keyword", "苹果");
                query.put("sort", "created_at");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(contentController.getContentList("articles", "苹果", "created_at")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/content/list", "GET", new HashMap<>(),
                                new HashMap<>(),
                                query);
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void create_order_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("product_id", "200");
                body.put("quantity", 2);
                body.put("buyer_name", "李四");
                body.put("buyer_address", "北京市朝阳区");
                body.put("buyer_phone", "13800138001");
                body.put("remark", "尽快发货");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(orderController.createOrder(any(CreateOrderRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/buyer/orders", "POST", body, new HashMap<>(),
                                new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);

                ArgumentCaptor<CreateOrderRequestDTO> dtoCaptor = ArgumentCaptor.forClass(CreateOrderRequestDTO.class);
                verify(orderController).createOrder(dtoCaptor.capture());
                CreateOrderRequestDTO dto = dtoCaptor.getValue();
                assertThat(dto.getProductId()).isEqualTo("200");
                assertThat(dto.getQuantity()).isEqualTo(2);
                assertThat(dto.getBuyerName()).isEqualTo("李四");
                assertThat(dto.getBuyerAddress()).isEqualTo("北京市朝阳区");
                assertThat(dto.getBuyerPhone()).isEqualTo("13800138001");
                assertThat(dto.getRemark()).isEqualTo("尽快发货");
        }

        @Test
        void auth_balance_query_parsing_and_dispatch() {
                Map<String, String> query = new HashMap<>();
                query.put("phone", "13800138020");
                query.put("user_type", "buyer");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(authController.getBalance("13800138020", "buyer")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/auth/balance", "GET", new HashMap<>(),
                                new HashMap<>(), query);
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void auth_balance_body_fallback_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138021");
                body.put("user_type", "farmer");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(authController.getBalance("13800138021", "farmer")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/auth/balance", "GET", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void create_product_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("title", "苹果F");
                body.put("detailed_description", "规格F");
                body.put("price", 9.99);
                body.put("stock", 7);
                body.put("description", "desc");
                body.put("origin", "河北");
                body.put("phone", "13800138022");
                body.put("category", "fruits");
                body.put("images", java.util.Arrays.asList("http://img/f1.jpg", "http://img/f2.jpg"));

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(productController.createProduct(any(dto.farmer.ProductCreateRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);

                org.mockito.ArgumentCaptor<dto.farmer.ProductCreateRequestDTO> captor =
                                org.mockito.ArgumentCaptor.forClass(dto.farmer.ProductCreateRequestDTO.class);
                verify(productController).createProduct(captor.capture());
                dto.farmer.ProductCreateRequestDTO dto = captor.getValue();
                assertThat(dto.getTitle()).isEqualTo("苹果F");
                assertThat(dto.getDetailedDescription()).isEqualTo("规格F");
                assertThat(dto.getPrice()).isEqualTo(9.99);
                assertThat(dto.getStock()).isEqualTo(7);
                assertThat(dto.getDescription()).isEqualTo("desc");
                assertThat(dto.getOrigin()).isEqualTo("河北");
                assertThat(dto.getPhone()).isEqualTo("13800138022");
                assertThat(dto.getCategory()).isEqualTo("fruits");
                assertThat(dto.getImages()).containsExactly("http://img/f1.jpg", "http://img/f2.jpg");
        }

        @Test
        void update_order_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("buyer_name", "张三");
                body.put("buyer_address", "上海市浦东新区");
                body.put("buyer_phone", "13800138001");
                body.put("remark", "修改地址");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.updateOrder(any(String.class), any(UpdateOrderRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/buyer/orders/ORD-100", "PUT", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<UpdateOrderRequestDTO> reqCaptor = ArgumentCaptor.forClass(UpdateOrderRequestDTO.class);
                verify(orderController).updateOrder(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("ORD-100");
                assertThat(reqCaptor.getValue().getBuyerName()).isEqualTo("张三");
                assertThat(reqCaptor.getValue().getBuyerAddress()).isEqualTo("上海市浦东新区");
                assertThat(reqCaptor.getValue().getBuyerPhone()).isEqualTo("13800138001");
                assertThat(reqCaptor.getValue().getRemark()).isEqualTo("修改地址");
        }

        @Test
        void get_order_detail_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("buyer_phone", "13800138001");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.getOrderDetail(any(String.class), any(QueryOrderRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/buyer/orders/query/ORD-200", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<QueryOrderRequestDTO> reqCaptor = ArgumentCaptor.forClass(QueryOrderRequestDTO.class);
                verify(orderController).getOrderDetail(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("ORD-200");
                assertThat(reqCaptor.getValue().getBuyerPhone()).isEqualTo("13800138001");
        }

        @Test
        void refund_order_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("buyer_phone", "13800138001");
                body.put("refund_type", "return_and_refund");
                body.put("refund_reason", "质量问题");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.applyRefund(any(String.class), any(RefundRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/buyer/orders/ORD-300/refund", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<RefundRequestDTO> reqCaptor = ArgumentCaptor.forClass(RefundRequestDTO.class);
                verify(orderController).applyRefund(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("ORD-300");
                assertThat(reqCaptor.getValue().getBuyerPhone()).isEqualTo("13800138001");
                assertThat(reqCaptor.getValue().getRefundType()).isEqualTo("return_and_refund");
                assertThat(reqCaptor.getValue().getRefundReason()).isEqualTo("质量问题");
        }

        @Test
        void confirm_receipt_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("buyer_phone", "13800138001");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.confirmReceipt(any(String.class), any(ConfirmReceiptRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/buyer/orders/ORD-400/confirm_receipt", "POST",
                                body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<ConfirmReceiptRequestDTO> reqCaptor = ArgumentCaptor
                                .forClass(ConfirmReceiptRequestDTO.class);
                verify(orderController).confirmReceipt(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("ORD-400");
                assertThat(reqCaptor.getValue().getBuyerPhone()).isEqualTo("13800138001");
        }

        @Test
        void farmer_order_detail_phone_from_body_then_query_fallback() {
                Map<String, Object> body = new HashMap<>();
                body.put("farmer_phone", "13800138002");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.getFarmerOrderDetail(any(String.class), any(String.class))).thenReturn(expected);

                Map<String, Object> resp1 = router.handleRequest("/api/v1/farmer/orders/query/ORD-1", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp1.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor1 = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> phoneCaptor1 = ArgumentCaptor.forClass(String.class);
                verify(orderController).getFarmerOrderDetail(idCaptor1.capture(), phoneCaptor1.capture());
                assertThat(idCaptor1.getValue()).isEqualTo("ORD-1");
                assertThat(phoneCaptor1.getValue()).isEqualTo("13800138002");
        }

        @Test
        void farmer_order_detail_phone_from_query_when_body_missing() {
                Map<String, String> query = new HashMap<>();
                query.put("farmer_phone", "13800138003");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.getFarmerOrderDetail(any(String.class), any(String.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/orders/query/ORD-2", "POST",
                                new HashMap<>(),
                                new HashMap<>(), query);
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void farmer_order_list_query_params_dispatch() {
                Map<String, String> query = new HashMap<>();
                query.put("farmer_phone", "13800138002");
                query.put("status", "shipped");
                query.put("title", "苹果");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.getFarmerOrderList("13800138002", "shipped", "苹果")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/orders/list_query", "POST",
                                new HashMap<>(),
                                new HashMap<>(), query);
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void buyer_order_list_query_params() {
                Map<String, String> query = new HashMap<>();
                query.put("buyer_phone", "13800138001");
                query.put("status", "shipped");
                query.put("title", "苹果");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(orderController.getOrderList("13800138001", "shipped", "苹果")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/buyer/orders/list_query", "POST",
                                new HashMap<>(),
                                new HashMap<>(), query);
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void delete_product_dispatch_and_status_204() {
                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 204);
                when(productController.deleteProduct(any(String.class), any(ProductStatusUpdateRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/100", "DELETE",
                                new HashMap<>(),
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(204);
        }

        @Test
        void get_product_detail_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138000");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(productController.getProductDetail(any(String.class), any(ProductStatusUpdateRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/query/555", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<ProductStatusUpdateRequestDTO> reqCaptor = ArgumentCaptor
                                .forClass(ProductStatusUpdateRequestDTO.class);
                verify(productController).getProductDetail(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("555");
                assertThat(reqCaptor.getValue().getPhone()).isEqualTo("13800138000");
        }

        @Test
        void update_product_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138000");
                body.put("title", "新标题");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(productController.updateProduct(any(String.class), any(ProductUpdateRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/777", "PUT", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<ProductUpdateRequestDTO> reqCaptor = ArgumentCaptor
                                .forClass(ProductUpdateRequestDTO.class);
                verify(productController).updateProduct(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("777");
                assertThat(reqCaptor.getValue().getPhone()).isEqualTo("13800138000");
                assertThat(reqCaptor.getValue().getTitle()).isEqualTo("新标题");
        }

        @Test
        void product_list_query_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("phone", "13800138000");
                body.put("status", "on_shelf");
                body.put("title", "苹果");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(productController.getProductList(any(Map.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/list_query", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void batch_actions_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("action", "off-shelf");
                body.put("phone", "13800138000");
                java.util.List<String> ids = java.util.Arrays.asList("1", "2");
                body.put("product_ids", ids);

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(productController.batchActionProducts(any(ProductBatchActionRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/farmer/products/batch-actions", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);

                ArgumentCaptor<ProductBatchActionRequestDTO> captor = ArgumentCaptor
                                .forClass(ProductBatchActionRequestDTO.class);
                verify(productController).batchActionProducts(captor.capture());
                assertThat(captor.getValue().getAction()).isEqualTo("off-shelf");
                assertThat(captor.getValue().getPhone()).isEqualTo("13800138000");
                assertThat(captor.getValue().getProduct_ids()).containsExactly("1", "2");
        }

        @Test
        void publish_content_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("title", "标题");
                body.put("content", "内容");
                body.put("content_type", "articles");
                body.put("images", java.util.Arrays.asList("http://img/1.jpg"));
                body.put("phone", "13800138005");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(contentController.publishContent(any(PublishContentRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/content/publish", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);

                ArgumentCaptor<PublishContentRequestDTO> captor = ArgumentCaptor
                                .forClass(PublishContentRequestDTO.class);
                verify(contentController).publishContent(captor.capture());
                assertThat(captor.getValue().getTitle()).isEqualTo("标题");
                assertThat(captor.getValue().getContent()).isEqualTo("内容");
                assertThat(captor.getValue().getContentType()).isEqualTo("articles");
                assertThat(captor.getValue().getImages()).containsExactly("http://img/1.jpg");
                assertThat(captor.getValue().getPhone()).isEqualTo("13800138005");
        }

        @Test
        void get_content_detail_dispatch() {
                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(contentController.getContentDetail("CNT-1")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/content/CNT-1", "GET", new HashMap<>(),
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void post_comment_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("comment", "很好");
                body.put("phone", "13800138006");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(commentController.postComment(any(String.class), any(PostCommentRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/content/CNT-2/comments", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<PostCommentRequestDTO> reqCaptor = ArgumentCaptor.forClass(PostCommentRequestDTO.class);
                verify(commentController).postComment(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("CNT-2");
                assertThat(reqCaptor.getValue().getComment()).isEqualTo("很好");
                assertThat(reqCaptor.getValue().getPhone()).isEqualTo("13800138006");
        }

        @Test
        void get_comment_list_dispatch() {
                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 200);
                when(commentController.getCommentList("CNT-3")).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/content/CNT-3/comments", "GET",
                                new HashMap<>(),
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(200);
        }

        @Test
        void post_reply_parsing_and_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("comment", "回复");
                body.put("phone", "13800138007");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(commentController.postReply(any(String.class), any(PostReplyRequestDTO.class)))
                                .thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/comment/CMT-1/replies", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);

                ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<PostReplyRequestDTO> reqCaptor = ArgumentCaptor.forClass(PostReplyRequestDTO.class);
                verify(commentController).postReply(idCaptor.capture(), reqCaptor.capture());
                assertThat(idCaptor.getValue()).isEqualTo("CMT-1");
                assertThat(reqCaptor.getValue().getComment()).isEqualTo("回复");
                assertThat(reqCaptor.getValue().getPhone()).isEqualTo("13800138007");
        }

        @Test
        void register_buyer_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("user_type", "buyer");
                body.put("password", "Abcdef12");
                body.put("nickname", "买家");
                body.put("phone", "13800138011");
                body.put("shipping_address", "地址");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(authController.register(any(dto.auth.BuyerRegisterRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/auth/register", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);
        }

        @Test
        void register_expert_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("user_type", "expert");
                body.put("password", "Abcdef12");
                body.put("nickname", "专家");
                body.put("phone", "13800138012");
                body.put("expertise_field", "果树");
                body.put("work_experience", 10);

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(authController.register(any(dto.auth.ExpertRegisterRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/auth/register", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);
        }

        @Test
        void register_bank_dispatch() {
                Map<String, Object> body = new HashMap<>();
                body.put("user_type", "bank");
                body.put("password", "Abcdef12");
                body.put("nickname", "银行");
                body.put("phone", "13800138013");
                body.put("bank_name", "XX银行");
                body.put("branch_name", "XX支行");

                Map<String, Object> expected = new HashMap<>();
                expected.put("code", 201);
                when(authController.register(any(dto.auth.BankRegisterRequestDTO.class))).thenReturn(expected);

                Map<String, Object> resp = router.handleRequest("/api/v1/auth/register", "POST", body,
                                new HashMap<>(), new HashMap<>());
                assertThat(resp.get("code")).isEqualTo(201);
        }
}
