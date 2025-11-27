<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">审批信贷额度申请</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loadingApplications" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载申请列表中...</p>
        </div>

        <div v-else-if="applications.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <h3>暂无待审批申请</h3>
          <p>当前没有待审批的信贷额度申请</p>
        </div>

        <div v-else class="applications-list">
          <div class="list-header">
            <h3>待审批申请 ({{ applications.length }})</h3>
          </div>

          <div class="application-item" v-for="application in applications" :key="application.application_id">
            <div class="application-info">
              <div class="application-header">
                <span class="application-id">{{ application.application_id }}</span>
                <span class="application-amount">{{ formatCurrency(application.apply_amount) }}</span>
              </div>
              <div class="application-details">
                <div class="detail-item">
                  <span class="label">申请人：</span>
                  <span class="value">{{ application.farmer_name }} ({{ application.farmer_phone }})</span>
                </div>
                <div class="detail-item">
                  <span class="label">证明类型：</span>
                  <span class="value">{{ getProofTypeName(application.proof_type) }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">申请时间：</span>
                  <span class="value">{{ formatDate(application.created_at) }}</span>
                </div>
                <div v-if="application.description" class="detail-item">
                  <span class="label">申请说明：</span>
                  <span class="value">{{ application.description }}</span>
                </div>
                <div v-if="getProofImages(application.proof_images).length > 0" class="detail-item">
                  <span class="label">证明材料：</span>
                  <div class="proof-images">
                    <div 
                      v-for="(imageUrl, index) in getProofImages(application.proof_images)" 
                      :key="index"
                      class="image-preview"
                      @click="viewImage(imageUrl)"
                    >
                      <img :src="imageUrl" :alt="`证明材料${index + 1}`" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="application-actions">
              <button 
                class="btn btn-approve" 
                @click="handleApprove(application)"
                :disabled="submitting"
              >
                批准
              </button>
              <button 
                class="btn btn-reject" 
                @click="handleReject(application)"
                :disabled="submitting"
              >
                拒绝
              </button>
            </div>
          </div>
        </div>

        <!-- 审批表单弹窗 -->
        <div v-if="showApprovalForm" class="approval-form-overlay" @click.self="closeApprovalForm">
          <div class="approval-form-container">
            <div class="approval-form-header">
              <h3>{{ approvalAction === 'approve' ? '批准申请' : '拒绝申请' }}</h3>
              <button class="btn-close" @click="closeApprovalForm">×</button>
            </div>
            <div class="approval-form-body">
              <div class="selected-application-info">
                <p><strong>申请ID：</strong>{{ selectedApplication?.application_id }}</p>
                <p><strong>申请人：</strong>{{ selectedApplication?.farmer_name }}</p>
                <p><strong>申请金额：</strong>{{ formatCurrency(selectedApplication?.apply_amount) }}</p>
              </div>
              
              <form @submit.prevent="submitApproval">
                <div v-if="approvalAction === 'approve'" class="form-group">
                  <label class="form-label">批准额度 <span class="required">*</span></label>
                  <input
                    v-model.number="formData.approved_amount"
                    type="number"
                    class="form-input"
                    placeholder="请输入批准额度（元）"
                    min="0"
                    step="0.01"
                    required
                  />
                  <div class="form-hint">
                    提示：可以调整申请额度，建议不超过申请金额的120%
                  </div>
                </div>

                <div v-if="approvalAction === 'reject'" class="form-group">
                  <label class="form-label">拒绝原因 <span class="required">*</span></label>
                  <textarea
                    v-model="formData.reject_reason"
                    class="form-input textarea"
                    rows="4"
                    placeholder="请输入拒绝原因"
                    required
                  ></textarea>
                  <div class="form-hint">
                    常见拒绝原因：证明材料不足、信用记录不良、申请金额过高等
                  </div>
                </div>

                <div class="form-actions">
                  <button type="button" class="btn btn-secondary" @click="closeApprovalForm">
                    取消
                  </button>
                  <button type="submit" class="btn btn-primary" :disabled="submitting">
                    {{ submitting ? '提交中...' : (approvalAction === 'approve' ? '确认批准' : '确认拒绝') }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'CreditApprovalModal',
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const applications = ref([]);
    const loadingApplications = ref(false);
    const submitting = ref(false);
    const showApprovalForm = ref(false);
    const approvalAction = ref('');
    const selectedApplication = ref(null);
    const formData = reactive({
      approved_amount: null,
      reject_reason: ''
    });

    onMounted(async () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        await loadApplications();
      }
    });

    const loadApplications = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      loadingApplications.value = true;
      try {
        logger.info('FINANCING', '获取待审批信贷额度申请列表', { phone: userInfo.value.phone });
        
        const response = await financingService.getPendingCreditApplications(userInfo.value.phone);
        applications.value = response.data.applications || [];
        
        logger.info('FINANCING', '获取待审批信贷额度申请列表成功', { 
          count: applications.value.length 
        });
      } catch (error) {
        logger.error('FINANCING', '获取待审批信贷额度申请列表失败', {
          errorMessage: error.message || error
        }, error);
        alert('获取申请列表失败：' + (error.message || '请稍后重试'));
      } finally {
        loadingApplications.value = false;
      }
    };

    const handleApprove = (application) => {
      selectedApplication.value = application;
      approvalAction.value = 'approve';
      formData.approved_amount = application.apply_amount;
      formData.reject_reason = '';
      showApprovalForm.value = true;
    };

    const handleReject = (application) => {
      selectedApplication.value = application;
      approvalAction.value = 'reject';
      formData.approved_amount = null;
      formData.reject_reason = '';
      showApprovalForm.value = true;
    };

    const closeApprovalForm = () => {
      showApprovalForm.value = false;
      selectedApplication.value = null;
      approvalAction.value = '';
      formData.approved_amount = null;
      formData.reject_reason = '';
    };

    const submitApproval = async () => {
      if (!selectedApplication.value) return;

      if (approvalAction.value === 'approve' && !formData.approved_amount) {
        alert('请输入批准额度');
        return;
      }

      if (approvalAction.value === 'reject' && !formData.reject_reason.trim()) {
        alert('请输入拒绝原因');
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '提交信贷额度审批', { 
          application_id: selectedApplication.value.application_id,
          action: approvalAction.value 
        });

        const approvalData = {
          phone: userInfo.value.phone,
          application_id: selectedApplication.value.application_id,
          action: approvalAction.value,
          ...(approvalAction.value === 'approve' && { approved_amount: parseFloat(formData.approved_amount) }),
          ...(approvalAction.value === 'reject' && { reject_reason: formData.reject_reason })
        };

        const response = await financingService.approveCreditApplication(approvalData);
        
        logger.info('FINANCING', '信贷额度审批提交成功', { 
          application_id: selectedApplication.value.application_id,
          action: approvalAction.value
        });
        
        if (approvalAction.value === 'approve') {
          alert(`审批成功！已批准额度：${formData.approved_amount}元`);
        } else {
          alert('审批成功！已拒绝申请');
        }

        // 关闭审批表单并重新加载列表
        closeApprovalForm();
        await loadApplications();
        
        emit('success');
      } catch (error) {
        logger.error('FINANCING', '提交信贷额度审批失败', {
          errorMessage: error.message || error
        }, error);
        alert('提交失败：' + (error.message || '请稍后重试'));
      } finally {
        submitting.value = false;
      }
    };

    const handleClose = () => {
      emit('close');
    };

    // 工具函数
    const formatCurrency = (amount) => {
      return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'CNY',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }).format(amount);
    };

    const formatDate = (dateString) => {
      return new Date(dateString).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    const getProofTypeName = (proofType) => {
      const typeMap = {
        'land_certificate': '土地证书',
        'property_certificate': '房产证书',
        'income_proof': '收入证明',
        'business_license': '营业执照',
        'other': '其他'
      };
      return typeMap[proofType] || proofType;
    };

    const getProofImages = (proofImagesJson) => {
      if (!proofImagesJson) return [];
      try {
        // 如果已经是数组，直接返回
        if (Array.isArray(proofImagesJson)) {
          return proofImagesJson;
        }
        // 如果是字符串，尝试解析JSON
        return JSON.parse(proofImagesJson);
      } catch (error) {
        console.warn('解析证明材料图片失败:', error);
        return [];
      }
    };

    const viewImage = (imageUrl) => {
      // 在新窗口打开图片
      window.open(imageUrl, '_blank');
    };

    return {
      userInfo,
      applications,
      loadingApplications,
      submitting,
      showApprovalForm,
      approvalAction,
      selectedApplication,
      formData,
      handleApprove,
      handleReject,
      closeApprovalForm,
      submitApproval,
      handleClose,
      formatCurrency,
      formatDate,
      getProofTypeName,
      getProofImages,
      viewImage
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
  max-width: 900px;
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

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
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

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 3rem;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: var(--gray-600);
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: var(--gray-500);
}

/* 申请列表 */
.applications-list {
  max-height: 60vh;
  overflow-y: auto;
}

.list-header {
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--gray-100);
}

