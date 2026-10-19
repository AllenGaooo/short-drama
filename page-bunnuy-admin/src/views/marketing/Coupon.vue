<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>优惠券管理</span>
          <el-button type="primary" :icon="Plus" @click="onAdd">发放优惠券</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe border height="460">
        <el-table-column prop="id" label="ID" width="120" />
        <el-table-column prop="name" label="券名称" min-width="160" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.type === 'CASH' ? '满减券' : '折扣券' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="面额/折扣" width="120" />
        <el-table-column prop="total" label="发放量" width="100" />
        <el-table-column prop="received" label="已领取" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '进行中' : '已结束' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button text :type="row.enabled ? 'warning' : 'success'" @click="onToggle(row)">
              {{ row.enabled ? '结束' : '开启' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 示例数据：真实接入时替换为 GET /api/marketing/coupon/list 的返回
const list = ref([
  { id: 3001, name: '新人专享满20减10', type: 'CASH', value: '¥10', total: 1000, received: 642, enabled: true },
  { id: 3002, name: '会员日8折券', type: 'DISCOUNT', value: '8折', total: 500, received: 233, enabled: true },
  { id: 3003, name: '周末观影满50减20', type: 'CASH', value: '¥20', total: 800, received: 800, enabled: false },
  { id: 3004, name: '老用户回馈7折', type: 'DISCOUNT', value: '7折', total: 300, received: 88, enabled: true },
  { id: 3005, name: '首充赠送满30减15', type: 'CASH', value: '¥15', total: 1200, received: 405, enabled: true },
])

const loading = ref(false)

const onAdd = () => ElMessage.info('打开发放优惠券表单（示例）')
const onEdit = (row) => ElMessage.info(`编辑优惠券「${row.name}」（示例）`)
const onToggle = (row) => {
  row.enabled = !row.enabled
  ElMessage.success('操作成功（示例）')
}
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
