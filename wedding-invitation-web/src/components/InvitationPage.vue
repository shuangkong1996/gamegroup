<script setup lang="ts">
import QRCode from 'qrcode'
import { onMounted, onUnmounted, ref } from 'vue'
import { invitation } from '../data/invitation'
import { submitRsvp } from '../services/rsvp'

const sceneNames = ['序章', '相遇', '心动', '吉日', '喜宴', '赴约']
const scrollRoot = ref<HTMLElement | null>(null)
const qrCanvas = ref<HTMLCanvasElement | null>(null)
const activeScene = ref(0)
const visibleScenes = ref(sceneNames.map((_, index) => index === 0))
const attendance = ref<'yes' | 'no'>('yes')
const guestName = ref('')
const guestCount = ref(1)
const submitting = ref(false)
const submitMessage = ref('')
const remaining = ref({ days: 0, hours: 0, minutes: 0 })
const weddingDate = new Date(invitation.date)
const dateLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
  timeZone: 'Asia/Shanghai'
}).format(weddingDate)
const lunarDateLabel = '农历丙午年十月初八'
let countdownTimer: ReturnType<typeof setInterval> | undefined
let observer: IntersectionObserver | undefined

function updateCountdown() {
  const difference = Math.max(0, weddingDate.getTime() - Date.now())
  remaining.value = {
    days: Math.floor(difference / 86_400_000),
    hours: Math.floor((difference % 86_400_000) / 3_600_000),
    minutes: Math.floor((difference % 3_600_000) / 60_000)
  }
}

