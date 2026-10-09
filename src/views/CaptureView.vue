<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ItemLabel from '../components/ItemLabel.vue'
import { recognizeWithMimo } from '../lib/mimo.js'
import { addRecognizedItems } from '../lib/prices.js'

const route = useRoute()
const liveText = ref('点一次按钮，只截一张画面并识别一次')
const reasoning = ref('')
const recognizing = ref(false)
const rows = ref([])
const videoRef = ref(null)

const categoryTree = ref([])
const treeProps = { value: 'id', label: 'name', children: 'children' }
const selling = computed(() => route.meta.side === '摆摊')
const needsCategory = computed(() => rows.value.some((row) => !row.matched && !row.categoryId))
const itemCount = computed(() => new Set(rows.value.map((row) => row.name)).size)
const topPrice = computed(() => {
  const prices = rows.value.map((row) => row.price).filter((price) => price)
  return prices.length ? Math.max(...prices) : null
})

onMounted(() => {
  loadCategories()
})

async function loadCategories() {
  if (categoryTree.value.length) return
  const response = await fetch('/api/categories')
  if (!response.ok) return
  categoryTree.value = await response.json()
}

function findCategory(nodes, id) {
  for (const node of nodes || []) {
    if (node.id === id) return node
    const child = findCategory(node.children, id)
    if (child) return child
  }
  return null
}

function pickCategory(row, id) {
  row.categoryId = id
  row.category = findCategory(categoryTree.value, id)?.path || ''
}

async function applyRecognized(items) {
  let resolved = []
  try {
    const response = await fetch('/api/catalog/resolve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ names: items.map((item) => item.name) }),
    })
    if (response.ok) {
      const data = await response.json()
      resolved = data.items || []
    }
  } catch {
    resolved = []
  }
  await loadCategories()
  rows.value = items.map((item, index) => {
    const match = resolved[index] || {}
    return {
      ...item,
      matched: Boolean(match.matched),
      category: match.matched ? (match.categoryPath || '') : '',
      categoryId: match.matched ? match.categoryId : null,
    }
  })
  const matched = rows.value.filter((row) => row.matched).length
  liveText.value = `认出 ${rows.value.length} 条，其中 ${matched} 条已对上分类。再点一次才会再识别。`
}

function releaseVideo() {
  const video = videoRef.value
  const current = video?.srcObject
  if (current && typeof current.getTracks === 'function') {
    current.getTracks().forEach((track) => track.stop())
  }
  if (video) video.srcObject = null
}

function startObs() {
  liveText.value = 'OBS 地址是 127.0.0.1:4455。当前页面不会自动识别，点按钮才会截一张图。'
}

function toJpeg(source) {
  const canvas = source instanceof HTMLCanvasElement ? source : null
  const drawFile = () => new Promise((resolve, reject) => {
    const image = new Image()
    const url = URL.createObjectURL(source)
    image.onload = () => {
      const frame = document.createElement('canvas')
      frame.width = image.width
      frame.height = image.height
      frame.getContext('2d').drawImage(image, 0, 0)
      URL.revokeObjectURL(url)
      resolve(frame)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('没有收到画面'))
    }
    image.src = url
  })
  return Promise.resolve(canvas || drawFile()).then((frame) => new Promise((resolve, reject) => {
    frame.toBlob((blob) => {
      if (blob) resolve(blob)
      else reject(new Error('没有收到画面'))
    }, 'image/jpeg', 0.85)
  }))
}

