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

    // 添加生成唯一申请ID的方法
    private String generateUniqueApplicationId() {
        // 使用时间戳+随机数确保唯一性
        return "APP" + System.currentTimeMillis() + String.format("%03d", new Random().nextInt(1000));
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

            // 检查可用额度是否足够
            if (request.getApply_amount().compareTo(farmerCreditLimit.getAvailableLimit()) > 0) {
                throw new IllegalArgumentException("申请金额" + request.getApply_amount() + "元超过可用额度" +
                        farmerCreditLimit.getAvailableLimit() + "元，请先申请提高额度或减少申请金额");
            }

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

            // 计算发起者应承担的份额（平均分配）
            BigDecimal shareAmount = request.getApply_amount().divide(BigDecimal.valueOf(request.getPartner_phones().size() + 1), 2, BigDecimal.ROUND_HALF_UP);

            // 检查发起者额度是否足够
            if (shareAmount.compareTo(initiatorCreditLimit.getAvailableLimit()) > 0) {
                throw new IllegalArgumentException("您当前可用额度" + initiatorCreditLimit.getAvailableLimit() +
                        "元，不足以承担联合贷款" + shareAmount + "元的份额，请先申请提高额度");
            }

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

                // 检查伙伴是否有待审批的联合贷款申请
                // 这里需要实现检查逻辑
            }

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

    // 创建贷款申请记录
    private long createLoanApplication(String loanApplicationId, Long farmerId, Long productId, String applicationType,
                                       BigDecimal applyAmount, String purpose, String repaymentSource) throws SQLException {
        return dbManager.createLoanApplication(loanApplicationId, farmerId, productId, applicationType,
                applyAmount, purpose, repaymentSource);
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
        } else if (request.getPartner_phones().size() < 2 || request.getPartner_phones().size() > 5) {
            errors.add(createError("partner_phones", "联合贷款至少需要2个伙伴，最多5个伙伴"));
        }

        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            errors.add(createError("purpose", "贷款用途不能为空"));
        } else if (request.getPurpose().length() < 2 || request.getPurpose().length() > 200) {
            errors.add(createError("purpose", "贷款用途描述至少需要2个字，最多200个字"));
        }

        if (request.getRepayment_plan() == null || request.getRepayment_plan().trim().isEmpty()) {
            errors.add(createError("repayment_plan", "还款计划说明不能为空"));
        } else if (request.getRepayment_plan().length() < 2 || request.getRepayment_plan().length() > 500) {
            errors.add(createError("repayment_plan", "还款计划说明至少需要2个字，最多500个字"));
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
