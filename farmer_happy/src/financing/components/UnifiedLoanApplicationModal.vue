<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">智能贷款申请</h2>
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

        <!-- 第一步：输入申请金额并获取推荐 -->
        <div v-if="currentStep === 'input'" class="step-input">
          <div class="form-group">
            <label class="form-label">申请金额 <span class="required">*</span></label>
            <input
              v-model.number="applyAmount"
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

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleClose">
              取消
            </button>
            <button 
              type="button" 
              class="btn btn-primary" 
              :disabled="!applyAmount || checkingRecommendation"
              @click="getRecommendation"
            >
              {{ checkingRecommendation ? '分析中...' : '智能分析' }}
            </button>
          </div>
        </div>

        <!-- 第二步：显示推荐结果 -->
        <div v-if="currentStep === 'recommendation'" class="step-recommendation">
          <!-- 用户额度信息 -->
          <div class="credit-info-card">
            <div class="credit-info-header">
              <h4>您的信用情况</h4>
            </div>
            <div class="credit-info-content">
              <div class="credit-item">
                <span class="label">可用额度：</span>
                <span class="value available">¥{{ formatAmount(recommendation.user_available_limit) }}</span>
              </div>
              <div class="credit-item">
                <span class="label">申请金额：</span>
                <span class="value apply">¥{{ formatAmount(recommendation.apply_amount) }}</span>
              </div>
            </div>
          </div>

          <!-- 推荐信息 -->
          <div class="recommendation-card">
            <div class="recommendation-header">
              <span class="recommendation-icon">
                {{ recommendation.recommendation_type === 'single' ? '👤' : '👥' }}
              </span>
              <h4>{{ recommendation.recommendation_type === 'single' ? '推荐单人贷款' : '推荐联合贷款' }}</h4>
            </div>
            <div class="recommendation-reason">
              {{ recommendation.recommendation_reason }}
            </div>
          </div>

          <!-- 联合贷款伙伴推荐 -->
          <div v-if="recommendation.recommended_partners && recommendation.recommended_partners.length > 0" 
               class="partners-section">
            <h4 class="section-title">推荐合作伙伴（最多选择1人）</h4>
            <div class="partners-grid">
              <div 
                v-for="partner in recommendation.recommended_partners"
                :key="partner.phone"
                class="partner-card"
                :class="{ selected: selectedPartner && selectedPartner.phone === partner.phone }"
                @click="selectPartner(partner)"
              >
                <div class="partner-info">
                  <div class="partner-name">{{ partner.nickname || partner.phone }}</div>
                  <div class="partner-phone">{{ partner.phone }}</div>
                  <div class="partner-credit">
                    可用额度：¥{{ formatAmount(partner.available_credit_limit) }}
                  </div>
                  <div class="partner-combined">
                    联合后总额度：¥{{ formatAmount(recommendation.user_available_limit + partner.available_credit_limit) }}
                  </div>
                </div>
                <div class="selection-indicator">
                  {{ selectedPartner && selectedPartner.phone === partner.phone ? '✓' : '' }}
                </div>
              </div>
            </div>
          </div>

          <!-- 选择申请方式 -->
          <div class="application-options">
            <button 
              v-if="recommendation.can_apply_single"
              type="button" 
              class="btn btn-outline"
              @click="proceedWithSingle"
            >
              申请单人贷款
            </button>
            <button 
              v-if="recommendation.can_apply_joint"
              type="button" 
              class="btn btn-primary"
              :disabled="!canProceedWithJoint"
              @click="proceedWithJoint"
            >
              {{ selectedPartner ? '申请联合贷款' : '请先选择合作伙伴' }}
            </button>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="goBack">
              返回修改
            </button>
          </div>
        </div>

        <!-- 第三步：填写贷款申请详情 -->
        <div v-if="currentStep === 'details'" class="step-details">
          <div class="application-summary">
            <h4>申请概要</h4>
            <div class="summary-item">
              <span>申请类型：</span>
              <span>{{ selectedApplicationType === 'single' ? '单人贷款' : '联合贷款' }}</span>
            </div>
            <div class="summary-item">
              <span>申请金额：</span>
              <span>¥{{ formatAmount(applyAmount) }}</span>
            </div>
            <div v-if="selectedApplicationType === 'joint' && selectedPartner" class="summary-item">
              <span>合作伙伴：</span>
              <span>{{ selectedPartner.nickname || selectedPartner.phone }}</span>
            </div>
          </div>

          <form @submit.prevent="handleSubmit" class="form">
            <div class="form-group">
              <label class="form-label">贷款用途 <span class="required">*</span></label>
              <textarea
                v-model="formData.purpose"
                class="form-input textarea"
                rows="3"
                :placeholder="selectedApplicationType === 'single' ? 
                  '请详细说明贷款用途（如：购买种子、购买农具、扩大生产等）' : 
                  '请详细说明贷款用途（如：共同购买农机设备、共同扩大生产等）'"
                required
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">
                {{ selectedApplicationType === 'single' ? '还款来源说明' : '还款计划说明' }}
                <span class="required">*</span>
              </label>
              <textarea
                v-model="repaymentInfo"
                class="form-input textarea"
                rows="3"
                :placeholder="selectedApplicationType === 'single' ? 
                  '请说明您的还款来源（如：农产品销售收入、其他收入等）' : 
                  '请说明还款计划和各伙伴的还款责任'"
                required
              ></textarea>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="goBackToRecommendation">
                返回
              </button>
              <button type="submit" class="btn btn-primary" :disabled="submitting">
                {{ submitting ? '提交中...' : '提交申请' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'UnifiedLoanApplicationModal',
  props: {
    product: {
      type: Object,
      required: true
    }
  },
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const currentStep = ref('input'); // 'input', 'recommendation', 'details'
    const applyAmount = ref(null);
    const checkingRecommendation = ref(false);
    const recommendation = ref(null);
    const selectedPartner = ref(null);
    const selectedApplicationType = ref('');
    const submitting = ref(false);

    const formData = reactive({
      purpose: '',
      repayment_source: '',
      repayment_plan: ''
    });

    // 计算属性
    const canProceedWithJoint = computed(() => {
      return recommendation.value?.can_apply_joint && 
             recommendation.value?.recommended_partners?.length > 0 && 
             selectedPartner.value;
    });

    // 处理还款信息的双向绑定
    const repaymentInfo = computed({
      get() {
        return selectedApplicationType.value === 'single' 
          ? formData.repayment_source 
          : formData.repayment_plan;
      },
      set(value) {
        if (selectedApplicationType.value === 'single') {
          formData.repayment_source = value;
        } else {
          formData.repayment_plan = value;
        }
      }
    });

     // 初始化
     onMounted(() => {
       const storedUser = localStorage.getItem('user');
       if (storedUser) {
         try {
           userInfo.value = JSON.parse(storedUser);
         } catch (error) {
           logger.error('UNIFIED_LOAN', '解析用户信息失败', {}, error);
         }
       }
       
       // 调试：输出接收到的产品对象
       console.log('DEBUG: 统一贷款申请组件接收到的产品对象:', props.product);
       console.log('DEBUG: 产品对象的所有属性:', Object.keys(props.product));
     });

    // 格式化金额
    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

     // 获取智能推荐
     const getRecommendation = async () => {
       if (!applyAmount.value || !userInfo.value.phone) return;

       // 检查产品ID是否存在
       if (!props.product || !props.product.product_id) {
         alert('产品信息缺失，请重新选择产品');
         return;
       }

       checkingRecommendation.value = true;
       try {
         // 调试：输出产品对象结构
         console.log('DEBUG: props.product =', props.product);
         console.log('DEBUG: product_id =', props.product.product_id);
         console.log('DEBUG: product_id type =', typeof props.product.product_id);
         
         const requestData = {
           phone: userInfo.value.phone,
           product_id: props.product.product_id,
           apply_amount: applyAmount.value
         };

         // 如果product_id仍然为空，尝试使用product.id作为备用
         if (!requestData.product_id && props.product.id) {
           console.log('WARN: product_id为空，尝试使用product.id作为备用');
           requestData.product_id = props.product.id;
         }
         
         console.log('DEBUG: 发送给后端的请求数据 =', requestData);
         logger.info('UNIFIED_LOAN', '获取智能推荐', requestData);
        const data = await financingService.getSmartLoanRecommendation(requestData);
        
        recommendation.value = data;
        currentStep.value = 'recommendation';
        
        logger.info('UNIFIED_LOAN', '智能推荐获取成功', { 
          recommendation_type: data.recommendation_type,
          partners_count: data.recommended_partners?.length || 0
        });
      } catch (error) {
        logger.error('UNIFIED_LOAN', '获取智能推荐失败', {
          errorMessage: error.message
        }, error);
        
        // 显示错误信息给用户
        alert(error.message || '获取推荐失败，请稍后重试');
      } finally {
        checkingRecommendation.value = false;
      }
    };

    // 选择合作伙伴
    const selectPartner = (partner) => {
      if (selectedPartner.value && selectedPartner.value.phone === partner.phone) {
        selectedPartner.value = null;
      } else {
        selectedPartner.value = partner;
      }
    };

    // 选择单人贷款
    const proceedWithSingle = () => {
      selectedApplicationType.value = 'single';
      currentStep.value = 'details';
    };

    // 选择联合贷款
    const proceedWithJoint = () => {
      if (!selectedPartner.value) {
        alert('请先选择一个合作伙伴');
        return;
      }
      selectedApplicationType.value = 'joint';
      currentStep.value = 'details';
    };

    // 返回上一步
    const goBack = () => {
      currentStep.value = 'input';
      recommendation.value = null;
      selectedPartner.value = null;
    };

    // 从详情页返回推荐页
    const goBackToRecommendation = () => {
      currentStep.value = 'recommendation';
      selectedApplicationType.value = '';
      // 清空表单数据
      formData.purpose = '';
      formData.repayment_source = '';
      formData.repayment_plan = '';
    };

     // 提交申请
     const handleSubmit = async () => {
       if (!formData.purpose || 
           (selectedApplicationType.value === 'single' && !formData.repayment_source) ||
           (selectedApplicationType.value === 'joint' && !formData.repayment_plan)) {
         alert('请填写完整的申请信息');
         return;
       }

       // 检查产品ID是否存在
       if (!props.product || !props.product.product_id) {
         alert('产品信息缺失，请重新选择产品');
         return;
       }

       submitting.value = true;
       try {
         let requestData, apiMethod;

         if (selectedApplicationType.value === 'single') {
           // 单人贷款申请
           requestData = {
             phone: userInfo.value.phone,
             product_id: props.product.product_id,
             apply_amount: applyAmount.value,
             purpose: formData.purpose,
             repayment_source: formData.repayment_source
           };
           apiMethod = financingService.applyForSingleLoan;
         } else {
           // 联合贷款申请
           requestData = {
             phone: userInfo.value.phone,
             product_id: props.product.product_id,
             apply_amount: applyAmount.value,
             purpose: formData.purpose,
             repayment_plan: formData.repayment_plan,
             partner_phones: [selectedPartner.value.phone]
           };
           apiMethod = financingService.applyForJointLoan;
         }

         // 如果product_id仍然为空，尝试使用product.id作为备用
         if (!requestData.product_id && props.product.id) {
           console.log('WARN: 提交时product_id为空，尝试使用product.id作为备用');
           requestData.product_id = props.product.id;
         }

         console.log('DEBUG: 提交申请的请求数据 =', requestData);

        logger.info('UNIFIED_LOAN', `提交${selectedApplicationType.value === 'single' ? '单人' : '联合'}贷款申请`, {
          apply_amount: applyAmount.value,
          partners: selectedApplicationType.value === 'joint' ? [selectedPartner.value.phone] : []
        });

        await apiMethod(requestData);

        logger.info('UNIFIED_LOAN', '贷款申请提交成功');
        
        // 显示成功消息
        alert(selectedApplicationType.value === 'single' ? 
          '单人贷款申请提交成功，请等待银行审批' : 
          '联合贷款申请提交成功，请等待合作伙伴确认和银行审批'
        );

        emit('success');
      } catch (error) {
        logger.error('UNIFIED_LOAN', '贷款申请提交失败', {
          errorMessage: error.message
        }, error);
        
        alert(error.message || '申请提交失败，请稍后重试');
      } finally {
        submitting.value = false;
      }
    };

    // 关闭弹窗
    const handleClose = () => {
      emit('close');
    };

    return {
      currentStep,
      applyAmount,
      checkingRecommendation,
      recommendation,
      selectedPartner,
      selectedApplicationType,
      submitting,
      formData,
      canProceedWithJoint,
      repaymentInfo,
      formatAmount,
      getRecommendation,
      selectPartner,
      proceedWithSingle,
      proceedWithJoint,
      goBack,
      goBackToRecommendation,
      handleSubmit,
      handleClose
    };
  }
};
</script>

