<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="query.keyword"
          placeholder="按标题搜索"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.status" placeholder="上架状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-select v-model="query.payType" placeholder="付费类型" clearable style="width: 130px" @change="handleSearch">
          <el-option label="免费" :value="0" />
          <el-option label="付费" :value="1" />
          <el-option label="部分付费" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
      <el-button type="success" :icon="Plus" @click="openDialog()">新建短剧</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-image
            v-if="row.cover"
            :src="row.cover"
            fit="cover"
            style="width: 48px; height: 64px; border-radius: 4px"
            :preview-src-list="[row.cover]"
            preview-teleported
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付费" width="90">
        <template #default="{ row }">
          <el-tag :type="row.payType === 0 ? 'success' : 'warning'">
            {{ payTypeMap[row.payType] || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalEpisodes" label="集数" width="70" />
      <el-table-column prop="views" label="播放量" width="90" />
      <el-table-column prop="likes" label="点赞" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="View" @click="$router.push(`/drama/detail/${row.id}`)">详情</el-button>
          <el-button size="small" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        @current-change="fetchList"
        @size-change="handleSearch"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑短剧' : '新建短剧'" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入短剧标题" />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="form.cover" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="付费类型">
          <el-select v-model="form.payType" style="width: 100%">
            <el-option label="免费" :value="0" />
            <el-option label="付费" :value="1" />
            <el-option label="部分付费" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="总集数">
          <el-input-number v-model="form.totalEpisodes" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete, View } from '@element-plus/icons-vue'
import { listDramas, createDrama, updateDrama, deleteDrama } from '@/api/drama'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: null,
  payType: null
})

const defaultForm = () => ({
  id: null,
  title: '',
  cover: '',
  description: '',
  status: 1,
  payType: 0,
  totalEpisodes: 1
})
const form = reactive(defaultForm())

const payTypeMap = { 0: '免费', 1: '付费', 2: '部分付费' }

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listDramas(query)
    const data = res.data || {}
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  fetchList()
}

const openDialog = (row) => {
  Object.assign(form, defaultForm())
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.title) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateDrama(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createDrama(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除短剧「${row.title}」？`, '提示', { type: 'warning' })
  await deleteDrama(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

onMounted(fetchList)
</script>
