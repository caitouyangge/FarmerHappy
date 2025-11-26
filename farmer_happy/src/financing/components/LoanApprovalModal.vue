<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">审批贷款申请</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="form">
          <div class="form-group">
            <label class="form-label">贷款申请ID <span class="required">*</span></label>
            <input
              v-model="formData.application_id"
              type="text"
              class="form-input"
              placeholder="请输入贷款申请ID"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">审批动作 <span class="required">*</span></label>
            <select v-model="formData.action" class="form-input" required @change="handleActionChange">
              <option value="">请选择审批动作</option>
              <option value="approve">批准</option>
              <option value="reject">拒绝</option>
            </select>
          </div>

          <div v-if="formData.action === 'approve'" class="form-group">
            <label class="form-label">批准金额 <span class="required">*</span></label>
            <input
              v-model.number="formData.approved_amount"
              type="number"
              class="form-input"
              placeholder="请输入批准金额（元）"
              min="0"
              step="0.01"
              :required="formData.action === 'approve'"
            />
          </div>

          <div v-if="formData.action === 'reject'" class="form-group">
            <label class="form-label">拒绝原因 <span class="required">*</span></label>
            <textarea
              v-model="formData.reject_reason"
              class="form-input textarea"
              rows="4"
              placeholder="请输入拒绝原因"
              :required="formData.action === 'reject'"
            ></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleClose">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交审批' }}
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
  name: 'LoanApprovalModal',
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const submitting = ref(false);
    const formData = reactive({
      application_id: '',
      action: '',
      approved_amount: null,
      reject_reason: ''
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
      }
    });

    const handleActionChange = () => {
      // 切换审批动作时清空相关字段
      if (formData.action === 'approve') {
        formData.reject_reason = '';
      } else if (formData.action === 'reject') {
        formData.approved_amount = null;
      }
    };

    const handleClose = () => {
      emit('close');
    };

    const handleSubmit = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      if (formData.action === 'approve' && !formData.approved_amount) {
        alert('批准时请输入批准金额');
        return;
      }

      if (formData.action === 'reject' && !formData.reject_reason.trim()) {
        alert('拒绝时请输入拒绝原因');
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '提交贷款审批', { formData });

        const approvalData = {
          phone: userInfo.value.phone,
          application_id: formData.application_id,
          action: formData.action,
          ...(formData.action === 'approve' && { approved_amount: parseFloat(formData.approved_amount) }),
          ...(formData.action === 'reject' && { reject_reason: formData.reject_reason })
        };

        const response = await financingService.approveLoan(approvalData);
        
        logger.info('FINANCING', '贷款审批提交成功', { 
          application_id: formData.application_id,
          action: formData.action
        });
        
        alert('审批提交成功！');
        emit('success');
        handleClose();
      } catch (error) {
        logger.error('FINANCING', '提交贷款审批失败', {
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
      handleActionChange,
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

