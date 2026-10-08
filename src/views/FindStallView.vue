<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <section>
    <el-text type="info">定位辅助</el-text>
    <h2>找摊位</h2>
    <el-alert
      title="本地物价没有摊位坐标"
      type="info"
      show-icon
      :closable="false"
      description="原站这一页靠识别收购摊位时记下的画面和地图位置，帮你回到游戏里找摊。这里没有那些坐标，也不会自动寻路。"
    />
    <el-button class="go" type="primary" @click="router.push('/buy/today')">去今日摊位物价查价</el-button>
  </section>
</template>

<style scoped>
h2 { margin: 4px 0 12px; }
.go { margin-top: 16px; }
</style>
