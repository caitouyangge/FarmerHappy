<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <!-- 左侧用户信息 -->
      <div class="user-info">
        <div class="avatar-wrapper">
          <div class="avatar">{{ userInitial }}</div>
          <div class="avatar-ring"></div>
        </div>
        <div class="user-details">
          <div class="user-name-row">
            <span class="user-name">{{ userInfo.nickname || '用户' }}</span>
            <span class="user-role-badge">{{ userRoleText }}</span>
          </div>
          <div class="user-meta">
            <span class="user-phone">{{ userInfo.phone }}</span>
            <span v-if="shouldShowBalance" class="user-balance">
              <span class="balance-label">余额</span>
              <span v-if="loadingBalance" class="balance-loading">加载中...</span>
              <span v-else class="balance-amount">¥{{ formattedBalance }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧登出按钮 -->
      <button class="btn-logout" @click="handleLogout">
        <svg class="logout-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
          <polyline points="16 17 21 12 16 7"></polyline>
          <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
        <span>登出</span>
      </button>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 欢迎标题 -->
        <div class="welcome-section">
          <div class="welcome-content">
            <div class="welcome-greeting">
              <span class="greeting-icon">👋</span>
              <h1 class="welcome-title">{{ welcomeMessage }}</h1>
            </div>
            <p class="welcome-subtitle">{{ subtitleMessage }}</p>
          </div>
          <div class="welcome-decoration">
            <div class="decoration-circle circle-1"></div>
            <div class="decoration-circle circle-2"></div>
            <div class="decoration-circle circle-3"></div>
          </div>
        </div>

        <!-- 广告轮播区域（仅买家显示） -->
        <div v-if="userInfo.userType === 'buyer'" class="ad-banner-section">
          <div class="ad-section-header">
            <div class="ad-header-left">
              <span class="ad-badge">🔥</span>
              <h2 class="ad-section-title">热门推荐</h2>
            </div>
            <span class="ad-subtitle">发现优质农产品</span>
          </div>
          <Skeleton v-if="loadingAds" type="ad-carousel" />
          <div v-else-if="adProducts.length > 0" class="ad-carousel">
            <div class="carousel-container" @click="handleAdClick(currentAdIndex)">
              <div class="carousel-slide" :style="{ transform: `translateX(-${currentAdIndex * 100}%)` }">
                <div
                  v-for="(product, index) in adProducts"
                  :key="product.product_id"
                  class="ad-slide"
                  :class="{ active: index === currentAdIndex }"
                >
                  <div class="ad-badge-hot">热门</div>
                  <div v-if="getProductImages(product).length > 0" class="ad-image-container">
                    <div class="ad-images-slider" :style="{ transform: `translateX(-${getCurrentImageIndex(index) * 100}%)` }">
                      <div
                        v-for="(img, imgIndex) in getProductImages(product)"
                        :key="`${product.product_id}-${imgIndex}`"
                        class="ad-image-wrapper"
                      >
                        <img
                          :src="img"
                          :alt="product.title"
                          class="ad-image"
                        />
                      </div>
                    </div>
                    <div class="ad-image-overlay"></div>
                    <!-- 图片指示器 -->
                    <div v-if="getProductImages(product).length > 1" class="ad-image-indicators">
                      <span
                        v-for="(img, imgIndex) in getProductImages(product)"
                        :key="`indicator-${product.product_id}-${imgIndex}`"
                        class="ad-image-indicator"
                        :class="{ active: getCurrentImageIndex(index) === imgIndex }"
                      ></span>
                    </div>
                  </div>
                  <div class="ad-content" :class="{ 'full-width': getProductImages(product).length === 0 }">
                    <div class="ad-tag">精选好物</div>
                    <h3 class="ad-title">{{ product.title }}</h3>
                    <p class="ad-description">{{ product.detailed_description || '新鲜优质，产地直供' }}</p>
                    <div class="ad-price-section">
                      <span class="ad-price-label">特惠价</span>
                      <span class="ad-price">¥{{ product.price || '面议' }}</span>
                    </div>
                    <div class="ad-action-btn">
                      <span class="btn-text">立即查看</span>
                      <span class="btn-arrow">→</span>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 轮播指示器 -->
              <div class="carousel-indicators" v-if="adProducts.length > 1">
                <span
                  v-for="(item, index) in adProducts"
                  :key="index"
                  class="indicator"
                  :class="{ active: index === currentAdIndex }"
                  @click.stop="currentAdIndex = index"
                ></span>
              </div>
              <!-- 左右切换按钮 -->
              <button
                v-if="adProducts.length > 1"
                class="carousel-btn prev"
                @click.stop="prevAd"
              >
                ‹
              </button>
              <button
                v-if="adProducts.length > 1"
                class="carousel-btn next"
                @click.stop="nextAd"
              >
                ›
              </button>
            </div>
          </div>
          <div v-else class="ad-empty">
            <p>暂无广告商品</p>
          </div>
        </div>

        <!-- 功能模块区域 -->
        <div class="modules-section">
          <h2 class="section-title">功能模块</h2>
          <div class="modules-grid">
            <div 
              v-for="module in availableModules" 
              :key="module.id"
              class="module-card"
              @click="handleModuleClick(module)"
            >
              <div class="module-card-background"></div>
              <div class="module-icon-wrapper">
                <div class="module-icon">{{ module.icon }}</div>
                <div class="module-icon-glow"></div>
              </div>
              <div class="module-content">
                <h3 class="module-name">{{ module.name }}</h3>
                <p class="module-desc">{{ module.description }}</p>
              </div>
              <div class="module-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, reactive, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../api/auth';
import { productService } from '../api/product';
import logger from '../utils/logger';
import Skeleton from '../components/Skeleton.vue';

export default {
  name: 'Home',
  components: {
    Skeleton
  },
  setup() {
    const router = useRouter();
    const userInfo = ref({});
    const balance = ref(null);
    const loadingBalance = ref(false);
    
    // 广告相关
    const adProducts = ref([]);
    const loadingAds = ref(false);
    const currentAdIndex = ref(0);
    const currentImageIndices = reactive({}); // 每个商品的当前图片索引（使用reactive确保响应式）
    let adInterval = null; // 商品切换定时器
    let imageIntervals = {}; // 每个商品的图片轮播定时器

    // 获取用户信息
    onMounted(async () => {
      logger.lifecycle('Home', 'mounted');
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
          logger.info('HOME', '加载用户信息成功', { userType: userInfo.value.userType });
          
          // 实时获取余额
          await loadBalance();
          
          // 如果是买家，加载广告商品
          if (userInfo.value.userType === 'buyer') {
            await loadAdProducts();
          }
        } catch (error) {
          logger.error('HOME', '解析用户信息失败', {}, error);
          router.push('/login');
        }
      } else {
        logger.warn('HOME', '未找到用户信息，跳转到登录页');
        router.push('/login');
      }
    });

    // 组件卸载时清除定时器
    onUnmounted(() => {
      if (adInterval) {
        clearInterval(adInterval);
        adInterval = null;
      }
      // 清除所有图片轮播定时器
      Object.keys(imageIntervals).forEach(productId => {
        clearInterval(imageIntervals[productId]);
      });
      imageIntervals = {};
    });

    // 加载余额
    const loadBalance = async () => {
      const userType = userInfo.value.userType;
      if (userType === 'farmer' || userType === 'buyer' || userType === 'bank') {
        loadingBalance.value = true;
        try {
          const currentBalance = await authService.getBalance(userInfo.value.phone, userType);
          balance.value = currentBalance;
          // 同时更新localStorage中的余额
          userInfo.value.money = currentBalance;
          localStorage.setItem('user', JSON.stringify(userInfo.value));
          logger.info('HOME', '获取余额成功', { balance: currentBalance });
        } catch (error) {
          logger.error('HOME', '获取余额失败', {}, error);
          // 如果获取失败，使用localStorage中的余额
          balance.value = userInfo.value.money || 0;
        } finally {
          loadingBalance.value = false;
        }
      }
    };

    // 用户名首字母
    const userInitial = computed(() => {
      const name = userInfo.value.nickname || userInfo.value.phone || 'U';
      return name.charAt(0).toUpperCase();
    });

    // 用户角色文本
    const userRoleText = computed(() => {
      const roleMap = {
        farmer: '农户',
        buyer: '买家',
        expert: '技术专家',
        bank: '银行'
      };
      return roleMap[userInfo.value.userType] || '无';
    });

    // 是否显示余额（仅对农户、买家、银行显示）
    const shouldShowBalance = computed(() => {
      const userType = userInfo.value.userType;
      return userType === 'farmer' || userType === 'buyer' || userType === 'bank';
    });

    // 格式化余额
    const formattedBalance = computed(() => {
      const currentBalance = balance.value !== null ? balance.value : userInfo.value.money;
      if (currentBalance === null || currentBalance === undefined) {
        return '0.00';
      }
      // 确保是数字类型
      const numBalance = typeof currentBalance === 'number' ? currentBalance : parseFloat(currentBalance);
      if (isNaN(numBalance)) {
        return '0.00';
      }
      return numBalance.toFixed(2);
    });

    // 欢迎信息
    const welcomeMessage = computed(() => {
      const hour = new Date().getHours();
      let greeting = '你好';
      if (hour>5 && hour < 12) greeting = '早上好';
      else if (hour>12 && hour < 18) greeting = '下午好';
      else if (hour>18 && hour < 24) greeting = '晚上好';
      else greeting = '凌晨好';
      
      return `${greeting}，${userInfo.value.nickname || '用户'}`;
    });

    // 副标题信息
    const subtitleMessage = computed(() => {
      if (userInfo.value.userType === 'farmer') {
        return '欢迎来到农乐平台，在这里管理您的农产品';
      } else if (userInfo.value.userType === 'buyer') {
        return '欢迎来到农乐平台，发现优质农产品';
      } else if (userInfo.value.userType === 'expert') {
        return '欢迎来到农乐平台，与农户分享专业知识';
      }
      return '欢迎来到农乐平台';
    });

    // 根据用户类型获取可用的功能模块
    const availableModules = computed(() => {
      const modules = {
        farmer: [
          {
            id: 'trading',
            name: '农产品交易',
            description: '发布和管理您的农产品',
            icon: '🌾',
            route: '/trading'
          },
          {
            id: 'orders',
            name: '我的订单',
            description: '查看和管理您的订单',
            icon: '📦',
            route: '/orders'
          },
          {
            id: 'expert-appointment',
            name: '专家预约',
            description: '选择专家，提交预约请求',
            icon: '📅',
            route: '/expert-appointment'
          },
          {
            id: 'community',
            name: '专家农户交流平台',
            description: '与专家和其他农户交流，分享经验与提问',
            icon: '💬',
            route: '/community'
          },
          {
            id: 'loan',
            name: '贷款',
            description: '申请农业贷款，查看贷款进度',
            icon: '💰',
            route: '/loan'
          },
          {
            id: 'price-prediction',
            name: '价格预测',
            description: '上传价格数据，预测未来价格走势',
            icon: '📊',
            route: '/price-prediction'
          },
          {
            id: 'price-data',
            name: '价格数据获取',
            description: '获取农产品价格数据，导出为Excel文件',
            icon: '📈',
            route: '/price-data'
          },
        ],
        expert: [
          {
            id: 'community',
            name: '专家农户交流平台',
            description: '与农户交流，分享专业知识与解答问题',
            icon: '💬',
            route: '/community'
          },
          {
            id: 'expert-appointment',
            name: '专家预约',
            description: '查看并处理农户的预约请求',
            icon: '📅',
            route: '/expert-appointment'
          }
        ],
        buyer: [
          {
            id: 'trading',
            name: '农产品交易',
            description: '浏览优质农产品，下单购买',
            icon: '🌾',
            route: '/trading'
          },
          {
            id: 'orders',
            name: '我的订单',
            description: '查看和管理您的订单',
            icon: '📦',
            route: '/orders'
          }
        ],
        bank: [
          {
            id: 'loan',
            name: '融资服务',
            description: '发布贷款产品，审批贷款申请，管理放款',
            icon: '💰',
            route: '/loan'
          }
        ]
      };

      return modules[userInfo.value.userType] || [];
    });

    // 登出
    const handleLogout = () => {
      logger.userAction('LOGOUT_CLICK', { userType: userInfo.value.userType });
      authService.logout();
      router.push('/login');
    };

    // 获取商品的所有图片
    const getProductImages = (product) => {
      if (product.images && product.images.length > 0) {
        return product.images;
      }
      if (product.main_image_url) {
        return [product.main_image_url];
      }
      return [];
    };

    // 获取当前商品的当前图片索引
    const getCurrentImageIndex = (productIndex) => {
      // 只有当前显示的商品才返回图片索引，其他商品返回0
      if (productIndex !== currentAdIndex.value) {
        return 0;
      }
      const productId = adProducts.value[productIndex]?.product_id;
      if (productId) {
        const index = currentImageIndices[productId] || 0;
        return index;
      }
      return 0;
    };

    // 启动单个商品的图片轮播
    const startImageCarousel = (productId, imageCount) => {
      if (imageCount <= 1) return;
      
      // 清除旧的定时器
      if (imageIntervals[productId]) {
        clearInterval(imageIntervals[productId]);
      }
      
      // 初始化索引
      if (currentImageIndices[productId] === undefined) {
        currentImageIndices[productId] = 0;
      }
      
      // 启动新的定时器，每1秒切换一次
      imageIntervals[productId] = setInterval(() => {
        currentImageIndices[productId] = 
          (currentImageIndices[productId] + 1) % imageCount;
        logger.info('HOME', '图片轮播', { 
          productId, 
          currentIndex: currentImageIndices[productId],
          totalImages: imageCount 
        });
      }, 3000);
    };

    // 停止单个商品的图片轮播
    const stopImageCarousel = (productId) => {
      if (imageIntervals[productId]) {
        clearInterval(imageIntervals[productId]);
        delete imageIntervals[productId];
      }
    };

    // 加载广告商品
    const loadAdProducts = async () => {
      loadingAds.value = true;
      try {
        logger.info('HOME', '开始加载广告商品', {});
        const response = await productService.getAllOnShelfProducts();
        const products = response.list || [];
        
        // 随机选择最多5个商品用于广告
        if (products.length > 0) {
          const shuffled = [...products].sort(() => Math.random() - 0.5);
          adProducts.value = shuffled.slice(0, Math.min(5, shuffled.length));
          currentAdIndex.value = 0;
          
          // 初始化每个商品的图片索引
          await nextTick(); // 等待DOM更新
          adProducts.value.forEach(product => {
            const images = getProductImages(product);
            logger.info('HOME', '初始化商品图片', { 
              productId: product.product_id, 
              imageCount: images.length,
              images: images 
            });
            if (images.length > 0) {
              currentImageIndices[product.product_id] = 0;
              // 启动当前显示商品的图片轮播
              if (product.product_id === adProducts.value[0].product_id) {
                startImageCarousel(product.product_id, images.length);
              }
            }
          });
          
          // 如果有多张广告，启动自动轮播
          if (adProducts.value.length > 1) {
            startAdCarousel();
          }
          
          logger.info('HOME', '广告商品加载成功', { count: adProducts.value.length });
        }
      } catch (error) {
        logger.error('HOME', '加载广告商品失败', {}, error);
        adProducts.value = [];
      } finally {
        loadingAds.value = false;
      }
    };

    // 启动广告轮播（商品切换）
    const startAdCarousel = () => {
      if (adInterval) {
        clearInterval(adInterval);
      }
      adInterval = setInterval(() => {
        // 停止当前商品的图片轮播
        const currentProduct = adProducts.value[currentAdIndex.value];
        if (currentProduct) {
          stopImageCarousel(currentProduct.product_id);
        }
        
        // 切换到下一个商品
        currentAdIndex.value = (currentAdIndex.value + 1) % adProducts.value.length;
        
        // 启动新商品的图片轮播
        const nextProduct = adProducts.value[currentAdIndex.value];
        if (nextProduct) {
          const images = getProductImages(nextProduct);
          // 重置图片索引
          currentImageIndices[nextProduct.product_id] = 0;
          if (images.length > 1) {
            startImageCarousel(nextProduct.product_id, images.length);
          }
        }
      }, 10000); // 每5秒切换一次商品
    };

    // 上一张广告
    const prevAd = () => {
      // 停止当前商品的图片轮播
      const currentProduct = adProducts.value[currentAdIndex.value];
      if (currentProduct) {
        stopImageCarousel(currentProduct.product_id);
      }
      
      currentAdIndex.value = (currentAdIndex.value - 1 + adProducts.value.length) % adProducts.value.length;
      
      // 启动新商品的图片轮播
      const newProduct = adProducts.value[currentAdIndex.value];
      if (newProduct) {
        const images = getProductImages(newProduct);
        // 重置图片索引
        currentImageIndices[newProduct.product_id] = 0;
        if (images.length > 1) {
          startImageCarousel(newProduct.product_id, images.length);
        }
      }
      
      if (adInterval) {
        clearInterval(adInterval);
        startAdCarousel();
      }
    };

    // 下一张广告
    const nextAd = () => {
      // 停止当前商品的图片轮播
      const currentProduct = adProducts.value[currentAdIndex.value];
      if (currentProduct) {
        stopImageCarousel(currentProduct.product_id);
      }
      
      currentAdIndex.value = (currentAdIndex.value + 1) % adProducts.value.length;
      
      // 启动新商品的图片轮播
      const newProduct = adProducts.value[currentAdIndex.value];
      if (newProduct) {
        const images = getProductImages(newProduct);
        // 重置图片索引
        currentImageIndices[newProduct.product_id] = 0;
        if (images.length > 1) {
          startImageCarousel(newProduct.product_id, images.length);
        }
      }
      
      if (adInterval) {
        clearInterval(adInterval);
        startAdCarousel();
      }
    };

    // 点击广告
    const handleAdClick = (index) => {
      const product = adProducts.value[index];
      if (product) {
        logger.userAction('AD_CLICK', { productId: product.product_id, productTitle: product.title });
        // 跳转到交易页面并传递商品ID，让页面自动打开商品详情
        router.push({
          path: '/trading',
          query: { productId: product.product_id }
        });
      }
    };

    // 点击功能模块
    const handleModuleClick = (module) => {
      logger.userAction('MODULE_CLICK', { 
        moduleId: module.id,
        moduleName: module.name,
        userType: userInfo.value.userType 
      });
      
      // 支持路由的模块直接跳转
      if (module.id === 'trading' || module.id === 'community' || module.id === 'orders' || module.id === 'loan' || module.id === 'price-prediction' || module.id === 'price-data' || module.id === 'expert-appointment') {
        router.push(module.route);
      } else {
        // 其他模块暂时使用提示
        alert(`即将进入：${module.name}\n功能开发中...`);
      }
    };

    return {
      userInfo,
      balance,
      loadingBalance,
      userInitial,
      userRoleText,
      shouldShowBalance,
      formattedBalance,
      welcomeMessage,
      subtitleMessage,
      availableModules,
      handleLogout,
      handleModuleClick,
      loadBalance,
      adProducts,
      loadingAds,
      currentAdIndex,
      prevAd,
      nextAd,
      handleAdClick,
      getProductImages,
      getCurrentImageIndex
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';
@import '../assets/styles/loading.css';

.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 50%, #f0f4ff 100%);
  position: relative;
  overflow-x: hidden;
}

.home-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400px;
  background: radial-gradient(circle at 20% 50%, rgba(107, 70, 193, 0.06) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(139, 92, 246, 0.05) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.home-container::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 300px;
  background: radial-gradient(circle at 50% 0%, rgba(107, 70, 193, 0.04) 0%, transparent 60%);
  pointer-events: none;
  z-index: 0;
}

/* 顶部导航栏 */
.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: var(--spacing-5) var(--spacing-8);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-card);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(107, 70, 193, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-5);
}

