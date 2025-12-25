<template>
  <div class="financing-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <button class="btn-back" @click="handleBack">
          <span class="back-icon">←</span>
          返回
        </button>
        <h1 class="page-title">融资服务</h1>
      </div>
      <div class="header-right">
        <div class="user-info">
          <span class="user-name">{{ userInfo.nickname || '用户' }}</span>
          <span class="user-role">{{ userRoleText }}</span>
        </div>
      </div>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-wrapper">
        <!-- 农户功能界面 -->
        <div v-if="isFarmer" class="farmer-view">
          <!-- 信用额度概览卡片 -->
          <div class="overview-card">
            <div class="overview-header">
              <h2 class="overview-title">我的信用额度</h2>
              <button class="btn-refresh" @click="loadCreditLimit">
                <span class="refresh-icon">🔄</span>
                刷新
              </button>
            </div>
            <div v-if="loadingCreditLimit" class="loading-overview">
              <div class="loading-spinner-small"></div>
              <span>加载中...</span>
            </div>
            <div v-else class="overview-content">
              <div class="credit-item">
                <span class="credit-label">可用额度：</span>
                <span class="credit-value available">¥{{ formatAmount(creditLimit?.available_limit || 0) }}</span>
              </div>
              <div class="credit-item">
                <span class="credit-label">总额度：</span>
                <span class="credit-value total">¥{{ formatAmount(creditLimit?.total_limit || 0) }}</span>
              </div>
              <div class="credit-item">
                <span class="credit-label">已用额度：</span>
                <span class="credit-value used">¥{{ formatAmount((creditLimit?.total_limit || 0) - (creditLimit?.available_limit || 0)) }}</span>
              </div>
            </div>
          </div>

          <!-- 功能模块 -->
          <div class="modules-section">
            <h2 class="section-title">功能模块</h2>
            <div class="modules-grid">
              <div 
                v-for="module in farmerModules" 
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

        <!-- 银行功能界面 -->
        <div v-else-if="isBank" class="bank-view">
          <div class="modules-section">
            <h2 class="section-title">银行管理功能</h2>
            <div class="modules-grid">
              <div 
                v-for="module in bankModules" 
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

        <!-- 其他用户类型 -->
        <div v-else class="no-access">
          <div class="empty-icon">🔒</div>
          <h3>暂不支持</h3>
          <p>当前用户类型不支持融资服务</p>
        </div>
      </div>
    </main>

    <!-- 子组件弹窗 -->
    <!-- 申请贷款额度 -->
    <CreditLimitApplicationModal
      v-if="showCreditLimitModal"
      @close="showCreditLimitModal = false"
      @success="handleCreditLimitSuccess"
      @viewHistory="handleViewHistoryFromApply"
    />

    <!-- 申请记录查看 -->
    <CreditApplicationHistoryModal
      v-if="showApplicationHistoryModal"
      @close="showApplicationHistoryModal = false"
      @apply="handleApplicationHistoryApply"
    />

    <!-- 查询贷款产品 -->
    <LoanProductListModal
      v-if="showLoanProductModal"
      @close="showLoanProductModal = false"
      @apply="handleLoanApply"
    />

    <!-- 统一贷款申请 -->
    <UnifiedLoanApplicationModal
      v-if="showUnifiedLoanModal && selectedProduct"
      :product="selectedProduct"
      @close="closeUnifiedLoanModal"
      @success="handleLoanSuccess"
      @switch-to-joint="handleSwitchToJointLoan"
      @switch-to-joint-partners="handleSwitchToJointPartners"
    />

    <!-- 申请单人贷款（保留兼容性） -->
    <SingleLoanApplicationModal
      v-if="showSingleLoanModal && selectedProduct"
      :product="selectedProduct"
      @close="closeLoanModal"
      @success="handleLoanSuccess"
    />

    <!-- 申请联合贷款（保留兼容性） -->
    <JointLoanApplicationModal
      v-if="showJointLoanModal && selectedProduct"
      ref="jointLoanComponentRef"
      :product="selectedProduct"
      :selected-partner="selectedPartnerForJointLoan"
      @close="closeLoanModal"
      @success="handleLoanSuccess"
      @open-partners="showPartnersModal = true"
    />


    <!-- 还款计划 -->
    <RepaymentScheduleModal
      v-if="showRepaymentModal"
      @close="showRepaymentModal = false"
    />

    <!-- 银行发布贷款产品 -->
    <LoanProductPublishModal
      v-if="showPublishProductModal"
      @close="showPublishProductModal = false"
      @success="handlePublishSuccess"
    />

    <!-- 银行审批贷款 -->
    <LoanApprovalModal
      v-if="showApprovalModal"
      @close="showApprovalModal = false"
      @success="handleApprovalSuccess"
    />

    <!-- 银行放款 -->
    <LoanDisbursementListModal
      v-if="showDisbursementModal"
      @close="showDisbursementModal = false"
      @success="handleDisbursementSuccess"
    />

    <!-- 银行审批信贷额度申请 -->
    <CreditApprovalModal
      v-if="showCreditApprovalModal"
      @close="showCreditApprovalModal = false"
      @success="handleCreditApprovalSuccess"
    />

    <!-- 浏览可联合农户 -->
    <JointPartnersModal
      v-if="showPartnersModal && selectedProduct"
      :product="selectedProduct"
      @close="showPartnersModal = false"
      @select="handlePartnerSelect"
    />

    <!-- 贷款申请记录 -->
    <LoanApplicationHistoryModal
      v-if="showLoanApplicationHistoryModal"
      @close="showLoanApplicationHistoryModal = false"
      @apply="handleLoanApplicationHistoryApply"
    />

    <!-- 待确认的联合贷款申请 -->
    <JointLoanConfirmationModal
      v-if="showJointLoanConfirmationModal"
      @close="showJointLoanConfirmationModal = false"
      @success="handleJointLoanConfirmationSuccess"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { financingService } from '../api/financing';
