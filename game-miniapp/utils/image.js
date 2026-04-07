// utils/image.js
// 图片路径处理工具

const config = require('./config');

/**
 * 将相对路径转换为完整URL
 * @param {string} imagePath - 图片相对路径，如 'images/games/game-stone.png'
 * @returns {string} 完整URL，如 'https://gamegroup.shuangkong.top/images/games/game-stone.png'
 */
function getImageUrl(imagePath) {
  if (!imagePath) {
    return '';
  }

  // 如果已经是完整URL，直接返回
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath;
  }

  // 如果以 / 开头，直接拼接
  if (imagePath.startsWith('/')) {
    return config.imageBaseUrl + imagePath;
  }

  // 相对路径（数据库存储格式：images/games/xxx.png）
  // 需要转换为：https://gamegroup.shuangkong.top/images/games/xxx.png
  return config.imageBaseUrl + '/' + imagePath;
}

/**
 * 获取默认图片URL
 * @param {string} type - 图片类型：'game' | 'avatar'
 * @returns {string} 默认图片URL
 */
function getDefaultImageUrl(type = 'game') {
  const defaultImages = {
    game: config.imageBaseUrl + '/images/game-default.png',
    avatar: config.imageBaseUrl + '/images/avatar-default.png'
  };
  return defaultImages[type] || defaultImages.game;
}

module.exports = {
  getImageUrl,
  getDefaultImageUrl
};

