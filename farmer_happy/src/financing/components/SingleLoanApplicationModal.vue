<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">申请单人贷款</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <!-- 产品信息展示 -->
        <div class="product-info">
          <h3 class="product-title">{{ product.product_name }}</h3>
          <div class="product-details">
            <div class="detail-row">
              <span>贷款额度：</span>
              <span>¥{{ formatAmount(product.min_amount) }} - ¥{{ formatAmount(product.max_amount) }}</span>
            </div>
            <div class="detail-row">
              <span>年利率：</span>
              <span>{{ (product.interest_rate * 100).toFixed(2) }}%</span>
            </div>
            <div class="detail-row">
              <span>贷款期限：</span>
              <span>{{ product.term_months }} 个月</span>
            </div>
          </div>
        </div>

        <form @submit.prevent="handleSubmit" class="form">
          <div class="form-group">
            <label class="form-label">申请金额 <span class="required">*</span></label>
            <input
              v-model.number="formData.apply_amount"
              type="number"
              class="form-input"
              :placeholder="`请输入申请金额（¥${formatAmount(product.min_amount)} - ¥${formatAmount(product.max_amount)}）`"
              :min="product.min_amount"
              :max="product.max_amount"
              step="0.01"
              required
            />
            <div class="form-hint">
              最低：¥{{ formatAmount(product.min_amount) }}，最高：¥{{ formatAmount(product.max_amount) }}
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">贷款用途 <span class="required">*</span></label>
            <textarea
              v-model="formData.purpose"
              class="form-input textarea"
              rows="3"
              placeholder="请详细说明贷款用途（如：购买种子、购买农具、扩大生产等）"
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">还款来源说明 <span class="required">*</span></label>
            <textarea
              v-model="formData.repayment_source"
              class="form-input textarea"
              rows="3"
              placeholder="请说明您的还款来源（如：农产品销售收入、其他收入等）"
              required
            ></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleClose">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交申请' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'SingleLoanApplicationModal',
  props: {
    product: {
      type: Object,
      required: true
    }
  },
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const submitting = ref(false);
    const formData = reactive({
      apply_amount: null,
      purpose: '',
      repayment_source: ''
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
      }
    });

    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    const handleClose = () => {
      emit('close');
    };

    const handleSubmit = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      if (formData.apply_amount < props.product.min_amount || 
          formData.apply_amount > props.product.max_amount) {
        alert(`申请金额必须在 ¥${formatAmount(props.product.min_amount)} - ¥${formatAmount(props.product.max_amount)} 之间`);
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '提交单人贷款申请', { 
          productId: props.product.product_id,
          applyAmount: formData.apply_amount
        });

        const loanData = {
          phone: userInfo.value.phone,
          product_id: props.product.product_id,
          apply_amount: parseFloat(formData.apply_amount),
          purpose: formData.purpose,
          repayment_source: formData.repayment_source
        };

        const response = await financingService.applyForSingleLoan(loanData);
        
        logger.info('FINANCING', '单人贷款申请提交成功', { 
          loan_application_id: response.data?.loan_application_id 
        });
        
        alert('申请提交成功！请等待审核');
        emit('success');
        handleClose();
      } catch (error) {
        logger.error('FINANCING', '提交单人贷款申请失败', {
          errorMessage: error.message || error
        }, error);
        alert('提交失败：' + (error.message || '请稍后重试'));
      } finally {
        submitting.value = false;
      }
    };

    return {
      userInfo,
      submitting,
      formData,
      formatAmount,
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
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
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

.product-info {
  background: var(--gray-50);
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

.product-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0 0 0.75rem 0;
}

.product-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
}

.detail-row span:first-child {
  color: var(--gray-500);
}

.detail-row span:last-child {
  color: #1a202c;
  font-weight: 500;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--gray-600);
  font-size: 0.875rem;
  font-weight: 500;
}

.required {
  color: var(--error);
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.form-input.textarea {
  resize: vertical;
  font-family: inherit;
}

.form-hint {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: var(--gray-500);
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--gray-200);
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
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
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-secondary:hover {
  background: var(--gray-300);
}
</style>

