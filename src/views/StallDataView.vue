<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { catalog, hasHistory, loadPrices, status } from '../lib/prices.js'
import { serverGroup, serverName } from '../lib/session.js'

const router = useRouter()
onMounted(() => { loadPrices().catch(() => {}) })

const stats = computed(() => {
  const items = catalog.value?.items ?? []
  const categories = new Set(items.map((item) => item.category))
  return [
    { label: '已录入物品', value: items.length },
    { label: '分类', value: categories.size },
    { label: '有更新记录', value: items.filter(hasHistory).length },
    { label: '最近更新日期', value: catalog.value?.updatedAt || '—' },
  ]
})
</script>

<template>
  <section>
    <el-text type="info">摆摊 · {{ serverGroup }} · {{ serverName }}</el-text>
    <h2>摊位数据</h2>
    <el-text type="info">这里汇总本地已经录入的摆摊单价，不连接游戏客户端。</el-text>
    <el-row :gutter="12" class="stats" v-loading="status === 'loading'">
      <el-col v-for="item in stats" :key="item.label" :xs="12" :sm="6">
        <el-card shadow="never">
          <el-text type="info">{{ item.label }}</el-text>
          <div class="value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-space>
      <el-button type="primary" @click="router.push('/stall/today')">今天摊位物价</el-button>
      <el-button @click="router.push('/stall/region')">全区物价</el-button>
    </el-space>
  </section>
</template>

<style scoped>
h2 { margin: 4px 0 8px; }
.stats { margin: 16px 0; }
.value { margin-top: 8px; font-size: 28px; font-weight: 700; }
</style>
