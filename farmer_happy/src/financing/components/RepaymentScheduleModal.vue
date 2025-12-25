<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">还款计划</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <!-- 贷款列表视图 -->
        <div v-if="!selectedLoanId" class="loans-list-view">
          <div class="view-header">
            <h3>我的贷款记录</h3>
            <button class="btn btn-secondary" @click="loadLoans">
              <span class="refresh-icon">🔄</span>
              刷新
            </button>
          </div>

          <div v-if="loadingLoans" class="loading-container">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <div v-else-if="loansError" class="error-container">
            <span class="error-icon">⚠️</span>
            <span>{{ loansError }}</span>
          </div>

          <div v-else-if="loans.length === 0" class="empty-container">
            <div class="empty-icon">📋</div>
            <p>暂无贷款记录</p>
          </div>

          <div v-else class="loans-list">
            <div
              v-for="loan in loans"
              :key="loan.loan_id"
              class="loan-card"
              @click="viewSchedule(loan.loan_id)"
            >
              <div class="loan-header">
                <span class="loan-id">贷款ID：{{ loan.loan_id }}</span>
                <span :class="['loan-status', `status-${loan.loan_status || 'pending'}`]">
                  {{ getLoanStatusText(loan.loan_status) }}
                </span>
              </div>
              <div class="loan-body">
                <div class="loan-detail">
                  <span class="detail-label">贷款金额：</span>
                  <span class="detail-value highlight">¥{{ formatAmount(loan.loan_amount) }}</span>
                </div>
                <div class="loan-detail">
                  <span class="detail-label">年利率：</span>
                  <span class="detail-value">{{ (loan.interest_rate || 0).toFixed(2) }}%</span>
                </div>
                <div class="loan-detail">
                  <span class="detail-label">贷款期限：</span>
                  <span class="detail-value">{{ loan.term_months }} 个月</span>
                </div>
                <div class="loan-detail">
                  <span class="detail-label">已还金额：</span>
                  <span class="detail-value">¥{{ formatAmount(loan.total_paid_amount || 0) }}</span>
                </div>
              </div>
              <div class="loan-footer">
                <button class="btn btn-primary" @click.stop="viewSchedule(loan.loan_id)">
                  查看还款计划
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 还款计划详情视图 -->
        <div v-else class="schedule-detail-view">
          <div class="view-header">
            <button class="btn btn-secondary" @click="backToList">
              <span class="back-icon">←</span>
              返回列表
            </button>
            <h3>还款计划详情</h3>
          </div>

          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>

          <div v-else-if="error" class="error-container">
            <span class="error-icon">⚠️</span>
            <span>{{ error }}</span>
          </div>

          <div v-else-if="schedule" class="schedule-content">
            <!-- 贷款概览 -->
            <div class="loan-overview">
              <h3 class="overview-title">贷款信息</h3>
              <div class="overview-grid">
                <div class="overview-item">
                  <span class="overview-label">贷款ID：</span>
                  <span class="overview-value">{{ schedule.loan_id }}</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">贷款金额：</span>
                  <span class="overview-value highlight">¥{{ formatAmount(schedule.loan_amount) }}</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">年利率：</span>
                  <span class="overview-value">{{ (schedule.interest_rate || 0).toFixed(2) }}%</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">贷款期限：</span>
                  <span class="overview-value">{{ schedule.term_months }} 个月</span>
                </div>
              </div>
            </div>

            <!-- 简化还款区域 -->
            <div class="simple-repayment-container">
              <h3 class="table-title">贷款还款</h3>
              <div class="repayment-summary">
                <div class="summary-grid">
                  <div class="summary-item">
                    <span class="summary-label">贷款总额：</span>
                    <span class="summary-value">¥{{ formatAmount(schedule.loan_amount) }}</span>
                  </div>
                  <div class="summary-item">
                    <span class="summary-label">已还金额：</span>
                    <span class="summary-value">¥{{ formatAmount(schedule.summary?.total_paid || 0) }}</span>
                  </div>
                </div>
                
                <div v-if="getRemainingDebt() > 0 && schedule.loan_status === 'active'" class="repayment-action">
                  <div class="repayment-form">
                    <div class="form-group">
                      <label>还款金额（¥）</label>
                      <input 
                        type="number" 
                        v-model="repaymentAmount"
                        :max="getRemainingDebt()"
                        :min="1"
                        step="0.01"
                        class="form-control"
                        placeholder="输入要还款的金额"
                      />
                    </div>
                    <div class="form-actions">
                      <button 
                        class="btn btn-outline btn-sm"
                        @click="setFullRepayment"
                      >
                        全额还清
                      </button>
                      <button 
                        class="btn btn-primary"
                        @click="executeRepayment"
                        :disabled="!repaymentAmount || repaymentAmount <= 0 || repaymentAmount > getRemainingDebt() || processingRepayment"
                      >
                        {{ processingRepayment ? '处理中...' : '确认还款' }}
                      </button>
                    </div>
                  </div>
                </div>
                
                <div v-else-if="getRemainingDebt() <= 0" class="repayment-completed">
                  <div class="completed-icon">✅</div>
                  <p class="completed-text">贷款已全部还清</p>
                </div>
                
                <div v-else class="repayment-disabled">
                  <p class="disabled-text">当前贷款状态不允许还款</p>
                </div>
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
  name: 'RepaymentScheduleModal',
  emits: ['close'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const loans = ref([]);
    const selectedLoanId = ref('');
    const schedule = ref(null);
    const loading = ref(false);
    const loadingLoans = ref(false);
    const error = ref('');
    const loansError = ref('');
    const processingRepayment = ref(false);
    const repaymentAmount = ref('');

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        loadLoans();
      }
    });

    // 加载贷款列表
    const loadLoans = async () => {
      if (!userInfo.value.phone) {
        loansError.value = '请先登录';
        return;
      }

      loadingLoans.value = true;
      loansError.value = '';
      loans.value = [];

      try {
        logger.info('FINANCING', '开始加载贷款列表', { phone: userInfo.value.phone });
        
        const data = await financingService.getFarmerLoans(userInfo.value.phone);
        loans.value = data.loans || [];
        
        logger.info('FINANCING', '贷款列表加载成功', { count: loans.value.length });
      } catch (err) {
        logger.error('FINANCING', '加载贷款列表失败', {
          errorMessage: err.message || err
        }, err);
        loansError.value = err.message || '加载失败，请稍后重试';
      } finally {
        loadingLoans.value = false;
      }
    };

    // 查看还款计划
    const viewSchedule = async (loanId) => {
      selectedLoanId.value = loanId;
      await loadSchedule(loanId);
    };

    // 返回列表
    const backToList = () => {
      selectedLoanId.value = '';
      schedule.value = null;
      error.value = '';
    };

    // 加载还款计划
    const loadSchedule = async (loanId) => {
      if (!userInfo.value.phone) {
        error.value = '请先登录';
        return;
      }

      loading.value = true;
      error.value = '';
      schedule.value = null;

      try {
        logger.info('FINANCING', '开始加载还款计划', { 
          phone: userInfo.value.phone,
          loanId
        });

        const data = await financingService.getRepaymentSchedule(
          userInfo.value.phone,
          loanId
        );
        schedule.value = data;
        
        logger.info('FINANCING', '还款计划加载成功', { loanId });
      } catch (err) {
        logger.error('FINANCING', '加载还款计划失败', {
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

    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    const getRepaymentMethodText = (method) => {
      const methodMap = {
        equal_installment: '等额本息',
        interest_first: '先息后本',
        bullet_repayment: '一次性还本付息'
      };
      return methodMap[method] || method;
    };

    const getStatusText = (status) => {
      const statusMap = {
        paid: '已还',
        pending: '待还',
        overdue: '逾期'
      };
      return statusMap[status] || status || '待还';
    };

    const getLoanStatusText = (status) => {
      const statusMap = {
        pending: '待审批',
        approved: '已批准',
        rejected: '已拒绝',
        disbursed: '已放款',
        active: '进行中',
        completed: '已完成',
        closed: '已关闭'
      };
      return statusMap[status] || status || '未知';
    };

    // 计算剩余欠款
    const getRemainingDebt = () => {
      if (!schedule.value) return 0;
      
      // 优先使用summary中的remaining_total
      if (schedule.value.summary?.remaining_total) {
        return schedule.value.summary.remaining_total;
      }
      
      // 如果没有summary，简单计算：剩余本金
      if (schedule.value.remaining_principal) {
        return schedule.value.remaining_principal;
      }
      
      // 最后兜底：总应还款额 - 已还款额
      const totalAmount = schedule.value.loan_amount || 0;
      const paidAmount = schedule.value.total_paid || 0;
      return Math.max(0, totalAmount - paidAmount);
    };

    // 设置全额还款
    const setFullRepayment = () => {
      repaymentAmount.value = getRemainingDebt();
    };

    // 执行还款
    const executeRepayment = async () => {
      if (!repaymentAmount.value || repaymentAmount.value <= 0) {
        alert('请输入有效的还款金额');
        return;
      }

      if (repaymentAmount.value > getRemainingDebt()) {
        alert('还款金额不能超过剩余欠款');
        return;
      }

      processingRepayment.value = true;

      try {
        logger.info('FINANCING', '开始执行还款', {
          loanId: selectedLoanId.value,
          amount: repaymentAmount.value
        });

        // 判断还款类型
        const repaymentType = repaymentAmount.value >= getRemainingDebt() ? 'advance' : 'partial';
        
        const result = await financingService.makeRepayment(
          userInfo.value.phone,
          selectedLoanId.value,
          parseFloat(repaymentAmount.value),
          repaymentType, // partial: 部分还款, advance: 提前全额还款
          '', // 不需要账户信息
          `${repaymentType === 'advance' ? '提前全额还款' : '部分还款'}：${repaymentAmount.value}元`
        );

        logger.info('FINANCING', '还款执行成功', { result });

        // 显示成功消息
        alert(`还款成功！已还款 ¥${repaymentAmount.value}`);

        // 重置还款金额
        repaymentAmount.value = '';

        // 重新加载还款计划
        await loadSchedule(selectedLoanId.value);

        // 重新加载贷款列表
        await loadLoans();

      } catch (err) {
        logger.error('FINANCING', '还款执行失败', {
          errorMessage: err.message || err
        }, err);
        alert(err.message || '还款失败，请稍后重试');
      } finally {
        processingRepayment.value = false;
      }
    };

    const handleClose = () => {
      emit('close');
    };

    return {
      userInfo,
      loans,
      selectedLoanId,
      schedule,
      loading,
      loadingLoans,
      error,
      loansError,
      processingRepayment,
      repaymentAmount,
      formatAmount,
      formatDate,
      getRepaymentMethodText,
      getStatusText,
      getLoanStatusText,
      loadLoans,
      viewSchedule,
      backToList,
      getRemainingDebt,
      setFullRepayment,
      executeRepayment,
      handleClose
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
  padding: 1.5rem;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.view-header h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
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

/* 贷款列表样式 */
.loans-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.loan-card {
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s;
}

.loan-card:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.1);
}

.loan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--gray-200);
}

.loan-id {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1a202c;
}

.loan-status {
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
}

.status-approved {
  background: #dbeafe;
  color: #1e40af;
}

.status-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-disbursed, .status-active {
  background: #dcfce7;
  color: #166534;
}

.status-completed {
  background: #e0f2fe;
  color: #0369a1;
}

.status-closed {
  background: var(--gray-200);
  color: var(--gray-600);
}

.loan-body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.loan-detail {
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

.loan-footer {
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
  display: flex;
  justify-content: flex-end;
}

/* 还款计划详情样式 */
.schedule-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.loan-overview {
  background: var(--gray-50);
  padding: 1.5rem;
  border-radius: 12px;
}

.overview-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.overview-label {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.overview-value {
  font-size: 1rem;
  color: #1a202c;
  font-weight: 500;
}

.overview-value.highlight {
  color: var(--primary);
  font-weight: 600;
  font-size: 1.125rem;
}

.schedule-table-container {
  overflow-x: auto;
}

.table-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
}

.schedule-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.schedule-table thead {
  background: var(--gray-50);
}

.schedule-table th {
  padding: 0.75rem;
  text-align: left;
  font-weight: 600;
  color: var(--gray-600);
  border-bottom: 2px solid var(--gray-200);
}

.schedule-table td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--gray-200);
  color: #1a202c;
}

