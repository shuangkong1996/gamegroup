const { wardrobeApi } = require('../../utils/api');

Page({
  data: {
    id: null,
    detail: null,
    loading: true,
    saving: false,
    pollingTimer: null,
    form: {
      name: '',
      categoryLevel1: '',
      categoryLevel2: '',
      color: '',
      patternTags: '',
      seasonTags: '',
      styleTags: '',
      occasionTags: '',
      material: '',
      thicknessLevel: 2,
      warmthLevel: 2,
      waterproofLevel: 0,
      fitType: '',
      summaryText: '',
      matchPoints: '',
      avoidPoints: ''
    }
  },

  onLoad(options) {
    const id = options.id;
    this.setData({ id });
    this.loadDetail();
  },

  onUnload() {
    this.stopPolling();
  },

  async loadDetail() {
    const { id } = this.data;
    if (!id) return;

    try {
      this.setData({ loading: true });
      const detail = await wardrobeApi.getItemDetail(id);
      this.setData({
        detail,
        form: {
          name: detail.name || '',
          categoryLevel1: detail.categoryLevel1 || '',
          categoryLevel2: detail.categoryLevel2 || '',
          color: detail.color || '',
          patternTags: detail.patternTags || '',
          seasonTags: detail.seasonTags || '',
          styleTags: detail.styleTags || '',
          occasionTags: detail.occasionTags || '',
          material: detail.material || '',
          thicknessLevel: detail.thicknessLevel == null ? 2 : detail.thicknessLevel,
          warmthLevel: detail.warmthLevel == null ? 2 : detail.warmthLevel,
          waterproofLevel: detail.waterproofLevel == null ? 0 : detail.waterproofLevel,
          fitType: detail.fitType || '',
          summaryText: detail.summaryText || '',
          matchPoints: detail.matchPoints || '',
          avoidPoints: detail.avoidPoints || ''
        }
      });
      this.handlePolling(detail.recognizeStatus);
    } catch (err) {
      console.error('加载衣物详情失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  handlePolling(status) {
    if (status === 'PENDING' || status === 'PROCESSING') {
      if (this.data.pollingTimer) return;
      const timer = setInterval(() => {
        this.loadDetail();
      }, 4000);
      this.setData({ pollingTimer: timer });
      return;
    }
    this.stopPolling();
  },

  stopPolling() {
    if (this.data.pollingTimer) {
      clearInterval(this.data.pollingTimer);
      this.setData({ pollingTimer: null });
    }
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({
      [`form.${field}`]: value
    });
  },

  async onSave() {
    if (this.data.saving) return;

    try {
      this.setData({ saving: true });
      await wardrobeApi.updateItem(this.data.id, this.data.form);
      wx.showToast({ title: '已保存', icon: 'success' });
      this.loadDetail();
    } catch (err) {
      console.error('保存衣物失败', err);
    } finally {
      this.setData({ saving: false });
    }
  }
});
