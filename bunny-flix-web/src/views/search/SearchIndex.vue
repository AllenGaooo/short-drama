<template>
  <div class="page-card">
    <div class="search-box">
      <el-input
        v-model="keyword"
        placeholder="搜索短剧标题、简介、演员、标签"
        size="large"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" size="large" :icon="Search" @click="handleSearch">搜索</el-button>
    </div>

    <div class="toolbar" style="margin-top: 16px">
      <span v-if="total !== null" class="result-info">共找到 {{ total }} 条结果</span>
      <el-select v-model="sortBy" style="width: 140px" @change="handleSearch">
        <el-option label="综合排序" value="default" />
        <el-option label="热度最高" value="heat" />
        <el-option label="最新发布" value="newest" />
        <el-option label="播放最多" value="play" />
      </el-select>
    </div>

    <!-- 热门搜索词 -->
    <div v-if="hotWords.length" style="margin: 12px 0">
      <el-tag
        v-for="w in hotWords"
        :key="w"
        class="hot-word"
        :type="w === keyword ? 'primary' : 'info'"
        @click="useHotWord(w)"
      >{{ w }}</el-tag>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="封面" width="90">
        <template #default="{ row }">
          <el-image
            v-if="row.cover"
            :src="row.cover"
            fit="cover"
            style="width: 56px; height: 76px; border-radius: 4px"
            :preview-src-list="[row.cover]"
            preview-teleported
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="简介" min-width="240" show-overflow-tooltip />
      <el-table-column prop="actors" label="演员" min-width="120" show-overflow-tooltip />
      <el-table-column prop="tags" label="标签" min-width="120" show-overflow-tooltip />
      <el-table-column label="热度" width="90">
        <template #default="{ row }">{{ row.heatScore != null ? Number(row.heatScore).toFixed(1) : '-' }}</template>
      </el-table-column>
      <el-table-column prop="playCount" label="播放量" width="90" />
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button size="small" @click="goDetail(row.dramaId)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无搜索结果" />

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.page"
        @current-change="handleSearch"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { searchDramas, getHotSearch } from '@/api/search'

const router = useRouter()
const keyword = ref('')
const list = ref([])
const total = ref(null)
const loading = ref(false)
const sortBy = ref('default')
const hotWords = ref([])

const query = reactive({ page: 1, size: 10 })

const handleSearch = async () => {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value,
      page: query.page,
      size: query.size,
      sortBy: sortBy.value
    }
    const res = await searchDramas(params)
    const data = res.data || []
    list.value = data
    total.value = list.value.length
  } finally {
    loading.value = false
  }
}

const useHotWord = (w) => {
  keyword.value = w
  query.page = 1
  handleSearch()
}

const goDetail = (dramaId) => {
  if (dramaId) router.push(`/drama/detail/${dramaId}`)
}

onMounted(async () => {
  try {
    const res = await getHotSearch()
    hotWords.value = res.data || []
  } catch (e) {}
})
</script>

<style scoped>
.search-box {
  display: flex;
  gap: 12px;
}
.result-info {
  color: #6b7280;
  font-size: 13px;
}
.hot-word {
  margin-right: 8px;
  cursor: pointer;
}
</style>
