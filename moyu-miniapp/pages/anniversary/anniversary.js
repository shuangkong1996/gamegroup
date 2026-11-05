// 图片走 MinIO，不打包进小程序包体
const { staticResourceApi } = require('../../utils/api');
const { buildShareTitle, renderInvitationCards } = require('../../utils/shareCard');

const INVITATION = {
  groom: '邓睿',
  bride: '陈爽',
  solarDate: '2026年11月16日',
  weekday: '星期一',
  lunarDate: '农历丙午年十月初八',
  venue: '玉泉大酒店',
  address: '四川省遂宁市安居区拦江镇东平街20号玉泉大酒店',
  cover: 'https://gamegroup.shuangkong.top/oss/game-images/94cab691-5968-413b-b7ee-61584e779fd4.jpg',
  portraits: [
    { name: '邓睿', label: 'THE GROOM', image: 'https://gamegroup.shuangkong.top/oss/game-images/160bbc40-af03-4a49-93fa-a0196a3c61ba.jpg' },
    { name: '陈爽', label: 'THE BRIDE', image: 'https://gamegroup.shuangkong.top/oss/game-images/9e82906d-7d81-4c21-8510-c1fa6bcc7a20.jpg' }
  ],
  momentPairs: [
    {
      layout: 'stack',
      photos: [
        'https://gamegroup.shuangkong.top/oss/game-images/f2f64936-24cf-4c44-94fe-b824277e1cd8.jpg',
        'https://gamegroup.shuangkong.top/oss/game-images/791ac7d4-90bf-400e-98c0-40e66d3cb5e7.jpg'
      ]
    },
    {
      layout: 'offset',
      photos: [
        'https://gamegroup.shuangkong.top/oss/game-images/c7a2b08e-543e-4677-80a1-a37734f843d3.jpg',
        'https://gamegroup.shuangkong.top/oss/game-images/9ad12286-a119-4bc0-a8f7-f2a8d08dea57.jpg'
      ]
    }
  ],
  shareLines: [
    '诚邀您参加',
    '爱子与儿媳的婚礼，',
    '见证他们的爱情之路，',
    '共享美好时刻'
  ]
};

Page({
  data: {
    invitation: INVITATION,
    sheetHeight: 0,
    scrollIntoView: '',
    copied: false,
    visibleMap: {}
  },

  onLoad() {
    this.loadStaticResources();
    wx.showShareMenu({
      menus: ['shareAppMessage', 'shareTimeline']
    });

    wx.hideTabBar({
      animation: false,
      complete: () => this.measureSheetHeight()
    });
    // 兜底：hideTabBar 未回调时也量一次
    this.measureSheetHeight();
  },

  async loadStaticResources() {
    try {
      const resources = await staticResourceApi.getByGroup('anniversary');
      const resourceUrls = resources.reduce((result, item) => {
        result[item.resourceKey] = item.resourceUrl;
        return result;
      }, {});
      const invitation = this.data.invitation;

      this.setData({
        invitation: {
          ...invitation,
          cover: resourceUrls.cover || invitation.cover,
          portraits: invitation.portraits.map((item, index) => ({
            ...item,
            image: resourceUrls[index === 0 ? 'portrait_groom' : 'portrait_bride'] || item.image
          })),
          momentPairs: invitation.momentPairs.map((pair, pairIndex) => ({
            ...pair,
            photos: pair.photos.map((photo, photoIndex) => (
              resourceUrls[`moment_${pairIndex * 2 + photoIndex + 1}`] || photo
            ))
          }))
        }
      }, () => {
        if (this.canvasReady) this.scheduleShareCards();
      });
    } catch (error) {
      console.error('加载纪念册图片资源失败', error);
    }
  },

  onReady() {
    this.canvasReady = true;
    this.measureSheetHeight();
    this.observeRevealTargets();
    this.scheduleShareCards();
  },

  measureSheetHeight() {
    const query = wx.createSelectorQuery().in(this);
    query.select('.album-root').boundingClientRect((rect) => {
      const fallback = (wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()).windowHeight || 667;
      const height = Math.round((rect && rect.height) || fallback);
      if (!height || height === this.sheetHeight) return;
      this.sheetHeight = height;
      this.setData({ sheetHeight: height });
    }).exec();
  },

  onUnload() {
    if (this.revealObserver) {
      this.revealObserver.disconnect();
    }
    wx.showTabBar({ animation: false });
  },

  observeRevealTargets() {
    this.revealObserver = wx.createIntersectionObserver(this, {
      thresholds: [0.08, 0.22],
      observeAll: true
    });

    this.revealObserver.relativeToViewport().observe('.reveal-target', (result) => {
      const key = result.dataset.revealKey;
      if (!key) return;
      const visible = result.intersectionRatio >= 0.08;
      if (this.data.visibleMap[key] === visible) return;
      this.setData({
        visibleMap: { ...this.data.visibleMap, [key]: visible }
      });
    });
  },

  openAlbum() {
    this.setData({ scrollIntoView: '' }, () => {
      this.setData({ scrollIntoView: 'story' });
    });
  },

  previewPhoto(event) {
    const current = event.currentTarget.dataset.src;
    const { invitation } = this.data;
    wx.previewImage({
      current,
      urls: [
        invitation.cover,
        ...invitation.portraits.map((item) => item.image),
        ...invitation.momentPairs.flatMap((pair) => pair.photos)
      ]
    });
  },

  copyAddress() {
    wx.setClipboardData({
      data: INVITATION.address,
      success: () => {
        this.setData({ copied: true });
        setTimeout(() => this.setData({ copied: false }), 1800);
      }
    });
  },

  queryCanvas(selector, attempt = 0) {
    return new Promise((resolve, reject) => {
      wx.createSelectorQuery()
        .in(this)
        .select(selector)
        .fields({ node: true, size: true })
        .exec((res) => {
          const node = res && res[0] && res[0].node;
          if (node) {
            resolve(node);
            return;
          }
          if (attempt >= 2) {
            reject(new Error(`未找到分享画布 ${selector}`));
            return;
          }
          wx.nextTick(() => {
            this.queryCanvas(selector, attempt + 1).then(resolve, reject);
          });
        });
    });
  },

  scheduleShareCards() {
    const run = () => this.renderShareCards().catch((error) => {
      console.error('生成分享卡片失败', error);
    });
    this.shareTask = Promise.resolve(this.shareTask).then(run, run);
    return this.shareTask;
  },

  async renderShareCards() {
    const invitation = this.data.invitation;
    const key = invitation.cover;
    if (this.renderedCover === key && this.shareCardPath && this.timelineCardPath) return;

    const [message, timeline] = await Promise.all([
      this.queryCanvas('#shareCanvas'),
      this.queryCanvas('#timelineCanvas')
    ]);
    const cards = await renderInvitationCards({ message, timeline }, invitation);
    if (this.data.invitation.cover !== key) return;
    this.renderedCover = key;
    this.shareCardPath = cards.messagePath;
    this.timelineCardPath = cards.timelinePath;
  },

  onShareAppMessage() {
    const path = '/pages/anniversary/anniversary';
    const ready = this.shareTask || this.scheduleShareCards();
    const fallback = () => ({
      title: buildShareTitle(this.data.invitation),
      path,
      imageUrl: this.shareCardPath || this.data.invitation.cover
    });
    return {
      ...fallback(),
      promise: ready.then(fallback)
    };
  },

  onShareTimeline() {
    return {
      title: buildShareTitle(this.data.invitation),
      query: '',
      imageUrl: this.timelineCardPath || this.data.invitation.cover
    };
  }
});
