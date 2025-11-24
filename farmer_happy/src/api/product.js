import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/farmer/products';

export const productService = {
    // 获取产品列表
    async getProductList(phone, status = null, title = null) {
        try {
            const requestData = {
                phone,
                ...(status && { status }),
                ...(title && { title })
            };

            logger.apiRequest('POST', `${API_URL}/list_query`, requestData);
            logger.info('PRODUCT', '获取产品列表', { phone, status, title });

            const response = await axios.post(`${API_URL}/list_query`, requestData);

            logger.apiResponse('POST', `${API_URL}/list_query`, response.status, {
                code: response.data.code,
                count: response.data.data?.list?.length || 0
            });

            if (response.data.code !== 200) {
                logger.error('PRODUCT', '获取产品列表失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取产品列表失败');
            }

            logger.info('PRODUCT', '获取产品列表成功', {
                count: response.data.data?.list?.length || 0
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/list_query`, error);
            logger.error('PRODUCT', '获取产品列表失败', {
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    // 创建产品
    async createProduct(productData) {
        try {
            logger.apiRequest('POST', API_URL, {
                title: productData.title,
                category: productData.category
            });
            logger.info('PRODUCT', '创建产品', { title: productData.title });

            const response = await axios.post(API_URL, productData);

            logger.apiResponse('POST', API_URL, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 201 && response.data.code !== 200) {
                logger.error('PRODUCT', '创建产品失败', {
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

            logger.info('PRODUCT', '产品创建成功', {
                productId: response.data.data?.product_id
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', API_URL, error);
            logger.error('PRODUCT', '创建产品失败', {
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
                message: error.message || '创建产品失败，请稍后重试',
                errors: []
            };
        }
    },

    // 获取产品详情
    async getProductDetail(productId, phone) {
        try {
            const url = `${API_URL}/query/${productId}`;
            logger.apiRequest('POST', url, { phone });
            logger.info('PRODUCT', '获取产品详情', { productId });

            const response = await axios.post(url, { phone });

            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('PRODUCT', '获取产品详情失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取产品详情失败');
            }

            logger.info('PRODUCT', '获取产品详情成功', { productId });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/query/${productId}`, error);
            logger.error('PRODUCT', '获取产品详情失败', {
                productId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    // 更新产品
    async updateProduct(productId, productData) {
        try {
            const url = `${API_URL}/${productId}`;
            logger.apiRequest('PUT', url, { title: productData.title });
            logger.info('PRODUCT', '更新产品', { productId });

            const response = await axios.put(url, productData);

            logger.apiResponse('PUT', url, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('PRODUCT', '更新产品失败', {
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

            logger.info('PRODUCT', '产品更新成功', { productId });

            return response.data;
        } catch (error) {
            logger.apiError('PUT', `${API_URL}/${productId}`, error);
            logger.error('PRODUCT', '更新产品失败', {
                productId,
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
                message: error.message || '更新产品失败，请稍后重试',
                errors: []
            };
        }
    },

    // 删除产品
    async deleteProduct(productId, phone) {
        try {
            const url = `${API_URL}/${productId}`;
            logger.apiRequest('DELETE', url, { phone });
            logger.info('PRODUCT', '删除产品', { productId });

            const response = await axios.delete(url, { data: { phone } });

            logger.apiResponse('DELETE', url, response.status, {
                code: response.data.code
            });

            if (response.data.code != 204 && response.data.code != 200) {
                logger.error('PRODUCT', '删除产品失败1', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '删除产品失败0');
            }

            logger.info('PRODUCT', '产品删除成功', { productId });

            return response.data;
        } catch (error) {
            logger.apiError('DELETE', `${API_URL}/${productId}`, error);
            logger.error('PRODUCT', '删除产品失败2', {
                productId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    // 商品上架
    async onShelfProduct(productId, phone) {
        try {
            const url = `${API_URL}/${productId}/on-shelf`;
            logger.apiRequest('POST', url, { phone });
            logger.info('PRODUCT', '商品上架', { productId });

            const response = await axios.post(url, { phone });

            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('PRODUCT', '商品上架失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '商品上架失败');
            }

            logger.info('PRODUCT', '商品上架成功', { productId });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/${productId}/on-shelf`, error);
            logger.error('PRODUCT', '商品上架失败', {
                productId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    // 商品下架
    async offShelfProduct(productId, phone) {
        try {
            const url = `${API_URL}/${productId}/off-shelf`;
            logger.apiRequest('POST', url, { phone });
            logger.info('PRODUCT', '商品下架', { productId });

            const response = await axios.post(url, { phone });

            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('PRODUCT', '商品下架失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '商品下架失败');
            }

            logger.info('PRODUCT', '商品下架成功', { productId });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/${productId}/off-shelf`, error);
            logger.error('PRODUCT', '商品下架失败', {
                productId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    // 批量操作商品
    async batchActionProducts(action, productIds, phone) {
        try {
            const url = `${API_URL}/batch-actions`;
            const requestData = {
                action,
                product_ids: productIds,
                phone
            };

            logger.apiRequest('POST', url, { action, count: productIds.length });
            logger.info('PRODUCT', '批量操作商品', { action, count: productIds.length });

            const response = await axios.post(url, requestData);

            logger.apiResponse('POST', url, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('PRODUCT', '批量操作商品失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '批量操作失败');
            }

            logger.info('PRODUCT', '批量操作商品成功', {
                action,
                successCount: response.data.data?.success_count,
                failureCount: response.data.data?.failure_count
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/batch-actions`, error);
            logger.error('PRODUCT', '批量操作商品失败', {
                action,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    }
};

