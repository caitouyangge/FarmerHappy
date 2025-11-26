<template>
  <div class="content-form-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <button class="btn-back" @click="goBack">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1 class="header-title">发布内容</h1>
      <div class="header-placeholder"></div>
    </header>

    <!-- 表单内容 -->
    <main class="main-content">
      <div class="form-wrapper">
        <form @submit.prevent="handleSubmit" class="content-form">
          <!-- 内容类型 -->
          <div class="form-group">
            <label class="form-label">内容类型 <span class="required">*</span></label>
            <select
              v-model="form.content_type"
              class="form-input"
              :class="{ 'error': errors.content_type }"
              @change="clearError('content_type')"
            >
              <option value="">请选择内容类型</option>
              <option value="articles">文章</option>
              <option value="questions">提问</option>
              <option value="experiences">经验分享</option>
            </select>
            <span v-if="errors.content_type" class="form-error">{{ errors.content_type }}</span>
          </div>

          <!-- 标题 -->
          <div class="form-group">
            <label class="form-label">标题 <span class="required">*</span></label>
            <input
              v-model="form.title"
              type="text"
              class="form-input"
              :class="{ 'error': errors.title }"
              placeholder="请输入标题"
              maxlength="100"
              @input="clearError('title')"
            />
            <div class="input-hint">
              <span class="char-count">{{ form.title.length }}/100</span>
            </div>
            <span v-if="errors.title" class="form-error">{{ errors.title }}</span>
          </div>

          <!-- 内容 -->
          <div class="form-group">
            <label class="form-label">内容 <span class="required">*</span></label>
            <textarea
              v-model="form.content"
              class="form-textarea"
              :class="{ 'error': errors.content }"
              placeholder="请输入内容..."
              rows="12"
              @input="clearError('content')"
            ></textarea>
            <div class="input-hint">
              <span class="char-count">{{ form.content.length }}/5000</span>
            </div>
            <span v-if="errors.content" class="form-error">{{ errors.content }}</span>
          </div>

          <!-- 图片上传（可选） -->
          <div class="form-group">
            <label class="form-label">图片（可选）</label>
            <div class="image-upload-section">
              <div class="image-preview-list">
                <div
                  v-for="(image, idx) in imagePreviews"
                  :key="idx"
                  class="image-preview-item"
                >
                  <img :src="image" :alt="`预览${idx + 1}`" class="preview-image" />
                  <button
                    type="button"
                    class="btn-remove-image"
                    @click="removeImage(idx)"
                  >
                    ×
                  </button>
                </div>
              </div>
              <label v-if="imagePreviews.length < 9" class="upload-btn">
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  @change="handleImageSelect"
                  class="file-input"
                />
                <span class="upload-icon">📷</span>
                <span class="upload-text">添加图片</span>
              </label>
            </div>
            <div class="upload-hint">
              最多可上传 9 张图片，支持 JPG、PNG 格式
            </div>
            <span v-if="errors.images" class="form-error">{{ errors.images }}</span>
          </div>

          <!-- 提交按钮 -->
          <div class="form-actions">
            <button type="button" class="btn btn-cancel" @click="goBack">
              取消
            </button>
            <button type="submit" class="btn btn-submit" :disabled="submitting">
              {{ submitting ? '发布中...' : '发布内容' }}
            </button>
          </div>
        </form>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { communityService } from '../api/community';
import logger from '../utils/logger';

