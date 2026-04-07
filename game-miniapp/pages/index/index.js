// pages/index/index.js
const { gameApi } = require('../../utils/api');
const { checkLogin } = require('../../utils/auth');
const { getImageUrl, getDefaultImageUrl } = require('../../utils/image');

Page({
  data: {
    games: [],
    loading: true
  },

  onLoad() {
    this.loadGames();
  },

  onShow() {
    // 每次显示时检查登录状态
    this.setData({
      isLoggedIn: checkLogin()
    });
  },

  onPullDownRefresh() {
    this.loadGames().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 加载游戏列表
  async loadGames() {
    this.setData({ loading: true });
    try {
      const games = await gameApi.getList();
      // 转换图片路径为完整URL，如果没有则使用默认图片
      const gamesWithImageUrl = (games || []).map(game => ({
        ...game,
        icon: game.icon ? getImageUrl(game.icon) : getDefaultImageUrl('game')
      }));
      this.setData({
        games: gamesWithImageUrl,
        loading: false
      });
    } catch (err) {
      console.error('加载游戏列表失败', err);
      this.setData({ loading: false });
    }
  },

  // 点击游戏卡片
  onGameTap(e) {
    const { id, name } = e.currentTarget.dataset;

    if (!checkLogin()) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再开始游戏',
        confirmText: '去登录',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            });
          }
        }
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/match/match?gameId=${id}&gameName=${encodeURIComponent(name)}`
    });
  }
});
