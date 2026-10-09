<script setup>
import { computed, onMounted } from 'vue'
import ItemLabel from '../components/ItemLabel.vue'
import { catalog, formatPrice, loadPrices, priceDrop, quoteOf, status } from '../lib/prices.js'
import { serverName } from '../lib/session.js'

onMounted(() => { loadPrices().catch(() => {}) })

const rows = computed(() => (catalog.value?.items ?? [])
  .filter((item) => priceDrop(item) !== 0)
  .map((item) => {
    const previous = item.history.at(-1).price
    const quote = quoteOf(item)
    return { item, previous, quote, gap: item.price - previous }
  }))
</script>

<template>
  <section>
    <h2>捡漏集合</h2>
    <el-alert type="info" show-icon :closable="false" title="本地没有采集基准，用上一条价格记录当作当天基准。摊主、坐标和证据要等画面采集之后才有。" />
    <el-table v-loading="status === 'loading'" :data="rows" class="table">
      <el-table-column label="物品" min-width="160">
        <template #default="{ row }">
          <ItemLabel :item="row.item" />
        </template>
      </el-table-column>
      <el-table-column label="当前价" width="120">
        <template #default="{ row }">{{ formatPrice(row.item.price) }}</template>
      </el-table-column>
      <el-table-column label="当天基准" width="120">
        <template #default="{ row }">{{ formatPrice(row.previous) }}</template>
      </el-table-column>
      <el-table-column label="高出当天" width="120">
        <template #default="{ row }">{{ formatPrice(row.gap) }}</template>
      </el-table-column>
      <el-table-column label="7天基准" width="120">
        <template #default="{ row }">{{ formatPrice(row.quote.week) }}</template>
      </el-table-column>
      <el-table-column label="高出7天" width="120">
        <template #default="{ row }">{{ formatPrice(row.item.price - row.quote.week) }}</template>
      </el-table-column>
      <el-table-column label="区服" width="150">
        <template #default>{{ serverName }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default>本地记录</template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 12px; }
.table { margin-top: 12px; }
</style>
