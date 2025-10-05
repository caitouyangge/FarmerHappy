import axios from 'axios';

const API_URL = '/api/v1/auth';

export const authService = {
    async register(userData) {
        try {
            const response = await axios.post(`${API_URL}/register`, userData);
            return response.data;
        } catch (error) {
            throw error.response?.data || error.message;
        }
    },

    async login(credentials) {
        try {
            const response = await axios.post(`${API_URL}/login`, credentials);
            if (response.data.token) {
                localStorage.setItem('user', JSON.stringify(response.data));
            }
            return response.data;
        } catch (error) {
            throw error.response?.data || error.message;
        }
    },

    logout() {
        localStorage.removeItem('user');
    }
};