.avatar-wrapper {
  position: relative;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: var(--white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 2;
  transition: transform 0.3s ease;
}

.avatar-wrapper:hover .avatar {
  transform: scale(1.05);
}

.avatar-ring {
  position: absolute;
  top: calc(-1 * var(--spacing-1));
  left: calc(-1 * var(--spacing-1));
  right: calc(-1 * var(--spacing-1));
  bottom: calc(-1 * var(--spacing-1));
  border-radius: var(--radius-full);
  border: 2px solid rgba(107, 70, 193, 0.2);
  animation: pulse-ring 2s ease-out infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.2);
    opacity: 0;
  }
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.user-name {
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--gray-900);
  letter-spacing: -0.01em;
}

.user-role-badge {
  display: inline-block;
  padding: var(--spacing-1) var(--spacing-3);
  background: linear-gradient(135deg, rgba(107, 70, 193, 0.1) 0%, rgba(159, 122, 234, 0.1) 100%);
  color: var(--primary);
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(107, 70, 193, 0.2);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  flex-wrap: wrap;
}

.user-phone {
  font-size: var(--font-sm);
  color: var(--gray-600);
  font-weight: var(--font-medium);
  letter-spacing: 0.02em;
}

.user-balance {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-1) var(--spacing-3);
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.balance-label {
  color: var(--gray-600);
  font-weight: var(--font-medium);
  font-size: var(--font-xs);
}

