<template>
  <div class="page-card">
    <el-tabs v-model="activeTab">
      <!-- VIP -->
      <el-tab-pane label="VIP" name="vip">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>我的 VIP 状态</template>
              <el-descriptions v-if="vipStatus" :column="1" border>
                <el-descriptions-item label="等级">{{ vipStatus.level || vipStatus.levelId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="vipStatus.status === 1 || vipStatus.valid ? 'success' : 'info'">
                    {{ vipStatus.status === 1 || vipStatus.valid ? '有效' : '无' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="到期时间">{{ vipStatus.expireTime || '-' }}</el-descriptions-item>
              </el-descriptions>
              <el-empty v-else description="暂未开通 VIP" />
              <el-button style="margin-top: 16px" type="primary" :loading="loading" @click="loadVipStatus">
                刷新状态
              </el-button>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>VIP 套餐</template>
              <div v-loading="loading">
                <el-table :data="vipPlans" border>
                  <el-table-column prop="name" label="套餐" />
                  <el-table-column label="价格" width="120">
                    <template #default="{ row }">¥{{ row.price }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="110">
                    <template #default="{ row }">
                      <el-button size="small" type="success" @click="handlePurchase(row)">开通</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="!loading && vipPlans.length === 0" description="暂无套餐" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 钱包 -->
      <el-tab-pane label="钱包" name="wallet">
        <el-descriptions :column="2" border v-if="wallet">
          <el-descriptions-item label="余额">¥{{ (wallet.balance / 100).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="累计充值">¥{{ (wallet.totalRecharge / 100).toFixed(2) }}</el-descriptions-item>
        </el-descriptions>
        <el-button style="margin-top: 16px" type="primary" :loading="loading" @click="loadWallet">刷新钱包</el-button>
      </el-tab-pane>

      <!-- 充值 -->
      <el-tab-pane label="充值" name="recharge">
        <el-form label-width="100px" style="max-width: 360px">
          <el-form-item label="充值金额(元)">
            <el-input-number v-model="rechargeAmount" :min="1" :step="10" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleRecharge">创建充值订单</el-button>
          </el-form-item>
        </el-form>
        <el-divider content-position="left">充值记录</el-divider>
        <el-table :data="rechargeOrders" border>
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '已支付' : '待支付' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 消费记录 -->
      <el-tab-pane label="消费记录" name="consume">
        <el-table :data="consumeRecords" border>
          <el-table-column prop="id" label="ID" width="120" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getVipStatus,
  getVipPlans,
  purchaseVip,
  getWallet,
  createRechargeOrder,
  getRechargeOrders,
  getConsumeRecords
} from '@/api/member'

const activeTab = ref('vip')
const loading = ref(false)
const vipStatus = ref(null)
const vipPlans = ref([])
const wallet = ref(null)
const rechargeAmount = ref(100)
const rechargeOrders = ref([])
const consumeRecords = ref([])

const loadVipStatus = async () => {
  loading.value = true
  try {
    const res = await getVipStatus()
    vipStatus.value = res.data
  } finally {
    loading.value = false
  }
}

const loadVipPlans = async () => {
  const res = await getVipPlans()
  vipPlans.value = res.data?.records || res.data || []
}

const handlePurchase = async (plan) => {
  try {
    const res = await purchaseVip(plan.id)
    ElMessage.success('购买成功')
    if (res.data?.orderNo) {
      await payCallbackSafe(res.data.orderNo, true)
    }
    loadVipStatus()
  } catch (e) {}
}

const payCallbackSafe = async (orderNo, success) => {
  // 演示用：直接调用支付回调标记成功
  const { payCallback } = await import('@/api/member')
  await payCallback(orderNo, success)
}

const loadWallet = async () => {
  loading.value = true
  try {
    const res = await getWallet()
    wallet.value = res.data
  } finally {
    loading.value = false
  }
}

const handleRecharge = async () => {
  try {
    const res = await createRechargeOrder(rechargeAmount.value)
    ElMessage.success('充值订单已创建')
    const order = res.data
    if (order?.orderNo) {
      await payCallbackSafe(order.orderNo, true)
      ElMessage.success('演示：已模拟支付成功')
    }
    loadRechargeOrders()
    loadWallet()
  } catch (e) {}
}

const loadRechargeOrders = async () => {
  const res = await getRechargeOrders(1, 20)
  rechargeOrders.value = res.data?.records || res.data || []
}

const loadConsumeRecords = async () => {
  const res = await getConsumeRecords(1, 20, null)
  consumeRecords.value = res.data?.records || res.data || []
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '-')

onMounted(() => {
  loadVipStatus()
  loadVipPlans()
  loadWallet()
  loadRechargeOrders()
  loadConsumeRecords()
})
</script>
