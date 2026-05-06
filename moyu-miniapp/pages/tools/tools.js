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
        id: 'toilet',
        name: '厕所时间',
        desc: '带薪拉屎计算器',
        emoji: '🚽',
        color: '#4ade80'
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
        id: 'community-compare',
        name: '小区对比助手',
        desc: '搜索对比并联动房贷',
        emoji: '🏘️',
        color: '#22d3ee'
      }
    ]
  },

  onToolTap(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/${id}/${id}`
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