.balance-amount {
  color: var(--success);
  font-weight: var(--font-bold);
  font-size: var(--font-sm);
}

.balance-loading {
  color: var(--gray-500);
  font-size: var(--font-xs);
}

.btn-logout {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-6);
  background: transparent;
  border: 1.5px solid var(--gray-300);
  border-radius: var(--radius-lg);
  color: var(--gray-600);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-logout:hover {
  background: rgba(107, 70, 193, 0.05);
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.logout-icon {
  width: 18px;
  height: 18px;
  transition: transform 0.3s ease;
}

.btn-logout:hover .logout-icon {
  transform: translateX(2px);
}

/* 主内容区域 */
.main-content {
  padding: var(--spacing-6) var(--spacing-8);
  position: relative;
  z-index: 1;
}

.content-wrapper {
  max-width: 1280px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 欢迎区域 */
.welcome-section {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: var(--spacing-6) var(--spacing-8);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-6);
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(107, 70, 193, 0.08);
}

.welcome-content {
  position: relative;
  z-index: 2;
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.welcome-greeting {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-2);
}

.greeting-icon {
  font-size: var(--font-2xl);
  animation: wave 2s ease-in-out infinite;
  transform-origin: 70% 70%;
}

@keyframes wave {
  0%, 100% { transform: rotate(0deg); }
  10%, 30% { transform: rotate(14deg); }
  20% { transform: rotate(-8deg); }
  40%, 60% { transform: rotate(-4deg); }
  50% { transform: rotate(10deg); }
}

.welcome-title {
  font-size: var(--font-2xl);
  font-weight: var(--font-extrabold);
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: -0.02em;
  line-height: var(--leading-tight);
}

.welcome-subtitle {
  font-size: var(--font-base);
  color: var(--gray-600);
  margin: 0;
  line-height: var(--leading-normal);
  font-weight: var(--font-medium);
}

.welcome-decoration {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(107, 70, 193, 0.1) 0%, rgba(159, 122, 234, 0.05) 100%);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 120px;
  height: 120px;
  top: -40px;
  right: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 80px;
  height: 80px;
  bottom: -20px;
  right: 20%;
  animation-delay: 2s;
}

