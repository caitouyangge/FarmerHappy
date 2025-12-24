<template>
  <div class="profile-container">
    <div class="profile-content">
      <div class="profile-header">
        <h1 class="page-title">个人信息</h1>
        <p class="page-subtitle">管理您的个人信息和账户</p>
      </div>

      <div class="profile-sections">
        <!-- 基本信息 -->
        <div class="profile-section">
          <div class="section-header">
            <h2 class="section-title">基本信息</h2>
            <button 
              v-if="!isEditing" 
              class="btn-edit" 
              @click="startEdit"
            >
              编辑
            </button>
            <div v-else class="edit-actions">
              <button class="btn-cancel" @click="cancelEdit">取消</button>
              <button class="btn-save" @click="saveProfile">保存</button>
            </div>
          </div>

          <div class="section-content">
            <div class="info-group">
              <label class="info-label">手机号</label>
              <div class="info-value readonly">{{ userInfo.phone }}</div>
            </div>

            <div class="info-group">
              <label class="info-label">用户类型</label>
              <div class="info-value readonly">{{ userRoleText }}</div>
            </div>

            <div class="info-group">
              <label class="info-label">昵称</label>
              <input
                v-if="isEditing"
                v-model="editedNickname"
                type="text"
                class="info-input"
                placeholder="请输入昵称"
                maxlength="30"
              />
              <div v-else class="info-value">{{ userInfo.nickname || '未设置' }}</div>
            </div>

            <!-- 买家收货地址 -->
            <div v-if="isBuyer && !isEditing" class="info-group">
              <label class="info-label">默认收货地址</label>
              <div class="info-value">{{ shippingAddress || '未设置' }}</div>
            </div>

            <div v-if="isBuyer && isEditing" class="info-group">
              <label class="info-label">默认收货地址</label>
              <textarea
                v-model="editedShippingAddress"
                class="info-textarea"
                placeholder="请输入默认收货地址"
                maxlength="500"
                rows="3"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 账户信息 -->
        <div v-if="shouldShowBalance" class="profile-section">
          <div class="section-header">
            <h2 class="section-title">账户信息</h2>
          </div>

          <div class="section-content">
            <div class="balance-display">
              <div class="balance-label">账户余额</div>
              <div class="balance-amount">
                <span v-if="loadingBalance" class="balance-loading">加载中...</span>
                <span v-else>¥{{ formattedBalance }}</span>
              </div>
            </div>

            <div class="recharge-section">
              <h3 class="recharge-title">账户充值</h3>
              <p class="recharge-desc">选择充值金额（模拟充值，无需实际付款）</p>
              
              <div class="recharge-options">
                <button
                  v-for="amount in rechargeAmounts"
                  :key="amount"
                  class="recharge-btn"
                  :class="{ active: selectedAmount === amount }"
                  @click="selectAmount(amount)"
                >
                  ¥{{ amount }}
                </button>
              </div>

              <div class="custom-amount">
                <label class="custom-label">自定义金额</label>
                <input
                  v-model="customAmount"
                  type="number"
                  class="custom-input"
                  placeholder="请输入金额"
                  min="0.01"
                  step="0.01"
                  @input="handleCustomAmountInput"
                />
              </div>

              <button 
                class="btn-recharge"
                :disabled="!canRecharge || recharging"
                @click="handleRecharge"
              >
                <span v-if="recharging">充值中...</span>
                <span v-else>确认充值</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../api/auth';
import logger from '../utils/logger';

