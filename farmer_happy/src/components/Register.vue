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

        <!-- 农户特定字段（已隐藏，使用默认值） -->
        <template v-if="form.user_type === 'farmer'">
          <!-- 农户额外信息已隐藏，使用默认值 -->
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

        <!-- 专家特定字段（已隐藏，使用默认值） -->
        <template v-if="form.user_type === 'expert'">
          <!-- 专家额外信息已隐藏，使用默认值 -->
        </template>

        <!-- 银行特定字段 -->
        <template v-if="form.user_type === 'bank'">
          <div class="form-group">
            <label class="form-label">银行名称</label>
            <input
              v-model="form.bank_name"
              type="text"
              class="form-input"
              readonly
              style="background-color: #f5f5f5; cursor: not-allowed;"
            />
          </div>

          <div class="form-group">
            <label class="form-label">分行名称</label>
            <input
              v-model="form.branch_name"
              type="text"
              class="form-input"
              readonly
              style="background-color: #f5f5f5; cursor: not-allowed;"
            />
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
      // 当选择银行类型时，自动设置银行名称为"农乐银行"，分行名称为"农乐分行"
      if (newType === 'bank') {
        form.bank_name = '农乐银行';
        form.branch_name = '农乐分行';
      } else if (oldType === 'bank') {
        // 切换出银行类型时，清空银行和分行名称
        form.bank_name = '';
        form.branch_name = '';
      }
      // 当选择农户类型时，自动设置默认值
      if (newType === 'farmer') {
        form.farm_name = '农乐农场';
        form.farm_address = '默认地址';
        form.farm_size = 10.0;
      } else if (oldType === 'farmer') {
        // 切换出农户类型时，清空农户字段
        form.farm_name = '';
        form.farm_address = '';
        form.farm_size = null;
      }
      // 当选择技术专家类型时，自动设置默认值
      if (newType === 'expert') {
        form.expertise_field = '农业技术';
        form.work_experience = 5;
      } else if (oldType === 'expert') {
        // 切换出技术专家类型时，清空专家字段
        form.expertise_field = '';
        form.work_experience = null;
      }
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
        // 农户特定字段使用默认值，只验证长度限制
        if (form.farm_name && form.farm_name.length > 100) {
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
        // 专家特定字段使用默认值，只验证长度限制
        if (form.expertise_field && form.expertise_field.length > 100) {
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
        // 银行名称固定为"农乐银行"，不需要验证
        if (form.bank_name && form.bank_name.length > 100) {
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
        
        // 显示成功提示
        alert('注册成功！即将跳转到登录页面');
        router.push('/login');
      } catch (error) {
        logger.error('REGISTER_COMPONENT', '注册失败', {
          phone: form.phone,
          userType: form.user_type,
          errorCode: error.code,
          errorMessage: error.message || error
        }, error);
        
        // 先清空所有错误信息
        Object.keys(errors).forEach(key => errors[key] = '');
        
        // 根据错误码处理不同情况
        if (error.code === 409) {
          // 该手机号已注册此用户类型
          errors.phone = '该手机号已注册此用户类型，请选择其他用户类型或直接登录';
          logger.warn('REGISTER_COMPONENT', '该手机号已注册此用户类型', { 
            phone: form.phone,
            userType: form.user_type
          });
        } else if (error.code === 400) {
          // 可能是密码错误或参数验证失败
          if (error.message === '密码错误') {
            errors.password = '该手机号已存在，但密码不正确。如果您已注册过其他身份，请使用相同的密码';
            logger.warn('REGISTER_COMPONENT', '手机号已存在但密码错误', { 
              phone: form.phone 
            });
          } else if (error.message === '参数验证失败' && error.errors && error.errors.length > 0) {
            // 处理字段级别的错误
            console.log('参数验证失败，详细错误：', error.errors);
            error.errors.forEach(err => {
              console.log('处理错误字段：', err.field, err.message);
              if (Object.prototype.hasOwnProperty.call(errors, err.field)) {
                errors[err.field] = err.message;
              } else {
                // 如果前端没有对应的错误字段，显示在密码字段
                console.warn('未找到对应的错误字段：', err.field);
              }
            });
            logger.warn('REGISTER_COMPONENT', '参数验证失败', { errors: error.errors });
          } else {
            // 其他 400 错误
            errors.password = error.message || '注册失败，请检查输入信息';
            logger.warn('REGISTER_COMPONENT', '注册失败', { message: error.message });
          }
        } else if (error.code === 401) {
          errors.password = '认证失败，请重新尝试';
          logger.warn('REGISTER_COMPONENT', '认证失败');
        } else {
          // 500 或其他错误
          errors.password = error.message || '注册失败，服务器内部错误，请稍后重试';
          logger.error('REGISTER_COMPONENT', '注册出现无错误', { 
            code: error.code,
            message: error.message 
          });
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
