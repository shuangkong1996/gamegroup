// pages/index/index.js
const { getUpcomingHolidays, getOffWorkCountdown, getWeekendCountdown } = require('../../utils/holiday');
const { getTodayQuote } = require('../../utils/quotes');
const { quoteApi, authApi } = require('../../utils/api');

const app = getApp();

Page({
  data: {
    // 下班倒计时
    countdown: {
      isOffWork: false,
      text: '--:--:--'
    },
    offWorkTime: '18:00',

    // 周末倒计时
    weekend: {
      isWeekend: false,
      text: ''
    },

    // 今日专属语录
    quote: {
      content: '',
      emoji: ''
    },
    quoteLoading: false,

    // 节假日列表
    holidays: [],

    // 当前日期
    today: '',
    weekday: '',

    // 定时器
    timer: null
  },

  onLoad() {
    this.initData();
    this.startTimer();
    this.loadTodayQuote();
    // 延迟加载用户设置，确保登录完成
    setTimeout(() => {
      this.loadUserSettings().then(loaded => {
        if (loaded) {
          // 如果成功加载了设置，更新页面显示
          const { offWorkHour, offWorkMinute } = app.globalData;
          const offWorkTime = `${offWorkHour.toString().padStart(2, '0')}:${offWorkMinute.toString().padStart(2, '0')}`;
          this.setData({ offWorkTime });
          this.updateCountdown();
        }
      });
    }, 500);
  },

  onShow() {
    // 每次显示时重启定时器
    this.startTimer();
    // 延迟检查登录状态，确保登录完成后再加载用户设置
    setTimeout(() => {
      this.checkAndLoadSettings();
    }, 500);
  },

  onUnload() {
    this.stopTimer();
  },

  onHide() {
    this.stopTimer();
  },

  // 加载用户设置
  async loadUserSettings() {
    if (!app.isLoggedIn()) {
      // 未登录，使用本地存储的默认值
      return false;
    }

    try {
      const settings = await authApi.getSettings();
      if (settings) {
        const hour = settings.offWorkHour !== null && settings.offWorkHour !== undefined ? settings.offWorkHour : 18;
        const minute = settings.offWorkMinute !== null && settings.offWorkMinute !== undefined ? settings.offWorkMinute : 0;
        
        app.globalData.offWorkHour = hour;
        app.globalData.offWorkMinute = minute;
        wx.setStorageSync('offWorkHour', hour);
        wx.setStorageSync('offWorkMinute', minute);
        
        return true; // 返回 true 表示成功加载
      }
    } catch (err) {
      console.error('加载用户设置失败', err);
      // 失败时使用本地存储的默认值
    }
    return false;
  },

  // 检查并加载用户设置（用于登录后刷新）
  async checkAndLoadSettings() {
    if (app.isLoggedIn()) {
      const loaded = await this.loadUserSettings();
      if (loaded) {
        // 重新更新页面显示
        const { offWorkHour, offWorkMinute } = app.globalData;
        const offWorkTime = `${offWorkHour.toString().padStart(2, '0')}:${offWorkMinute.toString().padStart(2, '0')}`;
        this.setData({ offWorkTime });
        this.updateCountdown();
      }
    }
  },

  initData() {
    // 设置日期
    const now = new Date();
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    const today = `${now.getMonth() + 1}月${now.getDate()}日`;
    const weekday = weekdays[now.getDay()];

    // 下班时间
    const { offWorkHour, offWorkMinute } = app.globalData;
    const offWorkTime = `${offWorkHour.toString().padStart(2, '0')}:${offWorkMinute.toString().padStart(2, '0')}`;

    // 节假日
    let holidays = getUpcomingHolidays();
    
    // 在第一条固定添加"发财倒计时     明天"
    const fortuneCountdown = {
      name: '发财倒计时',
      emoji: '💰',
      daysLeft: '明天'
    };
    
    // 将"发财倒计时"插入到第一条
    holidays = [fortuneCountdown, ...holidays];

    // 周末
    const weekend = getWeekendCountdown();

    // 先用本地语录占位
    const quote = getTodayQuote();

    this.setData({
      today,
      weekday,
      offWorkTime,
      holidays,
      weekend,
      quote: {
        content: quote.text,
        emoji: quote.emoji
      }
    });

    this.updateCountdown();
  },

  // 从 API 加载今日专属语录
  async loadTodayQuote() {
    // 等待一下确保登录完成
    setTimeout(async () => {
      try {
        this.setData({ quoteLoading: true });

        let quote;
        if (app.isLoggedIn()) {
          // 已登录，获取今日专属语录
          quote = await quoteApi.getToday();
        } else {
          // 未登录，获取随机语录
          quote = await quoteApi.getRandom();
        }

        if (quote) {
          this.setData({
            quote: {
              content: quote.content,
              emoji: quote.emoji
            }
          });
        }
      } catch (err) {
        console.error('获取语录失败', err);
        // 失败时使用本地语录，已在 initData 中设置
      } finally {
        this.setData({ quoteLoading: false });
      }
    }, 500);
  },

  startTimer() {
    this.stopTimer();
    const timer = setInterval(() => {
      this.updateCountdown();
    }, 1000);
    this.setData({ timer });
  },

  stopTimer() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
      this.setData({ timer: null });
    }
  },

  updateCountdown() {
    const { offWorkHour, offWorkMinute } = app.globalData;
    const countdown = getOffWorkCountdown(offWorkHour, offWorkMinute);
    this.setData({ countdown });
  },

  // 设置下班时间
  onSetOffWorkTime() {
    console.log('点击了设置下班时间');
    // 微信小程序的 showActionSheet 最多支持 6 个选项
    wx.showActionSheet({
      itemList: ['17:00', '17:30', '18:00', '18:30', '19:00', '19:30'],
      success: async (res) => {
        console.log('选择了时间，索引：', res.tapIndex);
        const times = ['17:00', '17:30', '18:00', '18:30', '19:00', '19:30'];
        const selected = times[res.tapIndex];
        const [hour, minute] = selected.split(':').map(Number);

        // 更新本地数据
        app.globalData.offWorkHour = hour;
        app.globalData.offWorkMinute = minute;
        wx.setStorageSync('offWorkHour', hour);
        wx.setStorageSync('offWorkMinute', minute);

        this.setData({ offWorkTime: selected });
        this.updateCountdown();

        // 如果已登录，保存到后端
        if (app.isLoggedIn()) {
          try {
            await authApi.updateSettings(hour, minute);
          } catch (err) {
            console.error('保存用户设置失败', err);
            // 即使保存失败，本地设置也已经更新，不影响使用
          }
        }

        wx.showToast({
          title: `已设置 ${selected} 下班`,
          icon: 'none'
        });
      },
      fail: (err) => {
        console.error('显示选择器失败', err);
        // 如果用户取消选择，不显示错误
        if (err.errMsg && !err.errMsg.includes('cancel')) {
          wx.showToast({
            title: '操作失败',
            icon: 'none'
          });
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '莫庾工具集',
      path: '/pages/index/index'
    };
  },

  onShareTimeline() {
    return {
      title: '莫庾工具集'
    };
  }
});
