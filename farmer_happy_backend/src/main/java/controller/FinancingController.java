// src/controller/FinancingController.java
package controller;

import dto.bank.LoanApprovalRequestDTO;
import dto.bank.LoanApprovalResponseDTO;
import dto.bank.LoanDisbursementRequestDTO;
import dto.bank.LoanDisbursementResponseDTO;
import service.financing.FinancingService;
import dto.financing.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinancingController {
    private FinancingService financingService;

    public FinancingController() {
        this.financingService = new FinancingService();
    }

    public Map<String, Object> publishLoanProduct(BankLoanProductRequestDTO request) {
        try {
            BankLoanProductResponseDTO response = financingService.publishLoanProduct(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "发布成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "参数验证失败");
            // 这里可以进一步解析错误信息并构造errors数组
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            // 检查是否是重复product_code错误
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("product_code")) {
                result.put("code", 400);
                result.put("message", "参数验证失败");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "product_code");
                error.put("message", "产品编号已存在，请使用其他编号");
                errors.add(error);
                result.put("errors", errors);
            } else {
                result.put("code", 500);
                result.put("message", e.getMessage());
            }
            return result;
        }
    }

    public Map<String, Object> approveLoan(LoanApprovalRequestDTO request) {
        try {
            LoanApprovalResponseDTO response = financingService.approveLoan(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "贷款申请已批准");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("无银行审批权限")) {
                result.put("code", 403);
                result.put("message", "无审批权限");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "phone");
                error.put("message", "该用户无银行审批权限");
                errors.add(error);
                result.put("errors", errors);
            } else if (e.getMessage().contains("指定的申请ID不存在")) {
                result.put("code", 404);
                result.put("message", "贷款申请不存在");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "application_id");
                error.put("message", "指定的申请ID不存在");
                errors.add(error);
                result.put("errors", errors);
            } else if (e.getMessage().contains("不能重复审批")) {
                result.put("code", 400);
                result.put("message", "申请状态不允许审批");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "application_id");
                error.put("message", "该申请已批准，不能重复审批");
                errors.add(error);
                result.put("errors", errors);
            } else {
                result.put("code", 400);
                result.put("message", "参数验证失败");
                // 可以进一步解析错误信息并构造errors数组
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getAvailableLoanProducts(LoanProductsRequestDTO request) {
        try {
            LoanProductsResponseDTO response = financingService.getAvailableLoanProducts(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 404);
            result.put("message", "用户不存在");
            // 这里可以进一步解析错误信息并构造errors数组
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> disburseLoan(LoanDisbursementRequestDTO request) {
        try {
            LoanDisbursementResponseDTO response = financingService.disburseLoan(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "贷款已放款");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("无银行放款权限")) {
                result.put("code", 403);
                result.put("message", "无放款权限");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "phone");
                error.put("message", "该用户无银行放款权限");
                errors.add(error);
                result.put("errors", errors);
            } else if (e.getMessage().contains("指定的申请ID不存在")) {
                result.put("code", 404);
                result.put("message", "贷款申请不存在");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "application_id");
                error.put("message", "指定的申请ID不存在");
                errors.add(error);
                result.put("errors", errors);
            } else if (e.getMessage().contains("不能重复放款")) {
                result.put("code", 400);
                result.put("message", "申请状态不允许放款");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "application_id");
                error.put("message", "该申请已放款，不能重复放款");
                errors.add(error);
                result.put("errors", errors);
            } else {
                result.put("code", 400);
                result.put("message", "参数验证失败");
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }


    public Map<String, Object> getCreditLimit(CreditLimitRequestDTO request) {
        try {
            CreditLimitDTO response = financingService.getCreditLimit(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 404);
            result.put("message", "用户不存在");
            // 这里可以进一步解析错误信息并构造errors数组
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> applyForCreditLimit(CreditApplicationRequestDTO request) {
        try {
            CreditApplicationDTO response = financingService.applyForCreditLimit(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "申请提交成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("待审批")) {
                // 处理存在待审批申请的情况
                result.put("code", 409);
                result.put("message", "存在待审批的额度申请");
                // 这里可以进一步构造data字段返回现有申请信息
            } else {
                result.put("code", 400);
                result.put("message", "参数验证失败");
                // 这里可以进一步解析错误信息并构造errors数组
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }


    public Map<String, Object> applyForSingleLoan(SingleLoanApplicationRequestDTO request) {
        try {
            SingleLoanApplicationResponseDTO response = financingService.applyForSingleLoan(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "贷款申请提交成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("待审批")) {
                result.put("code", 409);
                result.put("message", "存在待审批的贷款申请");
            } else if (e.getMessage().contains("额度不足")) {
                result.put("code", 400);
                result.put("message", "可用额度不足");
            } else if (e.getMessage().contains("产品不存在")) {
                result.put("code", 404);
                result.put("message", "贷款产品不存在");
            } else {
                result.put("code", 400);
                result.put("message", "参数验证失败");
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> applyForJointLoan(JointLoanApplicationRequestDTO request) {
        try {
            JointLoanApplicationResponseDTO response = financingService.applyForJointLoan(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "联合贷款申请提交成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("待审批")) {
                result.put("code", 409);
                result.put("message", "存在待审批的贷款申请");
            } else if (e.getMessage().contains("额度不足")) {
                result.put("code", 400);
                result.put("message", "发起者额度不足");
            } else if (e.getMessage().contains("伙伴不符合条件")) {
                result.put("code", 400);
                result.put("message", "伙伴不符合条件");
            } else if (e.getMessage().contains("产品不存在")) {
                result.put("code", 404);
                result.put("message", "贷款产品不存在");
            } else if (e.getMessage().contains("必须同意")) {
                result.put("code", 400);
                result.put("message", "参数验证失败");
            } else {
                result.put("code", 400);
                result.put("message", "参数验证失败");
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getJointPartners(PartnersRequestDTO request) {
        try {
            PartnersResponseDTO response = financingService.getJointPartners(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("不符合联合贷款条件")) {
                result.put("code", 400);
                result.put("message", "自身不符合联合贷款条件");
            } else {
                result.put("code", 400);
                result.put("message", "参数验证失败");
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

}
