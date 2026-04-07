// utils/auth.js
// 认证相关工具

const { post } = require('./request');

// 微信登录
const wxLogin = () => {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          // 调用后端登录接口
          post('/auth/wx-login', {
            code: res.code
          }).then((data) => {
            const app = getApp();
            app.setUserInfo({
              userId: data.userId,
              nickname: data.nickname,
              avatarUrl: data.avatarUrl
            }, data.token);
            resolve(data);
          }).catch(reject);
        } else {
          reject(new Error('微信登录失败'));
        }
      },
      fail: reject
    });
  });
};

// 检查登录状态
const checkLogin = () => {
  const app = getApp();
  return app.isLoggedIn();
};

// 确保已登录
const ensureLogin = () => {
  return new Promise((resolve, reject) => {
    if (checkLogin()) {
      resolve();
    } else {
      wx.navigateTo({
        url: '/pages/login/login',
        success: () => reject(new Error('请先登录')),
        fail: reject
      });
    }
  });
};

module.exports = {
  wxLogin,
  checkLogin,
  ensureLogin
};
