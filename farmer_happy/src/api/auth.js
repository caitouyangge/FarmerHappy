import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/auth';

export const authService = {
    async register(userData) {
        try {
            logger.apiRequest('POST', `${API_URL}/register`, {
                phone: userData.phone,
                user_type: userData.user_type,
                nickname: userData.nickname
            });
            logger.info('AUTH', '开始注册用户', { userType: userData.user_type });
            
            const response = await axios.post(`${API_URL}/register`, userData);
            
            logger.apiResponse('POST', `${API_URL}/register`, response.status, {
                code: response.data.code,
                success: response.data.code === 200
            });
            
            // 检查响应体中的 code 字段
            if (response.data.code !== 200) {
                logger.error('AUTH', '注册失败', {
                    phone: userData.phone,
                    code: response.data.code,
                    message: response.data.message
                });
                
                // 根据不同的错误码抛出详细的错误信息
                const errorObj = {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
                
                throw errorObj;
            }
            
            logger.info('AUTH', '用户注册成功', { 
                phone: userData.phone,
                userType: userData.user_type 
            });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/register`, error);
            logger.error('AUTH', '用户注册失败', {
                phone: userData.phone,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            
            // 如果是 axios 错误，提取详细的错误信息
            if (error.response?.data) {
                throw {
                    code: error.response.data.code,
                    message: error.response.data.message,
                    errors: error.response.data.errors || []
                };
            }
            
            // 如果已经是我们构造的错误对象，直接抛出
            if (error.code && error.message) {
                throw error;
            }
            
            // 其他错误
            throw {
                code: 500,
                message: error.message || '注册失败，请稍后重试',
                errors: []
            };
        }
    },

    async login(credentials) {
        try {
            logger.apiRequest('POST', `${API_URL}/login`, {
                phone: credentials.phone
            });
            logger.info('AUTH', '开始用户登录', { phone: credentials.phone });
            
            const response = await axios.post(`${API_URL}/login`, credentials);
            
            logger.apiResponse('POST', `${API_URL}/login`, response.status, {
                code: response.data.code
            });
            
            // 检查响应体中的 code 字段
            if (response.data.code !== 200) {
                logger.error('AUTH', '登录失败', {
                    phone: credentials.phone,
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '登录失败');
            }
            
            // 从data字段中获取实际数据
            const authData = response.data.data;
            
            if (authData) {
                // 从响应中提取用户信息，排除密码
                const { password, ...userInfo } = authData;
                // 保存用户信息到本地存储
                localStorage.setItem('user', JSON.stringify(userInfo));
                logger.info('AUTH', '用户登录成功，已保存用户信息', {
                    phone: credentials.phone,
                    userType: userInfo.userType
                });
            }
            
            return authData;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/login`, error);
            logger.error('AUTH', '用户登录失败', {
                phone: credentials.phone,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    async getBalance(phone, userType) {
        try {
            logger.apiRequest('GET', `${API_URL}/balance`, { phone, user_type: userType });
            logger.info('AUTH', '开始获取用户余额', { phone, userType });
            
            const response = await axios.get(`${API_URL}/balance`, {
                params: {
                    phone: phone,
                    user_type: userType
                }
            });
            
            logger.apiResponse('GET', `${API_URL}/balance`, response.status, {
                code: response.data.code,
                success: response.data.code === 200
            });
            
            if (response.data.code !== 200) {
                logger.error('AUTH', '获取余额失败', {
                    phone,
                    userType,
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取余额失败');
            }
            
            const balance = response.data.data?.balance || 0;
            logger.info('AUTH', '获取余额成功', { phone, userType, balance });
            
            return balance;
        } catch (error) {
            logger.apiError('GET', `${API_URL}/balance`, error);
            logger.error('AUTH', '获取余额失败', {
                phone,
                userType,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    async updateProfile(phone, nickname) {
        try {
            logger.apiRequest('PUT', `${API_URL}/profile`, { phone, nickname });
            logger.info('AUTH', '开始更新用户信息', { phone, nickname });
            
            const response = await axios.put(`${API_URL}/profile`, {
                phone: phone,
                nickname: nickname
            });
            
            logger.apiResponse('PUT', `${API_URL}/profile`, response.status, {
                code: response.data.code,
                success: response.data.code === 200
            });
            
            if (response.data.code !== 200) {
                logger.error('AUTH', '更新用户信息失败', {
                    phone,
                    nickname,
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '更新用户信息失败');
            }
            
            // 更新localStorage中的用户信息
            const storedUser = localStorage.getItem('user');
            if (storedUser) {
                const userData = JSON.parse(storedUser);
                userData.nickname = nickname;
                localStorage.setItem('user', JSON.stringify(userData));
            }
            
            logger.info('AUTH', '更新用户信息成功', { phone, nickname });
            return response.data;
        } catch (error) {
            logger.apiError('PUT', `${API_URL}/profile`, error);
            logger.error('AUTH', '更新用户信息失败', {
                phone,
                nickname,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    async recharge(phone, userType, amount) {
        try {
            logger.apiRequest('POST', `${API_URL}/recharge`, { phone, user_type: userType, amount });
            logger.info('AUTH', '开始充值', { phone, userType, amount });
            
            const response = await axios.post(`${API_URL}/recharge`, {
                phone: phone,
                user_type: userType,
                amount: amount
            });
            
            logger.apiResponse('POST', `${API_URL}/recharge`, response.status, {
                code: response.data.code,
                success: response.data.code === 200
            });
            
            if (response.data.code !== 200) {
                logger.error('AUTH', '充值失败', {
                    phone,
                    userType,
                    amount,
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '充值失败');
            }
            
            // 更新localStorage中的余额
            const storedUser = localStorage.getItem('user');
            if (storedUser) {
                const userData = JSON.parse(storedUser);
                userData.money = response.data.data?.balance || userData.money;
                localStorage.setItem('user', JSON.stringify(userData));
            }
            
            logger.info('AUTH', '充值成功', { phone, userType, amount, newBalance: response.data.data?.balance });
            return response.data.data?.balance;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/recharge`, error);
            logger.error('AUTH', '充值失败', {
                phone,
                userType,
                amount,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    async getUserProfile(phone, userType) {
        try {
            logger.apiRequest('GET', `${API_URL}/profile/detail`, { phone, user_type: userType });
            logger.info('AUTH', '开始获取用户详细信息', { phone, userType });
            
            const response = await axios.get(`${API_URL}/profile/detail`, {
                params: {
                    phone: phone,
                    user_type: userType
                }
            });
            
            logger.apiResponse('GET', `${API_URL}/profile/detail`, response.status, {
                code: response.data.code,
                success: response.data.code === 200
            });
            
            if (response.data.code !== 200) {
                logger.error('AUTH', '获取用户详细信息失败', {
                    phone,
                    userType,
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取用户详细信息失败');
            }
            
            logger.info('AUTH', '获取用户详细信息成功', { phone, userType });
            return response.data.data;
        } catch (error) {
            logger.apiError('GET', `${API_URL}/profile/detail`, error);
            logger.error('AUTH', '获取用户详细信息失败', {
                phone,
                userType,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    async updateShippingAddress(phone, shippingAddress) {
        try {
            logger.apiRequest('PUT', `${API_URL}/shipping-address`, { phone, shipping_address: shippingAddress });
            logger.info('AUTH', '开始更新收货地址', { phone, shippingAddress });
            
            const response = await axios.put(`${API_URL}/shipping-address`, {
                phone: phone,
                shipping_address: shippingAddress
            });
            
            logger.apiResponse('PUT', `${API_URL}/shipping-address`, response.status, {
                code: response.data.code,
                success: response.data.code === 200
            });
            
            if (response.data.code !== 200) {
                logger.error('AUTH', '更新收货地址失败', {
                    phone,
                    shippingAddress,
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '更新收货地址失败');
            }
            
            // 更新localStorage中的用户信息
            const storedUser = localStorage.getItem('user');
            if (storedUser) {
                const userData = JSON.parse(storedUser);
                userData.shippingAddress = shippingAddress;
                localStorage.setItem('user', JSON.stringify(userData));
            }
            
            logger.info('AUTH', '更新收货地址成功', { phone, shippingAddress });
            return response.data;
        } catch (error) {
            logger.apiError('PUT', `${API_URL}/shipping-address`, error);
            logger.error('AUTH', '更新收货地址失败', {
                phone,
                shippingAddress,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    logout() {
        logger.info('AUTH', '用户登出');
        const user = localStorage.getItem('user');
        if (user) {
            try {
                const userData = JSON.parse(user);
                logger.info('AUTH', '清除用户信息', { phone: userData.phone });
            } catch (e) {
                logger.warn('AUTH', '解析用户信息失败');
            }
        }
        localStorage.removeItem('user');
        logger.info('AUTH', '用户登出成功');
    }
};
