// pages/salary/salary.js
const app = getApp();

Page({
  data: {
    monthlySalary: 10000,
    workDays: 22,
    workHours: 8,

    // 计算结果
    hourlyRate: 0,
    minuteRate: 0,
    secondRate: 0,

    // 今日已赚
    todayEarned: 0,
    todayEarnedDisplay: '0.00',

    // 计时器
    timer: null,
    workStartTime: null
  },

  onLoad() {
    const monthlySalary = app.globalData.monthlySalary || 10000;
    this.setData({ monthlySalary });
    this.calculateRates();
    this.startEarning();
  },

  onUnload() {
    this.stopTimer();
  },

  onHide() {
    this.stopTimer();
  },

  onShow() {
    if (!this.data.timer) {
      this.startEarning();
    }
  },

  calculateRates() {
    const { monthlySalary, workDays, workHours } = this.data;
    const hourlyRate = monthlySalary / workDays / workHours;
    const minuteRate = hourlyRate / 60;
    const secondRate = minuteRate / 60;

    this.setData({
      hourlyRate: hourlyRate.toFixed(2),
      minuteRate: minuteRate.toFixed(4),
      secondRate: secondRate.toFixed(6)
    });
  },

  startEarning() {
    this.stopTimer();

    // 假设9点上班
    const now = new Date();
    const workStart = new Date();
    workStart.setHours(9, 0, 0, 0);

    // 如果还没到上班时间，从0开始
    if (now < workStart) {
      this.setData({
        workStartTime: now,
        todayEarned: 0
      });
    } else {
      this.setData({
        workStartTime: workStart
      });
    }

    const timer = setInterval(() => {
      this.updateEarnings();
    }, 100);

    this.setData({ timer });
    this.updateEarnings();
  },

  stopTimer() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
      this.setData({ timer: null });
    }
  },

  updateEarnings() {
    const { workStartTime, monthlySalary, workDays, workHours } = this.data;
    const now = new Date();

    // 计算工作了多少秒
    let workedSeconds = (now - workStartTime) / 1000;

    // 限制最大工作时间（不超过设定的每日工作时长）
    const maxSeconds = workHours * 60 * 60;
    if (workedSeconds > maxSeconds) {
      workedSeconds = maxSeconds;
    }
    if (workedSeconds < 0) {
      workedSeconds = 0;
    }

    // 计算已赚金额
    const secondRate = monthlySalary / workDays / workHours / 60 / 60;
    const todayEarned = workedSeconds * secondRate;

    this.setData({
      todayEarned,
      todayEarnedDisplay: todayEarned.toFixed(4)
    });
  },

  // 修改月薪
  onSalaryInput(e) {
    const value = parseInt(e.detail.value) || 0;
    this.setData({ monthlySalary: value });
  },

  onSalaryBlur() {
    const { monthlySalary } = this.data;
    app.globalData.monthlySalary = monthlySalary;
    wx.setStorageSync('monthlySalary', monthlySalary);
    this.calculateRates();
    this.startEarning();
  },

  // 修改工作天数
  onWorkDaysChange(e) {
    const workDays = parseInt(e.detail.value) || 22;
    this.setData({ workDays });
    this.calculateRates();
  },

  // 修改工作时长
  onWorkHoursChange(e) {
    const workHours = parseInt(e.detail.value) || 8;
    this.setData({ workHours });
    this.calculateRates();
  },

  onShareAppMessage() {
    return {
      title: '工资计算器',
      path: '/pages/salary/salary'
    };
  },

  onShareTimeline() {
    return {
      title: '工资计算器'
    };
  }
});
