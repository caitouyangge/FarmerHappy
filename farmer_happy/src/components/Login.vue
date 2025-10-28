<template>
  <div class="auth-container">
    <div class="auth-card">
      <h1 class="auth-title">欢迎登录农乐平台</h1>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="form-label">手机号</label>
          <input
            v-model="form.phone"
            type="tel"
            class="form-input"
            :class="{ 'error': errors.phone }"
            placeholder="请输入手机号"
          />
          <span v-if="errors.phone" class="form-error">{{ errors.phone }}</span>
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            :class="{ 'error': errors.password }"
            placeholder="请输入密码"
          />
          <span v-if="errors.password" class="form-error">{{ errors.password }}</span>
        </div>

        <div class="form-group">
          <label class="form-label">用户类型</label>
          <select
            v-model="form.userType"
            class="form-input"
            :class="{ 'error': errors.userType }"
          >
            <option value="">请选择用户类型</option>
            <option value="farmer">农户</option>
            <option value="buyer">买家</option>
            <option value="expert">技术专家</option>
            <option value="bank">银行</option>
          </select>
          <span v-if="errors.userType" class="form-error">{{ errors.userType }}</span>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="auth-switch">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../api/auth';
import logger from '../utils/logger';

export default {
  name: 'Login',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const form = reactive({
      phone: '',
      password: '',
      userType: ''
    });
    const errors = reactive({
      phone: '',
      password: '',
      userType: ''
    });

    onMounted(() => {
      logger.lifecycle('Login', 'mounted');
      logger.info('LOGIN_COMPONENT', '登录页面已加载');
    });

    onUnmounted(() => {
      logger.lifecycle('Login', 'unmounted');
    });

    const validateForm = () => {
      logger.debug('LOGIN_COMPONENT', '开始验证登录表单');
      let isValid = true;
      errors.phone = '';
      errors.password = '';
      errors.userType = '';

      if (!form.phone) {
        errors.phone = '请输入手机号';
        isValid = false;
      } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
        errors.phone = '请输入有效的手机号';
        isValid = false;
      }

      if (!form.password) {
        errors.password = '请输入密码';
        isValid = false;
      }

      if (!form.userType) {
        errors.userType = '请选择用户类型';
        isValid = false;
      }

      logger.validation('LoginForm', isValid, errors);
      return isValid;
    };

    const handleSubmit = async () => {
      logger.userAction('LOGIN_SUBMIT', { phone: form.phone, userType: form.userType });
      
      if (!validateForm()) {
        logger.warn('LOGIN_COMPONENT', '表单验证失败');
        return;
      }

      loading.value = true;
      logger.info('LOGIN_COMPONENT', '开始提交登录请求', { 
        phone: form.phone,
        userType: form.userType 
      });
      
      try {
        await authService.login(form);
        logger.info('LOGIN_COMPONENT', '登录成功，准备跳转到首页');
        logger.navigation('/login', '/home');
        router.push('/home');
      } catch (error) {
        logger.error('LOGIN_COMPONENT', '登录失败', {
          phone: form.phone,
          userType: form.userType,
          errorMessage: error.message || error
        }, error);
        
        // 根据错误消息显示对应的提示
        const errorMsg = typeof error === 'string' ? error : error.message || error.toString();
        
        if (errorMsg.includes('用户名或密码错误') || errorMsg.includes('Invalid credentials')) {
          errors.password = '手机号或密码错误';
          logger.warn('LOGIN_COMPONENT', '登录凭据无效');
        } else if (errorMsg.includes('服务器内部错误') || errorMsg.includes('数据库')) {
          errors.password = '服务器错误';
          logger.error('LOGIN_COMPONENT', '服务器内部错误');
        } else {
          errors.password = errorMsg || '登录失败，请稍后重试';
          logger.error('LOGIN_COMPONENT', '登录出现错误', { errorMsg });
        }
      } finally {
        loading.value = false;
        logger.debug('LOGIN_COMPONENT', '登录请求处理完成');
      }
    };

    return {
      form,
      errors,
      loading,
      handleSubmit
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';
</style>