export default {
  name: 'Profile',
  setup() {
    const router = useRouter();
    const userInfo = ref({});
    const isEditing = ref(false);
    const editedNickname = ref('');
    const editedShippingAddress = ref('');
    const shippingAddress = ref('');
    const balance = ref(null);
    const loadingBalance = ref(false);
    const selectedAmount = ref(null);
    const customAmount = ref('');
    const recharging = ref(false);
    const rechargeAmounts = [50, 100, 200, 500, 1000, 2000];

    const isBuyer = computed(() => {
      return userInfo.value.userType === 'buyer';
    });

    onMounted(async () => {
      logger.lifecycle('Profile', 'mounted');
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
          await loadUserProfile();
          await loadBalance();
        } catch (error) {
          logger.error('PROFILE', '解析用户信息失败', {}, error);
          router.push('/login');
        }
      } else {
        router.push('/login');
      }
    });

    const loadUserProfile = async () => {
      try {
        const profile = await authService.getUserProfile(userInfo.value.phone, userInfo.value.userType);
        if (profile) {
          // 更新用户信息
          userInfo.value.nickname = profile.nickname || userInfo.value.nickname;
          userInfo.value.money = profile.money || userInfo.value.money;
          
          // 如果是买家，获取收货地址
          if (profile.userType === 'buyer' && profile.shippingAddress) {
            shippingAddress.value = profile.shippingAddress;
            userInfo.value.shippingAddress = profile.shippingAddress;
            localStorage.setItem('user', JSON.stringify(userInfo.value));
          }
        }
      } catch (error) {
        logger.error('PROFILE', '获取用户详细信息失败', {}, error);
        // 如果获取失败，使用localStorage中的信息
        if (userInfo.value.shippingAddress) {
          shippingAddress.value = userInfo.value.shippingAddress;
        }
      }
    };

    const userRoleText = computed(() => {
      const roleMap = {
        farmer: '农户',
        buyer: '买家',
        expert: '技术专家',
        bank: '银行'
      };
      return roleMap[userInfo.value.userType] || '无';
    });

    const shouldShowBalance = computed(() => {
      const userType = userInfo.value.userType;
      return userType === 'farmer' || userType === 'buyer' || userType === 'bank';
    });

    const formattedBalance = computed(() => {
      const currentBalance = balance.value !== null ? balance.value : userInfo.value.money;
      if (currentBalance === null || currentBalance === undefined) {
        return '0.00';
      }
      const numBalance = typeof currentBalance === 'number' ? currentBalance : parseFloat(currentBalance);
      return numBalance.toFixed(2);
    });

    const canRecharge = computed(() => {
      return (selectedAmount.value !== null) || (customAmount.value && parseFloat(customAmount.value) > 0);
    });

    const loadBalance = async () => {
      const userType = userInfo.value.userType;
      if (shouldShowBalance.value) {
        loadingBalance.value = true;
        try {
          const currentBalance = await authService.getBalance(userInfo.value.phone, userType);
          balance.value = currentBalance;
          userInfo.value.money = currentBalance;
          localStorage.setItem('user', JSON.stringify(userInfo.value));
        } catch (error) {
          logger.error('PROFILE', '获取余额失败', {}, error);
          balance.value = userInfo.value.money || 0;
        } finally {
          loadingBalance.value = false;
        }
      }
    };

    const startEdit = () => {
      isEditing.value = true;
      editedNickname.value = userInfo.value.nickname || '';
      editedShippingAddress.value = shippingAddress.value || '';
    };

    const cancelEdit = () => {
      isEditing.value = false;
      editedNickname.value = '';
      editedShippingAddress.value = '';
    };

    const saveProfile = async () => {
      if (!editedNickname.value || editedNickname.value.trim() === '') {
        alert('昵称不能为空');
        return;
      }

      if (editedNickname.value.length > 30) {
        alert('昵称长度不能超过30个字符');
        return;
      }

      try {
        logger.userAction('UPDATE_PROFILE', { phone: userInfo.value.phone, nickname: editedNickname.value });
        
        // 更新昵称
        await authService.updateProfile(userInfo.value.phone, editedNickname.value.trim());
        userInfo.value.nickname = editedNickname.value.trim();
        
        // 如果是买家，更新收货地址
        if (isBuyer.value) {
          try {
            await authService.updateShippingAddress(userInfo.value.phone, editedShippingAddress.value.trim() || '');
            shippingAddress.value = editedShippingAddress.value.trim() || '';
            userInfo.value.shippingAddress = shippingAddress.value;
          } catch (error) {
            logger.error('PROFILE', '更新收货地址失败', {}, error);
            // 收货地址更新失败不影响昵称的更新
          }
        }
        
        localStorage.setItem('user', JSON.stringify(userInfo.value));
        
        isEditing.value = false;
        alert('更新成功');
        logger.info('PROFILE', '更新个人信息成功');
      } catch (error) {
        logger.error('PROFILE', '更新个人信息失败', {}, error);
        alert('更新失败：' + (error.message || '未知错误'));
      }
    };

    const selectAmount = (amount) => {
      selectedAmount.value = amount;
      customAmount.value = '';
    };

    const handleCustomAmountInput = () => {
      if (customAmount.value) {
        selectedAmount.value = null;
      }
    };

    const handleRecharge = async () => {
      let amount = null;
      if (selectedAmount.value !== null) {
        amount = selectedAmount.value;
      } else if (customAmount.value && parseFloat(customAmount.value) > 0) {
        amount = parseFloat(customAmount.value);
      }

      if (!amount || amount <= 0) {
        alert('请选择或输入充值金额');
        return;
      }

      if (amount < 0.01) {
        alert('充值金额不能少于0.01元');
        return;
      }

      if (!confirm(`确认充值 ¥${amount.toFixed(2)} 吗？\n（模拟充值，无需实际付款）`)) {
        return;
      }

      recharging.value = true;
      try {
        logger.userAction('RECHARGE', { phone: userInfo.value.phone, amount });
        const newBalance = await authService.recharge(userInfo.value.phone, userInfo.value.userType, amount);
        
        balance.value = newBalance;
        userInfo.value.money = newBalance;
        localStorage.setItem('user', JSON.stringify(userInfo.value));
        
        selectedAmount.value = null;
        customAmount.value = '';
        
        alert(`充值成功！当前余额：¥${newBalance.toFixed(2)}`);
        logger.info('PROFILE', '充值成功', { amount, newBalance });
      } catch (error) {
        logger.error('PROFILE', '充值失败', { amount }, error);
        alert('充值失败：' + (error.message || '未知错误'));
      } finally {
        recharging.value = false;
      }
    };

    return {
      userInfo,
      isEditing,
      editedNickname,
      editedShippingAddress,
      shippingAddress,
      isBuyer,
      balance,
      loadingBalance,
      selectedAmount,
      customAmount,
      recharging,
      rechargeAmounts,
      userRoleText,
      shouldShowBalance,
      formattedBalance,
      canRecharge,
      startEdit,
      cancelEdit,
      saveProfile,
      selectAmount,
      handleCustomAmountInput,
      handleRecharge
    };
  }
};
</script>

