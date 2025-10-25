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
          <label class="form-label">昵称</label>
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
          </select>
          <span v-if="errors.user_type" class="form-error">{{ errors.user_type }}</span>
        </div>

        <!-- 农户特定字段 -->
        <template v-if="form.user_type === 'farmer'">
          <div class="form-group">
            <label class="form-label">农场名称</label>
            <input
              v-model="form.farm_name"
              type="text"
              class="form-input"
              :class="{ 'error': errors.farm_name }"
              placeholder="请输入农场名称"
            />
            <span v-if="errors.farm_name" class="form-error">{{ errors.farm_name }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">农场地址</label>
            <input
              v-model="form.farm_address"
              type="text"
              class="form-input"
              :class="{ 'error': errors.farm_address }"
              placeholder="请输入农场地址"
            />
            <span v-if="errors.farm_address" class="form-error">{{ errors.farm_address }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">农场面积（亩）</label>
            <input
              v-model.number="form.farm_size"
              type="number"
              step="0.01"
              class="form-input"
              :class="{ 'error': errors.farm_size }"
              placeholder="请输入农场面积"
            />
            <span v-if="errors.farm_size" class="form-error">{{ errors.farm_size }}</span>
          </div>
        </template>

        <!-- 买家特定字段 -->
        <template v-if="form.user_type === 'buyer'">
          <div class="form-group">
            <label class="form-label">收货地址</label>
            <input
              v-model="form.shipping_address"
              type="text"
              class="form-input"
              :class="{ 'error': errors.shipping_address }"
              placeholder="请输入默认收货地址"
            />
            <span v-if="errors.shipping_address" class="form-error">{{ errors.shipping_address }}</span>
          </div>
        </template>

        <!-- 专家特定字段 -->
        <template v-if="form.user_type === 'expert'">
          <div class="form-group">
            <label class="form-label">专业领域</label>
            <input
              v-model="form.expertise_field"
              type="text"
              class="form-input"
              :class="{ 'error': errors.expertise_field }"
              placeholder="请输入专业领域"
            />
            <span v-if="errors.expertise_field" class="form-error">{{ errors.expertise_field }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">工作经验（年）</label>
            <input
              v-model.number="form.work_experience"
              type="number"
              class="form-input"
              :class="{ 'error': errors.work_experience }"
              placeholder="请输入工作经验年限"
            />
            <span v-if="errors.work_experience" class="form-error">{{ errors.work_experience }}</span>
          </div>
        </template>

        <!-- 银行特定字段 -->
        <template v-if="form.user_type === 'bank'">
          <div class="form-group">
            <label class="form-label">银行名称</label>
            <input
              v-model="form.bank_name"
              type="text"
              class="form-input"
              :class="{ 'error': errors.bank_name }"
              placeholder="请输入银行名称"
            />
            <span v-if="errors.bank_name" class="form-error">{{ errors.bank_name }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">分行名称</label>
            <input
              v-model="form.branch_name"
              type="text"
              class="form-input"
              :class="{ 'error': errors.branch_name }"
              placeholder="请输入分行名称"
            />
            <span v-if="errors.branch_name" class="form-error">{{ errors.branch_name }}</span>
          </div>
        </template>

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
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { authService } from '../api/auth';
import logger from '../utils/logger';

export default {
  name: 'Register',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const form = reactive({
      // 通用字段
      phone: '',
      password: '',
      nickname: '',
      user_type: '',
      // 农户特定字段
      farm_name: '',
      farm_address: '',
      farm_size: null,
      // 买家特定字段
      shipping_address: '',
      // 专家特定字段
      expertise_field: '',
      work_experience: null,
      // 银行特定字段
      bank_name: '',
      branch_name: ''
    });
    const errors = reactive({
      // 通用字段
      phone: '',
      password: '',
      nickname: '',
      user_type: '',
      // 农户特定字段
      farm_name: '',
      farm_address: '',
      farm_size: '',
      // 买家特定字段
      shipping_address: '',
      // 专家特定字段
      expertise_field: '',
      work_experience: '',
      // 银行特定字段
      bank_name: '',
      branch_name: ''
    });

    onMounted(() => {
      logger.lifecycle('Register', 'mounted');
      logger.info('REGISTER_COMPONENT', '注册页面已加载');
    });

    onUnmounted(() => {
      logger.lifecycle('Register', 'unmounted');
    });

    // 监听用户类型变化
    watch(() => form.user_type, (newType, oldType) => {
      logger.info('REGISTER_COMPONENT', '用户类型变化', {
        from: oldType,
        to: newType
      });
    });

    const validateForm = () => {
      logger.debug('REGISTER_COMPONENT', '开始验证注册表单', {
        userType: form.user_type
      });
      
      let isValid = true;
      // Reset errors
      Object.keys(errors).forEach(key => errors[key] = '');

      // 通用字段验证
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

      // Nickname validation
      if (form.nickname && (form.nickname.length < 1 || form.nickname.length > 30)) {
        errors.nickname = '昵称长度应在1-30个字符之间';
        isValid = false;
      }

      // User type validation
      if (!form.user_type) {
        errors.user_type = '请选择用户类型';
        isValid = false;
      }

      // 根据用户类型验证特定字段
      if (form.user_type === 'farmer') {
        logger.debug('REGISTER_COMPONENT', '验证农户特定字段');
        // 农户特定字段验证
        if (!form.farm_name) {
          errors.farm_name = '请输入农场名称';
          isValid = false;
        } else if (form.farm_name.length > 100) {
          errors.farm_name = '农场名称不能超过100个字符';
          isValid = false;
        }

        if (form.farm_address && form.farm_address.length > 200) {
          errors.farm_address = '农场地址不能超过200个字符';
          isValid = false;
        }

        if (form.farm_size && (isNaN(form.farm_size) || form.farm_size < 0)) {
          errors.farm_size = '请输入有效的农场面积';
          isValid = false;
        }
      }

      else if (form.user_type === 'buyer') {
        logger.debug('REGISTER_COMPONENT', '验证买家特定字段');
        // 买家特定字段验证
        if (form.shipping_address && form.shipping_address.length > 500) {
          errors.shipping_address = '收货地址不能超过500个字符';
          isValid = false;
        }
      }

      else if (form.user_type === 'expert') {
        logger.debug('REGISTER_COMPONENT', '验证专家特定字段');
        // 专家特定字段验证
        if (!form.expertise_field) {
          errors.expertise_field = '请输入专业领域';
          isValid = false;
        } else if (form.expertise_field.length > 100) {
          errors.expertise_field = '专业领域不能超过100个字符';
          isValid = false;
        }

        if (form.work_experience && (isNaN(form.work_experience) || form.work_experience < 0)) {
          errors.work_experience = '请输入有效的工作经验年限';
          isValid = false;
        }
      }

      else if (form.user_type === 'bank') {
        logger.debug('REGISTER_COMPONENT', '验证银行特定字段');
        // 银行特定字段验证
        if (!form.bank_name) {
          errors.bank_name = '请输入银行名称';
          isValid = false;
        } else if (form.bank_name.length > 100) {
          errors.bank_name = '银行名称不能超过100个字符';
          isValid = false;
        }

        if (form.branch_name && form.branch_name.length > 100) {
          errors.branch_name = '分行名称不能超过100个字符';
          isValid = false;
        }
      }

      logger.validation('RegisterForm', isValid, errors);
      return isValid;
    };

    const handleSubmit = async () => {
      logger.userAction('REGISTER_SUBMIT', {
        phone: form.phone,
        userType: form.user_type
      });
      
      if (!validateForm()) {
        logger.warn('REGISTER_COMPONENT', '表单验证失败', {
          userType: form.user_type
        });
        return;
      }

      loading.value = true;
      logger.info('REGISTER_COMPONENT', '开始提交注册请求', {
        phone: form.phone,
        userType: form.user_type
      });
      
      try {
        await authService.register(form);
        logger.info('REGISTER_COMPONENT', '注册成功，准备跳转到登录页');
        logger.navigation('/register', '/login');
        router.push('/login');
      } catch (error) {
        logger.error('REGISTER_COMPONENT', '注册失败', {
          phone: form.phone,
          userType: form.user_type,
          errorMessage: error.message || error
        }, error);
        
        if (error.message === 'Phone already exists') {
          errors.phone = '该手机号已被注册';
          logger.warn('REGISTER_COMPONENT', '手机号已存在', { phone: form.phone });
        } else {
          errors.password = '注册失败，请稍后重试';
          logger.error('REGISTER_COMPONENT', '注册出现未知错误');
        }
      } finally {
        loading.value = false;
        logger.debug('REGISTER_COMPONENT', '注册请求处理完成');
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
