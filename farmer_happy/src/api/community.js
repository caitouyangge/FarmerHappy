import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/content';
const STORAGE_API_URL = '/api/v1/storage';
const COMMENT_API_URL = '/api/v1/comment';

export const communityService = {
    /**
     * 发布内容
     * POST /api/v1/content/publish
     * @param {Object} contentData - 内容数据
     * @param {string} contentData.title - 标题
     * @param {string} contentData.content - 内容
     * @param {string} contentData.content_type - 内容类型 (articles, questions, experiences)
     * @param {Array<string>} contentData.images - 图片数组（可选）
     * @param {string} contentData.phone - 用户手机号
     */
    async publishContent(contentData) {
        try {
            logger.apiRequest('POST', `${API_URL}/publish`, {
                title: contentData.title,
                content_type: contentData.content_type,
                phone: contentData.phone
            });
            logger.info('COMMUNITY', '发布内容', { 
                contentType: contentData.content_type,
                phone: contentData.phone 
            });
            
            const response = await axios.post(`${API_URL}/publish`, contentData);
            
            logger.apiResponse('POST', `${API_URL}/publish`, response.status, {
                code: response.data.code,
                success: response.data.code === 201
            });
            
            if (response.data.code !== 201) {
                logger.error('COMMUNITY', '发布内容失败', {
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
            
            logger.info('COMMUNITY', '内容发布成功', { 
                contentId: response.data.data?.content_id 
            });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/publish`, error);
            logger.error('COMMUNITY', '发布内容失败', {
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
                message: error.message || '发布内容失败，请稍后重试',
                errors: []
            };
        }
    },

    async uploadImages(images) {
        try {
            logger.apiRequest('POST', `${STORAGE_API_URL}/upload`);
            const response = await axios.post(`${STORAGE_API_URL}/upload`, { images });
            logger.apiResponse('POST', `${STORAGE_API_URL}/upload`, response.status, {
                code: response.data.code
            });
            if (response.data.code !== 201) {
                const errorObj = {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
                throw errorObj;
            }
            const urls = response.data.data?.urls || [];
            return urls;
        } catch (error) {
            logger.apiError('POST', `${STORAGE_API_URL}/upload`, error);
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
                message: error.message || '图片上传失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取内容列表
     * GET /api/v1/content/list
     * @param {Object} params - 查询参数
     * @param {string} params.content_type - 内容类型（可选）
     * @param {string} params.keyword - 搜索关键词（可选）
     * @param {string} params.sort - 排序方式：newest, hottest, commented（可选）
     */
    async getContentList(params = {}) {
        try {
            const queryParams = new URLSearchParams();
            if (params.content_type) {
                queryParams.append('content_type', params.content_type);
            }
            if (params.keyword) {
                queryParams.append('keyword', params.keyword);
            }
            if (params.sort) {
                queryParams.append('sort', params.sort);
            }
            
            const url = `${API_URL}/list${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
            
            logger.apiRequest('GET', url);
            logger.info('COMMUNITY', '获取内容列表', params);
            
            const response = await axios.get(url);
            
            logger.apiResponse('GET', url, response.status, {
                code: response.data.code,
                count: response.data.data?.list?.length || 0
            });
            
            if (response.data.code !== 200) {
                logger.error('COMMUNITY', '获取内容列表失败', {
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
            
            logger.info('COMMUNITY', '获取内容列表成功', { 
                count: response.data.data?.list?.length || 0 
            });
            
            return response.data.data;
        } catch (error) {
            logger.apiError('GET', `${API_URL}/list`, error);
            logger.error('COMMUNITY', '获取内容列表失败', {
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
                message: error.message || '获取内容列表失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取内容详情
     * GET /api/v1/content/{content_id}
     * @param {string} contentId - 内容ID
     */
    async getContentDetail(contentId) {
        try {
            const url = `${API_URL}/${contentId}`;
            logger.apiRequest('GET', url);
            logger.info('COMMUNITY', '获取内容详情', { contentId });
            
            const response = await axios.get(url);
            
            logger.apiResponse('GET', url, response.status, {
                code: response.data.code
            });
            
            if (response.data.code !== 200) {
                logger.error('COMMUNITY', '获取内容详情失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取内容详情失败');
            }
            
            logger.info('COMMUNITY', '获取内容详情成功', { contentId });
            
            return response.data.data;
        } catch (error) {
            logger.apiError('GET', `${API_URL}/${contentId}`, error);
            logger.error('COMMUNITY', '获取内容详情失败', {
                contentId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 对帖子发表评论
     * POST /api/v1/content/{content_id}/comments
     * @param {string} contentId - 内容ID
     * @param {Object} commentData - 评论数据
     * @param {string} commentData.comment - 评论内容
     * @param {string} commentData.phone - 用户手机号
     */
    async postComment(contentId, commentData) {
        try {
            const url = `${API_URL}/${contentId}/comments`;
            logger.apiRequest('POST', url, {
                phone: commentData.phone
            });
            logger.info('COMMUNITY', '发表评论', { contentId, phone: commentData.phone });
            
            const response = await axios.post(url, commentData);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code,
                success: response.data.code === 201
            });
            
            if (response.data.code !== 201) {
                logger.error('COMMUNITY', '发表评论失败', {
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
            
            logger.info('COMMUNITY', '评论发表成功', { 
                contentId,
                commentId: response.data.data?.comment_id 
            });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', `${API_URL}/${contentId}/comments`, error);
            logger.error('COMMUNITY', '发表评论失败', {
                contentId,
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
                message: error.message || '发表评论失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取评论列表
     * GET /api/v1/content/{content_id}/comments
     * @param {string} contentId - 内容ID
     */
    async getCommentList(contentId) {
        try {
            const url = `${API_URL}/${contentId}/comments`;
            logger.apiRequest('GET', url);
            logger.info('COMMUNITY', '获取评论列表', { contentId });
            
            const response = await axios.get(url);
            
            logger.apiResponse('GET', url, response.status, {
                code: response.data.code,
                count: response.data.data?.comments?.length || 0
            });
            
            if (response.data.code !== 200) {
                logger.error('COMMUNITY', '获取评论列表失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '获取评论列表失败');
            }
            
            // 修正数据结构：后端返回 list，前端期望 comments
            const commentData = {
                ...response.data.data,
                comments: response.data.data.list || []
            };
            
            logger.info('COMMUNITY', '获取评论列表成功', { 
                contentId,
                count: commentData.comments?.length || 0 
            });
            
            return commentData;
        } catch (error) {
            logger.apiError('GET', `${API_URL}/${contentId}/comments`, error);
            logger.error('COMMUNITY', '获取评论列表失败', {
                contentId,
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 回复评论
     * POST /api/v1/comment/{comment_id}/replies
     * @param {string} commentId - 评论ID
     * @param {Object} replyData - 回复数据
     * @param {string} replyData.comment - 回复内容
     * @param {string} replyData.phone - 用户手机号
     */
    async postReply(commentId, replyData) {
        try {
            const url = `${COMMENT_API_URL}/${commentId}/replies`;
            logger.apiRequest('POST', url, {
                phone: replyData.phone
            });
            logger.info('COMMUNITY', '回复评论', { commentId, phone: replyData.phone });
            
            const response = await axios.post(url, replyData);
            
            logger.apiResponse('POST', url, response.status, {
                code: response.data.code,
                success: response.data.code === 201
            });
            
            if (response.data.code !== 201) {
                logger.error('COMMUNITY', '回复评论失败', {
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
            
            logger.info('COMMUNITY', '回复评论成功', { 
                commentId,
                replyId: response.data.data?.reply_id 
            });
            
            return response.data;
        } catch (error) {
            logger.apiError('POST', `${COMMENT_API_URL}/${commentId}/replies`, error);
            logger.error('COMMUNITY', '回复评论失败', {
                commentId,
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
                message: error.message || '回复评论失败，请稍后重试',
                errors: []
            };
        }
    }
};

