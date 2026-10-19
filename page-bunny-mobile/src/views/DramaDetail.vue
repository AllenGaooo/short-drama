<template>
  <div class="detail" v-loading="loading">
    <div v-if="drama" class="detail-inner">
      <img :src="cover" class="cover" />
      <div class="info">
        <h1>{{ drama.title }}</h1>
        <p class="desc">{{ drama.description || '暂无简介' }}</p>
        <div class="stats">
          <span>{{ drama.totalEpisodes || 0 }} 集</span>
          <span>{{ formatViews(drama.views || 0) }}次播放</span>
          <span>{{ drama.likes || 0 }} 点赞</span>
        </div>
        <el-button type="primary" size="large" @click="play">立即播放</el-button>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="短剧不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDrama } from '@/api/drama.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const drama = ref(null)
const loading = ref(false)
const cover = ref('/default-cover.svg')

const play = () => {
  // 暂用短剧ID作为剧集ID跳转播放页（真实应按剧集列表取第一集）
  router.push(`/play/${route.params.id}`)
}

const formatViews = (v) => {
  if (v >= 100000000) return (v / 100000000).toFixed(1) + '亿'
  if (v >= 10000) return (v / 10000).toFixed(1) + '万'
  return String(v)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDrama(route.params.id)
    drama.value = res.data
    if (drama.value?.cover) cover.value = drama.value.cover
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || e))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail { background: #fff; border-radius: 10px; padding: 24px; min-height: 360px; }
.detail-inner { display: flex; gap: 24px; }
.cover { width: 180px; height: 240px; object-fit: cover; border-radius: 10px; flex-shrink: 0; }
.info { flex: 1; }
.info h1 { margin: 0 0 12px; font-size: 24px; }
.desc { color: #666; line-height: 1.8; font-size: 14px; margin: 0 0 16px; }
.stats { display: flex; gap: 18px; color: #999; font-size: 13px; margin-bottom: 24px; }
@media (max-width: 768px) {
  .detail-inner { flex-direction: column; }
  .cover { width: 140px; height: 187px; }
}
</style>
