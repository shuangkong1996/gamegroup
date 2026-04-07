// utils/config.js
// 环境配置

const env = 'dev'; // dev | prod

const config = {
  dev: {
    baseUrl: 'https://gamegroup.shuangkong.top/api',
    imageBaseUrl: 'https://gamegroup.shuangkong.top'
  },
  prod: {
    baseUrl: 'https://gamegroup.shuangkong.top/api',
    imageBaseUrl: 'https://gamegroup.shuangkong.top'
  }
};

module.exports = config[env];
