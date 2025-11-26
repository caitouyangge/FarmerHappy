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
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../api/auth';
import logger from '../utils/logger';

export default {
  name: 'Home',
  setup() {
    const router = useRouter();
    const userInfo = ref({});
    const balance = ref(null);
    const loadingBalance = ref(false);

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
        } catch (error) {
          logger.error('HOME', '解析用户信息失败', {}, error);
          router.push('/login');
        }
      } else {
        logger.warn('HOME', '未找到用户信息，跳转到登录页');
        router.push('/login');
      }
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
        ],
        expert: [
          {
            id: 'community',
            name: '专家农户交流平台',
            description: '与农户交流，分享专业知识与解答问题',
            icon: '💬',
            route: '/community'
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

    // 点击功能模块
    const handleModuleClick = (module) => {
      logger.userAction('MODULE_CLICK', { 
        moduleId: module.id,
        moduleName: module.name,
        userType: userInfo.value.userType 
      });
      
      // 支持路由的模块直接跳转
      if (module.id === 'trading' || module.id === 'community' || module.id === 'orders' || module.id === 'loan') {
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
      loadBalance
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
}
</style>

