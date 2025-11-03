<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">产品详情</h2>
        <button class="close-btn" @click="handleClose">
          <span class="close-icon">×</span>
        </button>
      </div>

      <div class="modal-content">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="error" class="error-container">
          <div class="error-icon">❌</div>
          <h3>加载失败</h3>
          <p>{{ error }}</p>
          <button class="retry-btn" @click="loadProductDetail">
            重试
          </button>
        </div>

        <div v-else-if="product" class="product-detail">
          <!-- 产品图片和基本信息 -->
          <div class="product-header">
            <div class="product-image">
              <div class="image-placeholder">
                <span class="image-icon">🌾</span>
              </div>
            </div>

            <div class="product-basic-info">
              <h1 class="product-title">{{ product.title }}</h1>
              <p class="product-category">{{ categoryText }}</p>
              
              <div class="price-section">
                <span class="price-label">价格:</span>
                <span class="price-value">¥{{ product.price || '面议' }}</span>
              </div>

              <div class="stock-section">
                <span class="stock-label">库存:</span>
                <span class="stock-value">{{ product.stock || 0 }} 斤</span>
              </div>
            </div>
          </div>

          <!-- 详细信息 -->
          <div class="product-details">
            <div class="detail-section">
              <h3 class="detail-title">产品信息</h3>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-label">发布时间:</span>
                  <span class="detail-value">{{ formatDate(product.created_at) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">更新时间:</span>
                  <span class="detail-value">{{ formatDate(product.updated_at) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">商品详细介绍:</span>
                  <span class="detail-value">{{ product.detailed_description || '未填写' }}</span>
                </div>
              </div>
            </div>

            <!-- 农户信息 -->
            <div class="detail-section" v-if="product.farmer_info">
              <h3 class="detail-title">农户信息</h3>
              <div class="farmer-info">
                <div class="farmer-item">
                  <span class="farmer-label">农户姓名:</span>
                  <span class="farmer-value">{{ product.farmer_info.nickname || '未填写' }}</span>
                </div>
                <div class="farmer-item">
                  <span class="farmer-label">联系电话:</span>
                  <span class="farmer-value">{{ product.farmer_info.phone || '未填写' }}</span>
                </div>
                <div class="farmer-item">
                  <span class="farmer-label">注册时间:</span>
                  <span class="farmer-value">{{ formatDate(product.farmer_info.created_at) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <button class="action-btn back-btn" @click="handleClose">
              <span class="btn-icon">←</span>
              返回
            </button>

            <!-- 农户操作 -->
            <template v-if="isFarmer">
              <button class="action-btn edit-btn" @click="handleEdit">
                <span class="btn-icon">✏️</span>
                编辑
              </button>
              
              <button 
                v-if="product.status === 'draft' || product.status === 'off_shelf'"
                class="action-btn on-shelf-btn" 
                @click="handleOnShelf"
              >
                <span class="btn-icon">📤</span>
                上架
              </button>
              
              <button 
                v-if="product.status === 'on_shelf'"
                class="action-btn off-shelf-btn" 
                @click="handleOffShelf"
              >
                <span class="btn-icon">📥</span>
                下架
              </button>
              
              <button class="action-btn delete-btn" @click="handleDelete">
                <span class="btn-icon">🗑️</span>
                删除
              </button>
            </template>
            
            <!-- 买家操作 -->
            <template v-else>
              <button 
                v-if="product.status === 'on_shelf' && product.stock > 0"
                class="action-btn purchase-btn" 
                @click="handlePurchase"
              >
                <span class="btn-icon">🛒</span>
                立即购买
              </button>
              
              <button 
                v-else-if="product.status === 'sold_out'"
                class="action-btn disabled-btn" 
                disabled
              >
                <span class="btn-icon">❌</span>
                已售罄
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { productService } from '../api/product';
import logger from '../utils/logger';

export default {
  name: 'ProductDetail',
  props: {
    productId: {
      type: [String, Number],
      required: true
    }
  },
  emits: ['close', 'edit', 'delete', 'on-shelf', 'off-shelf', 'purchase'],
  setup(props, { emit }) {
    const product = ref(null);
    const loading = ref(false);
    const error = ref('');
    const userInfo = ref({});

    // 获取用户信息
    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
        } catch (error) {
          logger.error('PRODUCT_DETAIL', '解析用户信息失败', {}, error);
        }
      }
      loadProductDetail();
    });

    // 用户类型判断
    const isFarmer = computed(() => userInfo.value.userType === 'farmer');

    // 分类文本
    const categoryText = computed(() => {
      if (!product.value) return '';
      const categoryMap = {
        vegetables: '蔬菜',
        fruits: '水果',
        grains: '粮食'
      };
      return categoryMap[product.value.category] || product.value.category || '其他';
    });

    // 加载产品详情
    const loadProductDetail = async () => {
      loading.value = true;
      error.value = '';
      
      try {
        logger.info('PRODUCT_DETAIL', '开始加载产品详情', { productId: props.productId });
        
        const response = await productService.getProductDetail(props.productId, userInfo.value.phone);
        product.value = response;
        
        logger.info('PRODUCT_DETAIL', '产品详情加载成功', { productId: props.productId });
      } catch (err) {
        logger.error('PRODUCT_DETAIL', '加载产品详情失败', {
          productId: props.productId,
          errorMessage: err.message || err
        }, err);
        error.value = err.message || '加载失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    };

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '无';
      try {
        const date = new Date(dateString);
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        });
      } catch (error) {
        return '无';
      }
    };

    // 关闭弹窗
    const handleClose = () => {
      logger.userAction('PRODUCT_DETAIL_CLOSE', { productId: props.productId });
      emit('close');
    };

    // 点击遮罩层关闭
    const handleOverlayClick = (event) => {
      if (event.target === event.currentTarget) {
        handleClose();
      }
    };

    // 编辑产品
    const handleEdit = () => {
      logger.userAction('PRODUCT_DETAIL_EDIT', { productId: props.productId });
      emit('edit', product.value);
    };

    // 删除产品
    const handleDelete = async () => {
      if (!confirm('确定要删除这个产品吗？')) {
        return;
      }

      try {
        logger.userAction('PRODUCT_DETAIL_DELETE', { productId: props.productId });
        await productService.deleteProduct(props.productId, userInfo.value.phone);
        logger.info('PRODUCT_DETAIL', '产品删除成功', { productId: props.productId });
        emit('delete', props.productId);
        handleClose();
      } catch (err) {
        logger.error('PRODUCT_DETAIL', '删除产品失败', {
          productId: props.productId,
          errorMessage: err.message || err
        }, err);
        alert('删除失败：' + (err.message || err));
      }
    };

    // 上架产品
    const handleOnShelf = async () => {
      try {
        logger.userAction('PRODUCT_DETAIL_ON_SHELF', { productId: props.productId });
        await productService.onShelfProduct(props.productId, userInfo.value.phone);
        logger.info('PRODUCT_DETAIL', '产品上架成功', { productId: props.productId });
        emit('on-shelf', props.productId);
        loadProductDetail(); // 重新加载详情
      } catch (err) {
        logger.error('PRODUCT_DETAIL', '产品上架失败', {
          productId: props.productId,
          errorMessage: err.message || err
        }, err);
        alert('上架失败：' + (err.message || err));
      }
    };

    // 下架产品
    const handleOffShelf = async () => {
      try {
        logger.userAction('PRODUCT_DETAIL_OFF_SHELF', { productId: props.productId });
        await productService.offShelfProduct(props.productId, userInfo.value.phone);
        logger.info('PRODUCT_DETAIL', '产品下架成功', { productId: props.productId });
        emit('off-shelf', props.productId);
        loadProductDetail(); // 重新加载详情
      } catch (err) {
        logger.error('PRODUCT_DETAIL', '产品下架失败', {
          productId: props.productId,
          errorMessage: err.message || err
        }, err);
        alert('下架失败：' + (err.message || err));
      }
    };

    // 购买产品
    const handlePurchase = () => {
      logger.userAction('PRODUCT_DETAIL_PURCHASE', { productId: props.productId });
      emit('purchase', product.value);
    };

    return {
      product,
      loading,
      error,
      isFarmer,
      categoryText,
      loadProductDetail,
      formatDate,
      handleClose,
      handleOverlayClick,
      handleEdit,
      handleDelete,
      handleOnShelf,
      handleOffShelf,
      handlePurchase
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-container {
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--gray-200);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.close-btn:hover {
  background: var(--gray-100);
}

.close-icon {
  font-size: 1.5rem;
  color: var(--gray-500);
}

.modal-content {
  flex: 1;
  overflow-y: auto;
}

.loading-container, .error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
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

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-container h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--error);
  margin-bottom: 0.5rem;
}

.error-container p {
  color: var(--gray-500);
  margin-bottom: 1rem;
}

.retry-btn {
  padding: 0.75rem 1.5rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: var(--primary-dark);
}

.product-detail {
  padding: 2rem;
}

/* 产品头部 */
.product-header {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 2rem;
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid var(--gray-200);
}

.product-image {
  position: relative;
  height: 200px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  background: rgba(107, 70, 193, 0.1);
  border-radius: 50%;
}

.image-icon {
  font-size: 3rem;
}

.status-badge {
  position: absolute;
  top: 1rem;
  right: 1rem;
  padding: 0.5rem 1rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--white);
}

.status-draft {
  background: var(--gray-500);
}

.status-on-shelf {
  background: var(--success);
}

.status-off-shelf {
  background: #f59e0b;
}

.status-sold-out {
  background: var(--error);
}

.product-basic-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
}

