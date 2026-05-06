// pages/toilet/toilet.js
const app = getApp();
const { toiletApi } = require('../../utils/api');

Page({
  data: {
    isRunning: false,
    startTime: null, // 后端返回的 startTime (ISO 字符串)
    elapsedTime: 0,
    elapsedDisplay: '00:00',
    earnedMoney: '0.00',
    timer: null,
    sessionId: null, // 后端会话 ID
    isProcessing: false, // 防止重复点击

    // 历史记录
    history: [],
    totalTime: 0,
    totalMoney: 0,
    loading: false
  },

  onLoad() {
    this.syncBackendState();
    this.loadStatistics();
    this.loadHistory();
  },

  onShow() {
    // 每次显示时同步后端状态
    this.syncBackendState();
    if (this.data.isRunning) {
      this.startUpdateTimer();
    }
  },

  onUnload() {
    this.stopTimer();
  },

  onHide() {
    this.stopTimer();
  },

  // 同步后端状态
  async syncBackendState() {
    if (!app.isLoggedIn()) {
      // 未登录，使用本地存储
      this.loadHistory();
      return;
    }

    try {
      const session = await toiletApi.getCurrent();
      if (session && session.sessionId) {
        // 有进行中的会话，恢复显示
        // 使用当前时间减去已过时长来得到正确的 startTime
        const now = Date.now();
        const elapsedMs = (session.elapsedSeconds || 0) * 1000;
        const startTime = now - elapsedMs;
        
        this.setData({
          isRunning: true,
          startTime: startTime,
          sessionId: session.sessionId,
          elapsedTime: elapsedMs,
          elapsedDisplay: this.formatTimeFromSeconds(session.elapsedSeconds || 0),
          earnedMoney: session.earnedMoney ? session.earnedMoney.toFixed(2) : '0.00'
        });
        this.startUpdateTimer();
      } else {
        // 没有进行中的会话
        this.setData({
          isRunning: false,
          startTime: null,
          sessionId: null
        });
      }
      // 同步状态后加载历史记录
      this.loadHistory();
    } catch (err) {
      console.error('同步后端状态失败', err);
      // 失败时使用本地存储
      this.loadHistory();
    }
  },

  // 加载统计数据
  async loadStatistics() {
    if (!app.isLoggedIn()) {
      return;
    }

    try {
      const stats = await toiletApi.getStatistics();
      if (stats) {
        this.setData({
          totalTime: stats.totalDuration || 0,
          totalMoney: (stats.totalMoney || 0).toFixed(2)
        });
      }
    } catch (err) {
      console.error('加载统计数据失败', err);
    }
  },

  // 加载历史记录（兼容本地存储）
  loadHistory() {
    if (app.isLoggedIn()) {
      this.loadHistoryFromBackend();
    } else {
      this.loadHistoryFromLocal();
    }
  },

  // 从后端加载历史记录
  async loadHistoryFromBackend() {
    try {
      this.setData({ loading: true });
      const result = await toiletApi.getHistory(1, 20);
      if (result && result.content) {
        const history = result.content.map(record => ({
          date: this.formatDate(new Date(record.startTime)),
          duration: record.durationSeconds,
          earned: parseFloat(record.earnedMoney)
        }));

        this.setData({
          history: history
        });
      }
    } catch (err) {
      console.error('加载历史记录失败', err);
      // 失败时尝试从本地加载
      this.loadHistoryFromLocal();
    } finally {
      this.setData({ loading: false });
    }
  },

  // 从本地存储加载历史记录
  loadHistoryFromLocal() {
    const history = wx.getStorageSync('toiletHistory') || [];
    let totalTime = 0;
    let totalMoney = 0;

    history.forEach(item => {
      totalTime += item.duration;
      totalMoney += item.earned;
    });

    this.setData({
      history: history.slice(0, 10),
      totalTime,
      totalMoney: totalMoney.toFixed(2)
    });
  },

  // 开始/停止计时
  async toggleTimer() {
    // 防止重复点击
    if (this.data.isProcessing) {
      return;
    }

    if (this.data.isRunning) {
      await this.stopAndSave();
    } else {
      await this.startTimer();
    }
  },

  // 开始计时
  async startTimer() {
    // 防止重复点击
    if (this.data.isProcessing || this.data.isRunning) {
      return;
    }

    this.setData({ isProcessing: true });

    if (!app.isLoggedIn()) {
      // 未登录，使用本地计时
      this.startLocalTimer();
      this.setData({ isProcessing: false });
      return;
    }

    try {
      const monthlySalary = app.globalData.monthlySalary || 10000;
      const session = await toiletApi.start(monthlySalary);
      
      if (session) {
        // 新开始的会话，elapsedSeconds 应该是 0
        // 使用当前时间作为 startTime
        const now = Date.now();
        
        this.setData({
          isRunning: true,
          startTime: now,
          sessionId: session.sessionId,
          elapsedTime: 0,
          elapsedDisplay: '00:00',
          earnedMoney: '0.00',
          isProcessing: false
        });

        // 启动定时器更新显示
        this.startUpdateTimer();
        wx.vibrateShort({ type: 'medium' });
      } else {
        this.setData({ isProcessing: false });
      }
    } catch (err) {
      console.error('开始计时失败', err);
      wx.showToast({
        title: err.message || '开始计时失败',
        icon: 'none'
      });
      // 失败时使用本地计时
      this.startLocalTimer();
      this.setData({ isProcessing: false });
    }
  },

  // 启动定时器更新显示（用于已登录用户）
  startUpdateTimer() {
    // 先停止旧的定时器
    this.stopTimer();
    
    const timer = setInterval(() => {
      this.updateElapsed();
    }, 100);

    this.setData({ timer });
  },

  // 本地开始计时（未登录时使用）
  startLocalTimer() {
    const now = Date.now();
    this.setData({
      isRunning: true,
      startTime: now,
      elapsedTime: 0
    });

    const timer = setInterval(() => {
      this.updateElapsed();
    }, 100);

    this.setData({ timer });
    wx.vibrateShort({ type: 'medium' });
  },

  // 停止定时器
  stopTimer() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
      this.setData({ timer: null });
    }
  },

  // 结束计时并保存
  async stopAndSave() {
    // 防止重复点击
    if (this.data.isProcessing) {
      return;
    }

    this.setData({ isProcessing: true });
    this.stopTimer();

    if (!app.isLoggedIn()) {
      // 未登录，使用本地保存
      this.stopAndSaveLocal();
      this.setData({ isProcessing: false });
      return;
    }

    try {
      const response = await toiletApi.stop();
      
      if (response) {
        wx.showToast({
          title: `本次赚了 ¥${response.earnedMoney.toFixed(2)}`,
          icon: 'none',
          duration: 2000
        });

        // 刷新历史记录和统计数据
        this.loadHistory();
        this.loadStatistics();
      }

      this.setData({
        isRunning: false,
        startTime: null,
        sessionId: null,
        elapsedTime: 0,
        elapsedDisplay: '00:00',
        earnedMoney: '0.00',
        isProcessing: false
      });

      wx.vibrateShort({ type: 'light' });
    } catch (err) {
      console.error('结束计时失败', err);
      wx.showToast({
        title: err.message || '结束计时失败',
        icon: 'none'
      });
      // 失败时使用本地保存
      this.stopAndSaveLocal();
      this.setData({ isProcessing: false });
    }
  },

  // 本地保存（未登录时使用）
  stopAndSaveLocal() {
    const { elapsedTime, earnedMoney } = this.data;
    const duration = Math.floor(elapsedTime / 1000);

    if (duration > 0) {
      const record = {
        date: this.formatDate(new Date()),
        duration,
        earned: parseFloat(earnedMoney)
      };

      let history = wx.getStorageSync('toiletHistory') || [];
      history.unshift(record);
      history = history.slice(0, 50);
      wx.setStorageSync('toiletHistory', history);

      this.loadHistory();

      wx.showToast({
        title: `本次赚了 ¥${earnedMoney}`,
        icon: 'none',
        duration: 2000
      });
    }

    this.setData({
      isRunning: false,
      elapsedTime: 0,
      elapsedDisplay: '00:00',
      earnedMoney: '0.00'
    });

    wx.vibrateShort({ type: 'light' });
  },

  // 更新已过时长
  updateElapsed() {
    const { startTime } = this.data;
    if (!startTime) {
      return;
    }

    const now = Date.now();
    const elapsed = now - startTime;

    const minutes = Math.floor(elapsed / 60000);
    const seconds = Math.floor((elapsed % 60000) / 1000);
    const display = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

    // 计算赚了多少钱
    const monthlySalary = app.globalData.monthlySalary || 10000;
    const secondRate = monthlySalary / 22 / 8 / 60 / 60;
    const earned = (elapsed / 1000) * secondRate;

    this.setData({
      elapsedTime: elapsed,
      elapsedDisplay: display,
      earnedMoney: earned.toFixed(2)
    });
  },

  // 格式化日期
  formatDate(date) {
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hour = date.getHours().toString().padStart(2, '0');
    const minute = date.getMinutes().toString().padStart(2, '0');
    return `${month}/${day} ${hour}:${minute}`;
  },

  // 格式化时长
  formatDuration(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}分${secs}秒`;
  },

  // 从秒数格式化时间显示 (MM:SS)
  formatTimeFromSeconds(totalSeconds) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  },

  // 清空历史
  clearHistory() {
    wx.showModal({
      title: '确认清空',
      content: '确定要清空所有记录吗？',
      success: (res) => {
        if (res.confirm) {
          if (app.isLoggedIn()) {
            // 已登录，提示用户后端数据无法清空
            wx.showToast({
              title: '后端数据无法清空',
              icon: 'none'
            });
          } else {
            // 未登录，清空本地存储
            wx.removeStorageSync('toiletHistory');
            this.setData({
              history: [],
              totalTime: 0,
              totalMoney: 0
            });
            wx.showToast({
              title: '已清空',
              icon: 'success'
            });
          }
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '厕所时间',
      path: '/pages/toilet/toilet'
    };
  },

  onShareTimeline() {
    return {
      title: '厕所时间'
    };
  }
});
