<template>
  <div class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">批量操作</h2>
        <button class="close-btn" @click="handleClose">
          <span class="close-icon">×</span>
        </button>
      </div>

      <div class="modal-content">
        <div class="selection-info">
          <p class="info-text">已选择 <span class="count">{{ selectedCount }}</span> 个产品</p>
        </div>

        <div class="action-section">
          <h3 class="section-title">选择操作</h3>
          <div class="action-list">
            <label class="action-item">
              <input
                v-model="selectedAction"
                type="radio"
                value="on_shelf"
                class="radio-input"
              />
              <div class="action-content">
                <div class="action-icon">📤</div>
                <div class="action-text">
                  <div class="action-name">批量上架</div>
                  <div class="action-desc">将选中的产品上架销售</div>
                </div>
              </div>
            </label>

            <label class="action-item">
              <input
                v-model="selectedAction"
                type="radio"
                value="off_shelf"
                class="radio-input"
              />
              <div class="action-content">
                <div class="action-icon">📥</div>
                <div class="action-text">
                  <div class="action-name">批量下架</div>
                  <div class="action-desc">将选中的产品下架</div>
                </div>
              </div>
            </label>

            <label class="action-item">
              <input
                v-model="selectedAction"
                type="radio"
                value="delete"
                class="radio-input"
              />
              <div class="action-content">
                <div class="action-icon">🗑️</div>
                <div class="action-text">
                  <div class="action-name">批量删除</div>
                  <div class="action-desc">删除选中的产品（不可恢复）</div>
                </div>
              </div>
            </label>
          </div>
        </div>

        <div class="warning-section" v-if="selectedAction === 'delete'">
          <div class="warning-icon">⚠️</div>
          <div class="warning-text">
            <h4>警告</h4>
            <p>删除操作不可恢复，请确认您要删除选中的产品。</p>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn-cancel" @click="handleClose">
          取消
        </button>
        <button 
          type="button" 
          class="btn-confirm" 
          @click="handleConfirm"
          :disabled="!selectedAction"
        >
          确认执行
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import logger from '../utils/logger';

export default {
  name: 'BatchActionModal',
  props: {
    selectedCount: {
      type: Number,
      required: true
    }
  },
  emits: ['close', 'confirm'],
  setup(props, { emit }) {
    const selectedAction = ref('');

    // 关闭弹窗
    const handleClose = () => {
      logger.userAction('BATCH_MODAL_CLOSE', { count: props.selectedCount });
      emit('close');
    };

    // 点击遮罩层关闭
    const handleOverlayClick = (event) => {
      if (event.target === event.currentTarget) {
        handleClose();
      }
    };

    // 确认操作
    const handleConfirm = () => {
      if (!selectedAction.value) {
        return;
      }

      logger.userAction('BATCH_MODAL_CONFIRM', { 
        action: selectedAction.value,
        count: props.selectedCount 
      });
      
      emit('confirm', selectedAction.value);
    };

    return {
      selectedAction,
      handleClose,
      handleOverlayClick,
      handleConfirm
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
  max-width: 500px;
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
  padding: 2rem;
}

.selection-info {
  text-align: center;
  margin-bottom: 2rem;
  padding: 1rem;
  background: var(--gray-100);
  border-radius: 8px;
}

.info-text {
  font-size: 1rem;
  color: var(--gray-600);
  margin: 0;
}

.count {
  font-weight: 600;
  color: var(--primary);
  font-size: 1.125rem;
}

.action-section {
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 1rem 0;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-item:hover {
  border-color: var(--primary-light);
  background: rgba(107, 70, 193, 0.05);
}

.action-item:has(.radio-input:checked) {
  border-color: var(--primary);
  background: rgba(107, 70, 193, 0.1);
}

.action-content {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.action-icon {
  font-size: 2rem;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gray-100);
  border-radius: 50%;
}

.action-text {
  flex: 1;
}

.action-name {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 0.25rem;
}

.action-desc {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.radio-input {
  accent-color: var(--primary);
  margin-right: 0.75rem;
}

.warning-section {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.warning-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.warning-text h4 {
  font-size: 0.875rem;
  font-weight: 600;
  color: #dc2626;
  margin: 0 0 0.25rem 0;
}

.warning-text p {
  font-size: 0.875rem;
  color: #7f1d1d;
  margin: 0;
  line-height: 1.4;
}

.modal-footer {
  padding: 1.5rem 2rem;
  border-top: 1px solid var(--gray-200);
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn-cancel, .btn-confirm {
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

.btn-confirm {
  background: var(--primary);
  color: var(--white);
}

.btn-confirm:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-container {
    max-height: 90vh;
  }

  .modal-header {
    padding: 1rem;
  }

  .modal-content {
    padding: 1rem;
  }

  .modal-footer {
    padding: 1rem;
    flex-direction: column;
  }

  .btn-cancel, .btn-confirm {
    width: 100%;
  }
}
</style>
