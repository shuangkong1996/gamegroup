# 个人记账功能开发规划 (Ledger Feature Plan)

## 1. 项目概述
在现有个人网站中集成全功能的记账模块，支持多维度（收支、分类、账户、时间）统计，并确保移动端和PC端的良好体验。

## 2. 核心业务模型与数据库设计 (Schema)

参考成熟记账软件，采用 **复式记账的简化版** 逻辑，核心引入“账户(Account)”概念。

### 2.1 维度分析
1.  **收支类型 (Type)**: 支出、收入、(转账 - 进阶期实现)。
2.  **分类 (Category)**: 消费的具体的类目（如：餐饮、交通）。
3.  **账户 (Account)**: 资金的来源/去向（如：微信钱包、招行储蓄卡、现金）。**这是统计净资产的关键维度。**
4.  **时间 (Time)**: 交易发生的实际时间。

### 2.2 数据库表设计 (SQL)

```sql
-- 1. 资产账户表 (Wallet/Account)
-- 用于统计：我到底还有多少钱？钱都在哪？
CREATE TABLE t_ledger_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '账户名称，如：微信、招行',
    type VARCHAR(20) DEFAULT 'General' COMMENT '类型：Cash, Bank, Credit, Alipay, Wechat',
    balance DECIMAL(12, 2) DEFAULT 0.00 COMMENT '当前余额(可由记录计算得出，也可冗余存储)',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='记账-资产账户表';

-- 2. 记账分类表 (Category)
-- 用于统计：钱花哪儿了？
CREATE TABLE t_ledger_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID，0表示系统默认',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    type INT NOT NULL COMMENT '类型：1-支出, 2-收入',
    icon VARCHAR(100) COMMENT '图标标识',
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='记账-分类表';

-- 3. 交易记录表 (Record)
-- 核心流水
CREATE TABLE t_ledger_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL COMMENT '关联账户ID',
    category_id BIGINT NOT NULL COMMENT '关联分类ID',
    amount DECIMAL(12, 2) NOT NULL COMMENT '金额',
    type INT NOT NULL COMMENT '类型：1-支出, 2-收入',
    record_time DATETIME NOT NULL COMMENT '记账时间',
    remark VARCHAR(255) COMMENT '备注',
    
    -- 冗余字段（方便快速查询显示，避免过多联表）
    category_name VARCHAR(50), 
    category_icon VARCHAR(100),
    account_name VARCHAR(50),
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, record_time),
    INDEX idx_account (account_id)
) COMMENT='记账-记录表';
```

## 3. 接口规划 (API)

### Account (账户)
*   `GET /ledger/accounts`: 获取账户列表（带余额）。
*   `POST /ledger/accounts`: 新增账户。

### Category (分类)
*   `GET /ledger/categories`: 获取分类列表（按支出/收入分组）。

### Record (记录)
*   `POST /ledger/records`: 记一笔。
    *   *逻辑*: 插入记录的同时，更新 `t_ledger_account` 的余额。
*   `DELETE /ledger/records/{id}`: 删除记录。
    *   *逻辑*: 删除记录，回滚账户余额。
*   `GET /ledger/records`: 获取列表。
    *   *参数*: `month` (yyyy-MM), `accountId` (可选), `categoryId` (可选)。
    *   *响应*: 按日期分组的数据结构，方便前端渲染。

### Stats (统计)
*   `GET /ledger/stats/trend`: 每日收支趋势（折线图）。
*   `GET /ledger/stats/category`: 分类占比（饼图）。
*   `GET /ledger/stats/overview`: 月度总览（总收入、总支出、结余）。

## 4. 前端开发策略 (Mobile Compatible)

为了兼容手机端，我们将采用 **响应式布局 (Responsive Design)** 策略，而不是开发两套代码。

### 4.1 布局调整
*   **PC端**:
    *   左侧/顶部为筛选器。
    *   中间为大表格 (`el-table`)。
    *   右侧为统计图表。
*   **Mobile端** (Media Query `< 768px`):
    *   隐藏复杂的侧边栏。
    *   **列表页**: 不使用 `el-table`，改用 `v-for` 渲染的 **卡片流 (Card List)**。
        *   左边是图标，中间是分类和备注，右边是金额。
    *   **记账操作**: 使用底部悬浮按钮 (FAB)，点击后弹出全屏 Drawer 或 BottomSheet，而不是居中的 Dialog。

### 4.2 交互优化
*   **记账弹窗**:
    *   数字键盘区域要大，方便手指点击。
    *   分类选择采用“图标九宫格”布局，而不是下拉菜单。

## 5. 开发步骤 (Step-by-Step)

### Phase 1: 基础设施 (Backend)
1.  在数据库执行 SQL 建表。
2.  生成 Entity (`LedgerAccount`, `LedgerCategory`, `LedgerRecord`)。
3.  生成 Repository。
4.  初始化一些默认数据（默认分类：餐饮、交通；默认账户：现金）。

### Phase 2: 核心记账 (Backend + Frontend)
1.  **后端**: 实现 `LedgerService` 的 `createRecord` (含余额变动逻辑) 和 `queryRecords`。
2.  **前端**:
    *   创建 `LedgerList.vue`。
    *   实现响应式列表：PC端 Table，手机端 List。
    *   实现“记一笔”组件：支持选择账户、分类、金额、时间。

### Phase 3: 统计与优化 (Visuals)
1.  **后端**: 实现统计接口 (SQL Aggregation)。
2.  **前端**: 集成 ECharts。
    *   手机端：图表单列展示。
    *   PC端：图表网格展示。

---
*Ready to start Phase 1.*
