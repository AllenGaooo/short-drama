<template>
  <div class="page">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="card-header">
          <span>🧠 AI 网关对话（意图识别 + Router）</span>
          <el-tag size="small" type="info">RAG + 图谱 + LLM</el-tag>
        </div>
      </template>

      <div class="messages" ref="msgBox">
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="msg-row"
          :class="m.role === 'user' ? 'right' : 'left'"
        >
          <div class="avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble" :class="m.role">
            <pre class="bubble-text">{{ m.content }}</pre>
            <div v-if="m.intent" class="meta">
              <el-tag size="small" effect="plain">意图：{{ m.intent.intent }}（{{ m.intent.confidence }}）</el-tag>
              <el-tag v-for="r in (m.route || [])" :key="r" size="small" type="warning" effect="plain">
                路由：{{ r }}
              </el-tag>
            </div>
          </div>
        </div>
        <div v-if="loading" class="msg-row left">
          <div class="avatar ai">AI</div>
          <div class="bubble ai"><span class="dot-typing">思考中</span></div>
        </div>
      </div>

      <div class="input-bar">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="试试：会员体系是怎样的？ / 重生之逆袭人生 属于什么分类？ / 短剧和平台是什么关系？"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        <el-button @click="clear">清空</el-button>
      </div>

      <div class="tips">
        架构：用户 → API Gateway → AI Gateway（意图识别 → Query Router → RAG检索 / 图谱查询 / LLM）。
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'

const messages = ref([
  { role: 'ai', content: '你好，我是 AI 网关助手 🧠\n我会先做「意图识别」，再用「Query Router」把问题分发到 RAG 知识库或知识图谱，最后汇总作答。\n\n示例：\n• “会员体系是怎样的？” → RAG\n• “重生之逆袭人生 属于什么分类？” → 图谱\n• “短剧和平台有什么关系？” → 图谱' }
])
const input = ref('')
const loading = ref(false)
const msgBox = ref(null)

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  const history = messages.value
    .filter((m) => m.role === 'user')
    .slice(-10)
    .map((m) => ({ role: m.content }))

  try {
    const res = await request.post('/aigateway/chat', { message: text, history })
    const data = res.data || {}
    messages.value.push({
      role: 'ai',
      content: data.reply || '（无回复）',
      intent: data.intent,
      route: data.route || []
    })
  } catch (e) {
    ElMessage.error('请求失败：' + (e.message || e))
    messages.value.push({ role: 'ai', content: '⚠️ 服务暂不可用，请确认 bunny-flix-aigateway 已启动。' })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const clear = () => {
  messages.value = [{ role: 'ai', content: '对话已清空。' }]
}

const scrollToBottom = async () => {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}
</script>

<style scoped>
.page { padding: 4px; }
.chat-card { height: calc(100vh - 140px); display: flex; flex-direction: column; }
.card-header { display: flex; align-items: center; gap: 10px; font-weight: 600; }
.messages { flex: 1; overflow-y: auto; padding: 12px 4px; background: #f7f8fa; border-radius: 8px; }
.msg-row { display: flex; gap: 10px; margin-bottom: 16px; align-items: flex-start; }
.msg-row.right { flex-direction: row-reverse; }
.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #fff; flex-shrink: 0; }
.avatar.user { background: #409eff; }
.avatar.ai { background: #7c4dff; }
.bubble { max-width: 72%; padding: 10px 14px; border-radius: 10px; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.bubble.user { background: #409eff; color: #fff; }
.bubble-text { margin: 0; white-space: pre-wrap; word-break: break-word; font-family: inherit; font-size: 14px; line-height: 1.6; }
.meta { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.input-bar { display: flex; gap: 10px; margin-top: 12px; align-items: flex-end; }
.input-bar .el-button { height: 54px; }
.tips { margin-top: 8px; color: #999; font-size: 12px; }
.dot-typing::after { content: '···'; animation: blink 1.2s infinite steps(4); }
@keyframes blink { 0% { opacity: .2 } 50% { opacity: 1 } 100% { opacity: .2 } }
</style>
