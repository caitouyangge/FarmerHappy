                 import axios from 'axios';
import logger from '../utils/logger';

// 融资相关API基础路径
const FINANCING_API_URL = '/api/v1/financing';
const BANK_API_URL = '/api/v1/bank';

export const financingService = {
    // ============= 银行相关接口 =============

    /**
     * 银行发布贷款产品
     * @param {Object} productData - 贷款产品数据
     * @param {string} productData.phone - 银行操作员手机号
     * @param {string} productData.product_name - 产品名称
     * @param {string} [productData.product_code] - 产品编号（可选）
     * @param {number} productData.min_credit_limit - 最低贷款额度要求
     * @param {number} productData.max_amount - 最高贷款额度
     * @param {number} productData.interest_rate - 年利率
     * @param {number} productData.term_months - 贷款期限（月）
     * @param {string} productData.repayment_method - 还款方式（equal_installment/interest_first/bullet_repayment）
     * @param {string} productData.description - 产品描述
     * @returns {Promise<Object>} 响应数据
     */
    async publishLoanProduct(productData) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/loans/products`, {
                product_name: productData.product_name,
                phone: productData.phone
            });
            logger.info('FINANCING', '银行发布贷款产品', { product_name: productData.product_name });

            const response = await axios.post(`${BANK_API_URL}/loans/products`, productData);

            logger.apiResponse('POST', `${BANK_API_URL}/loans/products`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '发布贷款产品失败', {
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

            logger.info('FINANCING', '贷款产品发布成功', {
                product_id: response.data.data?.product_id
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/loans/products`, error);
            logger.error('FINANCING', '发布贷款产品失败', {
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
                message: error.message || '发布贷款产品失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 银行审批贷款申请
     * @param {Object} approvalData - 审批数据
     * @param {string} approvalData.phone - 银行操作员手机号
     * @param {string} approvalData.application_id - 贷款申请ID
     * @param {string} approvalData.action - 审批动作（approve/reject）
     * @param {number} [approvalData.approved_amount] - 批准金额（action为approve时必填）
     * @param {string} [approvalData.reject_reason] - 拒绝原因（action为reject时必填）
     * @returns {Promise<Object>} 响应数据
     */
    async approveLoan(approvalData) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/loans/approve`, {
                application_id: approvalData.application_id,
                action: approvalData.action
            });
            logger.info('FINANCING', '银行审批贷款申请', {
                application_id: approvalData.application_id,
                action: approvalData.action
            });

            const response = await axios.post(`${BANK_API_URL}/loans/approve`, approvalData);

            logger.apiResponse('POST', `${BANK_API_URL}/loans/approve`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '审批贷款申请失败', {
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

            logger.info('FINANCING', '贷款申请审批成功', {
                application_id: approvalData.application_id,
                status: response.data.data?.status
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/loans/approve`, error);
            logger.error('FINANCING', '审批贷款申请失败', {
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
                message: error.message || '审批贷款申请失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 银行审批信贷额度申请
     * @param {Object} approvalData - 审批数据
     * @param {string} approvalData.phone - 银行操作员手机号
     * @param {string} approvalData.application_id - 信贷额度申请ID
     * @param {string} approvalData.action - 审批动作（approve/reject）
     * @param {number} [approvalData.approved_amount] - 批准金额（action为approve时必填）
     * @param {string} [approvalData.reject_reason] - 拒绝原因（action为reject时必填）
     * @returns {Promise<Object>} 响应数据
     */
    async approveCreditApplication(approvalData) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/credit/approve`, {
                application_id: approvalData.application_id,
                action: approvalData.action
            });
            logger.info('FINANCING', '银行审批信贷额度申请', {
                application_id: approvalData.application_id,
                action: approvalData.action
            });

            const response = await axios.post(`${BANK_API_URL}/credit/approve`, approvalData);

            logger.apiResponse('POST', `${BANK_API_URL}/credit/approve`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '审批信贷额度申请失败', {
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

            logger.info('FINANCING', '信贷额度申请审批成功', {
                application_id: approvalData.application_id,
                status: response.data.data?.status
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/credit/approve`, error);
            logger.error('FINANCING', '审批信贷额度申请失败', {
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
                message: error.message || '审批信贷额度申请失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取待审批的信贷额度申请列表
     * @param {string} phone - 银行操作员手机号
     * @returns {Promise<Object>} 响应数据
     */
    async getPendingCreditApplications(phone) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/credit/pending`, { phone });
            logger.info('FINANCING', '获取待审批信贷额度申请列表', { phone });

            const response = await axios.post(`${BANK_API_URL}/credit/pending`, { phone });

            logger.apiResponse('POST', `${BANK_API_URL}/credit/pending`, response.status, {
                code: response.data.code,
                count: response.data.data?.total || 0
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取待审批信贷额度申请列表失败', {
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

            logger.info('FINANCING', '获取待审批信贷额度申请列表成功', {
                count: response.data.data?.total || 0
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/credit/pending`, error);
            logger.error('FINANCING', '获取待审批信贷额度申请列表失败', {
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
                message: error.message || '获取待审批信贷额度申请列表失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取待审批的贷款申请列表
     * @param {string} phone - 银行操作员手机号
     * @returns {Promise<Object>} 响应数据
     */
    async getPendingLoanApplications(phone) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/loans/pending`, { phone });
            logger.info('FINANCING', '获取待审批贷款申请列表', { phone });

            const response = await axios.post(`${BANK_API_URL}/loans/pending`, { phone });

            logger.apiResponse('POST', `${BANK_API_URL}/loans/pending`, response.status, {
                code: response.data.code,
                count: response.data.data?.total || 0
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取待审批贷款申请列表失败', {
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

            logger.info('FINANCING', '获取待审批贷款申请列表成功', {
                count: response.data.data?.total || 0
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/loans/pending`, error);
            logger.error('FINANCING', '获取待审批贷款申请列表失败', {
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
                message: error.message || '获取待审批贷款申请列表失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取已审批待放款的贷款申请列表
     * @param {string} phone - 银行操作员手机号
     * @returns {Promise<Object>} 响应数据
     */
    async getApprovedLoanApplications(phone) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/loans/approved`, { phone });
            logger.info('FINANCING', '获取已审批贷款申请列表', { phone });

            const response = await axios.post(`${BANK_API_URL}/loans/approved`, { phone });

            logger.apiResponse('POST', `${BANK_API_URL}/loans/approved`, response.status, {
                code: response.data.code,
                count: response.data.data?.total || 0
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取已审批贷款申请列表失败', {
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

            logger.info('FINANCING', '获取已审批贷款申请列表成功', {
                count: response.data.data?.total || 0
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/loans/approved`, error);
            logger.error('FINANCING', '获取已审批贷款申请列表失败', {
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
                message: error.message || '获取已审批贷款申请列表失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 银行放款操作
     * @param {Object} disbursementData - 放款数据
     * @param {string} disbursementData.phone - 银行操作员手机号
     * @param {string} disbursementData.application_id - 贷款申请ID
     * @param {number} disbursementData.disburse_amount - 放款金额
     * @param {string} disbursementData.disburse_method - 放款方式（bank_transfer/cash/check）
     * @param {string} disbursementData.first_repayment_date - 首次还款日期（YYYY-MM-DD格式）
     * @param {string} disbursementData.loan_account - 贷款账户
     * @param {string} [disbursementData.remarks] - 放款备注（可选）
     * @returns {Promise<Object>} 响应数据
     */
    async disburseLoan(disbursementData) {
        try {
            logger.apiRequest('POST', `${BANK_API_URL}/loans/disburse`, {
                application_id: disbursementData.application_id,
                disburse_amount: disbursementData.disburse_amount
            });
            logger.info('FINANCING', '银行放款操作', {
                application_id: disbursementData.application_id
            });

            const response = await axios.post(`${BANK_API_URL}/loans/disburse`, disbursementData);

            logger.apiResponse('POST', `${BANK_API_URL}/loans/disburse`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '放款操作失败', {
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

            logger.info('FINANCING', '放款操作成功', {
                loan_id: response.data.data?.loan_id
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${BANK_API_URL}/loans/disburse`, error);
            logger.error('FINANCING', '放款操作失败', {
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
                message: error.message || '放款操作失败，请稍后重试',
                errors: []
            };
        }
    },

    // ============= 农户相关接口 =============

    /**
     * 查询可用贷款额度
     * @param {string} phone - 农户手机号
     * @returns {Promise<Object>} 信用额度信息
     */
    async getCreditLimit(phone) {
        try {
            const requestData = { phone };
            logger.apiRequest('POST', `${FINANCING_API_URL}/credit/limit`, requestData);
            logger.info('FINANCING', '查询可用贷款额度', { phone });

            const response = await axios.post(`${FINANCING_API_URL}/credit/limit`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/credit/limit`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '查询贷款额度失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '查询贷款额度失败');
            }

            logger.info('FINANCING', '查询贷款额度成功', {
                available_limit: response.data.data?.available_limit
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/credit/limit`, error);
            logger.error('FINANCING', '查询贷款额度失败', {
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 申请贷款额度
     * @param {Object} applicationData - 申请数据
     * @param {string} applicationData.phone - 农户手机号
     * @param {string} applicationData.proof_type - 证明类型（land_certificate/property_certificate/income_proof/business_license/other）
     * @param {Array<string>} applicationData.proof_images - 证明材料图片URL数组
     * @param {number} applicationData.apply_amount - 申请额度
     * @param {string} [applicationData.description] - 申请说明（可选）
     * @returns {Promise<Object>} 响应数据
     */
    async applyForCreditLimit(applicationData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/credit/apply`, {
                phone: applicationData.phone,
                apply_amount: applicationData.apply_amount
            });
            logger.info('FINANCING', '申请贷款额度', {
                phone: applicationData.phone,
                apply_amount: applicationData.apply_amount
            });

            const response = await axios.post(`${FINANCING_API_URL}/credit/apply`, applicationData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/credit/apply`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200 && response.data.code !== 409) {
                logger.error('FINANCING', '申请贷款额度失败', {
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

            logger.info('FINANCING', '贷款额度申请提交成功', {
                application_id: response.data.data?.application_id
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/credit/apply`, error);
            logger.error('FINANCING', '申请贷款额度失败', {
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
                message: error.message || '申请贷款额度失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 查询可申请的贷款产品
     * @param {string} phone - 农户手机号
     * @param {number} [credit_limit] - 信用额度（可选，不传则自动查询）
     * @returns {Promise<Object>} 可申请的贷款产品列表
     */
    async getAvailableLoanProducts(phone, credit_limit = null) {
        try {
            const requestData = {
                phone,
                ...(credit_limit && { credit_limit })
            };

            logger.apiRequest('POST', `${FINANCING_API_URL}/loans/products`, requestData);
            logger.info('FINANCING', '查询可申请的贷款产品', { phone, credit_limit });

            const response = await axios.post(`${FINANCING_API_URL}/loans/products`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/loans/products`, response.status, {
                code: response.data.code,
                count: response.data.data?.available_products?.length || 0
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '查询贷款产品失败', {
                    code: response.data.code,
                    message: response.data.message
                });
                throw new Error(response.data.message || '查询贷款产品失败');
            }

            logger.info('FINANCING', '查询贷款产品成功', {
                count: response.data.data?.available_products?.length || 0
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/loans/products`, error);
            logger.error('FINANCING', '查询贷款产品失败', {
                errorMessage: error.response?.data?.message || error.message
            }, error);
            throw error.response?.data?.message || error.message || error;
        }
    },

    /**
     * 申请单人贷款
     * @param {Object} loanData - 贷款申请数据
     * @param {string} loanData.phone - 农户手机号
     * @param {string} loanData.product_id - 贷款产品ID
     * @param {number} loanData.apply_amount - 申请金额
     * @param {string} loanData.purpose - 贷款用途
     * @param {string} loanData.repayment_source - 还款来源说明
     * @returns {Promise<Object>} 响应数据
     */
    async applyForSingleLoan(loanData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/loans/single`, {
                product_id: loanData.product_id,
                apply_amount: loanData.apply_amount
            });
            logger.info('FINANCING', '申请单人贷款', {
                product_id: loanData.product_id,
                apply_amount: loanData.apply_amount
            });

            const response = await axios.post(`${FINANCING_API_URL}/loans/single`, loanData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/loans/single`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200 && response.data.code !== 409) {
                logger.error('FINANCING', '申请单人贷款失败', {
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

            logger.info('FINANCING', '单人贷款申请提交成功', {
                loan_application_id: response.data.data?.loan_application_id
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/loans/single`, error);
            logger.error('FINANCING', '申请单人贷款失败', {
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
                message: error.message || '申请单人贷款失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 申请联合贷款
     * @param {Object} loanData - 联合贷款申请数据
     * @param {string} loanData.phone - 发起农户手机号
     * @param {string} loanData.product_id - 贷款产品ID
     * @param {number} loanData.apply_amount - 申请总金额
     * @param {Array<string>} loanData.partner_phones - 联合贷款伙伴手机号数组（2-5个）
     * @param {string} loanData.purpose - 贷款用途
     * @param {string} loanData.repayment_plan - 还款计划说明
     * @param {boolean} loanData.joint_agreement - 是否同意联合贷款协议
     * @returns {Promise<Object>} 响应数据
     */
    async applyForJointLoan(loanData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/loans/joint`, {
                product_id: loanData.product_id,
                apply_amount: loanData.apply_amount,
                partner_count: loanData.partner_phones?.length || 0
            });
            logger.info('FINANCING', '申请联合贷款', {
                product_id: loanData.product_id,
                apply_amount: loanData.apply_amount,
                partner_count: loanData.partner_phones?.length || 0
            });

            const response = await axios.post(`${FINANCING_API_URL}/loans/joint`, loanData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/loans/joint`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200 && response.data.code !== 409) {
                logger.error('FINANCING', '申请联合贷款失败', {
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

            logger.info('FINANCING', '联合贷款申请提交成功', {
                loan_application_id: response.data.data?.loan_application_id
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/loans/joint`, error);
            logger.error('FINANCING', '申请联合贷款失败', {
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
                message: error.message || '申请联合贷款失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 浏览可联合农户
     * @param {Object} requestData - 请求数据
     * @param {string} requestData.phone - 农户手机号
     * @param {number} [requestData.min_credit_limit] - 最低信用额度要求（可选）
     * @param {number} [requestData.max_partners] - 最大伙伴数量（可选，默认3，最多5）
     * @param {Array<string>} [requestData.exclude_phones] - 排除的手机号数组（可选）
     * @returns {Promise<Object>} 可联合农户列表
     */
    async getJointPartners(requestData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/partners`, {
                phone: requestData.phone,
                max_partners: requestData.max_partners
            });
            logger.info('FINANCING', '浏览可联合农户', {
                phone: requestData.phone,
                max_partners: requestData.max_partners
            });

            const response = await axios.post(`${FINANCING_API_URL}/partners`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/partners`, response.status, {
                code: response.data.code,
                count: response.data.data?.partners?.length || 0
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取可联合农户失败', {
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

            logger.info('FINANCING', '获取可联合农户成功', {
                count: response.data.data?.partners?.length || 0
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/partners`, error);
            logger.error('FINANCING', '获取可联合农户失败', {
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
                message: error.message || '获取可联合农户失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取待确认的联合贷款申请
     * @param {Object} requestData - 请求数据
     * @param {string} requestData.phone - 用户手机号
     * @returns {Promise<Object>} 待确认的联合贷款申请列表
     */
    async getPendingJointLoanApplications(requestData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/pending-joint-loans`, {
                phone: requestData.phone
            });
            logger.info('FINANCING', '获取待确认的联合贷款申请', {
                phone: requestData.phone
            });

            const response = await axios.post(`${FINANCING_API_URL}/pending-joint-loans`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/pending-joint-loans`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取待确认的联合贷款申请失败', {
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

            logger.info('FINANCING', '获取待确认的联合贷款申请成功', {
                total: response.data.data?.total || 0
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/pending-joint-loans`, error);
            logger.error('FINANCING', '获取待确认的联合贷款申请失败', {
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
                message: error.message || '获取待确认的联合贷款申请失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 确认或拒绝联合贷款申请
     * @param {Object} requestData - 请求数据
     * @param {string} requestData.phone - 用户手机号
     * @param {string} requestData.application_id - 贷款申请ID
     * @param {string} requestData.action - 操作类型：'confirm' 或 'reject'
     * @returns {Promise<Object>} 响应数据
     */
    async confirmJointLoanApplication(requestData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/joint-loan-confirmation`, {
                phone: requestData.phone,
                application_id: requestData.application_id,
                action: requestData.action
            });
            logger.info('FINANCING', '确认联合贷款申请', {
                application_id: requestData.application_id,
                action: requestData.action
            });

            const response = await axios.post(`${FINANCING_API_URL}/joint-loan-confirmation`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/joint-loan-confirmation`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '确认联合贷款申请失败', {
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

            logger.info('FINANCING', '确认联合贷款申请成功', {
                application_id: requestData.application_id,
                action: requestData.action
            });

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/joint-loan-confirmation`, error);
            logger.error('FINANCING', '确认联合贷款申请失败', {
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
                message: error.message || '确认联合贷款申请失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 发送联合贷款消息
     * @param {Object} requestData - 请求数据
     * @param {string} requestData.phone - 发送者手机号
     * @param {string} requestData.application_id - 贷款申请ID
     * @param {string} requestData.receiver_phone - 接收者手机号
     * @param {string} requestData.content - 消息内容
     * @returns {Promise<Object>} 响应数据
     */
    async sendJointLoanMessage(requestData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/joint-loan-messages/send`, {
                application_id: requestData.application_id,
                receiver_phone: requestData.receiver_phone
            });

            const response = await axios.post(`${FINANCING_API_URL}/joint-loan-messages/send`, requestData);

            if (response.data.code !== 200) {
                throw {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
            }

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/joint-loan-messages/send`, error);
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
                message: error.message || '发送消息失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取联合贷款消息列表
     * @param {Object} requestData - 请求数据
     * @param {string} requestData.phone - 用户手机号
     * @param {string} requestData.application_id - 贷款申请ID
     * @returns {Promise<Object>} 消息列表
     */
    async getJointLoanMessages(requestData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/joint-loan-messages`, {
                application_id: requestData.application_id
            });

            const response = await axios.post(`${FINANCING_API_URL}/joint-loan-messages`, requestData);

            if (response.data.code !== 200) {
                throw {
                    code: response.data.code,
                    message: response.data.message,
                    errors: response.data.errors || []
                };
            }

            return response.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/joint-loan-messages`, error);
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
                message: error.message || '获取消息失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取智能贷款推荐
     * @param {Object} requestData - 请求数据
     * @param {string} requestData.phone - 用户手机号
     * @param {number} requestData.product_id - 贷款产品ID
     * @param {number} requestData.apply_amount - 申请金额
     * @returns {Promise<Object>} 智能推荐结果
     */
    async getSmartLoanRecommendation(requestData) {
        try {
            logger.apiRequest('POST', `${FINANCING_API_URL}/smart-recommendation`, {
                phone: requestData.phone,
                product_id: requestData.product_id,
                apply_amount: requestData.apply_amount
            });
            logger.info('FINANCING', '获取智能贷款推荐', {
                product_id: requestData.product_id,
                apply_amount: requestData.apply_amount
            });

            const response = await axios.post(`${FINANCING_API_URL}/smart-recommendation`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/smart-recommendation`, response.status, {
                code: response.data.code,
                recommendation_type: response.data.data?.recommendation_type
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取智能贷款推荐失败', {
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

            logger.info('FINANCING', '获取智能贷款推荐成功', {
                recommendation_type: response.data.data?.recommendation_type,
                can_apply_single: response.data.data?.can_apply_single,
                can_apply_joint: response.data.data?.can_apply_joint
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/smart-recommendation`, error);
            logger.error('FINANCING', '获取智能贷款推荐失败', {
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
                message: error.message || '获取智能贷款推荐失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取还款计划
     * @param {string} phone - 农户手机号
     * @param {string} loan_id - 贷款ID
     * @returns {Promise<Object>} 还款计划信息
     */
    async getRepaymentSchedule(phone, loan_id) {
        try {
            const requestData = { phone, loan_id };
            logger.apiRequest('POST', `${FINANCING_API_URL}/repayment/schedule`, requestData);
            logger.info('FINANCING', '获取还款计划', { phone, loan_id });

            const response = await axios.post(`${FINANCING_API_URL}/repayment/schedule`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/repayment/schedule`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取还款计划失败', {
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

            logger.info('FINANCING', '获取还款计划成功', { loan_id });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/repayment/schedule`, error);
            logger.error('FINANCING', '获取还款计划失败', {
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
                message: error.message || '获取还款计划失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取农户申请记录
     * @param {string} phone - 农户手机号
     * @returns {Promise<Object>} 申请记录列表
     */
    async getFarmerCreditApplications(phone) {
        try {
            const requestData = { phone };
            logger.apiRequest('POST', `${FINANCING_API_URL}/credit/applications`, requestData);
            logger.info('FINANCING', '获取农户申请记录', { phone });

            const response = await axios.post(`${FINANCING_API_URL}/credit/applications`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/credit/applications`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取申请记录失败', {
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

            logger.info('FINANCING', '获取申请记录成功', { 
                count: response.data.data?.total || 0 
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/credit/applications`, error);
            logger.error('FINANCING', '获取申请记录失败', {
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
                message: error.message || '获取申请记录失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取农户已放款贷款列表
     * @param {string} phone - 农户手机号
     * @returns {Promise<Object>} 已放款贷款列表
     */
    async getFarmerLoans(phone) {
        try {
            const requestData = { phone };
            logger.apiRequest('POST', `${FINANCING_API_URL}/loans/list`, requestData);
            logger.info('FINANCING', '获取农户贷款列表', { phone });

            const response = await axios.post(`${FINANCING_API_URL}/loans/list`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/loans/list`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取贷款列表失败', {
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

            logger.info('FINANCING', '获取贷款列表成功', { 
                count: response.data.data?.total || 0 
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/loans/list`, error);
            logger.error('FINANCING', '获取贷款列表失败', {
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
                message: error.message || '获取贷款列表失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 执行还款
     * @param {string} phone - 农户手机号
     * @param {string} loanId - 贷款ID
     * @param {number} repaymentAmount - 还款金额
     * @param {string} repaymentMethod - 还款方式 (normal: 正常还款, partial: 部分还款, advance: 提前还款)
     * @param {string} paymentAccount - 付款账户
     * @param {string} remarks - 备注
     * @returns {Promise<Object>} 还款结果
     */
    async makeRepayment(phone, loanId, repaymentAmount, repaymentMethod, paymentAccount, remarks) {
        try {
            const requestData = {
                phone,
                loan_id: loanId,
                repayment_amount: repaymentAmount,
                repayment_method: repaymentMethod,
                payment_account: paymentAccount,
                remarks
            };
            
            logger.apiRequest('POST', `${FINANCING_API_URL}/repayment`, requestData);
            logger.info('FINANCING', '执行还款', { phone, loanId, repaymentAmount });

            const response = await axios.post(`${FINANCING_API_URL}/repayment`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/repayment`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '还款失败', {
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

            logger.info('FINANCING', '还款成功', { loanId });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/repayment`, error);
            logger.error('FINANCING', '还款失败', {
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
                message: error.message || '还款失败，请稍后重试',
                errors: []
            };
        }
    },

    /**
     * 获取农户贷款申请记录
     * @param {string} phone - 农户手机号
     * @returns {Promise<Object>} 贷款申请记录列表
     */
    async getFarmerLoanApplications(phone) {
        try {
            const requestData = { phone };
            logger.apiRequest('POST', `${FINANCING_API_URL}/loans/applications`, requestData);
            logger.info('FINANCING', '获取贷款申请记录', { phone });

            const response = await axios.post(`${FINANCING_API_URL}/loans/applications`, requestData);

            logger.apiResponse('POST', `${FINANCING_API_URL}/loans/applications`, response.status, {
                code: response.data.code
            });

            if (response.data.code !== 200) {
                logger.error('FINANCING', '获取贷款申请记录失败', {
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

            logger.info('FINANCING', '获取贷款申请记录成功', { 
                count: response.data.data?.total || 0 
            });

            return response.data.data;
        } catch (error) {
            logger.apiError('POST', `${FINANCING_API_URL}/loans/applications`, error);
            logger.error('FINANCING', '获取贷款申请记录失败', {
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
                message: error.message || '获取贷款申请记录失败，请稍后重试',
                errors: []
            };
        }
    }
};

