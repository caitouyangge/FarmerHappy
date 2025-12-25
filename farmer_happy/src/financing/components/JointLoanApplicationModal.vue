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
              <span>¥{{ formatAmount(product.max_amount) }}</span>
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
            <label class="form-label">申请金额</label>
            <div class="fixed-amount-display">
              <span class="amount-value">¥{{ formatAmount(product.max_amount) }}</span>
              <span class="amount-hint">（固定金额）</span>
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
                请点击"浏览并选择伙伴"来选择联合贷款伙伴（1个）
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
              <span>我已阅读并同意<a href="javascript:void(0)" @click.prevent="showAgreementModal = true" class="agreement-link">《联合贷款协议》</a></span>
            </label>
          </div>

          <!-- 联合贷款协议弹窗 -->
          <div v-if="showAgreementModal" class="agreement-modal-overlay" @click="showAgreementModal = false">
            <div class="agreement-modal" @click.stop>
              <div class="agreement-header">
                <h3>联合贷款协议</h3>
                <button class="btn-close" @click="showAgreementModal = false">×</button>
              </div>
              <div class="agreement-content">
                <h4>一、联合贷款说明</h4>
                <p>联合贷款是指由两位农户（发起人和1位联合伙伴）共同申请的贷款方式。通过联合贷款，两位农户可以合并各自的信用额度，共同承担贷款责任。</p>
                
                <h4>二、申请条件</h4>
                <ul>
                  <li>发起人和联合伙伴都必须是已注册的农户</li>
                  <li>发起人和联合伙伴都需具备有效的信用额度</li>
                  <li>联合贷款的总额度 = 发起人可用额度 + 联合伙伴可用额度</li>
                  <li>联合伙伴的可用额度需满足贷款金额要求</li>
                </ul>
                
                <h4>三、贷款流程</h4>
                <ol>
                  <li>发起人选择贷款产品和联合伙伴</li>
                  <li>发起人填写贷款用途和还款计划说明</li>
                  <li>发起人提交联合贷款申请</li>
                  <li>系统通知联合伙伴确认申请</li>
                  <li>联合伙伴确认后，申请提交至银行审批</li>
                  <li>银行审批通过后放款</li>
                </ol>
                
                <h4>四、还款责任</h4>
                <ul>
                  <li>发起人和联合伙伴对贷款承担<strong>连带责任</strong></li>
                  <li>贷款金额由双方按各自的可用额度比例分担，但任何一方都有义务承担全部还款责任</li>
                  <li>如一方无法按时还款，另一方需承担全部还款义务</li>
                  <li>建议在申请前明确双方的具体还款安排和比例</li>
                </ul>
                
                <h4>五、重要提示</h4>
                <ul>
                  <li>联合贷款需要双方都同意才能生效</li>
                  <li>双方需保持良好的信用记录，任何一方的信用问题都可能影响贷款审批</li>
                  <li>请确保与联合伙伴有良好的信任关系和还款能力评估</li>
                  <li>建议在申请前充分沟通，明确贷款用途和还款计划</li>
                </ul>
                
                <div class="agreement-footer">
                  <button class="btn btn-primary" @click="showAgreementModal = false">我已了解</button>
                </div>
              </div>
            </div>
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
import { ref, reactive, onMounted, watch } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'JointLoanApplicationModal',
  props: {
    product: {
      type: Object,
      required: true
    },
    selectedPartner: {
      type: Object,
      default: null
    }
  },
  emits: ['close', 'success', 'open-partners'],
  expose: ['handlePartnerSelect'],
  setup(props, { emit, expose }) {
    const userInfo = ref({});
    const submitting = ref(false);
    const selectedPartners = ref([]);
    const showAgreementModal = ref(false);
    const formData = reactive({
      purpose: '',
      repayment_plan: '',
      joint_agreement: false
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
      }
      
      // 如果prop中有选择的伙伴，初始化selectedPartners
      if (props.selectedPartner) {
        selectedPartners.value = [props.selectedPartner];
      }
    });

    // 监听selectedPartner prop的变化
    watch(() => props.selectedPartner, (newPartner) => {
      if (newPartner) {
        selectedPartners.value = [newPartner];
        console.log('JOINT_LOAN selectedPartner prop changed', newPartner, selectedPartners.value);
      } else {
        // 如果prop变为null，清空选择（但保留用户手动选择的）
        // selectedPartners.value = [];
      }
    }, { immediate: true });

    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    const handleOpenPartners = () => {
      emit('open-partners');
    };

    const handlePartnerSelect = (partners) => {
      console.log('JOINT_LOAN handlePartnerSelect called', { partners, selectedPartners: selectedPartners.value });
      logger.info('JOINT_LOAN', '接收到选择的伙伴', { 
        partnersCount: partners?.length || 0,
        partners: partners
      });
      
      // 只能选择1个伙伴，直接替换
      if (partners && partners.length > 0) {
        const partner = partners[0];
        const userPhone = userInfo.value?.phone;
        
        console.log('JOINT_LOAN partner selected', { 
          partner, 
          userPhone,
          partnerPhone: partner?.phone,
          willSet: partner?.phone !== userPhone
        });
        
        logger.info('JOINT_LOAN', '准备设置选择的伙伴', { 
          partnerPhone: partner?.phone,
          userPhone: userPhone,
          partner: partner
        });
        
        // 检查是否是自己的手机号（如果userInfo已加载）
        if (userPhone && partner?.phone === userPhone) {
          logger.warn('JOINT_LOAN', '不能选择自己作为伙伴');
          alert('不能选择自己作为联合伙伴');
          return;
        }
        
        // 设置选择的伙伴
        if (partner?.phone) {
          selectedPartners.value = [partner];
          console.log('JOINT_LOAN selectedPartners updated', selectedPartners.value);
          logger.info('JOINT_LOAN', '伙伴选择成功', { 
            selectedCount: selectedPartners.value.length,
            selectedPartner: selectedPartners.value[0]
          });
        } else {
          logger.error('JOINT_LOAN', '伙伴数据缺少phone字段', { partner });
          alert('选择的伙伴数据无效，请重试');
        }
      } else {
        logger.warn('JOINT_LOAN', '未接收到有效的伙伴数据', { partners });
        console.warn('JOINT_LOAN no valid partners', partners);
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

      // 验证伙伴数量（只能选择1个）
      if (selectedPartners.value.length !== 1) {
        alert('请选择1个联合伙伴');
        return;
      }

      const fixedAmount = parseFloat(props.product.max_amount);
      submitting.value = true;
      try {
        logger.info('FINANCING', '提交联合贷款申请', { 
          productId: props.product.product_id,
          applyAmount: fixedAmount,
          partnerCount: selectedPartners.value.length
        });
        const loanData = {
          phone: userInfo.value.phone,
          product_id: props.product.product_id,
          apply_amount: fixedAmount,
          partner_phones: selectedPartners.value.map(p => p.phone),
          purpose: formData.purpose,
          repayment_plan: formData.repayment_plan,
          joint_agreement: formData.joint_agreement
        };

        const response = await financingService.applyForJointLoan(loanData);
        
        logger.info('FINANCING', '联合贷款申请提交成功', { 
          loan_application_id: response.data?.loan_application_id 
        });
        
        // 检查响应状态
        const status = response.data?.status || 'pending_partners';
        if (status === 'pending_partners') {
          alert('申请已提交！已发送邀请给联合伙伴，请等待对方确认。确认后申请将进入银行审批流程。');
        } else {
          alert('申请提交成功！请等待审核');
        }
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
      showAgreementModal,
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

.fixed-amount-display {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: linear-gradient(135deg, #f8faff 0%, #f1f5ff 100%);
  border: 2px solid var(--primary-light);
  border-radius: 8px;
}

.amount-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
}

.amount-hint {
  font-size: 0.875rem;
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

.agreement-link {
  color: var(--primary);
  text-decoration: underline;
  cursor: pointer;
  margin: 0 0.25rem;
}

.agreement-link:hover {
  color: var(--primary-dark);
}

/* 联合贷款协议弹窗样式 */
.agreement-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.agreement-modal {
  background: var(--white);
  border-radius: 16px;
  width: 90%;
  max-width: 700px;
  max-height: 85vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.agreement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
}

.agreement-header h3 {
  margin: 0;
  color: var(--white);
  font-size: 1.25rem;
  font-weight: 600;
}

.agreement-header .btn-close {
  background: transparent;
  border: none;
  font-size: 2rem;
  color: var(--white);
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

.agreement-header .btn-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.agreement-content {
  padding: 2rem;
  overflow-y: auto;
  flex: 1;
}

.agreement-content h4 {
  color: var(--primary);
  font-size: 1.125rem;
  font-weight: 600;
  margin: 1.5rem 0 0.75rem 0;
}

.agreement-content h4:first-child {
  margin-top: 0;
}

.agreement-content p {
  color: var(--gray-700);
  line-height: 1.8;
  margin-bottom: 1rem;
}

.agreement-content ul,
.agreement-content ol {
  color: var(--gray-700);
  line-height: 1.8;
  margin-bottom: 1rem;
  padding-left: 1.5rem;
}

.agreement-content li {
  margin-bottom: 0.5rem;
}

.agreement-content strong {
  color: var(--error);
  font-weight: 600;
}

.agreement-footer {
  padding: 1.5rem 2rem;
  border-top: 1px solid var(--gray-200);
  display: flex;
  justify-content: flex-end;
  background: var(--gray-50);
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
