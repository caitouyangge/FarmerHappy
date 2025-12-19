import axios from 'axios';
import logger from '../utils/logger';

const BASE = '/api/v1/expert';

export const expertAppointmentService = {
  async getExperts() {
    try {
      const url = `${BASE}/experts/list`;
      logger.apiRequest('POST', url);
      const res = await axios.post(url, {});
      logger.apiResponse('POST', url, res.status, { code: res.data.code, count: res.data.data?.list?.length || 0 });
      if (res.data.code !== 200) throw new Error(res.data.message || '获取专家列表失败');
      return res.data.data?.list || [];
    } catch (error) {
      logger.apiError('POST', `${BASE}/experts/list`, error);
      throw error.response?.data?.message || error.message || error;
    }
  },

  async applyAppointment({ farmer_phone, mode, expert_ids, message }) {
    try {
      const url = `${BASE}/appointments/apply`;
      logger.apiRequest('POST', url, { farmer_phone, mode, expert_ids_count: expert_ids?.length || 0 });
      const res = await axios.post(url, { farmer_phone, mode, expert_ids, message });
      logger.apiResponse('POST', url, res.status, { code: res.data.code });
      if (res.data.code !== 201) throw new Error(res.data.message || '提交预约失败');
      return res.data.data;
    } catch (error) {
      logger.apiError('POST', `${BASE}/appointments/apply`, error);
      throw error.response?.data?.message || error.message || error;
    }
  },

  async getExpertAppointments(phone) {
    try {
      const url = `${BASE}/appointments/expert/list`;
      logger.apiRequest('POST', url, { phone });
      const res = await axios.post(url, { phone });
      logger.apiResponse('POST', url, res.status, { code: res.data.code });
      if (res.data.code !== 200) throw new Error(res.data.message || '获取预约请求失败');
      return res.data.data?.list || [];
    } catch (error) {
      logger.apiError('POST', `${BASE}/appointments/expert/list`, error);
      throw error.response?.data?.message || error.message || error;
    }
  },

  async getFarmerAppointments(phone) {
    try {
      const url = `${BASE}/appointments/farmer/list`;
      logger.apiRequest('POST', url, { phone });
      const res = await axios.post(url, { phone });
      logger.apiResponse('POST', url, res.status, { code: res.data.code });
      if (res.data.code !== 200) throw new Error(res.data.message || '获取预约记录失败');
      return res.data.data?.list || [];
    } catch (error) {
      logger.apiError('POST', `${BASE}/appointments/farmer/list`, error);
      throw error.response?.data?.message || error.message || error;
    }
  },

  async decideAppointment(appointment_id, { expert_phone, action, expert_note, scheduled_time, location }) {
    try {
      const url = `${BASE}/appointments/${appointment_id}/decision`;
      const payload = { expert_phone, action, expert_note, scheduled_time, location };
      logger.apiRequest('POST', url, { appointment_id, action });
      const res = await axios.post(url, payload);
      logger.apiResponse('POST', url, res.status, { code: res.data.code });
      if (res.data.code !== 200) throw new Error(res.data.message || '处理预约失败');
      return res.data.data;
    } catch (error) {
      logger.apiError('POST', `${BASE}/appointments/{id}/decision`, error);
      throw error.response?.data?.message || error.message || error;
    }
  },

  async getAppointmentDetail(appointment_id) {
    try {
      const url = `${BASE}/appointments/query/${appointment_id}`;
      logger.apiRequest('POST', url, { appointment_id });
      const res = await axios.post(url, {});
      logger.apiResponse('POST', url, res.status, { code: res.data.code });
      if (res.data.code !== 200) throw new Error(res.data.message || '获取预约详情失败');
      return res.data.data?.detail || null;
    } catch (error) {
      logger.apiError('POST', `${BASE}/appointments/query/{id}`, error);
      throw error.response?.data?.message || error.message || error;
    }
  }
};