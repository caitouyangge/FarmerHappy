<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container large">
      <div class="modal-header">
        <h2 class="modal-title">待确认的联合贷款申请</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <div v-else-if="error" class="error-container">
          <span class="error-icon">⚠️</span>
          <span>{{ error }}</span>
        </div>

        <div v-else-if="applications.length === 0" class="empty-container">
          <div class="empty-icon">📋</div>
          <p>暂无待确认的联合贷款申请</p>
        </div>

        <div v-else class="applications-list">
          <div
            v-for="application in applications"
            :key="application.loan_application_id"
            class="application-card"
          >
            <div class="application-header">
              <h3 class="application-title">联合贷款申请</h3>
              <span class="application-id">申请编号：{{ application.loan_application_id }}</span>
            </div>

            <div class="application-content">
              <div class="info-section">
                <h4 class="section-title">产品信息</h4>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">产品名称：</span>
                    <span class="info-value">{{ application.product_name }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">申请金额：</span>
                    <span class="info-value highlight">¥{{ formatAmount(application.apply_amount) }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">年利率：</span>
                    <span class="info-value">{{ (application.interest_rate || 0).toFixed(2) }}%</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">贷款期限：</span>
                    <span class="info-value">{{ application.term_months }} 个月</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">您的份额：</span>
                    <span class="info-value highlight">¥{{ formatAmount(application.partner_share_amount) }}</span>
                  </div>
                </div>
              </div>

              <div class="info-section">
                <h4 class="section-title">发起人信息</h4>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">姓名：</span>
                    <span class="info-value">{{ application.initiator_nickname || '农户' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">手机号：</span>
                    <span class="info-value">{{ application.initiator_phone }}</span>
                  </div>
                </div>
              </div>

              <div class="info-section">
                <h4 class="section-title">贷款用途</h4>
                <p class="purpose-text">{{ application.purpose }}</p>
              </div>

              <div class="info-section">
                <h4 class="section-title">还款计划</h4>
                <p class="repayment-text">{{ application.repayment_source || application.repayment_plan || '未提供' }}</p>
              </div>

              <div class="info-section">
                <h4 class="section-title">申请时间</h4>
                <p class="time-text">{{ formatDateTime(application.created_at) }}</p>
              </div>

              <!-- 消息对话区域 -->
              <div class="info-section">
                <h4 class="section-title">
                  消息对话
                  <button 
                    class="btn-toggle-chat" 
                    @click="toggleChat(application.loan_application_id)"
                  >
                    {{ showChat[application.loan_application_id] ? '收起' : '展开' }}
                  </button>
                </h4>
                <div 
                  v-if="showChat[application.loan_application_id]" 
                  class="chat-container"
                >
                  <div class="chat-messages" :ref="el => setChatRef(application.loan_application_id, el)">
                    <div
                      v-for="(message, index) in chatMessages[application.loan_application_id] || []"
                      :key="index"
                      :class="['chat-message', message.sender === userInfo.phone ? 'sent' : 'received']"
                    >
                      <div class="message-content">
                        <div class="message-text">{{ message.content }}</div>
                        <div class="message-time">{{ formatDateTime(message.created_at) }}</div>
                      </div>
                    </div>
                    <div v-if="(chatMessages[application.loan_application_id] || []).length === 0" class="no-messages">
                      暂无消息
                    </div>
                  </div>
                  <div class="chat-input-area">
                    <textarea
                      v-model="newMessages[application.loan_application_id]"
                      class="chat-input"
                      rows="2"
                      placeholder="输入消息..."
                    ></textarea>
                    <button
                      class="btn-send-message"
                      @click="sendMessage(application)"
                      :disabled="!newMessages[application.loan_application_id]?.trim()"
                    >
                      发送
                    </button>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="action-buttons">
                <button
                  class="btn btn-reject"
                  @click="handleReject(application)"
                  :disabled="processing[application.loan_application_id]"
                >
                  拒绝
                </button>
                <button
                  class="btn btn-confirm"
                  @click="handleConfirm(application)"
                  :disabled="processing[application.loan_application_id]"
                >
                  {{ processing[application.loan_application_id] ? '处理中...' : '确认参与' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { financingService } from '../../api/financing';
import logger from '../../utils/logger';

export default {
  name: 'JointLoanConfirmationModal',
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const userInfo = ref({});
    const applications = ref([]);
    const loading = ref(false);
    const error = ref('');
    const showChat = reactive({});
    const chatMessages = reactive({});
    const newMessages = reactive({});
    const processing = reactive({});
    const chatRefs = reactive({});

    const setChatRef = (applicationId, el) => {
      if (el) {
        chatRefs[applicationId] = el;
      }
    };

    onMounted(async () => {
      console.log('JointLoanConfirmationModal mounted');
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          userInfo.value = JSON.parse(storedUser);
          console.log('用户信息:', userInfo.value);
          if (!userInfo.value.phone) {
            console.error('用户手机号为空');
            error.value = '用户信息不完整，请重新登录';
            return;
          }
          await loadApplications();
        } catch (err) {
          console.error('解析用户信息失败:', err);
          error.value = '用户信息解析失败，请重新登录';
        }
      } else {
        console.warn('未找到用户信息');
        error.value = '请先登录';
      }
    });

    const loadApplications = async () => {
      if (!userInfo.value.phone) {
        error.value = '请先登录';
        return;
      }

      loading.value = true;
      error.value = '';
      try {
        logger.info('FINANCING', '加载待确认的联合贷款申请', {
          phone: userInfo.value.phone
        });

        const response = await financingService.getPendingJointLoanApplications({
          phone: userInfo.value.phone
        });

        // 修复：API返回的数据结构是 response.data.data.applications
        applications.value = response.data?.data?.applications || response.data?.applications || [];
        
        console.log('DEBUG: 待确认联合贷款申请数据', {
          response: response,
          applications: applications.value,
          count: applications.value.length
        });
        
        // 初始化聊天状态
        applications.value.forEach(app => {
          showChat[app.loan_application_id] = false;
          chatMessages[app.loan_application_id] = [];
          newMessages[app.loan_application_id] = '';
          processing[app.loan_application_id] = false;
        });

        logger.info('FINANCING', '待确认的联合贷款申请加载成功', {
          count: applications.value.length
        });
      } catch (err) {
        logger.error('FINANCING', '加载待确认的联合贷款申请失败', {
          errorMessage: err.message || err
        }, err);
        error.value = err.message || '加载失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    };

    const toggleChat = (applicationId) => {
      showChat[applicationId] = !showChat[applicationId];
      if (showChat[applicationId]) {
        // 加载消息（这里暂时使用模拟数据，后续可以添加真实的消息API）
        loadChatMessages(applicationId);
      }
    };

    const loadChatMessages = async (applicationId) => {
      if (!userInfo.value.phone) return;
      
      try {
        const response = await financingService.getJointLoanMessages({
          phone: userInfo.value.phone,
          application_id: applicationId
        });
        
        const messages = (response.data?.messages || []).map(msg => ({
          sender: msg.sender,
          content: msg.content,
          created_at: msg.created_at
        }));
        
        chatMessages[applicationId] = messages;
        
        // 滚动到底部
        await nextTick();
        const chatElement = chatRefs[applicationId];
        if (chatElement) {
          chatElement.scrollTop = chatElement.scrollHeight;
        }
      } catch (err) {
        logger.error('FINANCING', '加载消息失败', {
          errorMessage: err.message || err
        }, err);
        if (!chatMessages[applicationId]) {
          chatMessages[applicationId] = [];
        }
      }
    };

    const sendMessage = async (application) => {
      const message = newMessages[application.loan_application_id]?.trim();
      if (!message) return;

      try {
        // 确定接收者
        // 这个组件是给被邀请的伙伴使用的，所以当前用户是伙伴，接收者是发起人
        const receiverPhone = application.initiator_phone;

        await financingService.sendJointLoanMessage({
          phone: userInfo.value.phone,
          application_id: application.loan_application_id,
          receiver_phone: receiverPhone,
          content: message
        });

        // 重新加载消息列表
        await loadChatMessages(application.loan_application_id);
        
        newMessages[application.loan_application_id] = '';
      } catch (err) {
        logger.error('FINANCING', '发送消息失败', {
          errorMessage: err.message || err
        }, err);
        alert('发送消息失败：' + (err.message || '请稍后重试'));
      }
    };

    const handleConfirm = async (application) => {
      if (!confirm('确认参与该联合贷款申请？确认后申请将进入银行审批流程。')) {
        return;
      }

      processing[application.loan_application_id] = true;
      try {
        logger.info('FINANCING', '确认联合贷款申请', {
          application_id: application.loan_application_id
        });

        await financingService.confirmJointLoanApplication({
          phone: userInfo.value.phone,
          application_id: application.loan_application_id,
          action: 'confirm'
        });

        logger.info('FINANCING', '联合贷款申请确认成功');
        alert('确认成功！申请已提交至银行审批。');
        emit('success');
        await loadApplications();
      } catch (err) {
        logger.error('FINANCING', '确认联合贷款申请失败', {
          errorMessage: err.message || err
        }, err);
        alert('确认失败：' + (err.message || '请稍后重试'));
      } finally {
        processing[application.loan_application_id] = false;
      }
    };

    const handleReject = async (application) => {
      const reason = prompt('请输入拒绝原因（可选）：');
      if (reason === null) {
        return; // 用户取消了
      }

      processing[application.loan_application_id] = true;
      try {
        logger.info('FINANCING', '拒绝联合贷款申请', {
          application_id: application.loan_application_id,
          reason: reason || '未提供原因'
        });

        await financingService.confirmJointLoanApplication({
          phone: userInfo.value.phone,
          application_id: application.loan_application_id,
          action: 'reject'
        });

        logger.info('FINANCING', '联合贷款申请拒绝成功');
        alert('已拒绝该联合贷款申请。');
        emit('success');
        await loadApplications();
      } catch (err) {
        logger.error('FINANCING', '拒绝联合贷款申请失败', {
          errorMessage: err.message || err
        }, err);
        alert('拒绝失败：' + (err.message || '请稍后重试'));
      } finally {
        processing[application.loan_application_id] = false;
      }
    };

    const formatAmount = (amount) => {
      if (!amount && amount !== 0) return '0.00';
      return parseFloat(amount).toFixed(2);
    };

    const formatDateTime = (dateTime) => {
      if (!dateTime) return '';
      const date = new Date(dateTime);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };

    const handleClose = () => {
      emit('close');
    };

    return {
      userInfo,
      applications,
      loading,
      error,
      showChat,
      chatMessages,
      newMessages,
      processing,
      toggleChat,
      sendMessage,
      setChatRef,
      handleConfirm,
      handleReject,
      formatAmount,
      formatDateTime,
      handleClose
    };
  }
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background: var(--white);
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-container.large {
  max-width: 900px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
  position: sticky;
  top: 0;
  background: var(--white);
  z-index: 10;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 2rem;
  color: var(--gray-400);
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.btn-close:hover {
  background: var(--gray-100);
  color: var(--gray-600);
}

.modal-body {
  padding: 1.5rem;
}

.loading-container, .error-container, .empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
  text-align: center;
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

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.applications-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.application-card {
  border: 2px solid var(--gray-200);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s;
}

.application-card:hover {
  border-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.1);
}

.application-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--gray-200);
}

.application-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.application-id {
  font-size: 0.875rem;
  color: var(--gray-500);
}

.application-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--gray-700);
  margin: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-toggle-chat {
  background: var(--primary-light);
  color: var(--primary);
  border: none;
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-toggle-chat:hover {
  background: var(--primary);
  color: var(--white);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.info-label {
  color: var(--gray-600);
  font-weight: 500;
}

.info-value {
  color: #1a202c;
  font-weight: 500;
}

.info-value.highlight {
  color: var(--primary);
  font-weight: 600;
  font-size: 1rem;
}

.purpose-text, .repayment-text, .time-text {
  color: var(--gray-700);
  line-height: 1.6;
  margin: 0;
  padding: 0.75rem;
  background: var(--gray-50);
  border-radius: 8px;
}

/* 聊天区域样式 */
.chat-container {
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  background: var(--white);
  display: flex;
  flex-direction: column;
  max-height: 400px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 200px;
  max-height: 300px;
}

.chat-message {
  display: flex;
  margin-bottom: 0.5rem;
}

.chat-message.sent {
  justify-content: flex-end;
}

.chat-message.received {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: var(--gray-100);
}

.chat-message.sent .message-content {
  background: var(--primary);
  color: var(--white);
}

.message-text {
  margin-bottom: 0.25rem;
  word-wrap: break-word;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
}

.no-messages {
  text-align: center;
  color: var(--gray-500);
  padding: 2rem;
}

.chat-input-area {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 1px solid var(--gray-200);
}

.chat-input {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.875rem;
  resize: none;
  font-family: inherit;
}

.chat-input:focus {
  outline: none;
  border-color: var(--primary);
}

.btn-send-message {
  padding: 0.75rem 1.5rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-send-message:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-send-message:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-200);
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm {
  background: var(--primary);
  color: var(--white);
}

.btn-confirm:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-reject {
  background: var(--gray-200);
  color: var(--gray-700);
}

.btn-reject:hover:not(:disabled) {
  background: var(--gray-300);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

