<template>
  <div class="admin-shell">
    <el-container class="admin-layout">
      <el-aside v-if="!isMobile" width="232px" class="aside">
        <div class="logo">Control Panel</div>
        <el-menu :router="true" :default-active="$route.path" class="menu">
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
        <div class="logout-wrapper">
          <el-button type="danger" plain @click="logout" style="width: 100%">退出登录</el-button>
        </div>
      </el-aside>

      <el-container>
        <header v-if="isMobile" class="topbar mobile">
          <div class="left">
            <el-button circle class="icon-btn" @click="mobileMenuVisible = true">
              <el-icon><Expand /></el-icon>
            </el-button>
          </div>
        </header>

        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <el-drawer
      v-model="mobileMenuVisible"
      direction="ltr"
      size="78%"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-logo">Control Panel</div>
      <el-menu :router="true" :default-active="$route.path" class="menu" @select="mobileMenuVisible = false">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="logout-wrapper mobile">
        <el-button type="danger" plain @click="logout" style="width: 100%">退出登录</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Edit, Expand, Monitor, Money, VideoCamera } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const MOBILE_BREAKPOINT = 1200
const isMobile = ref(window.innerWidth < MOBILE_BREAKPOINT)
const mobileMenuVisible = ref(false)

const menuItems = [
  { index: '/admin/articles', label: '文章管理', icon: Document },
  { index: '/admin/publish', label: '发布文章', icon: Edit },
  { index: '/admin/ai-video', label: 'AI 视频', icon: VideoCamera },
  { index: '/admin/ledger', label: '记账台账', icon: Money },
  { index: '/blog', label: '返回前台', icon: Monitor }
]

const handleResize = () => {
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
  if (!isMobile.value) {
    mobileMenuVisible.value = false
  }
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

watch(
  () => route.path,
  () => {
    if (isMobile.value) {
      mobileMenuVisible.value = false
    }
  }
)
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background: linear-gradient(160deg, #f3f7ff 0%, #f2faf7 100%);
}

.admin-layout {
  min-height: 100vh;
}

.aside {
  background: linear-gradient(180deg, #172554, #1e3a8a);
  color: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--gg-border);
}

.logo {
  height: 66px;
  line-height: 66px;
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.4px;
  background: rgba(15, 23, 42, 0.24);
}

.drawer-logo {
  height: 58px;
  line-height: 58px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
  border-radius: 12px;
  margin-bottom: 10px;
  background: rgba(30, 64, 175, 0.45);
}

.menu {
  border-right: none;
  background-color: transparent;
  flex: 1;
}

:deep(.el-menu-item) {
  color: #d7e0ef;
  margin: 2px 8px;
  border-radius: 10px;
}

:deep(.el-menu-item:hover), :deep(.el-menu-item.is-active) {
  background-color: rgba(15, 23, 42, 0.3);
  color: #bfdbfe;
}

.topbar {
  height: 64px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--gg-surface-glass);
  border-bottom: 1px solid var(--gg-border);
  backdrop-filter: blur(8px);
}

.topbar.mobile {
  position: sticky;
  top: 0;
  z-index: 9;
  height: 52px;
  padding: 0 10px;
  justify-content: flex-start;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title {
  font-size: 17px;
  font-weight: 700;
  color: var(--gg-text);
}

.icon-btn {
  border-color: rgba(59, 130, 246, 0.4);
}

.main {
  padding: 18px;
}

.logout-wrapper {
  padding: 16px;
  margin-top: auto;
  border-top: 1px solid var(--gg-border);
}

.logout-wrapper.mobile {
  border-top: none;
}

:deep(.mobile-drawer) {
  background: linear-gradient(180deg, #0f172a, #1e293b);
}

@media (max-width: 900px) {
  .main {
    padding: 12px;
  }
}

@media (max-width: 1200px) {
  .aside {
    display: none;
  }
}
</style>
