// pages/ledger/ledger.js
// 记账台账：列表/记一笔/删除（接入 game-server 的 /ledger/*）

const { ledgerApi } = require('../../utils/api');
const app = getApp();

Page({
  data: {
    // 顶部筛选
    month: '',
    monthPickValue: '',
    accounts: [],
    allCategories: [],
    expenseCategories: [],
    incomeCategories: [],
    baseLoaded: false,
    accountFilterRange: ['全部账户'],
    categoryFilterRange: ['全部分类'],
    accountFilterIndex: 0,
    categoryFilterIndex: 0,
    selectedAccountId: null,
    selectedCategoryId: null,
    accountPickerRange: ['请选择账户'],
    categoryPickerRange: ['请选择分类'],
    recordAccountIndex: 0,
    recordCategoryIndex: 0,

    // 列表
    loading: true,
    loadingList: false,
    groupedRecords: [],
    page: 1,
    size: 20,
    total: 0,
    hasMore: false,

    // 统计
    overview: {
      totalIncome: 0,
      totalExpense: 0,
      balance: 0
    },
    expenseCategoryStats: [],
    trendLast7: [],

    // 弹窗状态
    openRecordModal: false,
    openAccountModal: false,
    openBatchModal: false,

    // 记一笔表单（骨架）
    recordForm: {
      type: '1', // 1=支出, 2=收入（radio-group value 为字符串）
      accountId: null,
      categoryId: null,
      amount: '',
      // 周期账相关
      isRecurring: false,
      // 生成规则固定每月 1 号 09:00，这里只让用户选择开始/结束月份
      recurringStartMonth: '', // yyyy-MM
      recurringStartMonthPickValue: '', // yyyy-MM-dd（picker需要完整日期）
      recurringEndMode: 'forever', // forever | end
      recurringEndMonth: '', // yyyy-MM
      recurringEndMonthPickValue: '', // yyyy-MM-dd

      // 普通记账相关
      recordDate: '', // yyyy-MM-dd
      recordTime: '12:00', // HH:mm
      remark: ''
    },

    // 新增账户表单（骨架）
    accountForm: {
      name: '',
      type: 'General'
    },

    // 批量记账表单（骨架）
    batchForm: {
      type: 1,
      accountId: null,
      categoryId: null,
      amount: '',
      month: '',
      recordDates: [],
      remark: ''
    },

    // 批量记账弹窗用的选择数据
    batchMonthPickValue: '',
    batchWorkdays: [],
    batchWeekHeaders: ['一', '二', '三', '四', '五', '六', '日'],
    batchCalendarCells: [],
    batchAccountPickerRange: ['请选择账户'],
    batchCategoryPickerRange: ['请选择分类'],
    // 批量记账预览（总金额）
    batchTotalAmount: '0.00',

    // 弹窗滚动区高度（px），避免 scroll-view 与底部按钮重叠
    modalScrollHeightPx: 420
  },

  onLoad() {
    const now = new Date();
    const ym = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
    const ymd = `${ym}-${`${now.getDate()}`.padStart(2, '0')}`;

    this.setData({
      month: ym,
      monthPickValue: ymd,
      loading: true,
      modalScrollHeightPx: this.computeModalScrollHeightPx()
    });

    this.init();
  },

  computeModalScrollHeightPx() {
    // 86vh 弹窗高度 - (标题/间距 + 底部按钮区 + 安全区) 约等于滚动区高度
    // 用 px 明确指定，避免 scroll-view 在 flex 下不按预期布局
    const info = wx.getSystemInfoSync();
    const vh = info.windowHeight * 0.86;
    const safeBottom = (info.safeArea && info.safeArea.bottom)
      ? Math.max(0, info.windowHeight - info.safeArea.bottom)
      : 0;

    const headerAndPadding = 120; // 标题+padding 粗略
    const actions = 110; // 底部操作区高度粗略
    const extra = 12;
    const h = Math.floor(vh - headerAndPadding - actions - safeBottom - extra);
    return Math.max(260, h);
  },

  async init() {
    try {
      const ok = await this.ensureLoggedIn();
      if (!ok) {
        wx.showToast({ title: '登录中，请稍后重试', icon: 'none' });
        this.setData({ loading: false });
        return;
      }

      await this.ensureBaseDataLoaded();
      await this.refreshAll();
    } catch (err) {
      console.error('加载记账台账失败', err);
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async ensureLoggedIn() {
    if (app.isLoggedIn()) return true;

    // 等待静默登录完成（最多 5s）
    const start = Date.now();
    return new Promise((resolve) => {
      const t = setInterval(() => {
        if (app.isLoggedIn() || Date.now() - start > 5000) {
          clearInterval(t);
          resolve(app.isLoggedIn());
        }
      }, 200);
    });
  },

  // 月份选择
  onMonthPick(e) {
    const value = e.detail.value || '';
    const month = value.slice(0, 7);
    this.setData({
      month,
      monthPickValue: value,
      page: 1,
      groupedRecords: [],
      total: 0,
      hasMore: false
    });

    this.refreshRecords();
  },

  // 筛选
  onAccountFilterChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx)) return;

    let selectedAccountId = null;
    if (idx > 0) {
      const account = this.data.accounts[idx - 1];
      selectedAccountId = account ? account.id : null;
    }
    this.setData({
      accountFilterIndex: idx,
      selectedAccountId,
      page: 1,
      groupedRecords: [],
      total: 0,
      hasMore: false
    });
    this.refreshRecords();
  },

  onCategoryFilterChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx)) return;

    let selectedCategoryId = null;
    if (idx > 0) {
      const category = this.data.allCategories[idx - 1];
      selectedCategoryId = category ? category.id : null;
    }
    this.setData({
      categoryFilterIndex: idx,
      selectedCategoryId,
      page: 1,
      groupedRecords: [],
      total: 0,
      hasMore: false
    });
    this.refreshRecords();
  },

  // 选择账户（记一笔）
  onRecordAccountChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx) || !this.data.accounts.length) return;
    const account = this.data.accounts[idx];
    if (!account) return;
    this.setData({
      recordAccountIndex: idx,
      'recordForm.accountId': account.id
    });
  },

  // 选择分类（记一笔）
  onRecordCategoryChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx)) return;

    const type = Number(this.data.recordForm.type);
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    if (!categories.length) return;

    const category = categories[idx];
    if (!category) return;
    this.setData({
      recordCategoryIndex: idx,
      'recordForm.categoryId': category.id
    });
  },

  async loadMore() {
    if (!this.data.hasMore || this.data.loadingList) return;
    this.setData({ loadingList: true });
    try {
      const nextPage = (this.data.page || 1) + 1;
      this.setData({ page: nextPage });
      await this.loadRecords(true);
    } finally {
      this.setData({ loadingList: false });
    }
  },

  // 删除记录
  onDeleteRecord(e) {
    const id = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : null;
    const recordId = parseInt(id, 10);
    if (Number.isNaN(recordId)) return;

    wx.showModal({
      title: '提示',
      content: '确定删除这条记录吗？',
      confirmText: '删除',
      confirmColor: '#ef4444',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await ledgerApi.deleteRecord(recordId);
          wx.showToast({ title: '删除成功', icon: 'success' });
          this.setData({ page: 1, groupedRecords: [], total: 0, hasMore: false });
          await this.refreshRecords();
        } catch (err) {
          console.error('删除记录失败', err);
          wx.showToast({ title: '删除失败', icon: 'none' });
        }
      }
    });
  },

  // 打开/关闭弹窗
  openRecordModal() {
    const now = new Date();
    const recordDate = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}`;
    const recordTime = `${`${now.getHours()}`.padStart(2, '0')}:${`${now.getMinutes()}`.padStart(2, '0')}`;
    const startMonth = this.data.month || `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
    const startMonthPickValue = `${startMonth}-01`;

    // 每次打开都默认“支出”
    const type = '1';
    const categories = this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;
    const accountId = this.data.accounts.length ? this.data.accounts[0].id : null;

    this.setData({
      openRecordModal: true,
      recordAccountIndex: 0,
      recordCategoryIndex: 0,
      modalScrollHeightPx: this.computeModalScrollHeightPx(),
      'recordForm.type': type,
      'recordForm.amount': '',
      'recordForm.remark': '',
      'recordForm.isRecurring': false,
      'recordForm.recurringStartMonth': startMonth,
      'recordForm.recurringStartMonthPickValue': startMonthPickValue,
      'recordForm.recurringEndMode': 'forever',
      'recordForm.recurringEndMonth': '',
      'recordForm.recurringEndMonthPickValue': startMonthPickValue,
      'recordForm.recordDate': recordDate,
      'recordForm.recordTime': recordTime,
      'recordForm.accountId': accountId,
      'recordForm.categoryId': categoryId,
      categoryPickerRange: categories.length ? categories.map(c => c.name || '分类') : ['请选择分类']
    });
  },

  closeRecordModal() {
    this.setData({ openRecordModal: false });
  },

  onRecordTypeChange(e) {
    const type = parseInt(e.detail.value, 10);
    const safeType = Number.isNaN(type) ? 1 : type;

    const categories = safeType === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;
    const categoryPickerRange = categories.length ? categories.map(c => c.name) : ['请选择分类'];

    this.setData({
      'recordForm.type': String(safeType),
      'recordForm.categoryId': categoryId,
      recordCategoryIndex: 0,
      categoryPickerRange
    });
  },

  onRecordAmountChange(e) {
    this.setData({ 'recordForm.amount': e.detail.value });
  },

  onRecordRemarkChange(e) {
    this.setData({ 'recordForm.remark': e.detail.value });
  },

  onRecordDateChange(e) {
    this.setData({ 'recordForm.recordDate': e.detail.value });
  },

  onRecordTimeChange(e) {
    this.setData({ 'recordForm.recordTime': e.detail.value });
  },

  // 周期账：开关
  onRecordRecurringToggle(e) {
    this.setData({ 'recordForm.isRecurring': !!e.detail.value });
  },

  // 周期账：开始月份
  onRecurringStartMonthChange(e) {
    const value = e.detail.value || '';
    const month = value.slice(0, 7);
    this.setData({
      'recordForm.recurringStartMonth': month,
      'recordForm.recurringStartMonthPickValue': value
    });
  },

  // 周期账：结束策略
  onRecurringEndModeChange(e) {
    const mode = e.detail.value;
    const safeMode = mode === 'end' ? 'end' : 'forever';
    const currentEndMonth = (this.data.recordForm && this.data.recordForm.recurringEndMonth) ? this.data.recordForm.recurringEndMonth : '';
    const currentEndPickValue = (this.data.recordForm && this.data.recordForm.recurringEndMonthPickValue) ? this.data.recordForm.recurringEndMonthPickValue : '';
    const derivedEndMonth = currentEndMonth || (currentEndPickValue ? currentEndPickValue.slice(0, 7) : '');
    this.setData({
      'recordForm.recurringEndMode': safeMode,
      'recordForm.recurringEndMonth': safeMode === 'end' ? derivedEndMonth : ''
    });
  },

  // 周期账：结束月份
  onRecurringEndMonthChange(e) {
    const value = e.detail.value || '';
    const month = value.slice(0, 7);
    this.setData({
      'recordForm.recurringEndMonth': month,
      'recordForm.recurringEndMonthPickValue': value
    });
  },

  async submitRecord() {
    try {
      const {
        type,
        accountId,
        categoryId,
        amount,
        recordDate,
        recordTime,
        remark,
        isRecurring,
        recurringStartMonth,
        recurringEndMode,
        recurringEndMonth
      } = this.data.recordForm;

      if (!accountId || !categoryId) {
        wx.showToast({ title: '请选择账户和分类', icon: 'none' });
        return;
      }

      const amountNum = parseFloat(amount);
      if (Number.isNaN(amountNum) || amountNum < 0.01) {
        wx.showToast({ title: '金额必须 >= 0.01', icon: 'none' });
        return;
      }

      if (isRecurring) {
        if (!recurringStartMonth) {
          wx.showToast({ title: '请选择开始月份', icon: 'none' });
          return;
        }
        let endMonth = null;
        if (recurringEndMode === 'end') {
          if (!recurringEndMonth) {
            wx.showToast({ title: '请选择结束月份', icon: 'none' });
            return;
          }
          endMonth = recurringEndMonth;
        }

        await ledgerApi.createRecurringRule({
          type: parseInt(type, 10) || 1,
          accountId,
          categoryId,
          amount: amountNum,
          remark: remark || '',
          startMonth: recurringStartMonth,
          endMonth
        });

        wx.showToast({ title: '周期账创建成功', icon: 'success' });
      } else {
        if (!recordDate || !recordTime) {
          wx.showToast({ title: '请选择日期/时间', icon: 'none' });
          return;
        }

        const normalizedRecordTime = `${recordDate}T${recordTime}:00`;

        await ledgerApi.createRecord({
          type: parseInt(type, 10) || 1,
          accountId,
          categoryId,
          amount: amountNum,
          recordTime: normalizedRecordTime,
          remark: remark || ''
        });

        wx.showToast({ title: '记账成功', icon: 'success' });
      }

      this.closeRecordModal();

      this.setData({
        page: 1,
        groupedRecords: [],
        total: 0,
        hasMore: false
      });
      await this.refreshRecords();
    } catch (err) {
      console.error('提交记账失败', err);
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }
  },

  openAccountModal() {
    this.setData({
      openAccountModal: true,
      modalScrollHeightPx: this.computeModalScrollHeightPx(),
      'accountForm.name': '',
      'accountForm.type': 'General'
    });
  },

  closeAccountModal() {
    this.setData({ openAccountModal: false });
  },

  onAccountNameChange(e) {
    this.setData({ 'accountForm.name': e.detail.value });
  },

  async submitAccount() {
    try {
      const name = (this.data.accountForm.name || '').trim();
      if (!name) {
        wx.showToast({ title: '请输入账户名称', icon: 'none' });
        return;
      }

      await ledgerApi.createAccount({
        name,
        type: this.data.accountForm.type || 'General'
      });

      wx.showToast({ title: '账户创建成功', icon: 'success' });
      this.closeAccountModal();
      await this.loadBaseData();
      await this.refreshRecords();
    } catch (err) {
      console.error('新增账户失败', err);
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }
  },

  openBatchModal() {
    const type = Number(this.data.batchForm.type || 1);
    const accountId = this.data.accounts.length ? this.data.accounts[0].id : null;
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;

    const now = new Date();
    const month = this.data.month || `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
    const [yearStr, monthStr] = month.split('-');
    const defaultPickValue = `${yearStr}-${monthStr}-${`${now.getDate()}`.padStart(2, '0')}`;

    const batchWorkdays = this.generateWorkdaysForMonth(month);
    const batchCalendarCells = this.buildCalendarCells(month, []);

    this.setData({
      openBatchModal: true,
      batchMonthPickValue: defaultPickValue,
      modalScrollHeightPx: this.computeModalScrollHeightPx(),
      'batchForm.type': type,
      'batchForm.accountId': accountId,
      'batchForm.categoryId': categoryId,
      'batchForm.amount': '',
      'batchForm.remark': '',
      'batchForm.month': month,
      // 默认不预选，避免全部同色看不出状态
      'batchForm.recordDates': [],
      batchWorkdays,
      batchCalendarCells,
      batchAccountPickerRange: this.data.accounts.length ? this.data.accounts.map(a => a.name || '账户') : ['请选择账户'],
      batchCategoryPickerRange: categories.length ? categories.map(c => c.name || '分类') : ['请选择分类']
    });
    this.updateBatchPreview([]);
  },

  closeBatchModal() {
    this.setData({ openBatchModal: false });
  },

  onBatchTypeChange(e) {
    const type = parseInt(e.detail.value, 10);
    const safeType = Number.isNaN(type) ? 1 : type;
    const categories = safeType === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;
    const batchCategoryPickerRange = categories.length ? categories.map(c => c.name || '分类') : ['请选择分类'];

    this.setData({
      'batchForm.type': safeType,
      'batchForm.categoryId': categoryId,
      batchCategoryPickerRange
    });
  },

  onBatchAccountChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx) || !this.data.accounts.length) return;
    const account = this.data.accounts[idx];
    if (!account) return;
    this.setData({ 'batchForm.accountId': account.id });
  },

  onBatchCategoryChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx)) return;
    const type = Number(this.data.batchForm.type);
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    if (!categories.length) return;
    const category = categories[idx];
    if (!category) return;
    this.setData({ 'batchForm.categoryId': category.id });
  },

  onBatchMonthPick(e) {
    const value = e.detail.value || '';
    const month = value.slice(0, 7);
    const batchWorkdays = this.generateWorkdaysForMonth(month);
    const batchCalendarCells = this.buildCalendarCells(month, []);

    const type = Number(this.data.batchForm.type || 1);
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;

    this.setData({
      'batchForm.month': month,
      batchMonthPickValue: value,
      batchWorkdays,
      batchCalendarCells,
      // 切换月份后默认清空选择，由用户按需点选或全选工作日
      'batchForm.recordDates': [],
      'batchForm.categoryId': categoryId
    });

    this.updateBatchPreview([]);
  },

  toggleBatchDate(e) {
    const date = e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.date : null;
    if (!date) return;

    const recordDates = [...(this.data.batchForm.recordDates || [])];
    const idx = recordDates.indexOf(date);
    if (idx >= 0) {
      recordDates.splice(idx, 1);
    } else {
      recordDates.push(date);
      recordDates.sort();
    }
    this.setData({ 'batchForm.recordDates': recordDates });
    this.setData({ batchCalendarCells: this.buildCalendarCells(this.data.batchForm.month, recordDates) });
    this.updateBatchPreview(recordDates);
  },

  selectAllWorkdays() {
    const recordDates = [...(this.data.batchWorkdays || [])];
    this.setData({ 'batchForm.recordDates': recordDates });
    this.setData({ batchCalendarCells: this.buildCalendarCells(this.data.batchForm.month, recordDates) });
    this.updateBatchPreview(recordDates);
  },

  clearBatchDates() {
    this.setData({ 'batchForm.recordDates': [] });
    this.setData({ batchCalendarCells: this.buildCalendarCells(this.data.batchForm.month, []) });
    this.updateBatchPreview([]);
  },

  noop() {},

  onBatchAmountChange(e) {
    this.setData({ 'batchForm.amount': e.detail.value });
    this.updateBatchPreview(this.data.batchForm.recordDates || []);
  },

  onBatchRemarkChange(e) {
    this.setData({ 'batchForm.remark': e.detail.value });
  },

  updateBatchPreview(recordDates) {
    const dates = recordDates || this.data.batchForm.recordDates || [];
    const amountNum = parseFloat(this.data.batchForm.amount);
    const total = dates.length * (Number.isNaN(amountNum) ? 0 : amountNum);
    this.setData({ batchTotalAmount: this.moneyToFixed2(total) });
  },

  async submitBatchRecords() {
    try {
      const { type, accountId, categoryId, amount, recordDates, remark } = this.data.batchForm;

      if (!accountId || !categoryId) {
        wx.showToast({ title: '请选择账户和分类', icon: 'none' });
        return;
      }

      const amountNum = parseFloat(amount);
      if (Number.isNaN(amountNum) || amountNum < 0.01) {
        wx.showToast({ title: '金额必须 >= 0.01', icon: 'none' });
        return;
      }

      if (!recordDates || !recordDates.length) {
        wx.showToast({ title: '请至少选择一个日期', icon: 'none' });
        return;
      }

      const res = await ledgerApi.createBatchRecords({
        type,
        accountId,
        categoryId,
        amount: amountNum,
        recordDates,
        remark: remark || ''
      });

      const createdCount = res && res.createdCount ? res.createdCount : 0;
      wx.showToast({ title: `批量创建成功，共 ${createdCount} 条`, icon: 'success' });
      this.closeBatchModal();

      await this.loadBaseData();
      this.setData({ page: 1, groupedRecords: [], total: 0, hasMore: false });
      await this.refreshRecords();
    } catch (err) {
      console.error('批量记账失败', err);
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }
  },

  // ---- 数据加载与刷新 ----

  async ensureBaseDataLoaded() {
    if (this.data.baseLoaded) return;
    await this.loadBaseData();
    this.setData({ baseLoaded: true });
  },

  async refreshAll() {
    await this.refreshRecords();
  },

  async refreshRecords() {
    await this.loadRecords(false);
    await Promise.all([this.loadOverviewStats(), this.loadCategoryStats(), this.loadTrendStats()]);
  },

  async loadBaseData() {
    const accounts = (await ledgerApi.getAccounts()) || [];
    const categoryGroup = (await ledgerApi.getCategories()) || {};
    const expenseCategories = categoryGroup.expense || [];
    const incomeCategories = categoryGroup.income || [];
    const allCategories = [...expenseCategories, ...incomeCategories];

    const accountFilterRange = ['全部账户', ...(accounts.map(a => a.name || '账户'))];
    const categoryFilterRange = ['全部分类', ...(allCategories.map(c => c.name || '分类'))];

    // 如果当前筛选项已无效（比如账户被删除/列表变化），回到“全部”
    const safeAccountFilterIndex = Math.min(this.data.accountFilterIndex || 0, Math.max(0, accountFilterRange.length - 1));
    const safeCategoryFilterIndex = Math.min(this.data.categoryFilterIndex || 0, Math.max(0, categoryFilterRange.length - 1));

    const recordType = Number(this.data.recordForm.type);
    const currentTypeCategories = recordType === 2 ? incomeCategories : expenseCategories;
    const categoryPickerRange = currentTypeCategories.length
      ? currentTypeCategories.map(c => c.name || '分类')
      : ['请选择分类'];
    const categoryId = currentTypeCategories.length ? currentTypeCategories[0].id : null;
    const accountId = accounts.length ? accounts[0].id : null;

    this.setData({
      accounts,
      expenseCategories,
      incomeCategories,
      allCategories,
      accountFilterRange,
      categoryFilterRange,
      accountFilterIndex: safeAccountFilterIndex,
      categoryFilterIndex: safeCategoryFilterIndex,
      accountPickerRange: accounts.length ? accounts.map(a => a.name || '账户') : ['请选择账户'],
      categoryPickerRange,
      'recordForm.accountId': accountId,
      'recordForm.categoryId': categoryId
    });
  },

  async loadOverviewStats() {
    const month = this.data.month;
    const overview = (await ledgerApi.getOverviewStats({ month })) || {};
    this.setData({
      'overview.totalIncome': this.moneyToFixed2(overview.totalIncome),
      'overview.totalExpense': this.moneyToFixed2(overview.totalExpense),
      'overview.balance': this.moneyToFixed2(overview.balance)
    });
  },

  async loadCategoryStats() {
    const month = this.data.month;
    const res = (await ledgerApi.getCategoryStats({ month })) || {};
    const expense = res.expense || [];

    const expenseCategoryStats = expense.map(item => {
      const percentage = Number(item.percentage || 0);
      return {
        categoryId: item.categoryId,
        categoryName: item.categoryName,
        totalAmount: this.moneyToFixed2(item.totalAmount),
        percentage: percentage.toFixed(2),
        widthPercent: Math.max(0, Math.min(100, percentage))
      };
    });

    this.setData({ expenseCategoryStats });
  },

  async loadTrendStats() {
    const month = this.data.month;
    const points = (await ledgerApi.getTrendStats({ month })) || [];
    const last7 = points.slice(-7);

    const maxValue = Math.max(...last7.map(p => Math.max(Number(p.income || 0), Number(p.expense || 0))));

    const trendLast7 = last7.map(p => {
      const incomeNum = Number(p.income || 0);
      const expenseNum = Number(p.expense || 0);
      const widthBase = maxValue <= 0 ? 0 : 100 / maxValue;
      return {
        date: p.date,
        day: p.date ? p.date.slice(5) : '',
        income: this.moneyToFixed2(incomeNum),
        expense: this.moneyToFixed2(expenseNum),
        incomeWidth: widthBase ? incomeNum * widthBase : 0,
        expenseWidth: widthBase ? expenseNum * widthBase : 0
      };
    });

    this.setData({ trendLast7 });
  },

  async loadRecords(append) {
    if (!this.data.month) return;

    const page = this.data.page || 1;
    const size = this.data.size || 20;

    const params = { month: this.data.month, page, size };
    if (this.data.selectedAccountId != null) params.accountId = this.data.selectedAccountId;
    if (this.data.selectedCategoryId != null) params.categoryId = this.data.selectedCategoryId;

    const res = (await ledgerApi.queryRecords(params)) || {};
    const nextGroups = (res.groups || []).map(g => this.normalizeGroup(g));

    this.setData({ total: res.total || 0 });

    if (!append || page === 1) {
      this.setData({ groupedRecords: nextGroups });
    } else {
      const oldGroups = this.data.groupedRecords || [];
      if (
        oldGroups.length &&
        nextGroups.length &&
        oldGroups[oldGroups.length - 1].date === nextGroups[0].date
      ) {
        const last = oldGroups[oldGroups.length - 1];
        const firstNew = nextGroups[0];
        last.totalIncome = this.moneyToFixed2(Number(last.totalIncome) + Number(firstNew.totalIncome));
        last.totalExpense = this.moneyToFixed2(Number(last.totalExpense) + Number(firstNew.totalExpense));
        last.records = (last.records || []).concat(firstNew.records || []);
        nextGroups.shift();
      }
      this.setData({ groupedRecords: oldGroups.concat(nextGroups) });
    }

    const total = res.total || 0;
    const hasMore = page * size < total;
    this.setData({ hasMore });
  },

  // ---- 工具函数 ----
  moneyToFixed2(value) {
    const num = Number(value || 0);
    return num.toFixed(2);
  },

  normalizeGroup(group) {
    const totalIncome = this.moneyToFixed2(group.totalIncome);
    const totalExpense = this.moneyToFixed2(group.totalExpense);
    const records = (group.records || []).map(r => ({
      ...r,
      amount: this.moneyToFixed2(r.amount)
    }));

    return {
      ...group,
      totalIncome,
      totalExpense,
      records
    };
  },

  // 生成某个月份的工作日列表（周日/周六排除）
  generateWorkdaysForMonth(month) {
    const [yearStr, monthStr] = (month || '').split('-');
    const year = Number(yearStr);
    const m = Number(monthStr);
    if (!year || !m) return [];

    const daysInMonth = new Date(year, m, 0).getDate();
    const dates = [];
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, m - 1, day);
      const week = d.getDay();
      if (week !== 0 && week !== 6) {
        const mm = `${m}`.padStart(2, '0');
        const dd = `${day}`.padStart(2, '0');
        dates.push(`${year}-${mm}-${dd}`);
      }
    }
    return dates;
  },

  // 构建月历网格（每行7列，周一为第一列）
  buildCalendarCells(month, selectedDates) {
    const [yearStr, monthStr] = (month || '').split('-');
    const year = Number(yearStr);
    const m = Number(monthStr);
    if (!year || !m) return [];

    const selectedSet = new Set(selectedDates || []);
    const firstDay = new Date(year, m - 1, 1);
    const daysInMonth = new Date(year, m, 0).getDate();
    // JS getDay: Sun=0..Sat=6 -> Mon=0..Sun=6
    const firstWeekdayMon0 = (firstDay.getDay() + 6) % 7;

    const cells = [];
    // 前置日期（上月，灰态不可选）
    const prevMonthDays = new Date(year, m - 1, 0).getDate();
    for (let i = 0; i < firstWeekdayMon0; i++) {
      const day = prevMonthDays - firstWeekdayMon0 + i + 1;
      cells.push({
        key: `pre-${i}`,
        date: '',
        dayText: `${day}`,
        inMonth: false,
        selected: false,
        isWorkday: false,
        selectable: false
      });
    }

    // 当月日期
    for (let day = 1; day <= daysInMonth; day++) {
      const dd = `${day}`.padStart(2, '0');
      const mm = `${m}`.padStart(2, '0');
      const date = `${year}-${mm}-${dd}`;
      const d = new Date(year, m - 1, day);
      const week = d.getDay();
      const isWorkday = week !== 0 && week !== 6;
      cells.push({
        key: date,
        date,
        dayText: `${day}`,
        inMonth: true,
        selected: selectedSet.has(date),
        isWorkday,
        selectable: true
      });
    }

    // 尾部日期（下月，灰态不可选）补齐到7的倍数
    const remainder = cells.length % 7;
    if (remainder > 0) {
      const tail = 7 - remainder;
      for (let i = 0; i < tail; i++) {
        const day = i + 1;
        cells.push({
          key: `tail-${i}`,
          date: '',
          dayText: `${day}`,
          inMonth: false,
          selected: false,
          isWorkday: false,
          selectable: false
        });
      }
    }
    return cells;
  }
});

