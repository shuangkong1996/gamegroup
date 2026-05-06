const { mortgageApi } = require('../../utils/api');

const REPAYMENT_METHODS = ['EQUAL_INSTALLMENT', 'EQUAL_PRINCIPAL'];
const PROVIDENT_TIERS = ['FIRST_HOME', 'SECOND_HOME'];

Page({
  data: {
    sourceCommunityName: '',
    sourceHouseTotal: '',
    commercialWan: '70',
    fundWan: '50',
    years: 30,
    targetInterestWan: '',
    commercialRate: '',
    fundRate: '',
    referenceNote: '',
    referencePeriod: '',
    providentFundRateNote: '',
    fundTierLabels: ['公积金首套', '公积金二套'],
    fundTierIndex: 0,
    defaultsPayload: null,
    useProvidentPreset: true,
    repaymentLabels: ['等额本息', '等额本金'],
    repaymentIndex: 0,
    loading: false,
    calculated: false,
    combined: null,
    prepayAdvice: null,
    schedulePreview: [],
    showSchedule: false
  },

  onLoad(options) {
    const patch = {};
    if (options && options.communityName) {
      patch.sourceCommunityName = decodeURIComponent(options.communityName);
    }
    if (options && options.houseTotal) {
      patch.sourceHouseTotal = options.houseTotal;
    }
    if (options && options.commercialWan) {
      patch.commercialWan = options.commercialWan;
      patch.fundWan = '0';
    }
    if (Object.keys(patch).length > 0) {
      this.setData(patch);
    }
    this.loadDefaults();
  },

  async loadDefaults() {
    try {
      const d = await mortgageApi.getDefaults();
      const years = parseInt(this.data.years, 10) || 30;
      const fundRate = fmtNum(pickProvidentRate(d, this.data.fundTierIndex, years));
      this.setData({
        defaultsPayload: d,
        commercialRate: fmtNum(d.commercialAnnualRatePercent),
        fundRate,
        referenceNote: d.referenceNote || '',
        referencePeriod: d.referencePeriod || '',
        providentFundRateNote: d.providentFundRateNote || '',
        useProvidentPreset: true
      });
    } catch (e) {
      const offline = offlineDefaultsPayload();
      const years = parseInt(this.data.years, 10) || 30;
      this.setData({
        defaultsPayload: offline,
        commercialRate: '3.05',
        fundRate: fmtNum(pickProvidentRate(offline, this.data.fundTierIndex, years)),
        referenceNote: '默认利率为离线参考值，请联网获取服务端最新默认。',
        providentFundRateNote: offline.providentFundRateNote,
        useProvidentPreset: true
      });
    }
  },

  onCommercialWan(e) {
    this.setData({ commercialWan: e.detail.value });
  },

  onFundWan(e) {
    this.setData({ fundWan: e.detail.value });
  },

  onYears(e) {
    this.setData({ years: e.detail.value });
    if (this.data.useProvidentPreset) {
      this.applyProvidentPreset();
    }
  },

  onCommercialRate(e) {
    this.setData({ commercialRate: e.detail.value });
  },

  onTargetInterestWan(e) {
    this.setData({ targetInterestWan: e.detail.value });
  },

  onFundRate(e) {
    this.setData({ fundRate: e.detail.value, useProvidentPreset: false });
  },

  onFundTierChange(e) {
    const idx = Number(e.detail.value);
    this.setData({ fundTierIndex: idx, useProvidentPreset: true });
    this.applyProvidentPreset();
  },

  applyProvidentPreset() {
    const d = this.data.defaultsPayload;
    if (!d || !this.data.useProvidentPreset) return;
    const years = parseInt(this.data.years, 10) || 30;
    const r = pickProvidentRate(d, this.data.fundTierIndex, years);
    this.setData({ fundRate: fmtNum(r) });
  },

  onRepaymentChange(e) {
    this.setData({ repaymentIndex: Number(e.detail.value) });
  },

  toggleSchedule() {
    this.setData({ showSchedule: !this.data.showSchedule });
  },

  async onCalculate() {
    const cw = parseFloat(this.data.commercialWan) || 0;
    const fw = parseFloat(this.data.fundWan) || 0;
    if (cw <= 0 && fw <= 0) {
      wx.showToast({ title: '请至少填一类贷款（万元）', icon: 'none' });
      return;
    }

    const years = parseInt(this.data.years, 10);
    if (!years || years < 1 || years > 40) {
      wx.showToast({ title: '贷款年限请填 1～40 年', icon: 'none' });
      return;
    }

    let comRateOpt = null;
    if (cw > 0 && this.data.commercialRate !== '' && this.data.commercialRate != null) {
      const cr = parseFloat(this.data.commercialRate);
      if (Number.isNaN(cr) || cr < 0) {
        wx.showToast({ title: '商贷年利率无效', icon: 'none' });
        return;
      }
      comRateOpt = cr;
    }
    let fundRateOpt = null;
    if (fw > 0 && !this.data.useProvidentPreset && this.data.fundRate !== '' && this.data.fundRate != null) {
      const fr = parseFloat(this.data.fundRate);
      if (Number.isNaN(fr) || fr < 0) {
        wx.showToast({ title: '公积金年利率无效', icon: 'none' });
        return;
      }
      fundRateOpt = fr;
    }

    const body = {
      years,
      repaymentMethod: REPAYMENT_METHODS[this.data.repaymentIndex],
      commercialPrincipal: Math.round(cw * 10000 * 100) / 100,
      providentFundPrincipal: Math.round(fw * 10000 * 100) / 100,
      providentFundTier: PROVIDENT_TIERS[this.data.fundTierIndex] || 'FIRST_HOME'
    };
    const targetInterestWan = parseFloat(this.data.targetInterestWan);
    if (!Number.isNaN(targetInterestWan) && targetInterestWan > 0) {
      body.targetTotalInterestAmount = Math.round(targetInterestWan * 10000 * 100) / 100;
      body.prepayWindowStartYear = 5;
      body.prepayWindowEndYear = 10;
    }
    if (comRateOpt !== null) body.commercialAnnualRatePercent = comRateOpt;
    if (fundRateOpt !== null) body.providentFundAnnualRatePercent = fundRateOpt;

    this.setData({ loading: true, calculated: false });
    try {
      const res = await mortgageApi.calculate(body);
      const preview = (res.combinedSchedule || []).slice(0, 12).map((row) => ({
        monthIndex: row.monthIndex,
        monthlyPayment: fmtMoney(row.monthlyPayment),
        principalPortion: fmtMoney(row.principalPortion),
        interestPortion: fmtMoney(row.interestPortion),
        remainingPrincipal: fmtMoney(row.remainingPrincipal)
      }));
      this.setData({
        calculated: true,
        combined: {
          firstMonthPayment: fmtMoney(res.combined.firstMonthPayment),
          lastMonthPayment: fmtMoney(res.combined.lastMonthPayment),
          totalPayment: fmtMoney(res.combined.totalPayment),
          totalInterest: fmtMoney(res.combined.totalInterest)
        },
        prepayAdvice: mapPrepayAdvice(res.prepayAdvice),
        schedulePreview: preview,
        showSchedule: false,
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false });
    }
  },

  onShareAppMessage() {
    return {
      title: '房贷计算器',
      path: '/pages/mortgage/mortgage'
    };
  },

  onShareTimeline() {
    return {
      title: '房贷计算器'
    };
  }
});

