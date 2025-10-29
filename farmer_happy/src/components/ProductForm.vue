<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">{{ isEdit ? '编辑农产品' : '发布农产品' }}</h2>
        <button class="close-btn" @click="handleClose">
          <span class="close-icon">×</span>
        </button>
      </div>

      <form @submit.prevent="handleSubmit" class="modal-form">
        <div class="form-content">
          <!-- 基本信息 -->
          <div class="form-section">
            <h3 class="section-title">基本信息</h3>
            
            <div class="form-group">
              <label class="form-label required">产品标题</label>
              <input
                v-model="form.title"
                type="text"
                class="form-input"
                :class="{ 'error': errors.title }"
                placeholder="请输入产品标题"
                maxlength="100"
              />
              <span v-if="errors.title" class="form-error">{{ errors.title }}</span>
            </div>

             <div class="form-group">
               <label class="form-label required">产品分类</label>
               <select
                 v-model="form.category"
                 class="form-input"
                 :class="{ 'error': errors.category }"
               >
                 <option value="">请选择产品分类</option>
                 <option value="vegetables">蔬菜</option>
                 <option value="fruits">水果</option>
                 <option value="grains">粮食</option>
               </select>
               <span v-if="errors.category" class="form-error">{{ errors.category }}</span>
             </div>

          </div>

          <!-- 价格和库存 -->
          <div class="form-section">
            <h3 class="section-title">价格和库存</h3>
            
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">价格 (元/斤)</label>
                <input
                  v-model="form.price"
                  type="number"
                  class="form-input"
                  :class="{ 'error': errors.price }"
                  placeholder="0.00"
                  step="0.01"
                  min="0"
                />
                <span v-if="errors.price" class="form-error">{{ errors.price }}</span>
              </div>

              <div class="form-group">
                <label class="form-label">库存数量(斤)</label>
                <input
                  v-model="form.stock"
                  type="number"
                  class="form-input"
                  :class="{ 'error': errors.stock }"
                  placeholder="0"
                  min="0"
                />
                <span v-if="errors.stock" class="form-error">{{ errors.stock }}</span>
              </div>
            </div>

          </div>

          <!-- 产品详细介绍 -->
          <div class="form-section">
            <h3 class="section-title">产品详细介绍</h3>
            
             <div class="form-group">
               <label class="form-label required">商品详细介绍</label>
               <textarea
                 v-model="form.detailedDescription"
                 class="form-textarea"
                 :class="{ 'error': errors.detailedDescription }"
                 placeholder="请详细描述商品的规格、重量、尺寸等具体信息"
                 rows="3"
                 maxlength="200"
               ></textarea>
               <span v-if="errors.detailedDescription" class="form-error">{{ errors.detailedDescription }}</span>
             </div>
          </div>

        </div>

        <div class="modal-footer">
          <button type="button" class="btn-cancel" @click="handleClose">
            取消
          </button>
          <button type="submit" class="btn-submit" :disabled="loading">
            {{ loading ? '保存中...' : (isEdit ? '更新' : '发布') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { productService } from '../api/product';
import logger from '../utils/logger';

export default {
  name: 'ProductForm',
  props: {
    product: {
      type: Object,
      default: null
    },
    isEdit: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const loading = ref(false);
    const userInfo = ref({});

     // 表单数据
     const form = reactive({
       title: '',
       category: '',
       price: '',
       stock: '',
       detailedDescription: ''
     });

     // 表单验证错误
     const errors = reactive({
       title: '',
       category: '',
       price: '',
       stock: '',
       detailedDescription: ''
     });

    // 获取用户信息
    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
        } catch (error) {
          logger.error('PRODUCT_FORM', '解析用户信息失败', {}, error);
        }
      }

       // 如果是编辑模式，填充表单数据
       if (props.isEdit && props.product) {
         Object.assign(form, {
           title: props.product.title || '',
           category: props.product.category || '',
           price: props.product.price || '',
           stock: props.product.stock || '',
           detailedDescription: props.product.detailed_description || ''
         });
       }
    });

    // 表单验证
    const validateForm = () => {
      logger.debug('PRODUCT_FORM', '开始验证表单');
      let isValid = true;
      
      // 清空错误信息
      Object.keys(errors).forEach(key => {
        errors[key] = '';
      });

      // 验证必填字段
      if (!form.title.trim()) {
        errors.title = '请输入产品标题';
        isValid = false;
      } else if (form.title.length > 100) {
        errors.title = '产品标题不能超过100个字符';
        isValid = false;
      }

      if (!form.category) {
        errors.category = '请选择产品分类';
        isValid = false;
      }

      // 验证价格
      if (!form.price || isNaN(form.price) || form.price <= 0) {
        errors.price = '请输入有效的价格';
        isValid = false;
      }

      // 验证库存
      if (form.stock === '' || isNaN(form.stock) || form.stock < 0) {
        errors.stock = '请输入有效的库存数量';
        isValid = false;
      }

       // 验证详细介绍
       if (!form.detailedDescription.trim()) {
         errors.detailedDescription = '请输入商品详细介绍';
         isValid = false;
       } else if (form.detailedDescription.length > 200) {
         errors.detailedDescription = '详细介绍不能超过200个字符';
         isValid = false;
       }

       logger.validation('ProductForm', isValid, errors);
       return isValid;
    };

    // 提交表单
    const handleSubmit = async () => {
      logger.userAction('PRODUCT_FORM_SUBMIT', { 
        isEdit: props.isEdit,
        productId: props.product?.product_id 
      });

      if (!validateForm()) {
        logger.warn('PRODUCT_FORM', '表单验证失败');
        return;
      }

      // 检查用户信息
      if (!userInfo.value.phone) {
        logger.error('PRODUCT_FORM', '用户手机号不存在', { userInfo: userInfo.value });
        alert('用户信息不完整，请重新登录');
        return;
      }

      loading.value = true;
      try {
         // 准备提交数据
         const submitData = {
           title: form.title.trim(),
           category: form.category,
           price: parseFloat(form.price),
           stock: parseInt(form.stock),
           detailed_description: form.detailedDescription.trim(),
           phone: userInfo.value.phone
         };

         // 调试日志
         logger.debug('PRODUCT_FORM', '用户信息', { 
           userInfo: userInfo.value,
           phone: userInfo.value.phone 
         });

        logger.info('PRODUCT_FORM', '开始提交产品数据', { 
          isEdit: props.isEdit,
          data: submitData 
        });

        let response;
        if (props.isEdit) {
          response = await productService.updateProduct(props.product.product_id, submitData);
        } else {
          response = await productService.createProduct(submitData);
        }

        logger.info('PRODUCT_FORM', '产品操作成功', { 
          isEdit: props.isEdit,
          productId: response.data?.product_id 
        });

        emit('success');
      } catch (error) {
        logger.error('PRODUCT_FORM', '产品操作失败', {
          isEdit: props.isEdit,
          errorMessage: error.message || error
        }, error);

        // 处理验证错误
        if (error.errors && Array.isArray(error.errors)) {
          error.errors.forEach(err => {
            if (err.field && Object.prototype.hasOwnProperty.call(errors, err.field)) {
              errors[err.field] = err.message;
            }
          });
        } else {
          alert('操作失败：' + (error.message || error));
        }
      } finally {
        loading.value = false;
      }
    };

    // 关闭弹窗
    const handleClose = () => {
      logger.userAction('PRODUCT_FORM_CLOSE', { isEdit: props.isEdit });
      emit('close');
    };

    // 点击遮罩层关闭
    const handleOverlayClick = (event) => {
      if (event.target === event.currentTarget) {
        handleClose();
      }
    };

    return {
      loading,
      form,
      errors,
      handleSubmit,
      handleClose,
      handleOverlayClick
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

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
  padding: 1rem;
}

