import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/farmer/price-prediction';

export const pricePredictionService = {
  /**
   * 上传Excel文件
   * POST /api/v1/farmer/price-prediction/upload
   * @param {File} file - Excel文件
   */
  async uploadExcel(file) {
    try {
      logger.apiRequest('POST', `${API_URL}/upload`, { fileName: file.name });
      logger.info('PRICE_PREDICTION', '上传Excel文件', { fileName: file.name, fileSize: file.size });

      // 将文件转换为Base64
      const base64 = await fileToBase64(file);
      
      const response = await axios.post(`${API_URL}/upload`, {
        file: base64,
        fileName: file.name
      });

      logger.apiResponse('POST', `${API_URL}/upload`, response.status, {
        code: response.data.code,
      });

      if (response.data.code !== 200) {
        logger.error('PRICE_PREDICTION', '上传Excel失败', {
          code: response.data.code,
          message: response.data.message,
        });
        throw new Error(response.data.message || '上传Excel文件失败');
      }

      return response.data.data;
    } catch (error) {
      logger.apiError('POST', `${API_URL}/upload`, error);
      logger.error('PRICE_PREDICTION', '上传Excel文件失败', {
        errorMessage: error.response?.data?.message || error.message,
      }, error);
      throw new Error(error.response?.data?.message || error.message || '上传Excel文件失败，请稍后重试');
    }
  },

  /**
   * 预测价格
   * POST /api/v1/farmer/price-prediction/predict
   * @param {string} fileId - 文件ID
   * @param {number} predictionDays - 预测天数
   * @param {string} modelType - 模型类型 (linear, polynomial_2, polynomial_3)
   */
  async predictPrice(fileId, predictionDays = 30, modelType = 'linear') {
    try {
      logger.apiRequest('POST', `${API_URL}/predict`, { fileId, predictionDays, modelType });
      logger.info('PRICE_PREDICTION', '开始价格预测', { fileId, predictionDays, modelType });

      const response = await axios.post(`${API_URL}/predict`, {
        file_id: fileId,
        prediction_days: predictionDays,
        model_type: modelType
      });

      logger.apiResponse('POST', `${API_URL}/predict`, response.status, {
        code: response.data.code,
      });

      if (response.data.code !== 200) {
        logger.error('PRICE_PREDICTION', '价格预测失败', {
          code: response.data.code,
          message: response.data.message,
        });
        throw new Error(response.data.message || '价格预测失败');
      }

      return response.data.data;
    } catch (error) {
      logger.apiError('POST', `${API_URL}/predict`, error);
      logger.error('PRICE_PREDICTION', '价格预测失败', {
        errorMessage: error.response?.data?.message || error.message,
      }, error);
      throw new Error(error.response?.data?.message || error.message || '价格预测失败，请稍后重试');
    }
  }
};

/**
 * 将文件转换为Base64字符串
 */
function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      // reader.result 是 data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,...
      resolve(reader.result);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}



