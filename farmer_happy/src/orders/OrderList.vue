<template>
  <div class="order-list-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <button class="btn-back" @click="handleBack">
          <span class="back-icon">←</span>
          返回
        </button>
        <h1 class="page-title">我的订单</h1>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 筛选和搜索区域 -->
        <div class="filter-section">
          <div class="filter-row">
            <div class="search-box">
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索商品标题..."
                class="search-input"
                @input="handleSearch"
              />
              <span class="search-icon">🔍</span>
            </div>
            <div class="filter-group">
              <label class="filter-label">订单状态：</label>
              <select v-model="statusFilter" class="filter-select" @change="handleFilter">
                <option value="">全部</option>
                <option value="shipped">已发货</option>
                <option value="completed">已完成</option>
                <option value="cancelled">已取消</option>
                <option value="refunded">已退款</option>
              </select>
            </div>
            <button class="btn-refresh" @click="loadOrders">
              <span class="refresh-icon">🔄</span>
              刷新
            </button>
          </div>
        </div>

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
          <button class="btn-retry" @click="loadOrders">重试</button>
        </div>

        <!-- 订单列表 -->
        <div v-else-if="orders.length > 0" class="orders-list">
          <div
            v-for="order in orders"
            :key="order.order_id"
            class="order-card"
            @click="handleOrderClick(order.order_id)"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-id">订单号：{{ order.order_id }}</span>
                <span class="order-date">{{ formatDate(order.created_at) }}</span>
              </div>
              <span :class="['order-status', `status-${order.status}`]">
                {{ getStatusText(order.status) }}
              </span>
            </div>

            <div class="order-body">
              <div class="product-image">
                <img
                  v-if="order.main_image_url"
                  :src="order.main_image_url"
                  :alt="order.title"
                  class="image"
                />
                <div v-else class="image-placeholder">📦</div>
              </div>
              <div class="order-details">
                <h3 class="product-title">{{ order.title }}</h3>
                <div class="order-meta">
                  <span class="meta-item">
                    <span class="meta-label">数量：</span>
                    <span class="meta-value">{{ order.quantity }} 斤</span>
                  </span>
                  <span class="meta-item">
                    <span class="meta-label">金额：</span>
                    <span class="meta-value price">¥{{ formatPrice(order.total_amount) }}</span>
                  </span>
                </div>
              </div>
            </div>

            <div class="order-footer">
              <div class="order-actions">
                <button
                  class="btn-action btn-detail"
                  @click.stop="handleViewDetail(order.order_id)"
                >
                  查看详情
                </button>
                <!-- 买家操作按钮 -->
                <template v-if="isBuyer">
                  <button
                    v-if="order.status === 'shipped'"
                    class="btn-action btn-primary"
                    @click.stop="handleConfirmReceipt(order.order_id)"
                  >
                    确认收货
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-container">
          <div class="empty-icon">📦</div>
          <p class="empty-text">暂无订单</p>
          <p class="empty-hint">{{ isBuyer ? '去购买一些优质农产品吧！' : '还没有收到任何订单' }}</p>
          <button v-if="isBuyer" class="btn-go-shopping" @click="handleGoShopping">
            去购物
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { orderService } from '../api/order';
import logger from '../utils/logger';

