const { communityApi } = require('../../utils/api');

Page({
  data: {
    city: '重庆',
    keyword: '',
    searching: false,
    comparing: false,
    searchResults: [],
    selected: [],
    compareData: null,
    metricClassMap: {},
    isThreeColumns: false
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value || '' });
  },

  onCityInput(e) {
    this.setData({ city: e.detail.value || '' });
  },

  async onSearch() {
    this.setData({ searching: true });
    try {
      const list = await communityApi.search(this.data.keyword, this.data.city);
      this.setData({ searchResults: list || [], searching: false });
      if (!list || list.length === 0) {
        wx.showToast({ title: '未找到相关小区', icon: 'none' });
      }
    } catch (e) {
      this.setData({ searching: false });
    }
  },

  onAddCommunity(e) {
    const id = Number(e.currentTarget.dataset.id);
    const selected = this.data.selected || [];
    if (selected.some((it) => it.id === id)) {
      wx.showToast({ title: '该小区已在对比栏', icon: 'none' });
      return;
    }
    if (selected.length >= 3) {
      wx.showToast({ title: '最多选择 3 个小区', icon: 'none' });
      return;
    }
    const item = (this.data.searchResults || []).find((it) => it.id === id);
    if (!item) return;
    this.setData({ selected: [...selected, item] });
  },

  onRemoveCommunity(e) {
    const id = Number(e.currentTarget.dataset.id);
    const selected = (this.data.selected || []).filter((it) => it.id !== id);
    this.setData({ selected });
  },

  async onCompare() {
    const ids = (this.data.selected || []).map((it) => it.id);
    if (ids.length < 2) {
      wx.showToast({ title: '至少选择 2 个小区', icon: 'none' });
      return;
    }
    this.setData({ comparing: true });
    try {
      const compareData = await communityApi.compare(ids, this.data.city);
      const metricClassMap = this.buildMetricClassMap(compareData);
      this.setData({
        compareData,
        comparing: false,
        metricClassMap,
        isThreeColumns: ids.length === 3
      });
    } catch (e) {
      this.setData({ comparing: false });
    }
  },

  buildMetricClassMap(compareData) {
    const map = {};
    const groups = (compareData && compareData.groups) || [];
    groups.forEach((group) => {
      (group.items || []).forEach((item) => {
        const direction = item.betterDirection;
        if (direction === 'NONE') return;
        const values = item.values || [];
        const numericValues = values
          .map((v, idx) => ({ idx, val: Number(v) }))
          .filter((it) => !Number.isNaN(it.val));
        if (numericValues.length === 0) return;
        let best;
        if (direction === 'LOWER_BETTER') {
          best = Math.min(...numericValues.map((it) => it.val));
        } else {
          best = Math.max(...numericValues.map((it) => it.val));
        }
        numericValues.forEach((it) => {
          if (it.val === best) {
            map[`${item.field}-${it.idx}`] = 'better';
          }
        });
      });
    });
    return map;
  },

  goMortgage(e) {
    const id = Number(e.currentTarget.dataset.id);
    const communities = (this.data.compareData && this.data.compareData.communities) || [];
    const target = communities.find((it) => it.id === id);
    if (!target || !target.mortgageReference) return;
    const totalPrice = Number(target.mortgageReference.totalPrice || 0);
    const loanPrincipal = Number(target.mortgageReference.loanPrincipal || 0);
    wx.navigateTo({
      url: `/pages/mortgage/mortgage?communityName=${encodeURIComponent(target.name || '')}&houseTotal=${totalPrice}&commercialWan=${(loanPrincipal / 10000).toFixed(2)}`
    });
  },

  onShareAppMessage() {
    return {
      title: '小区对比助手',
      path: '/pages/community-compare/community-compare'
    };
  },

  onShareTimeline() {
    return {
      title: '小区对比助手'
    };
  }
});