<style scoped>
@import '../../assets/styles/theme.css';

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background: var(--white);
  border-radius: 16px;
  max-width: 800px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(107, 70, 193, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--gray-200);
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--white);
  margin: 0;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 2rem;
  color: var(--white);
  cursor: pointer;
  padding: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.btn-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.modal-body {
  padding: 2rem;
  max-height: calc(90vh - 80px);
  overflow-y: auto;
}

/* 产品信息 */
.product-info {
  background: linear-gradient(135deg, #f8faff 0%, #f1f5ff 100%);
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  margin-bottom: 2rem;
}

.product-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0 0 1rem 0;
}

.product-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.detail-row span:first-child {
  color: var(--gray-600);
}

.detail-row span:last-child {
  font-weight: 600;
  color: var(--primary);
}

/* 信用信息卡片 */
.credit-info-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.credit-info-header h4 {
  margin: 0 0 1rem 0;
  color: var(--primary);
  font-size: 1.125rem;
}

.credit-info-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.credit-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.credit-item .label {
  color: var(--gray-600);
  font-size: 0.875rem;
}

.credit-item .value {
  font-weight: 600;
  font-size: 1rem;
}

.credit-item .value.available {
  color: var(--success);
}

.credit-item .value.apply {
  color: var(--primary);
}