async function recognizeSource(source, mode) {
  if (recognizing.value) return
  recognizing.value = true
  liveText.value = '正在让 MiMo 识别这张画面'
  reasoning.value = ''
  try {
    const items = await recognizeWithMimo(await toJpeg(source), mode, (text) => {
      reasoning.value += text
    })
    await applyRecognized(items)
  } catch (error) {
    rows.value = []
    liveText.value = error instanceof Error ? error.message : '识别失败'
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

async function captureAndRecognize(mode) {
  if (recognizing.value) return
  recognizing.value = true
  liveText.value = '请选择要截取的窗口'
  let stream = null
  try {
    stream = await navigator.mediaDevices.getDisplayMedia({ video: true })
    const video = videoRef.value
    video.srcObject = stream
    try {
      await video.play()
    } catch {
      // 自动播放被拦住时，仍然用当前这一帧。
    }
    await waitForFrame(video)
    await waitForFrame(video)
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    canvas.getContext('2d').drawImage(video, 0, 0)
    stream.getTracks().forEach((track) => track.stop())
    stream = null
    video.srcObject = null
    liveText.value = '正在让 MiMo 识别这张画面'
    reasoning.value = ''
    const items = await recognizeWithMimo(await toJpeg(canvas), mode, (text) => {
      reasoning.value += text
    })
    await applyRecognized(items)
  } catch (error) {
    const cancelled = error instanceof DOMException && error.name === 'NotAllowedError'
    if (!cancelled) rows.value = []
    liveText.value = cancelled || !(error instanceof Error) || error.message === '没有收到画面'
      ? '没有拿到画面'
      : error.message
  } finally {
    stream?.getTracks().forEach((track) => track.stop())
    releaseVideo()
    recognizing.value = false
  }
}

function onFile(upload, mode) {
  recognizeSource(upload.raw, mode)
}

async function saveRows() {
  const priced = rows.value.filter((row) => row.price)
  if (!priced.length) {
    liveText.value = '没有可写入的价格'
    return
  }
  try {
    await addRecognizedItems(priced, route.meta.side)
    liveText.value = '已写入物价，可在行情列表查看'
  } catch (error) {
    liveText.value = error instanceof Error ? error.message : '没有写入数据库'
  }
}

onBeforeUnmount(releaseVideo)
</script>

<template>
  <section>
    <el-card shadow="never">
      <template #header>{{ selling ? '摆摊识别' : '收购识别' }}</template>
      <el-alert title="点一次只截一张画面，再调一次 MiMo。不会在录屏期间连续识别。" type="info" show-icon :closable="false" />
      <el-alert class="warn" type="warning" show-icon :closable="false" title="采集画面仅稳定支持 1024×768 分辨率">
        <p>请将游戏窗口设置为 1024×768，并避免缩小共享窗口。</p>
      </el-alert>
      <el-space wrap>
        <template v-if="selling">
          <el-button type="primary" :disabled="recognizing" @click="captureAndRecognize('sell-stall')">摊位识别</el-button>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="(file) => onFile(file, 'sell-stall')">
            <el-button :disabled="recognizing">识别截图</el-button>
          </el-upload>
        </template>
        <template v-else>
          <el-button type="primary" :disabled="recognizing" @click="captureAndRecognize('buy-stall')">收购摊位识别</el-button>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="(file) => onFile(file, 'buy-stall')">
            <el-button :disabled="recognizing">识别收购截图</el-button>
          </el-upload>
          <el-button type="primary" :disabled="recognizing" @click="captureAndRecognize('buy-chat')">喊话收购识别</el-button>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="(file) => onFile(file, 'buy-chat')">
            <el-button :disabled="recognizing">识别喊话截图</el-button>
          </el-upload>
        </template>
        <el-button @click="startObs">OBS识别</el-button>
      </el-space>
      <video ref="videoRef" class="preview" autoplay muted playsinline />
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>识别状态</template>
      <el-text type="info">{{ liveText }}</el-text>
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>推理过程</template>
      <el-input
        :model-value="reasoning"
        type="textarea"
        readonly
        :rows="8"
        placeholder="点识别后，MiMo 的推理会显示在这里，结束后仍保留。"
      />
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
          <el-table-column label="物品" min-width="160">
            <template #default="{ row }"><ItemLabel :name="row.name" /></template>
          </el-table-column>
          <el-table-column label="分类" min-width="280">
            <template #default="{ row }">
              <span v-if="row.matched">{{ row.category }}</span>
              <div v-else class="category-pick">
                <el-tree-select
                  :model-value="row.categoryId"
                  :data="categoryTree"
                  :props="treeProps"
                  check-strictly
                  filterable
                  placeholder="选择分类"
                  @update:model-value="(id) => pickCategory(row, id)"
                />
                <el-tag v-if="!row.categoryId" type="warning" size="small">需要分类</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="140">
            <template #default="{ row }">{{ row.price ?? '未认出' }}</template>
          </el-table-column>
        </el-table>
        <el-button type="primary" class="save" :disabled="needsCategory" @click="saveRows">写入物价</el-button>
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
.category-pick { display: flex; align-items: center; gap: 8px; }
.category-pick :deep(.el-select) { width: 220px; }
.empty-price { display: flex; flex-direction: column; gap: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.empty-price strong { color: var(--el-text-color-primary); font-size: 20px; }
</style>
