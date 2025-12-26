<template>
  <div class="order-detail-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <button class="btn-back" @click="handleBack">
          <span class="back-icon">←</span>
          返回
        </button>
        <h1 class="page-title">订单详情</h1>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="error" class="error-container">
          <div class="error-message">
            <span class="error-icon">⚠️</span>
            <span class="error-text">{{ error }}</span>
          </div>
          <button class="btn-retry" @click="loadOrderDetail">重试</button>
        </div>

        <!-- 订单详情 -->
        <div v-else-if="orderDetail" class="order-detail">
          <!-- 订单状态卡片 -->
          <div class="status-card">
            <div class="status-header">
              <span class="status-label">订单状态</span>
              <span :class="['status-badge', `status-${orderDetail.status}`]">
                {{ getStatusText(orderDetail.status) }}
              </span>
            </div>
            <div class="status-info">
              <p class="order-id-text">订单号：{{ orderDetail.order_id }}</p>
              <p class="create-time">创建时间：{{ formatDateTime(orderDetail.created_at) }}</p>
              <p v-if="orderDetail.shipped_at" class="time-info">
                发货时间：{{ formatDateTime(orderDetail.shipped_at) }}
              </p>
              <p v-if="orderDetail.completed_at" class="time-info">
                完成时间：{{ formatDateTime(orderDetail.completed_at) }}
              </p>
              <p v-if="orderDetail.cancelled_at" class="time-info">
                取消时间：{{ formatDateTime(orderDetail.cancelled_at) }}
              </p>
              <p v-if="orderDetail.refunded_at" class="time-info">
                退款时间：{{ formatDateTime(orderDetail.refunded_at) }}
              </p>
            </div>
          </div>

          <!-- 商品信息 -->
          <div class="section-card">
            <h2 class="section-title">商品信息</h2>
            <div class="product-info">
              <div v-if="orderDetail.images && orderDetail.images.length > 0" class="product-images">
                <img
                  v-for="(image, index) in orderDetail.images"
                  :key="index"
                  :src="image"
                  :alt="orderDetail.title"
                  class="product-image"
                  @click="handleImageClick(image)"
                />
              </div>
              <div class="product-details">
                <h3 class="product-title">{{ orderDetail.title }}</h3>
                <div v-if="orderDetail.specification" class="product-spec">
                  规格：{{ orderDetail.specification }}
                </div>
                <div class="product-price-info">
                  <div class="price-row">
                    <span class="price-label">单价：</span>
                    <span class="price-value">¥{{ formatPrice(orderDetail.price) }}</span>
                  </div>
                  <div class="price-row">
                    <span class="price-label">数量：</span>
                    <span class="price-value">{{ orderDetail.quantity }} 斤</span>
                  </div>
                  <div class="price-row total">
                    <span class="price-label">订单总额：</span>
                    <span class="price-value total-price">¥{{ formatPrice(orderDetail.total_amount) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 收货信息 -->
          <div class="section-card">
            <h2 class="section-title">收货信息</h2>
            <div class="delivery-info">
              <div class="info-row">
                <span class="info-label">收货人：</span>
                <span class="info-value">{{ orderDetail.buyer_name }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">手机号：</span>
                <span class="info-value">{{ orderDetail.buyer_phone }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">收货地址：</span>
                <span class="info-value">{{ orderDetail.buyer_address }}</span>
              </div>
            </div>
          </div>

          <!-- 订单备注 -->
          <div v-if="orderDetail.remark" class="section-card">
            <h2 class="section-title">订单备注</h2>
            <p class="remark-text">{{ orderDetail.remark }}</p>
          </div>

          <!-- 操作按钮 -->
          <div v-if="isBuyer" class="action-section">
            <button
              v-if="orderDetail.status === 'shipped'"
              class="btn-action btn-primary"
              @click="handleConfirmReceipt"
            >
              确认收货
            </button>
            <button
              v-if="orderDetail.status === 'shipped'"
              class="btn-action btn-secondary"
              @click="handleApplyRefund"
            >
              申请退款
            </button>
            <button
              class="btn-action btn-default"
              @click="handleBack"
            >
              返回订单列表
            </button>
          </div>
          <div v-else class="action-section">
            <button
              class="btn-action btn-default"
              @click="handleBack"
            >
              返回订单列表
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- 图片预览模态框 -->
    <div v-if="showImagePreview" class="image-modal">
      <img :src="previewImage" alt="预览" class="preview-image" @click.stop />
      <button class="close-preview" @click="showImagePreview = false">×</button>
    </div>

    <!-- 退款对话框 -->
    <div v-if="showRefundDialog" class="dialog-overlay">
      <div class="dialog-content" @click.stop>
        <h3 class="dialog-title">申请退款</h3>
        <div class="dialog-body">
          <div class="form-group">
            <label class="form-label">退款类型</label>
            <select v-model="refundForm.refundType" class="form-input">
              <option value="">请选择退款类型</option>
              <option value="refund">仅退款</option>
              <option value="refund_return">退货退款</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">退款原因 <span class="required">*</span></label>
            <textarea
              v-model="refundForm.refundReason"
              class="form-textarea"
              placeholder="请输入退款原因"
              rows="4"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-dialog btn-cancel" @click="showRefundDialog = false">取消</button>
          <button class="btn-dialog btn-confirm" @click="handleSubmitRefund">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { orderService } from '../api/order';
import logger from '../utils/logger';

export default {
  name: 'OrderDetail',
  setup() {
    const route = useRoute();
    const router = useRouter();
    const orderId = computed(() => {
      const id = route.params.id;
      // 确保返回字符串类型，避免 undefined
      return id ? String(id) : null;
    });
    const orderDetail = ref(null);
    const loading = ref(false);
    const error = ref('');
    const showImagePreview = ref(false);
    const previewImage = ref('');
    const showRefundDialog = ref(false);
    const refundForm = ref({
      refundType: '',
      refundReason: ''
    });

    // 获取用户信息
    const userInfo = ref({});
    const userPhone = computed(() => {
      return userInfo.value.phone || '';
    });
    const isBuyer = computed(() => {
      return userInfo.value.userType === 'buyer';
    });
    const isFarmer = computed(() => {
      return userInfo.value.userType === 'farmer';
    });

    // 获取当前有效的订单ID
    const getCurrentOrderId = () => {
      return route.params.id || orderId.value || orderDetail.value?.order_id;
    };

    // 加载用户信息
    const loadUserInfo = () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
        } catch (err) {
          logger.error('ORDER_DETAIL', '解析用户信息失败', {}, err);
        }
      }
    };

    // 加载订单详情
    const loadOrderDetail = async () => {
      if (!userPhone.value) {
        error.value = '请先登录';
        return;
      }

      // 获取当前路由参数，确保获取到最新的值
      const currentOrderId = route.params.id || orderId.value;
      if (!currentOrderId) {
        error.value = '订单ID不存在，请从订单列表进入';
        logger.error('ORDER_DETAIL', '订单ID不存在', {
          routeParams: route.params,
          orderIdValue: orderId.value
        });
        return;
      }

      loading.value = true;
      error.value = '';

      try {
        logger.info('ORDER_DETAIL', '开始加载订单详情', {
          orderId: currentOrderId,
          userType: userInfo.value.userType,
          phone: userPhone.value,
          routeParams: route.params
        });

        let data;

        if (isBuyer.value) {
          // 买家查询订单详情
          data = await orderService.getOrderDetail(currentOrderId, {
            buyer_phone: userPhone.value
          });
        } else if (isFarmer.value) {
          // 农户查询订单详情
          data = await orderService.getFarmerOrderDetail(currentOrderId, {
            farmer_phone: userPhone.value
          });
        } else {
          error.value = '当前用户类型不支持查看订单详情';
          return;
        }

        orderDetail.value = data;

        logger.info('ORDER_DETAIL', '订单详情加载成功', {
          orderId: currentOrderId,
          status: data.status
        });
      } catch (err) {
        logger.error('ORDER_DETAIL', '加载订单详情失败', {
          orderId: currentOrderId,
          routeParams: route.params,
          errorMessage: err.message || err
        }, err);
        error.value = err.message || '加载订单详情失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    };

    // 格式化日期时间
    const formatDateTime = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    };

    // 格式化价格
    const formatPrice = (price) => {
      if (!price) return '0.00';
      return parseFloat(price).toFixed(2);
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        shipped: '已发货',
        completed: '已完成',
        cancelled: '已取消',
        refunded: '已退款'
      };
      return statusMap[status] || status;
    };

    // 返回
    const handleBack = () => {
      router.push('/orders');
    };

    // 点击图片预览
    const handleImageClick = (image) => {
      previewImage.value = image;
      showImagePreview.value = true;
    };

    // 确认收货
    const handleConfirmReceipt = async () => {
      if (!confirm('确认收到商品吗？')) {
        return;
      }

      try {
        const currentOrderId = getCurrentOrderId();
        if (!currentOrderId) {
          alert('订单ID不存在');
          return;
        }
        logger.userAction('ORDER_CONFIRM_RECEIPT', { orderId: currentOrderId });
        await orderService.confirmReceipt(currentOrderId, {
          buyer_phone: userPhone.value
        });
        alert('确认收货成功！');
        loadOrderDetail();
      } catch (err) {
        logger.error('ORDER_DETAIL', '确认收货失败', {
          orderId: getCurrentOrderId(),
          errorMessage: err.message || err
        }, err);
        alert(err.message || '确认收货失败，请稍后重试');
      }
    };

    // 申请退款
    const handleApplyRefund = () => {
      refundForm.value = {
        refundType: '',
        refundReason: ''
      };
      showRefundDialog.value = true;
    };

    // 提交退款申请
    const handleSubmitRefund = async () => {
      if (!refundForm.value.refundType) {
        alert('请选择退款类型');
        return;
      }

      if (!refundForm.value.refundReason.trim()) {
        alert('请输入退款原因');
        return;
      }

      try {
        const currentOrderId = getCurrentOrderId();
        if (!currentOrderId) {
          alert('订单ID不存在');
          return;
        }
        logger.userAction('ORDER_APPLY_REFUND', {
          orderId: currentOrderId,
          refundType: refundForm.value.refundType
        });

        await orderService.applyRefund(currentOrderId, {
          buyer_phone: userPhone.value,
          refund_type: refundForm.value.refundType,
          refund_reason: refundForm.value.refundReason.trim()
        });

        alert('退款申请提交成功！');
        showRefundDialog.value = false;
        loadOrderDetail();
      } catch (err) {
        logger.error('ORDER_DETAIL', '申请退款失败', {
          orderId: getCurrentOrderId(),
          errorMessage: err.message || err,
          errors: err.errors || []
        }, err);

        if (err.errors && Array.isArray(err.errors) && err.errors.length > 0) {
          const errorMessages = err.errors.map(e => e.message).join('\n');
          alert(errorMessages || '申请退款失败，请稍后重试');
        } else {
          alert(err.message || '申请退款失败，请稍后重试');
        }
      }
    };


    // 监听路由参数变化
    watch(() => route.params.id, (newId) => {
      if (newId) {
        loadOrderDetail();
      }
    }, { immediate: false });

    // 初始化
    onMounted(() => {
      loadUserInfo();
      // 确保路由参数已加载
      const currentOrderId = getCurrentOrderId();
      if (currentOrderId) {
        loadOrderDetail();
      } else {
        // 如果路由参数未加载，等待一下再试
        setTimeout(() => {
          const retryOrderId = getCurrentOrderId();
          if (retryOrderId) {
            loadOrderDetail();
          } else {
            error.value = '订单ID不存在，请从订单列表进入';
          }
        }, 100);
      }
    });

    return {
      orderId,
      orderDetail,
      loading,
      error,
      isBuyer,
      isFarmer,
      showImagePreview,
      previewImage,
      showRefundDialog,
      refundForm,
      formatDateTime,
      formatPrice,
      getStatusText,
      handleBack,
      handleImageClick,
      handleConfirmReceipt,
      handleApplyRefund,
      handleSubmitRefund,
      loadOrderDetail
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.order-detail-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}

/* 顶部导航栏 */
.header {
  background: var(--white);
  padding: 1rem 2rem;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  color: var(--gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--gray-100);
  border-color: var(--primary);
  color: var(--primary);
}

.back-icon {
  font-size: 1.125rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

/* 主内容区域 */
.main-content {
  padding: 2rem;
}

.content-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--gray-200);
  border-top: 4px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误提示 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
  gap: 1rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--error);
  font-size: 1rem;
}

.error-icon {
  font-size: 1.25rem;
}

.btn-retry {
  padding: 0.75rem 1.5rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-retry:hover {
  background: var(--primary-dark);
}

/* 订单详情 */
.order-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 状态卡片 */
.status-card {
  background: var(--white);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.status-label {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
}

.status-badge {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-shipped {
  background: #e0f2fe;
  color: #0369a1;
}

.status-completed {
  background: #dcfce7;
  color: #166534;
}

.status-cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.status-refunded {
  background: #fef3c7;
  color: #92400e;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.order-id-text {
  font-size: 0.875rem;
  color: #1a202c;
  margin: 0;
}

.create-time,
.time-info {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0;
}

/* 通用卡片 */
.section-card {
  background: var(--white);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--primary-light);
}

/* 商品信息 */
.product-info {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-images {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.product-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  border: 1px solid var(--gray-200);
}

.product-image:hover {
  transform: scale(1.05);
}

.product-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.product-spec {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.product-price-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--gray-100);
  border-radius: 8px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-label {
  font-size: 0.875rem;
  color: var(--gray-600);
}

.price-value {
  font-size: 0.875rem;
  color: #1a202c;
  font-weight: 500;
}

.price-row.total {
  padding-top: 0.75rem;
  border-top: 2px solid rgba(107, 70, 193, 0.2);
  margin-top: 0.25rem;
}

.price-row.total .price-label {
  font-size: 1rem;
  font-weight: 600;
}

.total-price {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary);
}

/* 收货信息 */
.delivery-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.info-label {
  font-size: 0.875rem;
  color: var(--gray-600);
  min-width: 80px;
  flex-shrink: 0;
}

.info-value {
  font-size: 0.875rem;
  color: #1a202c;
  flex: 1;
}

/* 订单备注 */
.remark-text {
  font-size: 0.875rem;
  color: #1a202c;
  line-height: 1.6;
  margin: 0;
  padding: 1rem;
  background: var(--gray-100);
  border-radius: 8px;
}

/* 操作按钮 */
.action-section {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 1.5rem;
  background: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
}

.btn-action {
  flex: 1;
  min-width: 120px;
  padding: 0.875rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-action.btn-primary {
  background: var(--primary);
  color: var(--white);
}

.btn-action.btn-primary:hover {
  background: var(--primary-dark);
}

.btn-action.btn-secondary {
  background: var(--white);
  color: var(--primary);
  border: 1px solid var(--primary);
}

.btn-action.btn-secondary:hover {
  background: var(--primary-light);
  color: var(--white);
}

.btn-action.btn-cancel {
  background: var(--white);
  color: var(--error);
  border: 1px solid var(--error);
}

.btn-action.btn-cancel:hover {
  background: var(--error);
  color: var(--white);
}

.btn-action.btn-default {
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-action.btn-default:hover {
  background: var(--gray-300);
}

/* 图片预览模态框 */
.image-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 2rem;
}

.preview-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  border-radius: 8px;
}

.close-preview {
  position: absolute;
  top: 2rem;
  right: 2rem;
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: var(--white);
  border-radius: 50%;
  font-size: 1.5rem;
  cursor: pointer;
  transition: background 0.2s;
}

.close-preview:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 退款对话框 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
}

.dialog-content {
  background: var(--white);
  border-radius: 12px;
  padding: 2rem;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.dialog-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1.5rem 0;
}

.dialog-body {
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #1a202c;
}

.required {
  color: var(--error);
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.dialog-footer {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn-dialog {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-dialog.btn-cancel {
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-dialog.btn-cancel:hover {
  background: var(--gray-300);
}

.btn-dialog.btn-confirm {
  background: var(--primary);
  color: var(--white);
}

.btn-dialog.btn-confirm:hover {
  background: var(--primary-dark);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .main-content {
    padding: 1rem;
  }

  .status-card,
  .section-card {
    padding: 1rem;
  }

  .product-images {
    justify-content: center;
  }

  .product-image {
    width: 100px;
    height: 100px;
  }

  .action-section {
    flex-direction: column;
  }

  .btn-action {
    width: 100%;
  }

  .dialog-content {
    padding: 1.5rem;
  }

  .dialog-footer {
    flex-direction: column;
  }

  .btn-dialog {
    width: 100%;
  }
}
</style>
