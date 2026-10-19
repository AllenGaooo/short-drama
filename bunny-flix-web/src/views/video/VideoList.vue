<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- 视频上传 -->
      <el-tab-pane label="视频上传" name="upload">
        <el-form label-width="100px" style="max-width: 520px">
          <el-form-item label="剧集ID" required>
            <el-input v-model="uploadForm.episodeId" placeholder="请输入集数ID" />
          </el-form-item>
          <el-form-item label="文件URL" required>
            <el-input v-model="uploadForm.fileUrl" placeholder="https://..." />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleUpload">上传</el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="uploadedVideo"
          type="success"
          :closable="false"
          title="上传成功"
          :description="`videoId=${uploadedVideo.id}, 短剧=${uploadedVideo.dramaId}, 集数=${uploadedVideo.episodeId}`"
        />
      </el-tab-pane>

      <!-- 观看历史 -->
      <el-tab-pane label="观看历史" name="history">
        <el-table :data="history" v-loading="loading">
          <el-table-column prop="id" label="ID" width="120" />
          <el-table-column prop="dramaId" label="短剧ID" width="120" />
          <el-table-column prop="episodeId" label="集数ID" width="120" />
          <el-table-column prop="position" label="播放进度" width="120" />
          <el-table-column prop="updateTime" label="更新时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDeleteHistory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && history.length === 0" description="暂无观看历史" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadVideo, listHistory, deleteHistory } from '@/api/video'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('upload')
const loading = ref(false)
const history = ref([])
const uploadedVideo = ref(null)

const uploadForm = reactive({ episodeId: null, fileUrl: '' })

const handleUpload = async () => {
  if (!uploadForm.episodeId || !uploadForm.fileUrl) {
    ElMessage.warning('请填写完整')
    return
  }
  loading.value = true
  try {
    const res = await uploadVideo(uploadForm.episodeId, uploadForm.fileUrl)
    uploadedVideo.value = res.data
    ElMessage.success('上传成功')
  } finally {
    loading.value = false
  }
}

const fetchHistory = async () => {
  loading.value = true
  try {
    const res = await listHistory(userStore.userId, 1, 20)
    history.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

const handleDeleteHistory = async (row) => {
  await deleteHistory(row.id)
  ElMessage.success('删除成功')
  fetchHistory()
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

onMounted(fetchHistory)
</script>
