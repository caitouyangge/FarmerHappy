<template>
  <div class="auth-container">
    <div class="auth-card">
      <h1 class="auth-title">注册农乐平台</h1>
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
            placeholder="请输入密码（8-32位，包含大小写字母和数字）"
          />
          <span v-if="errors.password" class="form-error">{{ errors.password }}</span>
        </div>

        <div class="form-group">
          <label class="form-label">昵称（选填）</label>
          <input
            v-model="form.nickname"
            type="text"
            class="form-input"
            :class="{ 'error': errors.nickname }"
            placeholder="请输入昵称"
          />
          <span v-if="errors.nickname" class="form-error">{{ errors.nickname }}</span>
        </div>

        <div class="form-group">
          <label class="form-label">用户类型</label>
          <select
            v-model="form.user_type"
            class="form-input"
            :class="{ 'error': errors.user_type }"
          >
            <option value="">请选择用户类型</option>
            <option value="farmer">农户</option>
            <option value="buyer">买家</option>
            <option value="expert">技术专家</option>
            <option value="bank">银行</option>
            <option value="admin">管理员</option>
          </select>
          <span v-if="errors.user_type" class="form-error">{{ errors.user_type }}</span>
        </div>

        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <div class="auth-switch">
          已有账号？
          <router-link to="/login">立即登录</router-link>
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
  name: 'Register',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const form = reactive({
      phone: '',
      password: '',
      nickname: '',
      user_type: ''
    });
    const errors = reactive({
      phone: '',
      password: '',
      nickname: '',
      user_type: ''
    });

    const validateForm = () => {
      let isValid = true;
      // Reset errors
      Object.keys(errors).forEach(key => errors[key] = '');

      // Phone validation
      if (!form.phone) {
        errors.phone = '请输入手机号';
        isValid = false;
      } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
        errors.phone = '请输入有效的手机号';
        isValid = false;
      }

      // Password validation
      if (!form.password) {
        errors.password = '请输入密码';
        isValid = false;
      } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,32}$/.test(form.password)) {
        errors.password = '密码必须包含大小写字母和数字，长度8-32位';
        isValid = false;
      }

      // Nickname validation (optional)
      if (form.nickname && (form.nickname.length < 1 || form.nickname.length > 30)) {
        errors.nickname = '昵称长度应在1-30个字符之间';
        isValid = false;
      }

      // User type validation
      if (!form.user_type) {
        errors.user_type = '请选择用户类型';
        isValid = false;
      }

      return isValid;
    };

    const handleSubmit = async () => {
      if (!validateForm()) return;

      loading.value = true;
      try {
        await authService.register(form);
        router.push('/login');
      } catch (error) {
        if (error.message === 'Phone already exists') {
          errors.phone = '该手机号已被注册';
        } else {
          errors.password = '注册失败，请稍后重试';
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

select.form-input {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23A0AEC0' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 16px;
  padding-right: 2.5rem;
}
</style>
