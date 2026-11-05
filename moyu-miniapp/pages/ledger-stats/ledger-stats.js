const { ledgerApi } = require('../../utils/api');

Page({
  data: {
    timeFilterMode: 'month',
    month: '',
    monthPickValue: '',
    rangeStartDate: '',
    rangeEndDate: '',
    accountId: null,
    categoryId: null,
    loading: true,
    overview: { totalIncome: '0.00', totalExpense: '0.00', balance: '0.00' },
    expenseRatio: 0,
    expenseRatioText: '0.0',
    activeType: 1,
    categories: [],
    trends: [],
    topCategory: null,
    averageDailyExpense: '0.00'
  },

  onLoad(options) {
    const now = new Date();
    const month = options.month || `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}`;
    const today = this.formatDate(now);
    this.setData({
      timeFilterMode: options.startDate && options.endDate ? 'range' : 'month',
      month,
      monthPickValue: `${month}-01`,
      rangeStartDate: options.startDate || `${month}-01`,
      rangeEndDate: options.endDate || today,
      accountId: options.accountId ? Number(options.accountId) : null,
      categoryId: options.categoryId ? Number(options.categoryId) : null
    });
    this.loadStats();
  },

  onTimeFilterModeTap(e) {
    const timeFilterMode = e.currentTarget.dataset.mode === 'range' ? 'range' : 'month';
    this.setData({ timeFilterMode });
    this.loadStats();
  },

  onMonthPick(e) {
    const month = e.detail.value || '';
    this.setData({ month, monthPickValue: `${month}-01` });
    this.loadStats();
  },

  onRangeStartDateChange(e) {
    this.setData({ rangeStartDate: e.detail.value || '' });
    this.loadStats();
  },

  onRangeEndDateChange(e) {
    this.setData({ rangeEndDate: e.detail.value || '' });
    this.loadStats();
  },

  onCategoryTypeTap(e) {
    this.setData({ activeType: Number(e.currentTarget.dataset.type) === 2 ? 2 : 1 });
    this.updateVisibleCategories();
  },

  buildTimeParams() {
    if (this.data.timeFilterMode === 'range') {
      const { rangeStartDate, rangeEndDate } = this.data;
      if (!rangeStartDate || !rangeEndDate || rangeStartDate > rangeEndDate) return null;
      const start = new Date(`${rangeStartDate}T00:00:00`);
      const end = new Date(`${rangeEndDate}T00:00:00`);
      if ((end - start) / 86400000 > 365) return null;
      return this.addDimensionFilters({ startDate: rangeStartDate, endDate: rangeEndDate });
    }
    return this.data.month ? this.addDimensionFilters({ month: this.data.month }) : null;
  },

  addDimensionFilters(params) {
    if (this.data.accountId != null) params.accountId = this.data.accountId;
    if (this.data.categoryId != null) params.categoryId = this.data.categoryId;
    return params;
  },

  async loadStats() {
    const requestId = (this.statsRequestId || 0) + 1;
    this.statsRequestId = requestId;
    const params = this.buildTimeParams();
    if (!params) {
      this.setData({ loading: false });
      wx.showToast({ title: '日期需有效，分析范围不能超过一年', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      const [overview, categoryData, trendData] = await Promise.all([
        ledgerApi.getOverviewStats(params),
        ledgerApi.getCategoryStats(params),
        ledgerApi.getTrendStats(params)
      ]);
      const totalIncome = Number(overview && overview.totalIncome || 0);
      const totalExpense = Number(overview && overview.totalExpense || 0);
      const categories = (categoryData && categoryData.expense || []).map(item => ({
        ...item,
        amountText: this.money(item.totalAmount),
        percentageText: Number(item.percentage || 0).toFixed(1),
        barWidth: Math.max(0, Math.min(100, Number(item.percentage || 0)))
      }));
      const trends = this.buildTrendRows(trendData || []);
      const days = this.periodDays(params);
      if (requestId !== this.statsRequestId) return;

      this.setData({
        overview: {
          totalIncome: this.money(totalIncome),
          totalExpense: this.money(totalExpense),
          balance: this.money(totalIncome - totalExpense)
        },
        expenseRatio: totalIncome > 0 ? Math.min(100, totalExpense / totalIncome * 100) : 0,
        expenseRatioText: totalIncome > 0 ? (totalExpense / totalIncome * 100).toFixed(1) : '0.0',
        allExpenseCategories: categories,
        allIncomeCategories: (categoryData && categoryData.income || []).map(item => ({
          ...item,
          amountText: this.money(item.totalAmount),
          percentageText: Number(item.percentage || 0).toFixed(1),
          barWidth: Math.max(0, Math.min(100, Number(item.percentage || 0)))
        })),
        trends,
        topCategory: categories[0] || null,
        averageDailyExpense: this.money(totalExpense / Math.max(1, days))
      });
      this.updateVisibleCategories();
    } catch (err) {
      if (requestId !== this.statsRequestId) return;
      console.error('加载收支分析失败', err);
      wx.showToast({ title: err.message || '分析数据加载失败', icon: 'none' });
    } finally {
      if (requestId === this.statsRequestId) this.setData({ loading: false });
    }
  },

  updateVisibleCategories() {
    const key = this.data.activeType === 2 ? 'allIncomeCategories' : 'allExpenseCategories';
    const source = this.data[key] || [];
    this.setData({ categories: source.slice(0, 8) });
  },

  buildTrendRows(points) {
    const visible = points.slice(-14);
    const max = Math.max(0, ...visible.map(item => Math.max(Number(item.income || 0), Number(item.expense || 0))));
    return visible.map(item => ({
      date: item.date,
      day: item.date ? item.date.slice(5) : '',
      income: this.money(item.income),
      expense: this.money(item.expense),
      incomeWidth: max > 0 ? Number(item.income || 0) / max * 100 : 0,
      expenseWidth: max > 0 ? Number(item.expense || 0) / max * 100 : 0
    }));
  },

  periodDays(params) {
    if (params.month) {
      const [year, month] = params.month.split('-').map(Number);
      return new Date(year, month, 0).getDate();
    }
    const start = new Date(`${params.startDate}T00:00:00`);
    const end = new Date(`${params.endDate}T00:00:00`);
    return Math.floor((end - start) / 86400000) + 1;
  },

  formatDate(date) {
    return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
  },

  money(value) {
    return Number(value || 0).toFixed(2);
  },

  onShareAppMessage() {
    return { title: '收支分析', path: '/pages/ledger-stats/ledger-stats' };
  },

  onShareTimeline() {
    return { title: '收支分析' };
  }
});
