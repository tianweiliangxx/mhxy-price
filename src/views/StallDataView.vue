<script setup>
import { computed, onMounted } from 'vue'
import { catalog, formatPrice, loadPrices, status } from '../lib/prices.js'
import { serverName } from '../lib/session.js'

onMounted(() => { loadPrices().catch(() => {}) })

const rows = computed(() => {
  const items = (catalog.value?.items ?? []).filter((item) => !item.side || item.side === '摆摊')
  if (!items.length) return []
  const prices = items.map((item) => item.price)
  return [{
    stall: '本地录入',
    owner: '—',
    coord: '—',
    type: '摆摊',
    goods: items.length,
    count: items.length,
    low: Math.min(...prices),
    high: Math.max(...prices),
    at: catalog.value?.updatedAt || items[0].updatedAt,
    server: serverName.value,
  }]
})
</script>

<template>
  <section>
    <h2>摊位快照 {{ rows.length }} 个</h2>
    <el-text type="info">还没有从画面采集摊位。下面这一行是本地物价本的汇总，摊主和坐标留空。</el-text>
    <el-table v-loading="status === 'loading'" :data="rows" class="table">
      <el-table-column prop="stall" label="摊位" />
      <el-table-column prop="owner" label="摊主ID" />
      <el-table-column prop="coord" label="坐标" />
      <el-table-column prop="type" label="类型" />
      <el-table-column prop="goods" label="物品" />
      <el-table-column prop="count" label="数量" />
      <el-table-column label="最低" width="120">
        <template #default="{ row }">{{ formatPrice(row.low) }}</template>
      </el-table-column>
      <el-table-column label="最高" width="120">
        <template #default="{ row }">{{ formatPrice(row.high) }}</template>
      </el-table-column>
      <el-table-column prop="at" label="最后采集" />
    </el-table>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; }
.table { margin-top: 12px; }
</style>
