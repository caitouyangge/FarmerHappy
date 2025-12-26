<template>
  <Teleport to="body">
    <div class="chat-window-container" v-if="visible">
    <div class="chat-window">
      <!-- 聊天窗口头部 -->
      <div class="chat-header">
        <button class="btn-back" @click="closeChat">
          <span>←</span>
        </button>
        <div class="chat-title">
          <div class="chat-name">{{ chatTitle }}</div>
          <div class="chat-subtitle">{{ chatSubtitle }}</div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-message">
          <p>暂无消息，开始聊天吧~</p>
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="{ 'message-sent': isSent(msg), 'message-received': !isSent(msg) }"
        >
          <div class="message-bubble">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.created_at) }}</div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <textarea
          v-model="inputMessage"
          class="chat-input"
          placeholder="输入消息..."
          rows="1"
          @keydown="handleKeydown"
          @input="handleInput"
          ref="inputRef"
        ></textarea>
        <button class="btn-send" @click="sendMessage" :disabled="!canSend">
          发送
        </button>
      </div>
    </div>
  </div>
  </Teleport>
</template>

<script>
import { ref, computed, watch, nextTick, onMounted } from 'vue';
import { expertAppointmentService } from '../../api/expertAppointment';
import logger from '../../utils/logger';

export default {
  name: 'ExpertChatWindow',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    appointmentId: {
      type: String,
      required: false,
      default: ''
    },
    currentUserPhone: {
      type: String,
      required: false,
      default: ''
    },
    chatTitle: {
      type: String,
      default: '专家咨询'
    },
    chatSubtitle: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  setup(props, { emit }) {
    console.log('ExpertChatWindow setup called:', {
      visible: props.visible,
      appointmentId: props.appointmentId,
      currentUserPhone: props.currentUserPhone
    });
    const messages = ref([]);
    const inputMessage = ref('');
    const messagesContainer = ref(null);
    const inputRef = ref(null);
    const loading = ref(false);

    const canSend = computed(() => {
      return inputMessage.value.trim().length > 0 && !loading.value;
    });

    const isSent = (msg) => {
      const userPhone = getUserPhone();
      return msg.sender === userPhone;
    };

    const formatTime = (timestamp) => {
      if (!timestamp) return '';
      try {
        const date = new Date(timestamp);
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

        if (msgDate.getTime() === today.getTime()) {
          // 今天：显示时间
          const hours = String(date.getHours()).padStart(2, '0');
          const minutes = String(date.getMinutes()).padStart(2, '0');
          return `${hours}:${minutes}`;
        } else {
          // 其他日期：显示日期和时间
          const month = String(date.getMonth() + 1).padStart(2, '0');
          const day = String(date.getDate()).padStart(2, '0');
          const hours = String(date.getHours()).padStart(2, '0');
          const minutes = String(date.getMinutes()).padStart(2, '0');
          return `${month}-${day} ${hours}:${minutes}`;
        }
      } catch (e) {
        return '';
      }
    };

    const loadMessages = async () => {
      if (!props.appointmentId) {
        return;
      }
      
      // 始终从 localStorage 获取最新的用户手机号
      const userPhone = getUserPhone();
      if (!userPhone || userPhone.trim() === '') {
        alert('用户信息不完整，无法加载消息。请重新登录。');
        return;
      }

      try {
        loading.value = true;
        console.log('loadMessages - 开始加载消息:', { appointmentId: props.appointmentId, userPhone });
        const msgs = await expertAppointmentService.getMessages(
          props.appointmentId,
          userPhone
        );
        console.log('loadMessages - 获取到的消息数量:', msgs.length);
        console.log('loadMessages - 消息列表:', msgs);
        
        messages.value = msgs.map(msg => ({
          sender: msg.sender,
          receiver: msg.receiver,
          content: msg.content,
          created_at: msg.created_at
        }));
        
        console.log('loadMessages - 处理后的消息数量:', messages.value.length);
        
        // 滚动到底部
        await nextTick();
        scrollToBottom();
      } catch (error) {
        logger.error('EXPERT_CHAT', '加载消息失败', {
          appointmentId: props.appointmentId,
          errorMessage: error.message || error
        }, error);
        alert('加载消息失败：' + (error.message || '请稍后重试'));
      } finally {
        loading.value = false;
      }
    };

    // 从 localStorage 获取用户手机号
    const getUserPhone = () => {
      try {
        const stored = localStorage.getItem('user');
        if (stored) {
          const parsed = JSON.parse(stored);
          return parsed.phone || '';
        }
      } catch (e) {
        console.error('获取用户手机号失败:', e);
      }
      return '';
    };

    const sendMessage = async () => {
      if (!canSend.value) return;

      const content = inputMessage.value.trim();
      if (!content) return;

      // 始终从 localStorage 获取最新的用户手机号
      const userPhone = getUserPhone();
      if (!userPhone || userPhone.trim() === '') {
        alert('用户信息不完整，无法发送消息。请重新登录。');
        return;
      }

      try {
        loading.value = true;
        await expertAppointmentService.sendMessage(props.appointmentId, {
          sender_phone: userPhone,
          content: content
        });

        // 清空输入框
        inputMessage.value = '';
        
        // 等待一小段时间确保数据库已提交，然后重新加载消息
        await new Promise(resolve => setTimeout(resolve, 100));
        await loadMessages();
      } catch (error) {
        logger.error('EXPERT_CHAT', '发送消息失败', {
          appointmentId: props.appointmentId,
          errorMessage: error.message || error
        }, error);
        alert('发送消息失败：' + (error.message || '请稍后重试'));
      } finally {
        loading.value = false;
      }
    };

    const handleKeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    };

    const handleInput = () => {
      // 自动调整输入框高度
      if (inputRef.value) {
        inputRef.value.style.height = 'auto';
        inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px';
      }
    };

    const scrollToBottom = () => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
      }
    };

    const closeChat = () => {
      emit('close');
    };

    // 组件挂载时，如果 visible 为 true，加载消息（初次进入时）
    onMounted(() => {
      if (props.visible && props.appointmentId) {
        loadMessages();
      }
    });

    // 监听visible变化，打开时加载消息（当 visible 从 false 变为 true 时）
    watch(() => props.visible, (newVal, oldVal) => {
      // 只有当从 false 变为 true 时才加载，避免重复加载
      if (newVal && !oldVal && props.appointmentId) {
        loadMessages();
      }
    });

    // 监听appointmentId变化（当切换不同的预约时）
    watch(() => props.appointmentId, (newId, oldId) => {
      // 只有当 appointmentId 变化且 visible 为 true 时才加载
      if (props.visible && newId && newId !== oldId) {
        loadMessages();
      }
    });

    return {
      messages,
      inputMessage,
      messagesContainer,
      inputRef,
      loading,
      canSend,
      isSent,
      formatTime,
      sendMessage,
      handleKeydown,
      handleInput,
      closeChat
    };
  }
};
</script>

