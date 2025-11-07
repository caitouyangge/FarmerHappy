<template>
  <div class="product-card" :class="{ 'selected': selected }">
    <!-- 选择框 -->
    <div class="card-checkbox" v-if="isFarmer">
      <input
        type="checkbox"
        :checked="selected"
        @change="handleSelect"
        class="checkbox-input"
      />
    </div>

    <!-- 产品图片 -->
    <div class="product-image">
      <div class="image-placeholder">
        <span class="image-icon">🌾</span>
      </div>
    </div>

    <!-- 产品信息 -->
    <div class="product-info">
      <h3 class="product-title" @click="handleView">{{ product.title }}</h3>
      
      <div class="product-meta">
        <div class="meta-item">
          <span class="meta-label">价格:</span>
          <span class="meta-value price">¥{{ product.price || '面议' }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">库存:</span>
          <span class="meta-value">{{ product.stock || 0 }} 斤</span>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="product-actions">
      <button class="action-btn view-btn" @click="handleView">
        <span class="btn-icon">👁</span>
        查看
      </button>
      
      <!-- 农户操作 -->
      <template v-if="isFarmer">
        <button class="action-btn edit-btn" @click="handleEdit">
          <span class="btn-icon">✏️</span>
          编辑
        </button>
        
        <button 
          v-if="product.status === 'draft' || product.status === 'off_shelf'"
          class="action-btn on-shelf-btn" 
          @click="handleOnShelf"
        >
          <span class="btn-icon">📤</span>
          上架
        </button>
        
        <button 
          v-if="product.status === 'on_shelf'"
          class="action-btn off-shelf-btn" 
          @click="handleOffShelf"
        >
          <span class="btn-icon">📥</span>
          下架
        </button>
        
        <button class="action-btn delete-btn" @click="handleDelete">
          <span class="btn-icon">🗑️</span>
          删除
        </button>
      </template>
      
      <!-- 买家操作 -->
      <template v-else>
        <button 
          v-if="product.status === 'on_shelf' && product.stock > 0"
          class="action-btn purchase-btn" 
          @click="handlePurchase"
        >
          <span class="btn-icon">🛒</span>
          购买
        </button>
        
        <button 
          v-else
          class="action-btn disabled-btn" 
          disabled
          :title="getPurchaseDisabledReason"
        >
          <span class="btn-icon">🛒</span>
          {{ getPurchaseButtonText }}
        </button>
      </template>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue';
import logger from '../utils/logger';

export default {
  name: 'ProductCard',
  props: {
    product: {
      type: Object,
      required: true
    },
    isFarmer: {
      type: Boolean,
      default: false
    },
    selected: {
      type: Boolean,
      default: false
    }
  },
  emits: ['select', 'view', 'edit', 'delete', 'on-shelf', 'off-shelf', 'purchase'],
  setup(props, { emit }) {

    // 分类文本
    const categoryText = computed(() => {
      const categoryMap = {
        vegetables: '蔬菜',
        fruits: '水果',
        grains: '粮食',
        livestock: '肉类',
        aquatic: '水产'
      };
      return categoryMap[props.product.category] || props.product.category || '其他';
    });

    // 获取购买按钮文本
    const getPurchaseButtonText = computed(() => {
      if (props.product.status === 'sold_out' || props.product.stock === 0) {
        return '已售罄';
      }
      if (props.product.status === 'off_shelf') {
        return '已下架';
      }
      if (props.product.status === 'draft') {
        return '未上架';
      }
      return '暂不可购买';
    });

    // 获取购买禁用原因
    const getPurchaseDisabledReason = computed(() => {
      if (props.product.status === 'sold_out' || props.product.stock === 0) {
        return '商品已售罄';
      }
      if (props.product.status === 'off_shelf') {
        return '商品已下架';
      }
      if (props.product.status === 'draft') {
        return '商品未上架';
      }
      return '商品暂不可购买';
    });

    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '无';
      try {
        const date = new Date(dateString);
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        });
      } catch (error) {
        return '无';
      }
    };

    // 处理选择
    const handleSelect = (event) => {
      logger.userAction('PRODUCT_SELECT', { 
        productId: props.product.product_id,
        selected: event.target.checked 
      });
      emit('select', props.product.product_id, event.target.checked);
    };

    // 处理查看
    const handleView = () => {
      logger.userAction('PRODUCT_VIEW', { productId: props.product.product_id });
      emit('view', props.product.product_id);
    };

    // 处理编辑
    const handleEdit = () => {
      logger.userAction('PRODUCT_EDIT', { productId: props.product.product_id });
      emit('edit', props.product);
    };

    // 处理删除
    const handleDelete = () => {
      logger.userAction('PRODUCT_DELETE', { productId: props.product.product_id });
      emit('delete', props.product.product_id);
    };

    // 处理上架
    const handleOnShelf = () => {
      logger.userAction('PRODUCT_ON_SHELF', { productId: props.product.product_id });
      emit('on-shelf', props.product.product_id);
    };

    // 处理下架
    const handleOffShelf = () => {
      logger.userAction('PRODUCT_OFF_SHELF', { productId: props.product.product_id });
      emit('off-shelf', props.product.product_id);
    };

    // 处理购买
    const handlePurchase = () => {
      logger.userAction('PRODUCT_PURCHASE', { productId: props.product.product_id });
      emit('purchase', props.product);
    };

    return {
      categoryText,
      formatDate,
      getPurchaseButtonText,
      getPurchaseDisabledReason,
      handleSelect,
      handleView,
      handleEdit,
      handleDelete,
      handleOnShelf,
      handleOffShelf,
      handlePurchase
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.product-card {
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  overflow: hidden;
  transition: all 0.3s;
  position: relative;
  border: 2px solid transparent;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(107, 70, 193, 0.15);
}

.product-card.selected {
  border-color: var(--primary);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.2);
}

