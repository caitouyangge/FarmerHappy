// src/main/java/service/financing/FinancingService.java
package service.financing;

import entity.financing.LoanProduct;
import entity.financing.CreditLimit;
import entity.financing.CreditApplication;
import entity.User;
import repository.DatabaseManager;
import dto.financing.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class FinancingService {
    private DatabaseManager dbManager;
    private static final AtomicLong productIdCounter = new AtomicLong(1);
    private static final AtomicLong applicationIdCounter = new AtomicLong(1);

    public FinancingService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // 银行发布贷款产品
    public BankLoanProductResponseDTO publishLoanProduct(BankLoanProductRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateBankLoanProductRequest(request, errors);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("参数验证失败: " + errors.toString());
            }

            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行操作员权限");
            }

            // 检查产品名称是否重复
            if (isProductNameExists(request.getProduct_name())) {
                throw new IllegalArgumentException("产品名称已存在");
            }

            // 创建贷款产品
            LoanProduct loanProduct = new LoanProduct();
            String productId = "PROD" + new Timestamp(System.currentTimeMillis()).toString().substring(0, 10).replace("-", "") +
                    String.format("%04d", productIdCounter.getAndIncrement());
            loanProduct.setProductId(productId);

            String productCode = request.getProduct_code();
            if (productCode == null || productCode.trim().isEmpty()) {
                productCode = "PRD-" + new Timestamp(System.currentTimeMillis()).toString().substring(0, 10).replace("-", "") +
                        "-" + String.format("%03d", productIdCounter.get());
            }
            loanProduct.setProductCode(productCode);

            loanProduct.setProductName(request.getProduct_name());
            loanProduct.setMinCreditLimit(request.getMin_credit_limit());
            loanProduct.setMaxAmount(request.getMax_amount());
            loanProduct.setInterestRate(request.getInterest_rate());
            loanProduct.setTermMonths(request.getTerm_months());
            loanProduct.setRepaymentMethod(request.getRepayment_method());
            loanProduct.setDescription(request.getDescription());
            loanProduct.setStatus("active");
            loanProduct.setBankId(((Long) bankInfo.get("bank_id")).longValue());
            loanProduct.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            loanProduct.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 保存到数据库
            saveLoanProduct(loanProduct);

            // 构造成功响应
            BankLoanProductResponseDTO response = new BankLoanProductResponseDTO();
            response.setProduct_id(loanProduct.getProductId());
            response.setProduct_code(loanProduct.getProductCode());
            response.setStatus(loanProduct.getStatus());
            response.setCreated_at(loanProduct.getCreatedAt());
            response.setCreated_by((String) bankInfo.get("bank_name"));

            return response;

        } catch (Exception e) {
            throw new RuntimeException("发布贷款产品失败: " + e.getMessage(), e);
        }
    }

    // 查询可申请的贷款产品
    public LoanProductsResponseDTO getAvailableLoanProducts(LoanProductsRequestDTO request) {
        try {
            // 参数验证
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空");
            }

            // 检查用户是否存在
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("该手机号未注册");
            }

            // 检查用户是否具有农户身份
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("该用户不是农户");
            }

            // 获取农户信用额度
            BigDecimal creditLimit;
            if (request.getCredit_limit() != null) {
                creditLimit = request.getCredit_limit();
            } else {
                CreditLimit farmerCreditLimit = getCreditLimitByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
                if (farmerCreditLimit != null && "active".equals(farmerCreditLimit.getStatus())) {
                    creditLimit = farmerCreditLimit.getAvailableLimit();
                } else {
                    creditLimit = BigDecimal.ZERO;
                }
            }

            // 获取所有激活的贷款产品
            List<LoanProduct> allProducts = getAllActiveLoanProducts();
            List<LoanProductDTO> availableProducts = new ArrayList<>();

            for (LoanProduct product : allProducts) {
                LoanProductDTO dto = new LoanProductDTO();
                dto.setProduct_id(product.getProductId());
                dto.setProduct_name(product.getProductName());
                dto.setProduct_code(product.getProductCode());
                dto.setMin_credit_limit(product.getMinCreditLimit());
                dto.setMax_amount(product.getMaxAmount());
                dto.setInterest_rate(product.getInterestRate());
                dto.setTerm_months(product.getTermMonths());
                dto.setRepayment_method(product.getRepaymentMethod());
                dto.setRepayment_method_name(getRepaymentMethodName(product.getRepaymentMethod()));
                dto.setDescription(product.getDescription());
                dto.setStatus(product.getStatus());

                // 判断是否可以申请
                boolean canApply = creditLimit.compareTo(product.getMinCreditLimit()) >= 0;
                dto.setCan_apply(canApply);

                if (canApply) {
                    // 计算最大可申请金额
                    BigDecimal maxApplyAmount = creditLimit.min(product.getMaxAmount());
                    dto.setMax_apply_amount(maxApplyAmount);
                } else {
                    dto.setReason("可用额度不足，需要" + product.getMinCreditLimit() + "元额度");
                }

                availableProducts.add(dto);
            }

            // 构造响应
            LoanProductsResponseDTO response = new LoanProductsResponseDTO();
            response.setTotal(availableProducts.size());
            response.setAvailable_products(availableProducts);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("查询可申请贷款产品失败: " + e.getMessage(), e);
        }
    }

    // 查询可用贷款额度
    public CreditLimitDTO getCreditLimit(CreditLimitRequestDTO request) {
        try {
            // 参数验证
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空");
            }

            // 检查用户是否存在
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("该手机号未注册");
            }

            // 检查用户是否具有农户身份
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("该用户不是农户");
            }

            // 获取信用额度
            CreditLimit creditLimit = getCreditLimitByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());

            // 构造响应
            CreditLimitDTO response = new CreditLimitDTO();

            if (creditLimit != null && "active".equals(creditLimit.getStatus())) {
                response.setTotal_limit(creditLimit.getTotalLimit());
                response.setUsed_limit(creditLimit.getUsedLimit());
                response.setAvailable_limit(creditLimit.getAvailableLimit());
                response.setCurrency(creditLimit.getCurrency());
                response.setStatus(creditLimit.getStatus());
                response.setLast_updated(creditLimit.getLastUpdated());
            } else {
                response.setTotal_limit(BigDecimal.ZERO);
                response.setUsed_limit(BigDecimal.ZERO);
                response.setAvailable_limit(BigDecimal.ZERO);
                response.setCurrency("CNY");
                response.setStatus("no_limit");
                response.setLast_updated(null);
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException("查询可用贷款额度失败: " + e.getMessage(), e);
        }
    }

    // 申请贷款额度
    public CreditApplicationDTO applyForCreditLimit(CreditApplicationRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateCreditApplicationRequest(request, errors);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("参数验证失败: " + errors.toString());
            }

            // 检查用户是否存在
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有农户身份
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查是否存在待审批的申请
            CreditApplication existingApplication = getPendingApplicationByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            if (existingApplication != null) {
                throw new IllegalArgumentException("存在待审批的额度申请");
            }

            // 创建额度申请
            CreditApplication application = new CreditApplication();
            String applicationId = "APP" + new Timestamp(System.currentTimeMillis()).toString().substring(0, 10).replace("-", "") +
                    String.format("%04d", applicationIdCounter.getAndIncrement());
            application.setApplicationId(applicationId);
            application.setFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            application.setProofType(request.getProof_type());
            application.setProofImages(convertListToJson(request.getProof_images()));
            application.setApplyAmount(request.getApply_amount());
            application.setDescription(request.getDescription());
            application.setStatus("approved");
            application.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            application.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 保存到数据库
            saveCreditApplication(application);

            // 处理信用额度更新逻辑
            Long farmerId = (Long) farmerInfo.get("farmer_id");
            BigDecimal applyAmount = request.getApply_amount();

            // 获取现有的信用额度记录
            CreditLimit existingCreditLimit = getCreditLimitByFarmerId(farmerId);

            if (existingCreditLimit != null) {
                // 如果存在现有记录，更新总额度
                BigDecimal newTotalLimit = existingCreditLimit.getTotalLimit().add(applyAmount);
                BigDecimal newAvailableLimit = newTotalLimit.subtract(existingCreditLimit.getUsedLimit());

                // 更新现有记录
                existingCreditLimit.setTotalLimit(newTotalLimit);
                existingCreditLimit.setAvailableLimit(newAvailableLimit);
                existingCreditLimit.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                existingCreditLimit.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

                // 保存更新后的信用额度记录
                updateCreditLimit(existingCreditLimit);
            } else {
                // 如果不存在现有记录，创建新记录
                CreditLimit newCreditLimit = new CreditLimit();
                newCreditLimit.setFarmerId(farmerId);
                newCreditLimit.setTotalLimit(applyAmount);
                newCreditLimit.setUsedLimit(BigDecimal.ZERO);
                newCreditLimit.setAvailableLimit(applyAmount);
                newCreditLimit.setCurrency("CNY");
                newCreditLimit.setStatus("active");
                newCreditLimit.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                newCreditLimit.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                newCreditLimit.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

                // 保存新的信用额度记录
                saveCreditLimit(newCreditLimit);
            }

            // 构造成功响应
            CreditApplicationDTO response = new CreditApplicationDTO();
            response.setApplication_id(application.getApplicationId());
            response.setStatus(application.getStatus());
            response.setCreated_at(application.getCreatedAt());
            response.setApply_amount(application.getApplyAmount());
            response.setProof_type(application.getProofType());
            response.setProof_images(request.getProof_images());
            response.setDescription(application.getDescription());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("申请贷款额度失败: " + e.getMessage(), e);
        }
    }


    // 私有辅助方法：根据手机号查找用户
    private User findUserByPhone(String phone) throws SQLException {
        return dbManager.findUserByPhone(phone);
    }

    private void updateCreditLimit(entity.financing.CreditLimit creditLimit) throws SQLException {
        dbManager.updateCreditLimit(creditLimit);
    }

    private void saveCreditLimit(entity.financing.CreditLimit creditLimit) throws SQLException {
        dbManager.saveCreditLimit(creditLimit);
    }


    // 私有辅助方法：检查用户是否具有银行身份
    private Map<String, Object> checkUserBankRole(String uid) throws SQLException {
        // 检查用户是否具有银行身份
        String userRole = dbManager.getUserRole(uid);
        if (!"bank".equals(userRole)) {
            return null;
        }

        // 获取银行信息
        return dbManager.getBankInfoByUid(uid);
    }

    // 私有辅助方法：检查用户是否具有农户身份
    private Map<String, Object> checkUserFarmerRole(String uid) throws SQLException {
        // 检查用户是否具有农户身份
        String userRole = dbManager.getUserRole(uid);
        if (!"farmer".equals(userRole)) {
            return null;
        }

        // 获取农户信息
        return dbManager.getFarmerInfoByUid(uid);
    }

    // 私有辅助方法
    private void validateBankLoanProductRequest(BankLoanProductRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "手机号不能为空"));
        }

        if (request.getProduct_name() == null || request.getProduct_name().trim().isEmpty()) {
            errors.add(createError("product_name", "产品名称不能为空"));
        } else if (request.getProduct_name().length() < 2 || request.getProduct_name().length() > 50) {
            errors.add(createError("product_name", "产品名称必须在2-50个字符之间"));
        }

        if (request.getMin_credit_limit() == null || request.getMin_credit_limit().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("min_credit_limit", "最低贷款额度要求必须大于0"));
        }

        if (request.getMax_amount() == null || request.getMax_amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("max_amount", "最高贷款额度必须大于0"));
        }

        if (request.getInterest_rate() == null) {
            errors.add(createError("interest_rate", "年利率不能为空"));
        } else if (request.getInterest_rate().compareTo(new BigDecimal("1.0")) < 0 ||
                request.getInterest_rate().compareTo(new BigDecimal("20.0")) > 0) {
            errors.add(createError("interest_rate", "年利率必须在1%-20%之间"));
        }

        if (request.getTerm_months() == null) {
            errors.add(createError("term_months", "贷款期限不能为空"));
        } else if (request.getTerm_months() < 1 || request.getTerm_months() > 60) {
            errors.add(createError("term_months", "贷款期限必须在1-60个月之间"));
        }

        if (request.getRepayment_method() == null || request.getRepayment_method().trim().isEmpty()) {
            errors.add(createError("repayment_method", "还款方式不能为空"));
        } else if (!Arrays.asList("equal_installment", "interest_first", "bullet_repayment")
                .contains(request.getRepayment_method())) {
            errors.add(createError("repayment_method", "还款方式必须是 equal_installment、interest_first 或 bullet_repayment"));
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            errors.add(createError("description", "产品描述不能为空"));
        } else if (request.getDescription().length() < 10 || request.getDescription().length() > 500) {
            errors.add(createError("description", "产品描述必须在10-500个字符之间"));
        }
    }

    private void validateCreditApplicationRequest(CreditApplicationRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "手机号不能为空"));
        }

        if (request.getProof_type() == null || request.getProof_type().trim().isEmpty()) {
            errors.add(createError("proof_type", "证明类型不能为空"));
        } else if (!Arrays.asList("land_certificate", "property_certificate", "income_proof",
                "business_license", "other").contains(request.getProof_type())) {
            errors.add(createError("proof_type", "证明类型必须是 land_certificate、property_certificate、income_proof、business_license 或 other"));
        }

        if (request.getProof_images() == null || request.getProof_images().isEmpty()) {
            errors.add(createError("proof_images", "证明材料图片不能为空"));
        } else if (request.getProof_images().size() > 5) {
            errors.add(createError("proof_images", "证明材料图片不能超过5张"));
        }

        if (request.getApply_amount() == null || request.getApply_amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("apply_amount", "申请额度必须大于0"));
        } else if (request.getApply_amount().compareTo(new BigDecimal("1000000")) > 0) {
            errors.add(createError("apply_amount", "申请额度不能超过100万元"));
        }
    }

    private Map<String, String> createError(String field, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("field", field);
        error.put("message", message);
        return error;
    }

    private String getRepaymentMethodName(String method) {
        switch (method) {
            case "equal_installment": return "等额本息";
            case "interest_first": return "先息后本";
            case "bullet_repayment": return "一次性还本付息";
            default: return method;
        }
    }

    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            json.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    private boolean isProductNameExists(String productName) throws SQLException {
        return dbManager.isProductNameExists(productName);
    }

    private entity.financing.LoanProduct getLoanProductByName(String productName) throws SQLException {
        return dbManager.getLoanProductByName(productName);
    }

    private void saveLoanProduct(entity.financing.LoanProduct loanProduct) throws SQLException {
        dbManager.saveLoanProduct(loanProduct);
    }

    private List<entity.financing.LoanProduct> getAllActiveLoanProducts() throws SQLException {
        return dbManager.getAllActiveLoanProducts();
    }

    private entity.financing.CreditLimit getCreditLimitByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getCreditLimitByFarmerId(farmerId);
    }

    private entity.financing.CreditApplication getPendingApplicationByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getPendingApplicationByFarmerId(farmerId);
    }

    private void saveCreditApplication(entity.financing.CreditApplication application) throws SQLException {
        dbManager.saveCreditApplication(application);
    }

}
