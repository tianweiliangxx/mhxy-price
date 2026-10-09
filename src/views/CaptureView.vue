<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { addRecognizedItems } from '../lib/prices.js'
import { loadRecognizer, recognizeImage } from '../lib/recognize.js'

const route = useRoute()
const modelText = ref('正在加载本地识别模型...')
const modelReady = ref(false)
const liveText = ref('等待录屏/OBS识别')
const running = ref(false)
const recognizing = ref(false)
const rows = ref([])
const videoRef = ref(null)
let stream = null

const itemCount = computed(() => new Set(rows.value.map((row) => row.name)).size)
const topPrice = computed(() => {
  const prices = rows.value.map((row) => row.price).filter((price) => price)
  return prices.length ? Math.max(...prices) : null
})

onMounted(() => {
  loadRecognizer((message) => {
    if (message.status) modelText.value = `正在加载本地识别模型...${message.status}`
  }).then(() => {
    modelReady.value = true
    modelText.value = '物品文字检测和价格识别模型已加载。可以对录屏画面或截图识别。'
  }).catch(() => {
    modelText.value = '识别模型加载失败。请检查网络后点刷新。'
  })
})

function stop() {
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
  running.value = false
  if (videoRef.value) videoRef.value.srcObject = null
  liveText.value = '等待录屏/OBS识别'
}

async function startScreen() {
  if (!modelReady.value) {
    liveText.value = '模型还在加载'
    return
  }
  try {
    stream = await navigator.mediaDevices.getDisplayMedia({ video: true })
    running.value = true
    liveText.value = '录屏已连接，可以识别当前画面'
    if (videoRef.value) videoRef.value.srcObject = stream
    stream.getVideoTracks()[0].addEventListener('ended', stop)
  } catch {
    liveText.value = '没有拿到画面'
  }
}

function startObs() {
  liveText.value = 'OBS 地址是 127.0.0.1:4455。当前页面还不能直接接收 OBS 画面，可以先用截图识别。'
}

async function recognizeSource(source) {
  recognizing.value = true
  liveText.value = '正在识别物品和价格'
  try {
    const found = await recognizeImage(source)
    rows.value = found.items.map((item) => ({
      ...item,
      category: route.meta.side === '摆摊' ? '摆摊' : '收购',
    }))
    const size = found.width ? `画面 ${found.width}×${found.height}，` : ''
    liveText.value = rows.value.length
      ? `${size}认出 ${rows.value.length} 条`
      : `${size}这一帧没有认出物品。模型原文：${found.text.slice(0, 80) || '空'}`
  } catch {
    liveText.value = '识别失败'
  } finally {
    recognizing.value = false
  }
}

function waitForFrame(video) {
  return new Promise((resolve) => {
    if (video.requestVideoFrameCallback) {
      video.requestVideoFrameCallback(() => resolve())
      return
    }
    requestAnimationFrame(() => resolve())
  })
}

async function recognizeFrame() {
  const video = videoRef.value
  if (!video?.videoWidth) {
    liveText.value = '还没有画面。请先点录屏识别，并在预览里看到游戏窗口。'
    return
  }
  try {
    await video.play()
  } catch {
    // 自动播放被拦住时，仍然用当前这一帧。
  }
  await waitForFrame(video)
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  canvas.getContext('2d').drawImage(video, 0, 0)
  recognizeSource(canvas)
}

function onFile(upload) {
  recognizeSource(upload.raw)
}

function saveRows() {
  const priced = rows.value.filter((row) => row.price)
  addRecognizedItems(priced, route.meta.side)
  liveText.value = priced.length ? '已写入本地物价，可在行情列表查看' : '没有可写入的价格'
}

function reloadModel() {
  modelReady.value = false
  modelText.value = '正在加载本地识别模型...'
  loadRecognizer((message) => {
    if (message.status) modelText.value = `正在加载本地识别模型...${message.status}`
  }).then(() => {
    modelReady.value = true
    modelText.value = '物品文字检测和价格识别模型已加载。可以对录屏画面或截图识别。'
  })
}

onBeforeUnmount(stop)
</script>

<template>
  <section>
    <el-card shadow="never">
      <template #header>{{ route.meta.side === '摆摊' ? '摆摊实时识别' : '识别控制' }}</template>
      <el-alert :title="modelText" :type="modelReady ? 'success' : 'info'" show-icon :closable="false" />
      <el-alert class="warn" type="warning" show-icon :closable="false" title="采集画面仅稳定支持 1024×768 分辨率">
        <p>请将游戏窗口设置为 1024×768，并避免缩小共享窗口。</p>
      </el-alert>
      <el-space wrap>
        <el-button :disabled="!modelReady" @click="startScreen">录屏识别</el-button>
        <el-button @click="startObs">OBS识别</el-button>
        <el-button :disabled="!running" @click="stop">停止采集</el-button>
        <el-button :disabled="!modelReady || recognizing" @click="recognizeFrame">识别当前画面</el-button>
        <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onFile">
          <el-button :disabled="!modelReady || recognizing">识别截图</el-button>
        </el-upload>
        <el-button @click="reloadModel">刷新</el-button>
      </el-space>
      <video ref="videoRef" class="preview" autoplay muted playsinline />
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>实时识别状态</template>
      <el-text type="info">{{ liveText }}</el-text>
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>本次采集最高价</template>
      <el-row :gutter="12">
        <el-col :span="6"><el-statistic title="物品数" :value="itemCount" /></el-col>
        <el-col :span="6"><el-statistic title="记录数" :value="rows.length" /></el-col>
        <el-col :span="6">
          <el-statistic v-if="topPrice != null" title="最高价" :value="topPrice" />
          <div v-else class="empty-price"><span>最高价</span><strong>—</strong></div>
        </el-col>
        <el-col :span="6"><el-statistic title="摊位合计" :value="rows.length ? 1 : 0" /></el-col>
      </el-row>
      <el-empty v-if="rows.length === 0" description="还没有从画面里认出价格" />
      <template v-else>
        <el-table :data="rows" class="table">
          <el-table-column prop="name" label="物品" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column label="价格" width="140">
            <template #default="{ row }">{{ row.price ?? '未认出' }}</template>
          </el-table-column>
        </el-table>
        <el-button type="primary" class="save" @click="saveRows">写入物价</el-button>
      </template>
    </el-card>
  </section>
</template>

<style scoped>
.warn { margin: 12px 0; }
.block { margin-top: 12px; }
.preview { display: block; width: min(100%, 640px); margin-top: 12px; background: #0f172a; }
.preview:not([srcObject]) { min-height: 0; }
.table { margin-top: 12px; }
.save { margin-top: 12px; }
.empty-price { display: flex; flex-direction: column; gap: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.empty-price strong { color: var(--el-text-color-primary); font-size: 20px; }
</style>
