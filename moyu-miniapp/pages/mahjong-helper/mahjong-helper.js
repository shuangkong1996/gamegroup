const { mahjongApi } = require('../../utils/api');
const { getTileIcon, decorateTiles } = require('../../utils/mahjongTiles');

const HISTORY_STORAGE_KEY = 'mahjongAdviceHistory';
const MAX_HISTORY_COUNT = 8;
const MAX_HAND_TILES = 14;
const MAX_SAME_TILE = 4;

// 展示与录入顺序：筒 -> 条 -> 万 -> 字
const SUIT_ORDER = {
  筒: 0,
  条: 1,
  万: 2,
  字: 3
};

const RANK_ORDER = {
  一: 1,
  二: 2,
  三: 3,
  四: 4,
  五: 5,
  六: 6,
  七: 7,
  八: 8,
  九: 9
};

const HONOR_ORDER = {
  东风: 1,
  南风: 2,
  西风: 3,
  北风: 4,
  红中: 5,
  发财: 6,
  白板: 7
};

const TILE_GROUPS = [
  {
    key: '筒',
    name: '筒',
    type: 'number',
    tiles: [
      { name: '一筒', label: '1' },
      { name: '二筒', label: '2' },
      { name: '三筒', label: '3' },
      { name: '四筒', label: '4' },
      { name: '五筒', label: '5' },
      { name: '六筒', label: '6' },
      { name: '七筒', label: '7' },
      { name: '八筒', label: '8' },
      { name: '九筒', label: '9' }
    ]
  },
  {
    key: '条',
    name: '条',
    type: 'number',
    tiles: [
      { name: '一条', label: '1' },
      { name: '二条', label: '2' },
      { name: '三条', label: '3' },
      { name: '四条', label: '4' },
      { name: '五条', label: '5' },
      { name: '六条', label: '6' },
      { name: '七条', label: '7' },
      { name: '八条', label: '8' },
      { name: '九条', label: '9' }
    ]
  },
  {
    key: '万',
    name: '万',
    type: 'number',
    tiles: [
      { name: '一万', label: '1' },
      { name: '二万', label: '2' },
      { name: '三万', label: '3' },
      { name: '四万', label: '4' },
      { name: '五万', label: '5' },
      { name: '六万', label: '6' },
      { name: '七万', label: '7' },
      { name: '八万', label: '8' },
      { name: '九万', label: '9' }
    ]
  },
  {
    key: '字',
    name: '字',
    type: 'honor',
    tiles: [
      { name: '东风', label: '东' },
      { name: '南风', label: '南' },
      { name: '西风', label: '西' },
      { name: '北风', label: '北' },
      { name: '红中', label: '中' },
      { name: '发财', label: '发' },
      { name: '白板', label: '白' }
    ]
  }
];

