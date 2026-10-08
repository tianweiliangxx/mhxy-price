<script setup>
import { computed, onMounted } from 'vue'
import { catalog, formatPrice, loadPrices, priceRecords, status } from '../lib/prices.js'

onMounted(() => {
  loadPrices().catch(() => {})
})

const rows = computed(() => {
  const list = []
  for (const item of catalog.value?.items ?? []) {
    const records = priceRecords(item)
    for (let index = 1; index < records.length; index += 1) {
      list.push({
        name: item.name,
        category: item.category,
        date: records[index].updatedAt,
        source: records[index].source || '当前',
        from: records[index - 1].price,
        to: records[index].price,
      })
    }
  }
  return list.reverse()
})
</script>

<template>
  <section>
    <div class="page-head">
      <h2>趋势报表</h2>
      <el-text type="info">按已保存的更新记录，看每条物价从旧价变成新价。</el-text>
    </div>
    <el-table v-loading="status === 'loading'" :data="rows">
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="name" label="物品" min-width="160" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="旧价" width="140">
        <template #default="{ row }">{{ formatPrice(row.from) }}</template>
      </el-table-column>
      <el-table-column label="新价" width="140">
        <template #default="{ row }">{{ formatPrice(row.to) }}</template>
      </el-table-column>
      <el-table-column prop="source" label="来源" min-width="120" />
    </el-table>
  </section>
</template>

<style scoped>
.page-head h2 { margin: 0 0 4px; }
.page-head { margin-bottom: 16px; }
</style>
