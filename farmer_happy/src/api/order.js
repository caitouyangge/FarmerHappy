import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/buyer/orders';
const FARMER_API_URL = '/api/v1/farmer/orders';

export const orderService = {
    /**
     * 创建订单
     * POST /api/v1/buyer/orders
     * @param {Object} orderData - 订单数据
     * @param {string} orderData.product_id - 商品ID
     * @param {number} orderData.quantity - 购买数量
     * @param {string} orderData.buyer_name - 买家姓名
     * @param {string} orderData.buyer_address - 收货地址
     * @param {string} orderData.buyer_phone - 买家手机号
     * @param {string} orderData.remark - 订单备注（可选）
     */
    async createOrder(orderData) {
        try {
            logger.apiRequest('POST', API_URL, {
                product_id: orderData.product_id,
                quantity: orderData.quantity,
                buyer_phone: orderData.buyer_phone
            });
            logger.info('ORDER', '创建订单', { 
                productId: orderData.product_id,
                quantity: orderData.quantity 
            });
            
            const response = await axios.post(API_URL, orderData);
            
            logger.apiResponse('POST', API_URL, response.status, {
                code: response.data.code,
                success: response.data.code === 201
            });
            
            if (response.data.code !== 201) {
                logger.error('ORDER', '创建订单失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                
                const errorObj = {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
                
                throw errorObj;
            }
            
            logger.info('ORDER', '订单创建成功', { 
                orderId: response.data.data?.order_id 
            });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', API_URL, error);
            logger.error('ORDER', '创建订单失败', {
                errorMessage: error.response?.data?.message || error.message
            }, error);
            
            if (error.response?.data) {
                throw {
                    code: error.response.data.code,
                    message: error.response.data.message,
                    errors: error.response.data.errors || []
                };
            }
            
            if (error.code && error.message) {
                throw error;
            }
            
            throw {
                code: 500,
                message: error.message || '创建订单失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 更新订单信息
     * PUT /api/v1/buyer/orders/{order_id}
     * @param {string} orderId - 订单ID
     * @param {Object} orderData - 订单数据
     * @param {string} orderData.buyer_name - 买家姓名（可选）
     * @param {string} orderData.buyer_address - 收货地址（可选）
     * @param {string} orderData.buyer_phone - 买家手机号（必填）
     * @param {string} orderData.remark - 订单备注（可选）
     */
    async updateOrder(orderId, orderData) {
        try {
            const url = `${API_URL}/${orderId}`;
            logger.apiRequest('PUT', url, {
                order_id: orderId,
                buyer_phone: orderData.buyer_phone
            });
            logger.info('ORDER', '更新订单', { orderId });
            
            const response = await axios.put(url, orderData);
            
            logger.apiResponse('PUT', url, response.status, {
                code: response.data.code
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '更新订单失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                
                const errorObj = {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
                
                throw errorObj;
            }
            
            logger.info('ORDER', '订单更新成功', { orderId });
            
            return response.data;
        } catch (error) {
            logger.apiError('PUT', `${API_URL}/${orderId}`, error);
            logger.error('ORDER', '更新订单失败', {
                orderId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            
            if (error.response?.data) {
                throw {
                    code: error.response.data.code,
                    message: error.response.data.message,
                    errors: error.response.data.errors || []
                };
            }
            
            if (error.code && error.message) {
                throw error;
            }
            
            throw {
                code: 500,
                message: error.message || '更新订单失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取订单详情
     * POST /api/v1/buyer/orders/query/{order_id}
     * @param {string} orderId - 订单ID
     * @param {Object} queryData - 查询数据
     * @param {string} queryData.buyer_phone - 买家手机号（必填）
     */
    async getOrderDetail(orderId, queryData) {
        try {
            const url = `${API_URL}/query/${orderId}`;
            logger.apiRequest('POST', url, {
                order_id: orderId,
                buyer_phone: queryData.buyer_phone
            });
            logger.info('ORDER', '获取订单详情', { orderId });
            
            const response = await axios.post(url, queryData);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '获取订单详情失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取订单详情失败');
            }
            
            logger.info('ORDER', '获取订单详情成功', { orderId });
            
            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/query/${orderId}`, error);
            logger.error('ORDER', '获取订单详情失败', {
                orderId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 获取订单列表
     * POST /api/v1/buyer/orders/list_query
     * @param {Object} params - 查询参数
     * @param {string} params.buyer_phone - 买家手机号（可选）
     * @param {string} params.status - 订单状态（可选）
     * @param {string} params.title - 商品标题（可选）
     */
    async getOrderList(params = {}) {
        try {
            const queryParams = new URLSearchParams();
            if (params.buyer_phone) {
                queryParams.append('buyer_phone', params.buyer_phone);
            }
            if (params.status) {
                queryParams.append('status', params.status);
            }
            if (params.title) {
                queryParams.append('title', params.title);
            }
            
            const url = `${API_URL}/list_query${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
            
            logger.apiRequest('POST', url, params);
            logger.info('ORDER', '获取订单列表', params);
            
            const response = await axios.post(url);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code,
                count: response.data.data?.list?.length || 0
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '获取订单列表失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取订单列表失败');
            }
            
            logger.info('ORDER', '获取订单列表成功', { 
                count: response.data.data?.list?.length || 0 
            });
            
            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/list_query`, error);
            logger.error('ORDER', '获取订单列表失败', {
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },


    /**
     * 申请退货退款
     * POST /api/v1/buyer/orders/{order_id}/refund
     * @param {string} orderId - 订单ID
     * @param {Object} refundData - 退款数据
     * @param {string} refundData.refund_reason - 退款原因（必填）
     * @param {string} refundData.refund_type - 退款类型（必填）
     * @param {string} refundData.buyer_phone - 买家手机号（必填）
     */
    async applyRefund(orderId, refundData) {
        try {
            const url = `${API_URL}/${orderId}/refund`;
            logger.apiRequest('POST', url, {
                order_id: orderId,
                buyer_phone: refundData.buyer_phone
            });
            logger.info('ORDER', '申请退货退款', { orderId });
            
            const response = await axios.post(url, refundData);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '申请退货退款失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                
                const errorObj = {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
                
                throw errorObj;
            }
            
            logger.info('ORDER', '退款申请提交成功', { orderId });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/${orderId}/refund`, error);
            logger.error('ORDER', '申请退货退款失败', {
                orderId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            
            if (error.response?.data) {
                throw {
                    code: error.response.data.code,
                    message: error.response.data.message,
                    errors: error.response.data.errors || []
                };
            }
            
            if (error.code && error.message) {
                throw error;
            }
            
            throw {
                code: 500,
                message: error.message || '申请退货退款失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 确认收货
     * POST /api/v1/buyer/orders/{order_id}/confirm_receipt
     * @param {string} orderId - 订单ID
     * @param {Object} receiptData - 确认收货数据
     * @param {string} receiptData.buyer_phone - 买家手机号（必填）
     */
    async confirmReceipt(orderId, receiptData) {
        try {
            const url = `${API_URL}/${orderId}/confirm_receipt`;
            logger.apiRequest('POST', url, {
                order_id: orderId,
                buyer_phone: receiptData.buyer_phone
            });
            logger.info('ORDER', '确认收货', { orderId });
            
            const response = await axios.post(url, receiptData);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '确认收货失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '确认收货失败');
            }
            
            logger.info('ORDER', '确认收货成功', { orderId });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/${orderId}/confirm_receipt`, error);
            logger.error('ORDER', '确认收货失败', {
                orderId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 获取农户订单列表
     * POST /api/v1/farmer/orders/list_query
     * @param {Object} params - 查询参数
     * @param {string} params.farmer_phone - 农户手机号（可选）
     * @param {string} params.status - 订单状态（可选）
     * @param {string} params.title - 商品标题（可选）
     */
    async getFarmerOrderList(params = {}) {
        try {
            const queryParams = new URLSearchParams();
            if (params.farmer_phone) {
                queryParams.append('farmer_phone', params.farmer_phone);
            }
            if (params.status) {
                queryParams.append('status', params.status);
            }
            if (params.title) {
                queryParams.append('title', params.title);
            }
            
            const url = `${FARMER_API_URL}/list_query${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
            
            logger.apiRequest('POST', url, params);
            logger.info('ORDER', '获取农户订单列表', params);
            
            const response = await axios.post(url);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code,
                count: response.data.data?.list?.length || 0
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '获取农户订单列表失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取农户订单列表失败');
            }
            
            logger.info('ORDER', '获取农户订单列表成功', { 
                count: response.data.data?.list?.length || 0 
            });
            
            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FARMER_API_URL}/list_query`, error);
            logger.error('ORDER', '获取农户订单列表失败', {
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 获取农户订单详情
     * POST /api/v1/farmer/orders/query/{order_id}
     * @param {string} orderId - 订单ID
     * @param {Object} queryData - 查询数据
     * @param {string} queryData.farmer_phone - 农户手机号（必填）
     */
    async getFarmerOrderDetail(orderId, queryData) {
        try {
            const url = `${FARMER_API_URL}/query/${orderId}`;
            logger.apiRequest('POST', url, {
                order_id: orderId,
                farmer_phone: queryData.farmer_phone
            });
            logger.info('ORDER', '获取农户订单详情', { orderId });
            
            const response = await axios.post(url, queryData);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });
            
            if (response.data.code !== 200) {
                logger.error('ORDER', '获取农户订单详情失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取农户订单详情失败');
            }
            
            logger.info('ORDER', '获取农户订单详情成功', { orderId });
            
            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FARMER_API_URL}/query/${orderId}`, error);
            logger.error('ORDER', '获取农户订单详情失败', {
                orderId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    }
};

