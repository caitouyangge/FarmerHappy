<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">申请贷款额度</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="form">
          <div class="form-group">
            <label class="form-label">证明类型 <span class="required">*</span></label>
            <select v-model="formData.proof_type" class="form-input" required>
              <option value="">请选择证明类型</option>
              <option value="land_certificate">土地证书</option>
              <option value="property_certificate">房产证书</option>
              <option value="income_proof">收入证明</option>
              <option value="business_license">营业执照</option>
              <option value="other">其他</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">申请额度 <span class="required">*</span></label>
            <input
              v-model.number="formData.apply_amount"
              type="number"
              class="form-input"
              placeholder="请输入申请额度（元）"
              min="1"
              step="0.01"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label">证明材料图片</label>
            <div class="upload-area">
              <input
                ref="fileInput"
                type="file"
                multiple
                accept="image/*"
                @change="handleFileChange"
                class="file-input"
              />
              <div class="upload-placeholder" @click="$refs.fileInput.click()">
                <span class="upload-icon">📎</span>
                <span>点击上传图片（可多选）</span>
              </div>
            </div>
            <div v-if="formData.proof_images.length > 0" class="uploaded-files">
              <div v-for="(file, index) in formData.proof_images" :key="index" class="file-item">
                <span>{{ file.name || file }}</span>
                <button type="button" @click="removeFile(index)" class="btn-remove">×</button>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">申请说明</label>
            <textarea
              v-model="formData.description"
              class="form-input textarea"
              rows="4"
              placeholder="请输入申请说明（可选）"
            ></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="handleClose">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting || uploadingImages">
              {{ submitButtonText }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'CreditLimitApplicationModal',
  emits: ['close', 'success', 'viewHistory'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const fileInput = ref(null);
    const submitting = ref(false);
    const uploadingImages = ref(false);
    const formData = reactive({
      proof_type: '',
      apply_amount: null,
      proof_images: [],
      description: ''
    });

    const submitButtonText = computed(() => {
      if (uploadingImages.value) return '上传图片中...';
      if (submitting.value) return '提交申请中...';
      return '提交申请';
    });

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        userInfo.value = JSON.parse(storedUser);
      }
    });

    const handleFileChange = (event) => {
      const files = Array.from(event.target.files);
      files.forEach(file => {
        if (file.type.startsWith('image/')) {
          // 这里可以上传到服务器获取URL，暂时使用文件名
          formData.proof_images.push({
            name: file.name,
            file: file
          });
        }
      });
    };

    const removeFile = (index) => {
      formData.proof_images.splice(index, 1);
    };

    const handleClose = () => {
      emit('close');
    };

    const handleSubmit = async () => {
      if (!userInfo.value.phone) {
        alert('请先登录');
        return;
      }

      submitting.value = true;
      try {
        logger.info('FINANCING', '提交贷款额度申请', { formData });

        // 先上传图片获取真实URL
        let imageUrls = [];
        if (formData.proof_images.length > 0) {
          uploadingImages.value = true;
          logger.info('FINANCING', '开始上传图片', { count: formData.proof_images.length });
          
          try {
            // 将文件转换为base64格式用于上传
            const imagesToUpload = [];
            for (const imgObj of formData.proof_images) {
              if (imgObj.file) {
                const reader = new FileReader();
                const base64Promise = new Promise((resolve, reject) => {
                  reader.onload = () => resolve(reader.result);
                  reader.onerror = reject;
                });
                reader.readAsDataURL(imgObj.file);
                imagesToUpload.push(base64Promise);
              }
            }
            
            // 等待所有文件读取完成
            const base64Images = await Promise.all(imagesToUpload);
            
            // 上传图片到服务器
            const uploadResponse = await fetch('/api/v1/storage/upload', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
              },
              body: JSON.stringify({
                images: base64Images
              })
            });
            
            const uploadResult = await uploadResponse.json();
            if (uploadResult.code === 201 && uploadResult.data && uploadResult.data.urls) {
              imageUrls = uploadResult.data.urls;
              logger.info('FINANCING', '图片上传成功', { urls: imageUrls });
            } else {
              throw new Error('图片上传失败：' + (uploadResult.message || '未知错误'));
            }
          } finally {
            uploadingImages.value = false;
          }
        }

        const applicationData = {
          phone: userInfo.value.phone,
          proof_type: formData.proof_type,
          proof_images: imageUrls,
          apply_amount: parseFloat(formData.apply_amount),
          ...(formData.description && { description: formData.description })
        };

        const response = await financingService.applyForCreditLimit(applicationData);
        
        logger.info('FINANCING', '贷款额度申请提交成功', { 
          application_id: response.data?.application_id 
        });
        
        alert('申请提交成功！请等待审核');
        emit('success');
        handleClose();
      } catch (error) {
        logger.error('FINANCING', '提交贷款额度申请失败', {
          errorMessage: error.message || error,
          errorCode: error.code
        }, error);
        
        if (error.code === 409) {
          // 存在待审批申请的情况
          const result = confirm('您已有待审批的额度申请，请勿重复提交。\n\n点击"确定"查看申请记录，点击"取消"关闭对话框。');
          if (result) {
            emit('viewHistory'); // 触发查看申请记录
          }
        } else {
          alert('提交失败：' + (error.message || '请稍后重试'));
        }
      } finally {
        submitting.value = false;
      }
    };

    return {
      userInfo,
      fileInput,
      submitting,
      uploadingImages,
      formData,
      submitButtonText,
      handleFileChange,
      removeFile,
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

.upload-area {
  position: relative;
}

.file-input {
  display: none;
}

.upload-placeholder {
  padding: 2rem;
  border: 2px dashed var(--gray-300);
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: var(--gray-500);
}

.upload-placeholder:hover {
  border-color: var(--primary);
  background: var(--gray-50);
}

.upload-icon {
  font-size: 2rem;
}

.uploaded-files {
  margin-top: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem;
  background: var(--gray-100);
  border-radius: 6px;
  font-size: 0.875rem;
}

.btn-remove {
  background: none;
  border: none;
  color: var(--error);
  cursor: pointer;
  font-size: 1.25rem;
  padding: 0 0.5rem;
  line-height: 1;
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

