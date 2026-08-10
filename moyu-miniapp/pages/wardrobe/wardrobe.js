const { wardrobeApi } = require('../../utils/api');

Page({
  data: {
    currentView: 'list',
    currentFilter: '',
    filterOptions: [
      { label: '全部', value: '' },
      { label: '整理中', value: 'PENDING' },
      { label: '已整理', value: 'SUCCESS' },
      { label: '待补充', value: 'FAILED' }
    ],
    viewOptions: [
      { label: '列表', value: 'list' },
      { label: '分类', value: 'category' }
    ],
    overview: {
      totalCount: 0,
      pendingCount: 0,
      successCount: 0,
      recentItems: []
    },
    categoryOverview: {
      totalCount: 0,
      categories: []
    },
    activeCategoryIndex: 0,
    activeGroupName: '',
    activeCategory: null,
    activeGroups: [],
    activeItems: [],
    items: [],
    loading: true,
    uploading: false
  },

  onLoad() {
    this.loadOverview();
  },

  onShow() {
    this.loadOverview();
  },

  async loadOverview() {
    try {
      this.setData({ loading: true });
      const [overview, items, categoryOverview] = await Promise.all([
        wardrobeApi.getOverview(),
        wardrobeApi.getItems(this.data.currentFilter),
        wardrobeApi.getCategories()
      ]);
      const categoryState = this.buildCategoryState(categoryOverview, this.data.activeCategoryIndex, this.data.activeGroupName);
      this.setData({
        overview,
        items,
        categoryOverview,
        ...categoryState
      });
    } catch (err) {
      console.error('加载衣柜失败', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  onChooseImage() {
    if (this.data.uploading) {
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
          this.setData({ uploading: true });
          wx.showLoading({ title: '上传中' });
          const result = await wardrobeApi.uploadItem(file.tempFilePath);
          wx.hideLoading();
          wx.navigateTo({
            url: `/pages/wardrobe-item/wardrobe-item?id=${result.itemId}`
          });
        } catch (err) {
          wx.hideLoading();
          console.error('上传衣物失败', err);
        } finally {
          this.setData({ uploading: false });
        }
      }
    });
  },

  onOpenItem(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({
      url: `/pages/wardrobe-item/wardrobe-item?id=${id}`
    });
  },

  onOpenOutfit() {
    wx.navigateTo({
      url: '/pages/outfit/outfit'
    });
  },

  onOpenHistory() {
    wx.navigateTo({
      url: '/pages/outfit-history/outfit-history'
    });
  },

  onOpenProfile() {
    wx.navigateTo({
      url: '/pages/user-profile/user-profile'
    });
  },

  onFilterTap(e) {
    const value = e.currentTarget.dataset.value;
    this.setData({ currentFilter: value || '' });
    this.loadOverview();
  },

  onViewTap(e) {
    const value = e.currentTarget.dataset.value;
    if (!value || value === this.data.currentView) {
      return;
    }
    this.setData({ currentView: value });
  },

  onCategoryTap(e) {
    const index = Number(e.currentTarget.dataset.index || 0);
    const categoryState = this.buildCategoryState(this.data.categoryOverview, index, '');
    this.setData(categoryState);
  },

  onGroupTap(e) {
    const groupName = e.currentTarget.dataset.name || '';
    const categoryState = this.buildCategoryState(this.data.categoryOverview, this.data.activeCategoryIndex, groupName);
    this.setData(categoryState);
  },

  onLongPressItem(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;

    wx.showActionSheet({
      itemList: ['归档这件衣物'],
      success: async (res) => {
        if (res.tapIndex !== 0) return;
        try {
          await wardrobeApi.deleteItem(id);
          wx.showToast({ title: '已归档', icon: 'success' });
          this.loadOverview();
        } catch (err) {
          console.error('归档衣物失败', err);
        }
      }
    });
  },

  buildCategoryState(categoryOverview, activeCategoryIndex, activeGroupName) {
    const categories = (categoryOverview && categoryOverview.categories) || [];
    if (!categories.length) {
      return {
        activeCategoryIndex: 0,
        activeGroupName: '',
        activeCategory: null,
        activeGroups: [],
        activeItems: []
      };
    }

    const safeIndex = Math.min(Math.max(activeCategoryIndex || 0, 0), categories.length - 1);
    const activeCategory = categories[safeIndex];
    const groups = (activeCategory.groups || []).map((group) => ({
      ...group,
      isAll: false
    }));
    const allGroup = {
      name: '全部',
      count: activeCategory.count || 0,
      items: [],
      isAll: true
    };
    const activeGroups = [allGroup].concat(groups);
    const hasMatchedGroup = groups.some((group) => group.name === activeGroupName);
    const resolvedGroupName = hasMatchedGroup ? activeGroupName : '';
    const activeItems = resolvedGroupName
      ? (groups.find((group) => group.name === resolvedGroupName) || {}).items || []
      : this.flattenCategoryItems(activeCategory.groups || []);

    return {
      activeCategoryIndex: safeIndex,
      activeGroupName: resolvedGroupName,
      activeCategory,
      activeGroups,
      activeItems
    };
  },

  flattenCategoryItems(groups) {
    const items = [];
    (groups || []).forEach((group) => {
      (group.items || []).forEach((item) => {
        items.push(item);
      });
    });
    return items;
  }
});
