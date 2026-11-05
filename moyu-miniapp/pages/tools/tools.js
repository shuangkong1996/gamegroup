// pages/tools/tools.js
Page({
  data: {
    tools: [
      {
        id: 'salary',
        name: '工资计算器',
        desc: '看看今天赚了多少钱',
        emoji: '💰',
        color: '#ffd93d'
      },
      {
        id: 'mahjong-helper',
        name: '麻将胡牌提示',
        desc: '拍一张手牌图看看当前胡牌情况',
        emoji: '🀄',
        color: '#f59e0b'
      },
      {
        id: 'wardrobe',
        name: '我的衣柜',
        desc: '整理衣物和随机搭配',
        emoji: '👕',
        color: '#fb923c'
      },
      {
        id: 'ledger',
        name: '记账台账',
        desc: '记录收支与统计',
        emoji: '🧾',
        color: '#22c55e'
      },
      {
        id: 'mortgage',
        name: '房贷计算器',
        desc: '商贷+公积金组合',
        emoji: '🏠',
        color: '#f472b6'
      },
      {
        id: 'anniversary',
        name: '纪念册',
        desc: '与亲友分享的纪念一刻',
        emoji: '📖',
        color: '#c9ad74'
      },
      {
        id: 'anniversary-note',
        name: '纪念长页',
        desc: '简洁的纸感纪念页面',
        emoji: '📝',
        color: '#d7aaa0'
      },
      {
        id: 'toilet',
        name: '厕所时间',
        desc: '带薪拉屎计算器',
        emoji: '🚽',
        color: '#4ade80'
      }
    ]
  },

  onToolTap(e) {
    const id = e.currentTarget.dataset.id;
    const pagePath = id === 'anniversary-note'
      ? '/pages/anniversary-note/anniversary-note'
      : `/pages/${id}/${id}`;

    wx.navigateTo({
      url: pagePath,
      fail: (error) => {
        console.error('打开工具页面失败', id, error);
        wx.showToast({
          title: '页面打开失败，请重新编译',
          icon: 'none'
        });
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '工具箱',
      path: '/pages/tools/tools'
    };
  },

  onShareTimeline() {
    return {
      title: '工具箱'
    };
  }
});
