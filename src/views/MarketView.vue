<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { catalog, formatPrice, formatRmb, formatWan, iconFor, loadPrices, matchItems, quoteOf, status } from '../lib/prices.js'
import { serverGroup, serverName } from '../lib/session.js'

const route = useRoute()
const keyword = ref('')
const category = ref('')
const dateFrom = ref('2026-10-07')
const dateTo = ref('2026-10-08')
const selected = ref(null)

onMounted(() => {
  loadPrices().catch(() => {})
})

watch(catalog, (data) => {
  const dates = (data?.items ?? []).map((item) => item.updatedAt).filter(Boolean).sort()
  if (!dates.length) return
  dateFrom.value = dates[0]
  dateTo.value = dates[dates.length - 1]
}, { once: true })

const sell = computed(() => route.meta.side === '摆摊')
const showRmb = computed(() => route.path === '/buy/market')

const categories = computed(() => [...new Set((catalog.value?.items ?? []).filter((item) => !item.side || item.side === route.meta.side).map((item) => item.category).filter(Boolean))])

const rows = computed(() => {
  let items = (catalog.value?.items ?? []).filter((item) => {
    if (item.side && item.side !== route.meta.side) return false
    const day = item.updatedAt || ''
    return (!dateFrom.value || day >= dateFrom.value) && (!dateTo.value || day <= dateTo.value)
  })
  if (category.value) items = items.filter((item) => item.category === category.value)
  return matchItems(items, keyword.value).map((item) => ({ item, quote: quoteOf(item) }))
})
</script>

<template>
  <section class="split">
    <div>
      <div class="page-head">
        <div>
          <el-text type="info">{{ route.meta.side }} · {{ serverGroup }} · {{ serverName }}</el-text>
          <h2>{{ route.meta.title }} {{ rows.length }} 项</h2>
          <el-text type="info">{{ route.meta.hint }}</el-text>
        </div>
      </div>
      <el-form :inline="true" class="filters" @submit.prevent>
        <el-form-item label="日期">
          <el-date-picker v-model="dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始" />
          <span class="dash">-</span>
          <el-date-picker v-model="dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="keyword" clearable :placeholder="sell ? '搜索摆摊物品、规格、分类' : '搜索物品、等级、分类'" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="category" clearable placeholder="全部分类">
            <el-option v-for="name in categories" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-loading="status === 'loading'">
        <el-alert v-if="status === 'error'" title="价格数据加载失败" type="error" show-icon :closable="false" />
        <el-empty v-else-if="status === 'ready' && rows.length === 0" description="这个日期里没有物价" />
        <el-table v-else :data="rows" stripe highlight-current-row @current-change="selected = $event">
          <el-table-column :label="sell ? '规格' : '物品'" min-width="180">
            <template #default="{ row }">
              <div class="item">
                <el-image v-if="iconFor(row.item)" class="icon" :src="iconFor(row.item)" fit="contain" />
                <span>{{ row.item.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="220">
            <template #default="{ row }">{{ row.item.category }}</template>
          </el-table-column>
          <el-table-column v-if="sell" label="最低" width="120">
            <template #default="{ row }">{{ formatPrice(row.quote.low) }}</template>
          </el-table-column>
          <el-table-column v-else label="最高" width="120">
            <template #default="{ row }">{{ formatPrice(row.quote.high) }}</template>
          </el-table-column>
          <el-table-column label="均价" width="120">
            <template #default="{ row }">{{ formatPrice(row.quote.avg) }}</template>
          </el-table-column>
          <el-table-column v-if="showRmb" label="RMB最高" width="110">
            <template #default="{ row }">{{ formatRmb(row.quote.high) }}</template>
          </el-table-column>
          <el-table-column v-if="showRmb" label="RMB均价" width="110">
            <template #default="{ row }">{{ formatRmb(row.quote.avg) }}</template>
          </el-table-column>
          <el-table-column :label="sell ? '7天均' : '7天'" width="120">
            <template #default="{ row }">{{ formatPrice(row.quote.week) }}</template>
          </el-table-column>
          <el-table-column v-if="sell" label="最高" width="120">
            <template #default="{ row }">{{ formatPrice(row.quote.high) }}</template>
          </el-table-column>
          <el-table-column v-if="sell" label="样本" width="80">
            <template #default="{ row }">{{ row.quote.samples }}</template>
          </el-table-column>
          <el-table-column v-else label="摊位" width="80">
            <template #default="{ row }">{{ row.quote.stalls }}</template>
          </el-table-column>
          <el-table-column v-if="sell" label="时间" width="120">
            <template #default="{ row }">{{ row.item.updatedAt }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <el-card class="detail" shadow="never">
      <template #header>{{ sell ? '价格口径' : '摊位物价详情' }}</template>
      <el-empty v-if="!selected" :description="sell ? '选择规格' : '选择物品'" :image-size="64" />
      <div v-else>
        <strong>{{ selected.item.name }}</strong>
        <p>{{ selected.item.category }} · {{ formatWan(selected.item.price) }}</p>
        <p v-if="showRmb">约 {{ formatRmb(selected.item.price) }}</p>
        <el-text type="info">来源 {{ selected.item.source || '录入' }} · {{ selected.item.updatedAt }}</el-text>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.split { display: grid; grid-template-columns: 1fr 240px; gap: 16px; align-items: start; }
.page-head h2 { margin: 4px 0; }
.filters { margin-bottom: 8px; }
.dash { margin: 0 6px; color: var(--el-text-color-secondary); }
.item { display: flex; align-items: center; gap: 8px; }
.icon { width: 28px; height: 28px; }
.icon :deep(img) { image-rendering: pixelated; }
@media (max-width: 900px) { .split { grid-template-columns: 1fr; } }
</style>
