<template>
  <nav class="navbar">
    <div class="navbar-container">
      <div class="navbar-brand" @click="goHome">
        <span class="brand-icon">🌾</span>
        <span class="brand-text">农乐</span>
      </div>
      
      <ul class="navbar-menu">
        <li 
          v-for="item in menuItems" 
          :key="item.id"
          class="navbar-item"
          :class="{ active: isActive(item.route) }"
          @click="navigate(item.route)"
        >
          <span class="item-icon">{{ item.icon }}</span>
          <span class="item-text">{{ item.name }}</span>
        </li>
        
        <li 
          class="navbar-item"
          :class="{ active: $route.path === '/profile' }"
          @click="navigate('/profile')"
        >
          <span class="item-icon">👤</span>
          <span class="item-text">个人信息</span>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script>
import { computed, ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { authService } from '../api/auth';
import logger from '../utils/logger';

export default {
  name: 'Navbar',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const userInfo = ref({});

    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
        } catch (error) {
          logger.error('NAVBAR', '解析用户信息失败', {}, error);
        }
      }
    });

    const userInitial = computed(() => {
      const name = userInfo.value.nickname || userInfo.value.phone || 'U';
      return name.charAt(0).toUpperCase();
    });

    const userRoleText = computed(() => {
      const roleMap = {
        farmer: '农户',
        buyer: '买家',
        expert: '技术专家',
        bank: '银行'
      };
      return roleMap[userInfo.value.userType] || '无';
    });

    const menuItems = computed(() => {
      const userType = userInfo.value.userType;
      const allItems = {
        farmer: [
          { id: 'home', name: '首页', icon: '🏠', route: '/home' },
          { id: 'trading', name: '农产品交易', icon: '🌾', route: '/trading' },
          { id: 'orders', name: '我的订单', icon: '📦', route: '/orders' },
          { id: 'expert-appointment', name: '专家预约', icon: '📅', route: '/expert-appointment' },
          { id: 'community', name: '交流平台', icon: '💬', route: '/community' },
          { id: 'loan', name: '贷款', icon: '💰', route: '/loan' },
          { id: 'price-prediction', name: '价格预测', icon: '📊', route: '/price-prediction' },
          { id: 'price-data', name: '价格数据', icon: '📈', route: '/price-data' }
        ],
        expert: [
          { id: 'home', name: '首页', icon: '🏠', route: '/home' },
          { id: 'community', name: '交流平台', icon: '💬', route: '/community' },
          { id: 'expert-appointment', name: '专家预约', icon: '📅', route: '/expert-appointment' }
        ],
        buyer: [
          { id: 'home', name: '首页', icon: '🏠', route: '/home' },
          { id: 'trading', name: '农产品交易', icon: '🌾', route: '/trading' },
          { id: 'orders', name: '我的订单', icon: '📦', route: '/orders' }
        ],
        bank: [
          { id: 'home', name: '首页', icon: '🏠', route: '/home' },
          { id: 'loan', name: '融资服务', icon: '💰', route: '/loan' }
        ]
      };
      return allItems[userType] || [];
    });

    const isActive = (routePath) => {
      if (routePath === '/home') {
        return route.path === '/home';
      }
      return route.path.startsWith(routePath) && routePath !== '/home';
    };

    const navigate = (path) => {
      router.push(path);
    };

    const goHome = () => {
      router.push('/home');
    };

    const handleLogout = () => {
      logger.userAction('LOGOUT_CLICK', { userType: userInfo.value.userType });
      authService.logout();
      router.push('/login');
    };

    return {
      userInfo,
      userInitial,
      userRoleText,
      menuItems,
      isActive,
      navigate,
      goHome,
      handleLogout
    };
  }
};
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.navbar-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 1.25rem;
  font-weight: 700;
  color: white;
  transition: opacity 0.2s;
}

.navbar-brand:hover {
  opacity: 0.9;
}

.brand-icon {
  font-size: 1.5rem;
}

.brand-text {
  font-weight: 700;
}

.navbar-menu {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 0.5rem;
  flex: 1;
  justify-content: center;
}

.navbar-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.9375rem;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.navbar-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.navbar-item.active {
  background: rgba(255, 255, 255, 0.25);
  color: white;
  font-weight: 600;
}

.item-icon {
  font-size: 1.125rem;
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.user-avatar:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.user-name {
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
}

.user-role {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.75rem;
}

.btn-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
}

.btn-logout:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.logout-icon {
  width: 18px;
  height: 18px;
}

@media (max-width: 1024px) {
  .navbar-container {
    padding: 0 1rem;
  }

  .navbar-menu {
    gap: 0.25rem;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }

  .navbar-menu::-webkit-scrollbar {
    display: none;
  }

  .item-text {
    display: none;
  }

  .navbar-item {
    padding: 0.5rem;
    min-width: 40px;
    justify-content: center;
  }

  .user-info {
    display: none;
  }
}

@media (max-width: 768px) {
  .navbar-container {
    height: 56px;
    padding: 0 0.75rem;
  }

  .brand-text {
    display: none;
  }

  .navbar-user {
    gap: 0.5rem;
  }
}
</style>

