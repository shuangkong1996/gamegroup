<template>
  <div class="article-list">
    <el-card>
      <template #header>
        <div class="header">
          <span>文章管理</span>
          <el-button type="primary" @click="$router.push('/admin/publish')">写文章</el-button>
        </div>
      </template>
      
      <el-table v-if="!isMobile" :data="articles" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="阅读" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ new Date(scope.row.createTime).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="editArticle(scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除吗?" @confirm="deleteArticle(scope.row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="mobile-list" v-loading="loading">
        <el-card v-for="item in articles" :key="item.id" class="mobile-item" shadow="never">
          <div class="mobile-title">{{ item.title }}</div>
          <div class="mobile-meta">
            <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">
              {{ item.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
            <span>{{ item.category || '未分类' }}</span>
            <span>阅读 {{ item.views }}</span>
          </div>
          <div class="mobile-time">{{ new Date(item.createTime).toLocaleString() }}</div>
          <div class="mobile-actions">
            <el-button link type="primary" @click="editArticle(item)">编辑</el-button>
            <el-popconfirm title="确定删除吗?" @confirm="deleteArticle(item.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </el-card>
      </div>
      
      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
interface AdminArticle {
  id: number
  title: string
  category?: string
  status: number
  views: number
  createTime: string
}

const articles = ref<AdminArticle[]>([])
const loading = ref(false)
const total = ref(0)
const pageSize = 10
const currentPage = ref(1)
const isMobile = ref(window.innerWidth < 768)

const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}

const fetchArticles = async (page = 1) => {
  loading.value = true
  try {
    const res: any = await request.get('/articles', {
      params: {
        page: page - 1,
        size: pageSize
      }
    })
    articles.value = res.content
    total.value = res.totalElements
    currentPage.value = page
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  fetchArticles(page)
}

const editArticle = (row: any) => {
  // TODO: 实现编辑模式，需要传递 ID 到 Publish 页面
  ElMessage.info('编辑功能开发中，请直接发布新文章')
}

const deleteArticle = async (id: number) => {
  try {
    await request.delete(`/articles/${id}`)
    ElMessage.success('删除成功')
    fetchArticles(currentPage.value)
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  fetchArticles()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.article-list {
  animation: fade-in 260ms ease-out;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-item {
  border: 1px solid var(--gg-border);
  border-radius: 12px;
}

.mobile-title {
  font-weight: 700;
  color: var(--gg-text);
  margin-bottom: 8px;
}

.mobile-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--gg-text-soft);
  font-size: 12px;
  margin-bottom: 6px;
}

.mobile-time {
  font-size: 12px;
  color: var(--gg-text-soft);
}

.mobile-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 767px) {
  .header {
    gap: 10px;
    flex-wrap: wrap;
  }

  .pagination {
    justify-content: center;
  }
}
</style>