/* 选择框 */
.card-checkbox {
  position: absolute;
  top: 1rem;
  left: 1rem;
  z-index: 10;
}

.checkbox-input {
  width: 18px;
  height: 18px;
  accent-color: var(--primary);
  cursor: pointer;
}

/* 产品图片 */
.product-image {
  position: relative;
  height: 200px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: rgba(107, 70, 193, 0.1);
  border-radius: 50%;
}

.image-icon {
  font-size: 2.5rem;
}

.status-badge {
  position: absolute;
  top: 1rem;
  right: 1rem;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--white);
}

.status-draft {
  background: var(--gray-500);
}

.status-on-shelf {
  background: var(--success);
}

.status-off-shelf {
  background: #f59e0b;
}

.status-sold-out {
  background: var(--error);
}

/* 产品信息 */
.product-info {
  padding: 1.5rem;
}

.product-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.5rem 0;
  cursor: pointer;
  transition: color 0.2s;
}

.product-title:hover {
  color: var(--primary);
}

.product-category {
  font-size: 0.875rem;
  color: var(--primary);
  font-weight: 500;
  margin: 0 0 0.75rem 0;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-label {
  font-size: 0.75rem;
  color: var(--gray-400);
}

.meta-value {
  font-size: 0.875rem;
  color: #1a202c;
  font-weight: 500;
}

.price {
  color: var(--primary);
  font-weight: 600;
  font-size: 1rem;
}

/* 操作按钮 */
.product-actions {
  padding: 1rem 1.5rem;
  background: var(--gray-100);
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex: 1;
  justify-content: center;
  min-width: 60px;
}

.btn-icon {
  font-size: 0.875rem;
}

.view-btn {
  background: var(--primary);
  color: var(--white);
}

.view-btn:hover {
  background: var(--primary-dark);
}

.edit-btn {
  background: #3b82f6;
  color: var(--white);
}

.edit-btn:hover {
  background: #2563eb;
}

.on-shelf-btn {
  background: var(--success);
  color: var(--white);
}

.on-shelf-btn:hover {
  background: #38a169;
}

.off-shelf-btn {
  background: #f59e0b;
  color: var(--white);
}

.off-shelf-btn:hover {
  background: #d97706;
}

.delete-btn {
  background: var(--error);
  color: var(--white);
}

.delete-btn:hover {
  background: #c53030;
}

.purchase-btn {
  background: var(--primary);
  color: var(--white);
}

.purchase-btn:hover {
  background: var(--primary-dark);
}

.disabled-btn {
  background: var(--gray-300);
  color: var(--gray-500);
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .product-actions {
    flex-direction: column;
  }

  .action-btn {
    flex: none;
    width: 100%;
  }

  .product-info {
    padding: 1rem;
  }

  .product-actions {
    padding: 1rem;
  }
}
</style>
