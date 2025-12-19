<template>
  <div class="product-list-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <span class="back-icon">←</span>
          返回
        </button>
        <h1 class="page-title">农产品交易</h1>
      </div>
      <div class="header-right">
        <div class="user-info">
          <span class="user-name">{{ userInfo.nickname || '用户' }}</span>
          <span class="user-role">{{ userRoleText }}</span>
        </div>
      </div>
    </header>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <div class="search-container">
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索农产品标题..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch">
            <span class="search-icon">🔍</span>
          </button>
        </div>
        
        <div class="filter-section">
          <select v-model="statusFilter" class="filter-select" @change="handleFilterChange">
            <option value="">全部状态</option>
            <option value="draft">草稿</option>
            <option value="on_shelf">在售</option>
            <option value="off_shelf">下架</option>
            <option value="sold_out">售罄</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-section" v-if="isFarmer">
      <button class="btn-primary" @click="showCreateForm = true">
        <span class="btn-icon">+</span>
        发布农产品
      </button>
      <button 
        class="btn-secondary" 
        @click="handleBatchAction"
        :disabled="selectedProducts.length === 0"
      >
        <span class="btn-icon">📦</span>
        批量操作 ({{ selectedProducts.length }})
      </button>
    </div>

    <!-- 产品列表 -->
    <div class="products-section">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="products.length === 0" class="empty-container">
        <div class="empty-icon">🌾</div>
        <h3>暂无农产品</h3>
        <p v-if="isFarmer">点击"发布农产品"开始发布您的产品</p>
        <p v-else>暂无可购买的农产品</p>
      </div>

      <div v-else class="products-grid">
        <ProductCard
          v-for="product in products"
          :key="product.product_id"
          :product="product"
          :is-farmer="isFarmer"
          :selected="selectedProducts.includes(product.product_id)"
          @select="handleProductSelect"
          @view="handleViewProduct"
          @edit="handleEditProduct"
          @delete="handleDeleteProduct"
          @on-shelf="handleOnShelf"
          @off-shelf="handleOffShelf"
          @purchase="handlePurchase"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-section" v-if="products.length > 0">
      <div class="pagination-info">
        共 {{ displayTotalCount }} 个产品
      </div>
      <div class="pagination-controls">
        <button 
          class="pagination-btn" 
          @click="handlePageChange(currentPage - 1)"
          :disabled="currentPage <= 1"
        >
          上一页
        </button>
        <span class="pagination-info">
          第 {{ currentPage }} 页 / 共 {{ totalPages || 1 }} 页
        </span>
        <button 
          class="pagination-btn" 
          @click="handlePageChange(currentPage + 1)"
          :disabled="currentPage >= totalPages || products.length < pageSize"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 创建/编辑产品表单弹窗 -->
    <ProductForm
      v-if="showCreateForm"
      :product="editingProduct"
      :is-edit="!!editingProduct"
      @close="handleFormClose"
      @success="handleFormSuccess"
    />

    <!-- 产品详情弹窗 -->
    <ProductDetail
      v-if="showProductDetail"
      :product-id="viewingProductId"
      @close="showProductDetail = false"
      @purchase="handlePurchase"
    />

    <!-- 批量操作弹窗 -->
    <BatchActionModal
      v-if="showBatchModal"
      :selected-count="selectedProducts.length"
      @close="showBatchModal = false"
      @confirm="handleBatchConfirm"
    />

    <!-- 订单表单弹窗 -->
    <OrderForm
      v-if="showOrderForm && purchasingProduct"
      :product="purchasingProduct"
      @close="showOrderForm = false"
      @success="handleOrderSuccess"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, reactive, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { productService } from '../api/product';
import logger from '../utils/logger';
import ProductCard from '../components/ProductCard.vue';
import ProductForm from '../components/ProductForm.vue';
import ProductDetail from '../components/ProductDetail.vue';
import BatchActionModal from '../components/BatchActionModal.vue';
import OrderForm from '../components/OrderForm.vue';

