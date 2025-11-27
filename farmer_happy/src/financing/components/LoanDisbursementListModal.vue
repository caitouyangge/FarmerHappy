<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">放款操作</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <!-- 已审批申请列表 -->
        <div v-if="!showDisbursementForm">
          <div v-if="loadingApplications" class="loading-container">
            <div class="loading-spinner"></div>
            <p>加载已审批申请...</p>
          </div>

          <div v-else-if="errorMessage" class="error-container">
            <span class="error-icon">⚠️</span>
            <span>{{ errorMessage }}</span>
          </div>

          <div v-else-if="applications.length === 0" class="empty-container">
            <div class="empty-icon">📋</div>
            <p>暂无已审批待放款的贷款申请</p>
          </div>

          <div v-else>
            <div class="applications-header">
              <h3>已审批待放款申请列表</h3>
              <span class="applications-count">共 {{ applications.length }} 条申请</span>
            </div>
            
            <div class="applications-list">
              <div
                v-for="application in applications"
                :key="application.loan_application_id"
                class="application-card"
              >
                <div class="application-header">
                  <div class="application-id">
                    申请编号: {{ application.loan_application_id }}
                  </div>
                  <div class="application-type">
                    {{ getApplicationTypeText(application.application_type) }}
                  </div>
                </div>

                <div class="application-details">
                  <div class="detail-row">
                    <div class="detail-item">
                      <span class="detail-label">申请人：</span>
                      <span class="detail-value">{{ application.farmer_name }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">手机号：</span>
                      <span class="detail-value">{{ application.farmer_phone }}</span>
                    </div>
                  </div>
                  
                  <div class="detail-row">
                    <div class="detail-item">
                      <span class="detail-label">贷款产品：</span>
                      <span class="detail-value">{{ application.product_name }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">申请金额：</span>
                      <span class="detail-value amount">¥{{ formatAmount(application.apply_amount) }}</span>
                    </div>
                  </div>

                  <div class="detail-row">
                    <div class="detail-item">
                      <span class="detail-label">批准金额：</span>
                      <span class="detail-value amount highlight">¥{{ formatAmount(application.approved_amount) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">年利率：</span>
                      <span class="detail-value">{{ (application.interest_rate || 0).toFixed(2) }}%</span>
                    </div>
                  </div>

                  <div class="detail-row">
                    <div class="detail-item">
                      <span class="detail-label">贷款期限：</span>
                      <span class="detail-value">{{ application.term_months }} 个月</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">审批日期：</span>
                      <span class="detail-value">{{ formatDate(application.approved_at) }}</span>
                    </div>
                  </div>

                  <div class="detail-row">
                    <div class="detail-item full-width">
                      <span class="detail-label">贷款用途：</span>
                      <span class="detail-value">{{ application.purpose }}</span>
                    </div>
                  </div>
                </div>

                <div class="application-actions">
                  <button
                    class="btn btn-primary"
                    @click="handleDisburse(application)"
                  >
                    执行放款
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 放款操作表单 -->
        <div v-else class="disbursement-form">
          <div class="form-header">
            <button class="btn-back" @click="backToList">
              ← 返回申请列表
            </button>
            <h3>放款操作</h3>
          </div>

          <div class="selected-application">
            <h4>选中的申请</h4>
            <div class="application-summary">
              <p><strong>申请编号：</strong>{{ selectedApplication.loan_application_id }}</p>
              <p><strong>申请人：</strong>{{ selectedApplication.farmer_name }} ({{ selectedApplication.farmer_phone }})</p>
              <p><strong>批准金额：</strong>¥{{ formatAmount(selectedApplication.approved_amount) }}</p>
              <p><strong>贷款产品：</strong>{{ selectedApplication.product_name }}</p>
            </div>
          </div>

          <form @submit.prevent="handleSubmit" class="form">
            <div class="form-group">
              <label class="form-label">放款金额 <span class="required">*</span></label>
              <input
                v-model.number="formData.disburse_amount"
                type="number"
                class="form-input"
                placeholder="请输入放款金额（元）"
                :max="selectedApplication.approved_amount"
                min="0"
                step="0.01"
                required
              />
              <div class="form-hint">
                最大可放款金额：¥{{ formatAmount(selectedApplication.approved_amount) }}
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">放款方式 <span class="required">*</span></label>
              <select v-model="formData.disburse_method" class="form-input" required>
                <option value="">请选择放款方式</option>
                <option value="bank_transfer">银行转账</option>
                <option value="cash">现金</option>
                <option value="check">支票</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">首次还款日期 <span class="required">*</span></label>
              <input
                v-model="formData.first_repayment_date"
                type="date"
                class="form-input"
                :min="minRepaymentDate"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">贷款账户 <span class="required">*</span></label>
              <input
                v-model="formData.loan_account"
                type="text"
                class="form-input"
                placeholder="请输入贷款账户"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">放款备注</label>
              <textarea
                v-model="formData.remarks"
                class="form-input textarea"
                rows="3"
                placeholder="请输入放款备注（可选）"
              ></textarea>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="backToList">
                取消
              </button>
              <button type="submit" class="btn btn-primary" :disabled="submitting">
                {{ submitting ? '放款中...' : '确认放款' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'LoanDisbursementListModal',
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const applications = ref([]);
    const loadingApplications = ref(false);
    const errorMessage = ref('');
    const showDisbursementForm = ref(false);
    const selectedApplication = ref(null);
    const submitting = ref(false);
    
    const formData = reactive({
      disburse_amount: null,
      disburse_method: '',
      first_repayment_date: '',
      loan_account: '',
      remarks: ''
    });

    const minRepaymentDate = computed(() => {
      const nextMonth = new Date();
      nextMonth.setMonth(nextMonth.getMonth() + 1);
      return nextMonth.toISOString().split('T')[0];
    });

    onMounted(async () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        await loadApplications();
      }
      
      // 设置默认首次还款日期
      const nextMonth = new Date();
      nextMonth.setMonth(nextMonth.getMonth() + 1);
      formData.first_repayment_date = nextMonth.toISOString().split('T')[0];
    });

    const loadApplications = async () => {
      if (!userInfo.value.phone) {
        errorMessage.value = '请先登录';
        return;
      }

      loadingApplications.value = true;
      errorMessage.value = '';
      try {
        logger.info('FINANCING', '获取已审批贷款申请列表', { phone: userInfo.value.phone });
        
        const response = await financingService.getApprovedLoanApplications(userInfo.value.phone);
        applications.value = response.data.applications || [];
        
        logger.info('FINANCING', '获取已审批贷款申请列表成功', { 
          count: applications.value.length 
        });
      } catch (error) {
        logger.error('FINANCING', '获取已审批贷款申请列表失败', {
          errorMessage: error.message || error
        }, error);
        errorMessage.value = error.message || '获取申请列表失败，请稍后重试';
      } finally {
        loadingApplications.value = false;
      }
    };

    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    const formatDate = (dateStr) => {
      if (!dateStr) return '-';
      return new Date(dateStr).toLocaleDateString('zh-CN');
    };

    const getApplicationTypeText = (type) => {
      const typeMap = {
        'single': '单人贷款',
        'joint': '联合贷款'
      };
      return typeMap[type] || type;
    };

    const handleDisburse = (application) => {
      selectedApplication.value = application;
      formData.disburse_amount = parseFloat(application.approved_amount);
      formData.disburse_method = '';
      formData.loan_account = '';
      formData.remarks = '';
      showDisbursementForm.value = true;
    };

    const backToList = () => {
      showDisbursementForm.value = false;
      selectedApplication.value = null;
    };

    const handleClose = () => {
      emit('close');
    };

    const handleSubmit = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      if (!selectedApplication.value) {
        alert('未选择申请');
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '提交放款操作', { 
          applicationId: selectedApplication.value.loan_application_id,
          amount: formData.disburse_amount
        });

        const disbursementData = {
          phone: userInfo.value.phone,
          application_id: selectedApplication.value.loan_application_id,
          disburse_amount: parseFloat(formData.disburse_amount),
          disburse_method: formData.disburse_method,
          first_repayment_date: formData.first_repayment_date,
          loan_account: formData.loan_account,
          ...(formData.remarks && { remarks: formData.remarks })
        };

        const response = await financingService.disburseLoan(disbursementData);
        
        logger.info('FINANCING', '放款操作提交成功', { 
          loan_id: response.data?.loan_id 
        });
        
        alert('放款操作成功！');
        emit('success');
        handleClose();
      } catch (error) {
        logger.error('FINANCING', '提交放款操作失败', {
          errorMessage: error.message || error
        }, error);
        alert('提交失败：' + (error.message || '请稍后重试'));
      } finally {
        submitting.value = false;
      }
    };

    return {
      userInfo,
      applications,
      loadingApplications,
      errorMessage,
      showDisbursementForm,
      selectedApplication,
      submitting,
      formData,
      minRepaymentDate,
      loadApplications,
      formatAmount,
      formatDate,
      getApplicationTypeText,
      handleDisburse,
      backToList,
      handleClose,
      handleSubmit
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
}

.modal-container.large {
  max-width: 1200px;
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
  gap: 1.5rem;
}

.application-card {
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s;
  background: var(--white);
}

.application-card:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.1);
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.application-id {
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary);
}

