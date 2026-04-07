<template>
  <div class="ai-video">
    <el-card class="submit-card">
      <template #header>
        <div class="header">
          <span>即梦AI 视频生成</span>
        </div>
      </template>
      
      <el-form :model="form" label-width="100px" v-loading="submitting">
        <el-form-item label="提示词">
          <el-input 
            v-model="form.prompt" 
            type="textarea" 
            :rows="3" 
            placeholder="描述你想生成的视频内容，例如：一只在太空中飞翔的猫，赛博朋克风格" 
          />
        </el-form-item>
        
        <el-form-item label="参考图">
          <el-upload
            class="avatar-uploader"
            action="/api/v1/file/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
            name="file"
          >
            <img v-if="form.imageUrl" :src="form.imageUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="tip">（可选）上传一张图片作为首帧参考</div>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="submitTask" :disabled="!form.prompt">立即生成</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-divider content-position="left">生成记录 (保存在本地)</el-divider>

    <div class="task-list">
      <el-card v-for="(task, index) in tasks" :key="task.taskId" class="task-card">
        <div class="task-header">
          <el-tag :type="getStatusType(task.status)">{{ task.status }}</el-tag>
          <el-button type="danger" link size="small" @click="removeTask(index)">删除</el-button>
        </div>
        
        <div class="task-content">
          <p class="prompt">{{ task.prompt }}</p>
          
          <!-- Loading / Processing -->
          <div v-if="['pending', 'running', 'processing'].includes(task.status?.toLowerCase())" class="status-box">
             <el-progress type="circle" :percentage="50" status="warning" indeterminate />
             <p>生成中... ({{ task.taskId }})</p>
          </div>
          
          <!-- Failed -->
           <div v-else-if="task.status?.toLowerCase() === 'fail'" class="status-box error">
             <el-icon :size="40" color="var(--gg-danger)"><CircleCloseFilled /></el-icon>
             <p>生成失败: {{ task.failReason }}</p>
           </div>
          
          <!-- Success -->
          <div v-else-if="['success', 'succeeded', 'done'].includes(task.status?.toLowerCase())" class="video-box">
             <video controls :src="task.videoUrl" :poster="task.coverUrl" width="100%"></video>
             <div style="margin-top: 10px; text-align: right;">
                <a :href="task.videoUrl" target="_blank" download class="el-button el-button--primary el-button--small">
                  下载视频
                </a>
             </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Plus, CircleCloseFilled } from '@element-plus/icons-vue'

interface VideoTask {
  taskId: string
  prompt: string
  status: string
  videoUrl?: string
  coverUrl?: string
  failReason?: string
  createTime: number
}

const submitting = ref(false)
const token = localStorage.getItem('token')
const uploadHeaders = {
  Authorization: `Bearer ${token}`
}
const form = ref({
  prompt: '',
  imageUrl: ''
})

const tasks = ref<VideoTask[]>([])
let pollTimer: any = null

// 从 LocalStorage 加载历史任务
const loadTasks = () => {
  const saved = localStorage.getItem('jimeng_tasks')
  if (saved) {
    tasks.value = JSON.parse(saved)
  }
}

const saveTasks = () => {
  localStorage.setItem('jimeng_tasks', JSON.stringify(tasks.value))
}

const handleUploadSuccess = (res: any) => {
  // 假设后端返回结构是 ApiResponse<Map<String, String>>
  // 根据 FileController: return ApiResponse.success(result) -> result包含 url
  if (res.data && res.data.url) {
    form.value.imageUrl = res.data.url
  } else if (res.url) {
    // 兼容可能被拦截器处理过直接返回 data 的情况
    form.value.imageUrl = res.url
  }
}

const beforeUpload = (rawFile: any) => {
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const submitTask = async () => {
  if (!form.value.prompt) return
  
  submitting.value = true
  try {
    // 调用后端接口
    const res: any = await request.post('/video/submit', {
      prompt: form.value.prompt,
      imageUrl: form.value.imageUrl
    })
    
    // res 应该是 TaskResponse
    const newTask: VideoTask = {
      taskId: res.taskId,
      prompt: form.value.prompt,
      status: 'pending', // 初始状态
      createTime: Date.now()
    }
    
    tasks.value.unshift(newTask)
    saveTasks()
    ElMessage.success('任务提交成功，请耐心等待生成')
    form.value.prompt = ''
    form.value.imageUrl = ''
    
    // 立即触发一次轮询
    checkStatus()
    
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const removeTask = (index: number) => {
  tasks.value.splice(index, 1)
  saveTasks()
}

const getStatusType = (status: string) => {
  if (!status) return 'info'
  const s = status.toLowerCase()
  if (s === 'success' || s === 'succeeded' || s === 'done') return 'success'
  if (s === 'fail' || s === 'failed') return 'danger'
  return 'warning'
}

// 轮询逻辑
const checkStatus = async () => {
  let hasPending = false
  
  for (const task of tasks.value) {
    const s = task.status?.toLowerCase()
    // 只查询未完成的任务
    if (['pending', 'running', 'processing', 'in_queue'].includes(s)) {
      hasPending = true
      try {
        const res: any = await request.get(`/video/status/${task.taskId}`)
        // res 是 TaskResponse
        task.status = res.status
        task.videoUrl = res.videoUrl
        task.coverUrl = res.coverUrl
        task.failReason = res.failReason
      } catch (e) {
        console.error('Check status failed', e)
      }
    }
  }
  
  saveTasks()
  
  // 如果没有进行中的任务，可以考虑降低轮询频率或暂停，这里简单处理保持轮询但频率低一点
}

onMounted(() => {
  loadTasks()
  checkStatus() // Check immediately on load
  // 每 3 秒轮询一次
  pollTimer = setInterval(checkStatus, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.ai-video {
  max-width: 800px;
  margin: 0 auto;
  animation: fade-in 240ms ease-out;
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--gg-border-strong);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--gg-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: var(--gg-text-soft);
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
  border: 1px dashed var(--gg-border);
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.tip {
  font-size: 12px;
  color: var(--gg-text-soft);
  margin-top: 5px;
}

.task-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.task-card {
  display: flex;
  flex-direction: column;
}

.task-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.task-content {
  text-align: center;
}

.prompt {
  text-align: left;
  font-size: 13px;
  color: var(--gg-text-soft);
  margin-bottom: 10px;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.status-box {
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: var(--gg-bg-soft);
  border-radius: 8px;
}

.video-box video {
  border-radius: 8px;
  background: #000;
  max-height: 200px;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 767px) {
  .task-list {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .avatar,
  .avatar-uploader-icon {
    width: 132px;
    height: 132px;
    line-height: 132px;
  }
}
</style>