export default {
  name: 'ProductList',
  components: {
    ProductCard,
    ProductForm,
    ProductDetail,
    BatchActionModal,
    OrderForm
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const userInfo = ref({});
    const products = ref([]);
    const loading = ref(false);
    const searchKeyword = ref('');
    const statusFilter = ref('');
    const selectedProducts = ref([]);
    const showCreateForm = ref(false);
    const showProductDetail = ref(false);
    const showBatchModal = ref(false);
    const showOrderForm = ref(false);
    const editingProduct = ref(null);
    const viewingProductId = ref(null);
    const purchasingProduct = ref(null);

    // 分页相关
    const currentPage = ref(1);
    const pageSize = ref(15);
    const totalCount = ref(0);
    // 根据实际返回的商品数量计算总页数
    // 如果当前页返回的商品数等于pageSize，说明可能还有下一页
    // 如果当前页返回的商品数小于pageSize，说明这是最后一页
    const totalPages = computed(() => {
      if (products.value.length === 0) return 0;
      // 如果当前页商品数等于pageSize，可能还有更多页
      if (products.value.length === pageSize.value) {
        // 至少是当前页+1，但不知道具体有多少页，先显示当前页+1
        return currentPage.value + 1;
      } else {
        // 当前页不满，说明是最后一页
        return currentPage.value;
      }
    });
    // 计算总商品数（用于显示）
    const displayTotalCount = computed(() => {
      if (products.value.length === 0) return 0;
      return products.value.length + (currentPage.value - 1) * pageSize.value;
    });

    // 用户类型判断
    const isFarmer = computed(() => userInfo.value.userType === 'farmer');

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

    // 获取用户信息
    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
          logger.info('PRODUCT_LIST', '加载用户信息成功', { userType: userInfo.value.userType });
          loadProducts().then(() => {
            // 检查是否有productId查询参数，如果有则打开商品详情
            const productId = route.query.productId;
            if (productId) {
              handleViewProduct(productId);
              // 清除查询参数
              router.replace({ path: '/trading', query: {} });
            }
          });
        } catch (error) {
          logger.error('PRODUCT_LIST', '解析用户信息失败', {}, error);
          router.push('/login');
        }
      } else {
        logger.warn('PRODUCT_LIST', '未找到用户信息，跳转到登录页');
        router.push('/login');
      }
    });

    // 监听路由查询参数变化
    watch(() => route.query.productId, (newProductId) => {
      if (newProductId) {
        handleViewProduct(newProductId);
        // 清除查询参数
        router.replace({ path: '/trading', query: {} });
      }
    });

    // 加载产品列表
    const loadProducts = async () => {
      loading.value = true;
      try {
        logger.info('PRODUCT_LIST', '开始加载产品列表', {
          phone: userInfo.value.phone,
          status: statusFilter.value,
          title: searchKeyword.value,
          page: currentPage.value
        });

        const response = await productService.getProductList(
          userInfo.value.phone,
          statusFilter.value || null,
          searchKeyword.value || null
        );

        products.value = response.list || [];
        totalCount.value = response.total_count || 0;

        logger.info('PRODUCT_LIST', '产品列表加载成功', {
          count: products.value.length,
          total: totalCount.value
        });
      } catch (error) {
        logger.error('PRODUCT_LIST', '加载产品列表失败', {
          errorMessage: error.message || error
        }, error);
        products.value = [];
        totalCount.value = 0;
      } finally {
        loading.value = false;
      }
    };

    // 返回上一页
    const goBack = () => {
      logger.userAction('BACK_CLICK', { from: 'product_list' });
      router.push('/home');
    };

    // 搜索处理
    const handleSearch = () => {
      logger.userAction('SEARCH_CLICK', { keyword: searchKeyword.value });
      currentPage.value = 1;
      loadProducts();
    };

    // 筛选处理
    const handleFilterChange = () => {
      logger.userAction('FILTER_CHANGE', { status: statusFilter.value });
      currentPage.value = 1;
      loadProducts();
    };

    // 分页处理
    const handlePageChange = (page) => {
      if (page >= 1 && page <= totalPages.value) {
        logger.userAction('PAGE_CHANGE', { page });
        currentPage.value = page;
        loadProducts();
      }
    };

    // 产品选择处理
    const handleProductSelect = (productId, selected) => {
      if (selected) {
        selectedProducts.value.push(productId);
      } else {
        const index = selectedProducts.value.indexOf(productId);
        if (index > -1) {
          selectedProducts.value.splice(index, 1);
        }
      }
      logger.userAction('PRODUCT_SELECT', { productId, selected });
    };

    // 查看产品详情
    const handleViewProduct = (productId) => {
      logger.userAction('VIEW_PRODUCT', { productId });
      viewingProductId.value = productId;
      showProductDetail.value = true;
    };

    // 编辑产品
    const handleEditProduct = (product) => {
      logger.userAction('EDIT_PRODUCT', { productId: product.product_id });
      editingProduct.value = product;
      showCreateForm.value = true;
    };

    // 删除产品
    const handleDeleteProduct = async (productId) => {
      if (!confirm('确定要删除这个产品吗？')) {
        return;
      }

      try {
        logger.userAction('DELETE_PRODUCT', { productId });
        await productService.deleteProduct(productId, userInfo.value.phone);
        logger.info('PRODUCT_LIST', '产品删除成功', { productId });
        loadProducts();
      } catch (error) {
        logger.error('PRODUCT_LIST', '删除产品失败', {
          productId,
          errorMessage: error.message || error
        }, error);
        alert('删除失败：' + (error.message || error));
      }
    };

    // 上架产品
    const handleOnShelf = async (productId) => {
      try {
        logger.userAction('ON_SHELF_PRODUCT', { productId });
        await productService.onShelfProduct(productId, userInfo.value.phone);
        logger.info('PRODUCT_LIST', '产品上架成功', { productId });
        loadProducts();
      } catch (error) {
        logger.error('PRODUCT_LIST', '产品上架失败', {
          productId,
          errorMessage: error.message || error
        }, error);
        alert('上架失败：' + (error.message || error));
      }
    };

    // 下架产品
    const handleOffShelf = async (productId) => {
      try {
        logger.userAction('OFF_SHELF_PRODUCT', { productId });
        await productService.offShelfProduct(productId, userInfo.value.phone);
        logger.info('PRODUCT_LIST', '产品下架成功', { productId });
        loadProducts();
      } catch (error) {
        logger.error('PRODUCT_LIST', '产品下架失败', {
          productId,
          errorMessage: error.message || error
        }, error);
        alert('下架失败：' + (error.message || error));
      }
    };

    // 购买产品
    const handlePurchase = (product) => {
      logger.userAction('PURCHASE_PRODUCT', { productId: product.product_id });
      if (product.status === 'on_shelf' && product.stock > 0) {
        purchasingProduct.value = product;
        showOrderForm.value = true;
      } else {
        alert('商品不可购买');
      }
    };

    // 订单创建成功
    const handleOrderSuccess = (orderData) => {
      logger.info('PRODUCT_LIST', '订单创建成功', { orderId: orderData?.order_id });
      showOrderForm.value = false;
      purchasingProduct.value = null;
      // 重新加载产品列表以更新库存
      loadProducts();
    };

    // 批量操作
    const handleBatchAction = () => {
      logger.userAction('BATCH_ACTION_CLICK', { count: selectedProducts.value.length });
      showBatchModal.value = true;
    };

    // 批量操作确认
    const handleBatchConfirm = async (action) => {
      try {
        logger.userAction('BATCH_CONFIRM', { action, count: selectedProducts.value.length });
        await productService.batchActionProducts(action, selectedProducts.value, userInfo.value.phone);
        logger.info('PRODUCT_LIST', '批量操作成功', { action, count: selectedProducts.value.length });
        selectedProducts.value = [];
        showBatchModal.value = false;
        loadProducts();
      } catch (error) {
        logger.error('PRODUCT_LIST', '批量操作失败', {
          action,
          errorMessage: error.message || error
        }, error);
        alert('批量操作失败：' + (error.message || error));
      }
    };

    // 表单关闭
    const handleFormClose = () => {
      showCreateForm.value = false;
      editingProduct.value = null;
    };

    // 表单成功
    const handleFormSuccess = () => {
      showCreateForm.value = false;
      editingProduct.value = null;
      loadProducts();
    };

    return {
      userInfo,
      products,
      loading,
      searchKeyword,
      statusFilter,
      selectedProducts,
      showCreateForm,
      showProductDetail,
      showBatchModal,
      showOrderForm,
      editingProduct,
      viewingProductId,
      purchasingProduct,
      currentPage,
      pageSize,
      totalCount,
      totalPages,
      displayTotalCount,
      isFarmer,
      userRoleText,
      goBack,
      handleSearch,
      handleFilterChange,
      handlePageChange,
      handleProductSelect,
      handleViewProduct,
      handleEditProduct,
      handleDeleteProduct,
      handleOnShelf,
      handleOffShelf,
      handlePurchase,
      handleBatchAction,
      handleBatchConfirm,
      handleFormClose,
      handleFormSuccess,
      handleOrderSuccess
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.product-list-container {
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
  border-color: var(--primary-light);
  color: var(--primary);
}

.back-icon {
  font-size: 1rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1a202c;
}

.user-role {
  font-size: 0.75rem;
  color: var(--primary);
}

/* 搜索区域 */
.search-section {
  padding: 1.5rem 2rem;
}

.search-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 1rem;
  align-items: center;
}

