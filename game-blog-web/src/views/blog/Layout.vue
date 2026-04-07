<template>
  <el-container class="layout-container">
    <el-header height="64px" class="header">
      <div class="logo-wrap">
        <div class="logo-dot"></div>
        <div class="logo">Game Blog</div>
      </div>

      <el-menu v-if="!isMobile" mode="horizontal" :router="true" :default-active="$route.path" class="menu">
        <el-menu-item index="/blog">首页</el-menu-item>
        <el-menu-item index="/admin/articles">后台管理</el-menu-item>
      </el-menu>

      <el-button v-else class="mobile-menu-btn" circle @click="mobileMenuVisible = true">
        <el-icon><Expand /></el-icon>
      </el-button>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <el-footer height="44px" class="footer">
      © 2024 Game Blog. Powered by Vue 3 & Spring Boot.
    </el-footer>

    <el-drawer v-model="mobileMenuVisible" direction="rtl" size="72%" :with-header="false">
      <div class="mobile-menu-title">导航</div>
      <el-menu :router="true" :default-active="$route.path" @select="mobileMenuVisible = false">
        <el-menu-item index="/blog">首页</el-menu-item>
        <el-menu-item index="/admin/articles">后台管理</el-menu-item>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Expand } from '@element-plus/icons-vue'

const isMobile = ref(window.innerWidth < 768)
const mobileMenuVisible = ref(false)
const route = useRoute()

const handleResize = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) {
    mobileMenuVisible.value = false
  }
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
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(165deg, #f8fbff 0%, #f4faf5 100%);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--gg-border);
  padding: 0 20px;
  background: var(--gg-surface-glass);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(120deg, var(--gg-primary), var(--gg-primary-2));
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--gg-text);
}

.menu {
  border-bottom: none !important;
  flex: 1;
  justify-content: flex-end;
}

.mobile-menu-btn {
  border: 1px solid rgba(59, 130, 246, 0.35);
}

.mobile-menu-title {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--gg-text);
}

.main-content {
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px;
}

.footer {
  text-align: center;
  line-height: 44px;
  color: var(--gg-text-soft);
  border-top: 1px solid var(--gg-border);
  background: rgba(255, 255, 255, 0.55);
}

:deep(.el-menu--horizontal > .el-menu-item.is-active) {
  color: var(--gg-primary);
  border-bottom-color: var(--gg-primary);
}

:deep(.el-menu-item) {
  color: var(--gg-text-soft);
}

@media (max-width: 767px) {
  .header {
    padding: 0 12px;
  }

  .logo {
    font-size: 18px;
  }

  .main-content {
    padding: 14px 10px;
  }
}
</style>
