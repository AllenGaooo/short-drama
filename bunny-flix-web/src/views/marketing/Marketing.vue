<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- 优惠券 -->
      <el-tab-pane label="优惠券" name="coupon">
        <div class="toolbar">
          <el-button type="success" :icon="Plus" @click="templateVisible = true">新建券模板</el-button>
          <span class="result-info">userId: {{ userStore.userId || '-' }}</span>
        </div>
        <el-table :data="coupons" v-loading="loading" border stripe>
          <el-table-column prop="id" label="券ID" width="120" />
          <el-table-column prop="templateId" label="模板ID" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="receiveTime" label="领取时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.receiveTime) }}</template>
          </el-table-column>
          <el-table-column prop="expireTime" label="过期时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleUse(row)">模拟使用</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券" />
      </el-tab-pane>

      <!-- 签到 -->
      <el-tab-pane label="每日签到" name="checkin">
        <el-button type="primary" :loading="checkinLoading" @click="handleCheckin">今日签到</el-button>
        <el-divider />
        <el-result
          v-if="checkinResult"
          icon="success"
          title="签到成功"
          :sub-title="`获得积分：${checkinResult.points ?? checkinResult}`"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 新建券模板弹窗 -->
    <el-dialog v-model="templateVisible" title="新建券模板" width="520px">
      <el-form label-width="120px">
        <el-form-item label="模板名称">
          <el-input v-model="templateForm.name" />
        </el-form-item>
        <el-form-item label="面值">
          <el-input-number v-model="templateForm.faceValue" :min="0" />
        </el-form-item>
        <el-form-item label="门槛金额">
          <el-input-number v-model="templateForm.threshold" :min="0" />
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="templateForm.totalStock" :min="1" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="templateForm.validDays" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTemplate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUserCoupons, useCoupon, createCouponTemplate, checkin } from '@/api/marketing'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const activeTab = ref('coupon')
const coupons = ref([])
const loading = ref(false)
const checkinLoading = ref(false)
const checkinResult = ref(null)

const templateVisible = ref(false)
const templateForm = reactive({ name: '', faceValue: 0, threshold: 0, totalStock: 100, validDays: 7 })

const statusMap = {
  0: { label: '未使用', type: 'primary' },
  1: { label: '已使用', type: 'info' },
  2: { label: '已过期', type: 'danger' }
}

const fetchCoupons = async () => {
  loading.value = true
  try {
    const res = await getUserCoupons(userStore.userId, null, 1, 100)
    coupons.value = res.data?.records || res.data || []
  } finally {
    loading.value = false
  }
}

const handleUse = async (row) => {
  try {
    const res = await useCoupon(row.id, userStore.userId, 5000)
    ElMessage.success(`优惠券使用成功，优惠金额：${res.data}`)
    fetchCoupons()
  } catch (e) {}
}

const handleCheckin = async () => {
  checkinLoading.value = true
  try {
    const res = await checkin(userStore.userId)
    checkinResult.value = res.data
    ElMessage.success('签到成功')
  } finally {
    checkinLoading.value = false
  }
}

const handleCreateTemplate = async () => {
  await createCouponTemplate(templateForm)
  ElMessage.success('创建成功')
  templateVisible.value = false
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

onMounted(fetchCoupons)
</script>

<style scoped>
.result-info {
  color: #6b7280;
  font-size: 13px;
}
</style>
