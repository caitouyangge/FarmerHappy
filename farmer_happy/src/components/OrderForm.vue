<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">创建订单</h2>
        <button class="close-btn" @click="handleClose">
          <span class="close-icon">×</span>
        </button>
      </div>

      <div class="modal-content">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>提交中...</p>
        </div>

        <div v-else class="order-form">
          <!-- 商品信息 -->
          <div class="product-section">
            <h3 class="section-title">商品信息</h3>
            <div class="product-info-card">
              <div class="product-name">{{ product.title }}</div>
              <div class="product-details">
                <div class="detail-row">
                  <span class="detail-label">单价：</span>
                  <span class="detail-value price">¥{{ product.price || '面议' }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-label">库存：</span>
                  <span class="detail-value">{{ product.stock || 0 }} 斤</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 订单信息表单 -->
          <div class="form-section">
            <h3 class="section-title">订单信息</h3>
            
            <div class="form-group">
              <label class="form-label">
                购买数量 <span class="required">*</span>
              </label>
              <div class="quantity-control">
                <button 
                  class="quantity-btn" 
                  @click="decreaseQuantity"
                  :disabled="formData.quantity <= 1"
                >
                  -
                </button>
                <input
                  v-model.number="formData.quantity"
                  type="number"
                  min="1"
                  :max="product.stock || 1000"
                  class="quantity-input"
                  @input="validateQuantity"
                />
                <button 
                  class="quantity-btn" 
                  @click="increaseQuantity"
                  :disabled="formData.quantity >= (product.stock || 1000)"
                >
                  +
                </button>
                <span class="quantity-unit">斤</span>
              </div>
              <div class="form-hint">
                库存 {{ product.stock || 0 }} 斤，单次最多 1000 斤
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">
                买家姓名 <span class="required">*</span>
              </label>
              <input
                v-model="formData.buyer_name"
                type="text"
                placeholder="请输入买家姓名"
                class="form-input"
                maxlength="50"
              />
            </div>

            <div class="form-group">
              <label class="form-label">
                收货地址 <span class="required">*</span>
              </label>
              <textarea
                v-model="formData.buyer_address"
                placeholder="请输入详细收货地址"
                class="form-textarea"
                rows="3"
                maxlength="200"
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">
                买家手机号 <span class="required">*</span>
              </label>
              <input
                v-model="formData.buyer_phone"
                type="tel"
                placeholder="请输入手机号"
                class="form-input"
                maxlength="11"
              />
            </div>

            <div class="form-group">
              <label class="form-label">订单备注</label>
              <textarea
                v-model="formData.remark"
                placeholder="选填，如有特殊要求请在此说明"
                class="form-textarea"
                rows="2"
                maxlength="200"
              ></textarea>
            </div>
          </div>

          <!-- 订单汇总 -->
          <div class="summary-section">
            <h3 class="section-title">订单汇总</h3>
            <div class="summary-card">
              <div class="summary-row">
                <span class="summary-label">商品单价：</span>
                <span class="summary-value">¥{{ product.price || 0 }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">购买数量：</span>
                <span class="summary-value">{{ formData.quantity }} 斤</span>
              </div>
              <div class="summary-row total">
                <span class="summary-label">订单总额：</span>
                <span class="summary-value total-price">¥{{ totalAmount.toFixed(2) }}</span>
              </div>
              <div class="summary-row balance-row">
                <span class="summary-label">当前余额：</span>
                <span class="summary-value balance-value" :class="{ 'insufficient': !isBalanceSufficient }">
                  <span v-if="balanceLoading">加载中...</span>
                  <span v-else>¥{{ balance.toFixed(2) }}</span>
                </span>
              </div>
              <div v-if="!balanceLoading && !isBalanceSufficient" class="balance-warning">
                <span class="warning-icon">⚠️</span>
                <span class="warning-text">余额不足，还需 ¥{{ Math.abs(remainingBalance).toFixed(2) }}</span>
              </div>
              <div v-else-if="!balanceLoading && isBalanceSufficient" class="balance-info">
                <span class="info-text">支付后余额：¥{{ remainingBalance.toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="error" class="error-message">
            <span class="error-icon">⚠️</span>
            <span class="error-text">{{ error }}</span>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <button class="action-btn cancel-btn" @click="handleClose">
              取消
            </button>
            <button 
              class="action-btn submit-btn" 
              @click="handleSubmit"
              :disabled="!isFormValid || submitting"
            >
              <span v-if="submitting">提交中...</span>
              <span v-else>确认下单</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { orderService } from '../api/order';
import { authService } from '../api/auth';
import logger from '../utils/logger';

export default {
  name: 'OrderForm',
  props: {
    product: {
      type: Object,
      required: true
    }
  },
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const loading = ref(false);
    const submitting = ref(false);
    const error = ref('');
    const balance = ref(0);
    const balanceLoading = ref(false);
    const formData = ref({
      quantity: 1,
      buyer_name: '',
      buyer_address: '',
      buyer_phone: '',
      remark: ''
    });

    // 获取用户信息和余额
    onMounted(async () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          const userInfo = JSON.parse(storedUser);
          // 自动填充用户信息
          if (userInfo.nickname) {
            formData.value.buyer_name = userInfo.nickname;
          }
          if (userInfo.phone) {
            formData.value.buyer_phone = userInfo.phone;
          }
          
          // 如果是买家，获取详细信息并填充收货地址
          if (userInfo.userType === 'buyer' && userInfo.phone) {
            // 尝试从localStorage获取，如果没有则从API获取
            if (userInfo.shippingAddress) {
              formData.value.buyer_address = userInfo.shippingAddress;
            } else {
              try {
                const profile = await authService.getUserProfile(userInfo.phone, 'buyer');
                if (profile && profile.shippingAddress) {
                  formData.value.buyer_address = profile.shippingAddress;
                  // 更新localStorage
                  userInfo.shippingAddress = profile.shippingAddress;
                  localStorage.setItem('user', JSON.stringify(userInfo));
                }
              } catch (err) {
                logger.error('ORDER_FORM', '获取用户详细信息失败', {}, err);
              }
            }
            
            // 获取余额
            balanceLoading.value = true;
            try {
              const userBalance = await authService.getBalance(userInfo.phone, 'buyer');
              balance.value = parseFloat(userBalance) || 0;
              logger.info('ORDER_FORM', '获取余额成功', { balance: balance.value });
            } catch (err) {
              logger.error('ORDER_FORM', '获取余额失败', {}, err);
              balance.value = 0;
            } finally {
              balanceLoading.value = false;
            }
          }
        } catch (err) {
          logger.error('ORDER_FORM', '解析用户信息失败', {}, err);
        }
      }
    });

    // 计算订单总额
    const totalAmount = computed(() => {
      const price = parseFloat(props.product.price) || 0;
      const quantity = formData.value.quantity || 1;
      return price * quantity;
    });

    // 计算支付后余额
    const remainingBalance = computed(() => {
      return balance.value - totalAmount.value;
    });

    // 余额是否充足
    const isBalanceSufficient = computed(() => {
      return balance.value >= totalAmount.value;
    });

    // 表单验证
    const isFormValid = computed(() => {
      const maxQuantity = props.product.stock || 999999;
      return (
        formData.value.quantity > 0 &&
        formData.value.quantity <= maxQuantity &&
        formData.value.buyer_name.trim() !== '' &&
        formData.value.buyer_address.trim() !== '' &&
        formData.value.buyer_phone.trim() !== '' &&
        /^1[3-9]\d{9}$/.test(formData.value.buyer_phone)
      );
    });

    // 增加数量
    const increaseQuantity = () => {
      const maxQuantity = props.product.stock || 999999;
      if (formData.value.quantity < maxQuantity) {
        formData.value.quantity++;
        error.value = '';
      }
    };

    // 减少数量
    const decreaseQuantity = () => {
      if (formData.value.quantity > 1) {
        formData.value.quantity--;
        error.value = '';
      }
    };

    // 验证数量
    const validateQuantity = () => {
      const maxQuantity = props.product.stock || 999999;
      if (formData.value.quantity < 1) {
        formData.value.quantity = 1;
      } else if (formData.value.quantity > maxQuantity) {
        formData.value.quantity = maxQuantity;
        error.value = `最多可购买 ${maxQuantity} 斤`;
      } else {
        error.value = '';
      }
    };

    // 关闭弹窗
    const handleClose = () => {
      logger.userAction('ORDER_FORM_CLOSE', { productId: props.product.product_id });
      emit('close');
    };

    // 点击遮罩层关闭
    const handleOverlayClick = (event) => {
      if (event.target === event.currentTarget) {
        handleClose();
      }
    };

    // 提交订单
    const handleSubmit = async () => {
      if (!isFormValid.value) {
        error.value = '请填写完整的订单信息';
        return;
      }

      submitting.value = true;
      error.value = '';

      try {
        logger.userAction('ORDER_FORM_SUBMIT', {
          productId: props.product.product_id,
          quantity: formData.value.quantity
        });

        const orderData = {
          product_id: String(props.product.product_id),
          quantity: formData.value.quantity,
          buyer_name: formData.value.buyer_name.trim(),
          buyer_address: formData.value.buyer_address.trim(),
          buyer_phone: formData.value.buyer_phone.trim(),
          remark: formData.value.remark.trim() || undefined
        };

        const response = await orderService.createOrder(orderData);

        logger.info('ORDER_FORM', '订单创建成功', {
          orderId: response.data?.order_id,
          productId: props.product.product_id
        });

        // 显示成功消息
        alert(`订单创建成功！\n订单号：${response.data?.order_id || '未知'}\n订单总额：¥${totalAmount.value.toFixed(2)}`);

        // 触发成功事件
        emit('success', response.data);

        // 关闭弹窗
        handleClose();
      } catch (err) {
        logger.error('ORDER_FORM', '订单创建失败', {
          productId: props.product.product_id,
          errorMessage: err.message || err,
          errors: err.errors || []
        }, err);

        // 显示错误信息
        if (err.errors && Array.isArray(err.errors) && err.errors.length > 0) {
          // 如果有详细的字段错误，显示所有错误
          const errorMessages = err.errors.map(e => {
            const fieldName = {
              'product_id': '商品ID',
              'quantity': '购买数量',
              'buyer_name': '买家姓名',
              'buyer_address': '收货地址',
              'buyer_phone': '买家手机号',
              'remark': '订单备注'
            }[e.field] || e.field;
            return `${fieldName}：${e.message}`;
          });
          error.value = errorMessages.join('\n');
        } else if (err.code && err.message) {
          error.value = err.message;
        } else if (typeof err === 'string') {
          error.value = err;
        } else {
          error.value = '创建订单失败，请稍后重试';
        }
      } finally {
        submitting.value = false;
      }
    };

    return {
      loading,
      submitting,
      error,
      formData,
      balance,
      balanceLoading,
      totalAmount,
      remainingBalance,
      isBalanceSufficient,
      isFormValid,
      increaseQuantity,
      decreaseQuantity,
      validateQuantity,
      handleClose,
      handleOverlayClick,
      handleSubmit
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
  max-width: 600px;
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

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
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

.order-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--primary-light);
}

/* 商品信息 */
.product-section {
  margin-bottom: 0.5rem;
}

.product-info-card {
  background: var(--gray-100);
  border-radius: 12px;
  padding: 1.5rem;
}

.product-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 1rem;
}