.schedule-table tbody tr:hover {
  background: var(--gray-50);
}

.total-amount {
  font-weight: 600;
  color: var(--primary);
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-paid {
  background: #dcfce7;
  color: #166534;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
}

.status-overdue {
  background: #fee2e2;
  color: #991b1b;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary {
  background: var(--primary);
  color: var(--white);
}

.btn-primary:hover {
  background: var(--primary-dark);
}

.btn-secondary {
  background: var(--gray-200);
  color: var(--gray-600);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-secondary:hover {
  background: var(--gray-300);
}

.refresh-icon, .back-icon {
  font-size: 1rem;
}

/* 简化还款样式 */
.simple-repayment-container {
  background: var(--white);
  border-radius: 12px;
  border: 1px solid var(--gray-200);
  overflow: hidden;
}

.repayment-summary {
  padding: 1.5rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid var(--gray-200);
}

.summary-item.highlight {
  border-color: var(--primary);
  background: var(--primary-light);
}

.summary-label {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin-bottom: 0.5rem;
}

.summary-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a202c;
}

.summary-item.highlight .summary-value {
  color: var(--primary);
}

.repayment-action {
  background: var(--gray-50);
  padding: 1.5rem;
  border-radius: 8px;
  border: 1px solid var(--gray-200);
}

.repayment-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #1a202c;
}

.form-control {
  padding: 0.75rem;
  border: 2px solid var(--gray-200);
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary);
}

.form-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.btn-outline {
  background: transparent;
  border: 2px solid var(--primary);
  color: var(--primary);
}

.btn-outline:hover {
  background: var(--primary);
  color: var(--white);
}

.repayment-completed {
  text-align: center;
  padding: 2rem;
  background: #f0fdf4;
  border: 1px solid #16a34a;
  border-radius: 8px;
}

.completed-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.completed-text {
  font-size: 1.125rem;
  font-weight: 600;
  color: #16a34a;
  margin: 0;
}

.repayment-disabled {
  text-align: center;
  padding: 2rem;
  background: var(--gray-100);
  border: 1px solid var(--gray-300);
  border-radius: 8px;
}

.disabled-text {
  font-size: 1rem;
  color: var(--gray-600);
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid var(--gray-200);
  background: var(--gray-50);
  border-radius: 0 0 16px 16px;
}
</style>