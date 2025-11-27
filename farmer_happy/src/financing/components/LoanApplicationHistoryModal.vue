<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">贷款申请记录</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="applications.length === 0" class="empty-container">
          <div class="empty-icon">📋</div>
          <h3>暂无贷款申请记录</h3>
          <p>您还没有提交过贷款申请，点击申请按钮开始申请贷款</p>
          <button class="btn btn-primary" @click="handleApply">
            申请贷款
          </button>
        </div>

        <div v-else>
          <div class="applications-header">
            <h3>贷款申请记录</h3>
            <span class="applications-count">共 {{ applications.length }} 条记录</span>
          </div>

          <div class="applications-list">
            <div 
              v-for="application in applications" 
              :key="application.loan_application_id"
              class="application-item"
              :class="getStatusClass(application.status)"
            >
              <div class="application-header">
                <div class="application-id">
                  <span class="label">申请编号：</span>
                  <span class="value">{{ application.loan_application_id }}</span>
                </div>
                <div class="application-status">
                  <span class="status-badge" :class="getStatusClass(application.status)">
                    {{ getStatusText(application.status) }}
                  </span>
                </div>
              </div>

              <div class="application-details">
                <div class="detail-grid">
                  <div class="detail-item">
                    <span class="detail-label">贷款产品：</span>
                    <span class="detail-value">{{ application.product_name }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">申请类型：</span>
                    <span class="detail-value">{{ getApplicationTypeText(application.application_type) }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">申请金额：</span>
                    <span class="detail-value amount">¥{{ formatAmount(application.apply_amount) }}</span>
                  </div>
                  <div class="detail-item" v-if="application.approved_amount">
                    <span class="detail-label">批准金额：</span>
                    <span class="detail-value amount approved">¥{{ formatAmount(application.approved_amount) }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">年利率：</span>
                    <span class="detail-value">{{ (application.interest_rate || 0).toFixed(2) }}%</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">贷款期限：</span>
                    <span class="detail-value">{{ application.term_months }} 个月</span>
                  </div>
                  <div class="detail-item full-width">
                    <span class="detail-label">贷款用途：</span>
                    <span class="detail-value">{{ application.purpose }}</span>
                  </div>
                  <div class="detail-item full-width">
                    <span class="detail-label">还款来源：</span>
                    <span class="detail-value">{{ application.repayment_source }}</span>
                  </div>
                </div>

                <!-- 审批信息 -->
                <div v-if="application.status !== 'pending'" class="approval-info">
                  <div class="approval-header">审批信息</div>
                  <div class="approval-details">
                    <div v-if="application.approver_name" class="approval-item">
                      <span class="detail-label">审批人：</span>
                      <span class="detail-value">{{ application.approver_name }}</span>
                      <span v-if="application.approver_bank" class="bank-info">({{ application.approver_bank }})</span>
                    </div>
                    <div v-if="application.approved_at" class="approval-item">
                      <span class="detail-label">审批时间：</span>
                      <span class="detail-value">{{ formatDateTime(application.approved_at) }}</span>
                    </div>
                    <div v-if="application.reject_reason" class="approval-item reject-reason">
                      <span class="detail-label">拒绝原因：</span>
                      <span class="detail-value">{{ application.reject_reason }}</span>
                    </div>
                  </div>
                </div>

                <!-- 时间信息 -->
                <div class="time-info">
                  <div class="time-item">
                    <span class="detail-label">申请时间：</span>
                    <span class="detail-value">{{ formatDateTime(application.created_at) }}</span>
                  </div>
                  <div v-if="application.updated_at && application.updated_at !== application.created_at" class="time-item">
                    <span class="detail-label">更新时间：</span>
                    <span class="detail-value">{{ formatDateTime(application.updated_at) }}</span>
                  </div>
                </div>
              </div>

              <!-- 状态说明 -->
              <div class="status-description">
                <div class="status-text">
                  {{ getStatusDescription(application.status) }}
                </div>
                <div v-if="application.status === 'approved'" class="status-action">
                  <span class="action-hint">✅ 等待银行放款</span>
                </div>
                <div v-else-if="application.status === 'disbursed'" class="status-action">
                  <span class="action-hint">✅ 贷款已成功发放</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="handleClose">关闭</button>
        <button class="btn btn-primary" @click="handleApply">申请新贷款</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'LoanApplicationHistoryModal',
  emits: ['close', 'apply'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const loading = ref(false);
    const applications = ref([]);

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        loadApplications();
      }
    });

    const loadApplications = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      loading.value = true;
      try {
        logger.info('FINANCING', '获取贷款申请记录', { phone: userInfo.value.phone });
        
        const response = await financingService.getFarmerLoanApplications(userInfo.value.phone);
        applications.value = response.applications || [];
        
        logger.info('FINANCING', '获取贷款申请记录成功', { 
          count: applications.value.length 
        });
      } catch (error) {
        logger.error('FINANCING', '获取贷款申请记录失败', {
          errorMessage: error.message || error
        }, error);
        alert('获取申请记录失败：' + (error.message || '请稍后重试'));
      } finally {
        loading.value = false;
      }
    };

    const handleClose = () => {
      emit('close');
    };

    const handleApply = () => {
      emit('apply');
    };

    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    const formatDateTime = (dateStr) => {
      if (!dateStr) return '-';
      return new Date(dateStr).toLocaleString('zh-CN');
    };

    const getStatusClass = (status) => {
      switch (status) {
        case 'pending': return 'status-pending';
        case 'pending_partners': return 'status-pending';
        case 'approved': return 'status-approved';
        case 'rejected': return 'status-rejected';
        case 'disbursed': return 'status-disbursed';
        default: return '';
      }
    };

    const getStatusText = (status) => {
      switch (status) {
        case 'pending': return '待审批';
        case 'pending_partners': return '待伙伴确认';
        case 'approved': return '已批准';
        case 'rejected': return '已拒绝';
        case 'disbursed': return '已放款';
        default: return status;
      }
    };

    const getApplicationTypeText = (type) => {
      switch (type) {
        case 'single': return '单人贷款';
        case 'joint': return '联合贷款';
        default: return type;
      }
    };

    const getStatusDescription = (status) => {
      switch (status) {
        case 'pending': return '您的贷款申请正在银行审批中，请耐心等待审批结果。';
        case 'pending_partners': return '您的联合贷款申请正在等待合作伙伴确认，请联系相关伙伴完成确认。';
        case 'approved': return '恭喜您！贷款申请已获得批准，银行将尽快安排放款。';
        case 'rejected': return '很遗憾，您的贷款申请被拒绝。您可以根据拒绝原因调整后重新申请。';
        case 'disbursed': return '贷款已成功发放到您的账户，请按时还款。';
        default: return '';
      }
    };

    return {
      userInfo,
      loading,
      applications,
      loadApplications,
      handleClose,
      handleApply,
      formatAmount,
      formatDateTime,
      getStatusClass,
      getStatusText,
      getApplicationTypeText,
      getStatusDescription
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
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.modal-container.large {
  max-width: 1000px;
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
  flex: 1;
  padding: 1.5rem;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--gray-200);
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  background: var(--gray-50);
}

