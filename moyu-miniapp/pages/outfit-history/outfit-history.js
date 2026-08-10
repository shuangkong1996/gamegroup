const { wardrobeApi } = require('../../utils/api');

Page({
  data: {
    loading: true,
    records: []
  },

  onLoad() {
    this.loadRecords();
  },

  onShow() {
    this.loadRecords();
  },

  async loadRecords() {
    try {
      this.setData({ loading: true });
      const records = await wardrobeApi.getOutfitRecords();
      this.setData({ records });
    } catch (err) {
      console.error('加载搭配记录失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  onOpenRecord(e) {
    const renderTaskId = e.currentTarget.dataset.renderTaskId;
    if (!renderTaskId) {
      wx.showToast({ title: '该记录暂不支持详情', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/pages/outfit/outfit?taskId=${renderTaskId}&readonly=1`
    });
  }
});