.circle-3 {
  width: 60px;
  height: 60px;
  top: 50%;
  right: 5%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) translateX(0); }
  50% { transform: translateY(-20px) translateX(10px); }
}

/* 功能模块区域 */
.modules-section {
  margin-top: var(--spacing-6);
  position: relative;
}

.section-title {
  font-size: var(--font-2xl);
  font-weight: var(--font-bold);
  color: var(--gray-900);
  margin-bottom: var(--spacing-6);
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  letter-spacing: -0.01em;
}

.section-title::before {
  content: '';
  width: var(--spacing-1);
  height: 22px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  border-radius: var(--spacing-1);
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-5);
}

.module-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: var(--spacing-6) var(--spacing-6);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  border: 1px solid rgba(107, 70, 193, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.module-card-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(107, 70, 193, 0.04) 0%, rgba(159, 122, 234, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
  z-index: 0;
}

.module-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary) 0%, var(--primary-light) 100%);
  opacity: 0;
  transition: opacity 0.4s ease;
  z-index: 2;
}

.module-card:hover::before {
  opacity: 1;
}

.module-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-card-hover);
  border-color: rgba(107, 70, 193, 0.2);
}

.module-card:hover .module-card-background {
  opacity: 1;
}

.module-icon-wrapper {
  position: relative;
  margin-bottom: var(--spacing-4);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  z-index: 1;
}

