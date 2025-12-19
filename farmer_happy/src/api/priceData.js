import axios from 'axios';
import logger from '../utils/logger';

const API_URL = '/api/v1/agriculture';

export const priceDataService = {
  /**
   * 获取农产品价格数据
   * POST /api/v1/agriculture/price
   * @param {string} startTime - 开始时间，格式为YYYY-MM-DD
   * @param {string} endTime - 结束时间，格式为YYYY-MM-DD
   * @param {string} productName - 产品名称
   */
  async getPriceData(startTime, endTime, productName) {
    try {
      logger.apiRequest('POST', `${API_URL}/price`, { startTime, endTime, productName });
      logger.info('PRICE_DATA', '获取农产品价格数据', { startTime, endTime, productName });

      const response = await axios.post(`${API_URL}/price`, {
        start_time: startTime,
        end_time: endTime,
        product_name: productName
      });

      logger.apiResponse('POST', `${API_URL}/price`, response.status, {
        code: response.data.code,
      });

      if (response.data.code !== 200) {
        logger.error('PRICE_DATA', '获取价格数据失败', {
          code: response.data.code,
          message: response.data.message,
        });
        throw new Error(response.data.message || '获取价格数据失败');
      }

      return response.data.data;
    } catch (error) {
      logger.apiError('POST', `${API_URL}/price`, error);
      logger.error('PRICE_DATA', '获取价格数据失败', {
        errorMessage: error.response?.data?.message || error.message,
      }, error);
      throw new Error(error.response?.data?.message || error.message || '获取价格数据失败，请稍后重试');
    }
  },

  /**
   * 下载文件（CSV/XLS/XLSX）
   * GET /api/v1/agriculture/price/download?file_name=xxx
   * @param {string} fileName - 文件名（可为 .csv/.xls/.xlsx）
   * @param {object} options - 可选参数
   * @param {string} options.scope - root|split|dir|placed
   * @param {string} options.dir - scope=dir 时需要
   * @param {string} options.location - scope=placed 时需要（兼容）
   */
  async downloadCsvFile(fileName, options = {}) {
    try {
      const params = { file_name: fileName };
      if (options.scope) params.scope = options.scope;
      if (options.dir) params.dir = options.dir;
      if (options.location) params.location = options.location;

      logger.apiRequest('GET', `${API_URL}/price/download`, params);
      logger.info('PRICE_DATA', '下载文件', params);

      const response = await axios.get(`${API_URL}/price/download`, {
        params,
        responseType: 'blob' // 重要：指定响应类型为blob
      });

      logger.apiResponse('GET', `${API_URL}/price/download`, response.status);
      return response.data;
    } catch (error) {
      logger.apiError('GET', `${API_URL}/price/download`, error);
      logger.error('PRICE_DATA', '下载文件失败', {
        errorMessage: error.response?.data?.message || error.message,
      }, error);
      throw new Error(error.response?.data?.message || error.message || '下载文件失败，请稍后重试');
    }
  },

  /**
   * 获取 split 文件列表（可用于前端展示可选品种）
   * GET /api/v1/agriculture/price/split/list
   */
  async listSplitFiles() {
    try {
      logger.apiRequest('GET', `${API_URL}/price/split/list`);
      const response = await axios.get(`${API_URL}/price/split/list`);
      logger.apiResponse('GET', `${API_URL}/price/split/list`, response.status, { code: response.data.code });
      if (response.data.code !== 200) {
        throw new Error(response.data.message || '获取split列表失败');
      }
      return response.data.data;
    } catch (error) {
      logger.apiError('GET', `${API_URL}/price/split/list`, error);
      throw new Error(error.response?.data?.message || error.message || '获取split列表失败，请稍后重试');
    }
  }
  ,

  /**
   * 批量将 result/split 下已有 CSV 导出为 XLSX（不需要重新爬取）
   * POST /api/v1/agriculture/price/split/export_xlsx
   */
  async exportSplitXlsx() {
    try {
      logger.apiRequest('POST', `${API_URL}/price/split/export_xlsx`);
      const response = await axios.post(`${API_URL}/price/split/export_xlsx`, {});
      logger.apiResponse('POST', `${API_URL}/price/split/export_xlsx`, response.status, { code: response.data.code });
      if (response.data.code !== 200) {
        throw new Error(response.data.message || '导出xlsx失败');
      }
      return response.data.data;
    } catch (error) {
      logger.apiError('POST', `${API_URL}/price/split/export_xlsx`, error);
      throw new Error(error.response?.data?.message || error.message || '导出xlsx失败，请稍后重试');
    }
  }
};