function scrollToScene(index: number) {
  const scene = scrollRoot.value?.querySelector<HTMLElement>(`[data-scene="${index}"]`)
  scene?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleKeydown(event: KeyboardEvent) {
  if (event.target instanceof HTMLElement && event.target.closest('input, select, textarea')) return
  if (event.key === 'ArrowDown' || event.key === 'PageDown') {
    event.preventDefault()
    scrollToScene(Math.min(sceneNames.length - 1, activeScene.value + 1))
  }
  if (event.key === 'ArrowUp' || event.key === 'PageUp') {
    event.preventDefault()
    scrollToScene(Math.max(0, activeScene.value - 1))
  }
}

async function handleSubmit() {
  submitMessage.value = ''
  if (!guestName.value.trim()) {
    submitMessage.value = '请填写您的姓名。'
    return
  }

  submitting.value = true
  try {
    await submitRsvp({
      name: guestName.value.trim(),
      attending: attendance.value === 'yes',
      guestCount: attendance.value === 'yes' ? guestCount.value : 0
    })
    submitMessage.value = '谢谢回复，我们期待与你相见。'
    guestName.value = ''
  } catch (error) {
    submitMessage.value = error instanceof Error ? error.message : '提交失败，请稍后再试。'
  } finally {
    submitting.value = false
  }
}

async function saveQrCode() {
  if (!qrCanvas.value) return
  const link = document.createElement('a')
  link.download = 'deng-rui-chen-shuang-wedding.png'
  link.href = await QRCode.toDataURL(window.location.href, {
    width: 360,
    margin: 2,
    color: { dark: '#271f1b', light: '#ffffff' }
  })
  link.click()
}

onMounted(() => {
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 60_000)
  window.addEventListener('keydown', handleKeydown)

  if (scrollRoot.value) {
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const index = Number((entry.target as HTMLElement).dataset.scene)
          const isVisible = entry.isIntersecting && entry.intersectionRatio >= 0.55
          visibleScenes.value[index] = isVisible
          if (isVisible) activeScene.value = index
        }
      },
      { root: scrollRoot.value, threshold: [0, 0.55, 0.8] }
    )
    scrollRoot.value.querySelectorAll<HTMLElement>('[data-scene]').forEach((scene) => observer?.observe(scene))
  }

  if (qrCanvas.value) {
    QRCode.toCanvas(qrCanvas.value, window.location.href, {
      width: 152,
      margin: 1,
      color: { dark: '#271f1b', light: '#ffffff' }
    })
  }
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  observer?.disconnect()
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <main class="preview-scene">
    <aside class="desktop-rail left-rail" aria-label="新人信息">
      <p class="rail-kicker">THE WEDDING OF</p>
      <h2 class="rail-couple">{{ invitation.groom }}<span>&</span>{{ invitation.bride }}</h2>
      <p class="rail-date">{{ dateLabel }}</p>
      <p class="rail-lunar">{{ lunarDateLabel }}</p>
      <figure class="rail-photo"><img :src="invitation.images.moments[0]" alt="邓睿与陈爽合照" /></figure>
      <span class="rail-index">SUINING · SICHUAN · 2026</span>
    </aside>

    <section class="device-shell" aria-label="婚礼邀请函">
      <header class="device-header">
        <span class="device-camera"></span>
        <span>THE WEDDING INVITATION</span>
      </header>

      <div ref="scrollRoot" class="invitation-scroll">
        <section class="scene scene-cover" :class="{ 'is-visible': visibleScenes[0] }" data-scene="0">
          <img class="cover-photo" :src="invitation.images.cover" alt="邓睿与陈爽婚纱照" fetchpriority="high" />
          <div class="cover-shade"></div>
          <div class="cover-lines" aria-hidden="true"></div>
          <div class="cover-copy">
            <p class="scene-eyebrow cover-eyebrow">WITH JOY IN OUR HEARTS</p>
            <p class="cover-small-title">诚邀你见证</p>
            <h1><span>{{ invitation.groom }}</span><i>&</i><span>{{ invitation.bride }}</span></h1>
            <div class="cover-divider"><span></span><b>囍</b><span></span></div>
            <p class="cover-date">{{ dateLabel }}<span>{{ lunarDateLabel }}</span></p>
          </div>
          <p class="cover-side-note">TO OUR FOREVER</p>
          <div class="cover-bottom"><span>SCROLL TO BEGIN</span><i aria-hidden="true">↓</i></div>
        </section>

        <section class="scene scene-couple" :class="{ 'is-visible': visibleScenes[1] }" data-scene="1">
          <div class="scene-heading couple-heading">
            <p class="scene-eyebrow">TWO SOULS, ONE STORY</p>
            <h2>你好，<em>余生</em></h2>
            <p>从此以后，每一个平凡日子都有了回响。</p>
          </div>
          <div class="portrait-pair">
            <figure class="portrait portrait-groom">
              <img :src="invitation.images.groom" alt="新郎邓睿" loading="lazy" />
              <figcaption><span>GROOM</span><strong>{{ invitation.groom }}</strong></figcaption>
            </figure>
            <figure class="portrait portrait-bride">
              <img :src="invitation.images.bride" alt="新娘陈爽" loading="lazy" />
              <figcaption><span>BRIDE</span><strong>{{ invitation.bride }}</strong></figcaption>
            </figure>
            <span class="portrait-ampersand" aria-hidden="true">&</span>
          </div>
          <p class="chapter-footnote">A PROMISE FOR ALL THE DAYS TO COME</p>
        </section>

        <section class="scene scene-moments" :class="{ 'is-visible': visibleScenes[2] }" data-scene="2">
          <div class="scene-heading moments-heading">
            <p class="scene-eyebrow">LITTLE MOMENTS, BIG LOVE</p>
            <h2>心动的每一刻</h2>
          </div>
          <div class="moment-grid">
            <figure v-for="(image, index) in invitation.images.moments" :key="image" :class="`moment moment-${index + 1}`">
              <img :src="image" :alt="`邓睿与陈爽的纪念照片 ${index + 1}`" loading="lazy" />
              <figcaption>0{{ index + 1 }}</figcaption>
            </figure>
          </div>
          <p class="moments-caption">愿把往后的春夏秋冬，都过成喜欢的模样。</p>
        </section>

        <section class="scene scene-date" :class="{ 'is-visible': visibleScenes[3] }" data-scene="3">
          <img class="date-photo" :src="invitation.images.moments[0]" alt="新人婚纱照" loading="lazy" />
          <div class="date-shade"></div>
          <div class="date-copy">
            <p class="scene-eyebrow">PLEASE SAVE THE DATE</p>
            <p class="date-intro">谨定于</p>
            <div class="date-numbers" aria-label="2026年11月16日">
              <span>2026</span><i>.</i><span>11</span><i>.</i><strong>16</strong>
            </div>
            <p class="date-weekday">{{ dateLabel }}<span>{{ lunarDateLabel }}</span></p>
            <div class="countdown">
              <span>距离相聚</span>
              <strong>{{ remaining.days }}<i>天</i> {{ String(remaining.hours).padStart(2, '0') }}<i>时</i> {{ String(remaining.minutes).padStart(2, '0') }}<i>分</i></strong>
            </div>
          </div>
          <span class="date-vertical">A DAY TO REMEMBER</span>
        </section>

        <section class="scene scene-venue" :class="{ 'is-visible': visibleScenes[4] }" data-scene="4">
          <figure class="venue-image">
            <img :src="invitation.images.moments[3]" alt="新人婚纱照" loading="lazy" />
          </figure>
          <div class="venue-panel">
            <p class="scene-eyebrow">MEET US IN SUINING</p>
            <h2>喜宴地点</h2>
            <span class="venue-rule"></span>
            <strong>{{ invitation.venue }}</strong>
            <p>{{ invitation.address }}</p>
            <a class="map-button" :href="invitation.mapUrl" target="_blank" rel="noreferrer">
              <span>打开地图导航</span><i aria-hidden="true">↗</i>
            </a>
          </div>
          <span class="venue-coordinates">30° 22' N&nbsp;&nbsp; 105° 28' E</span>
        </section>

        <section class="scene scene-rsvp" :class="{ 'is-visible': visibleScenes[5] }" data-scene="5">
          <img class="rsvp-photo" :src="invitation.images.moments[1]" alt="邓睿与陈爽婚纱照" loading="lazy" />
          <div class="rsvp-shade"></div>
          <div class="rsvp-content">
            <p class="scene-eyebrow">WE SAVED YOU A SEAT</p>
            <h2>盼你赴约</h2>
            <p class="rsvp-intro">你的到来，会让这一天更加圆满。</p>
            <form class="rsvp-form" @submit.prevent="handleSubmit">
              <label for="guest-name">您的姓名</label>
              <input id="guest-name" v-model="guestName" autocomplete="name" maxlength="40" placeholder="请输入姓名" required />
              <fieldset>
                <legend>是否出席</legend>
                <div class="attendance-choices">
                  <label class="attendance-choice"><input v-model="attendance" type="radio" value="yes" /><span>欣然前往</span></label>
                  <label class="attendance-choice"><input v-model="attendance" type="radio" value="no" /><span>遗憾缺席</span></label>
                </div>
              </fieldset>
              <label v-if="attendance === 'yes'" for="guest-count">出席人数</label>
              <select v-if="attendance === 'yes'" id="guest-count" v-model.number="guestCount">
                <option v-for="count in 6" :key="count" :value="count">{{ count }} 位</option>
              </select>
              <button type="submit" :disabled="submitting">{{ submitting ? '正在发送…' : '确认回复' }} <span aria-hidden="true">↗</span></button>
              <p class="submit-message" aria-live="polite">{{ submitMessage }}</p>
            </form>
            <p class="rsvp-signature">{{ invitation.groom }} & {{ invitation.bride }}</p>
          </div>
          <span class="rsvp-seal" aria-hidden="true">囍</span>
        </section>
      </div>

      <nav class="scene-controls" aria-label="邀请函章节导航">
        <button type="button" aria-label="上一章节" :disabled="activeScene === 0" @click="scrollToScene(activeScene - 1)">↑</button>
        <div class="scene-progress">
          <button
            v-for="(name, index) in sceneNames"
            :key="name"
            type="button"
            :class="{ 'progress-active': activeScene === index }"
            :aria-label="`前往${name}`"
            :aria-current="activeScene === index ? 'step' : undefined"
            @click="scrollToScene(index)"
          ></button>
        </div>
        <span class="scene-counter">{{ String(activeScene + 1).padStart(2, '0') }} / {{ String(sceneNames.length).padStart(2, '0') }}</span>
        <button type="button" aria-label="下一章节" :disabled="activeScene === sceneNames.length - 1" @click="scrollToScene(activeScene + 1)">↓</button>
      </nav>
    </section>

    <aside class="desktop-rail right-rail" aria-label="婚礼日期和分享">
      <div class="rail-rule"></div>
      <p class="rail-kicker">A DATE TO CELEBRATE</p>
      <h2>2026<br /><span>· 11 ·</span><br />16</h2>
      <p class="rail-lunar">{{ lunarDateLabel }}</p>
      <p class="rail-copy">{{ invitation.venue }}<br />{{ invitation.address }}</p>
      <button type="button" class="qr-button rail-qr" aria-label="保存婚礼邀请函二维码" @click="saveQrCode">
        <canvas ref="qrCanvas" width="152" height="152"></canvas>
        <span>保存邀请函</span>
      </button>
      <div class="rail-turn"><span>SCROLL TO EXPLORE</span><i aria-hidden="true">↓</i></div>
    </aside>
  </main>
