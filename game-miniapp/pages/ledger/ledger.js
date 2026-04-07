// pages/ledger/ledger.js
// 记账台账：列表/记一笔/删除（接入 game-server 的 /ledger/*）

const { checkLogin } = require('../../utils/auth');
const { ledgerApi } = require('../../utils/api');

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
    selectedAccountId: null,
    selectedCategoryId: null,
    accountPickerRange: ['请选择账户'],
    categoryPickerRange: ['请选择分类'],

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
      type: 1, // 1=支出, 2=收入
      accountId: null,
      categoryId: null,
      amount: '',
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
    batchAccountPickerRange: ['请选择账户'],
    batchCategoryPickerRange: ['请选择分类'],
    // 批量记账预览（总金额）
    batchTotalAmount: '0.00'
  },

  onLoad() {
    const now = new Date();
    const ym = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
    const ymd = `${ym}-${`${now.getDate()}`.padStart(2, '0')}`;

    this.setData({
      month: ym,
      monthPickValue: ymd,
      loading: true
    });

    if (!checkLogin()) {
      wx.navigateTo({
        url: '/pages/login/login'
      });
      this.setData({ loading: false });
      return;
    }

    this.init();
  },

  async init() {
    try {
      await this.ensureBaseDataLoaded();
      await this.refreshAll();
    } finally {
      this.setData({ loading: false });
    }
  },

  // 月份选择（骨架，后续会触发重新加载）
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

  // 筛选（骨架）
  onAccountFilterChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx)) return;

    const selectedAccountId = idx <= 0 ? null : (this.data.accounts[idx - 1]?.id ?? null);
    this.setData({
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

    const selectedCategoryId = idx <= 0 ? null : (this.data.allCategories[idx - 1]?.id ?? null);
    this.setData({
      selectedCategoryId,
      page: 1,
      groupedRecords: [],
      total: 0,
      hasMore: false
    });
    this.refreshRecords();
  },

  // 选择账户（骨架）
  onRecordAccountChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx) || !this.data.accounts.length) return;
    const account = this.data.accounts[idx];
    if (!account) return;
    this.setData({ 'recordForm.accountId': account.id });
  },

  // 选择分类（骨架）
  onRecordCategoryChange(e) {
    const idx = parseInt(e.detail.value, 10);
    if (Number.isNaN(idx)) return;

    const type = Number(this.data.recordForm.type);
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    if (!categories.length) return;

    const category = categories[idx];
    if (!category) return;
    this.setData({ 'recordForm.categoryId': category.id });
  },

  // 列表加载更多（骨架）
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

  // 删除记录（骨架）
  onDeleteRecord(e) {
    const id = e?.currentTarget?.dataset?.id;
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

  // 打开/关闭弹窗（骨架）
  openRecordModal() {
    const now = new Date();
    const recordDate = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}`;
    const recordTime = `${`${now.getHours()}`.padStart(2, '0')}:${`${now.getMinutes()}`.padStart(2, '0')}`;

    const type = Number(this.data.recordForm.type);
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;
    const accountId = this.data.accounts.length ? this.data.accounts[0].id : null;

    this.setData({
      openRecordModal: true,
      'recordForm.amount': '',
      'recordForm.remark': '',
      'recordForm.recordDate': recordDate,
      'recordForm.recordTime': recordTime,
      'recordForm.accountId': accountId,
      'recordForm.categoryId': categoryId
    });
  },

  closeRecordModal() {
    this.setData({ openRecordModal: false });
  },

  // 记一笔表单事件（骨架）
  onRecordTypeChange(e) {
    const type = parseInt(e.detail.value, 10);
    const safeType = Number.isNaN(type) ? 1 : type;

    const categories = safeType === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;
    const categoryPickerRange = categories.length ? categories.map(c => c.name) : ['请选择分类'];

    this.setData({
      'recordForm.type': safeType,
      'recordForm.categoryId': categoryId,
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

  // 保存（骨架）
  async submitRecord() {
    try {
      const { type, accountId, categoryId, amount, recordDate, recordTime, remark } = this.data.recordForm;

      if (!accountId || !categoryId) {
        wx.showToast({ title: '请选择账户和分类', icon: 'none' });
        return;
      }
      const amountNum = parseFloat(amount);
      if (Number.isNaN(amountNum) || amountNum < 0.01) {
        wx.showToast({ title: '金额必须 >= 0.01', icon: 'none' });
        return;
      }
      if (!recordDate || !recordTime) {
        wx.showToast({ title: '请选择日期/时间', icon: 'none' });
        return;
      }

      const normalizedRecordTime = `${recordDate}T${recordTime}:00`;

      await ledgerApi.createRecord({
        type,
        accountId,
        categoryId,
        amount: amountNum,
        recordTime: normalizedRecordTime,
        remark: remark || ''
      });

      wx.showToast({ title: '记账成功', icon: 'success' });
      this.closeRecordModal();

      // 重置表单并刷新列表/概览
      const now = new Date();
      const recordDate2 = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}`;
      const recordTime2 = `${`${now.getHours()}`.padStart(2, '0')}:${`${now.getMinutes()}`.padStart(2, '0')}`;

      this.setData({
        page: 1,
        groupedRecords: [],
        total: 0,
        hasMore: false,
        'recordForm.amount': '',
        'recordForm.remark': '',
        'recordForm.recordDate': recordDate2,
        'recordForm.recordTime': recordTime2
      });

      await this.refreshRecords();
    } catch (err) {
      console.error('提交记账失败', err);
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }
  },

  async ensureBaseDataLoaded() {
    if (this.data.baseLoaded) return;
    await this.loadBaseData();
    this.setData({ baseLoaded: true });
  },

  async refreshAll() {
    // 初始化时先拉列表，再拉概览（列表分页不依赖统计）
    await this.refreshRecords();
  },

  async refreshRecords() {
    // 刷新：列表 + 月度统计（概览/分类占比/趋势）
    await this.loadRecords(false);
    await Promise.all([
      this.loadOverviewStats(),
      this.loadCategoryStats(),
      this.loadTrendStats()
    ]);
  },

  async loadBaseData() {
    const accounts = (await ledgerApi.getAccounts()) || [];
    const categoryGroup = (await ledgerApi.getCategories()) || {};
    const expenseCategories = categoryGroup.expense || [];
    const incomeCategories = categoryGroup.income || [];
    const allCategories = [...expenseCategories, ...incomeCategories];

    const accountFilterRange = ['全部账户', ...(accounts.map(a => a.name || '账户'))];
    const categoryFilterRange = ['全部分类', ...(allCategories.map(c => c.name || '分类'))];

    // 根据当前记一笔类型设置分类下拉
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
      accountPickerRange: accounts.length ? accounts.map(a => a.name || '账户') : ['请选择账户'],
      categoryPickerRange,
      'recordForm.accountId': accountId,
      'recordForm.categoryId': categoryId
    });
  },

  async loadOverviewStats() {
    const month = this.data.month;
    const overview = (await ledgerApi.getOverviewStats({ month })) || {};

    const totalIncome = this.moneyToFixed2(overview.totalIncome);
    const totalExpense = this.moneyToFixed2(overview.totalExpense);
    const balance = this.moneyToFixed2(overview.balance);

    this.setData({
      'overview.totalIncome': totalIncome,
      'overview.totalExpense': totalExpense,
      'overview.balance': balance
    });
  },

  async loadCategoryStats() {
    const month = this.data.month;
    const res = (await ledgerApi.getCategoryStats({ month })) || {};
    const expense = res.expense || [];

    // 分类占比：饼图在小程序上先用“进度条排行”替代
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

    const maxValue = Math.max(
      ...last7.map(p => Math.max(Number(p.income || 0), Number(p.expense || 0)))
    );

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

    const params = {
      month: this.data.month,
      page,
      size
    };
    if (this.data.selectedAccountId != null) params.accountId = this.data.selectedAccountId;
    if (this.data.selectedCategoryId != null) params.categoryId = this.data.selectedCategoryId;

    const res = (await ledgerApi.queryRecords(params)) || {};
    const nextGroups = (res.groups || []).map(g => this.normalizeGroup(g));

    this.setData({
      total: res.total || 0
    });

    if (!append || page === 1) {
      this.setData({
        groupedRecords: nextGroups
      });
    } else {
      const oldGroups = this.data.groupedRecords || [];
      if (oldGroups.length && nextGroups.length && oldGroups[oldGroups.length - 1].date === nextGroups[0].date) {
        const last = oldGroups[oldGroups.length - 1];
        const firstNew = nextGroups[0];
        last.totalIncome = this.moneyToFixed2(Number(last.totalIncome) + Number(firstNew.totalIncome));
        last.totalExpense = this.moneyToFixed2(Number(last.totalExpense) + Number(firstNew.totalExpense));
        last.records = (last.records || []).concat(firstNew.records || []);
        nextGroups.shift();
      }
      this.setData({
        groupedRecords: oldGroups.concat(nextGroups)
      });
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

  // 新增账户（骨架）
  openAccountModal() {
    this.setData({
      openAccountModal: true,
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
      wx.showToast({ title: '保存失败', icon: 'none' });
    }
  },

  // 批量记账（骨架）
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

    this.setData({
      openBatchModal: true,
      batchMonthPickValue: defaultPickValue,
      'batchForm.type': type,
      'batchForm.accountId': accountId,
      'batchForm.categoryId': categoryId,
      'batchForm.amount': '',
      'batchForm.remark': '',
      'batchForm.month': month,
      'batchForm.recordDates': [...batchWorkdays],
      batchWorkdays,
      batchAccountPickerRange: this.data.accounts.length ? this.data.accounts.map(a => a.name || '账户') : ['请选择账户'],
      batchCategoryPickerRange: categories.length ? categories.map(c => c.name || '分类') : ['请选择分类']
    });
  },

  closeBatchModal() {
    this.setData({ openBatchModal: false });
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

      wx.showToast({ title: `批量创建成功，共 ${res?.createdCount || 0} 条`, icon: 'success' });
      this.closeBatchModal();

      // 重新加载账户余额与列表
      await this.loadBaseData();
      this.setData({ page: 1, groupedRecords: [], total: 0, hasMore: false });
      await this.refreshRecords();
    } catch (err) {
      console.error('批量记账失败', err);
      wx.showToast({ title: err.message || '保存失败', icon: 'none' });
    }
  },

  // ---- 批量记账弹窗事件 ----
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

    const type = Number(this.data.batchForm.type || 1);
    const categories = type === 2 ? this.data.incomeCategories : this.data.expenseCategories;
    const categoryId = categories.length ? categories[0].id : null;

    this.setData({
      'batchForm.month': month,
      batchMonthPickValue: value,
      batchWorkdays,
      'batchForm.recordDates': [...batchWorkdays],
      'batchForm.categoryId': categoryId
    });

    this.updateBatchPreview(batchWorkdays);
  },

  toggleBatchDate(e) {
    const date = e?.currentTarget?.dataset?.date;
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
    this.updateBatchPreview(recordDates);
  },

  selectAllWorkdays() {
    this.setData({ 'batchForm.recordDates': [...(this.data.batchWorkdays || [])] });
    this.updateBatchPreview(this.data.batchWorkdays || []);
  },

  clearBatchDates() {
    this.setData({ 'batchForm.recordDates': [] });
    this.updateBatchPreview([]);
  },

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

  // 生成某个月份的工作日列表（周日/周六排除）
  generateWorkdaysForMonth(month) {
    const [yearStr, monthStr] = (month || '').split('-');
    const year = Number(yearStr);
    const m = Number(monthStr);
    if (!year || !m) return [];

    const daysInMonth = new Date(year, m, 0).getDate(); // JS month: 1-12 -> pass as m, day=0 returns last day
    const dates = [];
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, m - 1, day);
      const week = d.getDay(); // 0=Sun, 6=Sat
      if (week !== 0 && week !== 6) {
        const mm = `${m}`.padStart(2, '0');
        const dd = `${day}`.padStart(2, '0');
        dates.push(`${year}-${mm}-${dd}`);
      }
    }
    return dates;
  },
});

