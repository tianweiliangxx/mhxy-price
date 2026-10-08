<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { catalog, formatPrice, loadPrices } from '../lib/prices.js'

const STORAGE = 'mhxy-warehouse'
const picked = ref('')
const quantity = ref(1)
const rows = ref([])

onMounted(() => {
  loadPrices().catch(() => {})
  try {
    rows.value = JSON.parse(localStorage.getItem(STORAGE) || '[]')
  } catch {
    rows.value = []
  }
})

watch(rows, (value) => {
  localStorage.setItem(STORAGE, JSON.stringify(value))
}, { deep: true })

const options = computed(() => (catalog.value?.items ?? []).map((item) => ({
  value: `${item.category}/${item.name}`,
  label: `${item.name} · ${item.category}`,
  item,
})))

const total = computed(() => rows.value.reduce((sum, row) => sum + row.price * row.quantity, 0))

function add() {
  const found = options.value.find((option) => option.value === picked.value)
  if (!found) return
  const existing = rows.value.find((row) => row.name === found.item.name && row.category === found.item.category)
  if (existing) existing.quantity += quantity.value
  else rows.value.push({
    name: found.item.name,
    category: found.item.category,
    price: found.item.price,
    quantity: quantity.value,
  })
}

function remove(index) {
  rows.value.splice(index, 1)
}
</script>

<template>
  <section>
    <h2>仓库管理</h2>
    <el-text type="info">按当前物价估算库存，数据保存在这台浏览器。</el-text>
    <el-form :inline="true" class="add" @submit.prevent>
      <el-form-item label="物品">
        <el-select v-model="picked" filterable placeholder="选择已录入物品" style="width: 260px">
          <el-option v-for="option in options" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="数量">
        <el-input-number v-model="quantity" :min="1" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="add">放入仓库</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="rows">
      <el-table-column prop="name" label="物品" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column label="单价" width="140">
        <template #default="{ row }">{{ formatPrice(row.price) }}</template>
      </el-table-column>
      <el-table-column label="小计" width="160">
        <template #default="{ row }">{{ formatPrice(row.price * row.quantity) }}</template>
      </el-table-column>
      <el-table-column label="" width="90">
        <template #default="{ $index }">
          <el-button link type="danger" @click="remove($index)">移出</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p class="total">合计 {{ formatPrice(total) }}</p>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; }
.add { margin: 16px 0; }
.total { text-align: right; font-size: 18px; font-weight: 700; }
</style>
