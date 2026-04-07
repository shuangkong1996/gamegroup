// utils/request.js
// 网络请求封装

const config = require('./config');

const request = (options) => {
  return new Promise((resolve, reject) => {
    const app = getApp();
    const token = app.globalData.token;

    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };

    // 添加认证头
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    wx.request({
      url: `${config.baseUrl}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      timeout: options.timeout || 30000,
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data;
          if (data.code === 200) {
            resolve(data.data);
          } else if (data.code === 401) {
            // Token 失效，清除登录状态并跳转登录页
            app.clearUserInfo();
            wx.navigateTo({
              url: '/pages/login/login'
            });
            reject(new Error(data.message || '登录已过期'));
          } else {
            wx.showToast({
              title: data.message || '请求失败',
              icon: 'none'
            });
            reject(new Error(data.message));
          }
        } else {
          wx.showToast({
            title: '网络错误',
            icon: 'none'
          });
          reject(new Error('网络错误'));
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络连接失败',
          icon: 'none'
        });
        reject(err);
      }
    });
  });
};

// GET 请求
const get = (url, data) => {
  return request({
    url,
    method: 'GET',
    data
  });
};

// POST 请求
const post = (url, data) => {
  return request({
    url,
    method: 'POST',
    data
  });
};

// DELETE 请求
const del = (url, data) => {
  return request({
    url,
    method: 'DELETE',
    data
  });
};

module.exports = {
  request,
  get,
  post,
  del
};
