// pages/match/match.js
const { matchApi, gameApi } = require('../../utils/api');
const { getImageUrl, getDefaultImageUrl } = require('../../utils/image');

Page({
  data: {
    gameId: null,
    gameName: '',
    game: null,
    mode: 'select',  // select | create | join | waiting | matched
    verifyCode: '',
    inputCode: '',
    room: null,
    loading: false,
    polling: null
  },

  onLoad(options) {
    const gameId = parseInt(options.gameId);
    const gameName = decodeURIComponent(options.gameName || '');

    this.setData({
      gameId,
      gameName
    });

    this.loadGameDetail(gameId);
  },

  onUnload() {
    this.stopPolling();
  },

  // 加载游戏详情
  async loadGameDetail(gameId) {
    try {
      const game = await gameApi.getDetail(gameId);
      // 转换图片路径为完整URL，如果没有则使用默认图片
      if (game) {
        game.icon = game.icon ? getImageUrl(game.icon) : getDefaultImageUrl('game');
      }
      this.setData({ game });
    } catch (err) {
      console.error('加载游戏详情失败', err);
    }
  },

  // 选择创建房间
  onCreateMode() {
    this.setData({ mode: 'create' });
    this.createRoom();
  },

  // 选择加入房间
  onJoinMode() {
    this.setData({ mode: 'join' });
  },

  // 创建房间
  async createRoom() {
    this.setData({ loading: true });

    try {
      const room = await matchApi.create(this.data.gameId);
      this.setData({
        room,
        verifyCode: room.verifyCode,
        mode: 'waiting',
        loading: false
      });

      // 开始轮询房间状态
      this.startPolling(room.id);
    } catch (err) {
      console.error('创建房间失败', err);
      this.setData({
        mode: 'select',
        loading: false
      });
    }
  },

  // 输入验证码
  onCodeInput(e) {
    this.setData({
      inputCode: e.detail.value
    });
  },

  // 加入房间
  async joinRoom() {
    const code = this.data.inputCode.trim();
    if (!code) {
      wx.showToast({
        title: '请输入验证号',
        icon: 'none'
      });
      return;
    }

    if (code.length !== 6) {
      wx.showToast({
        title: '验证号为6位数字',
        icon: 'none'
      });
      return;
    }

    this.setData({ loading: true });

    try {
      const room = await matchApi.join(code);
      this.setData({
        room,
        mode: room.status === 'MATCHED' ? 'matched' : 'waiting',
        loading: false
      });

      if (room.status !== 'MATCHED') {
        this.startPolling(room.id);
      }
    } catch (err) {
      console.error('加入房间失败', err);
      this.setData({ loading: false });
    }
  },

  // 开始轮询
  startPolling(roomId) {
    this.stopPolling();

    const polling = setInterval(async () => {
      try {
        const room = await matchApi.getStatus(roomId);
        this.setData({ room });

        if (room.status === 'MATCHED') {
          this.stopPolling();
          this.setData({ mode: 'matched' });

          wx.showToast({
            title: '匹配成功！',
            icon: 'success'
          });
        } else if (room.status === 'EXPIRED') {
          this.stopPolling();
          wx.showToast({
            title: '房间已过期',
            icon: 'none'
          });
          this.setData({ mode: 'select' });
        }
      } catch (err) {
        console.error('获取房间状态失败', err);
      }
    }, 2000);

    this.setData({ polling });
  },

  // 停止轮询
  stopPolling() {
    if (this.data.polling) {
      clearInterval(this.data.polling);
      this.setData({ polling: null });
    }
  },

  // 取消匹配
  async cancelMatch() {
    const room = this.data.room;
    if (!room) return;

    try {
      await matchApi.cancel(room.id);
      this.stopPolling();
      this.setData({
        mode: 'select',
        room: null,
        verifyCode: ''
      });

      wx.showToast({
        title: '已取消',
        icon: 'none'
      });
    } catch (err) {
      console.error('取消匹配失败', err);
    }
  },

  // 复制验证码
  copyCode() {
    wx.setClipboardData({
      data: this.data.verifyCode,
      success: () => {
        wx.showToast({
          title: '已复制',
          icon: 'success'
        });
      }
    });
  },

  // 开始游戏
  startGame() {
    const { room, gameId } = this.data;
    if (!room) return;

    // 石头剪刀布游戏 (gameId = 2)
    if (gameId === 2) {
      wx.navigateTo({
        url: `/pages/rps/rps?roomId=${room.id}`
      });
    } else {
      wx.showToast({
        title: '该游戏暂未开放',
        icon: 'none'
      });
    }
  },

  // 返回选择
  backToSelect() {
    this.stopPolling();
    this.setData({
      mode: 'select',
      room: null,
      verifyCode: '',
      inputCode: ''
    });
  }
});
