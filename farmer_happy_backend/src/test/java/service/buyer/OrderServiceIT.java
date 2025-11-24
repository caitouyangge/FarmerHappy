package service.buyer;

import dto.buyer.CreateOrderRequestDTO;
import dto.buyer.CreateOrderResponseDTO;
import entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceIT {
        private DatabaseManager db;
        private OrderServiceImpl service;
        private long pid;

        @BeforeEach
        void setUp() throws Exception {
                DatabaseManager.configure("jdbc:h2:mem:",
                                "farmer_happy;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                                "sa", "", "org.h2.Driver");
                db = DatabaseManager.getInstance();
                db.initializeDatabaseForTests();
                db.clearAllTablesForTests();

                Connection conn = db.getConnection();
                String buyerUid = "buyer-uid-1";
                String farmerUid = "farmer-uid-1";

                PreparedStatement insertUser1 = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, ?, 'pwd', 'buyer', 10000, TRUE)");
                insertUser1.setString(1, buyerUid);
                insertUser1.setString(2, "13800138001");
                insertUser1.executeUpdate();

                PreparedStatement insertUser2 = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, ?, 'pwd', 'farmer', 0, TRUE)");
                insertUser2.setString(1, farmerUid);
                insertUser2.setString(2, "13800138002");
                insertUser2.executeUpdate();

                PreparedStatement insertBuyer = conn
                                .prepareStatement("INSERT INTO user_buyers (uid, enable) VALUES (?, TRUE)");
                insertBuyer.setString(1, buyerUid);
                insertBuyer.executeUpdate();

                PreparedStatement insertFarmer = conn.prepareStatement(
                                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES (?, '好农场', TRUE)",
                                Statement.RETURN_GENERATED_KEYS);
                insertFarmer.setString(1, farmerUid);
                insertFarmer.executeUpdate();
                ResultSet farmerKeys = insertFarmer.getGeneratedKeys();
                farmerKeys.next();
                long farmerId = farmerKeys.getLong(1);

                PreparedStatement insertProduct = conn.prepareStatement(
                                "INSERT INTO products (farmer_id, category, title, detailed_description, price, stock, description, origin, status, enable, created_at) VALUES (?, 'fruits', '苹果', '规格', 10.00, 10, 'desc', '河北', 'on_shelf', TRUE, CURRENT_TIMESTAMP)",
                                Statement.RETURN_GENERATED_KEYS);
                insertProduct.setLong(1, farmerId);
                insertProduct.executeUpdate();
                ResultSet productKeys = insertProduct.getGeneratedKeys();
                productKeys.next();
                long productId = productKeys.getLong(1);
                pid = productId;

                PreparedStatement insertImage = conn.prepareStatement(
                                "INSERT INTO product_images (product_id, image_url, sort_order, is_main, enable) VALUES (?, 'http://img/1.jpg', 0, TRUE, TRUE)");
                insertImage.setLong(1, productId);
                insertImage.executeUpdate();

                conn.close();

                service = new OrderServiceImpl(db);
        }

        @Test
        void create_order_updates_stock_balance_and_sales() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO resp = service.createOrder(req);
                assertThat(resp.getStatus()).isEqualTo("shipped");
                assertThat(resp.getOrderId()).isNotBlank();

                BigDecimal balance = db.getBuyerBalance("13800138001");
                assertThat(balance).isEqualByComparingTo("9980.00");

                entity.Product product = db.getProductById(pid);
                assertThat(product.getStock()).isEqualTo(8);

                Connection conn = db.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE buyer_phone = ?");
                stmt.setString(1, "13800138001");
                ResultSet rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
                conn.close();
        }

        @Test
        void apply_refund_restores_balance_stock_and_sales() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);
                assertThat(created.getStatus()).isEqualTo("shipped");

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("不适合");
                refund.setBuyerPhone("13800138001");

                dto.buyer.RefundResponseDTO resp = service.applyRefund(created.getOrderId(), refund);
                assertThat(resp.getStatus()).isEqualTo("refunded");

                java.math.BigDecimal buyerBalance = db.getBuyerBalance("13800138001");
                assertThat(buyerBalance).isEqualByComparingTo("10000.00");

                entity.Product product = db.getProductById(pid);
                assertThat(product.getStock()).isEqualTo(10);
                assertThat(product.getSalesCount()).isEqualTo(0);
        }

        @Test
        void confirm_receipt_moves_to_completed_and_pays_farmer() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);
                assertThat(created.getStatus()).isEqualTo("shipped");

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138001");

                dto.buyer.ConfirmReceiptResponseDTO resp = service.confirmReceipt(created.getOrderId(), confirm);
                assertThat(resp.getStatus()).isEqualTo("completed");

                java.math.BigDecimal farmerBalance = db.getFarmerBalance("13800138002");
                assertThat(farmerBalance).isEqualByComparingTo("20.00");
        }

        @Test
        void get_order_detail_returns_expected_fields() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.QueryOrderRequestDTO query = new dto.buyer.QueryOrderRequestDTO();
                query.setBuyerPhone("13800138001");

                dto.buyer.OrderDetailResponseDTO detail = service.getOrderDetail(created.getOrderId(), query);
                assertThat(detail.getStatus()).isEqualTo("shipped");
                assertThat(detail.getProductId()).isEqualTo(String.valueOf(pid));
                assertThat(detail.getBuyerPhone()).isEqualTo("13800138001");
                assertThat(detail.getTitle()).isEqualTo("苹果");
                assertThat(detail.getTotalAmount()).isEqualByComparingTo("20.00");
        }

        @Test
        void get_order_list_filters_by_status_and_title() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                service.createOrder(req);

                dto.buyer.OrderListResponseDTO list = service.getOrderList("13800138001", "shipped", "苹果");
                assertThat(list.getList().size()).isEqualTo(1);
                assertThat(list.getList().get(0).getStatus()).isEqualTo("shipped");
                assertThat(list.getList().get(0).getTitle()).contains("苹果");
                assertThat(list.getList().get(0).getMainImageUrl()).isEqualTo("http://img/1.jpg");
        }

        @Test
        void refund_only_refund_throws_error_and_no_state_change() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("only_refund");
                refund.setRefundReason("不适合");
                refund.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund(created.getOrderId(), refund))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单已发货");

                java.math.BigDecimal buyerBalance = db.getBuyerBalance("13800138001");
                assertThat(buyerBalance).isEqualByComparingTo("9980.00");
        }

        @Test
        void update_order_changes_buyer_address_and_remark() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.UpdateOrderRequestDTO upd = new dto.buyer.UpdateOrderRequestDTO();
                upd.setBuyerPhone("13800138001");
                upd.setBuyerAddress("上海市浦东新区");
                upd.setRemark("改地址");

                dto.buyer.UpdateOrderResponseDTO updated = service.updateOrder(created.getOrderId(), upd);
                assertThat(updated.getStatus()).isEqualTo("shipped");
                assertThat(updated.getBuyerAddress()).isEqualTo("上海市浦东新区");
                assertThat(updated.getRemark()).isEqualTo("改地址");
        }

        @Test
        void get_farmer_order_detail_and_list_for_shipped() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.OrderDetailResponseDTO d = service.getFarmerOrderDetail(created.getOrderId(), "13800138002");
                assertThat(d.getStatus()).isEqualTo("shipped");
                assertThat(d.getTitle()).isEqualTo("苹果");

                dto.buyer.OrderListResponseDTO list = service.getFarmerOrderList("13800138002", "shipped", "苹果");
                assertThat(list.getList().size()).isGreaterThanOrEqualTo(1);
                assertThat(list.getList().get(0).getStatus()).isEqualTo("shipped");
        }

        @Test
        void buyer_order_list_invalid_status_throws_error() {
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getOrderList("13800138001", "unknown", null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无效的订单状态");
        }

        @Test
        void get_order_detail_with_wrong_buyer_phone_is_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                Connection conn = db.getConnection();
                String otherBuyerUid = "buyer-uid-2";
                PreparedStatement insertUser = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138003', 'pwd', 'buyer2', 10000, TRUE)");
                insertUser.setString(1, otherBuyerUid);
                insertUser.executeUpdate();
                PreparedStatement insertBuyer = conn
                                .prepareStatement("INSERT INTO user_buyers (uid, enable) VALUES (?, TRUE)");
                insertBuyer.setString(1, otherBuyerUid);
                insertBuyer.executeUpdate();
                conn.close();

                dto.buyer.QueryOrderRequestDTO query = new dto.buyer.QueryOrderRequestDTO();
                query.setBuyerPhone("13800138003");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getOrderDetail(created.getOrderId(), query))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无权限访问该订单");
        }

        @Test
        void confirm_receipt_twice_throws_and_no_double_pay() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138001");
                service.confirmReceipt(created.getOrderId(), confirm);

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.confirmReceipt(created.getOrderId(), confirm))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单已完成");

                java.math.BigDecimal farmerBalance = db.getFarmerBalance("13800138002");
                assertThat(farmerBalance).isEqualByComparingTo("10.00");
        }

        @Test
        void apply_refund_after_completed_throws_error() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138001");
                service.confirmReceipt(created.getOrderId(), confirm);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("完成后不退");
                refund.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund(created.getOrderId(), refund))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单已完成");
        }

        @Test
        void update_order_after_completed_is_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138001");
                service.confirmReceipt(created.getOrderId(), confirm);

                dto.buyer.UpdateOrderRequestDTO upd = new dto.buyer.UpdateOrderRequestDTO();
                upd.setBuyerPhone("13800138001");
                upd.setBuyerAddress("上海市浦东新区");

                org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.updateOrder(created.getOrderId(), upd))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("当前订单状态不允许修改");
        }

        @Test
        void get_order_detail_invalid_phone_format_throws_error() {
                dto.buyer.QueryOrderRequestDTO query = new dto.buyer.QueryOrderRequestDTO();
                query.setBuyerPhone("123");
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getOrderDetail("order-xx", query))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("手机号格式不正确");
        }

        @Test
        void get_order_list_invalid_phone_format_throws_error() {
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getOrderList("123", null, null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("手机号格式不正确");
        }

        @Test
        void farmer_order_list_invalid_phone_format_throws_error() {
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getFarmerOrderList("123", null, null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("手机号格式不正确");
        }

        @Test
        void apply_refund_wrong_buyer_phone_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                Connection conn = db.getConnection();
                String otherBuyerUid = "buyer-uid-3";
                PreparedStatement insertUser = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138004', 'pwd', 'buyer3', 10000, TRUE)");
                insertUser.setString(1, otherBuyerUid);
                insertUser.executeUpdate();
                PreparedStatement insertBuyer = conn
                                .prepareStatement("INSERT INTO user_buyers (uid, enable) VALUES (?, TRUE)");
                insertBuyer.setString(1, otherBuyerUid);
                insertBuyer.executeUpdate();
                conn.close();

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("他人申请");
                refund.setBuyerPhone("13800138004");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund(created.getOrderId(), refund))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无权限访问该订单");
        }

        @Test
        void confirm_receipt_invalid_phone_format_throws_error() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("123");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.confirmReceipt(created.getOrderId(), confirm))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("手机号格式不正确");
        }

        @Test
        void confirm_receipt_wrong_buyer_phone_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                Connection conn = db.getConnection();
                String otherBuyerUid = "buyer-uid-4";
                PreparedStatement insertUser = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138006', 'pwd', 'buyer4', 10000, TRUE)");
                insertUser.setString(1, otherBuyerUid);
                insertUser.executeUpdate();
                PreparedStatement insertBuyer = conn
                                .prepareStatement("INSERT INTO user_buyers (uid, enable) VALUES (?, TRUE)");
                insertBuyer.setString(1, otherBuyerUid);
                insertBuyer.executeUpdate();
                conn.close();

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138006");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.confirmReceipt(created.getOrderId(), confirm))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无权限访问该订单");
        }

        @Test
        void apply_refund_invalid_type_throws_validation_error() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("invalid");
                refund.setRefundReason("原因");
                refund.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund(created.getOrderId(), refund))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("参数验证失败");
        }

        @Test
        void apply_refund_reason_too_long_throws_validation_error() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                StringBuilder longReason = new StringBuilder();
                for (int i = 0; i < 210; i++)
                        longReason.append('x');

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason(longReason.toString());
                refund.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund(created.getOrderId(), refund))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("参数验证失败");
        }

        @Test
        void update_order_no_fields_throws_error() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.UpdateOrderRequestDTO upd = new dto.buyer.UpdateOrderRequestDTO();
                upd.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.updateOrder(created.getOrderId(), upd))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("至少需要提供一个需要修改的字段");
        }

        @Test
        void get_farmer_order_list_invalid_status_throws_error() {
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getFarmerOrderList("13800138002", "unknown", null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无效的订单状态");
        }

        @Test
        void get_order_detail_nonexistent_throws_error() {
                dto.buyer.QueryOrderRequestDTO query = new dto.buyer.QueryOrderRequestDTO();
                query.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getOrderDetail("non-exists-id", query))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单不存在");
        }

        @Test
        void get_order_list_unknown_buyer_phone_throws_error() {
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getOrderList("13800138009", null, null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("买家账户不存在或未启用");
        }

        @Test
        void confirm_receipt_on_refunded_throws_error_and_no_pay() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("退货");
                refund.setBuyerPhone("13800138001");
                service.applyRefund(created.getOrderId(), refund);

                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.confirmReceipt(created.getOrderId(), confirm))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("当前订单状态不允许确认收货");

                java.math.BigDecimal farmerBalance = db.getFarmerBalance("13800138002");
                assertThat(farmerBalance).isEqualByComparingTo("0");
        }

        @Test
        void create_order_with_off_shelf_product_throws_error() throws Exception {
                Connection conn = db.getConnection();
                PreparedStatement upd = conn
                                .prepareStatement("UPDATE products SET status='off_shelf' WHERE product_id=?");
                upd.setLong(1, pid);
                upd.executeUpdate();
                conn.close();

                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.createOrder(req))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("商品已下架");
        }

        @Test
        void create_order_stock_insufficient_throws_error() throws Exception {
                Connection conn = db.getConnection();
                PreparedStatement upd = conn.prepareStatement("UPDATE products SET stock=1 WHERE product_id=?");
                upd.setLong(1, pid);
                upd.executeUpdate();
                conn.close();

                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(2);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.createOrder(req))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("库存不足");
        }

        @Test
        void create_order_insufficient_balance_throws_error() throws Exception {
                Connection conn = db.getConnection();
                PreparedStatement upd = conn.prepareStatement("UPDATE users SET money=5 WHERE phone='13800138001'");
                upd.executeUpdate();
                conn.close();

                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.createOrder(req))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("余额不足");
        }

        @Test
        void create_order_quantity_over_limit_throws_error() {
                try {
                        Connection conn = db.getConnection();
                        PreparedStatement upd = conn
                                        .prepareStatement("UPDATE products SET stock=1000 WHERE product_id=?");
                        upd.setLong(1, pid);
                        upd.executeUpdate();
                        conn.close();
                } catch (Exception ignored) {
                }

                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(101);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.createOrder(req))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("单次购买数量");
        }

        @Test
        void apply_refund_invalid_phone_format_throws_error() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("退货");
                refund.setBuyerPhone("123");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund(created.getOrderId(), refund))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("参数验证失败");
        }

        @Test
        void confirm_receipt_nonexistent_order_throws_error() {
                dto.buyer.ConfirmReceiptRequestDTO confirm = new dto.buyer.ConfirmReceiptRequestDTO();
                confirm.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.confirmReceipt("order-not-exists", confirm))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单不存在");
        }

        @Test
        void apply_refund_nonexistent_order_throws_error() {
                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("退货");
                refund.setBuyerPhone("13800138001");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.applyRefund("order-not-exists", refund))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单不存在");
        }

        @Test
        void update_order_nonexistent_order_throws_error() {
                dto.buyer.UpdateOrderRequestDTO upd = new dto.buyer.UpdateOrderRequestDTO();
                upd.setBuyerPhone("13800138001");
                upd.setBuyerAddress("北京市朝阳区");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.updateOrder("order-not-exists", upd))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("订单不存在");
        }

        @Test
        void get_farmer_order_detail_wrong_farmer_phone_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                Connection conn = db.getConnection();
                PreparedStatement insertUser = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES ('farmer-uid-7', '13800138007', 'pwd', 'farmer7', 0, TRUE)");
                insertUser.executeUpdate();
                PreparedStatement insertFarmer = conn.prepareStatement(
                                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES ('farmer-uid-7', '别的农场', TRUE)");
                insertFarmer.executeUpdate();
                conn.close();

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(
                                                () -> service.getFarmerOrderDetail(created.getOrderId(), "13800138007"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无权限访问该订单");
        }

        @Test
        void get_farmer_order_detail_invalid_phone_format_throws_error() {
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getFarmerOrderDetail("order-xx", "123"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("手机号格式不正确");
        }

        @Test
        void get_farmer_order_list_unknown_phone_throws_error() {
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.getFarmerOrderList("13900000000", null, null))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("农户账户不存在或未启用");
        }

        @Test
        void update_order_after_refunded_is_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("退货");
                refund.setBuyerPhone("13800138001");
                service.applyRefund(created.getOrderId(), refund);

                dto.buyer.UpdateOrderRequestDTO upd = new dto.buyer.UpdateOrderRequestDTO();
                upd.setBuyerPhone("13800138001");
                upd.setBuyerAddress("深圳市南山区");

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> service.updateOrder(created.getOrderId(), upd))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("当前订单状态不允许修改");
        }

        @Test
        void get_farmer_order_detail_wrong_phone_forbidden() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                Connection conn = db.getConnection();
                String otherFarmerUid = "farmer-uid-9";
                PreparedStatement insertUser = conn.prepareStatement(
                                "INSERT INTO users (uid, phone, password, nickname, money, is_active) VALUES (?, '13800138009', 'pwd', 'farmer9', 0, TRUE)");
                insertUser.setString(1, otherFarmerUid);
                insertUser.executeUpdate();
                PreparedStatement insertFarmer = conn.prepareStatement(
                                "INSERT INTO user_farmers (uid, farm_name, enable) VALUES (?, '其他农场', TRUE)");
                insertFarmer.setString(1, otherFarmerUid);
                insertFarmer.executeUpdate();
                conn.close();

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(
                                                () -> service.getFarmerOrderDetail(created.getOrderId(), "13800138009"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("无权限访问该订单");
        }

        @Test
        void buyer_order_list_refunded_status_contains_order() throws Exception {
                CreateOrderRequestDTO req = new CreateOrderRequestDTO();
                req.setProductId(String.valueOf(pid));
                req.setQuantity(1);
                req.setBuyerName("李四");
                req.setBuyerAddress("北京市朝阳区");
                req.setBuyerPhone("13800138001");
                req.setRemark("尽快发货");

                CreateOrderResponseDTO created = service.createOrder(req);

                dto.buyer.RefundRequestDTO refund = new dto.buyer.RefundRequestDTO();
                refund.setRefundType("return_and_refund");
                refund.setRefundReason("退货");
                refund.setBuyerPhone("13800138001");
                service.applyRefund(created.getOrderId(), refund);

                dto.buyer.OrderListResponseDTO list = service.getOrderList("13800138001", "refunded", null);
                assertThat(list.getList().size()).isGreaterThanOrEqualTo(1);
                assertThat(list.getList().get(0).getStatus()).isEqualTo("refunded");
        }
}
