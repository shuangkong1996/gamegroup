// pages/profile/profile.js
const { checkLogin } = require('../../utils/auth');
const { getDefaultImageUrl } = require('../../utils/image');

Page({
  data: {
    isLoggedIn: false,
    userInfo: null
  },

  onShow() {
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const app = getApp();
    const isLoggedIn = checkLogin();

    let userInfo = null;
    if (isLoggedIn && app.globalData.userInfo) {
      userInfo = {
        ...app.globalData.userInfo,
        // 如果没有头像，使用默认头像
        avatarUrl: app.globalData.userInfo.avatarUrl || getDefaultImageUrl('avatar')
      };
    }

    this.setData({
      isLoggedIn,
      userInfo
    });
  },

  // 去登录
  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          const app = getApp();
          app.clearUserInfo();

          this.setData({
            isLoggedIn: false,
            userInfo: null
          });

          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          });
        }
      }
    });
  },

  // 菜单项点击
  onMenuTap(e) {
    const { type } = e.currentTarget.dataset;

    switch (type) {
      case 'history':
        wx.showToast({ title: '功能开发中', icon: 'none' });
        break;
      case 'ledger':
        if (!checkLogin()) {
          wx.navigateTo({
            url: '/pages/login/login'
          });
          return;
        }
        wx.navigateTo({
          url: '/pages/ledger/ledger'
        });
        break;
      case 'settings':
        wx.showToast({ title: '功能开发中', icon: 'none' });
        break;
      case 'about':
        wx.showToast({ title: '功能开发中', icon: 'none' });
        break;
      case 'feedback':
        wx.showToast({ title: '功能开发中', icon: 'none' });
        break;
    }
  }
});