.product-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.detail-value {
  font-size: 0.875rem;
  color: #1a202c;
  font-weight: 500;
}

.detail-value.price {
  font-size: 1.125rem;
  color: var(--primary);
  font-weight: 700;
}

/* 表单 */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #1a202c;
}

.required {
  color: var(--error);
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.quantity-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--gray-300);
  background: var(--white);
  border-radius: 6px;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--gray-600);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quantity-btn:hover:not(:disabled) {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
}

.quantity-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.quantity-input {
  width: 80px;
  height: 36px;
  padding: 0 0.5rem;
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  text-align: center;
  font-size: 0.875rem;
  font-weight: 500;
  /* 隐藏number输入框的默认加减按钮 */
  appearance: textfield;
  -moz-appearance: textfield;
}

.quantity-input::-webkit-outer-spin-button,
.quantity-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.quantity-input:focus {
  outline: none;
  border-color: var(--primary);
}

.quantity-unit {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.form-input,
.form-textarea {
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary);
}

.form-textarea {
  resize: vertical;
  min-height: 60px;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--gray-400);
}

/* 订单汇总 */
.summary-section {
  margin-top: 0.5rem;
}

.summary-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-label {
  font-size: 0.875rem;
  color: var(--gray-600);
}

.summary-value {
  font-size: 0.875rem;
  color: #1a202c;
  font-weight: 500;
}