.module-icon {
  font-size: var(--font-2xl);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(107, 70, 193, 0.08) 0%, rgba(159, 122, 234, 0.08) 100%);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.05));
}

.module-icon-glow {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 56px;
  height: 56px;
  background: radial-gradient(circle, rgba(107, 70, 193, 0.15) 0%, transparent 70%);
  border-radius: var(--radius-md);
  opacity: 0;
  transition: opacity 0.4s ease, transform 0.4s ease, width 0.4s ease, height 0.4s ease;
  z-index: 1;
}

.module-card:hover .module-icon {
  transform: translateY(-2px);
  background: linear-gradient(135deg, rgba(107, 70, 193, 0.12) 0%, rgba(159, 122, 234, 0.12) 100%);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.15);
}

.module-card:hover .module-icon-glow {
  opacity: 1;
  transform: translateY(-50%) scale(1.15);
  width: 62px;
  height: 62px;
}

.module-content {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.module-name {
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  color: var(--gray-900);
  margin: 0 0 var(--spacing-2) 0;
  letter-spacing: -0.01em;
  transition: color 0.3s ease;
  line-height: var(--leading-tight);
}

.module-card:hover .module-name {
  color: var(--primary);
}

.module-desc {
  font-size: var(--font-sm);
  color: var(--gray-600);
  line-height: var(--leading-normal);
  margin: 0;
  transition: color 0.3s ease;
}

.module-card:hover .module-desc {
  color: var(--gray-700);
}

.module-arrow {
  position: absolute;
  bottom: var(--spacing-6);
  right: var(--spacing-6);
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(107, 70, 193, 0.08);
  border-radius: var(--radius-sm);
  color: var(--primary);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.module-arrow svg {
  width: 14px;
  height: 14px;
}

.module-card:hover .module-arrow {
  transform: translateX(var(--spacing-1));
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: var(--white);
  box-shadow: var(--shadow-md);
}

/* 广告轮播区域 */
.ad-banner-section {
  margin-bottom: var(--spacing-6);
}

.ad-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
  padding: 0 var(--spacing-2);
}

.ad-header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.ad-badge {
  font-size: var(--font-2xl);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.ad-section-title {
  font-size: var(--font-xl);
  font-weight: var(--font-extrabold);
  color: var(--gray-900);
  margin: 0;
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.01em;
}

.ad-subtitle {
  font-size: var(--font-sm);
  color: var(--gray-500);
}

/* 加载状态样式已移至 loading.css */

.ad-carousel {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-card-elevated);
  overflow: hidden;
  position: relative;
  border: 3px solid rgba(255, 255, 255, 0.2);
}

.carousel-container {
  position: relative;
  width: 100%;
  height: 210px;
  overflow: hidden;
  cursor: pointer;
}

.carousel-slide {
  display: flex;
  width: 100%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.ad-slide {
  min-width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.ad-badge-hot {
  position: absolute;
  top: var(--spacing-4);
  left: var(--spacing-4);
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-xl);
  font-size: var(--font-xs);
  font-weight: var(--font-bold);
  z-index: 10;
  box-shadow: var(--shadow-md);
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.ad-image-container {
  flex: 1.2;
  height: 100%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.ad-images-slider {
  display: flex;
  width: 100%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.ad-image-wrapper {
  min-width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ad-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.ad-slide:hover .ad-image {
  transform: scale(1.05);
}

.ad-image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  pointer-events: none;
  z-index: 2;
}

.ad-image-indicators {
  position: absolute;
  bottom: 10%;
  left: 50%;
  transform: translate(-50%, 50%);
  display: flex;
  gap: 0.5rem;
  z-index: 5;
  background: rgba(0, 0, 0, 0.3);
  padding: 0.5rem;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.ad-image-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.ad-image-indicator.active {
  background: var(--white);
  width: 20px;
  border-radius: 10px;
}

.ad-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
}

.ad-icon {
  font-size: 4rem;
  opacity: 0.5;
}

.ad-content {
  flex: 1;
  padding: var(--spacing-6);
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.95) 100%);
  position: relative;
  z-index: 5;
}

.ad-content.full-width {
  flex: 1 1 100%;
  max-width: 100%;
}

.ad-tag {
  display: inline-block;
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
  color: white;
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-md);
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  margin-bottom: var(--spacing-3);
  width: fit-content;
  box-shadow: var(--shadow-sm);
}

.ad-title {
  font-size: var(--font-2xl);
  font-weight: var(--font-extrabold);
  color: var(--gray-900);
  margin: 0 0 var(--spacing-2) 0;
  line-height: var(--leading-tight);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

  .ad-description {
    font-size: var(--font-sm);
    color: var(--gray-600);
    line-height: var(--leading-normal);
    margin: 0 0 var(--spacing-4) 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    max-height: 2.625rem;
  }

.ad-price-section {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-4);
}

.ad-price-label {
  font-size: var(--font-xs);
  color: var(--gray-500);
  font-weight: var(--font-medium);
}

.ad-price {
  font-size: var(--font-3xl);
  font-weight: var(--font-extrabold);
  background: linear-gradient(135deg, var(--success) 0%, var(--success-dark) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.ad-action-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1);
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
  color: white;
  padding: var(--spacing-2) var(--spacing-5);
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  width: fit-content;
  box-shadow: var(--shadow-lg);
  transition: all 0.3s ease;
}

.ad-action-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-xl);
}

