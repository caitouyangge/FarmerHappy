<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <!-- 左侧用户信息 -->
      <div class="user-info">
        <div class="avatar">{{ userInitial }}</div>
        <div class="user-details">
          <div class="user-name">{{ userInfo.nickname || '用户' }}</div>
          <div class="user-phone">{{ userInfo.phone }}</div>
          <div class="user-role">{{ userRoleText }}</div>
          <div v-if="shouldShowBalance" class="user-balance">
            <span class="balance-label">余额：</span>
            <span v-if="loadingBalance" class="balance-loading">加载中...</span>
            <span v-else class="balance-amount">¥{{ formattedBalance }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧登出按钮 -->
      <button class="btn-logout" @click="handleLogout">
        <span class="logout-icon">⎋</span>
        登出
      </button>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 欢迎标题 -->
        <div class="welcome-section">
          <h1 class="welcome-title">{{ welcomeMessage }}</h1>
          <p class="welcome-subtitle">{{ subtitleMessage }}</p>
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
          <div v-if="loadingAds" class="ad-loading">
            <div class="loading-spinner"></div>
            <p>加载广告中...</p>
          </div>
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
              <div class="module-icon">{{ module.icon }}</div>
              <h3 class="module-name">{{ module.name }}</h3>
              <p class="module-desc">{{ module.description }}</p>
              <div class="module-arrow">→</div>
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

export default {
  name: 'Home',
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
      else if (hour>18 && hour < 21) greeting = '晚上好';
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
            description: '发布和管理您的农产品，查看交易订单',
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

.home-container {
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

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: var(--white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.3);
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
}

.user-role {
  font-size: 0.875rem;
  color: var(--primary);
  font-weight: 500;
}

.user-balance {
  font-size: 0.875rem;
  color: #10b981;
  font-weight: 600;
  margin-top: 0.25rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.balance-label {
  color: var(--gray-500);
  font-weight: 400;
}

.balance-amount {
  color: #10b981;
  font-weight: 600;
}

.balance-loading {
  color: var(--gray-500);
  font-size: 0.75rem;
}

.btn-logout {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  color: var(--gray-500);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-logout:hover {
  background: var(--gray-100);
  border-color: var(--primary-light);
  color: var(--primary);
}

.logout-icon {
  font-size: 1.125rem;
}

/* 主内容区域 */
.main-content {
  padding: 2rem;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

/* 欢迎区域 */
.welcome-section {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  margin-bottom: 2rem;
}

.welcome-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 0.5rem;
}

.welcome-subtitle {
  font-size: 1rem;
  color: var(--gray-500);
  margin: 0;
}

/* 功能模块区域 */
.modules-section {
  margin-top: 2rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 1.5rem;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.module-card {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  border: 2px solid transparent;
}

.module-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(107, 70, 193, 0.15);
  border-color: var(--primary-light);
}

.module-icon {
  font-size: 3.5rem;
  margin-bottom: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
}

.module-name {
  font-size: 1.375rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.75rem 0;
}

.module-desc {
  font-size: 0.9375rem;
  color: var(--gray-500);
  line-height: 1.6;
  margin: 0;
  min-height: 2.8rem;
}

.module-arrow {
  position: absolute;
  bottom: 1.5rem;
  right: 1.5rem;
  font-size: 1.5rem;
  color: var(--primary);
  transition: transform 0.3s;
}

.module-card:hover .module-arrow {
  transform: translateX(6px);
}

/* 广告轮播区域 */
.ad-banner-section {
  margin-bottom: 2rem;
}

.ad-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding: 0 0.5rem;
}

.ad-header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ad-badge {
  font-size: 1.5rem;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.ad-section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ad-subtitle {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.ad-loading {
  background: var(--white);
  padding: 3rem;
  border-radius: 20px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(107, 70, 193, 0.1);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--gray-200);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ad-carousel {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(107, 70, 193, 0.25);
  overflow: hidden;
  position: relative;
  border: 3px solid rgba(255, 255, 255, 0.2);
}

.carousel-container {
  position: relative;
  width: 100%;
  height: 360px;
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
  top: 1.5rem;
  left: 1.5rem;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
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
  bottom: 50%;
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
  padding: 2.5rem;
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
  padding: 0.375rem 0.875rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  margin-bottom: 1rem;
  width: fit-content;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.3);
}

.ad-title {
  font-size: 2rem;
  font-weight: 800;
  color: #1a202c;
  margin: 0 0 0.75rem 0;
  line-height: 1.2;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

  .ad-description {
    font-size: 1rem;
    color: var(--gray-600);
    line-height: 1.6;
    margin: 0 0 1.5rem 0;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    max-height: 4.8rem;
  }

.ad-price-section {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.ad-price-label {
  font-size: 0.875rem;
  color: var(--gray-500);
  font-weight: 500;
}

.ad-price {
  font-size: 2.25rem;
  font-weight: 800;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.ad-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: linear-gradient(135deg, var(--primary) 0%, #8b5cf6 100%);
  color: white;
  padding: 0.875rem 1.75rem;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  width: fit-content;
  box-shadow: 0 4px 16px rgba(107, 70, 193, 0.4);
  transition: all 0.3s ease;
}

.ad-action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(107, 70, 193, 0.5);
}

.btn-text {
  font-weight: 600;
}

.btn-arrow {
  font-size: 1.25rem;
  transition: transform 0.3s ease;
}

.ad-action-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.carousel-indicators {
  position: absolute;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.75rem;
  z-index: 10;
  background: rgba(0, 0, 0, 0.2);
  padding: 0.5rem 1rem;
  border-radius: 20px;
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
  padding: 3rem;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  color: var(--gray-500);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .main-content {
    padding: 1rem;
  }

  .welcome-section {
    padding: 1.5rem;
  }

  .welcome-title {
    font-size: 1.5rem;
  }

  .modules-grid {
    grid-template-columns: 1fr;
  }

  .module-card {
    padding: 1.5rem;
  }

  .ad-section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .carousel-container {
    height: 320px;
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
}
</style>