.loading-container, .empty-container {
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

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-container h3 {
  color: var(--gray-600);
  margin-bottom: 0.5rem;
}

.empty-container p {
  color: var(--gray-500);
  margin-bottom: 2rem;
}

.applications-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--primary-light);
}

.applications-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary);
}

.applications-count {
  background: var(--primary-light);
  color: var(--primary);
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 500;
}

.applications-list {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.application-item {
  background: var(--white);
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s;
}

.application-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.application-item.status-pending {
  border-color: var(--warning);
  background: var(--warning-light);
}

.application-item.status-approved {
  border-color: var(--success);
  background: var(--success-light);
}

.application-item.status-rejected {
  border-color: var(--error);
  background: var(--error-light);
}

.application-item.status-disbursed {
  border-color: var(--primary);
  background: var(--primary-light);
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.application-id .label {
  color: var(--gray-600);
  font-size: 0.875rem;
}

.application-id .value {
  font-weight: 600;
  color: var(--primary);
  font-size: 1rem;
}

.status-badge {
  padding: 0.375rem 0.875rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
}

.status-badge.status-pending {
  background: var(--warning);
  color: var(--white);
}

.status-badge.status-approved {
  background: var(--success);
  color: var(--white);
}

.status-badge.status-rejected {
  background: var(--error);
  color: var(--white);
}

.status-badge.status-disbursed {
  background: var(--primary);
  color: var(--white);
}

.application-details {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-label {
  color: var(--gray-600);
  min-width: fit-content;
  font-weight: 500;
}

.detail-value {
  color: #1a202c;
  font-weight: 500;
  flex: 1;
}

.detail-value.amount {
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary);
}

.detail-value.approved {
  color: var(--success);
}

.approval-info {
  background: var(--gray-50);
  border-radius: 8px;
  padding: 1rem;
}

.approval-header {
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 0.75rem;
  font-size: 0.95rem;
}

.approval-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.approval-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.approval-item.reject-reason {
  background: var(--error-light);
  padding: 0.75rem;
  border-radius: 6px;
  margin-top: 0.5rem;
}

.approval-item.reject-reason .detail-value {
  color: var(--error);
  font-weight: 600;
}

.bank-info {
  color: var(--gray-500);
  font-size: 0.8rem;
  font-style: italic;
}

.time-info {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.time-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--gray-500);
}

.status-description {
  background: var(--gray-50);
  border-radius: 8px;
  padding: 1rem;
  margin-top: 1rem;
}

.status-text {
  color: var(--gray-700);
  line-height: 1.6;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.status-action {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.action-hint {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--success);
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

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .applications-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
  
  .application-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
  
  .time-info {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>
