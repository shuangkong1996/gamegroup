<template>
  <div class="blog-home">
    <section class="home-hero">
      <h1>文章与灵感</h1>
      <p>记录开发、生活和想法的实验田</p>
    </section>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-else class="article-list">
      <el-card v-for="article in articles" :key="article.id" class="article-card" shadow="hover" @click="goDetail(article.id)">
        <template #header>
          <div class="card-header">
            <h3>{{ article.title }}</h3>
            <span class="date">{{ formatDate(article.createTime) }}</span>
          </div>
        </template>
        <div class="summary">
          {{ article.summary || '暂无摘要' }}
        </div>
        <div class="meta">
          <el-tag size="small" v-if="article.category">{{ article.category }}</el-tag>
          <span class="views">阅读: {{ article.views }}</span>
        </div>
      </el-card>
    </div>
    <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface Article {
  id: number
  title: string
  summary: string
  createTime: string
  views: number
  category: string
}

const router = useRouter()
const articles = ref<Article[]>([])
const loading = ref(false)

const fetchArticles = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/articles', { params: { status: 1 } }) // Only published
    articles.value = res.content || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goDetail = (id: number) => {
  router.push({ name: 'BlogArticle', params: { id } })
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.blog-home {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.home-hero {
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.9), rgba(14, 165, 233, 0.85));
  color: #eef6ff;
}

.home-hero h1 {
  margin: 0;
  font-size: 30px;
}

.home-hero p {
  margin: 6px 0 0;
  font-size: 14px;
  color: rgba(238, 246, 255, 0.9);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  cursor: pointer;
  transition: all 0.3s;
  border-radius: var(--gg-radius-md);
  border: 1px solid var(--gg-border);
}

.article-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.09);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
}

.date {
  color: #909399;
  font-size: 14px;
}

.summary {
  color: #606266;
  margin-bottom: 15px;
  line-height: 1.6;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 767px) {
  .home-hero {
    padding: 14px;
  }

  .home-hero h1 {
    font-size: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .meta {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>
