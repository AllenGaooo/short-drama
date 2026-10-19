<template>
  <div class="play-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>短剧播放</span>
          <el-tag v-if="detail" size="small" type="info">剧集 {{ episodeId }}</el-tag>
          <el-tag v-if="detail" size="small" type="warning">凭证 {{ detail.expireTime }}s</el-tag>
        </div>
      </template>

      <!-- 未登录提示 -->
      <el-alert
        v-if="!hasToken"
        type="warning"
        show-icon
        :closable="false"
        title="未检测到登录 token，播放接口会被网关拦截（401）"
        description="调试方式：在地址栏追加 ?token=你的JWT，或在 admin 端登录后复制 token。"
        class="tip"
      />

      <!-- 播放器 -->
      <div class="player-wrap">
        <video
          ref="videoRef"
          class="player"
          controls
          playsinline
          @loadedmetadata="onLoaded"
          @timeupdate="onTimeUpdate"
          @pause="onPause"
          @ended="onEnded"
        ></video>
        <div v-if="loading" class="loading">加载播放地址中…</div>
      </div>

      <!-- 播放信息 -->
      <div v-if="detail" class="info">
        <div class="row">
          <span class="label">播放地址：</span>
          <span class="value url">{{ detail.playUrl }}</span>
        </div>
        <div class="row">
          <span class="label">续播位置：</span>
          <span class="value">{{ detail.lastPosition }}s / {{ detail.duration }}s</span>
        </div>
        <div class="row">
          <span class="label">心跳状态：</span>
          <el-tag size="small" :type="heartbeatOk ? 'success' : 'danger'">
            {{ heartbeatOk ? '上报正常（每5秒）' : '上报失败' }}
          </el-tag>
        </div>
      </div>

      <div class="ops">
        <el-button type="primary" :loading="loading" @click="loadPlayInfo">重新获取播放地址</el-button>
        <el-button @click="reportComplete" :disabled="!detail">模拟播放完成（领金币）</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request, { getToken, setToken } from '@/utils/request.js'

const route = useRoute()
const episodeId = ref(Number(route.params.episodeId || route.query.episodeId || 10001))

const videoRef = ref(null)
const detail = ref(null)
const loading = ref(false)
const heartbeatOk = ref(true)

// 支持通过 URL ?token=xxx 注入 token，方便调试
const tokenParam = route.query.token
if (tokenParam) setToken(tokenParam)
const hasToken = computed(() => !!getToken())

let heartbeatTimer = null
let lastPosition = 0
let hlsInstance = null

onMounted(() => {
  loadPlayInfo()
  startHeartbeat()
})

onBeforeUnmount(() => {
  stopHeartbeat()
  destroyHls()
})

/** 获取播放地址（含防盗链 token 与续播位置） */
const loadPlayInfo = async () => {
  loading.value = true
  try {
    const res = await request.get(`/play/${episodeId.value}`)
    detail.value = res.data
    lastPosition = res.data?.lastPosition || 0
    await setupPlayer(res.data.playUrl, lastPosition)
  } catch (e) {
    ElMessage.error('获取播放地址失败：' + (e.message || e))
  } finally {
    loading.value = false
  }
}

/**
 * 初始化播放器
 * HLS(m3u8) 在 Safari/移动端原生支持；Chrome 等浏览器动态加载 hls.js 解码，
 * 避免为单一功能引入额外 npm 依赖。
 */
const setupPlayer = async (url, startAt) => {
  if (!url || !videoRef.value) return
  const video = videoRef.value
  destroyHls()

  const isHls = url.includes('.m3u8')
  if (isHls && !video.canPlayType('application/vnd.apple.mpegurl')) {
    try {
      const { default: Hls } = await import(/* @vite-ignore */ 'https://cdn.jsdelivr.net/npm/hls.js@1/dist/hls.min.js')
        .catch(() => import('hls.js').catch(() => ({ default: null })))
      if (Hls && Hls.isSupported()) {
        hlsInstance = new Hls()
        hlsInstance.loadSource(url)
        hlsInstance.attachMedia(video)
      } else {
        video.src = url
      }
    } catch (e) {
      video.src = url
    }
  } else {
    video.src = url
  }

  // 断点续播：从上次位置开始
  video.addEventListener('loadedmetadata', () => {
    if (startAt > 0) video.currentTime = startAt
  }, { once: true })
}

const destroyHls = () => {
  if (hlsInstance) {
    hlsInstance.destroy()
    hlsInstance = null
  }
}

const onLoaded = () => {
  if (lastPosition > 0 && videoRef.value) {
    videoRef.value.currentTime = lastPosition
  }
}

const onTimeUpdate = () => {
  if (videoRef.value) lastPosition = Math.floor(videoRef.value.currentTime)
}

const onPause = () => {
  reportEvent('PLAY_PAUSE')
}

const onEnded = () => {
  stopHeartbeat()
  reportEvent('PLAY_COMPLETE')
  ElMessage.success('播放完成，已触发金币奖励事件')
}

/** 每 5 秒上报一次观看进度（只写 Redis，不直接落库） */
const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = setInterval(async () => {
    if (!videoRef.value || videoRef.value.paused) return
    const position = Math.floor(videoRef.value.currentTime)
    const duration = Math.floor(videoRef.value.duration || 0)
    try {
      await request.post('/play/progress', null, {
        params: {
          episodeId: episodeId.value,
          position,
          duration,
          dramaId: detail.value?.dramaId
        }
      })
      heartbeatOk.value = true
    } catch (e) {
      heartbeatOk.value = false
    }
  }, 5000)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

/** 播放行为采集：START / PAUSE / PROGRESS / COMPLETE */
const reportEvent = async (event) => {
  try {
    await request.post('/play/event', null, {
      params: {
        episodeId: episodeId.value,
        event,
        time: lastPosition,
        duration: detail.value?.duration,
        dramaId: detail.value?.dramaId
      }
    })
  } catch (e) {
    // 埋点失败不影响观看
  }
}

/** 手动触发完成事件（验证金币幂等：重复点击只发一次） */
const reportComplete = async () => {
  await reportEvent('PLAY_COMPLETE')
  ElMessage.info('已上报完成事件（重复上报会被幂等拦截）')
}
</script>

<style scoped>
.play-page { padding: 16px; max-width: 720px; margin: 0 auto; }
.header { display: flex; align-items: center; gap: 10px; font-weight: 600; }
.tip { margin-bottom: 12px; }
.player-wrap { position: relative; background: #000; border-radius: 8px; overflow: hidden; }
.player { width: 100%; height: 320px; display: block; }
.loading {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  color: #fff; background: rgba(0,0,0,.45); font-size: 14px;
}
.info { margin-top: 14px; font-size: 13px; color: #555; }
.row { display: flex; margin-bottom: 6px; }
.label { width: 88px; color: #999; flex-shrink: 0; }
.value { flex: 1; word-break: break-all; }
.url { font-family: monospace; font-size: 12px; color: #888; }
.ops { margin-top: 14px; display: flex; gap: 10px; }
</style>
