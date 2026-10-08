<script setup>
import { computed, onMounted } from 'vue'
import { catalog, formatPrice, formatWan, iconFor, loadPrices, priceDrop, status } from '../lib/prices.js'
import { serverName } from '../lib/session.js'

onMounted(() => {
  loadPrices().catch(() => {})
})

const rows = computed(() => (catalog.value?.items ?? [])
  .map((item) => ({ item, drop: priceDrop(item) }))
  .filter((row) => row.drop > 0)
  .sort((a, b) => b.drop - a.drop))
</script>

<template>
  <section>
    <div class="page-head">
      <el-text type="info">收购高卖 · {{ serverName }}</el-text>
      <h2>捡漏集合</h2>
      <el-text type="info">最新单价低于上一条记录的物品，按降价金额从高到低排列。</el-text>
    </div>
    <div v-loading="status === 'loading'">
      <el-empty v-if="status === 'ready' && rows.length === 0" description="目前没有降价物品" />
      <el-table v-else :data="rows">
        <el-table-column label="物品" min-width="200">
          <template #default="{ row }">
            <div class="item">
              <el-image v-if="iconFor(row.item)" class="icon" :src="iconFor(row.item)" fit="contain" />
              <span>{{ row.item.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="上次" width="140">
          <template #default="{ row }">{{ formatPrice(row.item.history.at(-1).price) }}</template>
        </el-table-column>
        <el-table-column label="现价" width="140">
          <template #default="{ row }">{{ formatPrice(row.item.price) }}</template>
        </el-table-column>
        <el-table-column label="降了" width="160">
          <template #default="{ row }">
            <el-text type="success">{{ formatPrice(row.drop) }}（{{ formatWan(row.drop) }}）</el-text>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.page-head h2 { margin: 4px 0; }
.item { display: flex; align-items: center; gap: 8px; }
.icon { width: 32px; height: 32px; }
.icon :deep(img) { image-rendering: pixelated; }
</style>
