<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">可联合农户</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div class="filter-section">
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
                  <span class="detail-value highlight">¥{{ formatAmount(partner.credit_limit || 0) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">可用额度：</span>
                  <span class="detail-value">¥{{ formatAmount(partner.available_limit || 0) }}</span>
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
  emits: ['close', 'select'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const partners = ref([]);
    const loading = ref(false);
    const error = ref('');
    const filters = reactive({
      min_credit_limit: null,
      max_partners: null,
      exclude_phones: []
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        filters.exclude_phones = [userInfo.value.phone];
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
    };

    return {
      userInfo,
      partners,
      loading,
      error,
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

