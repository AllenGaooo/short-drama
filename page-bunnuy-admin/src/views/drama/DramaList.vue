<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>短剧列表</span>
          <el-button type="primary" :icon="Plus" @click="onAdd">新增短剧</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe border height="460">
        <el-table-column prop="id" label="ID" width="120" />
        <el-table-column prop="title" label="剧名" min-width="160" />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.online ? 'success' : 'info'" effect="light">
              {{ row.online ? '已上线' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="episodes" label="集数" width="90" />
        <el-table-column prop="playCount" label="播放量" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button text :type="row.online ? 'warning' : 'success'" @click="onToggle(row)">
              {{ row.online ? '下架' : '上线' }}
            </el-button>
            <el-button text type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 示例数据：真实接入时替换为 GET /api/drama/list 的返回
const list = ref([
  { id: 2001, title: '千金归来之复仇新娘', category: '豪门', online: true, episodes: 80, playCount: '128.6万' },
  { id: 2002, title: '闪婚娇妻有点甜', category: '甜宠', online: true, episodes: 64, playCount: '96.3万' },
  { id: 2003, title: '战神归来当奶爸', category: '战神', online: false, episodes: 52, playCount: '41.2万' },
  { id: 2004, title: '重生之逆袭千金', category: '重生', online: true, episodes: 72, playCount: '210.4万' },
  { id: 2005, title: '总裁的隐藏小娇妻', category: '甜宠', online: true, episodes: 60, playCount: '77.8万' },
  { id: 2006, title: '天才医生在都市', category: '神医', online: false, episodes: 48, playCount: '33.1万' },
])

const loading = ref(false)

const onAdd = () => ElMessage.info('打开新增短剧表单（示例）')
const onEdit = (row) => ElMessage.info(`编辑《${row.title}》（示例）`)
const onToggle = (row) => {
  row.online = !row.online
  ElMessage.success('操作成功（示例）')
}
const onDelete = (row) => {
  ElMessageBox.confirm(`确认删除《${row.title}》？`, '提示', { type: 'warning' })
    .then(() => {
      list.value = list.value.filter((d) => d.id !== row.id)
      ElMessage.success('已删除（示例）')
    })
    .catch(() => {})
}
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