.product-category {
  font-size: 1rem;
  color: var(--primary);
  font-weight: 500;
  margin: 0;
}

.product-description {
  font-size: 1rem;
  color: var(--gray-600);
  line-height: 1.6;
  margin: 0;
}

.price-section, .stock-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.price-label, .stock-label {
  font-size: 1rem;
  color: var(--gray-500);
  font-weight: 500;
}

.price-value {
  font-size: 1.5rem;
  color: var(--primary);
  font-weight: 700;
}

.stock-value {
  font-size: 1.125rem;
  color: #1a202c;
  font-weight: 600;
}

/* 详细信息 */
.product-details {
  margin-bottom: 2rem;
}

.detail-section {
  margin-bottom: 2rem;
}

.detail-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-light);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: var(--gray-100);
  border-radius: 8px;
}

.detail-label {
  font-size: 0.875rem;
  color: var(--gray-500);
  font-weight: 500;
}

.detail-value {
  font-size: 0.875rem;
  color: #1a202c;
  font-weight: 500;
}

.detail-value.status-draft {
  color: var(--gray-500);
}

.detail-value.status-on-shelf {
  color: var(--success);
}

.detail-value.status-off-shelf {
  color: #f59e0b;
}

.detail-value.status-sold-out {
  color: var(--error);
}

