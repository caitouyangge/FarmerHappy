# 融资模块

融资模块提供农户贷款和银行管理功能。

## 功能概述

### 农户功能

1. **信用额度管理**
   - 查询可用贷款额度
   - 申请贷款额度（需提交证明材料）

2. **贷款产品**
   - 浏览可申请的贷款产品
   - 查看产品详情（利率、期限、还款方式等）

3. **贷款申请**
   - 申请单人贷款
   - 申请联合贷款（与其他农户联合申请）

4. **联合农户**
   - 浏览可联合贷款的农户
   - 筛选符合条件的农户（按信用额度等）

5. **还款管理**
   - 查看还款计划
   - 查看还款明细

### 银行功能

1. **贷款产品管理**
   - 发布贷款产品
   - 设置产品参数（额度、利率、期限、还款方式等）

2. **贷款审批**
   - 审批贷款申请
   - 批准或拒绝申请
   - 设置批准金额或拒绝原因

3. **放款操作**
   - 对已审批通过的贷款进行放款
   - 设置放款方式和还款日期

## 文件结构

```
financing/
├── Financing.vue              # 主页面（根据用户类型显示不同功能）
├── components/                # 子组件
│   ├── CreditLimitApplicationModal.vue    # 申请贷款额度
│   ├── LoanProductListModal.vue           # 贷款产品列表
│   ├── SingleLoanApplicationModal.vue     # 单人贷款申请
│   ├── JointLoanApplicationModal.vue      # 联合贷款申请
│   ├── JointPartnersModal.vue             # 可联合农户
│   ├── RepaymentScheduleModal.vue         # 还款计划
│   ├── LoanProductPublishModal.vue        # 发布贷款产品（银行）
│   ├── LoanApprovalModal.vue              # 审批贷款（银行）
│   └── LoanDisbursementModal.vue          # 放款操作（银行）
└── README.md                 # 本文件
```

## 路由配置

融资模块路由：`/loan`

访问权限：需要登录认证（`meta: { requiresAuth: true }`）

## 使用方法

### 农户使用

1. 从首页点击"贷款"模块进入融资页面
2. 查看信用额度概览
3. 根据需要选择相应功能：
   - 如需提高额度，点击"申请贷款额度"
   - 如需申请贷款，点击"查看贷款产品"选择产品后申请
   - 如需联合贷款，先"寻找联合伙伴"，然后申请联合贷款
   - 查看"还款计划"了解还款详情

### 银行使用

1. 从首页点击"贷款"模块进入融资页面
2. 选择相应功能：
   - 发布新的贷款产品
   - 审批农户提交的贷款申请
   - 对已审批的贷款进行放款

## API 接口

所有API接口都在 `../api/financing.js` 中定义：

- `financingService.getCreditLimit()` - 查询信用额度
- `financingService.applyForCreditLimit()` - 申请信用额度
- `financingService.getAvailableLoanProducts()` - 查询可申请的贷款产品
- `financingService.applyForSingleLoan()` - 申请单人贷款
- `financingService.applyForJointLoan()` - 申请联合贷款
- `financingService.getJointPartners()` - 获取可联合农户
- `financingService.getRepaymentSchedule()` - 获取还款计划
- `financingService.publishLoanProduct()` - 发布贷款产品（银行）
- `financingService.approveLoan()` - 审批贷款（银行）
- `financingService.disburseLoan()` - 放款操作（银行）

## 注意事项

1. **用户类型限制**
   - 农户可以访问所有农户功能
   - 银行可以访问所有银行功能
   - 其他用户类型无法访问融资模块

2. **数据验证**
   - 所有表单都有前端验证
   - 申请金额必须在产品规定的范围内
   - 联合贷款需要2-5个伙伴

3. **错误处理**
   - 所有API调用都有错误处理
   - 失败时会显示错误提示信息

4. **图片上传**
   - 申请额度时的证明材料图片上传功能需要后端支持
   - 目前使用占位URL，实际使用需要集成文件上传服务

## 样式说明

所有组件使用统一的主题样式（`../assets/styles/theme.css`），包括：
- 主色调：紫色（`#6B46C1`）
- 响应式设计，支持移动端
- 统一的模态框样式和表单样式

