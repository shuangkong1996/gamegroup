// pages/rps/rps.js
// 石头剪刀布游戏页面
const { gamePlayApi } = require('../../utils/api');

Page({
  data: {
    roomId: null,
    session: null,
    loading: true,
    choosing: false,
    polling: null,
    showResult: false,
    lastRoundResult: null
  },

  onLoad(options) {
    const roomId = parseInt(options.roomId);
    this.setData({ roomId });
    this.startGame(roomId);
  },

  onUnload() {
    this.stopPolling();
  },

  // 开始游戏
  async startGame(roomId) {
    try {
      const session = await gamePlayApi.start(roomId);
      this.setData({
        session,
        loading: false
      });
      this.startPolling();
    } catch (err) {
      console.error('开始游戏失败', err);
      wx.showToast({
        title: err.message || '开始游戏失败',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  // 轮询游戏状态
  startPolling() {
    this.stopPolling();

    const polling = setInterval(async () => {
      try {
        const session = await gamePlayApi.getStatus(this.data.roomId);
        const oldSession = this.data.session;

        // 检测回合变化，显示结果
        if (oldSession && session.currentRound > oldSession.currentRound) {
          this.showRoundResult(oldSession);
        }

        this.setData({ session });

        // 游戏结束，停止轮询
        if (session.status === 'FINISHED') {
          this.stopPolling();
          this.showGameResult(session);
        }
      } catch (err) {
        console.error('获取游戏状态失败', err);
      }
    }, 1000);

    this.setData({ polling });
  },

  stopPolling() {
    if (this.data.polling) {
      clearInterval(this.data.polling);
      this.setData({ polling: null });
    }
  },

  // 出招
  async makeChoice(e) {
    const choice = e.currentTarget.dataset.choice;
    const { session, choosing } = this.data;

    if (choosing || session.hasChosen) {
      return;
    }

    this.setData({ choosing: true });

    try {
      const newSession = await gamePlayApi.makeChoice(this.data.roomId, choice);
      this.setData({ session: newSession });

      // 如果双方都出招了，显示结果
      if (newSession.opponentChoice) {
        this.showRoundResult(newSession);
      }
    } catch (err) {
      console.error('出招失败', err);
      wx.showToast({
        title: err.message || '出招失败',
        icon: 'none'
      });
    } finally {
      this.setData({ choosing: false });
    }
  },

  // 显示回合结果
  showRoundResult(session) {
    this.setData({
      showResult: true,
      lastRoundResult: {
        myChoice: session.myChoice,
        opponentChoice: session.opponentChoice,
        result: session.roundResult
      }
    });

    // 2秒后隐藏结果
    setTimeout(() => {
      this.setData({ showResult: false });
    }, 2000);
  },

  // 显示游戏最终结果
  showGameResult(session) {
    const isWinner = session.winnerId && (
      (session.isPlayer1 && session.player1Wins > session.player2Wins) ||
      (!session.isPlayer1 && session.player2Wins > session.player1Wins)
    );

    wx.showModal({
      title: '游戏结束',
      content: isWinner ? '恭喜你赢了！' : '很遗憾，你输了',
      showCancel: false,
      success: () => {
        wx.navigateBack();
      }
    });
  },

  // 获取选择的中文名
  getChoiceName(choice) {
    const names = {
      ROCK: '石头',
      PAPER: '布',
      SCISSORS: '剪刀'
    };
    return names[choice] || choice;
  },

  // 返回
  goBack() {
    wx.navigateBack();
  }
});
