// 节假日数据（2025年）
const holidays2025 = [
  { name: '元旦', date: '2025-01-01', emoji: '🎉' },
  { name: '春节', date: '2025-01-29', emoji: '🧧' },
  { name: '清明节', date: '2025-04-04', emoji: '🌸' },
  { name: '劳动节', date: '2025-05-01', emoji: '💪' },
  { name: '端午节', date: '2025-05-31', emoji: '🐲' },
  { name: '中秋节', date: '2025-10-06', emoji: '🥮' },
  { name: '国庆节', date: '2025-10-01', emoji: '🇨🇳' }
];

// 节假日数据（2026年）
const holidays2026 = [
  { name: '元旦', date: '2026-01-01', emoji: '🎉' },
  { name: '春节', date: '2026-02-17', emoji: '🧧' },
  { name: '清明节', date: '2026-04-05', emoji: '🌸' },
  { name: '劳动节', date: '2026-05-01', emoji: '💪' },
  { name: '端午节', date: '2026-06-19', emoji: '🐲' },
  { name: '中秋节', date: '2026-09-25', emoji: '🥮' },
  { name: '国庆节', date: '2026-10-01', emoji: '🇨🇳' }
];

/**
 * 获取即将到来的节假日列表
 */
function getUpcomingHolidays() {
  const now = new Date();
  const allHolidays = [...holidays2025, ...holidays2026];

  return allHolidays
    .map(h => {
      const holidayDate = new Date(h.date + 'T00:00:00');
      const diffTime = holidayDate.getTime() - now.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      return {
        ...h,
        daysLeft: diffDays
      };
    })
    .filter(h => h.daysLeft > 0)
    .sort((a, b) => a.daysLeft - b.daysLeft)
    .slice(0, 5);
}

/**
 * 计算下班倒计时
 */
function getOffWorkCountdown(offWorkHour = 18, offWorkMinute = 0) {
  const now = new Date();
  const offWork = new Date();
  offWork.setHours(offWorkHour, offWorkMinute, 0, 0);

  let diff = offWork.getTime() - now.getTime();

  // 如果已经过了下班时间
  if (diff <= 0) {
    return {
      isOffWork: true,
      hours: 0,
      minutes: 0,
      seconds: 0,
      text: '已下班，快跑！'
    };
  }

  const hours = Math.floor(diff / (1000 * 60 * 60));
  diff -= hours * 1000 * 60 * 60;
  const minutes = Math.floor(diff / (1000 * 60));
  diff -= minutes * 1000 * 60;
  const seconds = Math.floor(diff / 1000);

  return {
    isOffWork: false,
    hours,
    minutes,
    seconds,
    text: `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  };
}

/**
 * 计算周末倒计时
 */
function getWeekendCountdown() {
  const now = new Date();
  const dayOfWeek = now.getDay(); // 0=周日, 1-6=周一到周六

  if (dayOfWeek === 0 || dayOfWeek === 6) {
    return { isWeekend: true, daysLeft: 0, text: '今天就是周末！' };
  }

  const daysLeft = 6 - dayOfWeek;
  return {
    isWeekend: false,
    daysLeft,
    text: `还有 ${daysLeft} 天`
  };
}

module.exports = {
  getUpcomingHolidays,
  getOffWorkCountdown,
  getWeekendCountdown
};
