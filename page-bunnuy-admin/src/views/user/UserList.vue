<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-input
            v-model="keyword"
            placeholder="搜索手机号 / 昵称"
            :prefix-icon="Search"
            clearable
            style="width: 240px"
            @input="onSearch"
          />
        </div>
      </template>

      <el-table :data="pagedList" v-loading="loading" stripe border height="460">
        <el-table-column prop="id" label="ID" width="120" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="onView(row)">查看</el-button>
            <el-button text :type="row.status === 1 ? 'success' : 'warning'" @click="onToggle(row)">
              {{ row.status === 1 ? '解冻' : '冻结' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="filteredList.length"
          :page-size="pageSize"
          v-model:current-page="currentPage"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 示例数据：真实接入时把这里替换为 GET /api/user/list 的返回
const allUsers = ref([
  { id: 10001, phone: '13800001111', nickname: '小兔', status: 0, createTime: '2026-08-01 10:22' },
  { id: 10002, phone: '13800002222', nickname: '豆豆', status: 0, createTime: '2026-08-03 14:05' },
  { id: 10003, phone: '13800003333', nickname: '短剧迷', status: 1, createTime: '2026-08-05 09:41' },
  { id: 10004, phone: '13800004444', nickname: '观众A', status: 0, createTime: '2026-08-08 20:13' },
  { id: 10005, phone: '13800005555', nickname: '会员君', status: 2, createTime: '2026-08-10 11:58' },
  { id: 10006, phone: '13800006666', nickname: '路人乙', status: 0, createTime: '2026-08-12 16:30' },
  { id: 10007, phone: '13800007777', nickname: '追剧人', status: 0, createTime: '2026-08-14 08:09' },
  { id: 10008, phone: '13800008888', nickname: '阿白', status: 1, createTime: '2026-08-15 22:47' },
])

const keyword = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 6

const filteredList = computed(() => {
  const k = keyword.value.trim()
  if (!k) return allUsers.value
  return allUsers.value.filter(
    (u) => u.phone.includes(k) || (u.nickname || '').includes(k)
  )
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

const statusText = (s) => ({ 0: '正常', 1: '冻结', 2: '注销' }[s] ?? '未知')
const statusType = (s) => ({ 0: 'success', 1: 'danger', 2: 'info' }[s] ?? 'info')

const onSearch = () => { currentPage.value = 1 }

const onView = (row) => {
  ElMessage.info(`查看用户 ${row.nickname}（${row.phone}）`)
}
const onToggle = (row) => {
  row.status = row.status === 1 ? 0 : 1
  ElMessage.success('操作成功（示例）')
}
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
