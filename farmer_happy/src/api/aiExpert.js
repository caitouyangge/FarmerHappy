import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/ai';

export const aiExpertService = {
  /**
   * 向 AI 农业专家提问
   * POST /api/v1/ai/expert-chat
   * @param {string} question - 农户提问内容
   */
  async askExpert(question) {
    try {
      logger.apiRequest('POST', `${API_URL}/expert-chat`, { question });
      logger.info('AI_EXPERT', '向AI农业专家提问', { questionPreview: (question || '').slice(0, 50) });

      const response = await axios.post(`${API_URL}/expert-chat`, { question });

      logger.apiResponse('POST', `${API_URL}/expert-chat`, response.status, {
        code: response.data.code,
      });

      if (response.data.code !== 200) {
        logger.error('AI_EXPERT', 'AI回答失败', {
          code: response.data.code,
          message: response.data.message,
          errorType: response.data.error_type,
        });
        
        // 针对不同错误类型提供更友好的提示
        let errorMsg = response.data.message || 'AI 农业专家服务暂不可用，请稍后重试';
        if (response.data.code === 429 || response.data.error_type === 'rate_limit') {
          errorMsg = 'AI服务当前负载较高，请稍等片刻后再试';
        } else if (response.data.error_type === 'bad_request') {
          errorMsg = 'AI服务配置错误，请联系管理员';
        }
        
        const error = new Error(errorMsg);
        error.code = response.data.code;
        error.errorType = response.data.error_type;
        throw error;
      }

      const answer = response.data.data?.answer || '';
      return {
        answer,
        raw: response.data.data?.raw_response,
      };
    } catch (error) {
      logger.apiError('POST', `${API_URL}/expert-chat`, error);
      logger.error(
        'AI_EXPERT',
        '调用AI农业专家接口失败',
        {
          errorMessage: error.response?.data?.message || error.message,
        },
        error
      );

      throw new Error(error.message || 'AI 农业专家服务调用失败，请稍后重试');
    }
  },
};


