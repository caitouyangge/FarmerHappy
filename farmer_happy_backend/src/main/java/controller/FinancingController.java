// src/controller/FinancingController.java
package controller;

import dto.bank.LoanApprovalRequestDTO;
import dto.bank.LoanApprovalResponseDTO;
import dto.bank.LoanDisbursementRequestDTO;
import dto.bank.LoanDisbursementResponseDTO;
import dto.bank.CreditApprovalRequestDTO;
import dto.bank.CreditApprovalResponseDTO;
import dto.bank.PendingCreditApplicationsResponseDTO;
import dto.bank.PendingLoanApplicationsResponseDTO;
import service.financing.FinancingService;
import dto.financing.*;

import java.math.BigDecimal;
import java.sql.SQLException;
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

    public Map<String, Object> getRepaymentSchedule(Map<String, Object> requestBody, Map<String, String> headers) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取请求参数
            String phone = (String) requestBody.get("phone");
            String loanId = (String) requestBody.get("loan_id");

            // 调用服务方法
            RepaymentScheduleResponseDTO result = financingService.getRepaymentSchedule(phone, loanId);

            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", result);

        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "parameter");
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (SQLException e) {
            response.put("code", 500);
            response.put("message", "数据库查询失败: " + e.getMessage());
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "database");
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "server");
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        }

        return response;
    }

    public Map<String, Object> makeRepayment(Map<String, Object> requestBody, Map<String, String> headers) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 构造请求DTO
            RepaymentRequestDTO request = new RepaymentRequestDTO();
            request.setPhone((String) requestBody.get("phone"));
            request.setLoan_id((String) requestBody.get("loan_id"));

            // 处理还款金额
            Object repaymentAmountObj = requestBody.get("repayment_amount");
            if (repaymentAmountObj != null) {
                if (repaymentAmountObj instanceof Number) {
                    request.setRepayment_amount(new BigDecimal(repaymentAmountObj.toString()));
                } else if (repaymentAmountObj instanceof String) {
                    request.setRepayment_amount(new BigDecimal((String) repaymentAmountObj));
                }
            }

            request.setRepayment_method((String) requestBody.get("repayment_method"));
            request.setPayment_account((String) requestBody.get("payment_account"));
            request.setRemarks((String) requestBody.get("remarks"));

            // 调用服务方法
            RepaymentResponseDTO result = financingService.makeRepayment(request);

            response.put("code", 200);
            if ("closed".equals(result.getLoan_status())) {
                response.put("message", "提前还款成功，贷款已结清");
            } else {
                response.put("message", "还款成功");
            }
            response.put("data", result);

        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "parameter");
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (SQLException e) {
            response.put("code", 500);
            response.put("message", "数据库操作失败: " + e.getMessage());
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "database");
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("field", "server");
            error.put("message", e.getMessage());
            errors.add(error);
            response.put("errors", errors);
        }

        return response;
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

    public Map<String, Object> approveCreditApplication(CreditApprovalRequestDTO request) {
        try {
            CreditApprovalResponseDTO response = financingService.approveCreditApplication(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "信贷额度申请已审批");
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
                result.put("message", "信贷额度申请不存在");
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
                error.put("message", "该申请已审批，不能重复审批");
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

    public Map<String, Object> getPendingCreditApplications(String phone) {
        try {
            // 添加调试信息
            System.out.println("=== DEBUG: 获取待审批信贷额度申请列表 ===");
            System.out.println("银行操作员手机号: " + phone);
            
            PendingCreditApplicationsResponseDTO response = financingService.getPendingCreditApplications(phone);
            
            System.out.println("查询到的申请数量: " + (response != null ? response.getTotal() : 0));
            if (response != null && response.getApplications() != null) {
                for (Map<String, Object> app : response.getApplications()) {
                    System.out.println("申请ID: " + app.get("application_id") + ", 申请人: " + app.get("farmer_name"));
                }
            }
            
            // 添加详细的DTO内容调试
            System.out.println("=== DTO详细内容 ===");
            System.out.println("response对象: " + response);
            System.out.println("response.toString(): " + (response != null ? response.toString() : "null"));
            System.out.println("response.getApplications(): " + (response != null ? response.getApplications() : "null"));
            
            // 直接构建Map结构避免DTO序列化问题
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("total", response != null ? response.getTotal() : 0);
            dataMap.put("applications", response != null ? response.getApplications() : new ArrayList<>());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", dataMap);
            
            System.out.println("=== 返回结果调试 ===");
            System.out.println("result map: " + result);
            System.out.println("data内容: " + dataMap);
            System.out.println("applications数量: " + (response != null ? response.getApplications().size() : 0));
            System.out.println("=== DEBUG END ===");
            
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("无银行审批权限")) {
                result.put("code", 403);
                result.put("message", "无权限访问");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "phone");
                error.put("message", "该用户无银行审批权限");
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

    public Map<String, Object> getPendingLoanApplications(String phone) {
        try {
            // 添加调试信息
            System.out.println("=== DEBUG: 获取待审批贷款申请列表 ===");
            System.out.println("银行操作员手机号: " + phone);
            
            PendingLoanApplicationsResponseDTO response = financingService.getPendingLoanApplications(phone);
            
            System.out.println("查询到的贷款申请数量: " + (response != null ? response.getTotal() : 0));
            
            // 直接构建Map结构避免DTO序列化问题
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("total", response != null ? response.getTotal() : 0);
            dataMap.put("applications", response != null ? response.getApplications() : new ArrayList<>());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", dataMap);
            
            System.out.println("=== DEBUG END ===");
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("无银行审批权限")) {
                result.put("code", 403);
                result.put("message", "无权限访问");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "phone");
                error.put("message", "该用户无银行审批权限");
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

    public Map<String, Object> getApprovedLoanApplications(String phone) {
        try {
            // 添加调试信息
            System.out.println("=== DEBUG: 获取已审批待放款贷款申请列表 ===");
            System.out.println("银行操作员手机号: " + phone);
            
            PendingLoanApplicationsResponseDTO response = financingService.getApprovedLoanApplications(phone);
            
            System.out.println("查询到的已审批贷款申请数量: " + (response != null ? response.getTotal() : 0));
            
            // 直接构建Map结构避免DTO序列化问题
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("total", response != null ? response.getTotal() : 0);
            dataMap.put("applications", response != null ? response.getApplications() : new ArrayList<>());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", dataMap);
            
            System.out.println("=== DEBUG END ===");
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("无银行放款权限")) {
                result.put("code", 403);
                result.put("message", "无权限访问");
                List<Map<String, String>> errors = new ArrayList<>();
                Map<String, String> error = new HashMap<>();
                error.put("field", "phone");
                error.put("message", "该用户无银行放款权限");
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
            // 添加调试信息
            System.out.println("=== DEBUG: 农户申请额度 ===");
            System.out.println("申请人手机号: " + request.getPhone());
            System.out.println("申请金额: " + request.getApply_amount());
            System.out.println("证明类型: " + request.getProof_type());
            
            CreditApplicationDTO response = financingService.applyForCreditLimit(request);
            
            System.out.println("申请ID: " + (response != null ? response.getApplication_id() : "null"));
            System.out.println("申请状态: " + (response != null ? response.getStatus() : "null"));
            System.out.println("=== DEBUG END ===");
            
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

    public Map<String, Object> getFarmerCreditApplications(String phone) {
        try {
            // 添加调试信息
            System.out.println("=== DEBUG: 查询农户申请记录 ===");
            System.out.println("农户手机号: " + phone);
            
            Map<String, Object> response = financingService.getFarmerCreditApplications(phone);
            
            System.out.println("查询到的申请数量: " + response.get("total"));
            System.out.println("=== DEBUG END ===");
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取申请记录成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getFarmerLoans(String phone) {
        try {
            // 添加调试信息
            System.out.println("=== DEBUG: 查询农户贷款记录 ===");
            System.out.println("农户手机号: " + phone);
            
            Map<String, Object> response = financingService.getFarmerLoans(phone);
            
            System.out.println("查询到的贷款数量: " + response.get("total"));
            System.out.println("=== DEBUG END ===");
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取贷款记录成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getFarmerLoanApplications(String phone) {
        try {
            // 添加调试信息
            System.out.println("=== DEBUG: 查询农户贷款申请记录 ===");
            System.out.println("农户手机号: " + phone);
            
            Map<String, Object> response = financingService.getFarmerLoanApplications(phone);
            
            System.out.println("查询到的贷款申请数量: " + response.get("total"));
            System.out.println("=== DEBUG END ===");
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取贷款申请记录成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
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

    /**
     * 智能贷款推荐API
     */
    public Map<String, Object> getSmartLoanRecommendation(SmartLoanRecommendationRequestDTO request) {
        try {
            SmartLoanRecommendationResponseDTO response = financingService.getSmartLoanRecommendation(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取推荐成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("无可用贷款额度")) {
                result.put("code", 400);
                result.put("message", "您当前无可用贷款额度");
            } else if (e.getMessage().contains("不存在或已下架")) {
                result.put("code", 400);
                result.put("message", "所选产品不存在或已下架");
            } else if (e.getMessage().contains("超过产品最高额度")) {
                result.put("code", 400);
                result.put("message", "申请金额超过产品最高额度");
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

    /**
     * 联合贷款伙伴确认API
     */
    public Map<String, Object> confirmJointLoanApplication(JointLoanPartnerConfirmationRequestDTO request) {
        try {
            JointLoanPartnerConfirmationResponseDTO response = financingService.confirmJointLoanApplication(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", response.getMessage());
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            if (e.getMessage().contains("用户认证失败")) {
                result.put("code", 401);
                result.put("message", "用户认证失败，请检查手机号或重新登录");
            } else if (e.getMessage().contains("不是该联合贷款申请的合作伙伴")) {
                result.put("code", 403);
                result.put("message", "您不是该联合贷款申请的合作伙伴");
            } else if (e.getMessage().contains("不需要伙伴确认")) {
                result.put("code", 400);
                result.put("message", "该申请状态不需要伙伴确认");
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

    /**
     * 获取用户待确认的联合贷款申请API
     */
    public Map<String, Object> getPendingJointLoanApplications(PendingJointLoanApplicationsRequestDTO request) {
        try {
            PendingJointLoanApplicationsResponseDTO response = financingService.getPendingJointLoanApplications(request);
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

    /**
     * 发送联合贷款消息API
     */
    public Map<String, Object> sendJointLoanMessage(JointLoanMessageRequestDTO request) {
        try {
            JointLoanMessageResponseDTO response = financingService.sendJointLoanMessage(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "消息发送成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 获取联合贷款消息列表API
     */
    public Map<String, Object> getJointLoanMessages(GetJointLoanMessagesRequestDTO request) {
        try {
            JointLoanMessageResponseDTO response = financingService.getJointLoanMessages(request);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", response);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            return result;
        }
    }

}
