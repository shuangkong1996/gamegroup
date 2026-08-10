const { authApi, fileApi } = require('../../utils/api');

const STYLE_OPTIONS = ['通勤简约', '休闲日常', '韩系清爽', '轻熟利落', '运动舒适', '温柔气质'];
const GENDER_OPTIONS = ['女', '男', '其他'];

Page({
  data: {
    loading: true,
    saving: false,
    uploadingField: '',
    styleOptions: STYLE_OPTIONS,
    genderOptions: GENDER_OPTIONS,
    profile: null,
    form: {
      gender: '',
      heightCm: '',
      weightKg: '',
      dailyStyle: '',
      bodyNote: '',
      faceImageUrl: '',
      frontBodyImageUrl: '',
      sideBodyImageUrl: '',
      useWardrobeItems: true
    }
  },

  onLoad() {
    this.loadProfile();
  },

  async loadProfile() {
    try {
      this.setData({ loading: true });
      const profile = await authApi.getProfile();
      this.setData({
        profile,
        form: {
          gender: profile.gender || '',
          heightCm: profile.heightCm === null || profile.heightCm === undefined ? '' : String(profile.heightCm),
          weightKg: profile.weightKg === null || profile.weightKg === undefined ? '' : String(profile.weightKg),
          dailyStyle: profile.dailyStyle || '',
          bodyNote: profile.bodyNote || '',
          faceImageUrl: profile.faceImageUrl || '',
          frontBodyImageUrl: profile.frontBodyImageUrl || '',
          sideBodyImageUrl: profile.sideBodyImageUrl || '',
          useWardrobeItems: profile.useWardrobeItems !== false
        }
      });
    } catch (err) {
      console.error('加载形象资料失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  onGenderTap(e) {
    const value = e.currentTarget.dataset.value || '';
    this.setData({ 'form.gender': value });
  },

  onStyleTap(e) {
    const value = e.currentTarget.dataset.value || '';
    this.setData({ 'form.dailyStyle': value });
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field;
    if (!field) return;
    this.setData({ [`form.${field}`]: e.detail.value || '' });
  },

  onUseWardrobeChange(e) {
    this.setData({ 'form.useWardrobeItems': !!e.detail.value });
  },

  async onUploadImage(e) {
    const field = e.currentTarget.dataset.field;
    if (!field || this.data.uploadingField) {
      return;
    }

    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: async (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file || !file.tempFilePath) {
          return;
        }

        try {
          this.setData({ uploadingField: field });
          wx.showLoading({ title: '上传中' });
          const result = await fileApi.uploadImage(file.tempFilePath);
          this.setData({ [`form.${field}`]: result.url || '' });
          wx.showToast({ title: '已上传', icon: 'success' });
        } catch (err) {
          console.error('上传形象照片失败', err);
        } finally {
          wx.hideLoading();
          this.setData({ uploadingField: '' });
        }
      }
    });
  },

  onPreviewImage(e) {
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    wx.previewImage({
      current: url,
      urls: [url]
    });
  },

  onClearImage(e) {
    const field = e.currentTarget.dataset.field;
    if (!field) return;
    this.setData({ [`form.${field}`]: '' });
  },

  async onSave() {
    if (this.data.saving) {
      return;
    }

    try {
      this.setData({ saving: true });
      const payload = this.buildPayload();
      const profile = await authApi.updateProfile(payload);
      this.setData({ profile });
      wx.showToast({ title: '已保存', icon: 'success' });
      await this.loadProfile();
    } catch (err) {
      console.error('保存形象资料失败', err);
    } finally {
      this.setData({ saving: false });
    }
  },

  buildPayload() {
    const form = this.data.form;
    return {
      gender: this.normalizeText(form.gender),
      heightCm: this.normalizeInteger(form.heightCm),
      weightKg: this.normalizeNumber(form.weightKg),
      dailyStyle: this.normalizeText(form.dailyStyle),
      bodyNote: this.normalizeText(form.bodyNote),
      faceImageUrl: this.normalizeText(form.faceImageUrl),
      frontBodyImageUrl: this.normalizeText(form.frontBodyImageUrl),
      sideBodyImageUrl: this.normalizeText(form.sideBodyImageUrl),
      outfitReferenceImageUrl: this.normalizeText(form.faceImageUrl),
      useWardrobeItems: form.useWardrobeItems !== false
    };
  },

  normalizeText(value) {
    if (value === null || value === undefined) {
      return null;
    }
    const trimmed = String(value).trim();
    return trimmed ? trimmed : null;
  },

  normalizeInteger(value) {
    const text = this.normalizeText(value);
    if (!text) {
      return null;
    }
    const parsed = Number.parseInt(text, 10);
    return Number.isNaN(parsed) ? null : parsed;
  },

  normalizeNumber(value) {
    const text = this.normalizeText(value);
    if (!text) {
      return null;
    }
    const parsed = Number(text);
    return Number.isNaN(parsed) ? null : parsed;
  }
});
