/**
 * 麻将牌面资源映射。
 * 素材来源：perthmahjongsoc/mahjong-tiles-svg（Wikimedia Cangjie6 等），CC BY-SA 4.0。
 * 命名：筒 p / 条 s / 万 m / 字 z
 */

const RANK_CN = ['一', '二', '三', '四', '五', '六', '七', '八', '九'];

const TILE_CODE_MAP = {
  东风: '1z',
  南风: '2z',
  西风: '3z',
  北风: '4z',
  红中: '5z',
  发财: '6z',
  白板: '7z'
};

RANK_CN.forEach((rank, index) => {
  const n = index + 1;
  TILE_CODE_MAP[`${rank}筒`] = `${n}p`;
  TILE_CODE_MAP[`${rank}条`] = `${n}s`;
  TILE_CODE_MAP[`${rank}万`] = `${n}m`;
});

function getTileCode(name) {
  if (!name) {
    return '';
  }
  return TILE_CODE_MAP[name] || '';
}

function getTileIcon(name) {
  const code = getTileCode(name);
  return code ? `/images/mahjong/${code}.png` : '';
}

function decorateTile(name, extra = {}) {
  return {
    name,
    icon: getTileIcon(name),
    ...extra
  };
}

function decorateTiles(names) {
  return (names || []).map((name, index) =>
    decorateTile(name, {
      index,
      key: `${name}-${index}`
    })
  );
}

module.exports = {
  TILE_CODE_MAP,
  getTileCode,
  getTileIcon,
  decorateTile,
  decorateTiles
};