import logger from '../utils/logger';
import CreditLimitApplicationModal from './components/CreditLimitApplicationModal.vue';
import CreditApplicationHistoryModal from './components/CreditApplicationHistoryModal.vue';
import LoanProductListModal from './components/LoanProductListModal.vue';
import UnifiedLoanApplicationModal from './components/UnifiedLoanApplicationModal.vue';
import SingleLoanApplicationModal from './components/SingleLoanApplicationModal.vue';
import JointLoanApplicationModal from './components/JointLoanApplicationModal.vue';
import JointPartnersModal from './components/JointPartnersModal.vue';
import JointLoanConfirmationModal from './components/JointLoanConfirmationModal.vue';
import LoanApplicationHistoryModal from './components/LoanApplicationHistoryModal.vue';
import RepaymentScheduleModal from './components/RepaymentScheduleModal.vue';
import LoanProductPublishModal from './components/LoanProductPublishModal.vue';
import LoanApprovalModal from './components/LoanApprovalModal.vue';
import LoanDisbursementListModal from './components/LoanDisbursementListModal.vue';
import CreditApprovalModal from './components/CreditApprovalModal.vue';

export default {
  name: 'Financing',
  components: {
    CreditLimitApplicationModal,
    CreditApplicationHistoryModal,
    LoanProductListModal,
    UnifiedLoanApplicationModal,
    SingleLoanApplicationModal,
    JointLoanApplicationModal,
    JointPartnersModal,
    JointLoanConfirmationModal,
    LoanApplicationHistoryModal,
    RepaymentScheduleModal,
    LoanProductPublishModal,
    LoanApprovalModal,
    LoanDisbursementListModal,
    CreditApprovalModal
  },
  setup() {
    const router = useRouter();
    const userInfo = ref({});
    const creditLimit = ref(null);
    const loadingCreditLimit = ref(false);
    const showCreditLimitModal = ref(false);
    const showApplicationHistoryModal = ref(false);
    const showLoanProductModal = ref(false);
    const showUnifiedLoanModal = ref(false);
    const showSingleLoanModal = ref(false);
    const showJointLoanModal = ref(false);
    const showPartnersModal = ref(false);
    const showLoanApplicationHistoryModal = ref(false);
    const showRepaymentModal = ref(false);
    const showJointLoanConfirmationModal = ref(false);
    const showPublishProductModal = ref(false);
    const showApprovalModal = ref(false);
    const showDisbursementModal = ref(false);
    const showCreditApprovalModal = ref(false);
    const selectedProduct = ref(null);
    const jointLoanComponentRef = ref(null);
    const selectedPartnerForJointLoan = ref(null); // 存储选择的联合伙伴

    // 用户类型判断
    const isFarmer = computed(() => userInfo.value.userType === 'farmer');
    const isBank = computed(() => userInfo.value.userType === 'bank');

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

    // 农户功能模块
    const farmerModules = computed(() => [
      {
        id: 'apply_credit',
        name: '申请贷款额度',
        description: '提交证明材料，申请提高贷款额度',
        icon: '📝',
        action: () => { showCreditLimitModal.value = true; }
      },
      {
        id: 'application_history',
        name: '额度申请记录',
        description: '查看额度申请历史记录及审批状态',
        icon: '📊',
        action: () => { showApplicationHistoryModal.value = true; }
      },
      {
        id: 'loan_application_history',
        name: '贷款申请记录',
        description: '查看贷款申请状态，追踪审批进度',
        icon: '📜',
        action: () => { showLoanApplicationHistoryModal.value = true; }
      },
      {
        id: 'joint_loan_confirmation',
        name: '待确认联合贷款',
        description: '查看并处理待确认的联合贷款申请',
        icon: '🤝',
        action: () => { showJointLoanConfirmationModal.value = true; }
      },
      {
        id: 'loan_products',
        name: '查看贷款产品',
        description: '浏览可申请的贷款产品，选择合适的贷款方案',
        icon: '📋',
        action: () => { showLoanProductModal.value = true; }
      },
      {
        id: 'repayment',
        name: '还款计划',
        description: '查看贷款还款计划和明细',
        icon: '📊',
        action: () => { showRepaymentModal.value = true; }
      }
    ]);

    // 银行功能模块
    const bankModules = computed(() => [
      {
        id: 'publish_product',
        name: '发布贷款产品',
        description: '创建新的贷款产品供农户申请',
        icon: '➕',
        action: () => { showPublishProductModal.value = true; }
      },
      {
        id: 'approve_credit',
        name: '审批信贷额度申请',
        description: '审核农户提交的信贷额度申请',
        icon: '📝',
        action: () => { showCreditApprovalModal.value = true; }
      },
      {
        id: 'approve_loan',
        name: '审批贷款申请',
        description: '审核农户提交的贷款申请',
        icon: '✅',
        action: () => { showApprovalModal.value = true; }
      },
      {
        id: 'disburse_loan',
        name: '放款操作',
        description: '对已审批通过的贷款进行放款',
        icon: '💰',
        action: () => { showDisbursementModal.value = true; }
      }
    ]);

    // 获取用户信息
    onMounted(() => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
          logger.info('FINANCING', '加载用户信息成功', { userType: userInfo.value.userType });
          if (isFarmer.value) {
            loadCreditLimit();
          }
        } catch (error) {
          logger.error('FINANCING', '解析用户信息失败', {}, error);
          router.push('/login');
        }
      } else {
        logger.warn('FINANCING', '未找到用户信息，跳转到登录页');
        router.push('/login');
      }
    });

    // 加载信用额度
    const loadCreditLimit = async () => {
      if (!isFarmer.value || !userInfo.value.phone) return;
      
      loadingCreditLimit.value = true;
      try {
        logger.info('FINANCING', '开始加载信用额度', { phone: userInfo.value.phone });
        const data = await financingService.getCreditLimit(userInfo.value.phone);
        creditLimit.value = data;
        logger.info('FINANCING', '信用额度加载成功', { data });
      } catch (error) {
        logger.error('FINANCING', '加载信用额度失败', {
          errorMessage: error.message || error
        }, error);
        // 如果失败，显示空值
        creditLimit.value = { available_limit: 0, total_limit: 0 };
      } finally {
        loadingCreditLimit.value = false;
      }
    };

    // 格式化金额
    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    // 返回上一页
    const handleBack = () => {
      logger.userAction('BACK_CLICK', { from: 'financing' });
      router.push('/home');
    };

    // 点击功能模块
    const handleModuleClick = (module) => {
      logger.userAction('MODULE_CLICK', { 
        moduleId: module.id,
        moduleName: module.name,
        userType: userInfo.value.userType 
      });
      console.log('点击模块:', module.id, module.name);
      if (module.action) {
        console.log('执行模块action');
        module.action();
        console.log('showJointLoanConfirmationModal:', showJointLoanConfirmationModal.value);
      } else {
        console.warn('模块没有action函数:', module);
      }
    };

    // 申请额度成功
    const handleCreditLimitSuccess = () => {
      showCreditLimitModal.value = false;
      loadCreditLimit();
    };

    // 申请记录页面的申请按钮处理
    const handleApplicationHistoryApply = () => {
      showApplicationHistoryModal.value = false;
      showCreditLimitModal.value = true;
    };

    // 从申请页面跳转到申请记录
    const handleViewHistoryFromApply = () => {
      showCreditLimitModal.value = false;
      showApplicationHistoryModal.value = true;
    };

    // 贷款申请
    const handleLoanApply = (product, loanType) => {
      selectedProduct.value = product;
      showLoanProductModal.value = false;
      if (loanType === 'unified' || !loanType) {
        // 默认使用统一申请入口
        showUnifiedLoanModal.value = true;
      } else if (loanType === 'single') {
        showSingleLoanModal.value = true;
      } else if (loanType === 'joint') {
        showJointLoanModal.value = true;
      }
    };

    // 关闭贷款申请弹窗
    const closeLoanModal = () => {
      showSingleLoanModal.value = false;
      showJointLoanModal.value = false;
      selectedProduct.value = null;
      selectedPartnerForJointLoan.value = null; // 清空选择的伙伴
    };

    // 关闭统一贷款申请弹窗
    const closeUnifiedLoanModal = () => {
      showUnifiedLoanModal.value = false;
      selectedProduct.value = null;
    };

    // 从智能申请切换到联合贷款
    const handleSwitchToJointLoan = () => {
      // 关闭智能申请弹窗，打开联合贷款弹窗
      showUnifiedLoanModal.value = false;
      showJointLoanModal.value = true;
      // selectedProduct 保持不变，用于联合贷款申请
    };

    // 从智能申请切换到选择联合伙伴页面
    const handleSwitchToJointPartners = (product) => {
      logger.info('FINANCING', '从智能推荐切换到选择联合伙伴', { 
        product_id: product?.product_id || product?.id 
      });
      // 关闭智能申请弹窗
      showUnifiedLoanModal.value = false;
      // 确保selectedProduct已设置（如果传入了product则使用，否则保持原有值）
      if (product) {
        selectedProduct.value = product;
      }
      // 打开选择联合伙伴弹窗
      showPartnersModal.value = true;
    };

    // 贷款申请成功
    const handleLoanSuccess = () => {
      closeLoanModal();
      loadCreditLimit();
    };


    // 发布产品成功
    const handlePublishSuccess = () => {
      showPublishProductModal.value = false;
    };

    // 审批成功
    const handleApprovalSuccess = () => {
      showApprovalModal.value = false;
    };

    // 放款成功
    const handleDisbursementSuccess = () => {
      showDisbursementModal.value = false;
    };

    // 信贷额度审批成功
    const handleCreditApprovalSuccess = () => {
      showCreditApprovalModal.value = false;
    };

    // 联合贷款确认成功
    const handleJointLoanConfirmationSuccess = () => {
      showJointLoanConfirmationModal.value = false;
      loadCreditLimit();
    };

    // 选择联合伙伴
    const handlePartnerSelect = (partners) => {
      logger.info('FINANCING', '父组件接收到伙伴选择', { 
        partnersCount: partners?.length || 0,
        partners: partners
      });
      
      // 直接存储选择的伙伴（选择第一个）
      if (partners && partners.length > 0) {
        selectedPartnerForJointLoan.value = partners[0];
        logger.info('FINANCING', '伙伴选择成功', {
          partner: selectedPartnerForJointLoan.value
        });
      }
      
      showPartnersModal.value = false;
    };

    // 贷款申请记录页面申请新贷款
    const handleLoanApplicationHistoryApply = () => {
      showLoanApplicationHistoryModal.value = false;
      showLoanProductModal.value = true;
    };

    return {
      userInfo,
      creditLimit,
      loadingCreditLimit,
      isFarmer,
      isBank,
      userRoleText,
      farmerModules,
      bankModules,
      showCreditLimitModal,
      showApplicationHistoryModal,
      showLoanProductModal,
      showUnifiedLoanModal,
      showSingleLoanModal,
      showJointLoanModal,
      showPartnersModal,
      showRepaymentModal,
      showPublishProductModal,
      showApprovalModal,
      showDisbursementModal,
      showCreditApprovalModal,
      showLoanApplicationHistoryModal,
      showJointLoanConfirmationModal,
      selectedProduct,
      selectedPartnerForJointLoan,
      formatAmount,
      handleBack,
      handleModuleClick,
      loadCreditLimit,
      handleCreditLimitSuccess,
      handleApplicationHistoryApply,
      handleViewHistoryFromApply,
      handleLoanApply,
      closeLoanModal,
      closeUnifiedLoanModal,
      handleSwitchToJointLoan,
      handleSwitchToJointPartners,
      handleLoanSuccess,
      handlePublishSuccess,
      handleApprovalSuccess,
      handleDisbursementSuccess,
      handleCreditApprovalSuccess,
      handleJointLoanConfirmationSuccess,
      handlePartnerSelect,
      handleLoanApplicationHistoryApply
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.financing-container {
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

/* 主内容区域 */
.main-content {
  padding: 2rem;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

/* 额度概览卡片 */
.overview-card {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
  margin-bottom: 2rem;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.overview-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.btn-refresh {
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

.btn-refresh:hover {
  background: var(--gray-100);
  border-color: var(--primary);
  color: var(--primary);
}

.refresh-icon {
  font-size: 1rem;
}

.loading-overview {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  justify-content: center;
  color: var(--gray-500);
}

.loading-spinner-small {
  width: 20px;
  height: 20px;
  border: 2px solid var(--gray-200);
  border-top: 2px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.overview-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.credit-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.credit-label {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.credit-value {
  font-size: 1.5rem;
  font-weight: 700;
}

.credit-value.available {
  color: var(--success);
}

.credit-value.total {
  color: var(--primary);
}

.credit-value.used {
  color: var(--gray-600);
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
  grid-template-columns: repeat(4, 1fr);
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

/* 无权限提示 */
.no-access {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--white);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.no-access h3 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.5rem 0;
}

.no-access p {
  color: var(--gray-500);
  margin: 0;
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
    padding: 1rem;
  }

  .main-content {
    padding: 1rem;
  }

  .overview-content {
    grid-template-columns: 1fr;
  }

  .modules-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .module-card {
    padding: 1.5rem;
  }
}
</style>