/* 推荐卡片 */
.recommendation-card {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #f59e0b;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.recommendation-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.recommendation-icon {
  font-size: 2rem;
}

.recommendation-header h4 {
  margin: 0;
  color: #92400e;
  font-size: 1.125rem;
}

.recommendation-reason {
  color: #92400e;
  line-height: 1.6;
}

/* 合作伙伴区域 */
.partners-section {
  margin-bottom: 2rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 1rem;
}

.partners-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.partner-card {
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.partner-card:hover {
  border-color: var(--primary-light);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.15);
}

.partner-card.selected {
  border-color: var(--primary);
  background: linear-gradient(135deg, #f8faff 0%, #f1f5ff 100%);
}

.partner-info {
  flex: 1;
}

.partner-name {
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 0.25rem;
}

.partner-phone {
  color: var(--gray-500);
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.partner-credit,
.partner-combined {
  font-size: 0.8125rem;
  color: var(--gray-600);
}

.partner-combined {
  color: var(--success);
  font-weight: 600;
}

.selection-indicator {
  font-size: 1.5rem;
  color: var(--primary);
  width: 2rem;
  text-align: center;
}

/* 申请选项 */
.application-options {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.application-options .btn {
  flex: 1;
}

/* 申请概要 */
.application-summary {
  background: linear-gradient(135deg, #f8faff 0%, #f1f5ff 100%);
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  margin-bottom: 2rem;
}

.application-summary h4 {
  margin: 0 0 1rem 0;
  color: var(--primary);
  font-size: 1.125rem;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item span:first-child {
  color: var(--gray-600);
}

.summary-item span:last-child {
  font-weight: 600;
  color: var(--primary);
}

/* 表单样式 */
.form {
  margin-top: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
}

.required {
  color: var(--error);
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid var(--gray-300);
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
  background-color: var(--white);
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.form-input.textarea {
  resize: vertical;
  min-height: 90px;
  line-height: 1.5;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--gray-500);
  margin-top: 0.25rem;
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
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  font-size: 0.875rem;
  min-width: 120px;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
  color: var(--white);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(107, 70, 193, 0.3);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  background: var(--white);
  color: var(--gray-600);
  border-color: var(--gray-300);
}

.btn-secondary:hover {
  background: var(--gray-50);
  border-color: var(--primary);
  color: var(--primary);
}

.btn-outline {
  background: transparent;
  color: var(--primary);
  border-color: var(--primary);
}

.btn-outline:hover {
  background: var(--primary);
  color: var(--white);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-container {
    width: 95%;
    max-height: 95vh;
  }

  .modal-body {
    padding: 1rem;
  }

  .product-details,
  .credit-info-content {
    grid-template-columns: 1fr;
  }

  .partners-grid {
    grid-template-columns: 1fr;
  }

  .application-options {
    flex-direction: column;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
