// utils/api.js
// API 接口封装

const { get, post, put } = require('./request');

// 语录相关
const quoteApi = {
  // 获取今日专属语录（需登录）
  getToday: () => get('/quote/today'),

  // 获取随机语录（无需登录）
  getRandom: () => get('/quote/random')
};

// 认证相关
const authApi = {
  // 微信登录
  wxLogin: (code) => post('/auth/wx-login', { code }),

  // 获取用户设置
  getSettings: () => get('/auth/settings'),

  // 更新用户设置
  updateSettings: (offWorkHour, offWorkMinute) => put('/auth/settings', { offWorkHour, offWorkMinute })
};

// 厕所时间相关
const toiletApi = {
  // 开始计时
  start: (monthlySalary) => post('/toilet/start', { monthlySalary }),

  // 结束计时
  stop: () => post('/toilet/stop'),

  // 获取当前会话状态
  getCurrent: () => get('/toilet/current'),

  // 获取历史记录
  getHistory: (page = 1, size = 20) => get(`/toilet/history?page=${page}&size=${size}`),

  // 获取统计数据
  getStatistics: () => get('/toilet/statistics')
};

// 记账相关
const ledgerApi = {
  // 获取账户列表（带余额）
  getAccounts: () => get('/ledger/accounts'),

  // 新增账户
  createAccount: (data) => post('/ledger/accounts', data),

  // 获取分类列表（按支出/收入分组）
  getCategories: () => get('/ledger/categories'),

  // 记一笔
  createRecord: (data) => post('/ledger/records', data),

  // 批量记账
  createBatchRecords: (data) => post('/ledger/records/batch', data),

  // 创建周期账规则（每月重复）
  createRecurringRule: (data) => post('/ledger/recurring-rules', data),

  // 删除记录
  deleteRecord: (id) => del(`/ledger/records/${id}`),

  // 获取记录列表（按天分组）
  queryRecords: (data) => get('/ledger/records', data),

  // 获取月度总览
  getOverviewStats: (data) => get('/ledger/stats/overview', data),

  // 获取每日收支趋势
  getTrendStats: (data) => get('/ledger/stats/trend', data),

  // 获取分类占比
  getCategoryStats: (data) => get('/ledger/stats/category', data)
};

// 房贷计算器（无需登录）
const mortgageApi = {
  getDefaults: () => get('/mortgage/defaults'),
  calculate: (data) => post('/mortgage/calculate', data)
};

module.exports = {
  quoteApi,
  authApi,
  toiletApi,
  ledgerApi,
  mortgageApi
};
