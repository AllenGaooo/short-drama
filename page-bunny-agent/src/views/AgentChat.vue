<template>
  <div class="page">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="card-header">
          <el-icon size="20" color="#f0883e"><ChatDotRound /></el-icon>
          <span>兔兔智能助手（Agent）</span>
          <el-tag size="small" type="success" effect="light">无需登录</el-tag>
          <el-tag size="small" type="info" effect="plain">RAG + MCP</el-tag>
        </div>
      </template>

      <!-- 消息区 -->
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
            <div v-if="m.tools && m.tools.length" class="tools">
              <el-tag
                v-for="(t, idx) in m.tools"
                :key="idx"
                size="small"
                type="warning"
                effect="plain"
              >🛠 {{ t.tool }}</el-tag>
            </div>
          </div>
        </div>
        <div v-if="loading" class="msg-row left">
          <div class="avatar ai">AI</div>
          <div class="bubble ai"><span class="dot-typing">思考中</span></div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-bar">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="例如：有哪些热门短剧？ / 查询用户 13800001111 / 平台会员体系是怎样的？"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        <el-button @click="clear">清空</el-button>
      </div>

      <div class="tips">
        提示：本对话 Agent 由 bunny-flix-agent 编排，自动检索知识库（RAG）并按需调用业务工具（MCP），无需登录即可使用。
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'

const messages = ref([
  {
    role: 'ai',
    content:
      '你好，我是兔兔智能助手 🐰（无需登录）\n可以帮你：\n• 查询短剧（试试“有哪些热门短剧”）\n• 查询用户（试试“查询用户 13800001111”）\n• 解答平台运营问题（会员、优惠券、推荐等）'
  }
])
const input = ref('')
const loading = ref(false)
const msgBox = ref(null)

// 进入时拉一次欢迎语（对应 GET /api/agent/hello）
onMounted(async () => {
  try {
    const res = await request.get('/agent/hello', { params: { name: '朋友' } })
    if (res.data && res.data.reply) {
      messages.value[0].content = res.data.reply
    }
  } catch (e) {
    // 忽略，保留默认欢迎语
  }
})

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
    const res = await request.post('/agent/chat', { message: text, history })
    const data = res.data || {}
    messages.value.push({
      role: 'ai',
      content: data.reply || '（无回复）',
      tools: data.usedTools || []
    })
  } catch (e) {
    ElMessage.error('对话请求失败：' + (e.message || e))
    messages.value.push({
      role: 'ai',
      content: '⚠️ 服务暂不可用，请确认 bunny-flix-agent 已启动且网关已放开 /api/agent 白名单。'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const clear = () => {
  messages.value = [{ role: 'ai', content: '对话已清空，有什么可以帮你的？' }]
}

const scrollToBottom = async () => {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}
</script>

<style scoped>
.page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
}
.chat-card {
  width: 100%;
  max-width: 880px;
  height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 4px;
  background: #f7f8fa;
  border-radius: 8px;
}
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}
.msg-row.right {
  flex-direction: row-reverse;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  flex-shrink: 0;
}
.avatar.user {
  background: #409eff;
}
.avatar.ai {
  background: #f0883e;
}
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.bubble.user {
  background: #409eff;
  color: #fff;
}
.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}
.tools {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.input-bar {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  align-items: flex-end;
}
.input-bar .el-button {
  height: 54px;
}
.tips {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}
.dot-typing::after {
  content: '···';
  animation: blink 1.2s infinite steps(4);
}
@keyframes blink {
  0% {
    opacity: 0.2;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.2;
  }
}
</style>
