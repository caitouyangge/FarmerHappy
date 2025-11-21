// src/controller/FinancingController.java
package controller;

import service.financing.FinancingService;
import dto.financing.*;

import java.util.HashMap;
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
}
