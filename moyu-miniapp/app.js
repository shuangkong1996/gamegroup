// app.js
const { authApi } = require('./utils/api');

App({
  globalData: {
    // 默认下班时间 18:00
    offWorkHour: 18,
    offWorkMinute: 0,
    // 月薪（用于计算时薪）
    monthlySalary: 10000,
    // 登录 token
    token: null
  },

  onLaunch() {
    // 读取本地存储的设置
    const offWorkHour = wx.getStorageSync('offWorkHour');
    const offWorkMinute = wx.getStorageSync('offWorkMinute');
    const monthlySalary = wx.getStorageSync('monthlySalary');
    const token = wx.getStorageSync('token');

    if (offWorkHour !== '') {
      this.globalData.offWorkHour = offWorkHour;
    }
    if (offWorkMinute !== '') {
      this.globalData.offWorkMinute = offWorkMinute;
    }
    if (monthlySalary !== '') {
      this.globalData.monthlySalary = monthlySalary;
    }
    if (token) {
      this.globalData.token = token;
    }

    // 静默登录
    this.silentLogin();
  },

  // 静默登录
  silentLogin() {
    wx.login({
      success: (res) => {
        if (res.code) {
          authApi.wxLogin(res.code)
            .then((data) => {
              this.globalData.token = data.token;
              wx.setStorageSync('token', data.token);
            })
            .catch((err) => {
              console.error('静默登录失败', err);
            });
        }
      },
      fail: (err) => {
        console.error('wx.login 失败', err);
      }
    });
  },

  // 检查是否已登录
  isLoggedIn() {
    return !!this.globalData.token;
  }
});
