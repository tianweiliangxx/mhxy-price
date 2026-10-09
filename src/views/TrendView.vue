<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import ItemLabel from '../components/ItemLabel.vue'
import { catalog, loadPrices, priceRecords, specOf, status } from '../lib/prices.js'

const dateFrom = ref('2026-10-02')
const dateTo = ref('2026-10-08')
const category = ref('')
const picked = ref([])

onMounted(() => { loadPrices().catch(() => {}) })

const categories = computed(() => [...new Set((catalog.value?.items ?? []).map((item) => item.category).filter(Boolean))])

const seriesOptions = computed(() => {
  const names = new Set()
  for (const item of catalog.value?.items ?? []) {
    if (category.value && item.category !== category.value) continue
    names.add(specOf(item.name).series)
  }
  return [...names]
})

watch(seriesOptions, (options) => {
  if (!category.value && categories.value.length) category.value = categories.value[0]
  picked.value = options.slice(0, 4)
}, { immediate: true })

const lines = computed(() => picked.value.map((name, index) => {
  const items = (catalog.value?.items ?? []).filter((item) => specOf(item.name).series === name && (!category.value || item.category === category.value))
  const points = items.flatMap((item) => priceRecords(item).map((record) => ({ date: record.updatedAt, price: record.price })))
    .filter((point) => point.date >= dateFrom.value && point.date <= dateTo.value)
    .sort((a, b) => a.date.localeCompare(b.date))
  return { name, points, color: ['#1677ff', '#e11d48', '#7c3aed', '#15803d'][index % 4] }
}).filter((line) => line.points.length >= 2))

const chart = computed(() => {
  const all = lines.value.flatMap((line) => line.points.map((point) => point.price))
  if (!all.length) return null
  const min = Math.min(...all)
  const max = Math.max(...all)
  const span = Math.max(max - min, 1)
  return lines.value.map((line) => {
    const d = line.points.map((point, index) => {
      const x = lines.value.length === 1 && line.points.length === 1 ? 20 : (index / Math.max(line.points.length - 1, 1)) * 280 + 20
      const y = 140 - ((point.price - min) / span) * 110
      return `${x},${y}`
    }).join(' ')
    return { ...line, d }
  })
})
</script>

<template>
  <section>
    <h2>趋势报表</h2>
    <el-form :inline="true" class="filters">
      <el-date-picker v-model="dateFrom" type="date" value-format="YYYY-MM-DD" />
      <el-date-picker v-model="dateTo" type="date" value-format="YYYY-MM-DD" />
      <el-select v-model="category" placeholder="分类" style="width: 140px">
        <el-option v-for="name in categories" :key="name" :label="name" :value="name" />
      </el-select>
      <el-tag>最高价</el-tag>
      <el-tag type="info">均价</el-tag>
    </el-form>
    <div class="tags">
      <el-text type="info">当前 {{ picked.length }} 个系列</el-text>
      <el-check-tag v-for="name in seriesOptions.slice(0, 12)" :key="name" :checked="picked.includes(name)" @change="(on) => { picked = on ? [...picked, name].slice(0, 24) : picked.filter((item) => item !== name) }">
        <ItemLabel :name="name" />
      </el-check-tag>
    </div>
    <el-card v-loading="status === 'loading'" shadow="never" class="chart-card">
      <el-empty v-if="!chart" description="当前筛选没有趋势数据" />
      <svg v-else viewBox="0 0 320 160" class="chart">
        <polyline v-for="line in chart" :key="line.name" :points="line.d" fill="none" :stroke="line.color" stroke-width="2" />
      </svg>
      <div v-if="chart" class="legend">
        <span v-for="line in chart" :key="line.name"><i :style="{ background: line.color }" /><ItemLabel :name="line.name" /></span>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 12px; }
.filters, .tags { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; }
.chart { width: 100%; height: 220px; background: #fff; }
.legend { display: flex; gap: 12px; flex-wrap: wrap; }
.legend i { display: inline-block; width: 10px; height: 10px; margin-right: 4px; }
</style>
