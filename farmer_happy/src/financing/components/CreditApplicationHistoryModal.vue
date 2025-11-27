<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">申请额度记录</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="applications.length === 0" class="empty-container">
          <div class="empty-icon">📋</div>
          <h3>暂无申请记录</h3>
          <p>您还没有申请过额度，点击申请按钮开始申请</p>
        </div>

        <div v-else class="applications-list">
          <div 
            v-for="application in applications" 
            :key="application.application_id"
            class="application-item"
            :class="getStatusClass(application.status)"
          >
            <div class="application-header">
              <div class="application-id">
                <span class="label">申请编号：</span>
                <span class="value">{{ application.application_id }}</span>
              </div>
              <div class="application-status">
                <span class="status-badge" :class="getStatusClass(application.status)">
                  {{ getStatusText(application.status) }}
                </span>
              </div>
            </div>

            <div class="application-details">
              <div class="detail-row">
                <span class="detail-label">申请金额：</span>
                <span class="detail-value amount">{{ formatAmount(application.apply_amount) }} 元</span>
              </div>
              
              <div v-if="application.approved_amount" class="detail-row">
                <span class="detail-label">批准金额：</span>
                <span class="detail-value amount success">{{ formatAmount(application.approved_amount) }} 元</span>
              </div>

              <div class="detail-row">
                <span class="detail-label">证明类型：</span>
                <span class="detail-value">{{ getProofTypeName(application.proof_type) }}</span>
              </div>

              <div class="detail-row">
                <span class="detail-label">申请时间：</span>
                <span class="detail-value">{{ formatDate(application.created_at) }}</span>
              </div>

              <div v-if="application.approved_at" class="detail-row">
                <span class="detail-label">审批时间：</span>
                <span class="detail-value">{{ formatDate(application.approved_at) }}</span>
              </div>

              <div v-if="application.approver_name" class="detail-row">
                <span class="detail-label">审批机构：</span>
                <span class="detail-value">{{ application.approver_name }}</span>
              </div>

              <div v-if="application.description" class="detail-row">
                <span class="detail-label">申请说明：</span>
                <span class="detail-value">{{ application.description }}</span>
              </div>

              <div v-if="application.reject_reason" class="detail-row">
                <span class="detail-label">拒绝原因：</span>
                <span class="detail-value reject-reason">{{ application.reject_reason }}</span>
              </div>
            </div>

            <div v-if="application.proof_images && application.proof_images !== '[]'" class="proof-images">
              <div class="images-label">证明材料：</div>
              <div class="images-grid">
                <div 
                  v-for="(image, index) in parseProofImages(application.proof_images)"
                  :key="index"
                  class="image-item"
                  @click="previewImage(image)"
                >
                  <img :src="image" :alt="`证明材料${index + 1}`" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-secondary" @click="handleClose">
          关闭
        </button>
        <button 
          class="btn btn-primary" 
          @click="handleApply"
          :disabled="hasPendingApplication"
        >
          {{ hasPendingApplication ? '存在待审批申请' : '申请额度' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 图片预览模态框 -->
  <div v-if="previewImageUrl" class="image-preview-overlay" @click="closePreview">
    <div class="image-preview-container" @click.stop>
      <img :src="previewImageUrl" alt="预览图片" />
      <button class="preview-close" @click="closePreview">×</button>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'CreditApplicationHistoryModal',
  emits: ['close', 'apply'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const loading = ref(false);
    const applications = ref([]);
    const previewImageUrl = ref(null);

    const hasPendingApplication = computed(() => {
      return applications.value.some(app => app.status === 'pending');
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
        loadApplications();
      }
    });

    const loadApplications = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      loading.value = true;
      try {
        logger.info('FINANCING', '获取申请记录', { phone: userInfo.value.phone });
        
        const response = await financingService.getFarmerCreditApplications(userInfo.value.phone);
        applications.value = response.applications || [];
        
        logger.info('FINANCING', '获取申请记录成功', { 
          count: applications.value.length 
        });
      } catch (error) {
        logger.error('FINANCING', '获取申请记录失败', {
          errorMessage: error.message || error
        }, error);
        alert('获取申请记录失败：' + (error.message || '请稍后重试'));
      } finally {
        loading.value = false;
      }
    };

    const handleClose = () => {
      emit('close');
    };

    const handleApply = () => {
      emit('apply');
    };

    const getStatusClass = (status) => {
      switch (status) {
        case 'pending': return 'status-pending';
        case 'approved': return 'status-approved';
        case 'rejected': return 'status-rejected';
        default: return '';
      }
    };

    const getStatusText = (status) => {
      switch (status) {
        case 'pending': return '待审批';
        case 'approved': return '已批准';
        case 'rejected': return '已拒绝';
        default: return status;
      }
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

    const formatAmount = (amount) => {
      if (!amount) return '0';
      return new Intl.NumberFormat('zh-CN').format(amount);
    };

    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    const parseProofImages = (imagesJson) => {
      if (!imagesJson || imagesJson === '[]') return [];
      try {
        return JSON.parse(imagesJson);
      } catch (error) {
        logger.error('FINANCING', '解析证明图片失败', { imagesJson }, error);
        return [];
      }
    };

    const previewImage = (imageUrl) => {
      previewImageUrl.value = imageUrl;
    };

    const closePreview = () => {
      previewImageUrl.value = null;
    };

    return {
      userInfo,
      loading,
      applications,
      previewImageUrl,
      hasPendingApplication,
      loadApplications,
      handleClose,
      handleApply,
      getStatusClass,
      getStatusText,
      getProofTypeName,
      formatAmount,
      formatDate,
      parseProofImages,
      previewImage,
      closePreview
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
  min-height: 200px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
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

.empty-container {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--gray-500);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-container h3 {
  color: var(--gray-600);
  margin-bottom: 0.5rem;
}

.applications-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.application-item {
  border: 1px solid var(--gray-200);
  border-radius: 12px;
  padding: 1.5rem;
  background: var(--white);
  transition: all 0.2s;
}

.application-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.application-item.status-pending {
  border-left: 4px solid var(--warning);
}

.application-item.status-approved {
  border-left: 4px solid var(--success);
}

.application-item.status-rejected {
  border-left: 4px solid var(--error);
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.application-id {
  font-size: 1.1rem;
  font-weight: 600;
}

.application-id .label {
  color: var(--gray-600);
}

.application-id .value {
  color: var(--primary);
  font-family: monospace;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-badge.status-pending {
  background: var(--warning-light);
  color: var(--warning-dark);
}

.status-badge.status-approved {
  background: var(--success-light);
  color: var(--success-dark);
}

.status-badge.status-rejected {
  background: var(--error-light);
  color: var(--error-dark);
}

.application-details {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.detail-row {
  display: flex;
  align-items: flex-start;
}

.detail-label {
  min-width: 100px;
  color: var(--gray-600);
  font-size: 0.9rem;
  flex-shrink: 0;
}

.detail-value {
  color: var(--gray-800);
  font-weight: 500;
}

.detail-value.amount {
  color: var(--primary);
  font-weight: 600;
}

.detail-value.amount.success {
  color: var(--success);
}

.reject-reason {
  color: var(--error);
  font-weight: 500;
}

.proof-images {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.images-label {
  font-size: 0.9rem;
  color: var(--gray-600);
  margin-bottom: 0.5rem;
}

.images-grid {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.image-item {
  width: 80px;
  height: 80px;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.image-item:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.modal-footer {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding: 1.5rem;
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

/* 图片预览样式 */
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.image-preview-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.image-preview-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}

.preview-close {
  position: absolute;
  top: -40px;
  right: -40px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  font-size: 2rem;
  color: var(--gray-700);
  cursor: pointer;
  padding: 0.5rem;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.preview-close:hover {
  background: var(--white);
  color: var(--gray-900);
}

/* CSS变量定义 */
:root {
  --white: #ffffff;
  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-200: #e5e7eb;
  --gray-300: #d1d5db;
  --gray-400: #9ca3af;
  --gray-500: #6b7280;
  --gray-600: #4b5563;
  --gray-700: #374151;
  --gray-800: #1f2937;
  --gray-900: #111827;
  --primary: #6b46c1;
  --primary-dark: #553c9a;
  --success: #10b981;
  --success-light: #d1fae5;
  --success-dark: #047857;
  --warning: #f59e0b;
  --warning-light: #fef3c7;
  --warning-dark: #d97706;
  --error: #ef4444;
  --error-light: #fecaca;
  --error-dark: #dc2626;
}
</style>