export default {
  name: 'OrderList',
  setup() {
    const router = useRouter();
    const orders = ref([]);
    const loading = ref(false);
    const error = ref('');
    const searchKeyword = ref('');
    const statusFilter = ref('');

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

    // 加载用户信息
    const loadUserInfo = () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
        } catch (err) {
          logger.error('ORDER_LIST', '解析用户信息失败', {}, err);
        }
      }
    };

    // 加载订单列表
    const loadOrders = async () => {
      if (!userPhone.value) {
        error.value = '请先登录';
        return;
      }

      loading.value = true;
      error.value = '';

      try {
        logger.info('ORDER_LIST', '开始加载订单列表', {
          userType: userInfo.value.userType,
          phone: userPhone.value,
          status: statusFilter.value,
          title: searchKeyword.value
        });

        const params = {};
        let data;

        if (isBuyer.value) {
          // 买家查询订单
          params.buyer_phone = userPhone.value;
          if (statusFilter.value) {
            params.status = statusFilter.value;
          }
          if (searchKeyword.value.trim()) {
            params.title = searchKeyword.value.trim();
          }
          data = await orderService.getOrderList(params);
        } else if (isFarmer.value) {
          // 农户查询订单
          params.farmer_phone = userPhone.value;
          if (statusFilter.value) {
            params.status = statusFilter.value;
          }
          if (searchKeyword.value.trim()) {
            params.title = searchKeyword.value.trim();
          }
          data = await orderService.getFarmerOrderList(params);
        } else {
          error.value = '当前用户类型不支持查看订单';
          return;
        }

        orders.value = data.list || [];

        logger.info('ORDER_LIST', '订单列表加载成功', {
          count: orders.value.length,
          firstOrder: orders.value.length > 0 ? orders.value[0] : null
        });
        
        // 调试：检查订单数据字段
        if (orders.value.length > 0) {
          const firstOrder = orders.value[0];
          console.log('第一个订单数据:', firstOrder);
          console.log('订单ID字段:', {
            order_id: firstOrder.order_id,
            orderId: firstOrder.orderId,
            keys: Object.keys(firstOrder)
          });
        }
      } catch (err) {
        logger.error('ORDER_LIST', '加载订单列表失败', {
          errorMessage: err.message || err
        }, err);
        error.value = err.message || '加载订单列表失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    };

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
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

    // 搜索
    const handleSearch = () => {
      loadOrders();
    };

    // 筛选
    const handleFilter = () => {
      loadOrders();
    };

    // 返回
    const handleBack = () => {
      router.push('/home');
    };

    // 点击订单卡片
    const handleOrderClick = (orderId) => {
      router.push(`/orders/${orderId}`);
    };

    // 查看详情
    const handleViewDetail = (orderId) => {
      router.push(`/orders/${orderId}`);
    };

    // 确认收货
    const handleConfirmReceipt = async (orderId) => {
      if (!confirm('确认收到商品吗？')) {
        return;
      }

      try {
        logger.userAction('ORDER_CONFIRM_RECEIPT', { orderId });
        await orderService.confirmReceipt(orderId, {
          buyer_phone: userPhone.value
        });
        alert('确认收货成功！');
        loadOrders();
      } catch (err) {
        logger.error('ORDER_LIST', '确认收货失败', {
          orderId,
          errorMessage: err.message || err
        }, err);
        alert(err.message || '确认收货失败，请稍后重试');
      }
    };


    // 去购物
    const handleGoShopping = () => {
      router.push('/trading');
    };

    // 初始化
    onMounted(() => {
      loadUserInfo();
      loadOrders();
    });

    return {
      orders,
      loading,
      error,
      searchKeyword,
      statusFilter,
      isBuyer,
      isFarmer,
      formatDate,
      formatPrice,
      getStatusText,
      handleSearch,
      handleFilter,
      handleBack,
      handleOrderClick,
      handleViewDetail,
      handleConfirmReceipt,
      handleGoShopping,
      loadOrders
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.order-list-container {
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
  max-width: 1200px;
  margin: 0 auto;
}

/* 筛选区域 */
.filter-section {
  background: var(--white);
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
  margin-bottom: 1.5rem;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 200px;
}

.search-input {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 1rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
  transition: border-color 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
}

.search-icon {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--gray-400);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-label {
  font-size: 0.875rem;
  color: var(--gray-600);
  white-space: nowrap;
}

.filter-select {
  padding: 0.75rem 1rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
  background: var(--white);
  cursor: pointer;
  transition: border-color 0.2s;
}

.filter-select:focus {
  outline: none;
  border-color: var(--primary);
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-refresh:hover {
  background: var(--primary-dark);
}

.refresh-icon {
  font-size: 1rem;
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

/* 订单列表 */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.order-card {
  background: var(--white);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(107, 70, 193, 0.15);
  border-color: var(--primary-light);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.order-id {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1a202c;
}

.order-date {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.order-status {
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  font-size: 0.75rem;
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

.order-body {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--gray-100);
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  background: var(--gray-200);
}

.order-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.product-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.order-meta {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
}

.meta-label {
  color: var(--gray-500);
}

.meta-value {
  color: #1a202c;
  font-weight: 500;
}

.meta-value.price {
  color: var(--primary);
  font-weight: 600;
  font-size: 1rem;
}

.order-footer {
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.order-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn-action {
  padding: 0.5rem 1rem;
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--white);
  color: var(--gray-600);
}

.btn-action:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.btn-action.btn-primary {
  background: var(--primary);
  color: var(--white);
  border-color: var(--primary);
}

.btn-action.btn-primary:hover {
  background: var(--primary-dark);
}

.btn-action.btn-cancel {
  border-color: var(--error);
  color: var(--error);
}

.btn-action.btn-cancel:hover {
  background: var(--error);
  color: var(--white);
}

/* 空状态 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.08);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-text {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.5rem 0;
}

.empty-hint {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin: 0 0 1.5rem 0;
}

.btn-go-shopping {
  padding: 0.75rem 2rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-go-shopping:hover {
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

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    min-width: 100%;
  }

  .filter-group {
    width: 100%;
  }

  .filter-select {
    flex: 1;
  }

  .btn-refresh {
    width: 100%;
    justify-content: center;
  }

  .order-body {
    flex-direction: column;
  }

  .product-image {
    width: 100%;
    height: 200px;
  }

  .order-actions {
    flex-direction: column;
  }

  .btn-action {
    width: 100%;
  }
}
</style>