/* 农户信息 */
.farmer-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.farmer-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: var(--gray-100);
  border-radius: 8px;
}

.farmer-label {
  font-size: 0.875rem;
  color: var(--gray-500);
  font-weight: 500;
}

.farmer-value {
  font-size: 0.875rem;
  color: #1a202c;
  font-weight: 500;
}

/* 操作按钮 */
.action-section {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  padding-top: 2rem;
  border-top: 1px solid var(--gray-200);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon {
  font-size: 1rem;
}

.back-btn {
  background: var(--gray-200);
  color: var(--gray-600);
}

.back-btn:hover {
  background: var(--gray-300);
}

.edit-btn {
  background: #3b82f6;
  color: var(--white);
}

.edit-btn:hover {
  background: #2563eb;
}

.on-shelf-btn {
  background: var(--success);
  color: var(--white);
}

.on-shelf-btn:hover {
  background: #38a169;
}

.off-shelf-btn {
  background: #f59e0b;
  color: var(--white);
}

.off-shelf-btn:hover {
  background: #d97706;
}

.delete-btn {
  background: var(--error);
  color: var(--white);
}

.delete-btn:hover {
  background: #c53030;
}

.purchase-btn {
  background: var(--primary);
  color: var(--white);
}

.purchase-btn:hover {
  background: var(--primary-dark);
}

.disabled-btn {
  background: var(--gray-300);
  color: var(--gray-500);
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-container {
    max-height: 95vh;
  }

  .modal-header {
    padding: 1rem;
  }

  .product-detail {
    padding: 1rem;
  }

  .product-header {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .product-image {
    height: 150px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .action-section {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
