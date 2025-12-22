<template>
  <div class="skeleton" :class="[`skeleton-${type}`, { 'skeleton-animated': animated }]">
    <!-- 广告轮播骨架屏 -->
    <div v-if="type === 'ad-carousel'" class="skeleton-ad-carousel">
      <div class="skeleton-ad-image"></div>
      <div class="skeleton-ad-content">
        <div class="skeleton-ad-tag"></div>
        <div class="skeleton-ad-title"></div>
        <div class="skeleton-ad-title-short"></div>
        <div class="skeleton-ad-description"></div>
        <div class="skeleton-ad-description"></div>
        <div class="skeleton-ad-price"></div>
      </div>
    </div>

    <!-- 商品卡片骨架屏 -->
    <div v-else-if="type === 'product-card'" class="skeleton-product-card">
      <div class="skeleton-product-image"></div>
      <div class="skeleton-product-content">
        <div class="skeleton-product-title"></div>
        <div class="skeleton-product-title-short"></div>
        <div class="skeleton-product-description"></div>
        <div class="skeleton-product-footer">
          <div class="skeleton-product-price"></div>
          <div class="skeleton-product-button"></div>
        </div>
      </div>
    </div>

    <!-- 商品列表骨架屏 -->
    <div v-else-if="type === 'product-list'" class="skeleton-product-list">
      <div
        v-for="i in count"
        :key="i"
        class="skeleton-product-card"
      >
        <div class="skeleton-product-image"></div>
        <div class="skeleton-product-content">
          <div class="skeleton-product-title"></div>
          <div class="skeleton-product-title-short"></div>
          <div class="skeleton-product-description"></div>
          <div class="skeleton-product-footer">
            <div class="skeleton-product-price"></div>
            <div class="skeleton-product-button"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 通用文本骨架屏 -->
    <div v-else-if="type === 'text'" class="skeleton-text" :style="{ width: width }">
      <span></span>
    </div>

    <!-- 通用矩形骨架屏 -->
    <div v-else-if="type === 'rect'" class="skeleton-rect" :style="{ width: width, height: height }"></div>

    <!-- 通用圆形骨架屏 -->
    <div v-else-if="type === 'circle'" class="skeleton-circle" :style="{ width: width, height: height }"></div>

    <!-- 默认骨架屏 -->
    <div v-else class="skeleton-default" :style="{ width: width, height: height }"></div>
  </div>
</template>

<script>
export default {
  name: 'Skeleton',
  props: {
    // 骨架屏类型：ad-carousel, product-card, product-list, text, rect, circle
    type: {
      type: String,
      default: 'rect'
    },
    // 宽度
    width: {
      type: String,
      default: '100%'
    },
    // 高度
    height: {
      type: String,
      default: '20px'
    },
    // 是否显示动画
    animated: {
      type: Boolean,
      default: true
    },
    // 数量（用于列表类型）
    count: {
      type: Number,
      default: 6
    }
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.skeleton {
  position: relative;
  overflow: hidden;
}

/* 动画效果 */
.skeleton-animated::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.6),
    transparent
  );
  animation: skeleton-loading 1.5s infinite;
  z-index: 1;
}

@keyframes skeleton-loading {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

/* 基础骨架屏样式 */
.skeleton-default,
.skeleton-rect {
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-md);
}

.skeleton-circle {
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: 50%;
}

.skeleton-text {
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
  height: 1em;
}

.skeleton-text span {
  display: inline-block;
  width: 100%;
  height: 100%;
}

/* 广告轮播骨架屏 */
.skeleton-ad-carousel {
  display: flex;
  width: 100%;
  height: 210px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  position: relative;
}

.skeleton-ad-image {
  flex: 1.2;
  height: 100%;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.1) 25%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.1) 75%
  );
  background-size: 200% 100%;
}

.skeleton-ad-content {
  flex: 1;
  padding: var(--spacing-6);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.95) 100%);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  justify-content: center;
}

.skeleton-ad-tag {
  width: 80px;
  height: 24px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-md);
}

.skeleton-ad-title {
  width: 70%;
  height: 28px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-ad-title-short {
  width: 50%;
  height: 28px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-ad-description {
  width: 100%;
  height: 16px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-ad-price {
  width: 120px;
  height: 36px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

/* 商品卡片骨架屏 */
.skeleton-product-card {
  background: var(--white);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(107, 70, 193, 0.08);
}

.skeleton-product-image {
  width: 100%;
  height: 200px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
}

.skeleton-product-content {
  padding: var(--spacing-4);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.skeleton-product-title {
  width: 80%;
  height: 20px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-product-title-short {
  width: 60%;
  height: 20px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-product-description {
  width: 100%;
  height: 14px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-2);
}

.skeleton-product-price {
  width: 80px;
  height: 24px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
}

.skeleton-product-button {
  width: 60px;
  height: 32px;
  background: linear-gradient(
    90deg,
    var(--gray-200) 25%,
    var(--gray-100) 50%,
    var(--gray-200) 75%
  );
  background-size: 200% 100%;
  border-radius: var(--radius-md);
}

/* 商品列表骨架屏 */
.skeleton-product-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-5);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .skeleton-product-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .skeleton-product-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .skeleton-ad-carousel {
    flex-direction: column;
    height: 200px;
  }

  .skeleton-ad-image {
    height: 50%;
  }

  .skeleton-ad-content {
    height: 50%;
    padding: var(--spacing-4);
  }

  .skeleton-product-list {
    grid-template-columns: 1fr;
  }
}
</style>