Page({
  data: {
    imageUrl: '',
    analyzing: false,
    recalculating: false,
    result: null,
    history: [],
    // 页面摘要用
    editableTiles: [],
    editableTileItems: [],
    winningTileItems: [],
    originalTiles: [],
    tilesDirty: false,
    canRecalculate: false,
    // 全屏录入
    editorVisible: false,
    editorTiles: [],
    editorTileItems: [],
    editorCount: 0,
    editorCountText: '0 / 14',
    editorStatusText: '建议录到 13 张听牌或 14 张胡牌',
    activeSuitIndex: 0,
    tileGroups: TILE_GROUPS,
    activePalette: [],
    tips: [
      '尽量只拍自己的手牌，避免把牌河和其他人的牌拍进去',
      '无副露：13 听 / 14 胡；已碰杠可不录副露，只录剩余手牌（10/11、7/8…）',
      '识牌不准时，点“编辑手牌”纠错；若已胡会明确提示“胡了哦”'
    ]
  },

  onLoad() {
    this.loadHistory();
    this.refreshPalette(0, []);
  },

  onChoosePhoto() {
    if (this.data.analyzing || this.data.recalculating || this.data.editorVisible) {
      return;
    }

    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['original'],
      sourceType: ['camera', 'album'],
      success: async (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file || !file.tempFilePath) {
          return;
        }

        this.setData({
          imageUrl: file.tempFilePath,
          result: null,
          analyzing: true,
          editableTiles: [],
          editableTileItems: [],
          winningTileItems: [],
          originalTiles: [],
          tilesDirty: false,
          canRecalculate: false
        });

        wx.showLoading({ title: '识别中', mask: true });
        try {
          const result = await mahjongApi.analyzePhoto(file.tempFilePath);
          this.applyResult(result, {
            imageUrl: (result && result.imageUrl) || file.tempFilePath,
            saveToHistory: true,
            syncEditable: true
          });
        } catch (err) {
          console.error('获取胡牌提示失败', err);
        } finally {
          wx.hideLoading();
          this.setData({ analyzing: false });
        }
      }
    });
  },

  onStartManualInput() {
    if (this.data.analyzing || this.data.recalculating) {
      return;
    }
    this.openEditor([]);
  },

  onOpenEditor() {
    if (this.data.analyzing || this.data.recalculating) {
      return;
    }
    this.openEditor(this.data.editableTiles);
  },

  onCloseEditor() {
    this.setData({ editorVisible: false });
  },

  onPreviewImage() {
    if (!this.data.imageUrl) {
      return;
    }
    wx.previewImage({
      urls: [this.data.imageUrl],
      current: this.data.imageUrl
    });
  },

  onLoadHistoryItem(e) {
    if (this.data.editorVisible) {
      return;
    }
    const index = Number(e.currentTarget.dataset.index);
    const item = this.data.history[index];
    if (!item) {
      return;
    }
    this.applyResult(item.result, {
      imageUrl: item.imageUrl || '',
      saveToHistory: false,
      syncEditable: true
    });
  },

  onClearHistory() {
    if (!this.data.history.length) {
      return;
    }
    wx.showModal({
      title: '清空记录',
      content: '确定清空最近查看记录吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        wx.removeStorageSync(HISTORY_STORAGE_KEY);
        this.setData({ history: [] });
        wx.showToast({ title: '已清空', icon: 'success' });
      }
    });
  },

  onSwitchSuit(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (Number.isNaN(index) || !TILE_GROUPS[index]) {
      return;
    }
    this.setData({ activeSuitIndex: index });
    this.refreshPalette(index, this.data.editorTiles);
  },

  onAddEditorTile(e) {
    const tile = e.currentTarget.dataset.tile;
    const disabled = !!e.currentTarget.dataset.disabled;
    if (!tile || disabled) {
      return;
    }

    const tiles = this.data.editorTiles.slice();
    if (tiles.length >= MAX_HAND_TILES) {
      wx.showToast({ title: `最多 ${MAX_HAND_TILES} 张`, icon: 'none' });
      return;
    }

    const sameCount = tiles.filter((item) => item === tile).length;
    if (sameCount >= MAX_SAME_TILE) {
      wx.showToast({ title: '同一张牌最多 4 张', icon: 'none' });
      return;
    }

    tiles.push(tile);
    this.setEditorTiles(tiles);
  },

  onRemoveEditorTile(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (Number.isNaN(index) || index < 0) {
      return;
    }
    const tiles = this.data.editorTiles.slice();
    if (index >= tiles.length) {
      return;
    }
    tiles.splice(index, 1);
    this.setEditorTiles(tiles);
  },

  onClearEditorTiles() {
    if (!this.data.editorTiles.length) {
      return;
    }
    wx.showModal({
      title: '清空手牌',
      content: '确定清空当前录入的手牌吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        this.setEditorTiles([]);
      }
    });
  },

  async onConfirmEditor() {
    if (this.data.analyzing || this.data.recalculating) {
      return;
    }
    const tiles = this.data.editorTiles.slice();
    if (!tiles.length) {
      wx.showToast({ title: '请先添加手牌', icon: 'none' });
      return;
    }

    this.setData({ recalculating: true });
    wx.showLoading({ title: '计算中', mask: true });
    try {
      const result = await mahjongApi.analyzeTiles(tiles);
      const merged = this.normalizeResult(result);
      if (this.data.imageUrl) {
        merged.imageUrl = this.data.result && this.data.result.imageUrl
          ? this.data.result.imageUrl
          : this.data.imageUrl;
      }
      this.applyResult(merged, {
        imageUrl: this.data.imageUrl,
        saveToHistory: true,
        syncEditable: true,
        asCorrected: true
      });
      this.setData({ editorVisible: false });
      if (!(merged && merged.alreadyWinning)) {
        wx.showToast({ title: '已按手牌计算', icon: 'success' });
      }
    } catch (err) {
      console.error('手牌计算失败', err);
    } finally {
      wx.hideLoading();
      this.setData({ recalculating: false });
    }
  },

  async onRecalculate() {
    if (this.data.analyzing || this.data.recalculating || !this.data.canRecalculate) {
      return;
    }
    const tiles = this.data.editableTiles.slice();
    if (!tiles.length) {
      wx.showToast({ title: '请先编辑手牌', icon: 'none' });
      return;
    }

    this.setData({ recalculating: true });
    wx.showLoading({ title: '重算中', mask: true });
    try {
      const result = await mahjongApi.analyzeTiles(tiles);
      const merged = this.normalizeResult(result);
      if (this.data.imageUrl) {
        merged.imageUrl = this.data.result && this.data.result.imageUrl
          ? this.data.result.imageUrl
          : this.data.imageUrl;
      }
      this.applyResult(merged, {
        imageUrl: this.data.imageUrl,
        saveToHistory: true,
        syncEditable: true,
        asCorrected: true
      });
      if (!(merged && merged.alreadyWinning)) {
        wx.showToast({ title: '已按手牌重算', icon: 'success' });
      }
    } catch (err) {
      console.error('手牌重算失败', err);
    } finally {
      wx.hideLoading();
      this.setData({ recalculating: false });
    }
  },

  openEditor(tiles) {
    const sorted = this.sortTiles(tiles || []);
    this.setData({
      editorVisible: true,
      activeSuitIndex: 0
    });
    this.setEditorTiles(sorted);
  },

  setEditorTiles(tiles) {
    const sorted = this.sortTiles(tiles || []);
    const editorTileItems = sorted.map((name, index) => ({
      name,
      index,
      key: `${name}-${index}`,
      icon: getTileIcon(name)
    }));
    const count = sorted.length;
    let statusText = '无副露建议 13 听 / 14 胡；已碰杠可只录剩余手牌';
    if (count === 0) {
      statusText = '无副露建议 13 听 / 14 胡；已碰杠可只录剩余手牌';
    } else if ([1, 4, 7, 10, 13].includes(count)) {
      const assumed = { 13: 0, 10: 1, 7: 2, 4: 3, 1: 4 }[count];
      statusText = assumed
        ? `当前 ${count} 张，可算听口（约 ${assumed} 组碰/杠未录入）`
        : `当前 ${count} 张，可直接计算听口`;
    } else if ([2, 5, 8, 11, 14].includes(count)) {
      const assumed = { 14: 0, 11: 1, 8: 2, 5: 3, 2: 4 }[count];
      statusText = assumed
        ? `当前 ${count} 张，可判断是否已胡（约 ${assumed} 组碰/杠未录入）`
        : `当前 ${count} 张，可直接判断是否胡牌`;
    } else if (count > 0) {
      statusText = `当前 ${count} 张不在常见听/胡张数，仍可点计算查看提示`;
    }

    this.setData({
      editorTiles: sorted,
      editorTileItems,
      editorCount: count,
      editorCountText: `${count} / ${MAX_HAND_TILES}`,
      editorStatusText: statusText
    });
    this.refreshPalette(this.data.activeSuitIndex, sorted);
  },

  refreshPalette(suitIndex, tiles) {
    const group = TILE_GROUPS[suitIndex] || TILE_GROUPS[0];
    const counts = this.countTiles(tiles || []);
    const total = (tiles || []).length;
    const palette = group.tiles.map((item) => {
      const used = counts[item.name] || 0;
      const disabled = total >= MAX_HAND_TILES || used >= MAX_SAME_TILE;
      return {
        name: item.name,
        label: item.label,
        icon: getTileIcon(item.name),
        used,
        disabled,
        badge: used > 0 ? `${used}` : ''
      };
    });
    this.setData({
      activeSuitIndex: suitIndex,
      activePalette: palette
    });
  },

  countTiles(tiles) {
    const map = {};
    (tiles || []).forEach((tile) => {
      map[tile] = (map[tile] || 0) + 1;
    });
    return map;
  },

  sortTiles(tiles) {
    return (tiles || []).slice().sort((left, right) => this.compareTile(left, right));
  },

  compareTile(left, right) {
    const leftMeta = this.getTileSortMeta(left);
    const rightMeta = this.getTileSortMeta(right);
    if (leftMeta.suit !== rightMeta.suit) {
      return leftMeta.suit - rightMeta.suit;
    }
    return leftMeta.rank - rightMeta.rank;
  },

  getTileSortMeta(tileName) {
    if (!tileName) {
      return { suit: 99, rank: 99 };
    }
    if (HONOR_ORDER[tileName]) {
      return { suit: SUIT_ORDER.字, rank: HONOR_ORDER[tileName] };
    }
    const suitChar = tileName.charAt(tileName.length - 1);
    const rankChar = tileName.charAt(0);
    const suit = Object.prototype.hasOwnProperty.call(SUIT_ORDER, suitChar)
      ? SUIT_ORDER[suitChar]
      : 99;
    const rank = Object.prototype.hasOwnProperty.call(RANK_ORDER, rankChar)
      ? RANK_ORDER[rankChar]
      : 99;
    return { suit, rank };
  },

  applyResult(result, options = {}) {
    const normalized = this.normalizeResult(result);
    if (!normalized) {
      this.setData({
        result: null,
        editableTiles: [],
        editableTileItems: [],
        winningTileItems: [],
        originalTiles: [],
        tilesDirty: false,
        canRecalculate: false,
        imageUrl: options.imageUrl || this.data.imageUrl
      });
      return;
    }

    if (options.asCorrected) {
      normalized.suggestion = this.appendCorrectionHint(normalized.suggestion);
      normalized.confidenceLabel = normalized.handValid ? '高' : (normalized.confidenceLabel || '低');
    }

    // 前端统一按录入排序展示
    const tiles = this.sortTiles(
      Array.isArray(normalized.recognizedTiles) ? normalized.recognizedTiles : []
    );
    const winningTiles = this.sortTiles(
      Array.isArray(normalized.winningTiles) ? normalized.winningTiles : []
    );
    normalized.recognizedTiles = tiles;
    normalized.winningTiles = winningTiles;
    normalized.tileCount = tiles.length;
    if (tiles.length) {
      normalized.handSummary = `手牌 ${tiles.length} 张：${tiles.join('、')}`;
    }

    const payload = {
      result: normalized,
      editableTileItems: decorateTiles(tiles),
      winningTileItems: decorateTiles(winningTiles),
      imageUrl: options.imageUrl != null ? options.imageUrl : this.data.imageUrl
    };

    if (options.syncEditable) {
      payload.editableTiles = tiles;
      payload.originalTiles = tiles.slice();
      payload.tilesDirty = false;
      payload.canRecalculate = tiles.length > 0 && !normalized.handValid;
    }

    this.setData(payload);

    if (options.saveToHistory) {
      this.saveHistory({
        imageUrl: payload.imageUrl || normalized.imageUrl || '',
        result: normalized,
        createTimeText: this.formatNow()
      });
    }

    if (normalized.alreadyWinning && options.saveToHistory) {
      // 计算完成后单独提示，避免被“已按手牌计算”覆盖观感
      setTimeout(() => {
        wx.showToast({ title: '胡了哦！', icon: 'success' });
      }, 30);
    }
  },

  appendCorrectionHint(suggestion) {
    const text = suggestion || '';
    if (text.includes('手动纠错') || text.includes('手动录入')) {
      return text;
    }
    const suffix = this.data.imageUrl
      ? '已根据手动纠错后的手牌重算。'
      : '已根据手动录入的手牌计算。';
    if (!text) {
      return suffix;
    }
    return text.endsWith('。') ? `${text}${suffix}` : `${text}。${suffix}`;
  },

  loadHistory() {
    const history = wx.getStorageSync(HISTORY_STORAGE_KEY);
    this.setData({
      history: Array.isArray(history) ? history : []
    });
  },

  saveHistory(record) {
    const currentHistory = Array.isArray(this.data.history) ? this.data.history.slice() : [];
    const normalized = {
      imageUrl: record.imageUrl,
      result: record.result,
      createTimeText: record.createTimeText
    };
    const deduplicated = currentHistory.filter((item) => {
      if (normalized.imageUrl && item.imageUrl === normalized.imageUrl) {
        return false;
      }
      if (!normalized.imageUrl && !item.imageUrl) {
        const left = item.result && item.result.handSummary;
        const right = normalized.result && normalized.result.handSummary;
        return left !== right;
      }
      return true;
    });
    const nextHistory = [normalized].concat(deduplicated).slice(0, MAX_HISTORY_COUNT);
    wx.setStorageSync(HISTORY_STORAGE_KEY, nextHistory);
    this.setData({ history: nextHistory });
  },

  normalizeResult(result) {
    if (!result) {
      return null;
    }
    const recognizedTiles = Array.isArray(result.recognizedTiles) ? result.recognizedTiles : [];
    const winningTiles = Array.isArray(result.winningTiles) ? result.winningTiles : [];
    return {
      ...result,
      recognizedTiles,
      winningTiles,
      tileCount: typeof result.tileCount === 'number' ? result.tileCount : recognizedTiles.length,
      handValid: result.handValid !== false,
      alreadyWinning: !!result.alreadyWinning
    };
  },

  formatNow() {
    const now = new Date();
    const month = `${now.getMonth() + 1}`.padStart(2, '0');
    const day = `${now.getDate()}`.padStart(2, '0');
    const hour = `${now.getHours()}`.padStart(2, '0');
    const minute = `${now.getMinutes()}`.padStart(2, '0');
    return `${month}-${day} ${hour}:${minute}`;
  },

  onShareAppMessage() {
    return {
      title: '麻将胡牌提示',
      path: '/pages/mahjong-helper/mahjong-helper'
    };
  },

  onShareTimeline() {
    return {
      title: '麻将胡牌提示'
    };
  }
});
