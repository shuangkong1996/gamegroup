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
        id: 'fake-work',
        name: '假装工作',
        desc: '老板来了一键切换',
        emoji: '💻',
        color: '#60a5fa'
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
      }
    ]
  },

  onToolTap(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/${id}/${id}`
    });
  }
});
