<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">发布贷款产品</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="form">
          <div class="form-group">
            <label class="form-label">产品名称 <span class="required">*</span></label>
            <input
              v-model="formData.product_name"
              type="text"
              class="form-input"
              placeholder="请输入产品名称"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">产品编号</label>
            <input
              v-model="formData.product_code"
              type="text"
              class="form-input"
              placeholder="可选，系统将自动生成"
            />
          </div>

          <div class="form-group">
            <label class="form-label">最低贷款额度要求 <span class="required">*</span></label>
            <input
              v-model.number="formData.min_credit_limit"
              type="number"
              class="form-input"
              placeholder="请输入最低信用额度要求（元）"
              min="0"
              step="0.01"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">最高贷款额度 <span class="required">*</span></label>
            <input
              v-model.number="formData.max_amount"
              type="number"
              class="form-input"
              placeholder="请输入最高贷款额度（元）"
              min="1"
              step="0.01"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">年利率 <span class="required">*</span></label>
            <input
              v-model.number="formData.interest_rate"
              type="number"
              class="form-input"
              placeholder="请输入年利率（小数，如0.06表示6%）"
              min="0"
              max="1"
              step="0.01"
              required
            />
            <div class="form-hint">
              例如：0.06 表示 6%，0.05 表示 5%
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">贷款期限（月） <span class="required">*</span></label>
            <input
              v-model.number="formData.term_months"
              type="number"
              class="form-input"
              placeholder="请输入贷款期限（月）"
              min="1"
              step="1"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">还款方式 <span class="required">*</span></label>
            <select v-model="formData.repayment_method" class="form-input" required>
              <option value="">请选择还款方式</option>
              <option value="equal_installment">等额本息</option>
              <option value="interest_first">先息后本</option>
              <option value="bullet_repayment">一次性还本付息</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">产品描述</label>
            <textarea
              v-model="formData.description"
              class="form-input textarea"
              rows="4"
              placeholder="请输入产品描述（必填）"
            ></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleClose">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? '发布中...' : '发布产品' }}
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
  name: 'LoanProductPublishModal',
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const submitting = ref(false);
    const formData = reactive({
      product_name: '',
      product_code: '',
      min_credit_limit: null,
      max_amount: null,
      interest_rate: null,
      term_months: null,
      repayment_method: '',
      description: ''
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
      }
    });

    const handleClose = () => {
      emit('close');
    };

    const handleSubmit = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      if (formData.max_amount < formData.min_credit_limit) {
        alert('最高贷款额度不能小于最低信用额度要求');
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '发布贷款产品', { formData });

        const productData = {
          phone: userInfo.value.phone,
          product_name: formData.product_name,
          ...(formData.product_code && { product_code: formData.product_code }),
          min_credit_limit: parseFloat(formData.min_credit_limit),
          max_amount: parseFloat(formData.max_amount),
          interest_rate: parseFloat(formData.interest_rate*100),
          term_months: parseInt(formData.term_months),
          repayment_method: formData.repayment_method,
          ...(formData.description && { description: formData.description })
        };

        const response = await financingService.publishLoanProduct(productData);
        
        logger.info('FINANCING', '贷款产品发布成功', { 
          product_id: response.data?.product_id 
        });
        
        alert('产品发布成功！');
        emit('success');
        handleClose();
      } catch (error) {
        logger.error('FINANCING', '发布贷款产品失败', {
          errorMessage: error.message || error
        }, error);
        alert('发布失败：' + (error.message || '请稍后重试'));
      } finally {
        submitting.value = false;
      }
    };

    return {
      userInfo,
      submitting,
      formData,
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