.btn-text {
  font-weight: 600;
}

.btn-arrow {
  font-size: 1.25rem;
  transition: transform 0.3s ease;
}

.ad-action-btn:hover .btn-arrow {
  transform: translateX(var(--spacing-1));
}

.carousel-indicators {
  position: absolute;
  bottom: var(--spacing-6);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: var(--spacing-3);
  z-index: 10;
  background: rgba(0, 0, 0, 0.2);
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-2xl);
  backdrop-filter: blur(10px);
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.3s;
}

.indicator:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: scale(1.2);
}

.indicator.active {
  background: var(--white);
  width: 28px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(255, 255, 255, 0.5);
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  color: var(--primary);
  font-size: 1.75rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.carousel-btn:hover {
  background: var(--white);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.25);
  transform: translateY(-50%) scale(1.1);
}

.carousel-btn.prev {
  left: 1.5rem;
}

.carousel-btn.next {
  right: 1.5rem;
}

.ad-empty {
  background: var(--white);
  padding: var(--spacing-12);
  border-radius: var(--radius-xl);
  text-align: center;
  box-shadow: var(--shadow-card);
  color: var(--gray-500);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .modules-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .header {
    padding: 1rem 1.25rem;
    flex-wrap: wrap;
    gap: 1rem;
  }

  .user-info {
    gap: 0.875rem;
  }

  .avatar {
    width: 48px;
    height: 48px;
    font-size: 1.25rem;
  }

  .user-name {
    font-size: 1rem;
  }

  .user-role-badge {
    font-size: 0.7rem;
    padding: 0.2rem 0.625rem;
  }

  .user-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .user-balance {
    padding: 0.25rem 0.75rem;
  }

  .btn-logout {
    padding: 0.625rem 1.25rem;
    font-size: 0.8125rem;
  }

  .main-content {
    padding: 1.25rem;
  }

  .welcome-section {
    padding: 2rem 1.75rem;
    border-radius: 20px;
  }

  .welcome-greeting {
    flex-wrap: wrap;
  }

  .greeting-icon {
    font-size: 2rem;
  }

  .welcome-title {
    font-size: 1.75rem;
  }

  .welcome-subtitle {
    font-size: 1rem;
  }

  .section-title {
    font-size: 1.5rem;
    margin-bottom: 1.5rem;
  }

  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1.5rem;
  }

  .module-card {
    padding: 1.75rem 1.5rem;
    border-radius: 18px;
  }

  .module-icon {
    font-size: 2rem;
    width: 56px;
    height: 56px;
  }

  .module-icon-wrapper {
    margin-bottom: 1rem;
  }

  .module-icon-glow {
    width: 56px;
    height: 56px;
  }

  .module-card:hover .module-icon-glow {
    width: 62px;
    height: 62px;
  }

  .module-name {
    font-size: 1.25rem;
  }

  .ad-section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .ad-section-title {
    font-size: 1.5rem;
  }

  .carousel-container {
    height: 200px;
  }

  .ad-slide {
    flex-direction: column;
  }

  .ad-image-container {
    height: 50%;
  }

  .ad-content {
    height: 50%;
    padding: 1.5rem;
  }

  .ad-title {
    font-size: 1.5rem;
  }

  .ad-description {
    font-size: 0.875rem;
    margin-bottom: 1rem;
  }

  .ad-price {
    font-size: 1.75rem;
  }

  .ad-action-btn {
    padding: 0.75rem 1.5rem;
    font-size: 0.875rem;
  }

  .carousel-btn {
    width: 40px;
    height: 40px;
    font-size: 1.5rem;
  }

  .carousel-btn.prev {
    left: 1rem;
  }

  .carousel-btn.next {
    right: 1rem;
  }

  .decoration-circle {
    display: none;
  }
}

@media (max-width: 480px) {
  .welcome-section {
    padding: 1.5rem;
  }

  .welcome-title {
    font-size: 1.5rem;
  }

  .module-card {
    padding: 1.5rem 1.25rem;
  }

  .module-icon {
    font-size: 2rem;
    width: 52px;
    height: 52px;
  }

  .module-icon-glow {
    width: 52px;
    height: 52px;
  }

  .module-card:hover .module-icon-glow {
    width: 58px;
    height: 58px;
  }
}
</style>

