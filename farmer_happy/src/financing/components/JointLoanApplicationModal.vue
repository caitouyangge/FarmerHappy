<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">申请联合贷款</h2>
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
              <span>{{ (product.interest_rate || 0).toFixed(2) }}%</span>
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
            <label class="form-label">联合伙伴 <span class="required">*</span></label>
            <div class="partners-section">
              <button
                type="button"
                class="btn btn-secondary btn-browse-partners"
                @click="handleOpenPartners"
              >
                🔍 浏览并选择伙伴
              </button>
              <div v-if="selectedPartners.length > 0" class="selected-partners">
                <div
                  v-for="(partner, index) in selectedPartners"
                  :key="partner.phone"
                  class="partner-tag"
                >
                  <span class="partner-name">{{ partner.nickname || partner.phone }}</span>
                  <span class="partner-phone">{{ partner.phone }}</span>
                  <button
                    type="button"
                    class="btn-remove-partner"
                    @click="removeSelectedPartner(index)"
                  >
                    ×
                  </button>
                </div>
              </div>
              <div v-else class="form-hint">
                请点击"浏览并选择伙伴"来选择联合贷款伙伴（2-5个）
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">贷款用途 <span class="required">*</span></label>
            <textarea
              v-model="formData.purpose"
              class="form-input textarea"
              rows="3"
              placeholder="请详细说明贷款用途（如：共同购买农机设备、共同扩大生产等）"
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">还款计划说明 <span class="required">*</span></label>
            <textarea
              v-model="formData.repayment_plan"
              class="form-input textarea"
              rows="3"
              placeholder="请说明还款计划和各伙伴的还款责任"
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input
                v-model="formData.joint_agreement"
                type="checkbox"
                required
                class="checkbox"
              />
              <span>我已阅读并同意《联合贷款协议》</span>
            </label>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleClose">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting || !formData.joint_agreement">
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
  name: 'JointLoanApplicationModal',
  props: {
    product: {
      type: Object,
      required: true
    }
  },
  emits: ['close', 'success', 'open-partners'],
  expose: ['handlePartnerSelect'],
  setup(props, { emit, expose }) {
    const userInfo = ref({});
    const submitting = ref(false);
    const selectedPartners = ref([]);
    const formData = reactive({
      apply_amount: null,
      purpose: '',
      repayment_plan: '',
      joint_agreement: false
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

    const handleOpenPartners = () => {
      emit('open-partners');
    };

    const handlePartnerSelect = (partners) => {
      // 合并新选择的伙伴，避免重复
      const existingPhones = selectedPartners.value.map(p => p.phone);
      partners.forEach(partner => {
        if (!existingPhones.includes(partner.phone) && partner.phone !== userInfo.value.phone) {
          selectedPartners.value.push(partner);
        }
      });
      // 限制最多5个伙伴
      if (selectedPartners.value.length > 5) {
        selectedPartners.value = selectedPartners.value.slice(0, 5);
        alert('最多只能选择5个联合伙伴');
      }
    };

    // 暴露方法供父组件调用
    expose({
      handlePartnerSelect
    });

    const removeSelectedPartner = (index) => {
      selectedPartners.value.splice(index, 1);
    };

    const handleClose = () => {
      emit('close');
    };

    const handleSubmit = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      // 验证伙伴数量
      if (selectedPartners.value.length < 1 || selectedPartners.value.length > 5) {
        alert('联合伙伴数量必须在 1-5 个之间');
        return;
      }

      if (formData.apply_amount < props.product.min_amount || 
          formData.apply_amount > props.product.max_amount) {
        alert(`申请金额必须在 ¥${formatAmount(props.product.min_amount)} - ¥${formatAmount(props.product.max_amount)} 之间`);
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '提交联合贷款申请', { 
          productId: props.product.product_id,
          applyAmount: formData.apply_amount,
          partnerCount: selectedPartners.value.length
        });

        const loanData = {
          phone: userInfo.value.phone,
          product_id: props.product.product_id,
          apply_amount: parseFloat(formData.apply_amount),
          partner_phones: selectedPartners.value.map(p => p.phone),
          purpose: formData.purpose,
          repayment_plan: formData.repayment_plan,
          joint_agreement: formData.joint_agreement
        };

        const response = await financingService.applyForJointLoan(loanData);
        
        logger.info('FINANCING', '联合贷款申请提交成功', { 
          loan_application_id: response.data?.loan_application_id 
        });
        
        alert('申请提交成功！请等待审核');
        emit('success');
        handleClose();
      } catch (error) {
        logger.error('FINANCING', '提交联合贷款申请失败', {
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
      selectedPartners,
      formData,
      formatAmount,
      handleOpenPartners,
      handlePartnerSelect,
      removeSelectedPartner,
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

.partners-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.btn-browse-partners {
  width: 100%;
  margin-bottom: 0.5rem;
}

.selected-partners {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.partner-tag {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--gray-50);
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
}

.partner-name {
  font-weight: 500;
  color: #1a202c;
}

.partner-phone {
  color: var(--gray-500);
  font-size: 0.75rem;
}

.btn-remove-partner {
  background: var(--error);
  color: var(--white);
  border: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  padding: 0;
  line-height: 1;
}

.btn-remove-partner:hover {
  background: #c53030;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--gray-600);
}

.checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
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
