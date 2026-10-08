<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { catalog, formatPrice, loadPrices, matchItems, specOf, status } from '../lib/prices.js'
import { serverName } from '../lib/session.js'

const route = useRoute()
const keyword = ref('')
const category = ref('')

onMounted(() => {
  loadPrices().catch(() => {})
})

const categories = computed(() => [...new Set((catalog.value?.items ?? []).map((item) => item.category).filter(Boolean))])

watch(categories, (list) => {
  if (!category.value && list[0]) category.value = list[0]
}, { immediate: true })

const seriesList = computed(() => {
  const current = category.value || categories.value[0]
  const items = matchItems(catalog.value?.items ?? [], keyword.value)
    .filter((item) => item.category === current)
  const map = new Map()
  for (const item of items) {
    const spec = specOf(item.name)
    if (!map.has(spec.series)) map.set(spec.series, [])
    map.get(spec.series).push({ item, spec })
  }
  return [...map.entries()].map(([name, rows]) => ({
    name,
    rows: rows.sort((a, b) => (b.spec.level ?? b.item.price) - (a.spec.level ?? a.item.price)),
  }))
})

function tone(index, total) {
  if (total <= 1) return 'mid'
  const ratio = index / (total - 1)
  if (ratio < 0.34) return 'high'
  if (ratio > 0.67) return 'low'
  return 'mid'
}

function labelOf(row) {
  if (row.spec.level == null) return row.item.name
  return `${row.spec.level}级${row.spec.series}`
}
</script>

<template>
  <section>
    <el-text type="info">{{ route.meta.side }} · {{ serverName }}</el-text>
    <h2>全区物价</h2>
    <div class="layout" v-loading="status === 'loading'">
      <aside>
        <el-input v-model="keyword" clearable placeholder="搜索物品/系列" />
        <el-menu :key="category || categories[0]" :default-active="category || categories[0]" @select="category = $event">
          <el-menu-item v-for="name in categories" :key="name" :index="name">{{ name }}</el-menu-item>
        </el-menu>
      </aside>
      <div>
        <el-empty v-if="status === 'ready' && seriesList.length === 0" description="这个分类没有录入" />
        <div v-else class="series">
          <el-card v-for="series in seriesList" :key="series.name" shadow="never">
            <template #header>{{ series.name }}</template>
            <div v-for="(row, index) in series.rows" :key="row.item.name" class="level">
              <span>{{ labelOf(row) }}</span>
              <strong :class="tone(index, series.rows.length)">{{ formatPrice(row.item.price) }}</strong>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
h2 { margin: 4px 0 12px; }
.layout { display: grid; grid-template-columns: 180px 1fr; gap: 12px; align-items: start; }
.series { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.level { display: flex; justify-content: space-between; gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.high { color: #e11d48; }
.mid { color: #7c3aed; }
.low { color: #15803d; }
@media (max-width: 800px) { .layout { grid-template-columns: 1fr; } }
</style>
