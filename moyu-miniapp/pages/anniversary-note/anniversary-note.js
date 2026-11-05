const { staticResourceApi } = require('../../utils/api');

const DEFAULT_PHOTO = 'https://gamegroup.shuangkong.top/oss/game-images/791ac7d4-90bf-400e-98c0-40e66d3cb5e7.jpg';
const ADDRESS = '四川省遂宁市安居区拦江镇东平街20号玉泉大酒店';
const CALENDAR_WEEKS = [
  ['1', '2', '3', '4', '5', '6', '7'],
  ['8', '9', '10', '11', '12', '13', '14'],
  ['15', '16', '17', '18', '19', '20', '21'],
  ['22', '23', '24', '25', '26', '27', '28'],
  ['29', '30', '', '', '', '', '']
].map((days, weekIndex) => ({
  key: `week-${weekIndex}`,
  days: days.map((value, dayIndex) => ({ key: `day-${weekIndex}-${dayIndex}`, value }))
}));

Page({
  data: {
    photo: DEFAULT_PHOTO,
    address: ADDRESS,
    copied: false,
    weekdays: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
    calendarWeeks: CALENDAR_WEEKS
  },

  onLoad() {
    this.loadPhoto();
    wx.showShareMenu({
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  async loadPhoto() {
    try {
      const resources = await staticResourceApi.getByGroup('anniversary');
      const moment = resources.find((item) => item.resourceKey === 'moment_2');
      if (moment && moment.resourceUrl) {
        this.setData({ photo: moment.resourceUrl });
      }
    } catch (error) {
      console.error('加载纪念照片失败', error);
    }
  },

  copyAddress() {
    wx.setClipboardData({
      data: ADDRESS,
      success: () => {
        this.setData({ copied: true });
        setTimeout(() => this.setData({ copied: false }), 1800);
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '陈爽 & 邓睿 · 婚礼邀请',
      path: '/pages/anniversary-note/anniversary-note',
      imageUrl: this.data.photo
    };
  },

  onShareTimeline() {
    return {
      title: '陈爽 & 邓睿 · 婚礼邀请',
      imageUrl: this.data.photo
    };
  }
});
