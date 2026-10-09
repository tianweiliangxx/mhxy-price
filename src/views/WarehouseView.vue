<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ItemLabel from '../components/ItemLabel.vue'
import { catalog, formatPrice, loadPrices } from '../lib/prices.js'
import { profile } from '../lib/session.js'

const router = useRouter()
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
    <el-empty v-if="profile.characters.length === 0" description="当前区服还没有角色">
      <el-button type="primary" @click="router.push('/me')">前往我的页面创建角色</el-button>
    </el-empty>
    <template v-else>
    <el-text type="info">按当前物价估算 {{ profile.characters[0].name }} 的库存，数据保存在这台浏览器。</el-text>
    <el-form :inline="true" class="add" @submit.prevent>
      <el-form-item label="物品">
        <el-select v-model="picked" filterable placeholder="选择已录入物品" style="width: 260px">
          <el-option v-for="option in options" :key="option.value" :label="option.label" :value="option.value">
            <ItemLabel :item="option.item" />
          </el-option>
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
      <el-table-column label="物品">
        <template #default="{ row }"><ItemLabel :name="row.name" /></template>
      </el-table-column>
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
    </template>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; }
.add { margin: 16px 0; }
.total { text-align: right; font-size: 18px; font-weight: 700; }
</style>
