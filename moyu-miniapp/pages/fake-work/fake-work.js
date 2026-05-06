// pages/fake-work/fake-work.js
Page({
  data: {
    mode: 'code', // code | doc | loading
    codeLines: [],
    docContent: '',
    loadingProgress: 0,
    timer: null
  },

  onLoad() {
    this.generateCode();
  },

  onUnload() {
    this.stopTimer();
  },

  stopTimer() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
      this.setData({ timer: null });
    }
  },

  // 切换模式
  switchMode(e) {
    const mode = e.currentTarget.dataset.mode;
    this.stopTimer();

    if (mode === 'code') {
      this.generateCode();
    } else if (mode === 'doc') {
      this.generateDoc();
    } else if (mode === 'loading') {
      this.startLoading();
    }

    this.setData({ mode });
  },

  // 生成假代码
  generateCode() {
    const codeSnippets = [
      'import { useCallback, useMemo } from "react";',
      'const handleSubmit = async (data) => {',
      '  try {',
      '    const result = await api.post("/submit", data);',
      '    if (result.status === 200) {',
      '      dispatch(updateState(result.data));',
      '      showToast("提交成功");',
      '    }',
      '  } catch (error) {',
      '    console.error("Submit failed:", error);',
      '    handleError(error);',
      '  }',
      '};',
      '',
      'const optimizedList = useMemo(() => {',
      '  return items.filter(item => item.active)',
      '    .map(item => ({',
      '      ...item,',
      '      computed: calculateValue(item)',
      '    }));',
      '}, [items]);',
      '',
      'useEffect(() => {',
      '  fetchData();',
      '  return () => cleanup();',
      '}, [dependency]);'
    ];

    this.setData({ codeLines: codeSnippets });

    // 自动滚动效果
    let lineIndex = 0;
    const timer = setInterval(() => {
      lineIndex = (lineIndex + 1) % codeSnippets.length;
      // 触发重新渲染产生闪烁效果
      this.setData({
        codeLines: [...codeSnippets.slice(lineIndex), ...codeSnippets.slice(0, lineIndex)]
      });
    }, 3000);

    this.setData({ timer });
  },

  // 生成假文档
  generateDoc() {
    const docs = [
      '## 项目需求分析报告\n\n### 1. 背景\n\n根据业务部门反馈，现有系统在高并发场景下存在性能瓶颈...',
      '### 2. 技术方案\n\n经过技术评审，建议采用以下方案：\n\n1. 引入Redis缓存层\n2. 数据库读写分离\n3. 接口限流降级',
      '### 3. 实施计划\n\n- 第一阶段：核心模块重构（2周）\n- 第二阶段：性能测试优化（1周）\n- 第三阶段：灰度发布（1周）',
      '### 4. 风险评估\n\n| 风险项 | 概率 | 影响 | 应对措施 |\n|--------|------|------|----------|\n| 数据迁移 | 中 | 高 | 制定回滚方案 |'
    ];

    let docIndex = 0;
    this.setData({ docContent: docs[docIndex] });

    const timer = setInterval(() => {
      docIndex = (docIndex + 1) % docs.length;
      this.setData({ docContent: docs[docIndex] });
    }, 5000);

    this.setData({ timer });
  },

  // 假加载进度
  startLoading() {
    let progress = 0;
    this.setData({ loadingProgress: 0 });

    const timer = setInterval(() => {
      // 随机增加进度，但永远到不了100%
      if (progress < 99) {
        const increment = Math.random() * 3;
        progress = Math.min(99, progress + increment);
        this.setData({ loadingProgress: progress.toFixed(1) });
      }
    }, 500);

    this.setData({ timer });
  },

  onShareAppMessage() {
    return {
      title: '假装工作',
      path: '/pages/fake-work/fake-work'
    };
  },

  onShareTimeline() {
    return {
      title: '假装工作'
    };
  }
});
