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
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../api/auth';

export default {
  name: 'Login',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const form = reactive({
      phone: '',
      password: ''
    });
    const errors = reactive({
      phone: '',
      password: ''
    });

    const validateForm = () => {
      let isValid = true;
      errors.phone = '';
      errors.password = '';

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

      return isValid;
    };

    const handleSubmit = async () => {
      if (!validateForm()) return;

      loading.value = true;
      try {
        await authService.login(form);
        router.push('/home');
      } catch (error) {
        if (error.message === 'Invalid credentials') {
          errors.password = '手机号或密码错误';
        } else {
          errors.password = '登录失败，请稍后重试';
        }
      } finally {
        loading.value = false;
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
