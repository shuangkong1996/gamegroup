// 小程序转发只能自定义标题和一张封面。网页链接里的标题、邀请文案、照片需要画进同一张图。
const MESSAGE_SIZE = { width: 1000, height: 800 };
const TIMELINE_SIZE = { width: 800, height: 800 };
const SERIF = '"Songti SC", "STSong", "Songti TC", serif';
const SANS = '"PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", sans-serif';

function buildShareTitle(invitation) {
  return `${invitation.groom}❤️${invitation.bride}的婚礼邀请`;
}

function roundRect(ctx, x, y, width, height, radius) {
  const r = Math.min(radius, width / 2, height / 2);
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + width, y, x + width, y + height, r);
  ctx.arcTo(x + width, y + height, x, y + height, r);
  ctx.arcTo(x, y + height, x, y, r);
  ctx.arcTo(x, y, x + width, y, r);
  ctx.closePath();
}

function drawHeart(ctx, cx, cy, size) {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(size, size);
  ctx.beginPath();
  ctx.moveTo(0, 0.32);
  ctx.bezierCurveTo(-0.04, 0.14, -0.48, 0.12, -0.48, -0.16);
  ctx.bezierCurveTo(-0.48, -0.46, -0.16, -0.5, 0, -0.2);
  ctx.bezierCurveTo(0.16, -0.5, 0.48, -0.46, 0.48, -0.16);
  ctx.bezierCurveTo(0.48, 0.12, 0.04, 0.14, 0, 0.32);
  ctx.closePath();
  ctx.fillStyle = '#ef3d3d';
  ctx.fill();
  ctx.restore();
}

function drawImageCover(ctx, image, x, y, width, height) {
  const scale = Math.max(width / image.width, height / image.height);
  const sourceWidth = width / scale;
  const sourceHeight = height / scale;
  const sx = (image.width - sourceWidth) / 2;
  const sy = Math.max(0, (image.height - sourceHeight) * 0.04);
  ctx.drawImage(image, sx, sy, sourceWidth, sourceHeight, x, y, width, height);
}

function measureNames(ctx, invitation, fontSize) {
  ctx.font = `600 ${fontSize}px ${SERIF}`;
  const groomWidth = ctx.measureText(invitation.groom).width;
  const brideWidth = ctx.measureText(invitation.bride).width;
  const heartSize = fontSize * 0.7;
  const gap = fontSize * 0.16;
  return {
    groomWidth,
    brideWidth,
    heartSize,
    gap,
    width: groomWidth + gap + heartSize + gap + brideWidth
  };
}

function fitFontSize(maxSize, minSize, fits) {
  let size = maxSize;
  while (size > minSize && !fits(size)) size -= 1;
  return size;
}

function cardMetrics(width, height) {
  const isSquare = width === height;
  const pad = Math.round(width * 0.034);
  const gap = Math.round(width * 0.02);
  const titleSize = Math.round(width * (isSquare ? 0.078 : 0.084));
  const bodySize = Math.round(width * (isSquare ? 0.056 : 0.06));
  const metaSize = Math.round(width * 0.04);
  // 分享图在聊天里会缩小，文字列按大约 10 个字留宽，保证字号够大
  const textWidth = Math.round(bodySize * 11);
  const photoWidth = Math.max(150, width - pad * 2 - gap - textWidth);
  const photoHeight = Math.min(
    Math.round(height * (isSquare ? 0.8 : 0.84)),
    Math.round(photoWidth / 0.54)
  );
  const photoX = width - pad - photoWidth;
  const photoY = Math.round((height - photoHeight) / 2);
  return {
    pad,
    width,
    height,
    photoWidth,
    photoHeight,
    photoX,
    photoY,
    textX: pad,
    textWidth: photoX - pad - gap,
    titleSize,
    bodySize,
    metaSize,
    radius: Math.round(photoWidth * 0.045)
  };
}

function layoutCopy(ctx, metrics, invitation) {
  const lines = invitation.shareLines || [];
  const titleSize = fitFontSize(metrics.titleSize, 36, (size) => {
    const names = measureNames(ctx, invitation, size);
    const subtitleSize = Math.round(size * 0.84);
    ctx.font = `600 ${subtitleSize}px ${SERIF}`;
    return names.width <= metrics.textWidth
      && ctx.measureText('的婚礼邀请').width <= metrics.textWidth;
  });
  const subtitleSize = Math.round(titleSize * 0.84);
  const bodySize = fitFontSize(metrics.bodySize, 28, (size) => {
    ctx.font = `400 ${size}px ${SANS}`;
    return lines.every((line) => ctx.measureText(line).width <= metrics.textWidth);
  });
  const metaSize = metrics.metaSize;
  ctx.font = `400 ${metaSize}px ${SANS}`;
  const combinedMeta = `${invitation.solarDate} · ${invitation.venue}`;
  const metaLines = ctx.measureText(combinedMeta).width <= metrics.textWidth
    ? [combinedMeta]
    : [invitation.solarDate, invitation.venue];
  const nameGap = Math.round(titleSize * 0.08);
  const titleGap = Math.round(bodySize * 0.62);
  const bodyLine = Math.round(bodySize * 1.42);
  const metaGap = Math.round(bodySize * 0.72);
  const metaLine = Math.round(metaSize * 1.35);
  const blockHeight = titleSize
    + nameGap
    + subtitleSize
    + titleGap
    + bodyLine * Math.max(lines.length - 1, 0)
    + (lines.length ? bodySize : 0)
    + metaGap
    + metaLine * Math.max(metaLines.length - 1, 0)
    + metaSize;
  let textTop = metrics.photoY + (metrics.photoHeight - blockHeight) / 2;
  const maxTop = metrics.height - metrics.pad - blockHeight;
  textTop = Math.min(Math.max(metrics.pad, textTop), Math.max(metrics.pad, maxTop));

  return {
    lines,
    metaLines,
    titleSize,
    subtitleSize,
    bodySize,
    metaSize,
    nameGap,
    titleGap,
    bodyLine,
    metaGap,
    metaLine,
    textTop
  };
}

