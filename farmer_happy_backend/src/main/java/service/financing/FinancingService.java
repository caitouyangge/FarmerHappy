// src/main/java/service/financing/FinancingService.java
package service.financing;

import dto.bank.*;
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

            // 预扣额度 - 在创建贷款申请记录前扣除
            preDeductCreditLimit(((Long) farmerInfo.get("farmer_id")).longValue(), shareAmount);

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

            // 计算每个伙伴的份额（包括发起人，总共人数为伙伴数量+1）
            int totalParticipants = request.getPartner_phones().size() + 1;
            BigDecimal partnerShareRatio = BigDecimal.valueOf(100.0).divide(BigDecimal.valueOf(totalParticipants), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal partnerShareAmount = request.getApply_amount().divide(BigDecimal.valueOf(totalParticipants), 2, BigDecimal.ROUND_HALF_UP);

            // 保存联合贷款伙伴记录
            List<Map<String, Object>> partnerRecords = new ArrayList<>();
            for (String partnerPhone : request.getPartner_phones()) {
                User partnerUser = findUserByPhone(partnerPhone);
                Map<String, Object> partnerFarmerInfo = checkUserFarmerRole(partnerUser.getUid());

                Map<String, Object> partnerRecord = new HashMap<>();
                partnerRecord.put("partner_farmer_id", ((Long) partnerFarmerInfo.get("farmer_id")).longValue());
                partnerRecord.put("partner_share_ratio", partnerShareRatio);
                partnerRecord.put("partner_share_amount", partnerShareAmount);
                partnerRecords.add(partnerRecord);
            }

            // 保存联合贷款伙伴记录到数据库
            saveJointLoanApplicationPartners(applicationRecordId, partnerRecords);

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
            if (!"pending".equals(loanApplication.getStatus()) &&
                    !"pending_partners".equals(loanApplication.getStatus())) {
                throw new IllegalArgumentException("该申请状态为" + loanApplication.getStatus() + "，不能重复审批");
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
                restoreCreditLimit(loanApplication.getFarmerId(), loanApplication.getApplyAmount());

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
            loan.setDisburseMethod(request.getDisburse_method());
            loan.setDisburseDate(new Timestamp(System.currentTimeMillis()));
            loan.setFirstRepaymentDate(request.getFirst_repayment_date());
            loan.setLoanAccount(request.getLoan_account());
            loan.setDisburseRemarks(request.getRemarks());
            loan.setLoanStatus("active");
            loan.setApprovedBy(((Long) bankInfo.get("bank_id")).longValue());
            loan.setApprovedAt(new Timestamp(System.currentTimeMillis()));
            loan.setTotalRepaymentAmount(totalRepaymentAmount);
            loan.setRemainingPrincipal(request.getDisburse_amount());
            loan.setNextPaymentDate(request.getFirst_repayment_date());
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
            response.setDisburse_method(request.getDisburse_method());
            response.setDisburse_date(new Timestamp(System.currentTimeMillis()));
            response.setFirst_repayment_date(request.getFirst_repayment_date());
            response.setLoan_status("active");
            response.setTotal_repayment_amount(totalRepaymentAmount);
            response.setMonthly_payment(monthlyPayment);
            response.setNext_payment_date(request.getFirst_repayment_date());

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

        // 更新主借款人的信用额度
        updateCreditLimitUsed(loanApplication.getFarmerId(), loan.getLoanAmount());
    }

    /**
     * 处理联合贷款放款
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

        // 给主借款人账户增加资金
        updateUserBalance(mainFarmerUid, loan.getDisburseAmount());

        // 更新主借款人的信用额度
        updateCreditLimitUsed(loanApplication.getFarmerId(), loan.getLoanAmount());

        // 计算每个参与者的份额（包括主申请人在内）
        int totalParticipants = partners.size() + 1; // 合作伙伴数量 + 主申请人
        BigDecimal partnerShareRatio = BigDecimal.valueOf(100.0).divide(BigDecimal.valueOf(totalParticipants), 2, BigDecimal.ROUND_HALF_UP);

        // 主申请人的份额金额基于贷款总金额计算
        BigDecimal mainShareAmount = loan.getLoanAmount().multiply(partnerShareRatio).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

        // 为主申请人创建联合贷款记录（将主申请人也视为合作伙伴之一）
        BigDecimal mainPrincipal = mainShareAmount;
        BigDecimal mainInterest = calculateTotalInterest(mainPrincipal, loan.getInterestRate(), loan.getTermMonths());
        BigDecimal mainTotalRepayment = mainPrincipal.add(mainInterest);

        entity.financing.JointLoan mainJointLoan = new entity.financing.JointLoan();
        mainJointLoan.setLoanId(loanRecordId);
        mainJointLoan.setPartnerFarmerId(loanApplication.getFarmerId()); // 主申请人的农户ID
        mainJointLoan.setPartnerShareRatio(partnerShareRatio); // 使用平均分配的份额比例
        mainJointLoan.setPartnerShareAmount(mainPrincipal);
        mainJointLoan.setPartnerPrincipal(mainPrincipal);
        mainJointLoan.setPartnerInterest(mainInterest);
        mainJointLoan.setPartnerTotalRepayment(mainTotalRepayment);
        mainJointLoan.setPartnerPaidAmount(BigDecimal.ZERO);
        mainJointLoan.setPartnerRemainingPrincipal(mainPrincipal);
        mainJointLoan.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        mainJointLoan.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        saveJointLoan(mainJointLoan);

        // 为每个联合贷款伙伴创建记录并处理资金
        for (Map<String, Object> partner : partners) {
            Long partnerFarmerId = (Long) partner.get("partner_farmer_id");
            // 根据贷款总金额和份额比例计算伙伴的份额金额
            BigDecimal partnerShareRatioFromDB = (BigDecimal) partner.get("partner_share_ratio");
            BigDecimal partnerShareAmount = loan.getLoanAmount().multiply(partnerShareRatioFromDB).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

            // 获取伙伴用户ID
            String partnerUid = getFarmerUidByFarmerId(partnerFarmerId);
            if (partnerUid == null) {
                throw new SQLException("未找到联合贷款伙伴信息: " + partner.get("phone"));
            }

            // 给伙伴账户增加资金
            updateUserBalance(partnerUid, partnerShareAmount);

            // 更新伙伴的信用额度
            updateCreditLimitUsed(partnerFarmerId, partnerShareAmount);

            // 计算伙伴的利息和总还款金额（与主贷款人使用相同的利率和期限）
            BigDecimal partnerPrincipal = partnerShareAmount;
            BigDecimal partnerInterest = calculateTotalInterest(partnerPrincipal, loan.getInterestRate(), loan.getTermMonths());
            BigDecimal partnerTotalRepayment = partnerPrincipal.add(partnerInterest);

            // 创建联合贷款记录
            entity.financing.JointLoan jointLoan = new entity.financing.JointLoan();
            jointLoan.setLoanId(loanRecordId);
            jointLoan.setPartnerFarmerId(partnerFarmerId);
            jointLoan.setPartnerShareRatio(partnerShareRatioFromDB);
            jointLoan.setPartnerShareAmount(partnerShareAmount);
            jointLoan.setPartnerPrincipal(partnerPrincipal);
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

        if (request.getDisburse_method() == null || request.getDisburse_method().trim().isEmpty()) {
            errors.add(createError("disburse_method", "放款方式不能为空"));
        } else if (!Arrays.asList("bank_transfer", "cash", "check").contains(request.getDisburse_method())) {
            errors.add(createError("disburse_method", "放款方式必须是bank_transfer、cash或check"));
        }

        if (request.getFirst_repayment_date() == null) {
            errors.add(createError("first_repayment_date", "首次还款日期不能为空"));
        } else {
            // 检查首次还款日期是否在放款日期之后至少15天
            java.util.Date disburseDate = new java.util.Date();
            java.util.Date firstRepaymentDate = new java.util.Date(request.getFirst_repayment_date().getTime());
            long diffInMillies = Math.abs(firstRepaymentDate.getTime() - disburseDate.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            if (diffInDays < 15) {
                errors.add(createError("first_repayment_date", "首次还款日期不能早于或等于放款日期，必须至少间隔15天"));
            }
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
            errors.add(createError("repayment_method", "还款方式必须是 normal、partial 或 advance"));
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

        // 创建虚拟还款记录（不保存到数据库）
        entity.financing.Repayment repayment = new entity.financing.Repayment();
        repayment.setRepaymentAmount(request.getRepayment_amount());
        repayment.setRepaymentMethod(request.getRepayment_method());
        repayment.setRepaymentDate(new Timestamp(System.currentTimeMillis()));

        // 更新贷款状态
        updateLoanAfterRepayment(loan, repayment, isJointPartner, partnerFarmerId);

        // 构造响应
        RepaymentResponseDTO response = new RepaymentResponseDTO();
        response.setRepayment_id("REP" + System.currentTimeMillis()); // 生成虚拟ID
        response.setLoan_id(loan.getLoanId());
        response.setRepayment_amount(request.getRepayment_amount());
        response.setPrincipal_amount(BigDecimal.ZERO);
        response.setInterest_amount(BigDecimal.ZERO);
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

        // 安全处理 null 值，确保 totalPaidAmount 不为 null
        BigDecimal currentPaidAmount = loan.getTotalPaidAmount() != null ? loan.getTotalPaidAmount() : BigDecimal.ZERO;

        // 更新总还款金额
        BigDecimal newTotalPaidAmount = currentPaidAmount.add(repayment.getRepaymentAmount());
        loan.setTotalPaidAmount(newTotalPaidAmount);

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

    private String getFarmerUidByFarmerId(Long farmerId) throws SQLException {
        return dbManager.getFarmerUidByFarmerId(farmerId);
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