.summary-row.total {
  padding-top: 0.75rem;
  border-top: 2px solid rgba(107, 70, 193, 0.2);
  margin-top: 0.25rem;
}

.summary-row.total .summary-label {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
}

.total-price {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary);
}

.balance-row {
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(107, 70, 193, 0.1);
}

.balance-value {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
}

.balance-value.insufficient {
  color: var(--error);
}

.balance-warning {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding: 0.75rem;
  background: #fee;
  border: 1px solid var(--error);
  border-radius: 8px;
  color: var(--error);
  font-size: 0.875rem;
}

.warning-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.warning-text {
  flex: 1;
}

.balance-info {
  margin-top: 0.75rem;
  padding: 0.5rem;
  text-align: center;
  font-size: 0.875rem;
  color: var(--gray-600);
}

.info-text {
  color: var(--gray-500);
}

/* 错误提示 */
.error-message {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #fee;
  border: 1px solid var(--error);
  border-radius: 8px;
  color: var(--error);
  font-size: 0.875rem;
}

.error-icon {
  font-size: 1rem;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.error-text {
  flex: 1;
  white-space: pre-line;
  word-break: break-word;
}

/* 操作按钮 */
.action-section {
  display: flex;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.action-btn {
  flex: 1;
  padding: 0.875rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: var(--gray-200);
  color: var(--gray-600);
}

.cancel-btn:hover {
  background: var(--gray-300);
}

.submit-btn {
  background: var(--primary);
  color: var(--white);
}

.submit-btn:hover:not(:disabled) {
  background: var(--primary-dark);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-container {
    max-height: 95vh;
  }

  .modal-header {
    padding: 1rem;
  }

  .modal-content {
    padding: 1.5rem;
  }

  .action-section {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }
}
</style>