.list-header h3 {
  color: var(--gray-700);
  margin: 0;
}

.application-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  margin-bottom: 0.75rem;
  border: 1px solid var(--gray-200);
  border-radius: 12px;
  background: var(--white);
  transition: all 0.2s;
}

.application-item:hover {
  border-color: var(--primary);
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
}

.application-info {
  flex: 1;
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.application-id {
  font-weight: 600;
  color: var(--primary);
  font-size: 0.875rem;
}

.application-amount {
  font-weight: 700;
  color: var(--success);
  font-size: 1.1rem;
}

.application-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.25rem;
}

.detail-item {
  font-size: 0.875rem;
  color: var(--gray-600);
}

.detail-item .label {
  font-weight: 500;
}

.detail-item .value {
  color: var(--gray-800);
}

/* 证明材料图片样式 */
.proof-images {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.image-preview {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid var(--gray-200);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--gray-50);
}

.image-preview:hover {
  border-color: var(--primary);
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.application-actions {
  display: flex;
  gap: 0.5rem;
  margin-left: 1rem;
}

/* 审批表单弹窗 */
.approval-form-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.approval-form-container {
  background: var(--white);
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.approval-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
}

.approval-form-header h3 {
  margin: 0;
  color: var(--primary);
}

.approval-form-body {
  padding: 1.5rem;
}

.selected-application-info {
  background: var(--gray-50);
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

.selected-application-info p {
  margin: 0.25rem 0;
  font-size: 0.875rem;
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

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-approve {
  background: var(--success);
  color: var(--white);
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.btn-approve:hover:not(:disabled) {
  background: #16a34a;
}

.btn-reject {
  background: var(--error);
  color: var(--white);
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.btn-reject:hover:not(:disabled) {
  background: #dc2626;
}

.btn-primary {
  background: var(--primary);
  color: var(--white);
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-secondary {
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-secondary:hover {
  background: var(--gray-300);
}
</style>
