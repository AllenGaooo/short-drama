<template>
  <div class="drama-list">
    <!-- 分类筛选（分类页显示） -->
    <div v-if="showCategory" class="category-bar">
      <span
        class="tag-chip"
        :class="{ active: activeTagId === null }"
        @click="pickTag(null)"
      >全部</span>
      <span
        v-for="t in tags"
        :key="t.id"
        class="tag-chip"
        :class="{ active: activeTagId === t.id }"
        @click="pickTag(t.id)"
      >{{ t.name }}</span>
    </div>

    <!-- 标题栏 -->
    <div class="section-head">
      <h2>{{ title }}</h2>
      <span class="total">共 {{ total }} 部</span>
    </div>

    <!-- 剧集网格：每行 6 个 -->
    <div class="grid">
      <div
        v-for="(d, i) in list"
        :key="d.id || i"
        class="card"
        @click="openDrama(d)"
      >
        <div class="cover-wrap">
          <img :src="coverOf(d)" :alt="d.title" class="cover" loading="lazy" />
          <span v-if="d.payType === 1" class="badge vip">付费</span>
          <span v-if="d.totalEpisodes" class="badge eps">{{ d.totalEpisodes }}集</span>
          <div class="play-mask"><span class="play-icon">▶</span></div>
        </div>
        <div class="title" :title="d.title">{{ d.title }}</div>
        <div class="meta">
          <span v-if="d.views">{{ formatViews(d.views) }}次播放</span>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div class="load-state">
      <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
      <span v-if="loading">加载中…</span>
      <span v-else-if="!hasMore" class="no-more">—— 已经到底啦 ——</span>
      <span v-else class="hint">下滑加载更多</span>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && list.length === 0" description="暂无短剧数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listDramas, listTags } from '@/api/drama.js'

const props = defineProps({
  mode: { type: String, default: 'home' },     // home | category
  title: { type: String, default: '热门推荐' }
})

const route = useRoute()
const router = useRouter()

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 18          // 每页 18 个（每行 6 个，正好 3 行）
const loading = ref(false)
const hasMore = ref(true)
const keyword = ref('')

const tags = ref([])
const activeTagId = ref(null)
const showCategory = props.mode === 'category'

// ---------- 数据加载 ----------

const load = async (reset = false) => {
  if (loading.value) return
  if (!reset && !hasMore.value) return

  if (reset) {
    page.value = 1
    hasMore.value = true
  }

  loading.value = true
  try {
    const res = await listDramas({
      page: page.value,
      pageSize,
      keyword: keyword.value || undefined,
      status: 1,
      tagId: activeTagId.value || undefined
    })
    const data = res.data || {}
    const records = data.records || []
    total.value = data.total || 0

    // 追加 or 覆盖
    list.value = reset ? records : list.value.concat(records)

    // 判断是否还有下一页
    const totalPages = data.pages || Math.ceil(total.value / pageSize)
    hasMore.value = page.value < totalPages
    if (hasMore.value) page.value++
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || e))
    // 失败时不推进页码，允许重试
  } finally {
    loading.value = false
  }
}

// ---------- 滚动加载 ----------

const onScroll = () => {
  // 距离底部 300px 时提前加载，提升体验
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const clientHeight = document.documentElement.clientHeight
  const scrollHeight = document.documentElement.scrollHeight
  if (scrollHeight - scrollTop - clientHeight < 300) {
    load(false)
  }
}

// ---------- 交互 ----------

const pickTag = (tagId) => {
  activeTagId.value = tagId
  load(true)
}

const openDrama = (d) => {
  // 跳到播放页（已有 /play/:episodeId 路由）
  if (d.id) router.push(`/drama/${d.id}`)
}

const coverOf = (d) => {
  return d.cover && d.cover.trim() ? d.cover : '/default-cover.svg'
}

const formatViews = (v) => {
  if (v >= 100000000) return (v / 100000000).toFixed(1) + '亿'
  if (v >= 10000) return (v / 10000).toFixed(1) + '万'
  return String(v)
}

// ---------- 生命周期 ----------

const onSearchEvent = (e) => {
  keyword.value = e.detail || ''
  load(true)
}

onMounted(async () => {
  // 支持 URL 传 keyword
  if (route.query.keyword) keyword.value = route.query.keyword
  if (showCategory) {
    try {
      const res = await listTags()
      tags.value = res.data || []
    } catch (e) {
      // 分类加载失败不影响列表
    }
  }
  await load(true)
  window.addEventListener('scroll', onScroll)
  window.addEventListener('drama-search', onSearchEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('drama-search', onSearchEvent)
})

watch(() => route.query.keyword, (v) => {
  keyword.value = v || ''
  load(true)
})
</script>

<style scoped>
.drama-list { min-height: 400px; }

.category-bar {
  display: flex; flex-wrap: wrap; gap: 10px;
  margin-bottom: 18px; padding: 14px;
  background: #fff; border-radius: 8px;
}
.tag-chip {
  padding: 5px 14px; font-size: 13px; cursor: pointer;
  background: #f2f3f5; color: #555; border-radius: 14px;
  transition: all .2s;
}
.tag-chip:hover { background: #e8eaf0; }
.tag-chip.active { background: #ff7a45; color: #fff; }

.section-head {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px;
}
.section-head h2 { margin: 0; font-size: 20px; color: #222; }
.total { color: #999; font-size: 13px; }

/* 每行 6 个 */
.grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}
@media (max-width: 1200px) { .grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 768px)  { .grid { grid-template-columns: repeat(2, 1fr); } }

.card { cursor: pointer; }
.cover-wrap {
  position: relative; width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 8px; overflow: hidden;
  background: #e9ebef;
}
.cover { width: 100%; height: 100%; object-fit: cover; display: block; }

.badge {
  position: absolute; top: 6px; font-size: 11px;
  padding: 2px 6px; border-radius: 4px; color: #fff;
}
.badge.vip { left: 6px; background: rgba(255,122,69,.92); }
.badge.eps { right: 6px; background: rgba(0,0,0,.55); }

.play-mask {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,.35); opacity: 0; transition: opacity .25s;
}
.card:hover .play-mask { opacity: 1; }
.play-icon {
  width: 42px; height: 42px; border-radius: 50%;
  background: rgba(255,255,255,.9); color: #ff7a45;
  display: flex; align-items: center; justify-content: center; font-size: 16px;
}

.title {
  margin-top: 8px; font-size: 14px; color: #333;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card:hover .title { color: #ff7a45; }
.meta { margin-top: 2px; font-size: 12px; color: #999; }

.load-state {
  text-align: center; padding: 28px 0; color: #888; font-size: 13px;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.no-more { color: #bbb; }
.hint { color: #bbb; }
.is-loading { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
