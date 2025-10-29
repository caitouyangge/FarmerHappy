<template>
  <div class="community-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <span class="back-icon">←</span>
          返回
        </button>
        <h1 class="header-title">专家农户交流平台</h1>
      </div>
      <button class="btn-publish" @click="handlePublishClick">
        <span class="publish-icon">✎</span>
        发布内容
      </button>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 筛选和搜索区域 -->
      <div class="filter-section">
        <div class="filter-tabs">
          <button
            v-for="type in contentTypes"
            :key="type.value"
            class="filter-tab"
            :class="{ active: currentType === type.value }"
            @click="handleTypeChange(type.value)"
          >
            {{ type.label }}
          </button>
        </div>
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索内容..."
            class="search-input"
            @input="handleSearch"
          />
          <span class="search-icon">🔍</span>
        </div>
      </div>

      <!-- 排序选项 -->
      <div class="sort-section">
        <span class="sort-label">排序：</span>
        <button
          v-for="sort in sortOptions"
          :key="sort.value"
          class="sort-btn"
          :class="{ active: currentSort === sort.value }"
          @click="handleSortChange(sort.value)"
        >
          {{ sort.label }}
        </button>
      </div>

      <!-- 内容列表 -->
      <div class="content-list" v-if="!loading && contentList.length > 0">
        <div
          v-for="item in contentList"
          :key="item.content_id"
          class="content-card"
          @click="handleContentClick(item.content_id)"
        >
          <div class="content-header">
            <div class="content-type-badge" :class="getTypeClass(item.content_type)">
              {{ getTypeLabel(item.content_type) }}
            </div>
            <div class="content-meta">
              <span class="author">
                <span class="author-icon">👤</span>
                {{ item.author_name }}
              </span>
              <span class="time">{{ formatTime(item.created_at) }}</span>
            </div>
          </div>
          
          <h3 class="content-title">{{ item.title }}</h3>
          
          <p class="content-summary">{{ item.content }}</p>
          
          <div v-if="item.images && item.images.length > 0" class="content-images">
            <img
              v-for="(image, idx) in item.images.slice(0, 3)"
              :key="idx"
              :src="image"
              :alt="`图片${idx + 1}`"
              class="content-image"
              @click.stop="previewImage(image, item.images)"
            />
            <span v-if="item.images.length > 3" class="image-count">+{{ item.images.length - 3 }}</span>
          </div>
          
          <div class="content-footer">
            <div class="content-stats">
              <span class="stat-item">
                <span class="stat-icon">👁</span>
                {{ item.view_count || 0 }}
              </span>
              <span class="stat-item">
                <span class="stat-icon">💬</span>
                {{ item.comment_count || 0 }}
              </span>
            </div>
            <div class="content-role">
              <span class="role-badge" :class="getRoleClass(item.author_role)">
                {{ getRoleLabel(item.author_role) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && contentList.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <p class="empty-text">暂无内容，快来发布第一个帖子吧！</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
    </main>

    <!-- 图片预览模态框 -->
    <div v-if="showImagePreview" class="image-preview-modal" @click="closeImagePreview">
      <img :src="currentImage" :alt="'预览图片'" class="preview-image" @click.stop />
      <button class="close-preview" @click="closeImagePreview">×</button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { communityService } from '../api/community';
import logger from '../utils/logger';

export default {
  name: 'Community',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const contentList = ref([]);
    const currentType = ref('all');
    const currentSort = ref('newest');
    const searchKeyword = ref('');
    const showImagePreview = ref(false);
    const currentImage = ref('');
    const imageList = ref([]);

    const contentTypes = [
      { value: 'all', label: '全部' },
      { value: 'articles', label: '文章' },
      { value: 'questions', label: '提问' },
      { value: 'experiences', label: '经验分享' }
    ];

    const sortOptions = [
      { value: 'newest', label: '最新' },
      { value: 'hottest', label: '最热' },
      { value: 'commented', label: '最多评论' }
    ];

    // 获取用户信息
    const getUserInfo = () => {
      try {
        const storedUser = localStorage.getItem('user');
        return storedUser ? JSON.parse(storedUser) : null;
      } catch (error) {
        logger.error('COMMUNITY', '获取用户信息失败', {}, error);
        return null;
      }
    };

    // 加载内容列表
    const loadContentList = async () => {
      loading.value = true;
      try {
        const params = {};
        if (currentType.value !== 'all') {
          params.content_type = currentType.value;
        }
        if (searchKeyword.value) {
          params.keyword = searchKeyword.value;
        }
        if (currentSort.value) {
          params.sort = currentSort.value;
        }

        logger.info('COMMUNITY', '加载内容列表', params);
        const data = await communityService.getContentList(params);
        contentList.value = data.list || [];
        logger.info('COMMUNITY', '内容列表加载成功', { count: contentList.value.length });
      } catch (error) {
        logger.error('COMMUNITY', '加载内容列表失败', {}, error);
        alert(error.message || '加载内容失败，请稍后重试');
      } finally {
        loading.value = false;
      }
    };

    // 类型切换
    const handleTypeChange = (type) => {
      if (currentType.value !== type) {
        currentType.value = type;
        logger.userAction('TYPE_CHANGE', { type });
        loadContentList();
      }
    };

    // 排序切换
    const handleSortChange = (sort) => {
      if (currentSort.value !== sort) {
        currentSort.value = sort;
        logger.userAction('SORT_CHANGE', { sort });
        loadContentList();
      }
    };

    // 搜索处理
    const handleSearch = () => {
      logger.userAction('SEARCH', { keyword: searchKeyword.value });
      loadContentList();
    };

    // 点击内容卡片
    const handleContentClick = (contentId) => {
      logger.userAction('CONTENT_CLICK', { contentId });
      router.push(`/community/${contentId}`);
    };

    // 发布内容
    const handlePublishClick = () => {
      const userInfo = getUserInfo();
      if (!userInfo) {
        router.push('/login');
        return;
      }
      logger.userAction('PUBLISH_CLICK');
      router.push('/community/publish');
    };

    // 返回
    const goBack = () => {
      router.push('/home');
    };

    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '';
      try {
        const date = new Date(timeStr);
        const now = new Date();
        const diff = now - date;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (minutes < 1) return '刚刚';
        if (minutes < 60) return `${minutes}分钟前`;
        if (hours < 24) return `${hours}小时前`;
        if (days < 7) return `${days}天前`;
        return date.toLocaleDateString('zh-CN');
      } catch (error) {
        return timeStr;
      }
    };

    // 获取类型标签
    const getTypeLabel = (type) => {
      const typeMap = {
        articles: '文章',
        questions: '提问',
        experiences: '经验分享'
      };
      return typeMap[type] || type;
    };

    // 获取类型样式类
    const getTypeClass = (type) => {
      return `type-${type}`;
    };

    // 获取角色标签
    const getRoleLabel = (role) => {
      const roleMap = {
        farmer: '农户',
        expert: '专家',
        buyer: '买家',
        bank: '银行'
      };
      return roleMap[role] || role;
    };

    // 获取角色样式类
    const getRoleClass = (role) => {
      return `role-${role}`;
    };

    // 预览图片
    const previewImage = (image, images) => {
      currentImage.value = image;
      imageList.value = images || [];
      showImagePreview.value = true;
    };

    // 关闭图片预览
    const closeImagePreview = () => {
      showImagePreview.value = false;
      currentImage.value = '';
      imageList.value = [];
    };

    onMounted(() => {
      logger.lifecycle('Community', 'mounted');
      loadContentList();
    });

    return {
      loading,
      contentList,
      currentType,
      currentSort,
      searchKeyword,
      contentTypes,
      sortOptions,
      showImagePreview,
      currentImage,
      handleTypeChange,
      handleSortChange,
      handleSearch,
      handleContentClick,
      handlePublishClick,
      goBack,
      formatTime,
      getTypeLabel,
      getTypeClass,
      getRoleLabel,
      getRoleClass,
      previewImage,
      closeImagePreview
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.community-container {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
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

.back-icon {
  font-size: 1.125rem;
}

.header-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0;
}

.btn-publish {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border: none;
  border-radius: 8px;
  color: var(--white);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.3);
}

.btn-publish:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(107, 70, 193, 0.4);
}

.publish-icon {
  font-size: 1.125rem;
}

/* 主内容区域 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

/* 筛选区域 */
.filter-section {
  background: var(--white);
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.filter-tabs {
  display: flex;
  gap: 0.5rem;
}

.filter-tab {
  padding: 0.625rem 1.25rem;
  background: var(--gray-100);
  border: 2px solid transparent;
  border-radius: 8px;
  color: var(--gray-600);
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tab:hover {
  background: var(--gray-200);
}

.filter-tab.active {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: var(--white);
  border-color: var(--primary);
}

.search-box {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 0.75rem 1rem 0.75rem 2.5rem;
  border: 2px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.9375rem;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1.125rem;
}

/* 排序区域 */
.sort-section {
  background: var(--white);
  padding: 1rem 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.05);
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.sort-label {
  color: var(--gray-600);
  font-size: 0.875rem;
  font-weight: 500;
}

.sort-btn {
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  color: var(--gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.sort-btn:hover {
  background: var(--gray-100);
  border-color: var(--primary-light);
}

.sort-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--white);
}

/* 内容列表 */
.content-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.content-card {
  background: var(--white);
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.content-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(107, 70, 193, 0.15);
  border-color: var(--primary-light);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.content-type-badge {
  padding: 0.375rem 0.875rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--white);
}

.type-articles {
  background: linear-gradient(135deg, #8B5CF6, #A78BFA);
}

.type-questions {
  background: linear-gradient(135deg, #EC4899, #F472B6);
}

.type-experiences {
  background: linear-gradient(135deg, #06B6D4, #22D3EE);
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: var(--gray-500);
  font-size: 0.875rem;
}

.author {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.author-icon {
  font-size: 1rem;
}

.content-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 0.75rem 0;
  line-height: 1.4;
}

.content-summary {
  color: var(--gray-600);
  font-size: 0.9375rem;
  line-height: 1.6;
  margin: 0 0 1rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.content-images {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.content-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.content-image:hover {
  transform: scale(1.05);
}

.image-count {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  background: rgba(107, 70, 193, 0.8);
  color: var(--white);
  border-radius: 8px;
  font-weight: 600;
}

.content-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.content-stats {
  display: flex;
  gap: 1.5rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--gray-500);
  font-size: 0.875rem;
}

.stat-icon {
  font-size: 1rem;
}

.role-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--white);
}

.role-farmer {
  background: linear-gradient(135deg, #10B981, #34D399);
}

.role-expert {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
}

.role-buyer {
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
}

.role-bank {
  background: linear-gradient(135deg, #6366F1, #818CF8);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-text {
  color: var(--gray-500);
  font-size: 1.125rem;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--gray-200);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 图片预览模态框 */
.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: pointer;
}

.preview-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  border-radius: 8px;
}

.close-preview {
  position: absolute;
  top: 2rem;
  right: 2rem;
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: var(--white);
  font-size: 2rem;
  cursor: pointer;
  transition: all 0.2s;
}

.close-preview:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .header-title {
    font-size: 1.25rem;
  }

  .btn-publish {
    padding: 0.625rem 1rem;
    font-size: 0.875rem;
  }

  .main-content {
    padding: 1rem;
  }

  .filter-tabs {
    flex-wrap: wrap;
  }

  .content-images {
    gap: 0.5rem;
  }

  .content-image {
    width: 80px;
    height: 80px;
  }
}
</style>

