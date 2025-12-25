// src/main/java/service/financing/FinancingService.java
package service.financing;

import dto.bank.*;
import dto.bank.CreditApprovalRequestDTO;
import dto.bank.CreditApprovalResponseDTO;
import dto.bank.PendingCreditApplicationsResponseDTO;
import dto.bank.PendingLoanApplicationsResponseDTO;
import entity.financing.LoanProduct;
import entity.financing.CreditLimit;
import entity.financing.CreditApplication;
import entity.User;
import repository.DatabaseManager;
import dto.financing.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
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
            // 修复产品ID生成逻辑，使用更可靠的格式
            String timestamp = String.valueOf(System.currentTimeMillis());
            String productId = "PROD" + timestamp.substring(timestamp.length() - 10) + 
                    String.format("%04d", productIdCounter.getAndIncrement());
            loanProduct.setProductId(productId);
            
            // 调试输出
            System.out.println("DEBUG: 生成的产品ID = " + productId);

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
                // 返回格式化的错误信息
                throw new IllegalArgumentException("参数验证失败: " + formatErrors(errors));
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
                throw new IllegalArgumentException("存在待审批的额度申请，请勿重复提交");
            }

            // 创建额度申请
            CreditApplication application = new CreditApplication();
            // 修改申请ID生成逻辑，确保唯一性
            String applicationId = generateUniqueApplicationId();
            application.setApplicationId(applicationId);
            application.setFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            application.setProofType(request.getProof_type());
            application.setProofImages(convertListToJson(request.getProof_images()));
            application.setApplyAmount(request.getApply_amount());
            application.setDescription(request.getDescription());
            application.setStatus("pending"); // 修改为pending状态，等待银行审批
            application.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            application.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 保存到数据库
            saveCreditApplication(application);

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

    // 添加生成唯一申请ID的方法
    private String generateUniqueApplicationId() {
        // 使用时间戳+随机数确保唯一性
        return "APP" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
    }

    /**
     * 银行审批信贷额度申请
     */
    public CreditApprovalResponseDTO approveCreditApplication(CreditApprovalRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateCreditApprovalRequest(request, errors);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("参数验证失败: " + formatErrors(errors));
            }

            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行审批权限");
            }

            // 获取信贷额度申请信息
            CreditApplication creditApplication = getCreditApplicationById(request.getApplication_id());
            if (creditApplication == null) {
                throw new IllegalArgumentException("指定的申请ID不存在");
            }

            // 检查申请状态
            if (!"pending".equals(creditApplication.getStatus())) {
                throw new IllegalArgumentException("该申请状态为" + creditApplication.getStatus() + "，不能重复审批");
            }

            // 构造响应
            CreditApprovalResponseDTO response = new CreditApprovalResponseDTO();
            response.setApplication_id(creditApplication.getApplicationId());

            // 根据审批动作处理
            if ("approve".equals(request.getAction())) {
                // 批准申请
                if (request.getApproved_amount() == null) {
                    throw new IllegalArgumentException("批准金额不能为空");
                }

                // 更新申请状态为已批准
                updateCreditApplicationStatus(
                        request.getApplication_id(),
                        "approved",
                        ((Long) bankInfo.get("bank_id")).longValue(),
                        new Timestamp(System.currentTimeMillis()),
                        request.getApproved_amount()
                );

                // 处理信用额度更新逻辑
                Long farmerId = creditApplication.getFarmerId();
                BigDecimal approvedAmount = request.getApproved_amount();

                // 获取现有的信用额度记录
                CreditLimit existingCreditLimit = getCreditLimitByFarmerId(farmerId);

                if (existingCreditLimit != null) {
                    // 如果存在现有记录，更新总额度
                    BigDecimal newTotalLimit = existingCreditLimit.getTotalLimit().add(approvedAmount);
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
                    newCreditLimit.setTotalLimit(approvedAmount);
                    newCreditLimit.setUsedLimit(BigDecimal.ZERO);
                    newCreditLimit.setAvailableLimit(approvedAmount);
                    newCreditLimit.setCurrency("CNY");
                    newCreditLimit.setStatus("active");
                    newCreditLimit.setLastUpdated(new Timestamp(System.currentTimeMillis()));
                    newCreditLimit.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                    newCreditLimit.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

                    // 保存新的信用额度记录
                    saveCreditLimit(newCreditLimit);
                }

                response.setStatus("approved");
                response.setApproved_amount(request.getApproved_amount());
                response.setApproved_by((String) bankInfo.get("bank_name"));
                response.setApproved_at(new Timestamp(System.currentTimeMillis()));
            } else if ("reject".equals(request.getAction())) {
                // 拒绝申请
                if (request.getReject_reason() == null || request.getReject_reason().trim().isEmpty()) {
                    throw new IllegalArgumentException("拒绝原因不能为空");
                }

                // 更新申请状态为已拒绝
                updateCreditApplicationRejection(
                        request.getApplication_id(),
                        "rejected",
                        ((Long) bankInfo.get("bank_id")).longValue(),
                        new Timestamp(System.currentTimeMillis()),
                        request.getReject_reason()
                );

                response.setStatus("rejected");
                response.setReject_reason(request.getReject_reason());
                response.setRejected_by((String) bankInfo.get("bank_name"));
                response.setRejected_at(new Timestamp(System.currentTimeMillis()));
            } else {
                throw new IllegalArgumentException("审批动作必须是approve或reject");
            }

            return response;
        } catch (Exception e) {
            throw new RuntimeException("审批信贷额度申请失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取待审批的信贷额度申请列表
     */
    public PendingCreditApplicationsResponseDTO getPendingCreditApplications(String phone) {
        try {
            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行审批权限");
            }

            // 获取待审批的信贷额度申请列表
            List<Map<String, Object>> pendingApplications = getPendingCreditApplicationsList();

            // 构造响应
            PendingCreditApplicationsResponseDTO response = new PendingCreditApplicationsResponseDTO();
            response.setTotal(pendingApplications.size());
            response.setApplications(pendingApplications);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取待审批信贷额度申请列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取农户的申请记录列表
     */
    public Map<String, Object> getFarmerCreditApplications(String phone) {
        try {
            // 检查用户是否存在
            User user = findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有农户身份
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 获取农户的申请记录列表
            Long farmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
            List<Map<String, Object>> applications = getCreditApplicationsByFarmerId(farmerId);

            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("total", applications.size());
            response.put("applications", applications);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取申请记录列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取农户的已放款贷款列表
     */
    public Map<String, Object> getFarmerLoans(String phone) {
        try {
            // 检查用户是否存在
            User user = findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有农户身份
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 获取农户的已放款贷款列表
            Long farmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
            List<Map<String, Object>> loans = dbManager.getLoansByFarmerId(farmerId);

            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("total", loans.size());
            response.put("loans", loans);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取贷款列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取农户的贷款申请记录列表
     */
    public Map<String, Object> getFarmerLoanApplications(String phone) {
        try {
            // 检查用户是否存在
            User user = findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有农户身份
            Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
            if (farmerInfo == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 获取农户的贷款申请记录列表
            Long farmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
            List<Map<String, Object>> applications = getLoanApplicationsByFarmerId(farmerId);

            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("total", applications.size());
            response.put("applications", applications);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取贷款申请记录列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取待审批的贷款申请列表
     */
    public PendingLoanApplicationsResponseDTO getPendingLoanApplications(String phone) {
        try {
            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行审批权限");
            }

            // 获取待审批的贷款申请列表
            List<Map<String, Object>> pendingApplications = getPendingLoanApplicationsList();

            // 构造响应
            PendingLoanApplicationsResponseDTO response = new PendingLoanApplicationsResponseDTO();
            response.setTotal(pendingApplications.size());
            response.setApplications(pendingApplications);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取待审批贷款申请列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取已审批待放款的贷款申请列表
     */
    public PendingLoanApplicationsResponseDTO getApprovedLoanApplications(String phone) {
        try {
            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(phone);
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行放款权限");
            }

            // 获取已审批的贷款申请列表
            List<Map<String, Object>> approvedApplications = getApprovedLoanApplicationsList();

            // 构造响应
            PendingLoanApplicationsResponseDTO response = new PendingLoanApplicationsResponseDTO();
            response.setTotal(approvedApplications.size());
            response.setApplications(approvedApplications);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取已审批贷款申请列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证信贷额度审批请求
     */
    private void validateCreditApprovalRequest(CreditApprovalRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "银行操作员手机号不能为空"));
        }

        if (request.getApplication_id() == null || request.getApplication_id().trim().isEmpty()) {
            errors.add(createError("application_id", "信贷额度申请ID不能为空"));
        }

        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            errors.add(createError("action", "审批动作不能为空"));
        } else if (!Arrays.asList("approve", "reject").contains(request.getAction())) {
            errors.add(createError("action", "审批动作必须是approve或reject"));
        }

        if ("approve".equals(request.getAction())) {
            if (request.getApproved_amount() == null || request.getApproved_amount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(createError("approved_amount", "批准金额必须大于0"));
            }
        }

        if ("reject".equals(request.getAction())) {
            if (request.getReject_reason() == null || request.getReject_reason().trim().isEmpty()) {
                errors.add(createError("reject_reason", "拒绝原因不能为空"));
            } else if (request.getReject_reason().length() < 2 || request.getReject_reason().length() > 200) {
                errors.add(createError("reject_reason", "拒绝原因必须在2-200个字符之间"));
            }
        }
    }

    private List<Map<String, Object>> getQualifiedPartners(BigDecimal minCreditLimit, List<String> excludePhones, int maxPartners) throws SQLException {
        return dbManager.getQualifiedPartners(minCreditLimit, excludePhones, maxPartners);
    }

    // 添加格式化错误信息的方法
    private String formatErrors(List<Map<String, String>> errors) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> error : errors) {
            sb.append("[").append(error.get("field")).append(": ").append(error.get("message")).append("] ");
        }
        return sb.toString().trim();
    }

    // 申请单人贷款
    public SingleLoanApplicationResponseDTO applyForSingleLoan(SingleLoanApplicationRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateSingleLoanApplicationRequest(request, errors);
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

            // 检查是否存在待审批的贷款申请
            // 这里需要实现检查逻辑

            // 获取贷款产品
            LoanProduct loanProduct = getLoanProductById(request.getProduct_id());
            if (loanProduct == null || !"active".equals(loanProduct.getStatus())) {
                throw new IllegalArgumentException("指定的产品" + request.getProduct_id() + "不存在或已下架，请选择其他产品");
            }

            // 检查申请金额是否超过产品最高额度
            if (request.getApply_amount().compareTo(loanProduct.getMaxAmount()) > 0) {
                throw new IllegalArgumentException("申请金额超过产品最高额度");
            }

            // 获取农户信用额度
            CreditLimit farmerCreditLimit = getCreditLimitByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            if (farmerCreditLimit == null || !"active".equals(farmerCreditLimit.getStatus())) {
                throw new IllegalArgumentException("您当前无可用贷款额度");
            }

            // 检查用户的信用额度是否满足产品的最低信用额度要求
            if (farmerCreditLimit.getAvailableLimit().compareTo(loanProduct.getMinCreditLimit()) < 0) {
                throw new IllegalArgumentException("您的可用额度" + farmerCreditLimit.getAvailableLimit() + 
                        "元低于产品最低额度要求" + loanProduct.getMinCreditLimit() + "元，无法申请该产品");
            }

            // 检查申请金额是否超过用户可用额度
            if (request.getApply_amount().compareTo(farmerCreditLimit.getAvailableLimit()) > 0) {
                throw new IllegalArgumentException("申请金额" + request.getApply_amount() + "元超过可用额度" +
                        farmerCreditLimit.getAvailableLimit() + "元，请先申请提高额度或减少申请金额");
            }

            // 预扣额度 - 在创建贷款申请记录前扣除
            preDeductCreditLimit(((Long) farmerInfo.get("farmer_id")).longValue(), request.getApply_amount());

            // 生成贷款申请ID (修改为符合数据库字段长度限制的格式)
            String loanApplicationId = "LOAN" + System.currentTimeMillis() +
                    String.format("%03d", new Random().nextInt(1000));
            // 确保ID长度不超过20个字符（数据库字段限制）
            if (loanApplicationId.length() > 20) {
                loanApplicationId = loanApplicationId.substring(0, 20);
            }

            // 创建贷款申请记录
            long applicationRecordId = createLoanApplication(
                    loanApplicationId,
                    ((Long) farmerInfo.get("farmer_id")).longValue(),
                    loanProduct.getId(),
                    "single",
                    request.getApply_amount(),
                    request.getPurpose(),
                    request.getRepayment_source()
            );

            // 构造成功响应
            SingleLoanApplicationResponseDTO response = new SingleLoanApplicationResponseDTO();
            // 设置响应数据
            response.setLoan_application_id(loanApplicationId);
            response.setStatus("pending");
            response.setProduct_name(loanProduct.getProductName());
            response.setApply_amount(request.getApply_amount());
            // 计算月还款额（简化计算）
            response.setEstimated_monthly_payment(request.getApply_amount().divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP));
            response.setCreated_at(new Timestamp(System.currentTimeMillis()));

            return response;

        } catch (Exception e) {
            throw new RuntimeException("申请单人贷款失败: " + e.getMessage(), e);
        }
    }


    // 申请联合贷款
    public JointLoanApplicationResponseDTO applyForJointLoan(JointLoanApplicationRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateJointLoanApplicationRequest(request, errors);
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

            // 检查是否存在待审批的贷款申请
            // 这里需要实现检查逻辑

            // 获取贷款产品
            LoanProduct loanProduct = getLoanProductById(request.getProduct_id());
            if (loanProduct == null || !"active".equals(loanProduct.getStatus())) {
                throw new IllegalArgumentException("指定的产品" + request.getProduct_id() + "不存在或已下架，请选择其他产品");
            }

            // 检查申请金额是否超过产品最高额度
            if (request.getApply_amount().compareTo(loanProduct.getMaxAmount()) > 0) {
                throw new IllegalArgumentException("申请金额超过产品最高额度");
            }

            // 获取发起者信用额度
            CreditLimit initiatorCreditLimit = getCreditLimitByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            if (initiatorCreditLimit == null || !"active".equals(initiatorCreditLimit.getStatus())) {
                throw new IllegalArgumentException("您当前无可用贷款额度");
            }

            // 计算总额度（发起人 + 所有伙伴）
            BigDecimal totalAvailableLimit = initiatorCreditLimit.getAvailableLimit();

            // 检查伙伴是否符合条件
            for (String partnerPhone : request.getPartner_phones()) {
                // 检查伙伴是否存在
                User partnerUser = findUserByPhone(partnerPhone);
                if (partnerUser == null) {
                    throw new IllegalArgumentException("伙伴" + partnerPhone + "不存在");
                }

                // 检查伙伴是否为农户
                Map<String, Object> partnerFarmerInfo = checkUserFarmerRole(partnerUser.getUid());
                if (partnerFarmerInfo == null) {
                    throw new IllegalArgumentException("伙伴" + partnerPhone + "不是农户");
                }

                // 检查伙伴信用额度
                CreditLimit partnerCreditLimit = getCreditLimitByFarmerId(((Long) partnerFarmerInfo.get("farmer_id")).longValue());
                if (partnerCreditLimit == null || !"active".equals(partnerCreditLimit.getStatus())) {
                    throw new IllegalArgumentException("伙伴" + partnerPhone + "无可用贷款额度，无法参与联合贷款");
                }

                // 累计伙伴额度到总额度（联合贷款以总额度为准）
                totalAvailableLimit = totalAvailableLimit.add(partnerCreditLimit.getAvailableLimit());

                // 检查伙伴是否有待审批的联合贷款申请
                // 这里需要实现检查逻辑
            }

            // 检查总额度是否满足产品的最低信用额度要求
            if (totalAvailableLimit.compareTo(loanProduct.getMinCreditLimit()) < 0) {
                throw new IllegalArgumentException("您和联合伙伴的总可用额度" + totalAvailableLimit + 
                        "元低于产品最低额度要求" + loanProduct.getMinCreditLimit() + "元，无法申请该产品");
            }

            // 检查总额度是否足够覆盖申请金额
            if (totalAvailableLimit.compareTo(request.getApply_amount()) < 0) {
                throw new IllegalArgumentException("您和联合伙伴的总可用额度" + totalAvailableLimit + 
                        "元不足以覆盖申请金额" + request.getApply_amount() + "元，请选择其他产品或寻找更多伙伴");
            }

            // 联合贷款不在此处预扣发起人额度，等所有伙伴确认后再统一预扣

            // 生成贷款申请ID (修改为符合数据库字段长度限制的格式)
            String loanApplicationId = "LOAN" + System.currentTimeMillis() +
                    String.format("%03d", new Random().nextInt(1000));
            // 确保ID长度不超过20个字符（数据库字段限制）
            if (loanApplicationId.length() > 20) {
                loanApplicationId = loanApplicationId.substring(0, 20);
            }

            // 创建联合贷款申请记录
            long applicationRecordId = createLoanApplication(
                    loanApplicationId,
                    ((Long) farmerInfo.get("farmer_id")).longValue(),
                    loanProduct.getId(),
                    "joint",
                    request.getApply_amount(),
                    request.getPurpose(),
                    request.getRepayment_plan() // 联合贷款使用还款计划作为还款来源
            );

            // 计算额度分配：发起人额度全部用光，伙伴支付剩余额度
            BigDecimal initiatorAvailableLimit = initiatorCreditLimit.getAvailableLimit();
            BigDecimal initiatorShareAmount = initiatorAvailableLimit; // 发起人额度全部用光
            BigDecimal remainingAmount = request.getApply_amount().subtract(initiatorShareAmount);
            
            // 检查剩余金额是否大于0（如果发起人额度已足够，则不需要伙伴）
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("您的可用额度已足够覆盖申请金额，无需联合贷款");
            }
            
            // 对于双人联合贷款，剩余金额由伙伴承担
            // 注意：当前系统只支持双人联合贷款（1个发起人 + 1个伙伴）
            if (request.getPartner_phones().size() != 1) {
                throw new IllegalArgumentException("联合贷款目前仅支持双人联合（1个发起人 + 1个伙伴）");
            }
            
            BigDecimal partnerShareAmount = remainingAmount; // 伙伴支付剩余额度
            
            // 计算份额比例（用于显示和记录）
            BigDecimal initiatorShareRatio = initiatorShareAmount.divide(request.getApply_amount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal partnerShareRatio = partnerShareAmount.divide(request.getApply_amount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);

            // 保存联合贷款伙伴记录（记录伙伴实际支付的额度）
            List<Map<String, Object>> partnerRecords = new ArrayList<>();
            for (String partnerPhone : request.getPartner_phones()) {
                User partnerUser = findUserByPhone(partnerPhone);
                Map<String, Object> partnerFarmerInfo = checkUserFarmerRole(partnerUser.getUid());
                
                // 检查伙伴额度是否足够支付剩余金额
                CreditLimit partnerCreditLimit = getCreditLimitByFarmerId(((Long) partnerFarmerInfo.get("farmer_id")).longValue());
                if (partnerCreditLimit.getAvailableLimit().compareTo(partnerShareAmount) < 0) {
                    throw new IllegalArgumentException("伙伴" + partnerPhone + "的可用额度" + partnerCreditLimit.getAvailableLimit() + 
                            "元不足以支付剩余额度" + partnerShareAmount + "元");
                }

                Map<String, Object> partnerRecord = new HashMap<>();
                partnerRecord.put("partner_farmer_id", ((Long) partnerFarmerInfo.get("farmer_id")).longValue());
                partnerRecord.put("partner_share_ratio", partnerShareRatio);
                partnerRecord.put("partner_share_amount", partnerShareAmount); // 记录伙伴实际支付的额度
                partnerRecords.add(partnerRecord);
            }
            
            // 保存发起人的份额信息（需要在数据库中添加字段或使用其他方式存储）
            // 暂时先保存到伙伴记录中，后续可以通过farmer_id区分发起人和伙伴

            // 保存联合贷款伙伴记录到数据库
            saveJointLoanApplicationPartners(applicationRecordId, partnerRecords);
            
            // 调试：验证数据是否正确保存
            System.out.println("=== DEBUG: 验证保存的联合贷款伙伴记录 ===");
            for (Map<String, Object> partnerRecord : partnerRecords) {
                Long partnerFarmerId = ((Long) partnerRecord.get("partner_farmer_id"));
                System.out.println("已保存伙伴记录: partner_farmer_id=" + partnerFarmerId + 
                                 ", loan_application_id(数据库ID)=" + applicationRecordId);
            }
            System.out.println("loan_application_id(字符串ID)=" + loanApplicationId);
            
            // 验证：立即查询保存的数据，确保可以查询到
            List<Map<String, Object>> savedPartners = getJointLoanPartnersByApplicationId(applicationRecordId);
            System.out.println("验证查询：保存后立即查询，找到 " + savedPartners.size() + " 条伙伴记录");
            for (Map<String, Object> savedPartner : savedPartners) {
                System.out.println("  伙伴ID: " + savedPartner.get("partner_farmer_id") + 
                                 ", 状态: " + savedPartner.get("status"));
            }
            System.out.println("=== DEBUG END ===");

            // 更新贷款申请状态为pending_partners（等待合作伙伴确认）
            updateLoanApplicationStatus(loanApplicationId, "pending_partners", null, null, null);

            // 构造成功响应
            JointLoanApplicationResponseDTO response = new JointLoanApplicationResponseDTO();
            response.setLoan_application_id(loanApplicationId);
            response.setStatus("pending_partners");
            response.setProduct_name(loanProduct.getProductName());
            response.setApply_amount(request.getApply_amount());
            response.setInitiator_phone(request.getPhone());
            response.setCreated_at(new Timestamp(System.currentTimeMillis()));
            response.setNext_step("等待伙伴接受邀请");

            // 构造伙伴信息
            List<JointPartnerDTO> partners = new ArrayList<>();
            for (String partnerPhone : request.getPartner_phones()) {
                JointPartnerDTO partner = new JointPartnerDTO();
                partner.setPhone(partnerPhone);
                partner.setStatus("pending_invitation");
                partner.setInvited_at(new Timestamp(System.currentTimeMillis()));
                partners.add(partner);
            }
            response.setPartners(partners);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("申请联合贷款失败: " + e.getMessage(), e);
        }
    }


    // 浏览可联合农户
    public PartnersResponseDTO getJointPartners(PartnersRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validatePartnersRequest(request, errors);
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

            // 检查用户自身是否符合联合贷款条件
            CreditLimit farmerCreditLimit = getCreditLimitByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            if (farmerCreditLimit == null || !"active".equals(farmerCreditLimit.getStatus())) {
                throw new IllegalArgumentException("您当前无可用贷款额度，无法发起联合贷款申请");
            }

            // 准备排除手机号列表（包括发起者自己）
            List<String> excludePhones = new ArrayList<>();
            excludePhones.add(request.getPhone()); // 排除发起者自己

            // 如果请求中有exclude_phones参数，则也排除这些手机号
            if (request.getExclude_phones() != null) {
                excludePhones.addAll(request.getExclude_phones());
            }

            // 设置最大伙伴数量，默认为3
            int maxPartners = request.getMax_partners() != null ? request.getMax_partners() : 3;
            if (maxPartners > 5) maxPartners = 5; // 最多显示5个
            if (maxPartners < 1) maxPartners = 3; // 最少显示1个，默认3个

            // 设置最小信用额度，默认为0
            BigDecimal minCreditLimit = request.getMin_credit_limit() != null ? request.getMin_credit_limit() : BigDecimal.ZERO;

            // 获取符合条件的可联合农户列表
            List<Map<String, Object>> qualifiedPartners = getQualifiedPartners(minCreditLimit, excludePhones, maxPartners);

            // 构造响应
            PartnersResponseDTO response = new PartnersResponseDTO();
            response.setTotal(qualifiedPartners.size());

            // 转换为PartnerItemDTO列表
            List<PartnerItemDTO> partners = new ArrayList<>();
            for (Map<String, Object> partnerInfo : qualifiedPartners) {
                PartnerItemDTO partner = new PartnerItemDTO();
                partner.setPhone((String) partnerInfo.get("phone"));
                partner.setNickname((String) partnerInfo.get("nickname"));
                partner.setAvailable_credit_limit((BigDecimal) partnerInfo.get("available_limit"));
                partner.setTotal_credit_limit((BigDecimal) partnerInfo.get("total_limit"));
                partners.add(partner);
            }
            response.setPartners(partners);

            // 设置推荐理由
            if (qualifiedPartners.isEmpty()) {
                response.setRecommendation_reason("当前条件下无符合条件的伙伴，建议降低额度要求或扩大搜索范围");
            } else {
                response.setRecommendation_reason("找到" + qualifiedPartners.size() + "位符合条件的可联合农户");
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException("获取可联合农户失败: " + e.getMessage(), e);
        }
    }

    /**
     * 智能贷款申请推荐 - 根据申请金额智能推荐单人贷款或联合贷款
     */
    public SmartLoanRecommendationResponseDTO getSmartLoanRecommendation(SmartLoanRecommendationRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateSmartLoanRecommendationRequest(request, errors);
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

            // 获取贷款产品
            LoanProduct loanProduct = getLoanProductById(request.getProduct_id());
            if (loanProduct == null || !"active".equals(loanProduct.getStatus())) {
                throw new IllegalArgumentException("指定的产品" + request.getProduct_id() + "不存在或已下架，请选择其他产品");
            }

            // 检查申请金额是否超过产品最高额度
            if (request.getApply_amount().compareTo(loanProduct.getMaxAmount()) > 0) {
                throw new IllegalArgumentException("申请金额超过产品最高额度");
            }

            // 获取用户信用额度
            CreditLimit farmerCreditLimit = getCreditLimitByFarmerId(((Long) farmerInfo.get("farmer_id")).longValue());
            if (farmerCreditLimit == null || !"active".equals(farmerCreditLimit.getStatus())) {
                throw new IllegalArgumentException("您当前无可用贷款额度");
            }

            // 检查用户的信用额度是否满足产品的最低信用额度要求
            if (farmerCreditLimit.getAvailableLimit().compareTo(loanProduct.getMinCreditLimit()) < 0) {
                throw new IllegalArgumentException("您的可用额度" + farmerCreditLimit.getAvailableLimit() + 
                        "元低于产品最低额度要求" + loanProduct.getMinCreditLimit() + "元，无法申请该产品");
            }

            SmartLoanRecommendationResponseDTO response = new SmartLoanRecommendationResponseDTO();
            response.setUser_available_limit(farmerCreditLimit.getAvailableLimit());
            response.setApply_amount(request.getApply_amount());

            // 检查申请金额是否超过用户可用额度
            if (request.getApply_amount().compareTo(farmerCreditLimit.getAvailableLimit()) <= 0) {
                // 个人额度足够，推荐单人贷款
                response.setRecommendation_type("single");
                response.setRecommendation_reason("您的可用额度足够，建议申请单人贷款，审批更快");
                response.setCan_apply_single(true);
                response.setCan_apply_joint(true); // 也可以选择联合贷款
            } else {
                // 个人额度不够，推荐联合贷款
                BigDecimal shortage = request.getApply_amount().subtract(farmerCreditLimit.getAvailableLimit());
                response.setRecommendation_type("joint");
                response.setRecommendation_reason("您的可用额度不足，缺少¥" + shortage + "，建议寻找合作伙伴联合申请");
                response.setCan_apply_single(false);
                response.setCan_apply_joint(true);

                // 查找合适的联合伙伴（最多1个，总共2人）
                List<String> excludePhones = new ArrayList<>();
                excludePhones.add(request.getPhone());
                
                // 计算所需的伙伴最小额度（两人平分）
                BigDecimal halfAmount = request.getApply_amount().divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);
                
                List<Map<String, Object>> qualifiedPartners = getQualifiedPartnersForAmount(halfAmount, excludePhones, 5);
                
                // 转换为PartnerItemDTO列表
                List<PartnerItemDTO> partners = new ArrayList<>();
                for (Map<String, Object> partnerInfo : qualifiedPartners) {
                    PartnerItemDTO partner = new PartnerItemDTO();
                    partner.setPhone((String) partnerInfo.get("phone"));
                    partner.setNickname((String) partnerInfo.get("nickname"));
                    partner.setAvailable_credit_limit((BigDecimal) partnerInfo.get("available_limit"));
                    partner.setTotal_credit_limit((BigDecimal) partnerInfo.get("total_limit"));
                    partners.add(partner);
                }
                response.setRecommended_partners(partners);
                
                if (partners.isEmpty()) {
                    response.setRecommendation_reason("您的可用额度不足，且暂时找不到合适的合作伙伴，建议先申请提升额度");
                    response.setCan_apply_joint(false);
                }
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException("获取智能贷款推荐失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取符合特定金额要求的伙伴列表
     */
    private List<Map<String, Object>> getQualifiedPartnersForAmount(BigDecimal requiredAmount, List<String> excludePhones, int maxPartners) throws SQLException {
        return dbManager.getQualifiedPartnersForAmount(requiredAmount, excludePhones, maxPartners);
    }

    /**
     * 联合贷款伙伴确认申请
     */
    public JointLoanPartnerConfirmationResponseDTO confirmJointLoanApplication(JointLoanPartnerConfirmationRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateJointLoanPartnerConfirmationRequest(request, errors);
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

            // 获取贷款申请信息
            entity.financing.LoanApplication loanApplication = getLoanApplicationById(request.getApplication_id());
            if (loanApplication == null) {
                throw new IllegalArgumentException("指定的申请ID不存在");
            }

            // 检查申请状态是否为待伙伴确认
            if (!"pending_partners".equals(loanApplication.getStatus())) {
                throw new IllegalArgumentException("该申请状态为" + loanApplication.getStatus() + "，不需要伙伴确认");
            }

            // 检查当前用户是否为该申请的伙伴
            List<Map<String, Object>> partners = getJointLoanPartnersByApplicationId(loanApplication.getId());
            boolean isPartner = false;
            for (Map<String, Object> partner : partners) {
                String partnerUid = getFarmerUidByFarmerId((Long) partner.get("partner_farmer_id"));
                if (user.getUid().equals(partnerUid)) {
                    isPartner = true;
                    break;
                }
            }

            if (!isPartner) {
                throw new IllegalArgumentException("您不是该联合贷款申请的合作伙伴");
            }

            JointLoanPartnerConfirmationResponseDTO response = new JointLoanPartnerConfirmationResponseDTO();
            response.setApplication_id(loanApplication.getLoanApplicationId());
            response.setPartner_phone(request.getPhone());

            if ("confirm".equals(request.getAction())) {
                // 确认参与联合贷款
                // 预扣该伙伴的信用额度
                Long partnerFarmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
                
                // 从数据库记录中获取该伙伴实际需要支付的额度（不是平均分配）
                BigDecimal partnerShareAmount = null;
                for (Map<String, Object> partner : partners) {
                    if (partner.get("partner_farmer_id").equals(partnerFarmerId)) {
                        partnerShareAmount = (BigDecimal) partner.get("partner_share_amount");
                        break;
                    }
                }
                
                if (partnerShareAmount == null) {
                    throw new IllegalArgumentException("未找到该伙伴的份额信息");
                }
                
                // 预扣伙伴额度（使用实际记录的额度）
                preDeductCreditLimit(partnerFarmerId, partnerShareAmount);
                
                // 更新合作伙伴确认状态
                updatePartnerConfirmationStatus(loanApplication.getId(), partnerFarmerId, "confirmed");
                
                // 检查是否所有伙伴都已确认
                if (areAllPartnersConfirmed(loanApplication.getId())) {
                    // 所有伙伴都确认了，预扣发起人的额度
                    // 发起人额度 = 申请金额 - 伙伴支付的额度
                    BigDecimal initiatorShareAmount = loanApplication.getApplyAmount().subtract(partnerShareAmount);
                    
                    // 预扣发起人额度（发起人额度全部用光）
                    preDeductCreditLimit(loanApplication.getFarmerId(), initiatorShareAmount);
                    
                    // 将申请状态更新为待银行审批
                    updateLoanApplicationStatus(loanApplication.getLoanApplicationId(), "pending", null, null, null);
                    response.setNext_step("所有合作伙伴已确认，申请已提交银行审批");
                } else {
                    response.setNext_step("确认成功，等待其他合作伙伴确认");
                }
                
                response.setAction_result("confirmed");
                response.setMessage("您已成功确认参与该联合贷款申请");
                
            } else if ("reject".equals(request.getAction())) {
                // 拒绝参与联合贷款
                Long rejectingPartnerFarmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
                updatePartnerConfirmationStatus(loanApplication.getId(), rejectingPartnerFarmerId, "rejected");
                
                // 将申请状态更新为已拒绝
                updateLoanApplicationStatus(loanApplication.getLoanApplicationId(), "rejected", null, null, null, "合作伙伴拒绝参与");
                
                // 恢复已确认伙伴的预扣额度（使用实际记录的额度，不是平均分配）
                // 获取所有已确认的伙伴，恢复他们的预扣额度
                List<Map<String, Object>> allPartners = getJointLoanPartnersByApplicationId(loanApplication.getId());
                for (Map<String, Object> partner : allPartners) {
                    String status = (String) partner.get("status");
                    if ("confirmed".equals(status) || "accepted".equals(status)) {
                        Long confirmedPartnerFarmerId = (Long) partner.get("partner_farmer_id");
                        BigDecimal confirmedPartnerShareAmount = (BigDecimal) partner.get("partner_share_amount");
                        // 使用实际记录的额度进行返还
                        restoreCreditLimit(confirmedPartnerFarmerId, confirmedPartnerShareAmount);
                    }
                }
                
                response.setAction_result("rejected");
                response.setMessage("您已拒绝参与该联合贷款申请，申请已被取消");
                response.setNext_step("联合贷款申请已取消");
            } else {
                throw new IllegalArgumentException("无效的操作类型：" + request.getAction());
            }

            return response;

        } catch (Exception e) {
            throw new RuntimeException("联合贷款伙伴确认失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户待确认的联合贷款申请
     */
    public PendingJointLoanApplicationsResponseDTO getPendingJointLoanApplications(PendingJointLoanApplicationsRequestDTO request) {
        try {
            // 参数验证
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空");
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

            // 获取待确认的联合贷款申请列表
            Long farmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
            List<Map<String, Object>> pendingApplications = getPendingJointLoanApplicationsByFarmerId(farmerId);

            PendingJointLoanApplicationsResponseDTO response = new PendingJointLoanApplicationsResponseDTO();
            response.setTotal(pendingApplications.size());
            response.setApplications(pendingApplications);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("获取待确认联合贷款申请失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证联合贷款伙伴确认请求
     */
    private void validateJointLoanPartnerConfirmationRequest(JointLoanPartnerConfirmationRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "phone");
            error.put("message", "手机号不能为空");
            errors.add(error);
        }

        if (request.getApplication_id() == null || request.getApplication_id().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "application_id");
            error.put("message", "申请ID不能为空");
            errors.add(error);
        }

        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "action");
            error.put("message", "操作类型不能为空");
            errors.add(error);
        } else if (!"confirm".equals(request.getAction()) && !"reject".equals(request.getAction())) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "action");
            error.put("message", "操作类型必须为confirm或reject");
            errors.add(error);
        }
    }

    /**
     * 验证智能贷款推荐请求
     */
    private void validateSmartLoanRecommendationRequest(SmartLoanRecommendationRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "phone");
            error.put("message", "手机号不能为空");
            errors.add(error);
        }

        if (request.getProduct_id() == null || request.getProduct_id().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "product_id");
            error.put("message", "产品ID不能为空");
            errors.add(error);
        }

        if (request.getApply_amount() == null || request.getApply_amount().compareTo(BigDecimal.ZERO) <= 0) {
            Map<String, String> error = new HashMap<>();
            error.put("field", "apply_amount");
            error.put("message", "申请金额必须大于0");
            errors.add(error);
        }
    }

    // 创建贷款申请记录
    private long createLoanApplication(String loanApplicationId, Long farmerId, Long productId, String applicationType,
                                       BigDecimal applyAmount, String purpose, String repaymentSource) throws SQLException {
        return dbManager.createLoanApplication(loanApplicationId, farmerId, productId, applicationType,
                applyAmount, purpose, repaymentSource);
    }

    private void saveJointLoanApplicationPartners(long loanApplicationId, List<Map<String, Object>> partners) throws SQLException {
        dbManager.saveJointLoanApplicationPartners(loanApplicationId, partners);
    }

    /**
     * 预扣农户信用额度
     */
    private void preDeductCreditLimit(Long farmerId, BigDecimal amount) throws SQLException {
        dbManager.preDeductCreditLimit(farmerId, amount);
    }

    /**
     * 恢复农户信用额度（申请失败时调用）
     */
    private void restoreCreditLimit(Long farmerId, BigDecimal amount) throws SQLException {
        dbManager.restoreCreditLimit(farmerId, amount);
    }

    /**
     * 更新合作伙伴确认状态
     */
    private void updatePartnerConfirmationStatus(Long loanApplicationId, Long partnerFarmerId, String status) throws SQLException {
        dbManager.updatePartnerConfirmationStatus(loanApplicationId, partnerFarmerId, status);
    }

    /**
     * 检查所有伙伴是否都已确认
     */
    private boolean areAllPartnersConfirmed(Long loanApplicationId) throws SQLException {
        return dbManager.areAllPartnersConfirmed(loanApplicationId);
    }

    /**
     * 获取用户待确认的联合贷款申请
     */
    private List<Map<String, Object>> getPendingJointLoanApplicationsByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getPendingJointLoanApplicationsByFarmerId(farmerId);
    }

    /**
     * 根据农户ID获取用户ID
     */
    private String getFarmerUidByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getFarmerUidByFarmerId(farmerId);
    }

    /**
     * 确认扣除农户信用额度（贷款批准时调用）
     */
    private void confirmDeductCreditLimit(Long farmerId, BigDecimal amount) throws SQLException {
        dbManager.confirmDeductCreditLimit(farmerId, amount);
    }

    /**
     * 银行审批贷款申请
     */
    public LoanApprovalResponseDTO approveLoan(LoanApprovalRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateLoanApprovalRequest(request, errors);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("参数验证失败: " + formatErrors(errors));
            }

            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行审批权限");
            }

            // 获取贷款申请信息
            entity.financing.LoanApplication loanApplication = getLoanApplicationById(request.getApplication_id());
            if (loanApplication == null) {
                throw new IllegalArgumentException("指定的申请ID不存在");
            }

            // 检查申请状态
            if (!"pending".equals(loanApplication.getStatus())) {
                if ("pending_partners".equals(loanApplication.getStatus())) {
                    throw new IllegalArgumentException("该联合贷款申请还在等待合作伙伴确认，不能审批。请等待所有合作伙伴确认后再审批。");
                }
                throw new IllegalArgumentException("该申请状态为" + loanApplication.getStatus() + "，不能审批");
            }

            // 构造响应
            LoanApprovalResponseDTO response = new LoanApprovalResponseDTO();
            response.setApplication_id(loanApplication.getLoanApplicationId());

            // 根据审批动作处理
            if ("approve".equals(request.getAction())) {
                // 批准申请
                if (request.getApproved_amount() == null) {
                    throw new IllegalArgumentException("批准金额不能为空");
                }

                // 更新贷款申请状态为已批准
                updateLoanApplicationStatus(
                        request.getApplication_id(),
                        "approved",
                        ((Long) bankInfo.get("bank_id")).longValue(),
                        new Timestamp(System.currentTimeMillis()),
                        request.getApproved_amount()
                );

                response.setStatus("approved");
                response.setApproved_amount(request.getApproved_amount());
                response.setApproved_by((String) bankInfo.get("bank_name"));
                response.setApproved_at(new Timestamp(System.currentTimeMillis()));
                response.setNext_step("等待放款");
            } else if ("reject".equals(request.getAction())) {
                // 拒绝申请
                if (request.getReject_reason() == null || request.getReject_reason().trim().isEmpty()) {
                    throw new IllegalArgumentException("拒绝原因不能为空");
                }

                // 还原预扣的信用额度
                // 如果是联合贷款，需要返还发起人和所有已确认伙伴的额度
                if ("joint".equals(loanApplication.getApplicationType())) {
                    // 获取联合贷款伙伴信息
                    List<Map<String, Object>> partners = getJointLoanPartnersByApplicationId(loanApplication.getId());
                    
                    // 计算发起人实际支付的额度（申请金额 - 伙伴支付的额度）
                    BigDecimal partnerShareAmount = BigDecimal.ZERO;
                    for (Map<String, Object> partner : partners) {
                        String status = (String) partner.get("status");
                        if ("confirmed".equals(status) || "accepted".equals(status)) {
                            BigDecimal shareAmount = (BigDecimal) partner.get("partner_share_amount");
                            if (shareAmount != null) {
                                partnerShareAmount = shareAmount;
                                // 返还已确认伙伴的额度
                                Long partnerFarmerId = (Long) partner.get("partner_farmer_id");
                                restoreCreditLimit(partnerFarmerId, shareAmount);
                            }
                        }
                    }
                    
                    // 返还发起人的额度（发起人额度 = 申请金额 - 伙伴支付的额度）
                    BigDecimal initiatorShareAmount = loanApplication.getApplyAmount().subtract(partnerShareAmount);
                    restoreCreditLimit(loanApplication.getFarmerId(), initiatorShareAmount);
                } else {
                    // 单人贷款，直接返还申请金额
                    restoreCreditLimit(loanApplication.getFarmerId(), loanApplication.getApplyAmount());
                }

                // 更新贷款申请状态为已拒绝
                updateLoanApplicationRejection(
                        request.getApplication_id(),
                        "rejected",
                        ((Long) bankInfo.get("bank_id")).longValue(),
                        new Timestamp(System.currentTimeMillis()),
                        request.getReject_reason()
                );

                response.setStatus("rejected");
                response.setReject_reason(request.getReject_reason());
                response.setRejected_by((String) bankInfo.get("bank_name"));
                response.setRejected_at(new Timestamp(System.currentTimeMillis()));
            } else {
                throw new IllegalArgumentException("审批动作必须是approve或reject");
            }

            return response;
        } catch (Exception e) {
            throw new RuntimeException("审批贷款申请失败: " + e.getMessage(), e);
        }
    }


    /**
     * 银行放款操作
     */
    public LoanDisbursementResponseDTO disburseLoan(LoanDisbursementRequestDTO request) {
        try {
            // 参数验证
            List<Map<String, String>> errors = new ArrayList<>();
            validateLoanDisbursementRequest(request, errors);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("参数验证失败: " + formatErrors(errors));
            }

            // 检查用户是否存在且为银行用户
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 检查用户是否具有银行身份
            Map<String, Object> bankInfo = checkUserBankRole(user.getUid());
            if (bankInfo == null) {
                throw new IllegalArgumentException("该用户无银行放款权限，请联系管理员开通权限");
            }

            // 获取贷款申请信息
            entity.financing.LoanApplication loanApplication = getLoanApplicationById(request.getApplication_id());
            if (loanApplication == null) {
                throw new IllegalArgumentException("指定的申请ID不存在");
            }

            // 检查申请状态
            if (!"approved".equals(loanApplication.getStatus())) {
                throw new IllegalArgumentException("该申请状态为" + loanApplication.getStatus() + "，必须先审批通过才能放款");
            }

            // 检查放款金额是否等于批准金额
            if (request.getDisburse_amount().compareTo(loanApplication.getApprovedAmount()) != 0) {
                if (request.getDisburse_amount().compareTo(loanApplication.getApprovedAmount()) > 0) {
                    throw new IllegalArgumentException("放款金额" + request.getDisburse_amount() + "元不能大于批准的金额" + loanApplication.getApprovedAmount() + "元");
                } else {
                    throw new IllegalArgumentException("放款金额" + request.getDisburse_amount() + "元必须等于批准的金额" + loanApplication.getApprovedAmount() + "元，不支持分批放款");
                }
            }

            // 获取贷款产品信息 (修改点：通过productId获取贷款产品)
            entity.financing.LoanProduct loanProduct = getLoanProductById(loanApplication.getProductId());
            if (loanProduct == null) {
                throw new IllegalArgumentException("贷款产品不存在");
            }

            // 生成贷款ID
            String loanId = "LOAN" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
            if (loanId.length() > 20) {
                loanId = loanId.substring(0, 20);
            }

            // 计算还款计划（简化计算）
            BigDecimal totalRepaymentAmount = calculateTotalRepaymentAmount(
                    request.getDisburse_amount(),
                    loanProduct.getInterestRate(),
                    loanProduct.getTermMonths()
            );

            BigDecimal monthlyPayment = totalRepaymentAmount.divide(
                    BigDecimal.valueOf(loanProduct.getTermMonths()),
                    2,
                    BigDecimal.ROUND_HALF_UP
            );

            // 计算下次还款日期（放款日期后一个月）
            Timestamp disburseDate = new Timestamp(System.currentTimeMillis());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(disburseDate);
            calendar.add(Calendar.MONTH, 1);
            Date nextPaymentDate = new Date(calendar.getTimeInMillis());
            
            // 首次还款日期（与下次还款日期相同，因为这是首次）
            Date firstRepaymentDate = new Date(nextPaymentDate.getTime());

            // 创建贷款记录
            entity.financing.Loan loan = new entity.financing.Loan();
            loan.setLoanId(loanId);
            loan.setFarmerId(loanApplication.getFarmerId());
            loan.setProductId(loanProduct.getId());
            loan.setLoanAmount(request.getDisburse_amount());
            loan.setInterestRate(loanProduct.getInterestRate());
            loan.setTermMonths(loanProduct.getTermMonths());
            loan.setRepaymentMethod(loanProduct.getRepaymentMethod());
            loan.setDisburseAmount(request.getDisburse_amount());
            loan.setDisburseMethod("bank_transfer"); // 默认银行转账
            loan.setDisburseDate(disburseDate);
            loan.setFirstRepaymentDate(firstRepaymentDate); // 设置为下次还款日期
            loan.setDisburseRemarks(request.getRemarks());
            loan.setLoanStatus("active");
            loan.setApprovedBy(((Long) bankInfo.get("bank_id")).longValue());
            loan.setApprovedAt(new Timestamp(System.currentTimeMillis()));
            loan.setTotalRepaymentAmount(totalRepaymentAmount);
            loan.setRemainingPrincipal(request.getDisburse_amount());
            loan.setNextPaymentDate(nextPaymentDate);
            loan.setNextPaymentAmount(monthlyPayment);
            loan.setPurpose(loanApplication.getPurpose());
            loan.setRepaymentSource(loanApplication.getRepaymentSource());
            loan.setIsJointLoan("joint".equals(loanApplication.getApplicationType()));
            loan.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            loan.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            // 保存贷款记录
            long loanRecordId = saveLoan(loan);

            // 处理资金发放
            if ("joint".equals(loanApplication.getApplicationType())) {
                // 联合贷款处理
                handleJointLoanDisbursement(loanApplication, loan, loanRecordId);
            } else {
                // 单人贷款处理
                handleSingleLoanDisbursement(loanApplication, loan);
            }

            // 更新贷款申请状态为已放款
            updateLoanApplicationStatus(
                    request.getApplication_id(),
                    "disbursed",
                    ((Long) bankInfo.get("bank_id")).longValue(),
                    new Timestamp(System.currentTimeMillis()),
                    request.getDisburse_amount()
            );

            // 构造响应
            LoanDisbursementResponseDTO response = new LoanDisbursementResponseDTO();
            response.setLoan_id(loanId);
            response.setDisbursement_id("DIS" + System.currentTimeMillis());
            response.setApplication_id(request.getApplication_id());
            response.setDisburse_amount(request.getDisburse_amount());
            response.setDisburse_date(new Timestamp(System.currentTimeMillis()));
            response.setLoan_status("active");
            response.setTotal_repayment_amount(totalRepaymentAmount);
            response.setMonthly_payment(monthlyPayment);
            response.setNext_payment_date(nextPaymentDate);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("放款操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询还款计划
     */
    public RepaymentScheduleResponseDTO getRepaymentSchedule(String phone, String loanId) throws SQLException {
        // 参数验证
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        if (loanId == null || loanId.trim().isEmpty()) {
            throw new IllegalArgumentException("贷款ID不能为空");
        }

        // 检查用户是否存在
        User user = findUserByPhone(phone);
        if (user == null) {
            throw new IllegalArgumentException("该手机号未注册");
        }

        // 检查用户是否具有农户身份
        Map<String, Object> farmerInfo = checkUserFarmerRole(user.getUid());
        if (farmerInfo == null) {
            throw new IllegalArgumentException("该用户不是农户");
        }

        // 获取贷款信息
        entity.financing.Loan loan = getLoanById(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("指定的贷款ID不存在");
        }

        // 检查是否有权限查看该贷款信息（支持联合贷款）
        boolean hasPermission = false;

        // 检查是否为主借款人
        if (loan.getFarmerId().equals(((Long) farmerInfo.get("farmer_id")).longValue())) {
            hasPermission = true;
        }
        // 如果是联合贷款，检查是否为联合贷款人
        else if (loan.getIsJointLoan()) {
            List<Map<String, Object>> jointLoanPartners = getJointLoanPartnersByLoanId(loan.getId());
            for (Map<String, Object> partner : jointLoanPartners) {
                if (partner.get("partner_farmer_id").equals(((Long) farmerInfo.get("farmer_id")).longValue())) {
                    hasPermission = true;
                    break;
                }
            }
        }

        if (!hasPermission) {
            throw new IllegalArgumentException("只能查看自己的贷款信息");
        }

        // 构造响应
        RepaymentScheduleResponseDTO response = new RepaymentScheduleResponseDTO();
        response.setLoan_id(loan.getLoanId());
        response.setLoan_status(loan.getLoanStatus());
        response.setLoan_amount(safeBigDecimal(loan.getLoanAmount()));
        response.setInterest_rate(safeBigDecimal(loan.getInterestRate()));
        response.setTerm_months(loan.getTermMonths());
        response.setRepayment_method(loan.getRepaymentMethod());

        // 处理日期字段，如果为null则设为null（显示时可表示为"暂无"）
        if (loan.getDisburseDate() != null) {
            response.setDisburse_date(new java.sql.Date(loan.getDisburseDate().getTime()));
        } else {
            response.setDisburse_date(null);
        }

        response.setTotal_periods(loan.getTermMonths());
        response.setCurrent_period(loan.getCurrentPeriod());
        response.setRemaining_principal(safeBigDecimal(loan.getRemainingPrincipal()));

        // 计算到期日期
        if (loan.getDisburseDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(loan.getDisburseDate());
            calendar.add(Calendar.MONTH, loan.getTermMonths());
            response.setMaturity_date(new java.sql.Date(calendar.getTimeInMillis()));
        } else {
            response.setMaturity_date(null);
        }

        // 如果贷款已结清，设置结清日期
        if ("closed".equals(loan.getLoanStatus()) && loan.getClosedDate() != null) {
            response.setClosed_date(new java.sql.Date(loan.getClosedDate().getTime()));
        } else if ("closed".equals(loan.getLoanStatus())) {
            response.setClosed_date(null); // closed状态但无closed_date时设为null
        }

        // 设置当前应还信息
        if (!"closed".equals(loan.getLoanStatus()) && loan.getNextPaymentDate() != null &&
                loan.getNextPaymentAmount() != null) {

            RepaymentScheduleResponseDTO.DueInfo dueInfo = new RepaymentScheduleResponseDTO.DueInfo();
            dueInfo.setDue_date(loan.getNextPaymentDate());
            dueInfo.setDue_amount(safeBigDecimal(loan.getNextPaymentAmount()));

            // 安全计算本金和利息
            BigDecimal nextPaymentAmount = safeBigDecimal(loan.getNextPaymentAmount());
            BigDecimal remainingPrincipal = safeBigDecimal(loan.getRemainingPrincipal());
            BigDecimal interestRate = safeBigDecimal(loan.getInterestRate());

            // 计算月利息
            BigDecimal monthlyInterest = BigDecimal.ZERO;
            if (interestRate.compareTo(BigDecimal.ZERO) > 0 && remainingPrincipal.compareTo(BigDecimal.ZERO) > 0) {
                monthlyInterest = remainingPrincipal.multiply(interestRate)
                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                        .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
            }

            dueInfo.setPrincipal_amount(nextPaymentAmount.subtract(monthlyInterest));
            dueInfo.setInterest_amount(monthlyInterest);
            dueInfo.setDays_overdue(loan.getOverdueDays());
            dueInfo.setOverdue_interest(safeBigDecimal(loan.getOverdueAmount()));
            response.setCurrent_due(dueInfo);
        }

        // 设置下次还款信息
        if (!"closed".equals(loan.getLoanStatus()) && loan.getNextPaymentDate() != null &&
                loan.getNextPaymentAmount() != null) {

            RepaymentScheduleResponseDTO.PaymentInfo paymentInfo = new RepaymentScheduleResponseDTO.PaymentInfo();
            paymentInfo.setPayment_date(loan.getNextPaymentDate());
            paymentInfo.setPayment_amount(safeBigDecimal(loan.getNextPaymentAmount()));

            // 安全计算本金和利息
            BigDecimal nextPaymentAmount = safeBigDecimal(loan.getNextPaymentAmount());
            BigDecimal remainingPrincipal = safeBigDecimal(loan.getRemainingPrincipal());
            BigDecimal interestRate = safeBigDecimal(loan.getInterestRate());

            // 计算月利息
            BigDecimal monthlyInterest = BigDecimal.ZERO;
            if (interestRate.compareTo(BigDecimal.ZERO) > 0 && remainingPrincipal.compareTo(BigDecimal.ZERO) > 0) {
                monthlyInterest = remainingPrincipal.multiply(interestRate)
                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                        .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
            }

            paymentInfo.setPrincipal_amount(nextPaymentAmount.subtract(monthlyInterest));
            paymentInfo.setInterest_amount(monthlyInterest);
            response.setNext_payment(paymentInfo);
        }

        // 设置汇总信息
        RepaymentScheduleResponseDTO.SummaryInfo summaryInfo = new RepaymentScheduleResponseDTO.SummaryInfo();
        summaryInfo.setTotal_paid(safeBigDecimal(loan.getTotalPaidAmount()));
        summaryInfo.setPrincipal_paid(safeBigDecimal(loan.getTotalPaidPrincipal()));
        summaryInfo.setInterest_paid(safeBigDecimal(loan.getTotalPaidInterest()));

        BigDecimal totalRepaymentAmount = safeBigDecimal(loan.getTotalRepaymentAmount());
        BigDecimal totalPaidAmount = safeBigDecimal(loan.getTotalPaidAmount());
        BigDecimal loanAmount = safeBigDecimal(loan.getLoanAmount());
        BigDecimal remainingPrincipal = safeBigDecimal(loan.getRemainingPrincipal());

        summaryInfo.setRemaining_total(totalRepaymentAmount.subtract(totalPaidAmount));
        summaryInfo.setRemaining_principal(remainingPrincipal);
        // 计算剩余利息
        summaryInfo.setRemaining_interest(totalRepaymentAmount.subtract(loanAmount));
        response.setSummary(summaryInfo);

        // 生成详细的还款计划
        List<RepaymentScheduleResponseDTO.RepaymentPlanItem> repaymentPlan = generateRepaymentPlan(loan);
        response.setRepayment_plan(repaymentPlan);

        return response;
    }

    // 私有辅助方法：安全处理BigDecimal，null值返回BigDecimal.ZERO
    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // 私有辅助方法：根据贷款ID获取贷款信息
    private entity.financing.Loan getLoanById(String loanId) throws SQLException {
        return dbManager.getLoanById(loanId);
    }

    // 私有辅助方法：根据贷款ID获取联合贷款伙伴信息
    private List<Map<String, Object>> getJointLoanPartnersByLoanId(long loanId) throws SQLException {
        return dbManager.getJointLoanPartnersByLoanId(loanId);
    }




    /**
     * 处理单人贷款放款
     */
    private void handleSingleLoanDisbursement(entity.financing.LoanApplication loanApplication,
                                              entity.financing.Loan loan) throws SQLException {
        // 获取主借款人信息
        String farmerUid = getFarmerUidByFarmerId(loanApplication.getFarmerId());
        if (farmerUid == null) {
            throw new SQLException("未找到主借款人信息");
        }

        // 给主借款人账户增加资金
        updateUserBalance(farmerUid, loan.getDisburseAmount());
    }

    /**
     * 处理联合贷款放款
     * 按各自实际支付的额度分别放款：发起人额度全部用光，伙伴支付剩余额度
     */
    private void handleJointLoanDisbursement(entity.financing.LoanApplication loanApplication,
                                             entity.financing.Loan loan,
                                             long loanRecordId) throws SQLException {
        // 获取联合贷款伙伴信息
        List<Map<String, Object>> partners = getJointLoanPartnersByApplicationId(loanApplication.getId());

        // 获取主借款人信息
        String mainFarmerUid = getFarmerUidByFarmerId(loanApplication.getFarmerId());
        if (mainFarmerUid == null) {
            throw new SQLException("未找到主借款人信息");
        }

        // 计算发起人实际支付的额度（申请金额 - 伙伴支付的额度）
        BigDecimal partnerShareAmount = BigDecimal.ZERO;
        for (Map<String, Object> partner : partners) {
            String status = (String) partner.get("status");
            if ("confirmed".equals(status) || "accepted".equals(status)) {
                BigDecimal shareAmount = (BigDecimal) partner.get("partner_share_amount");
                if (shareAmount != null) {
                    partnerShareAmount = shareAmount;
                    break; // 双人联合贷款只有一个伙伴
                }
            }
        }
        
        // 发起人实际支付的额度 = 申请金额 - 伙伴支付的额度
        BigDecimal initiatorShareAmount = loanApplication.getApplyAmount().subtract(partnerShareAmount);
        
        // 计算份额比例（用于记录）
        BigDecimal initiatorShareRatio = initiatorShareAmount.divide(loanApplication.getApplyAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal partnerShareRatio = partnerShareAmount.divide(loanApplication.getApplyAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
        
        // 按批准金额的比例计算实际放款金额
        // 如果批准金额与申请金额不同，需要按比例调整
        BigDecimal approvalRatio = loan.getLoanAmount().divide(loanApplication.getApplyAmount(), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal initiatorDisburseAmount = initiatorShareAmount.multiply(approvalRatio).setScale(2, BigDecimal.ROUND_HALF_UP);

        // 给发起人账户增加资金（按实际支付的额度比例放款）
        updateUserBalance(mainFarmerUid, initiatorDisburseAmount);

        // 为主申请人创建联合贷款记录
        BigDecimal mainPrincipal = initiatorDisburseAmount;
        BigDecimal mainInterest = calculateTotalInterest(mainPrincipal, loan.getInterestRate(), loan.getTermMonths());
        BigDecimal mainTotalRepayment = mainPrincipal.add(mainInterest);

        entity.financing.JointLoan mainJointLoan = new entity.financing.JointLoan();
        mainJointLoan.setLoanId(loanRecordId);
        mainJointLoan.setPartnerFarmerId(loanApplication.getFarmerId()); // 主申请人的农户ID
        mainJointLoan.setPartnerShareRatio(initiatorShareRatio); // 使用实际支付的份额比例
        mainJointLoan.setPartnerShareAmount(initiatorShareAmount); // 记录发起人实际支付的额度
        mainJointLoan.setPartnerPrincipal(mainPrincipal); // 实际放款的本金
        mainJointLoan.setPartnerInterest(mainInterest);
        mainJointLoan.setPartnerTotalRepayment(mainTotalRepayment);
        mainJointLoan.setPartnerPaidAmount(BigDecimal.ZERO);
        mainJointLoan.setPartnerRemainingPrincipal(mainPrincipal);
        mainJointLoan.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        mainJointLoan.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        saveJointLoan(mainJointLoan);

        // 为每个联合贷款伙伴创建记录并处理资金
        for (Map<String, Object> partner : partners) {
            String status = (String) partner.get("status");
            if (!"confirmed".equals(status) && !"accepted".equals(status)) {
                continue; // 跳过未确认的伙伴
            }
            
            Long partnerFarmerId = (Long) partner.get("partner_farmer_id");
            // 使用数据库中记录的伙伴实际支付的额度
            BigDecimal partnerShareAmountFromDB = (BigDecimal) partner.get("partner_share_amount");
            BigDecimal partnerShareRatioFromDB = (BigDecimal) partner.get("partner_share_ratio");
            
            // 按批准金额的比例计算实际放款金额
            BigDecimal partnerActualDisburseAmount = partnerShareAmountFromDB.multiply(approvalRatio).setScale(2, BigDecimal.ROUND_HALF_UP);

            // 获取伙伴用户ID
            String partnerUid = getFarmerUidByFarmerId(partnerFarmerId);
            if (partnerUid == null) {
                throw new SQLException("未找到联合贷款伙伴信息: " + partner.get("phone"));
            }

            // 给伙伴账户增加资金（按实际支付的额度比例放款）
            updateUserBalance(partnerUid, partnerActualDisburseAmount);

            // 计算伙伴的利息和总还款金额（与主贷款人使用相同的利率和期限）
            BigDecimal partnerPrincipal = partnerActualDisburseAmount;
            BigDecimal partnerInterest = calculateTotalInterest(partnerPrincipal, loan.getInterestRate(), loan.getTermMonths());
            BigDecimal partnerTotalRepayment = partnerPrincipal.add(partnerInterest);

            // 创建联合贷款记录
            entity.financing.JointLoan jointLoan = new entity.financing.JointLoan();
            jointLoan.setLoanId(loanRecordId);
            jointLoan.setPartnerFarmerId(partnerFarmerId);
            jointLoan.setPartnerShareRatio(partnerShareRatioFromDB); // 使用实际支付的份额比例
            jointLoan.setPartnerShareAmount(partnerShareAmountFromDB); // 记录伙伴实际支付的额度
            jointLoan.setPartnerPrincipal(partnerPrincipal); // 实际放款的本金
            jointLoan.setPartnerInterest(partnerInterest);
            jointLoan.setPartnerTotalRepayment(partnerTotalRepayment);
            jointLoan.setPartnerPaidAmount(BigDecimal.ZERO);
            jointLoan.setPartnerRemainingPrincipal(partnerPrincipal);
            jointLoan.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            jointLoan.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            saveJointLoan(jointLoan);
        }
    }



    /**
     * 计算总利息（简化计算）
     */
    private BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal interestRate, int termMonths) {
        // 总利息 = 本金 * 年利率 * 期限(年)
        BigDecimal annualInterest = principal.multiply(interestRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        return annualInterest.multiply(BigDecimal.valueOf(termMonths)).divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
    }


    /**
     * 计算总还款金额（简化计算）
     */
    private BigDecimal calculateTotalRepaymentAmount(BigDecimal principal, BigDecimal interestRate, int termMonths) {
        // 简化计算：总利息 = 本金 * 年利率 * 期限(年)
        BigDecimal annualInterest = principal.multiply(interestRate).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalInterest = annualInterest.multiply(BigDecimal.valueOf(termMonths)).divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
        return principal.add(totalInterest);
    }

    /**
     * 验证贷款审批请求
     */
    private void validateLoanApprovalRequest(LoanApprovalRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "银行操作员手机号不能为空"));
        }

        if (request.getApplication_id() == null || request.getApplication_id().trim().isEmpty()) {
            errors.add(createError("application_id", "贷款申请ID不能为空"));
        }

        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            errors.add(createError("action", "审批动作不能为空"));
        } else if (!Arrays.asList("approve", "reject").contains(request.getAction())) {
            errors.add(createError("action", "审批动作必须是approve或reject"));
        }

        if ("approve".equals(request.getAction())) {
            if (request.getApproved_amount() == null || request.getApproved_amount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(createError("approved_amount", "批准金额必须大于0"));
            }
        }

        if ("reject".equals(request.getAction())) {
            if (request.getReject_reason() == null || request.getReject_reason().trim().isEmpty()) {
                errors.add(createError("reject_reason", "拒绝原因不能为空"));
            } else if (request.getReject_reason().length() < 2 || request.getReject_reason().length() > 200) {
                errors.add(createError("reject_reason", "拒绝原因必须在2-200个字符之间"));
            }
        }
    }

    /**
     * 验证贷款放款请求
     */
    private void validateLoanDisbursementRequest(LoanDisbursementRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "银行操作员手机号不能为空"));
        }

        if (request.getApplication_id() == null || request.getApplication_id().trim().isEmpty()) {
            errors.add(createError("application_id", "贷款申请ID不能为空"));
        }

        if (request.getDisburse_amount() == null || request.getDisburse_amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("disburse_amount", "放款金额必须大于0"));
        }

        if (request.getRemarks() != null && request.getRemarks().length() > 200) {
            errors.add(createError("remarks", "放款备注不能超过200个字符"));
        }
    }

    // 私有辅助方法：验证还款请求
    private void validateRepaymentRequest(RepaymentRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "农户手机号不能为空"));
        }

        if (request.getLoan_id() == null || request.getLoan_id().trim().isEmpty()) {
            errors.add(createError("loan_id", "贷款ID不能为空"));
        }

        if (request.getRepayment_method() == null || request.getRepayment_method().trim().isEmpty()) {
            errors.add(createError("repayment_method", "还款方式不能为空"));
        } else if (!Arrays.asList("normal", "partial", "advance").contains(request.getRepayment_method())) {
            errors.add(createError("repayment_method", "还款方式必须是 normal（正常还款）、partial（部分还款）或 advance（提前还款）"));
        }

        if (request.getRepayment_amount() != null && request.getRepayment_amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("repayment_amount", "还款金额必须大于0"));
        }

        if (request.getRemarks() != null && request.getRemarks().length() > 100) {
            errors.add(createError("remarks", "还款备注不能超过100个字符"));
        }
    }

    /**
     * 农户发起还款
     */
    public RepaymentResponseDTO makeRepayment(RepaymentRequestDTO request) throws SQLException {
        // 参数验证
        List<Map<String, String>> errors = new ArrayList<>();
        validateRepaymentRequest(request, errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("参数验证失败: " + formatErrors(errors));
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

        // 获取贷款信息
        entity.financing.Loan loan = getLoanById(request.getLoan_id());
        if (loan == null) {
            throw new IllegalArgumentException("指定的贷款" + request.getLoan_id() + "不存在或已被删除");
        }

        // 检查是否有权限还款该贷款（支持联合贷款）
        boolean hasPermission = false;
        boolean isJointPartner = false;
        Long partnerFarmerId = null;

        // 检查是否为主借款人
        if (loan.getFarmerId().equals(((Long) farmerInfo.get("farmer_id")).longValue())) {
            hasPermission = true;
        }
        // 如果是联合贷款，检查是否为联合贷款人
        else if (loan.getIsJointLoan()) {
            List<Map<String, Object>> jointLoanPartners = getJointLoanPartnersByLoanId(loan.getId());
            for (Map<String, Object> partner : jointLoanPartners) {
                if (partner.get("partner_farmer_id").equals(((Long) farmerInfo.get("farmer_id")).longValue())) {
                    hasPermission = true;
                    isJointPartner = true;
                    partnerFarmerId = ((Long) farmerInfo.get("farmer_id")).longValue();
                    break;
                }
            }
        }

        if (!hasPermission) {
            throw new IllegalArgumentException("只能还款自己的贷款，不能操作他人的贷款");
        }

        // 检查贷款状态是否允许还款
        if ("closed".equals(loan.getLoanStatus())) {
            throw new IllegalArgumentException("该贷款状态为closed，已结清，无需还款");
        } else if ("frozen".equals(loan.getLoanStatus())) {
            throw new IllegalArgumentException("该贷款状态为frozen，因严重逾期被冻结，请联系银行处理");
        } else if (!"active".equals(loan.getLoanStatus())) {
            throw new IllegalArgumentException("该贷款状态为" + loan.getLoanStatus() + "，不允许还款");
        }

        // 验证还款金额
        if (request.getRepayment_amount() == null) {
            // 如果未指定还款金额，默认为应还总额
            request.setRepayment_amount(loan.getNextPaymentAmount());
        }

        // 计算本金和利息分配（必须在更新贷款状态之前计算）
        BigDecimal repaymentAmount = request.getRepayment_amount();
        // 注意：数据库中存储的利率已经是百分比形式（如12.00表示12%），所以需要除以100
        BigDecimal monthlyInterest = loan.getRemainingPrincipal().multiply(loan.getInterestRate())
            .divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP)
            .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal principalAmount;
        BigDecimal interestAmount;
        
        // 简化计算：如果还款金额大于等于月利息，剩余部分归本金
        if (repaymentAmount.compareTo(monthlyInterest) >= 0) {
            interestAmount = monthlyInterest;
            principalAmount = repaymentAmount.subtract(monthlyInterest);
        } else {
            // 还款金额不足以覆盖利息，全部算作利息
            interestAmount = repaymentAmount;
            principalAmount = BigDecimal.ZERO;
        }

        // 创建虚拟还款记录（不保存到数据库）
        entity.financing.Repayment repayment = new entity.financing.Repayment();
        repayment.setRepaymentAmount(request.getRepayment_amount());
        repayment.setPrincipalAmount(principalAmount);
        repayment.setInterestAmount(interestAmount);
        repayment.setRepaymentMethod(request.getRepayment_method());
        repayment.setRepaymentDate(new Timestamp(System.currentTimeMillis()));

        // 更新贷款状态（包含本金和利息的更新）
        updateLoanAfterRepayment(loan, repayment, isJointPartner, partnerFarmerId);

        // 构造响应
        RepaymentResponseDTO response = new RepaymentResponseDTO();
        response.setRepayment_id("REP" + System.currentTimeMillis()); // 生成虚拟ID
        response.setLoan_id(loan.getLoanId());
        response.setRepayment_amount(request.getRepayment_amount());
        response.setPrincipal_amount(principalAmount);
        response.setInterest_amount(interestAmount);
        response.setRemaining_principal(loan.getRemainingPrincipal());
        response.setRepayment_method(request.getRepayment_method());
        response.setRepayment_date(repayment.getRepaymentDate());
        response.setLoan_status(loan.getLoanStatus());

        if ("closed".equals(loan.getLoanStatus())) {
            response.setClosed_date(loan.getClosedDate());
        } else {
            response.setNext_payment_date(loan.getNextPaymentDate());
            response.setNext_payment_amount(loan.getNextPaymentAmount());
        }

        return response;
    }

    /**
     * 生成还款计划明细
     */
    private List<RepaymentScheduleResponseDTO.RepaymentPlanItem> generateRepaymentPlan(entity.financing.Loan loan) {
        List<RepaymentScheduleResponseDTO.RepaymentPlanItem> repaymentPlan = new ArrayList<>();
        
        // 基础数据
        int totalPeriods = loan.getTermMonths();
        BigDecimal loanAmount = safeBigDecimal(loan.getLoanAmount());
        BigDecimal interestRate = safeBigDecimal(loan.getInterestRate());
        BigDecimal totalRepaymentAmount = safeBigDecimal(loan.getTotalRepaymentAmount());
        int currentPeriod = loan.getCurrentPeriod() > 0 ? loan.getCurrentPeriod() : 1;
        
        // 计算每期还款金额（等额本息）
        BigDecimal monthlyPayment = totalRepaymentAmount.divide(
            BigDecimal.valueOf(totalPeriods), 2, BigDecimal.ROUND_HALF_UP);
        
        // 计算月利率
        BigDecimal monthlyInterestRate = interestRate.divide(
            BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(12)), 6, BigDecimal.ROUND_HALF_UP);
        
        // 剩余本金初始值
        BigDecimal remainingPrincipal = loanAmount;
        
        // 生成每期计划
        Calendar calendar = Calendar.getInstance();
        if (loan.getFirstRepaymentDate() != null) {
            calendar.setTime(loan.getFirstRepaymentDate());
        } else {
            calendar.setTime(loan.getDisburseDate());
            calendar.add(Calendar.MONTH, 1); // 放款后一个月开始还款
        }
        
        for (int i = 1; i <= totalPeriods; i++) {
            RepaymentScheduleResponseDTO.RepaymentPlanItem item = new RepaymentScheduleResponseDTO.RepaymentPlanItem();
            
            // 设置期数和到期日期
            item.setPeriod(i);
            item.setDue_date(new java.sql.Date(calendar.getTimeInMillis()));
            
            // 计算本期利息
            BigDecimal interestAmount = remainingPrincipal.multiply(monthlyInterestRate);
            
            // 计算本期本金
            BigDecimal principalAmount = monthlyPayment.subtract(interestAmount);
            
            // 最后一期调整，确保本金完全还清
            if (i == totalPeriods) {
                principalAmount = remainingPrincipal;
                monthlyPayment = principalAmount.add(interestAmount);
            }
            
            item.setPrincipal(principalAmount);
            item.setInterest(interestAmount);
            item.setTotal(monthlyPayment);
            
            // 确定状态
            String status;
            if (i < currentPeriod) {
                status = "paid";
            } else if (i == currentPeriod) {
                // 检查是否逾期
                Date today = new Date(System.currentTimeMillis());
                if (item.getDue_date().before(today)) {
                    status = "overdue";
                } else {
                    status = "pending";
                }
            } else {
                status = "pending";
            }
            item.setStatus(status);
            
            repaymentPlan.add(item);
            
            // 更新剩余本金
            remainingPrincipal = remainingPrincipal.subtract(principalAmount);
            
            // 下一期日期
            calendar.add(Calendar.MONTH, 1);
        }
        
        return repaymentPlan;
    }

    // 私有辅助方法：计算应还金额（按日计息）
    private BigDecimal calculateDueAmount(entity.financing.Loan loan) {
        // 简化计算：假设等额本息
        return loan.getNextPaymentAmount() != null ? loan.getNextPaymentAmount() : BigDecimal.ZERO;
    }

    // 私有辅助方法：计算应还本金
    private BigDecimal calculatePrincipalAmount(entity.financing.Loan loan, BigDecimal dueAmount) {
        // 简化计算：假设等额本息
        BigDecimal interest = loan.getRemainingPrincipal()
                .multiply(loan.getInterestRate())
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
                .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
        return dueAmount.subtract(interest);
    }

    // 私有辅助方法：保存还款记录
    private void saveRepayment(entity.financing.Repayment repayment) throws SQLException {
        dbManager.saveRepayment(repayment);
    }

    // 私有辅助方法：还款后更新贷款状态
    private void updateLoanAfterRepayment(entity.financing.Loan loan, entity.financing.Repayment repayment,
                                          boolean isJointPartner, Long partnerFarmerId) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 安全处理 null 值
        BigDecimal currentPaidAmount = loan.getTotalPaidAmount() != null ? loan.getTotalPaidAmount() : BigDecimal.ZERO;
        BigDecimal currentPaidPrincipal = loan.getTotalPaidPrincipal() != null ? loan.getTotalPaidPrincipal() : BigDecimal.ZERO;
        BigDecimal currentPaidInterest = loan.getTotalPaidInterest() != null ? loan.getTotalPaidInterest() : BigDecimal.ZERO;
        BigDecimal currentRemainingPrincipal = loan.getRemainingPrincipal() != null ? loan.getRemainingPrincipal() : BigDecimal.ZERO;

        // 获取本次还款的本金和利息
        BigDecimal principalAmount = repayment.getPrincipalAmount() != null ? repayment.getPrincipalAmount() : BigDecimal.ZERO;
        BigDecimal interestAmount = repayment.getInterestAmount() != null ? repayment.getInterestAmount() : BigDecimal.ZERO;

        // 更新总还款金额
        BigDecimal newTotalPaidAmount = currentPaidAmount.add(repayment.getRepaymentAmount());
        loan.setTotalPaidAmount(newTotalPaidAmount);

        // 更新累计已还本金
        BigDecimal newTotalPaidPrincipal = currentPaidPrincipal.add(principalAmount);
        loan.setTotalPaidPrincipal(newTotalPaidPrincipal);

        // 更新累计已还利息
        BigDecimal newTotalPaidInterest = currentPaidInterest.add(interestAmount);
        loan.setTotalPaidInterest(newTotalPaidInterest);

        // 更新剩余本金
        BigDecimal newRemainingPrincipal = currentRemainingPrincipal.subtract(principalAmount);
        // 确保剩余本金不为负数
        if (newRemainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
            newRemainingPrincipal = BigDecimal.ZERO;
        }
        loan.setRemainingPrincipal(newRemainingPrincipal);

        // 检查是否逾期
        if (loan.getNextPaymentDate() != null) {
            java.sql.Date nextPaymentDate = loan.getNextPaymentDate();
            java.sql.Date currentDate = new java.sql.Date(now.getTime());

            if (currentDate.after(nextPaymentDate)) {
                // 计算逾期天数
                long diffInMillis = currentDate.getTime() - nextPaymentDate.getTime();
                int overdueDays = (int) (diffInMillis / (24 * 60 * 60 * 1000));

                // 更新逾期天数和逾期金额
                // 修复：Integer 是对象类型，可以为 null，所以可以进行 != null 比较
                Integer currentOverdueDaysObj = loan.getOverdueDays();
                int currentOverdueDays = (currentOverdueDaysObj != null) ? currentOverdueDaysObj : 0;
                int newOverdueDays = currentOverdueDays + overdueDays;
                loan.setOverdueDays(newOverdueDays);

                // 逾期费用：每天100元
                BigDecimal currentOverdueAmount = loan.getOverdueAmount() != null ? loan.getOverdueAmount() : BigDecimal.ZERO;
                BigDecimal newOverdueAmount = currentOverdueAmount.add(BigDecimal.valueOf(overdueDays * 100));
                loan.setOverdueAmount(newOverdueAmount);

                // 将逾期费用加入总应还金额
                BigDecimal currentTotalRepaymentAmount = loan.getTotalRepaymentAmount() != null ? loan.getTotalRepaymentAmount() : BigDecimal.ZERO;
                loan.setTotalRepaymentAmount(currentTotalRepaymentAmount.add(BigDecimal.valueOf(overdueDays * 100)));
            }
        }

        // 检查是否还清
        BigDecimal totalRepaymentAmount = loan.getTotalRepaymentAmount() != null ? loan.getTotalRepaymentAmount() : BigDecimal.ZERO;
        if (newTotalPaidAmount.compareTo(totalRepaymentAmount) >= 0) {
            // 贷款已还清
            loan.setLoanStatus("closed");
            loan.setClosedDate(now);
        } else {
            // 贷款未还清，更新下次还款日期和期数
            if (loan.getNextPaymentDate() != null) {
                // 下次还款日期加30天
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(loan.getNextPaymentDate());
                calendar.add(Calendar.DAY_OF_MONTH, 30);
                loan.setNextPaymentDate(new java.sql.Date(calendar.getTimeInMillis()));
            }

            // 期数加1
            // 修复：Integer 是对象类型，可以为 null，所以可以进行 != null 比较
            Integer currentPeriod = loan.getCurrentPeriod();
            int newPeriod = (currentPeriod != null ? currentPeriod : 0) + 1;
            loan.setCurrentPeriod(newPeriod);
        }

        loan.setUpdatedAt(now);

        // 更新数据库中的贷款信息
        dbManager.updateLoanAfterRepayment(loan);


        // 如果是联合贷款且还款人是合作伙伴，还需要更新joint_loans表
        if (loan.getIsJointLoan() && isJointPartner && partnerFarmerId != null) {
            dbManager.updateJointLoanPartnerRepayment(loan.getId(), partnerFarmerId, repayment.getRepaymentAmount());
        }

    }







    // 私有辅助方法：更新联合贷款合作伙伴的还款信息
    private void updateJointLoanPartnerRepayment(Long loanId, Long partnerFarmerId, BigDecimal repaymentAmount) throws SQLException {
        dbManager.updateJointLoanPartnerRepayment(loanId, partnerFarmerId, repaymentAmount);
    }



    // 添加辅助方法
    private entity.financing.LoanApplication getLoanApplicationById(String applicationId) throws SQLException {
        return dbManager.getLoanApplicationById(applicationId);
    }

    private void updateLoanApplicationStatus(String applicationId, String status, Long approvedBy,
                                             Timestamp approvedAt, BigDecimal approvedAmount) throws SQLException {
        dbManager.updateLoanApplicationStatus(applicationId, status, approvedBy, approvedAt, approvedAmount);
    }

    // 包含拒绝原因的重载方法
    private void updateLoanApplicationStatus(String applicationId, String status, Long approvedBy,
                                             Timestamp approvedAt, BigDecimal approvedAmount, String rejectReason) throws SQLException {
        if ("rejected".equals(status)) {
            dbManager.updateLoanApplicationRejection(applicationId, status, approvedBy, approvedAt, rejectReason);
        } else {
            dbManager.updateLoanApplicationStatus(applicationId, status, approvedBy, approvedAt, approvedAmount);
        }
    }

    private void updateLoanApplicationRejection(String applicationId, String status, Long approvedBy,
                                                Timestamp approvedAt, String rejectReason) throws SQLException {
        dbManager.updateLoanApplicationRejection(applicationId, status, approvedBy, approvedAt, rejectReason);
    }

    private long saveLoan(entity.financing.Loan loan) throws SQLException {
        return dbManager.saveLoan(loan);
    }

    private void saveJointLoan(entity.financing.JointLoan jointLoan) throws SQLException {
        dbManager.saveJointLoan(jointLoan);
    }

    private List<Map<String, Object>> getJointLoanPartnersByApplicationId(long loanApplicationId) throws SQLException {
        return dbManager.getJointLoanPartnersByApplicationId(loanApplicationId);
    }

    private void updateUserBalance(String uid, BigDecimal amount) throws SQLException {
        dbManager.updateUserBalance(uid, amount);
    }

    private void updateCreditLimitUsed(Long farmerId, BigDecimal usedAmount) throws SQLException {
        dbManager.updateCreditLimitUsed(farmerId, usedAmount);
    }

    // 私有辅助方法：验证单人贷款申请请求
    private void validateSingleLoanApplicationRequest(SingleLoanApplicationRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "手机号不能为空"));
        }

        if (request.getProduct_id() == null || request.getProduct_id().trim().isEmpty()) {
            errors.add(createError("product_id", "贷款产品ID不能为空"));
        }

        if (request.getApply_amount() == null || request.getApply_amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("apply_amount", "申请金额必须大于0且不超过产品最高额度"));
        }

        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            errors.add(createError("purpose", "贷款用途不能为空"));
        } else if (request.getPurpose().length() < 2 || request.getPurpose().length() > 200) {
            errors.add(createError("purpose", "贷款用途描述至少需要2个字，最多200个字"));
        }

        if (request.getRepayment_source() == null || request.getRepayment_source().trim().isEmpty()) {
            errors.add(createError("repayment_source", "还款来源说明不能为空"));
        } else if (request.getRepayment_source().length() < 2 || request.getRepayment_source().length() > 200) {
            errors.add(createError("repayment_source", "还款来源说明至少需要2个字，最多200个字"));
        }
    }

    // 私有辅助方法：验证联合贷款申请请求
    private void validateJointLoanApplicationRequest(JointLoanApplicationRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "发起农户手机号不能为空"));
        }

        if (request.getProduct_id() == null || request.getProduct_id().trim().isEmpty()) {
            errors.add(createError("product_id", "贷款产品ID不能为空"));
        }

        if (request.getApply_amount() == null || request.getApply_amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(createError("apply_amount", "申请总金额必须为正数"));
        }

        if (request.getPartner_phones() == null || request.getPartner_phones().isEmpty()) {
            errors.add(createError("partner_phones", "联合贷款伙伴手机号数组不能为空"));
        } else if (request.getPartner_phones().size() != 1) {
            errors.add(createError("partner_phones", "联合贷款必须有且仅有1个伙伴"));
        }

        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            errors.add(createError("purpose", "贷款用途不能为空"));
        }

        if (request.getRepayment_plan() == null || request.getRepayment_plan().trim().isEmpty()) {
            errors.add(createError("repayment_plan", "还款计划说明不能为空"));
        }

        if (request.getJoint_agreement() == null || !request.getJoint_agreement()) {
            errors.add(createError("joint_agreement", "必须同意联合贷款协议才能申请"));
        }
    }

    // 私有辅助方法：验证可联合农户请求
    private void validatePartnersRequest(PartnersRequestDTO request, List<Map<String, String>> errors) {
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            errors.add(createError("phone", "农户手机号不能为空"));
        }

        if (request.getMax_partners() != null && (request.getMax_partners() < 1 || request.getMax_partners() > 5)) {
            errors.add(createError("max_partners", "最大伙伴数量应在1-5之间"));
        }

        if (request.getMin_credit_limit() != null && request.getMin_credit_limit().compareTo(new BigDecimal("1000000")) > 0) {
            errors.add(createError("min_credit_limit", "最低额度要求不能超过100万元"));
        }

        // 验证exclude_phones格式
        if (request.getExclude_phones() != null) {
            for (String phone : request.getExclude_phones()) {
                if (phone == null || phone.trim().isEmpty() || !phone.matches("^1[3-9]\\d{9}$")) {
                    errors.add(createError("exclude_phones", "排除手机号格式不正确: " + phone));
                    break;
                }
            }
        }
    }



    // 私有辅助方法：根据产品ID获取贷款产品
    private entity.financing.LoanProduct getLoanProductById(String productId) throws SQLException {
        // 直接使用字符串ID查询，不需要转换为Long
        return dbManager.getLoanProductById(productId);
    }

    // 私有辅助方法：根据产品ID（Long类型）获取贷款产品
    private entity.financing.LoanProduct getLoanProductById(Long productId) throws SQLException {
        return dbManager.getLoanProductById(productId);
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
        List<String> userRoles = dbManager.getUserRole(uid);
        if (!userRoles.contains("bank")) {
            return null;
        }

        // 获取银行信息
        return dbManager.getBankInfoByUid(uid);
    }

    // 私有辅助方法：检查用户是否具有农户身份
    private Map<String, Object> checkUserFarmerRole(String uid) throws SQLException {
        // 检查用户是否具有农户身份
        List<String> userRoles = dbManager.getUserRole(uid);
        if (!userRoles.contains("farmer")) {
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

    /**
     * 发送联合贷款消息
     */
    public JointLoanMessageResponseDTO sendJointLoanMessage(JointLoanMessageRequestDTO request) {
        try {
            // 参数验证
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空");
            }
            if (request.getApplication_id() == null || request.getApplication_id().trim().isEmpty()) {
                throw new IllegalArgumentException("申请ID不能为空");
            }
            if (request.getReceiver_phone() == null || request.getReceiver_phone().trim().isEmpty()) {
                throw new IllegalArgumentException("接收者手机号不能为空");
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("消息内容不能为空");
            }

            // 检查用户是否存在
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 保存消息
            dbManager.saveJointLoanMessage(
                request.getApplication_id(),
                request.getPhone(),
                request.getReceiver_phone(),
                request.getContent()
            );

            // 获取消息列表
            List<Map<String, Object>> messages = dbManager.getJointLoanMessages(
                request.getApplication_id(),
                request.getPhone()
            );

            JointLoanMessageResponseDTO response = new JointLoanMessageResponseDTO();
            response.setApplication_id(request.getApplication_id());
            response.setMessages(messages);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("发送消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取联合贷款消息列表
     */
    public JointLoanMessageResponseDTO getJointLoanMessages(GetJointLoanMessagesRequestDTO request) {
        try {
            // 参数验证
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("手机号不能为空");
            }
            if (request.getApplication_id() == null || request.getApplication_id().trim().isEmpty()) {
                throw new IllegalArgumentException("申请ID不能为空");
            }

            // 检查用户是否存在
            User user = findUserByPhone(request.getPhone());
            if (user == null) {
                throw new IllegalArgumentException("用户认证失败，请检查手机号或重新登录");
            }

            // 获取消息列表
            List<Map<String, Object>> messages = dbManager.getJointLoanMessages(
                request.getApplication_id(),
                request.getPhone()
            );

            JointLoanMessageResponseDTO response = new JointLoanMessageResponseDTO();
            response.setApplication_id(request.getApplication_id());
            response.setMessages(messages);

            return response;
        } catch (Exception e) {
            throw new RuntimeException("获取消息失败: " + e.getMessage(), e);
        }
    }

    private entity.financing.CreditApplication getCreditApplicationById(String applicationId) throws SQLException {
        return dbManager.getCreditApplicationById(applicationId);
    }

    private void updateCreditApplicationStatus(String applicationId, String status, Long approvedBy,
                                             Timestamp approvedAt, BigDecimal approvedAmount) throws SQLException {
        dbManager.updateCreditApplicationStatus(applicationId, status, approvedBy, approvedAt, approvedAmount);
    }

    private void updateCreditApplicationRejection(String applicationId, String status, Long approvedBy,
                                                Timestamp approvedAt, String rejectReason) throws SQLException {
        dbManager.updateCreditApplicationRejection(applicationId, status, approvedBy, approvedAt, rejectReason);
    }

    private List<Map<String, Object>> getPendingCreditApplicationsList() throws SQLException {
        return dbManager.getPendingCreditApplicationsList();
    }

    private List<Map<String, Object>> getCreditApplicationsByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getCreditApplicationsByFarmerId(farmerId);
    }

    private List<Map<String, Object>> getPendingLoanApplicationsList() throws SQLException {
        return dbManager.getPendingLoanApplicationsList();
    }

    private List<Map<String, Object>> getApprovedLoanApplicationsList() throws SQLException {
        return dbManager.getApprovedLoanApplicationsList();
    }

    private List<Map<String, Object>> getLoanApplicationsByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getLoanApplicationsByFarmerId(farmerId);
    }

}