function drawPhoto(ctx, image, metrics) {
  const { photoX, photoY, photoWidth, photoHeight, radius } = metrics;
  ctx.save();
  ctx.shadowColor = 'rgba(62, 44, 32, 0.16)';
  ctx.shadowBlur = 28;
  ctx.shadowOffsetY = 10;
  roundRect(ctx, photoX, photoY, photoWidth, photoHeight, radius);
  ctx.fillStyle = '#e7dfd4';
  ctx.fill();
  ctx.restore();

  ctx.save();
  roundRect(ctx, photoX, photoY, photoWidth, photoHeight, radius);
  ctx.clip();
  if (image && image.width && image.height) {
    drawImageCover(ctx, image, photoX, photoY, photoWidth, photoHeight);
  } else {
    ctx.fillStyle = '#e7dfd4';
    ctx.fillRect(photoX, photoY, photoWidth, photoHeight);
    ctx.fillStyle = '#c4a090';
    ctx.font = `500 ${Math.round(photoWidth * 0.16)}px ${SERIF}`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('囍', photoX + photoWidth / 2, photoY + photoHeight / 2);
  }
  ctx.restore();
}

function drawInvitationCard(ctx, { width, height, image, invitation }) {
  const metrics = cardMetrics(width, height);
  const copy = layoutCopy(ctx, metrics, invitation);
  const background = ctx.createLinearGradient(0, 0, width, height);
  background.addColorStop(0, '#f7f4ee');
  background.addColorStop(1, '#f1ebe3');
  ctx.fillStyle = background;
  ctx.fillRect(0, 0, width, height);
  drawPhoto(ctx, image, metrics);

  ctx.textAlign = 'left';
  ctx.textBaseline = 'top';
  ctx.fillStyle = '#2c2825';
  const names = measureNames(ctx, invitation, copy.titleSize);
  let cursor = metrics.textX;
  const titleY = copy.textTop + copy.titleSize * 0.08;
  ctx.font = `600 ${copy.titleSize}px ${SERIF}`;
  ctx.fillText(invitation.groom, cursor, copy.textTop);
  cursor += names.groomWidth + names.gap;
  drawHeart(ctx, cursor + names.heartSize / 2, titleY + copy.titleSize * 0.42, names.heartSize);
  cursor += names.heartSize + names.gap;
  ctx.fillStyle = '#2c2825';
  ctx.font = `600 ${copy.titleSize}px ${SERIF}`;
  ctx.fillText(invitation.bride, cursor, copy.textTop);

  let y = copy.textTop + copy.titleSize + copy.nameGap;
  ctx.fillStyle = '#2c2825';
  ctx.font = `600 ${copy.subtitleSize}px ${SERIF}`;
  ctx.fillText('的婚礼邀请', metrics.textX, y);
  y += copy.subtitleSize + copy.titleGap;
  ctx.fillStyle = '#5e5852';
  ctx.font = `400 ${copy.bodySize}px ${SANS}`;
  copy.lines.forEach((line, index) => {
    ctx.fillText(line, metrics.textX, y);
    y += index === copy.lines.length - 1 ? copy.bodySize : copy.bodyLine;
  });

  y += copy.metaGap;
  ctx.fillStyle = '#a47f6e';
  ctx.font = `400 ${copy.metaSize}px ${SANS}`;
  copy.metaLines.forEach((line, index) => {
    ctx.fillText(line, metrics.textX, y);
    y += index === copy.metaLines.length - 1 ? copy.metaSize : copy.metaLine;
  });
}

function loadCanvasImage(canvas, src) {
  return new Promise((resolve, reject) => {
    const image = canvas.createImage();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('分享照片加载失败'));
    image.src = src;
  });
}

function exportCanvas(canvas) {
  return new Promise((resolve, reject) => {
    wx.canvasToTempFilePath({
      canvas,
      fileType: 'jpg',
      quality: 0.92,
      success: (res) => resolve(res.tempFilePath),
      fail: reject
    });
  });
}

async function renderOne(canvas, size, invitation) {
  canvas.width = size.width;
  canvas.height = size.height;
  const ctx = canvas.getContext('2d');
  let image = null;
  try {
    image = await loadCanvasImage(canvas, invitation.cover);
  } catch (error) {
    console.error('分享卡片照片加载失败', error);
  }
  drawInvitationCard(ctx, {
    width: size.width,
    height: size.height,
    image,
    invitation
  });
  return exportCanvas(canvas);
}

async function renderInvitationCards(canvases, invitation) {
  const [messagePath, timelinePath] = await Promise.all([
    renderOne(canvases.message, MESSAGE_SIZE, invitation),
    renderOne(canvases.timeline, TIMELINE_SIZE, invitation)
  ]);
  return { messagePath, timelinePath };
}

module.exports = {
  buildShareTitle,
  renderInvitationCards
};