.modal-container {
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--gray-200);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.close-btn:hover {
  background: var(--gray-100);
}

.close-icon {
  font-size: 1.5rem;
  color: var(--gray-500);
}

.modal-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.form-content {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
}

.form-section {
  margin-bottom: 2rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-light);
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--gray-600);
  font-size: 0.875rem;
  font-weight: 500;
}

.form-label.required::after {
  content: ' *';
  color: var(--error);
}

.form-input, .form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
  transition: all 0.2s;
  background: var(--white);
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.form-input.error, .form-textarea.error {
  border-color: var(--error);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

 .form-error {
   display: block;
   color: var(--error);
   font-size: 0.75rem;
   margin-top: 0.25rem;
 }

 .unit-display {
   display: flex;
   align-items: center;
   gap: 0.5rem;
   padding: 0.75rem;
   background: var(--gray-100);
   border-radius: 8px;
   border: 1px solid var(--gray-300);
 }

 .unit-text {
   font-size: 0.875rem;
   font-weight: 500;
   color: #1a202c;
 }

 .unit-note {
   font-size: 0.75rem;
   color: var(--gray-500);
 }

.radio-group {
  display: flex;
  gap: 1rem;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.radio-input {
  accent-color: var(--primary);
}

.radio-label {
  font-size: 0.875rem;
  color: var(--gray-600);
}

.modal-footer {
  padding: 1.5rem 2rem;
  border-top: 1px solid var(--gray-200);
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn-cancel, .btn-submit {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-cancel:hover {
  background: var(--gray-300);
}

.btn-submit {
  background: var(--primary);
  color: var(--white);
}

.btn-submit:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-container {
    max-height: 95vh;
  }

  .modal-header {
    padding: 1rem;
  }

  .form-content {
    padding: 1rem;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .modal-footer {
    padding: 1rem;
    flex-direction: column;
  }

  .btn-cancel, .btn-submit {
    width: 100%;
  }
}
</style>
