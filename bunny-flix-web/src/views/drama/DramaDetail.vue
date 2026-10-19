<template>
  <div class="page-card">
    <el-page-header @back="$router.back()" :content="`短剧详情 #${detail.id || ''}`" style="margin-bottom: 20px" />
    <div v-loading="loading">
      <el-descriptions :column="2" border v-if="detail.id">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">
            {{ detail.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="付费类型">
          {{ payTypeMap[detail.payType] || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="总集数">{{ detail.totalEpisodes }}</el-descriptions-item>
        <el-descriptions-item label="发行方ID">{{ detail.publisherId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="播放量">{{ detail.views }}</el-descriptions-item>
        <el-descriptions-item label="点赞数">{{ detail.likes }}</el-descriptions-item>
        <el-descriptions-item label="收藏数">{{ detail.collects }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(detail.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">
          {{ detail.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="封面" :span="2">
          <el-image
            v-if="detail.cover"
            :src="detail.cover"
            fit="cover"
            style="width: 120px; height: 160px; border-radius: 6px"
            :preview-src-list="[detail.cover]"
            preview-teleported
          />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="短剧不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDrama } from '@/api/drama'

const route = useRoute()
const detail = ref({})
const loading = ref(false)
const payTypeMap = { 0: '免费', 1: '付费', 2: '部分付费' }

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDrama(route.params.id)
    detail.value = res.data || {}
  } finally {
    loading.value = false
  }
})
</script>