export default {
  name: 'ContentForm',
  setup() {
    const router = useRouter();
    const submitting = ref(false);
    const imagePreviews = ref([]);
    const imageFiles = ref([]);

    const form = reactive({
      title: '',
      content: '',
      content_type: '',
      images: []
    });

    const errors = reactive({
      title: '',
      content: '',
      content_type: '',
      images: ''
    });

    // 获取用户信息
    const getUserInfo = () => {
      try {
        const storedUser = localStorage.getItem('user');
        return storedUser ? JSON.parse(storedUser) : null;
      } catch (error) {
        logger.error('CONTENT_FORM', '获取用户信息失败', {}, error);
        return null;
      }
    };

    // 清除错误
    const clearError = (field) => {
      errors[field] = '';
    };

    // 验证表单
    const validateForm = () => {
      let isValid = true;
      errors.title = '';
      errors.content = '';
      errors.content_type = '';

      if (!form.content_type) {
        errors.content_type = '请选择内容类型';
        isValid = false;
      }

      if (!form.title || !form.title.trim()) {
        errors.title = '请输入标题';
        isValid = false;
      } else if (form.title.trim().length < 5) {
        errors.title = '标题至少需要 5 个字符';
        isValid = false;
      } else if (form.title.trim().length > 100) {
        errors.title = '标题不能超过 100 个字符';
        isValid = false;
      }

      if (!form.content || !form.content.trim()) {
        errors.content = '请输入内容';
        isValid = false;
      } else if (form.content.trim().length < 10) {
        errors.content = '内容至少需要 10 个字符';
        isValid = false;
      } else if (form.content.trim().length > 5000) {
        errors.content = '内容不能超过 5000 个字符';
        isValid = false;
      }

      logger.validation('ContentForm', isValid, errors);
      return isValid;
    };

    // 处理图片选择
    const handleImageSelect = (event) => {
      const files = Array.from(event.target.files);
      const remainingSlots = 9 - imagePreviews.value.length;

      if (files.length > remainingSlots) {
        alert(`最多只能上传 ${remainingSlots} 张图片`);
        files.splice(remainingSlots);
      }

      files.forEach((file) => {
        if (!file.type.startsWith('image/')) {
          alert('只能上传图片文件');
          return;
        }

        if (file.size > 5 * 1024 * 1024) {
          alert('图片大小不能超过 5MB');
          return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
          imagePreviews.value.push(e.target.result);
          imageFiles.value.push(file);
        };
        reader.readAsDataURL(file);
      });

      // 清空 input 以便再次选择相同文件时也能触发 change 事件
      event.target.value = '';
    };

    // 移除图片
    const removeImage = (index) => {
      imagePreviews.value.splice(index, 1);
      imageFiles.value.splice(index, 1);
    };

    const processImages = async () => {
      if (!imagePreviews.value || imagePreviews.value.length === 0) {
        return [];
      }
      const urls = await communityService.uploadImages(imagePreviews.value);
      return urls;
    };

    // 提交表单
    const handleSubmit = async () => {
      logger.userAction('SUBMIT_CONTENT', { contentType: form.content_type });

      if (!validateForm()) {
        logger.warn('CONTENT_FORM', '表单验证失败');
        return;
      }

      const userInfo = getUserInfo();
      if (!userInfo || !userInfo.phone) {
        alert('请先登录');
        router.push('/login');
        return;
      }

      submitting.value = true;

      try {
        const images = await processImages();

        const contentData = {
          title: form.title.trim(),
          content: form.content.trim(),
          content_type: form.content_type,
          phone: userInfo.phone,
          images: images
        };

        logger.info('CONTENT_FORM', '提交内容', {
          contentType: form.content_type,
          title: form.title.substring(0, 20) + '...',
          imageCount: images.length
        });

        const response = await communityService.publishContent(contentData);

        logger.info('CONTENT_FORM', '内容发布成功', {
          contentId: response.data?.content_id
        });

        alert('发布成功！');
        router.push('/community');
      } catch (error) {
        logger.error('CONTENT_FORM', '发布内容失败', {}, error);

        if (error.code && error.message) {
          // 处理字段级错误
          if (error.errors && error.errors.length > 0) {
            error.errors.forEach((err) => {
              if (err.field && Object.prototype.hasOwnProperty.call(errors, err.field)) {
                errors[err.field] = err.message || error.message;
              }
            });
          } else {
            alert(error.message || '发布失败，请稍后重试');
          }
        } else {
          alert(error.message || '发布失败，请稍后重试');
        }
      } finally {
        submitting.value = false;
      }
    };

    // 返回
    const goBack = () => {
      if (confirm('确定要离开吗？未保存的内容将丢失。')) {
        router.push('/community');
      }
    };

    onMounted(() => {
      logger.lifecycle('ContentForm', 'mounted');
    });

    return {
      form,
      errors,
      submitting,
      imagePreviews,
      clearError,
      handleImageSelect,
      removeImage,
      handleSubmit,
      goBack
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.content-form-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}

/* 顶部导航栏 */
.header {
  background: var(--white);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  color: var(--gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--gray-100);
  border-color: var(--primary);
  color: var(--primary);
}

.header-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0;
}

.header-placeholder {
  width: 120px;
}

/* 主内容 */
.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
}

.form-wrapper {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.content-form {
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
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
}

.required {
  color: var(--error);
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.875rem;
  border: 2px solid var(--gray-300);
  border-radius: 8px;
  font-size: 1rem;
  font-family: inherit;
  transition: all 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.form-input.error,
.form-textarea.error {
  border-color: var(--error);
}

.form-textarea {
  resize: vertical;
  line-height: 1.6;
}

.input-hint {
  display: flex;
  justify-content: flex-end;
}

.char-count {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.form-error {
  color: var(--error);
  font-size: 0.875rem;
}

/* 图片上传 */
.image-upload-section {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.image-preview-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid var(--gray-300);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.btn-remove-image {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  border-radius: 50%;
  color: var(--white);
  font-size: 1.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-remove-image:hover {
  background: rgba(0, 0, 0, 0.8);
}

.upload-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  border: 2px dashed var(--gray-300);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--gray-100);
}

.upload-btn:hover {
  border-color: var(--primary);
  background: var(--gray-200);
}

.file-input {
  display: none;
}

.upload-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.upload-text {
  color: var(--gray-600);
  font-size: 0.875rem;
}

.upload-hint {
  color: var(--gray-500);
  font-size: 0.875rem;
  margin-top: 0.5rem;
}

/* 表单操作 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.btn {
  padding: 0.875rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel {
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-cancel:hover {
  background: var(--gray-300);
}

.btn-submit {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: var(--white);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.3);
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(107, 70, 193, 0.4);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .header-title {
    font-size: 1.25rem;
  }

  .main-content {
    padding: 1rem;
  }

  .form-wrapper {
    padding: 1.5rem;
  }

  .image-preview-item,
  .upload-btn {
    width: 100px;
    height: 100px;
  }
}
</style>

