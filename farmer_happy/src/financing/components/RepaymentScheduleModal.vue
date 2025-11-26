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
                  <span class="detail-value">{{ (loan.interest_rate * 100).toFixed(2) }}%</span>
                </div>
                <div class="loan-detail">
                  <span class="detail-label">贷款期限：</span>
                  <span class="detail-value">{{ loan.term_months }} 个月</span>
                </div>
                <div class="loan-detail">
                  <span class="detail-label">已还金额：</span>
                  <span class="detail-value">¥{{ formatAmount(loan.total_paid_amount || 0) }}</span>
                </div>
                <div class="loan-detail">
                  <span class="detail-label">剩余金额：</span>
                  <span class="detail-value highlight">¥{{ formatAmount(loan.remaining_principal || loan.loan_amount) }}</span>
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
                  <span class="overview-value">{{ (schedule.interest_rate * 100).toFixed(2) }}%</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">贷款期限：</span>
                  <span class="overview-value">{{ schedule.term_months }} 个月</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">还款方式：</span>
                  <span class="overview-value">{{ getRepaymentMethodText(schedule.repayment_method) }}</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">已还金额：</span>
                  <span class="overview-value">¥{{ formatAmount(schedule.total_paid || 0) }}</span>
                </div>
                <div class="overview-item">
                  <span class="overview-label">剩余金额：</span>
                  <span class="overview-value highlight">¥{{ formatAmount(schedule.remaining_amount || 0) }}</span>
                </div>
              </div>
            </div>

            <!-- 还款计划表格 -->
            <div class="schedule-table-container">
              <h3 class="table-title">还款明细</h3>
              <table class="schedule-table">
                <thead>
                  <tr>
                    <th>期数</th>
                    <th>还款日期</th>
                    <th>本金</th>
                    <th>利息</th>
                    <th>合计</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in schedule.repayment_plan" :key="index">
                    <td>{{ item.period || index + 1 }}</td>
                    <td>{{ formatDate(item.due_date) }}</td>
                    <td>¥{{ formatAmount(item.principal) }}</td>
                    <td>¥{{ formatAmount(item.interest) }}</td>
                    <td class="total-amount">¥{{ formatAmount(item.total) }}</td>
                    <td>
                      <span :class="['status-badge', `status-${item.status || 'pending'}`]">
                        {{ getStatusText(item.status) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
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
        
        // TODO: 这里需要后端提供获取用户贷款列表的API
        // 目前先使用空数组，等待后端实现
        // const data = await financingService.getLoanList(userInfo.value.phone);
        // loans.value = data.loans || [];
        
        logger.warn('FINANCING', '获取贷款列表API未实现，显示空列表');
        loans.value = [];
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
      formatAmount,
      formatDate,
      getRepaymentMethodText,
      getStatusText,
      getLoanStatusText,
      loadLoans,
      viewSchedule,
      backToList,
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
</style>