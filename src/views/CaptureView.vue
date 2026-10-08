<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const modelText = ref('正在加载本地识别模型...')
const liveText = ref('等待录屏/OBS识别')
const running = ref(false)
let stream = null

function stop() {
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
  running.value = false
  liveText.value = '等待录屏/OBS识别'
}

async function startScreen() {
  try {
    stream = await navigator.mediaDevices.getDisplayMedia({ video: true })
    running.value = true
    modelText.value = '已接到录屏画面。物品检测和价格识别还没装上，这一帧不会自动入库。'
    liveText.value = '录屏已连接'
    stream.getVideoTracks()[0].addEventListener('ended', stop)
  } catch {
    liveText.value = '没有拿到画面'
  }
}

function startObs() {
  running.value = false
  liveText.value = '等待 OBS 127.0.0.1:4455。网页还没有接上 OBS 的数据。'
}

onBeforeUnmount(stop)
</script>

<template>
  <section>
    <el-card shadow="never">
      <template #header>{{ route.meta.side === '摆摊' ? '摆摊实时识别' : '识别控制' }}</template>
      <el-alert :title="modelText" type="info" show-icon :closable="false" />
      <el-alert class="warn" type="warning" show-icon :closable="false" title="采集画面仅稳定支持 1024×768 分辨率">
        <p>请将游戏窗口设置为 1024×768，并避免缩小共享窗口。</p>
      </el-alert>
      <el-space>
        <el-button @click="startScreen">录屏识别</el-button>
        <el-button @click="startObs">OBS识别</el-button>
        <el-button :disabled="!running" @click="stop">停止采集</el-button>
        <el-button @click="modelText = '正在加载本地识别模型...'">刷新</el-button>
      </el-space>
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>实时识别状态</template>
      <el-text type="info">{{ liveText }}</el-text>
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>本次采集最高价</template>
      <el-row :gutter="12">
        <el-col :span="6"><el-statistic title="物品数" :value="0" /></el-col>
        <el-col :span="6"><el-statistic title="记录数" :value="0" /></el-col>
        <el-col :span="6"><el-statistic title="最高价" value="-" /></el-col>
        <el-col :span="6"><el-statistic title="摊位合计" :value="0" /></el-col>
      </el-row>
      <el-empty description="还没有从画面里认出价格" />
    </el-card>
  </section>
</template>

<style scoped>
.warn { margin: 12px 0; }
.block { margin-top: 12px; }
</style>
