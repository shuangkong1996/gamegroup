const { authApi, outfitRenderApi } = require('../../utils/api');

const MODE_OPTIONS = [
  { label: '用衣柜搭一套', value: 'WARDROBE_BASED' },
  { label: '直接给我一套', value: 'FREE_GENERATE' }
];

const SCENE_OPTIONS = ['通勤上班', '周末逛街', '朋友见面', '外出拍照', '旅行出游'];

Page({
  data: {
    loading: true,
    generating: false,
    adopting: false,
    savingCity: false,
    refreshingTask: false,
    modeOptions: MODE_OPTIONS,
    sceneOptions: SCENE_OPTIONS,
    selectedMode: 'WARDROBE_BASED',
    selectedScene: '通勤上班',
    latestTask: null,
    specificTaskId: '',
    readonly: false,
    city: '',
    settingsLoaded: false,
    pollTimer: null
  },

  async onLoad(options) {
    const specificTaskId = options && options.taskId ? options.taskId : '';
    const readonly = options && options.readonly === '1';
    this.setData({ specificTaskId, readonly });
    await this.loadSettings();
    this.loadTaskData();
  },

  onShow() {
    this.loadTaskData();
  },

  onHide() {
    this.stopPolling();
  },

  onUnload() {
    this.stopPolling();
  },

  async loadSettings() {
    try {
      const settings = await authApi.getSettings();
      this.setData({
        city: settings.city || '',
        settingsLoaded: true
      });
    } catch (err) {
      console.error('加载城市设置失败', err);
      this.setData({ settingsLoaded: true });
    }
  },

  async loadLatestTask() {
    try {
      this.setData({ loading: true });
      const latestTask = await outfitRenderApi.getLatestTask();
      this.applyTaskState(latestTask);
    } catch (err) {
      console.error('加载参考方案失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadTaskData() {
    if (this.data.specificTaskId) {
      return this.loadTaskById(this.data.specificTaskId);
    }
    return this.loadLatestTask();
  },

  async loadTaskById(taskId) {
    try {
      this.setData({ loading: true });
      const latestTask = await outfitRenderApi.getTask(taskId);
      this.applyTaskState(latestTask);
    } catch (err) {
      console.error('加载参考方案详情失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  onModeTap(e) {
    const value = e.currentTarget.dataset.value;
    if (!value) return;
    this.setData({ selectedMode: value });
  },

  onSceneTap(e) {
    const value = e.currentTarget.dataset.value;
    if (!value) return;
    this.setData({ selectedScene: value });
  },

  async onGenerate() {
    if (this.data.generating) return;
    try {
      this.setData({ generating: true });
      const latestTask = await outfitRenderApi.createTask({
        mode: this.data.selectedMode,
        city: this.data.city,
        sceneRequirement: this.data.selectedScene,
        useWardrobeItems: this.data.selectedMode === 'WARDROBE_BASED'
      });
      this.applyTaskState(latestTask);
      this.startPolling();
      wx.showToast({ title: '已开始生成', icon: 'success' });
    } catch (err) {
      console.error('生成参考方案失败', err);
    } finally {
      this.setData({ generating: false });
    }
  },

  async onAdopt() {
    const { latestTask, adopting } = this.data;
    if (!latestTask || !latestTask.taskId || adopting) {
      return;
    }
    try {
      this.setData({ adopting: true });
      await outfitRenderApi.adoptTask(latestTask.taskId, {});
      wx.showToast({ title: '已采纳', icon: 'success' });
    } catch (err) {
      console.error('采纳参考搭配失败', err);
    } finally {
      this.setData({ adopting: false });
    }
  },

  async onRefreshTask() {
    if (this.data.refreshingTask) {
      return;
    }
    try {
      this.setData({ refreshingTask: true });
      await this.loadTaskData();
    } finally {
      this.setData({ refreshingTask: false });
    }
  },

  onCityInput(e) {
    this.setData({ city: e.detail.value || '' });
  },

  async onSaveCity() {
    if (this.data.savingCity) return;

    try {
      this.setData({ savingCity: true });
      await authApi.updateSettings({ city: this.data.city });
      wx.showToast({ title: '城市已保存', icon: 'success' });
      await this.loadLatestTask();
    } catch (err) {
      console.error('保存城市失败', err);
    } finally {
      this.setData({ savingCity: false });
    }
  },

  applyTaskState(task) {
    if (!task) {
      this.stopPolling();
      this.setData({ latestTask: null });
      return;
    }
    this.setData({
      latestTask: task,
      selectedMode: task.mode || this.data.selectedMode,
      selectedScene: task.sceneRequirement || this.data.selectedScene
    });
    if (this.isTaskPending(task)) {
      this.startPolling();
      return;
    }
    this.stopPolling();
  },

  isTaskPending(task) {
    return !!task && (task.status === 'PLANNING' || task.status === 'QUEUED' || task.status === 'RENDERING');
  },

  startPolling() {
    if (this.data.readonly || this.data.pollTimer) {
      return;
    }
    const timer = setInterval(() => {
      if (!this.isTaskPending(this.data.latestTask) || this.data.refreshingTask || this.data.loading) {
        if (!this.isTaskPending(this.data.latestTask)) {
          this.stopPolling();
        }
        return;
      }
      this.onRefreshTask();
    }, 3000);
    this.setData({ pollTimer: timer });
  },

  stopPolling() {
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer);
      this.setData({ pollTimer: null });
    }
  }
});
