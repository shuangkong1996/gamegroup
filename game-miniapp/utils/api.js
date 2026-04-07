// utils/api.js
// API 接口封装

const { get, post, del } = require('./request');

// 游戏相关
const gameApi = {
  // 获取游戏列表
  getList: () => get('/games'),

  // 获取游戏详情
  getDetail: (id) => get(`/games/${id}`)
};

// 匹配相关
const matchApi = {
  // 创建房间
  create: (gameId) => post('/match/create', { gameId }),

  // 加入房间
  join: (verifyCode) => post('/match/join', { verifyCode }),

  // 获取房间状态
  getStatus: (roomId) => get(`/match/status/${roomId}`),

  // 取消匹配
  cancel: (roomId) => del(`/match/${roomId}`)
};

// 用户相关
const userApi = {
  // 获取用户信息
  getInfo: () => get('/auth/user-info')
};

// 游戏对战相关
const gamePlayApi = {
  // 开始游戏
  start: (roomId) => post(`/gameplay/start/${roomId}`),

  // 获取游戏状态
  getStatus: (roomId) => get(`/gameplay/status/${roomId}`),

  // 出招
  makeChoice: (roomId, choice) => post('/gameplay/choice', { roomId, choice })
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

module.exports = {
  gameApi,
  matchApi,
  userApi,
  gamePlayApi,
  ledgerApi
};
