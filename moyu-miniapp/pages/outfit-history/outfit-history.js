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
  }
});
