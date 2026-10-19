<template>
  <div class="rank-page">
    <div class="rank-head">
      <h2>热门榜单</h2>
      <div class="tabs">
        <span :class="{ active: sortBy === 'hot' }" @click="switchSort('hot')">播放榜</span>
        <span :class="{ active: sortBy === 'newest' }" @click="switchSort('newest')">新剧榜</span>
      </div>
    </div>

    <!-- 前三名高亮 -->
    <div class="top3">
      <div v-for="(d, i) in top3" :key="d.id" class="top-item" @click="open(d)">
        <div class="rank-no" :class="'no' + (i + 1)">{{ i + 1 }}</div>
        <img :src="coverOf(d)" class="top-cover" />
        <div class="top-info">
          <div class="top-title">{{ d.title }}</div>
          <div class="top-meta">{{ formatViews(d.views || 0) }}次播放</div>
        </div>
      </div>
    </div>

    <!-- 4~N 名列表 -->
    <div class="rank-list">
      <div v-for="(d, i) in rest" :key="d.id" class="rank-row" @click="open(d)">
        <span class="no">{{ i + 4 }}</span>
        <img :src="coverOf(d)" class="row-cover" />
        <span class="row-title">{{ d.title }}</span>
        <span class="row-views">{{ formatViews(d.views || 0) }}</span>
      </div>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="暂无榜单数据" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRanking } from '@/api/drama.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const sortBy = ref('hot')

const top3 = computed(() => list.value.slice(0, 3))
const rest = computed(() => list.value.slice(3))

const load = async () => {
  loading.value = true
  try {
    const res = await getRanking({ size: 20, sortBy: sortBy.value })
    list.value = res.data || []
  } catch (e) {
    ElMessage.error('榜单加载失败：' + (e.message || e))
  } finally {
    loading.value = false
  }
}

const switchSort = (s) => {
  sortBy.value = s
  load()
}

const open = (d) => router.push(`/drama/${d.id}`)
const coverOf = (d) => (d.cover && d.cover.trim() ? d.cover : '/default-cover.svg')
const formatViews = (v) => {
  if (v >= 100000000) return (v / 100000000).toFixed(1) + '亿'
  if (v >= 10000) return (v / 10000).toFixed(1) + '万'
  return String(v)
}

onMounted(load)
</script>

<style scoped>
.rank-page { background: #fff; border-radius: 10px; padding: 20px; }
.rank-head { display: flex; align-items: center; gap: 18px; margin-bottom: 18px; }
.rank-head h2 { margin: 0; font-size: 20px; }
.tabs { display: flex; gap: 8px; }
.tabs span {
  padding: 4px 14px; font-size: 13px; cursor: pointer;
  background: #f2f3f5; border-radius: 14px; color: #555;
}
.tabs span.active { background: #ff7a45; color: #fff; }

.top3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 20px; }
.top-item {
  display: flex; align-items: center; gap: 12px; cursor: pointer;
  padding: 12px; background: #fafbfc; border-radius: 8px;
}
.top-item:hover { background: #f2f4f7; }
.rank-no {
  width: 26px; height: 26px; border-radius: 6px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 14px; background: #c0c4cc;
}
.rank-no.no1 { background: #f56c6c; }
.rank-no.no2 { background: #e6a23c; }
.rank-no.no3 { background: #409eff; }
.top-cover { width: 56px; height: 74px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.top-info { min-width: 0; }
.top-title { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.top-meta { font-size: 12px; color: #999; margin-top: 4px; }

.rank-list { display: flex; flex-direction: column; }
.rank-row {
  display: flex; align-items: center; gap: 14px;
  padding: 10px 8px; cursor: pointer; border-bottom: 1px solid #f2f3f5;
}
.rank-row:hover { background: #fafbfc; }
.no { width: 24px; text-align: center; color: #999; font-size: 14px; flex-shrink: 0; }
.row-cover { width: 40px; height: 54px; object-fit: cover; border-radius: 4px; flex-shrink: 0; }
.row-title { flex: 1; font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-views { color: #999; font-size: 13px; flex-shrink: 0; }
</style>
