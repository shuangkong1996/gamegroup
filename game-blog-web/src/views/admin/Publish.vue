<template>
  <div class="publish">
    <el-card class="publish-card" shadow="never">
      <template #header>
        <div class="header">
          <span>发布文章</span>
          <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="如: Java, Vue, Life" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="封面图">
              <el-input v-model="form.coverUrl" placeholder="图片 URL" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章简短描述" />
        </el-form-item>

        <el-form-item label="内容">
          <div class="editor-container">
            <component
              :is="markdownEditor"
              v-if="markdownReady"
              v-model="form.content"
              height="600px"
              :disabled-menus="[]"
              @upload-image="handleUploadImage"
            ></component>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { getMarkdownEditor } from '@/utils/markdown'

const router = useRouter()
const submitting = ref(false)
const markdownReady = ref(false)
const markdownEditor = ref<any>(null)

const form = ref({
  title: '',
  category: '',
  coverUrl: '',
  summary: '',
  content: '',
  status: 1 // Default published
})

const handleUploadImage = async (event: Event, insertImage: Function, files: File[]) => {
  // 这里复用 game-server 的 FileController 上传接口
  const file = files[0]
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res: any = await request.post('/v1/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    // res.data.url 是后端返回的图片地址
    insertImage({
      url: res.url,
      desc: file.name,
    })
  } catch (e) {
    ElMessage.error('图片上传失败')
    console.error(e)
  }
}

const submit = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  submitting.value = true
  try {
    await request.post('/articles', form.value)
    ElMessage.success('发布成功')
    router.push('/admin/articles')
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  getMarkdownEditor().then((editor) => {
    markdownEditor.value = editor
    markdownReady.value = true
  })
})
</script>

<style scoped>
.publish {
  animation: fade-in 260ms ease-out;
}

.publish-card {
  border-radius: 14px;
  border: 1px solid var(--gg-border);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-container {
  border: 1px solid var(--gg-border);
  border-radius: 8px;
  overflow: hidden;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 767px) {
  .header {
    flex-wrap: wrap;
    gap: 8px;
  }

  :deep(.el-form-item__label) {
    width: 70px !important;
  }

  :deep(.v-md-editor) {
    min-height: 420px;
  }
}
</style>
