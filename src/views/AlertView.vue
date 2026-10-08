<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { catalog, formatPrice, loadPrices, quoteOf } from '../lib/prices.js'

const KEY = 'mhxy-alerts'
const rules = ref({})

onMounted(() => {
  loadPrices().catch(() => {})
  try { rules.value = JSON.parse(localStorage.getItem(KEY) || '{}') } catch { rules.value = {} }
})

watch(rules, (value) => localStorage.setItem(KEY, JSON.stringify(value)), { deep: true })

const rows = computed(() => (catalog.value?.items ?? []).slice(0, 12).map((item) => {
  const rule = rules.value[item.name] || { fixed: '', ratio: 30, enabled: false }
  const avg = quoteOf(item).avg
  const fixed = Number(rule.fixed)
  const ratio = Number(rule.ratio)
  const hit = rule.enabled && (
    (Number.isFinite(fixed) && fixed > 0 && item.price >= fixed)
    || (Number.isFinite(ratio) && item.price >= avg * (1 + ratio / 100))
  )
  return { item, avg, rule, hit }
}))

function patch(name, field, value) {
  rules.value = { ...rules.value, [name]: { fixed: '', ratio: 30, enabled: false, ...rules.value[name], [field]: value } }
}
</script>

<template>
  <section>
    <h2>收购提醒</h2>
    <el-text type="info">当前价高于固定价，或高于均价一定比例时记一笔。规则保存在这台浏览器。</el-text>
    <el-table class="table" :data="rows">
      <el-table-column label="物品" min-width="140">
        <template #default="{ row }">{{ row.item.name }}</template>
      </el-table-column>
      <el-table-column label="系列" width="120">
        <template #default="{ row }">{{ row.item.category }}</template>
      </el-table-column>
      <el-table-column label="当前均价" width="120">
        <template #default="{ row }">{{ formatPrice(row.avg) }}</template>
      </el-table-column>
      <el-table-column label="固定价高于" width="150">
        <template #default="{ row }">
          <el-input :model-value="row.rule.fixed" placeholder="例如 70000" @update:model-value="patch(row.item.name, 'fixed', $event)" />
        </template>
      </el-table-column>
      <el-table-column label="均价比例高于" width="140">
        <template #default="{ row }">
          <el-input :model-value="row.rule.ratio" placeholder="默认 30" @update:model-value="patch(row.item.name, 'ratio', $event)" />
        </template>
      </el-table-column>
      <el-table-column label="开启" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.rule.enabled" @update:model-value="patch(row.item.name, 'enabled', $event)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.hit" type="danger">命中</el-tag>
          <el-text v-else type="info">—</el-text>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; }
.table { margin-top: 12px; }
</style>