function fmtNum(v) {
  if (v === undefined || v === null) return '';
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return String(n);
}

function fmtMoney(v) {
  const n = Number(v);
  if (Number.isNaN(n)) return String(v);
  return n.toFixed(2);
}

function pickProvidentRate(d, tierIndex, years) {
  const fivePlus = years > 5;
  if (tierIndex === 1) {
    return fivePlus
      ? d.providentFundSecondHomeFiveYearPlusPercent
      : d.providentFundSecondHomeFiveYearOrBelowPercent;
  }
  return fivePlus
    ? d.providentFundFirstHomeFiveYearPlusPercent
    : d.providentFundFirstHomeFiveYearOrBelowPercent;
}

function offlineDefaultsPayload() {
  return {
    commercialAnnualRatePercent: 3.05,
    providentFundFirstHomeFiveYearOrBelowPercent: 2.1,
    providentFundFirstHomeFiveYearPlusPercent: 2.6,
    providentFundSecondHomeFiveYearOrBelowPercent: 2.525,
    providentFundSecondHomeFiveYearPlusPercent: 3.075,
    providentFundRateNote:
      '离线参考：首套5年及以下2.10%、5年以上2.60%；二套5年及以下2.525%、5年以上3.075%。'
  };
}

function mapPrepayAdvice(advice) {
  if (!advice) return null;
  const optimal = advice.optimalPlan
    ? {
      prepayAtYear: advice.optimalPlan.prepayAtYear,
      monthIndex: advice.optimalPlan.monthIndex,
      requiredPrepayAmount: fmtMoney(advice.optimalPlan.requiredPrepayAmount),
      projectedTotalInterest: fmtMoney(advice.optimalPlan.projectedTotalInterest),
      estimatedInterestSaved: fmtMoney(advice.optimalPlan.estimatedInterestSaved),
      feasible: !!advice.optimalPlan.feasible
    }
    : null;
  const plans = (advice.plans || []).map((p) => ({
    prepayAtYear: p.prepayAtYear,
    monthIndex: p.monthIndex,
    requiredPrepayAmount: fmtMoney(p.requiredPrepayAmount),
    projectedTotalInterest: fmtMoney(p.projectedTotalInterest),
    estimatedInterestSaved: fmtMoney(p.estimatedInterestSaved),
    feasible: !!p.feasible
  }));
  return {
    targetTotalInterestAmount: fmtMoney(advice.targetTotalInterestAmount),
    baseTotalInterest: fmtMoney(advice.baseTotalInterest),
    windowStartYear: advice.windowStartYear,
    windowEndYear: advice.windowEndYear,
    optimalPlan: optimal,
    plans
  };
}