<style scoped>
.profile-container {
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 2rem;
}

.profile-content {
  max-width: 900px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 2rem;
  text-align: center;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 0.5rem 0;
}

.page-subtitle {
  font-size: 1rem;
  color: #718096;
  margin: 0;
}

.profile-sections {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.profile-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: white;
  margin: 0;
}

.section-content {
  padding: 1.5rem;
}

.info-group {
  margin-bottom: 1.5rem;
}

.info-group:last-child {
  margin-bottom: 0;
}

.info-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #4a5568;
  margin-bottom: 0.5rem;
}

.info-value {
  font-size: 1rem;
  color: #1a202c;
  padding: 0.75rem;
  background: #f7fafc;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.info-value.readonly {
  color: #718096;
  background: #edf2f7;
}

.info-input {
  width: 100%;
  padding: 0.75rem;
  font-size: 1rem;
  color: #1a202c;
  background: white;
  border: 2px solid #cbd5e0;
  border-radius: 6px;
  transition: border-color 0.2s;
}

.info-input:focus {
  outline: none;
  border-color: #667eea;
}

.info-textarea {
  width: 100%;
  padding: 0.75rem;
  font-size: 1rem;
  color: #1a202c;
  background: white;
  border: 2px solid #cbd5e0;
  border-radius: 6px;
  transition: border-color 0.2s;
  resize: vertical;
  font-family: inherit;
}

.info-textarea:focus {
  outline: none;
  border-color: #667eea;
}

.btn-edit,
.btn-save,
.btn-cancel {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.btn-edit:hover {
  background: rgba(255, 255, 255, 0.3);
}

.edit-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-save {
  background: white;
  color: #667eea;
}

.btn-save:hover {
  background: #f7fafc;
}

.btn-cancel {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.3);
}

.balance-display {
  text-align: center;
  padding: 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-bottom: 2rem;
}

.balance-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.5rem;
}

.balance-amount {
  font-size: 2.5rem;
  font-weight: 700;
  color: white;
}

.balance-loading {
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.25rem;
}

.recharge-section {
  padding-top: 1.5rem;
  border-top: 1px solid #e2e8f0;
}

.recharge-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.5rem 0;
}

.recharge-desc {
  font-size: 0.875rem;
  color: #718096;
  margin: 0 0 1.5rem 0;
}

.recharge-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.recharge-btn {
  padding: 1rem;
  font-size: 1rem;
  font-weight: 600;
  color: #4a5568;
  background: white;
  border: 2px solid #cbd5e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.recharge-btn:hover {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.2);
}

.recharge-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
}

.custom-amount {
  margin-bottom: 1.5rem;
}

.custom-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #4a5568;
  margin-bottom: 0.5rem;
}

.custom-input {
  width: 100%;
  padding: 0.75rem;
  font-size: 1rem;
  color: #1a202c;
  background: white;
  border: 2px solid #cbd5e0;
  border-radius: 6px;
  transition: border-color 0.2s;
}

.custom-input:focus {
  outline: none;
  border-color: #667eea;
}

.btn-recharge {
  width: 100%;
  padding: 1rem;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.btn-recharge:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(102, 126, 234, 0.4);
}

.btn-recharge:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 1rem;
  }

  .page-title {
    font-size: 1.5rem;
  }

  .recharge-options {
    grid-template-columns: repeat(2, 1fr);
  }

  .balance-amount {
    font-size: 2rem;
  }
}
</style>

