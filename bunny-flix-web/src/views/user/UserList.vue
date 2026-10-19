<template>
  <div class="page-card">
    <el-descriptions title="当前登录用户" :column="2" border>
      <el-descriptions-item label="用户ID">{{ userStore.userId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ userStore.phone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户类型">{{ userStore.userType || '-' }}</el-descriptions-item>
      <el-descriptions-item label="当前角色">
        <el-tag type="success">管理端</el-tag>
      </el-descriptions-item>
    </el-descriptions>

    <el-divider />

    <div class="toolbar">
      <h3>我的短剧发布</h3>
    </div>
    <el-table :data="myDramas" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="views" label="播放量" width="90" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listDramas } from '@/api/drama'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const myDramas = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await listDramas({ page: 1, pageSize: 50 })
    myDramas.value = res.data?.records || []
  } finally {
    loading.value = false
  }
})
</script>
