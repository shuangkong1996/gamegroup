// utils/api.js
// API 接口封装

const { request, get, post, put, upload } = require('./request');

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
  updateSettings: (offWorkHour, offWorkMinute, city) => {
    if (typeof offWorkHour === 'object' && offWorkHour !== null) {
      return put('/auth/settings', offWorkHour);
    }
    return put('/auth/settings', { offWorkHour, offWorkMinute, city });
  },

  // 获取用户形象资料
  getProfile: () => get('/auth/profile'),

  // 更新用户形象资料
  updateProfile: (data) => put('/auth/profile', data)
};

const fileApi = {
  uploadImage: (filePath) => upload('/v1/file/upload', filePath)
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
  deleteRecord: (id) => requestDelete(`/ledger/records/${id}`),

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

// 小区对比助手（无需登录）
const communityApi = {
  search: (keyword, city) => get(`/community/search?keyword=${encodeURIComponent(keyword || '')}&city=${encodeURIComponent(city || '')}`),
  compare: (ids, city) => post('/community/compare', { ids, city })
};

// 我的衣柜
const wardrobeApi = {
  uploadItem: (filePath) => upload('/wardrobe/items/upload', filePath),
  getOverview: () => get('/wardrobe/overview'),
  getCategories: () => get('/wardrobe/categories'),
  getItems: (recognizeStatus) => {
    if (!recognizeStatus) {
      return get('/wardrobe/items');
    }
    return get(`/wardrobe/items?recognizeStatus=${encodeURIComponent(recognizeStatus)}`);
  },
  getItemDetail: (id) => get(`/wardrobe/items/${id}`),
  updateItem: (id, data) => put(`/wardrobe/items/${id}`, data),
  deleteItem: (id) => requestDelete(`/wardrobe/items/${id}`),
  getDailyOutfit: (targetDate) => get(targetDate ? `/outfits/daily?targetDate=${encodeURIComponent(targetDate)}` : '/outfits/daily'),
  refreshDailyOutfit: (targetDate) => post(targetDate ? `/outfits/daily/refresh?targetDate=${encodeURIComponent(targetDate)}` : '/outfits/daily/refresh'),
  adoptOutfit: (planId, data) => post(`/outfits/${planId}/adopt`, data || {}),
  getOutfitRecords: () => get('/outfits/records')
};

const outfitRenderApi = {
  createTask: (data) => post('/outfit-renders/tasks', data),
  getTasks: () => get('/outfit-renders/tasks'),
  getLatestTask: () => get('/outfit-renders/tasks/latest'),
  getTask: (taskId) => get(`/outfit-renders/tasks/${taskId}`),
  adoptTask: (taskId, data) => post(`/outfit-renders/tasks/${taskId}/adopt`, data || {})
};

const mahjongApi = {
  analyzePhoto: (filePath) => upload('/mahjong/advice', filePath),
  analyzeTiles: (tiles) => post('/mahjong/analyze-tiles', { tiles })
};

function requestDelete(url) {
  return request({
    url,
    method: 'DELETE'
  });
}

module.exports = {
  quoteApi,
  authApi,
  fileApi,
  toiletApi,
  ledgerApi,
  mortgageApi,
  communityApi,
  wardrobeApi,
  outfitRenderApi,
  mahjongApi
};
