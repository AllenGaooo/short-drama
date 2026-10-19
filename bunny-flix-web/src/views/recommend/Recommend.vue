<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" :loading="hotLoading" @click="loadHot">热门推荐</el-button>
      <el-button type="success" :loading="personalLoading" @click="loadPersonal">个性化推荐</el-button>
    </div>

    <el-divider content-position="left">热门推荐</el-divider>
    <el-empty v-if="!hotLoading && hotList.length === 0" description="暂无热门数据" />
    <el-table :data="hotList" v-loading="hotLoading" border stripe>
      <el-table-column prop="dramaId" label="短剧ID" width="180" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="heatScore" label="热度" width="100" />
      <el-table-column prop="playCount" label="播放量" width="100" />
      <el-table-column prop="reason" label="推荐理由" min-width="160" />
    </el-table>

    <el-divider content-position="left">个性化推荐</el-divider>
    <el-empty v-if="!personalLoading && personalList.length === 0" description="暂无推荐数据" />
    <el-table :data="personalList" v-loading="personalLoading" border stripe>
      <el-table-column prop="dramaId" label="短剧ID" width="180" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="heatScore" label="热度" width="100" />
      <el-table-column prop="reason" label="推荐理由" min-width="160" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getRecommendations, getHotRecommend } from '@/api/recommend'

const hotList = ref([])
const personalList = ref([])
const hotLoading = ref(false)
const personalLoading = ref(false)

const loadHot = async () => {
  hotLoading.value = true
  try {
    const res = await getHotRecommend(1, 10)
    hotList.value = res.data?.records || res.data || []
  } finally {
    hotLoading.value = false
  }
}

const loadPersonal = async () => {
  personalLoading.value = true
  try {
    const res = await getRecommendations(1, 10)
    personalList.value = res.data?.records || res.data || []
  } finally {
    personalLoading.value = false
  }
}
</script>
