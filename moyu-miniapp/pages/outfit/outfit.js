const { wardrobeApi, authApi } = require('../../utils/api');

Page({
  data: {
    loading: true,
    refreshing: false,
    savingCity: false,
    outfit: null,
    city: '',
    settingsLoaded: false
  },

  async onLoad() {
    await this.loadSettings();
    this.loadOutfit();
  },

  async loadSettings() {
    try {
      const settings = await authApi.getSettings();
      this.setData({
        city: settings.city || '',
        settingsLoaded: true
      });
    } catch (err) {
      console.error('加载城市设置失败', err);
      this.setData({ settingsLoaded: true });
    }
  },

  async loadOutfit() {
    try {
      this.setData({ loading: true });
      const outfit = await wardrobeApi.getDailyOutfit();
      this.setData({ outfit });
    } catch (err) {
      console.error('加载搭配失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  async onRefresh() {
    if (this.data.refreshing) return;
    try {
      this.setData({ refreshing: true });
      const outfit = await wardrobeApi.refreshDailyOutfit();
      this.setData({ outfit });
    } catch (err) {
      console.error('刷新搭配失败', err);
    } finally {
      this.setData({ refreshing: false });
    }
  },

  async onAdopt() {
    const { outfit } = this.data;
    if (!outfit || !outfit.planId) return;
    try {
      await wardrobeApi.adoptOutfit(outfit.planId, {});
      wx.showToast({ title: '已加入搭配记录', icon: 'success' });
      this.loadOutfit();
    } catch (err) {
      console.error('采纳搭配失败', err);
    }
  },

  onCityInput(e) {
    this.setData({ city: e.detail.value || '' });
  },

  async onSaveCity() {
    if (this.data.savingCity) return;

    try {
      this.setData({ savingCity: true });
      await authApi.updateSettings({ city: this.data.city });
      wx.showToast({ title: '城市已保存', icon: 'success' });
      await this.onRefresh();
    } catch (err) {
      console.error('保存城市失败', err);
    } finally {
      this.setData({ savingCity: false });
    }
  }
});
