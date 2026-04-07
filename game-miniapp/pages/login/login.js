// pages/login/login.js
const { wxLogin } = require('../../utils/auth');

Page({
  data: {
    loading: false
  },

  // 微信登录
  async onLogin() {
    if (this.data.loading) return;

    this.setData({ loading: true });

    try {
      await wxLogin();

      wx.showToast({
        title: '登录成功',
        icon: 'success'
      });

      // 返回上一页或跳转首页
      const pages = getCurrentPages();
      if (pages.length > 1) {
        wx.navigateBack();
      } else {
        wx.switchTab({
          url: '/pages/index/index'
        });
      }
    } catch (err) {
      console.error('登录失败', err);
      wx.showToast({
        title: err.message || '登录失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  }
});
