<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">可联合农户</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="product" class="product-filter-info">
          <div class="info-card">
            <h4>筛选条件（已自动设置）</h4>
            <div class="info-details">
              <div class="info-item">
                <span class="info-label">产品：</span>
                <span class="info-value">{{ product.product_name }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">申请金额：</span>
                <span class="info-value">¥{{ formatAmount(product.max_amount) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">您的可用额度：</span>
                <span class="info-value">¥{{ formatAmount(userAvailableLimit) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">所需伙伴最低额度：</span>
                <span class="info-value highlight">¥{{ formatAmount(filters.min_credit_limit || 0) }}</span>
              </div>
            </div>
            <p class="info-hint">以下为符合条件的伙伴（能承担剩余额度即可联合贷款）</p>
          </div>
        </div>
        
        <div v-else class="filter-section">
          <div class="filter-row">
            <div class="filter-group">
              <label class="filter-label">最低信用额度：</label>
              <input
                v-model.number="filters.min_credit_limit"
                type="number"
                class="filter-input"
                placeholder="不限"
                min="0"
              />
            </div>
            <div class="filter-group">
              <label class="filter-label">最大伙伴数：</label>
              <select v-model.number="filters.max_partners" class="filter-select">
                <option :value="null">不限</option>
                <option :value="2">2个</option>
                <option :value="3">3个</option>
                <option :value="4">4个</option>
                <option :value="5">5个</option>
              </select>
            </div>
            <button class="btn btn-primary" @click="loadPartners">搜索</button>
          </div>
        </div>

        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="error" class="error-container">
          <span class="error-icon">⚠️</span>
          <span>{{ error }}</span>
        </div>

        <div v-else-if="partners.length === 0" class="empty-container">
          <div class="empty-icon">👥</div>
          <p>暂无可联合的农户</p>
        </div>

        <div v-else class="partners-list">
          <div
            v-for="partner in partners"
            :key="partner.phone"
            class="partner-card"
          >
            <div class="partner-info">
              <div class="partner-header">
                <h3 class="partner-name">{{ partner.nickname || '农户' }}</h3>
                <span class="partner-phone">{{ partner.phone }}</span>
              </div>
              <div class="partner-details">
                <div class="detail-item">
                  <span class="detail-label">信用额度：</span>
                  <span class="detail-value">¥{{ formatAmount(partner.total_credit_limit || 0) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">可用额度：</span>
                  <span class="detail-value highlight">¥{{ formatAmount(partner.available_credit_limit || 0) }}</span>
                </div>
              </div>
            </div>
            <button class="btn btn-primary" @click="handleSelect(partner)">
              选择此伙伴
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'JointPartnersModal',
  props: {
    product: {
      type: Object,
      default: null
    }
  },
  emits: ['close', 'select'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const partners = ref([]);
    const loading = ref(false);
    const error = ref('');
    const userAvailableLimit = ref(0); // 发起人的可用额度
    const filters = reactive({
      min_credit_limit: null,
      max_partners: null,
      exclude_phones: []
    });

    // 获取用户可用额度并计算所需伙伴最低额度
    const calculateRequiredPartnerLimit = async () => {
      if (!props.product || !userInfo.value.phone) {
        logger.warn('JOINT_PARTNERS', '缺少必要参数', {
          hasProduct: !!props.product,
          hasPhone: !!userInfo.value.phone
        });
        return;
      }

      try {
        // 获取发起人的可用额度
        logger.info('JOINT_PARTNERS', '开始获取用户可用额度', {
          phone: userInfo.value.phone
        });
        const creditLimitData = await financingService.getCreditLimit(userInfo.value.phone);
        
        // 调试：输出接收到的数据
        console.log('DEBUG: JointPartnersModal 获取到的额度数据:', creditLimitData);
        console.log('DEBUG: available_limit =', creditLimitData?.available_limit);
        
        // 确保正确解析数值
        const availableLimit = creditLimitData?.available_limit;
        if (availableLimit !== undefined && availableLimit !== null) {
          userAvailableLimit.value = parseFloat(availableLimit);
        } else {
          userAvailableLimit.value = 0;
          logger.warn('JOINT_PARTNERS', '可用额度数据为空，使用默认值0');
        }

        logger.info('JOINT_PARTNERS', '用户可用额度获取成功', {
          available_limit: userAvailableLimit.value
        });

        // 计算所需伙伴最低额度 = 贷款金额 - 发起人当前可用额度
        const loanAmount = parseFloat(props.product.max_amount || 0);
        const requiredAmount = loanAmount - userAvailableLimit.value;

        // 所需伙伴最低额度就是贷款金额减去发起人可用额度（必须大于0）
        filters.min_credit_limit = requiredAmount > 0 ? requiredAmount : 0;
        filters.max_partners = 5; // 最多显示5个符合条件的伙伴
        
        logger.info('JOINT_PARTNERS', '计算所需伙伴最低额度', {
          loanAmount,
          userAvailableLimit: userAvailableLimit.value,
          requiredAmount,
          min_credit_limit: filters.min_credit_limit
        });
      } catch (err) {
        logger.error('JOINT_PARTNERS', '获取用户额度失败', {
          errorMessage: err.message || err,
          phone: userInfo.value.phone
        }, err);
        // 如果获取额度失败，使用默认计算方式（假设用户额度为0）
        userAvailableLimit.value = 0;
        const loanAmount = parseFloat(props.product.max_amount || 0);
        filters.min_credit_limit = loanAmount;
        logger.warn('JOINT_PARTNERS', '使用默认值计算所需伙伴最低额度', {
          min_credit_limit: filters.min_credit_limit
        });
      }
    };

    onMounted(async () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        filters.exclude_phones = [userInfo.value.phone];
        
        // 如果传入了产品信息，获取用户额度并自动设置筛选条件
        if (props.product) {
          await calculateRequiredPartnerLimit();
        }
        
        loadPartners();
      }
    });

    const loadPartners = async () => {
      if (!userInfo.value.phone) {
        error.value = '请先登录';
        return;
      }

      loading.value = true;
      error.value = '';
      try {
        logger.info('FINANCING', '开始加载可联合农户', { 
          phone: userInfo.value.phone,
          filters
        });

        const requestData = {
          phone: userInfo.value.phone,
          ...(filters.min_credit_limit && { min_credit_limit: filters.min_credit_limit }),
          ...(filters.max_partners && { max_partners: filters.max_partners }),
          exclude_phones: filters.exclude_phones
        };

        const data = await financingService.getJointPartners(requestData);
        partners.value = data.partners || [];
        
        logger.info('FINANCING', '可联合农户加载成功', { count: partners.value.length });
      } catch (err) {
        logger.error('FINANCING', '加载可联合农户失败', {
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

    const handleClose = () => {
      emit('close');
    };

    const handleSelect = (partner) => {
      logger.userAction('SELECT_PARTNER', { partnerPhone: partner.phone });
      emit('select', [partner]);
      // 选择后自动关闭弹窗（因为只能选择1个）
      handleClose();
    };

    return {
      userInfo,
      partners,
      loading,
      error,
      userAvailableLimit,
      filters,
      formatAmount,
      loadPartners,
      handleClose,
      handleSelect
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
  max-width: 800px;
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

.product-filter-info {
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
}

.info-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 1.5rem;
}

.info-card h4 {
  margin: 0 0 1rem 0;
  color: var(--primary);
  font-size: 1.125rem;
}

.info-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.info-label {
  color: var(--gray-600);
  font-weight: 500;
}

.info-value {
  color: #1a202c;
  font-weight: 600;
}

.info-value.highlight {
  color: var(--primary);
  font-size: 1rem;
}

.info-hint {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--gray-600);
  font-style: italic;
}

.filter-section {
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
}

.filter-row {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-label {
  font-size: 0.875rem;
  color: var(--gray-600);
  font-weight: 500;
}

.filter-input, .filter-select {
  padding: 0.5rem;
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  font-size: 0.875rem;
  min-width: 120px;
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

.partners-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.partner-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  transition: all 0.3s;
}

.partner-card:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.1);
}

.partner-info {
  flex: 1;
}

.partner-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.partner-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.partner-phone {
  font-size: 0.875rem;
  color: var(--gray-500);
  background: var(--gray-100);
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
}

.partner-details {
  display: flex;
  gap: 1.5rem;
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
</style>

