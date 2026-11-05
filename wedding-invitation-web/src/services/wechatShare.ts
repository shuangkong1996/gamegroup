import { invitation } from '../data/invitation'

interface WechatShareData {
  title: string
  desc: string
  link: string
  imgUrl: string
  type?: 'link'
  dataUrl?: string
}

interface WechatSdk {
  config(options: {
    debug: boolean
    appId: string
    timestamp: number
    nonceStr: string
    signature: string
    jsApiList: string[]
  }): void
  ready(callback: () => void): void
  error(callback: (error: unknown) => void): void
  updateAppMessageShareData(data: WechatShareData): void
  updateTimelineShareData(data: WechatShareData): void
}

interface ApiResponse<T> {
  code: number
  data?: T
}

interface Signature {
  appId: string
  timestamp: number
  nonceStr: string
  signature: string
}

declare global {
  interface Window {
    wx?: WechatSdk
  }
}

const sdkUrl = 'https://res.wx.qq.com/open/js/jweixin-1.6.0.js'
let sdkPromise: Promise<WechatSdk> | undefined

function loadWechatSdk(): Promise<WechatSdk> {
  if (window.wx) return Promise.resolve(window.wx)
  if (sdkPromise) return sdkPromise

  sdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = sdkUrl
    script.onload = () => window.wx ? resolve(window.wx) : reject(new Error('微信 SDK 未加载'))
    script.onerror = () => reject(new Error('微信 SDK 加载失败'))
    document.head.append(script)
  })
  return sdkPromise
}

export async function initializeWechatShare(): Promise<void> {
  if (!/MicroMessenger/i.test(navigator.userAgent)) return

  try {
    const pageUrl = window.location.href.split('#')[0]
    const response = await fetch(
      `/api/v1/wechat/js-sdk/signature?url=${encodeURIComponent(pageUrl)}`
    )
    if (!response.ok) throw new Error('获取微信分享签名失败')

    const result = (await response.json()) as ApiResponse<Signature>
    if (result.code !== 200 || !result.data) throw new Error('微信分享签名响应无效')

    const wx = await loadWechatSdk()
    const debug = new URLSearchParams(window.location.search).get('wxdebug') === '1'
    wx.config({
      debug,
      ...result.data,
      jsApiList: [
        'updateAppMessageShareData',
        'updateTimelineShareData'
      ]
    })

    const shareData: WechatShareData = {
      title: `${invitation.groom} & ${invitation.bride} 的婚礼邀请`,
      desc: '诚邀您参加我们的婚礼，见证爱情，分享美好时刻。',
      link: pageUrl,
      imgUrl: invitation.images.cover,
      type: 'link',
      dataUrl: ''
    }

    wx.ready(() => {
      wx.updateAppMessageShareData(shareData)
      wx.updateTimelineShareData(shareData)
    })
    wx.error((error) => {
      console.warn('微信分享初始化失败', error)
      if (debug) window.alert(`微信分享初始化失败：${JSON.stringify(error)}`)
    })
  } catch (error) {
    console.warn('微信分享初始化失败', error)
  }
}