<style scoped>
@import '../../assets/styles/theme.css';

.chat-window-container {
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

.chat-window {
  width: 90%;
  max-width: 600px;
  height: 80vh;
  max-height: 800px;
  background: var(--white);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 1rem;
  background: var(--primary);
  color: var(--white);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-back {
  background: transparent;
  border: none;
  color: var(--white);
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0.5rem;
  margin-right: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-back:hover {
  opacity: 0.8;
}

.chat-title {
  flex: 1;
}

.chat-name {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.chat-subtitle {
  font-size: 0.75rem;
  opacity: 0.9;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.empty-message {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--gray-500);
  font-size: 0.9rem;
}

.message-item {
  display: flex;
  margin-bottom: 0.5rem;
}

.message-sent {
  justify-content: flex-end;
}

.message-received {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
  position: relative;
  word-wrap: break-word;
  word-break: break-word;
}

.message-sent .message-bubble {
  background: var(--primary);
  color: var(--white);
  border-bottom-right-radius: 4px;
}

.message-received .message-bubble {
  background: var(--white);
  color: var(--gray-900);
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-content {
  font-size: 0.9375rem;
  line-height: 1.5;
  margin-bottom: 0.25rem;
}

.message-time {
  font-size: 0.6875rem;
  opacity: 0.7;
  margin-top: 0.25rem;
}

.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--white);
  border-top: 1px solid var(--gray-200);
}

.chat-input {
  flex: 1;
  padding: 0.625rem 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.9375rem;
  font-family: inherit;
  resize: none;
  max-height: 120px;
  overflow-y: auto;
  line-height: 1.5;
}

.chat-input:focus {
  outline: none;
  border-color: var(--primary);
}

.btn-send {
  padding: 0.625rem 1.25rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-send:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .chat-window {
    width: 100%;
    height: 100vh;
    max-height: 100vh;
    border-radius: 0;
  }

  .message-bubble {
    max-width: 80%;
  }
}
</style>