.application-type {
  background: var(--success-light);
  color: var(--success);
  padding: 0.25rem 0.75rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
}

.application-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.detail-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-label {
  color: var(--gray-500);
  min-width: fit-content;
}

.detail-value {
  color: #1a202c;
  font-weight: 500;
}

.detail-value.amount {
  font-size: 1rem;
  font-weight: 600;
}

.detail-value.highlight {
  color: var(--primary);
  font-size: 1.1rem;
}

.application-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.disbursement-form {
  max-width: 600px;
  margin: 0 auto;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--primary-light);
}

.btn-back {
  background: none;
  border: 1px solid var(--gray-300);
  color: var(--gray-600);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--gray-100);
  border-color: var(--gray-400);
}

.form-header h3 {
  margin: 0;
  color: var(--primary);
  font-size: 1.25rem;
  font-weight: 600;
}

.selected-application {
  background: var(--gray-50);
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.selected-application h4 {
  margin: 0 0 1rem 0;
  color: var(--primary);
  font-size: 1rem;
  font-weight: 600;
}

.application-summary p {
  margin: 0.5rem 0;
  font-size: 0.875rem;
  color: var(--gray-700);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--gray-700);
}

.required {
  color: var(--error);
}

.form-input {
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.textarea {
  resize: vertical;
  min-height: 80px;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--gray-500);
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
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

.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  .detail-row {
    grid-template-columns: 1fr;
  }
  
  .applications-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
  
  .application-header {
    flex-direction: column;
    gap: 0.5rem;
    align-items: flex-start;
  }
}
</style>