</template>

<style scoped>
.preview-scene {
  position: relative;
  display: grid;
  grid-template-columns: minmax(190px, 1fr) minmax(300px, min(440px, calc((100svh - 68px) * .52))) minmax(190px, 1fr);
  align-items: center;
  gap: clamp(28px, 5vw, 88px);
  width: 100%;
  min-height: 100svh;
  overflow: hidden;
  padding: 34px clamp(32px, 7vw, 120px);
  background: linear-gradient(115deg, #eee9e1 0%, #faf8f4 47%, #ede5de 100%);
}

.preview-scene::before,
.preview-scene::after {
  position: absolute;
  right: -8%;
  bottom: -190px;
  left: -8%;
  height: 300px;
  border-radius: 50% 50% 0 0 / 35% 35% 0 0;
  background: rgb(224 210 192 / 32%);
  content: "";
  pointer-events: none;
}

.preview-scene::after {
  right: 55%;
  bottom: -215px;
  left: -24%;
  height: 240px;
  background: rgb(240 231 218 / 74%);
}

.device-shell {
  position: relative;
  z-index: 1;
  display: flex;
  grid-column: 2;
  grid-row: 1;
  justify-self: center;
  width: min(100%, 440px, calc((100svh - 68px) * .52));
  height: min(920px, calc(100svh - 68px));
  min-height: 620px;
  flex-direction: column;
  padding: 59px 13px 13px;
  border: 11px solid rgb(255 255 255 / 86%);
  border-radius: 42px;
  background: #fff;
  box-shadow: 0 26px 84px rgb(48 35 27 / 18%);
}

.device-shell::before {
  position: absolute;
  top: -1px;
  left: 50%;
  width: 176px;
  height: 27px;
  border-radius: 0 0 18px 18px;
  background: #fff8f4;
  content: "";
  transform: translateX(-50%);
}

.device-header {
  position: absolute;
  top: 22px;
  left: 0;
  display: grid;
  width: 100%;
  place-items: center;
  color: #8a837b;
  font: 9px Georgia, serif;
  letter-spacing: 2px;
}

.device-camera {
  position: absolute;
  top: -17px;
  left: calc(50% - 47px);
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c7c1bb;
}

.device-camera::after {
  position: absolute;
  top: 1px;
  left: 21px;
  width: 51px;
  height: 5px;
  border-radius: 8px;
  background: #bbb6b0;
  content: "";
}

.invitation-scroll {
  position: relative;
  width: 100%;
  min-height: 0;
  flex: 1;
  overflow: auto;
  border-radius: 24px;
  background: #211b18;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
}

.invitation-scroll::-webkit-scrollbar { display: none; }

.scene {
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 100%;
  flex-direction: column;
  overflow: hidden;
  isolation: isolate;
  scroll-snap-align: start;
  scroll-snap-stop: always;
}

.scene-eyebrow {
  margin: 0;
  color: #c7a879;
  font: 9px Georgia, serif;
  letter-spacing: 2.4px;
}

.scene-cover { justify-content: center; background: #36302d; color: #fff8ee; }
.cover-photo, .date-photo, .rsvp-photo { position: absolute; z-index: -2; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.cover-photo { object-position: center 43%; transform: scale(1.18); }
.cover-shade { position: absolute; z-index: -1; inset: 0; background: linear-gradient(180deg, rgb(24 18 19 / 12%) 0%, rgb(26 19 19 / 14%) 32%, rgb(31 19 19 / 70%) 100%); }
.cover-lines { position: absolute; inset: 18px; border: 1px solid rgb(240 218 183 / 56%); opacity: 0; }
.cover-lines::before, .cover-lines::after { position: absolute; top: 0; bottom: 0; width: 10px; border: 1px solid rgb(240 218 183 / 24%); content: ""; }
.cover-lines::before { left: 7px; }
.cover-lines::after { right: 7px; }
.cover-copy { position: relative; display: flex; height: 100%; flex-direction: column; align-items: center; justify-content: center; padding: 34px 20px; text-align: center; }
.cover-eyebrow { margin-bottom: 34px; color: #eee0c9; opacity: 0; }
.cover-small-title { margin: 0 0 12px; color: #f5dfbd; font-family: "Songti SC", serif; font-size: 14px; letter-spacing: 8px; opacity: 0; }
.cover-copy h1 { display: flex; flex-direction: column; align-items: center; gap: 3px; margin: 0; color: #fff8ee; font-family: "Songti SC", "Noto Serif CJK SC", serif; font-size: 55px; font-weight: 500; line-height: 1.18; text-shadow: 0 5px 24px rgb(0 0 0 / 24%); }
.cover-copy h1 span { opacity: 0; transform: translateY(30px); }
.cover-copy h1 i { color: #dbbb88; font: italic 19px Georgia, serif; opacity: 0; }
.cover-divider { display: flex; align-items: center; gap: 13px; margin-top: 25px; color: #f0d6ae; opacity: 0; }
.cover-divider span { width: 42px; height: 1px; background: rgb(240 214 174 / 70%); }
.cover-divider b { font-family: "Songti SC", serif; font-size: 20px; font-weight: 400; }
.cover-date { display: flex; flex-direction: column; gap: 6px; margin: 14px 0 0; color: #f3e8d6; font-family: "Songti SC", serif; font-size: 12px; letter-spacing: 2px; opacity: 0; }
.cover-date span, .date-weekday span { color: #e7d2b5; font-size: 11px; letter-spacing: 1px; }
.cover-side-note { position: absolute; top: 50%; right: 19px; margin: 0; color: rgb(255 248 238 / 72%); font: 8px Georgia, serif; letter-spacing: 2px; writing-mode: vertical-rl; opacity: 0; transform: translateY(-50%); }
.cover-bottom { position: absolute; right: 0; bottom: 76px; left: 0; display: flex; flex-direction: column; align-items: center; gap: 8px; color: rgb(255 248 238 / 76%); font: 8px Georgia, serif; letter-spacing: 2px; opacity: 0; }
.cover-bottom i { color: #eed2a9; font-size: 17px; font-style: normal; }

.scene-couple { justify-content: center; padding: 45px 26px 82px; background: #f4eee5; color: #443933; }
.scene-heading { position: relative; z-index: 2; text-align: center; }
.scene-heading h2 { margin: 12px 0 8px; color: #4d2928; font-family: "Songti SC", serif; font-size: 28px; font-weight: 500; }
.scene-heading h2 em { color: #9d6b55; font-style: normal; }
.scene-heading > p:last-child { margin: 0; color: #897c70; font-family: "Songti SC", serif; font-size: 12px; }
.couple-heading { opacity: 0; transform: translateY(18px); }
.portrait-pair { position: relative; display: grid; min-height: 0; flex: 1; grid-template-columns: 1fr 1fr; align-items: center; gap: 12px; margin: 28px 0 20px; }
.portrait { position: relative; height: 100%; max-height: 550px; min-height: 300px; margin: 0; overflow: hidden; background: #ddd5cc; }
.portrait img { display: block; width: 100%; height: 100%; object-fit: cover; }
.portrait-groom { clip-path: inset(0 100% 0 0); }
.portrait-bride { clip-path: inset(0 0 0 100%); }
.portrait-groom img { object-position: 53% center; }
.portrait-bride img { object-position: 50% center; }
.portrait figcaption { position: absolute; right: 0; bottom: 0; left: 0; display: flex; flex-direction: column; gap: 3px; padding: 29px 10px 12px; background: linear-gradient(transparent, rgb(29 22 20 / 65%)); color: #fff9f1; }
.portrait figcaption span { font: 8px Georgia, serif; letter-spacing: 1.5px; }
.portrait figcaption strong { font-family: "Songti SC", serif; font-size: 15px; font-weight: 500; }
.portrait-ampersand { position: absolute; top: 50%; left: 50%; display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid rgb(247 241 231 / 80%); border-radius: 50%; background: #8d4e42; color: #fff6e8; font: italic 19px Georgia, serif; opacity: 0; transform: translate(-50%, -50%) scale(.4); }
.chapter-footnote { margin: 0; color: #9b8d80; font: 8px Georgia, serif; letter-spacing: 1.6px; text-align: center; opacity: 0; }

.scene-moments { justify-content: center; padding: 38px 25px 82px; background: #e9dfd3; color: #433833; }
.moments-heading { margin-bottom: 18px; opacity: 0; transform: translateY(16px); }
.moments-heading h2 { margin-bottom: 0; }
.moment-grid { display: grid; min-height: 0; flex: 1; grid-template-columns: 1.1fr .9fr; grid-template-rows: 1fr 1fr; gap: 11px; }
.moment { position: relative; min-height: 0; margin: 0; overflow: hidden; background: #d1c7bb; opacity: 0; }
.moment img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform 1000ms cubic-bezier(.2,.7,.2,1); }
.moment figcaption { position: absolute; right: 9px; bottom: 8px; color: #fff6ec; font: 9px Georgia, serif; letter-spacing: 1px; text-shadow: 0 1px 8px #241b17; }
.moment-1 { grid-row: span 2; transform: translateY(25px) rotate(-2deg); }
.moment-2 { transform: translateX(24px) rotate(2deg); }
.moment-3 { transform: translateX(24px) rotate(-1deg); }
.moment-4 { display: none; }
.moments-caption { margin: 18px 0 0; color: #736459; font-family: "Songti SC", serif; font-size: 12px; text-align: center; opacity: 0; }

.scene-date { align-items: center; justify-content: center; background: #25201d; color: #fff8ee; text-align: center; }
.date-photo { object-position: 52% 42%; transform: scale(1.15); }
.date-shade { position: absolute; z-index: -1; inset: 0; background: linear-gradient(180deg, rgb(27 21 20 / 36%), rgb(34 24 24 / 70%)); }
.date-copy { display: flex; width: 100%; height: 100%; flex-direction: column; align-items: center; justify-content: center; padding: 36px 18px 88px; }
.date-intro { margin: 35px 0 8px; color: #eadcc8; font-family: "Songti SC", serif; font-size: 14px; letter-spacing: 5px; opacity: 0; }
.date-numbers { display: flex; align-items: baseline; gap: 4px; color: #fff7e9; font-family: Georgia, serif; font-size: 39px; font-weight: 400; letter-spacing: 0; clip-path: inset(0 50% 0 50%); }
.date-numbers i { color: #d8bb8c; font-size: 24px; font-style: normal; }
.date-numbers strong { color: #ecd1a4; font-size: 53px; font-weight: 400; }
.date-weekday { display: flex; flex-direction: column; gap: 6px; margin: 13px 0 0; color: #ebdfcf; font-family: "Songti SC", serif; font-size: 12px; letter-spacing: 2px; opacity: 0; }
.countdown { display: flex; flex-direction: column; align-items: center; gap: 10px; margin-top: auto; color: #e8d7c1; font-family: "Songti SC", serif; font-size: 11px; opacity: 0; }
.countdown strong { color: #fff3df; font: 16px Georgia, serif; }
.countdown i { padding-left: 2px; color: #cfb58b; font: 10px "Songti SC", serif; }
.date-vertical { position: absolute; top: 50%; right: 17px; color: rgb(255 246 232 / 58%); font: 8px Georgia, serif; letter-spacing: 2px; writing-mode: vertical-rl; opacity: 0; }

.scene-venue { justify-content: center; padding: 30px 24px; background: #f2e9df; color: #463a34; }
.venue-image { position: absolute; top: 0; right: 0; width: 100%; height: 57%; margin: 0; overflow: hidden; clip-path: inset(0 0 100%); }
.venue-image img { display: block; width: 100%; height: 100%; object-fit: cover; object-position: center 39%; transform: scale(1.1); }
.venue-panel { position: relative; width: 100%; margin-top: auto; padding: 23px 22px 18px; background: #f8f3ed; opacity: 0; transform: translateY(30px); }
.venue-panel h2 { margin: 8px 0 13px; color: #562f2b; font-family: "Songti SC", serif; font-size: 25px; font-weight: 500; }
.venue-rule { display: block; width: 34px; height: 1px; margin-bottom: 15px; background: #ae8864; }
.venue-panel > strong { display: block; color: #483b33; font-family: "Songti SC", serif; font-size: 16px; font-weight: 500; }
.venue-panel > p:not(.scene-eyebrow) { margin: 8px 0 17px; color: #817468; font-family: "Songti SC", serif; font-size: 12px; line-height: 1.8; }
.map-button { display: flex; min-height: 42px; align-items: center; justify-content: space-between; padding: 0 13px; border: 1px solid #b98e6f; color: #70443a; font-family: "Songti SC", serif; font-size: 12px; }
.map-button i { font: normal 17px Georgia, serif; }
.venue-coordinates { position: absolute; top: 18px; right: 15px; color: #fff8ef; font: 8px Georgia, serif; letter-spacing: 1px; text-shadow: 0 1px 8px #211b17; opacity: 0; }

.scene-rsvp { align-items: center; justify-content: center; background: #25201d; color: #fff8ed; }
.rsvp-photo { object-position: 52% center; transform: scale(1.15); }
.rsvp-shade { position: absolute; z-index: -1; inset: 0; background: linear-gradient(180deg, rgb(30 24 22 / 30%), rgb(35 25 24 / 88%)); }
.rsvp-content { width: 100%; max-height: 100%; overflow-y: auto; padding: 33px 27px 55px; scrollbar-width: none; }
.rsvp-content h2 { margin: 11px 0 6px; color: #fff6e9; font-family: "Songti SC", serif; font-size: 30px; font-weight: 500; opacity: 0; transform: translateY(18px); }
.rsvp-intro { margin: 0 0 21px; color: #e0d0bd; font-family: "Songti SC", serif; font-size: 12px; opacity: 0; }
.rsvp-form { display: grid; gap: 8px; opacity: 0; transform: translateY(22px); }
.rsvp-form > label, .rsvp-form legend { color: #e9d7c1; font-family: "Songti SC", serif; font-size: 11px; }
.rsvp-form > input, .rsvp-form > select { width: 100%; height: 38px; padding: 0 10px; border: 1px solid rgb(235 218 197 / 42%); border-radius: 0; outline-color: #d4b38a; background: rgb(255 255 255 / 8%); color: #fff8ee; font: inherit; font-size: 12px; }
.rsvp-form > input::placeholder { color: rgb(255 248 238 / 52%); }
.rsvp-form > select option { color: #3e342f; }
.rsvp-form fieldset { display: grid; gap: 8px; margin: 6px 0 4px; padding: 0; border: 0; }
.rsvp-form legend { margin-bottom: 0; }
.attendance-choices { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }
.attendance-choice { position: relative; display: flex; min-height: 36px; flex: 1; align-items: center; justify-content: center; border: 1px solid rgb(235 218 197 / 42%); color: #e9d7c1; font-family: "Songti SC", serif; font-size: 11px; }
.attendance-choice:has(input:checked) { border-color: #dbc19c; background: rgb(211 177 132 / 22%); color: #fff2dd; }
.attendance-choice input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.attendance-choice:focus-within { outline: 2px solid #d4b38a; outline-offset: 2px; }
.rsvp-form button { display: flex; min-height: 42px; align-items: center; justify-content: space-between; margin-top: 4px; padding: 0 13px; border: 1px solid #ead5b6; background: #ead5b6; color: #3a2b27; font-family: "Songti SC", serif; font-size: 13px; }
.rsvp-form button:disabled { opacity: .6; }
.submit-message { min-height: 16px; margin: 0; color: #f1d5ab; font-size: 11px; }
.rsvp-signature { margin: 17px 0 0; color: #e1c8a7; font-family: "Songti SC", serif; font-size: 13px; letter-spacing: 2px; opacity: 0; }
.rsvp-seal { position: absolute; right: 19px; bottom: 20px; display: grid; width: 36px; height: 36px; place-items: center; border: 1px solid rgb(239 210 170 / 74%); color: #eed5b2; font-family: "Songti SC", serif; opacity: 0; transform: rotate(-30deg) scale(.6); }

.scene-controls { position: absolute; z-index: 5; right: auto; bottom: 20px; left: 50%; display: flex; width: max-content; height: 40px; align-items: center; justify-content: center; gap: 13px; padding: 3px 9px; border: 1px solid rgb(255 249 239 / 25%); border-radius: 24px; background: rgb(38 30 26 / 57%); backdrop-filter: blur(12px); transform: translateX(-50%); }
.scene-controls > button { display: grid; width: 30px; height: 30px; place-items: center; border: 1px solid rgb(255 249 239 / 54%); border-radius: 50%; background: rgb(34 28 25 / 30%); color: #fff7ea; font-size: 14px; }
.scene-controls > button:disabled { opacity: .3; }
.scene-progress { display: flex; align-items: center; gap: 6px; }
.scene-progress button { width: 5px; height: 5px; padding: 0; border: 0; border-radius: 50%; background: rgb(255 248 238 / 55%); transition: width 350ms ease, border-radius 350ms ease, background 350ms ease; }
.scene-progress button.progress-active { width: 20px; border-radius: 5px; background: #e8cfaa; }
.scene-counter { min-width: 39px; color: #fff6e9; font: 9px Georgia, serif; letter-spacing: 1px; }

.desktop-rail { position: relative; z-index: 1; max-width: 330px; color: #403a34; }
.left-rail { grid-column: 1; grid-row: 1; justify-self: end; width: 100%; padding: 18px 0 18px 18px; }
.right-rail { grid-column: 3; grid-row: 1; justify-self: start; width: 100%; padding: 18px 18px 18px 0; }
.rail-kicker { margin: 0 0 17px; color: #9a7657; font: 9px Georgia, serif; letter-spacing: 2px; }
.desktop-rail h2 { margin: 0; color: #56322d; font-family: "Songti SC", serif; font-size: 31px; font-weight: 500; line-height: 1.42; }
.desktop-rail h2 span { color: #a17c60; font: italic 19px Georgia, serif; }
.desktop-rail h2.rail-couple { display: flex; align-items: center; gap: 9px; white-space: nowrap; font-size: 25px; }
.desktop-rail h2.rail-couple span { font-size: 17px; }
.rail-date { margin: 12px 0 20px; color: #897d70; font-family: "Songti SC", serif; font-size: 12px; }
.rail-lunar { margin: -12px 0 18px; color: #9a7657; font-family: "Songti SC", serif; font-size: 11px; }
.rail-photo { width: 100%; margin: 0; overflow: hidden; }
.rail-photo img { display: block; width: 100%; aspect-ratio: 1.12; object-fit: cover; object-position: 50% 22%; filter: saturate(.83); }
.rail-index { display: block; margin-top: 19px; color: #a19789; font: 8px Georgia, serif; letter-spacing: 1.6px; }
.rail-rule { width: 36px; height: 1px; margin: 0 0 22px; background: #9c744e; }
.rail-copy { margin: 10px 0 22px; color: #83786c; font-family: "Songti SC", serif; font-size: 12px; line-height: 1.9; }
.qr-button { display: grid; width: max-content; justify-items: start; gap: 7px; padding: 10px; border: 1px solid #e2d9ce; background: rgb(255 255 255 / 60%); color: #82796e; font: 10px "Songti SC", serif; }
.rail-qr canvas { display: block; width: 112px; height: 112px; }
.rail-turn { display: flex; align-items: center; gap: 10px; margin-top: 22px; color: #9b8d7d; font: 8px Georgia, serif; letter-spacing: 1.5px; }
.rail-turn i { color: #8f4c40; font-size: 16px; font-style: normal; }

.scene-couple.is-visible .couple-heading { animation: heading-rise 800ms cubic-bezier(.2,.8,.2,1) 180ms both; }
.scene-couple.is-visible .portrait-groom { animation: reveal-left 1050ms cubic-bezier(.2,.7,.2,1) 100ms forwards; }
.scene-couple.is-visible .portrait-bride { animation: reveal-right 1050ms cubic-bezier(.2,.7,.2,1) 280ms forwards; }
.scene-couple.is-visible .portrait-ampersand { animation: seal-pop 750ms cubic-bezier(.2,.8,.2,1) 800ms forwards; }
.scene-couple.is-visible .chapter-footnote { animation: fade-in 700ms ease 1100ms forwards; }
.scene-moments.is-visible .moments-heading { animation: heading-rise 700ms ease 100ms both; }
.scene-moments.is-visible .moment-1 { animation: gallery-left 950ms cubic-bezier(.2,.75,.2,1) 180ms forwards; }
.scene-moments.is-visible .moment-2 { animation: gallery-right 850ms cubic-bezier(.2,.75,.2,1) 330ms forwards; }
.scene-moments.is-visible .moment-3 { animation: gallery-right 850ms cubic-bezier(.2,.75,.2,1) 500ms forwards; }
.scene-moments.is-visible .moment img { transform: scale(1); }
.scene-moments.is-visible .moments-caption { animation: fade-in 700ms ease 800ms forwards; }
.scene-date.is-visible .date-photo { animation: photo-drift 1500ms cubic-bezier(.2,.7,.2,1) both; }
.scene-date.is-visible .date-intro { animation: fade-in 700ms ease 250ms both; }
.scene-date.is-visible .date-numbers { animation: date-unmask 1000ms cubic-bezier(.2,.7,.2,1) 360ms both; }
.scene-date.is-visible .date-weekday { animation: fade-in 650ms ease 650ms both; }
.scene-date.is-visible .countdown { animation: fade-in 700ms ease 900ms both; }
.scene-date.is-visible .date-vertical { animation: fade-in 700ms ease 650ms both; }
.scene-venue.is-visible .venue-image { animation: venue-unveil 1100ms cubic-bezier(.2,.75,.2,1) both; }
.scene-venue.is-visible .venue-image img { animation: photo-drift 1400ms cubic-bezier(.2,.7,.2,1) both; }
.scene-venue.is-visible .venue-panel { animation: venue-rise 800ms cubic-bezier(.2,.75,.2,1) 350ms both; }
.scene-venue.is-visible .venue-coordinates { animation: fade-in 700ms ease 700ms both; }
.scene-rsvp.is-visible .rsvp-photo { animation: photo-drift 1400ms cubic-bezier(.2,.7,.2,1) both; }
.scene-rsvp.is-visible .rsvp-content h2 { animation: heading-rise 700ms ease 150ms both; }
.scene-rsvp.is-visible .rsvp-intro { animation: fade-in 700ms ease 300ms both; }
.scene-rsvp.is-visible .rsvp-form { animation: venue-rise 750ms cubic-bezier(.2,.75,.2,1) 420ms both; }
.scene-rsvp.is-visible .rsvp-signature { animation: fade-in 700ms ease 700ms both; }
.scene-rsvp.is-visible .rsvp-seal { animation: seal-pop 700ms cubic-bezier(.2,.8,.2,1) 600ms both; }

.scene-cover.is-visible .cover-photo { animation: hero-drift 1500ms cubic-bezier(.2,.7,.2,1) both; }
.scene-cover.is-visible .cover-lines { animation: frame-in 900ms ease 350ms both; }
.scene-cover.is-visible .cover-eyebrow { animation: fade-in 700ms ease 450ms both; }
.scene-cover.is-visible .cover-small-title { animation: fade-in 800ms ease 650ms both; }
.scene-cover.is-visible .cover-copy h1 span:first-child { animation: name-arrive-left 900ms cubic-bezier(.2,.75,.2,1) 500ms both; }
.scene-cover.is-visible .cover-copy h1 span:last-child { animation: name-arrive-right 900ms cubic-bezier(.2,.75,.2,1) 700ms both; }
.scene-cover.is-visible .cover-copy h1 i { animation: fade-in 500ms ease 850ms both; }
.scene-cover.is-visible .cover-divider { animation: fade-in 700ms ease 1000ms both; }
.scene-cover.is-visible .cover-date { animation: fade-in 700ms ease 1150ms both; }
.scene-cover.is-visible .cover-side-note { animation: fade-in 700ms ease 1100ms both; }
.scene-cover.is-visible .cover-bottom { animation: fade-in 700ms ease 1350ms both; }

@keyframes hero-drift { from { opacity: .5; transform: scale(1.18); } to { opacity: 1; transform: scale(1); } }
@keyframes photo-drift { from { transform: scale(1.15); } to { transform: scale(1); } }
@keyframes frame-in { from { opacity: 0; inset: 35px; } to { opacity: 1; inset: 18px; } }
@keyframes fade-in { to { opacity: 1; } }
@keyframes name-arrive-left { to { opacity: 1; transform: translateY(0); } }
@keyframes name-arrive-right { to { opacity: 1; transform: translateY(0); } }
@keyframes heading-rise { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: translateY(0); } }
@keyframes reveal-left { to { clip-path: inset(0); } }
@keyframes reveal-right { to { clip-path: inset(0); } }
@keyframes seal-pop { to { opacity: 1; transform: translate(-50%, -50%) scale(1); } }
@keyframes gallery-left { to { opacity: 1; transform: translateY(0) rotate(0); } }
@keyframes gallery-right { to { opacity: 1; transform: translateX(0) rotate(0); } }
@keyframes date-unmask { from { clip-path: inset(0 50% 0 50%); letter-spacing: 12px; } to { clip-path: inset(0); letter-spacing: 0; } }
@keyframes venue-unveil { from { clip-path: inset(0 0 100%); } to { clip-path: inset(0); } }
@keyframes venue-rise { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 900px) {
  .preview-scene { grid-template-columns: minmax(140px, 1fr) minmax(280px, min(400px, calc((100svh - 68px) * .52))) minmax(140px, 1fr); gap: 20px; padding-right: 28px; padding-left: 28px; }
  .desktop-rail h2 { font-size: 24px; }
  .rail-qr canvas { width: 90px; height: 90px; }
}

@media (max-width: 760px) and (min-width: 701px) {
  .preview-scene { grid-template-columns: minmax(0, 1fr) minmax(280px, min(380px, calc((100svh - 48px) * .52))) minmax(0, 1fr); gap: 8px; padding: 24px 16px; }
  .left-rail, .right-rail { padding-right: 0; padding-left: 0; }
  .desktop-rail h2 { font-size: 19px; }
  .rail-qr canvas { width: 66px; height: 66px; }
}

@media (max-width: 700px) {
  .preview-scene { display: block; min-height: 100dvh; padding: 0; background: #241e1b; }
  .preview-scene::before, .preview-scene::after, .device-shell::before, .device-header, .desktop-rail { display: none; }
  .device-shell { display: block; width: 100%; height: 100dvh; min-height: 0; padding: 0; border: 0; border-radius: 0; background: #241e1b; box-shadow: none; }
  .invitation-scroll { position: absolute; inset: 0; width: 100%; height: 100%; border-radius: 0; }
  .scene-cover { min-height: 100dvh; }
  .cover-copy h1 { font-size: 56px; }
  .scene-couple { padding: 56px 22px 84px; }
  .scene-moments { padding: 52px 21px 84px; }
  .moment-grid { gap: 8px; }
  .scene-date { min-height: 100dvh; }
  .scene-venue { min-height: 100dvh; padding: 22px 18px 52px; }
  .venue-image { height: 64%; }
  .venue-panel { padding: 19px 17px 15px; }
  .scene-rsvp { min-height: 100dvh; }
  .rsvp-content { padding: max(44px, env(safe-area-inset-top)) 24px calc(78px + env(safe-area-inset-bottom)); }
  .scene-controls { bottom: max(13px, env(safe-area-inset-bottom)); gap: 12px; }
  .scene-controls > button { width: 28px; height: 28px; }
}

@media (max-height: 740px) and (min-width: 701px) {
  .device-shell { min-height: 0; padding-top: 48px; }
  .scene-couple, .scene-moments { padding-top: 25px; padding-bottom: 28px; }
  .portrait { min-height: 230px; }
  .scene-venue { padding-top: 20px; padding-bottom: 20px; }
  .rsvp-content { padding-top: 25px; padding-bottom: 45px; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; animation-duration: 1ms !important; animation-delay: 0ms !important; }
}
</style>
