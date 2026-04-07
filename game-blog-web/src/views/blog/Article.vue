<template>
  <div class="article-detail" v-loading="loading">
    <div v-if="article">
      <div class="header">
        <h1>{{ article.title }}</h1>
        <div class="meta">
          <span>{{ formatDate(article.createTime) }}</span>
          <span class="separator">|</span>
          <span>阅读: {{ article.views }}</span>
          <span class="separator">|</span>
          <el-tag size="small">{{ article.category }}</el-tag>
        </div>
      </div>
      
      <component
        :is="markdownEditor"
        v-if="markdownReady"
        :model-value="article.content"
        mode="preview"
        class="markdown-body"
      ></component>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { getMarkdownEditor } from '@/utils/markdown'

const route = useRoute()
const article = ref<any>(null)
const loading = ref(false)
const markdownReady = ref(false)
const markdownEditor = ref<any>(null)

const initMarkdown = async () => {
  markdownEditor.value = await getMarkdownEditor()
  markdownReady.value = true
}

const fetchArticle = async () => {
  loading.value = true
  try {
    const id = route.params.id
    const res = await request.get(`/articles/${id}`)
    article.value = res
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  initMarkdown()
  fetchArticle()
})
</script>

<style scoped>
.article-detail {
  background: #fff;
  padding: 26px;
  border-radius: var(--gg-radius-md);
  border: 1px solid var(--gg-border);
  box-shadow: var(--gg-shadow-soft);
}

.header {
  margin-bottom: 30px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 20px;
}

.header h1 {
  margin-bottom: 15px;
  color: #303133;
  line-height: 1.35;
}

.meta {
  color: #909399;
  font-size: 14px;
}

.separator {
  margin: 0 10px;
}

@media (max-width: 767px) {
  .article-detail {
    padding: 14px;
    border-radius: 10px;
  }

  .header {
    margin-bottom: 16px;
    padding-bottom: 14px;
  }

  .header h1 {
    font-size: 24px;
    margin-bottom: 10px;
  }

  .meta {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 6px;
  }

  .separator {
    display: none;
  }
}
</style>