.search-box {
  flex: 1;
  display: flex;
  background: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
  overflow: hidden;
}

.search-input {
  flex: 1;
  padding: 0.875rem 1rem;
  border: none;
  outline: none;
  font-size: 1rem;
  background: transparent;
}

.search-input::placeholder {
  color: var(--gray-400);
}

.search-btn {
  padding: 0.875rem 1rem;
  background: var(--primary);
  border: none;
  color: var(--white);
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: var(--primary-dark);
}

.search-icon {
  font-size: 1rem;
}

.filter-section {
  display: flex;
  gap: 0.5rem;
}

.filter-select {
  padding: 0.875rem 1rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  background: var(--white);
  font-size: 0.875rem;
  cursor: pointer;
  min-width: 120px;
}

/* 操作按钮区域 */
.action-section {
  padding: 0 2rem 1rem;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 1rem;
}

.btn-primary, .btn-secondary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--primary);
  color: var(--white);
}

.btn-primary:hover {
  background: var(--primary-dark);
}

.btn-secondary {
  background: var(--white);
  color: var(--primary);
  border: 1px solid var(--primary);
}

.btn-secondary:hover {
  background: var(--primary-light);
  color: var(--white);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 1rem;
}

/* 产品列表区域 */
.products-section {
  padding: 0 2rem 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.loading-container, .empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
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

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-container h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 0.5rem;
}

.empty-container p {
  color: var(--gray-400);
  margin: 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

/* 分页区域 */
.pagination-section {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--white);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
}

.pagination-info {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  background: var(--white);
  color: var(--gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: var(--primary);
  color: var(--white);
  border-color: var(--primary);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .search-section {
    padding: 1rem;
  }

  .search-container {
    flex-direction: column;
    gap: 0.75rem;
  }

  .action-section {
    padding: 0 1rem 1rem;
    flex-direction: column;
  }

  .products-section {
    padding: 0 1rem 1rem;
  }

  .products-grid {
    grid-template-columns: 1fr;
  }

  .pagination-section {
    padding: 1rem;
    flex-direction: column;
    gap: 1rem;
  }
}
</style>
