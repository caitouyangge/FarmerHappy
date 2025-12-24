<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">可申请的贷款产品</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="error" class="error-container">
          <span class="error-icon">⚠️</span>
          <span>{{ error }}</span>
        </div>

        <div v-else-if="products.length === 0" class="empty-container">
          <div class="empty-icon">📋</div>
          <p>暂无可申请的贷款产品</p>
        </div>

        <div v-else class="products-list">
          <div
            v-for="product in products"
            :key="product.product_id"
            class="product-card"
          >
            <div class="product-header">
              <h3 class="product-name">{{ product.product_name }}</h3>
              <span v-if="product.product_code" class="product-code">{{ product.product_code }}</span>
            </div>

            <div class="product-details">
              <div class="detail-item">
                <span class="detail-label">贷款额度：</span>
                <span class="detail-value">¥{{ formatAmount(product.max_amount) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">年利率：</span>
                <span class="detail-value highlight">{{ (product.interest_rate || 0).toFixed(2) }}%</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">贷款期限：</span>
                <span class="detail-value">{{ product.term_months }} 个月</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">还款方式：</span>
                <span class="detail-value">{{ getRepaymentMethodText(product.repayment_method) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">最低信用额度要求：</span>
                <span class="detail-value">¥{{ formatAmount(product.min_credit_limit) }}</span>
              </div>
            </div>

            <div v-if="product.description" class="product-description">
              <span class="description-label">产品描述：</span>
              <p>{{ product.description }}</p>
            </div>

            <div class="product-actions">
              <button class="btn btn-primary btn-main" @click="handleApply(product, 'unified')">
                🧠 智能贷款申请（推荐）
              </button>
              <div class="traditional-options">
                <span class="traditional-hint">或选择传统方式：</span>
                <button class="btn btn-link" @click="handleApply(product, 'single')">
                  单人贷款
                </button>
                <span class="separator">|</span>
                <button class="btn btn-link" @click="handleApply(product, 'joint')">
                  联合贷款
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'LoanProductListModal',
  emits: ['close', 'apply'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const products = ref([]);
    const loading = ref(false);
    const error = ref('');

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        loadProducts();
      }
    });

    const loadProducts = async () => {
      if (!userInfo.value.phone) {
        error.value = '请先登录';
        return;
      }

      loading.value = true;
      error.value = '';
      try {
        logger.info('FINANCING', '开始加载贷款产品列表', { phone: userInfo.value.phone });
        const data = await financingService.getAvailableLoanProducts(userInfo.value.phone);
        products.value = data.available_products || [];
        logger.info('FINANCING', '贷款产品列表加载成功', { count: products.value.length });
      } catch (err) {
        logger.error('FINANCING', '加载贷款产品列表失败', {
          errorMessage: err.message || err
        }, err);
        error.value = err.message || '加载失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    };

    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    const getRepaymentMethodText = (method) => {
      const methodMap = {
        equal_installment: '等额本息',
        interest_first: '先息后本',
        bullet_repayment: '一次性还本付息'
      };
      return methodMap[method] || method;
    };

    const handleClose = () => {
      emit('close');
    };

    const handleApply = (product, loanType) => {
      logger.userAction('APPLY_LOAN', { productId: product.product_id, loanType });
      emit('apply', product, loanType);
    };

    return {
      userInfo,
      products,
      loading,
      error,
      formatAmount,
      getRepaymentMethodText,
      handleClose,
      handleApply
    };
  }
};
</script>

<style scoped>
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
}

.modal-container {
  background: var(--white);
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-container.large {
  max-width: 900px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
  position: sticky;
  top: 0;
  background: var(--white);
  z-index: 10;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 2rem;
  color: var(--gray-400);
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.btn-close:hover {
  background: var(--gray-100);
  color: var(--gray-600);
}

.modal-body {
  padding: 1.5rem;
}

.loading-container, .error-container, .empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
  text-align: center;
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

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.products-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-card {
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s;
}

.product-card:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.1);
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.product-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.product-code {
  font-size: 0.875rem;
  color: var(--gray-500);
  background: var(--gray-100);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.product-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.detail-label {
  color: var(--gray-500);
}

.detail-value {
  color: #1a202c;
  font-weight: 500;
}

.detail-value.highlight {
  color: var(--primary);
  font-weight: 600;
  font-size: 1rem;
}

.product-description {
  margin-bottom: 1rem;
  padding: 1rem;
  background: var(--gray-50);
  border-radius: 8px;
}

.description-label {
  font-size: 0.875rem;
  color: var(--gray-600);
  font-weight: 500;
  display: block;
  margin-bottom: 0.5rem;
}

.product-description p {
  margin: 0;
  color: var(--gray-600);
  line-height: 1.6;
  font-size: 0.875rem;
}

.product-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.btn-main {
  font-size: 1rem !important;
  padding: 1rem 2rem !important;
  font-weight: 700 !important;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.2);
}

.btn-main:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(107, 70, 193, 0.3);
}

.traditional-options {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--gray-200);
  opacity: 0.7;
  font-size: 0.8125rem;
}

.traditional-options:hover {
  opacity: 1;
}

.traditional-hint {
  color: var(--gray-500);
  font-size: 0.75rem;
  margin-right: 0.5rem;
}

.separator {
  color: var(--gray-400);
  margin: 0 0.25rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--primary);
  color: var(--white);
}

.btn-primary:hover {
  background: var(--primary-dark);
}

.btn-secondary {
  background: var(--white);
  color: var(--primary);
  border: 1px solid var(--primary);
}

.btn-secondary:hover {
  background: var(--primary-light);
  color: var(--white);
}

.btn-link {
  background: transparent;
  color: var(--primary);
  border: none;
  padding: 0.25rem 0.5rem;
  font-size: 0.8125rem;
  text-decoration: underline;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-link:hover {
  color: var(--primary-dark);
  text-decoration: none;
}
</style>

