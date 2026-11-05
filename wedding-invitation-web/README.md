# Wedding Invitation

移动端优先的婚礼请帖 H5，基于 Vue 3、TypeScript 和 Vite。桌面端是三栏请帖展台，中心提供手机预览；请帖采用纵向沉浸式章节，每章使用不同的照片、遮罩和文字入场效果。

## 本地开发

```bash
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:3204`。`/api` 请求会代理到本地 `game-server` 的 `8080` 端口。

电脑端提供新人信息、手机外框预览和可保存的分享二维码；手机端全屏显示请帖。滚轮、上下滑动和章节导航均可浏览，请帖资料位于 `src/data/invitation.ts`，照片地址来自纪念册静态资源接口。

## 请帖内容

编辑 `src/data/invitation.ts` 中的姓名、日期、酒店地址、地图链接和静态资源图片地址。

## RSVP 接口约定

页面将提交到 `POST /api/v1/wedding/invitations/{slug}/rsvps`，请求体为：

```json
{
  "name": "宾客姓名",
  "attending": true,
  "guestCount": 2
}
```

后端通过 `game-server` 的婚礼 RSVP 接口写入回复；接口返回成功状态后，页面才会显示回复成功。

## 微信分享配置

微信公众号凭据与小程序凭据分开配置。后端部署时设置 `WX_OFFICIAL_ACCOUNT_APPID`、`WX_OFFICIAL_ACCOUNT_SECRET` 和 `WX_OFFICIAL_ACCOUNT_JS_SDK_DOMAIN`；AppSecret 仅保存在后端环境变量中。公众号后台需将 `wedding.shuangkong.top` 配置为 JS 接口安全域名，生产站点需通过 HTTPS 访问。

后端提供 `GET /api/v1/wechat/js-sdk/signature?url=...`，前端只在微信内置浏览器中请求该接口，并配置好友及朋友圈分享卡片。生产环境需确保婚礼站点的 `/api` 反向代理到 `game-server`；本地开发由 Vite 代理到 `localhost:8080`。

## 构建

```bash
npm run build
```
