<script setup>
import { computed, onMounted, ref } from 'vue'
import { pinyin } from 'pinyin-pro'

const keyword = ref('')
const category = ref('')
const onlyChanges = ref(false)
const status = ref('loading')
const catalog = ref(null)

const priceFormat = new Intl.NumberFormat('zh-CN')

onMounted(async () => {
  try {
    const response = await fetch('/prices.json')
    if (!response.ok) throw new Error(String(response.status))
    const data = await response.json()
    if (!data || !Array.isArray(data.items)) {
      throw new Error('invalid prices')
    }
    catalog.value = data
    status.value = 'ready'
  } catch {
    status.value = 'error'
  }
})

const categories = computed(() => {
  const names = (catalog.value?.items ?? [])
    .map((item) => item.category)
    .filter(Boolean)
  return [...new Set(names)]
})

const initialsByItem = computed(() => {
  const map = new Map()
  for (const item of catalog.value?.items ?? []) {
    map.set(item, initialsOf(item.name ?? ''))
  }
  return map
})

const filtered = computed(() => {
  const items = catalog.value?.items ?? []
  const scoped = category.value
    ? items.filter((item) => item.category === category.value)
    : items.slice()
  const pool = onlyChanges.value ? scoped.filter(hasHistory) : scoped
  const query = keyword.value.trim()
  if (!query) return pool
  const queryKey = query.toLowerCase()

  const ranked = [[], [], [], []]
  for (const item of pool) {
    const name = item.name ?? ''
    const initials = initialsByItem.value.get(item) ?? ''
    if (name.startsWith(query)) ranked[0].push(item)
    else if (initials.startsWith(queryKey)) ranked[1].push(item)
    else if (name.includes(query)) ranked[2].push(item)
    else if (initials.includes(queryKey)) ranked[3].push(item)
  }
  return ranked.flat()
})

function initialsOf(name) {
  const letters = pinyin(name, {
    pattern: 'first',
    toneType: 'none',
    type: 'array',
    nonZh: 'consecutive',
  })
  return letters.join('').toLowerCase().replace(/[^a-z0-9]/g, '')
}

function hasHistory(item) {
  return Array.isArray(item.history) && item.history.length > 0
}

function priceRecords(item) {
  return [
    ...(item.history ?? []),
    { price: item.price, updatedAt: item.updatedAt, source: item.source },
  ]
}

const icons = {
  '炼妖石·105': '/icons/lianyaoshi.png',
  '炼妖石·115': '/icons/lianyaoshi.png',
  '炼妖石·125': '/icons/lianyaoshi.png',
  '顺逆神针': '/icons/shunnishenzhen.png',
  '储灵袋': '/icons/chulingdai.png',
  '玉灵果': '/icons/yulingguo.png',
}

function iconFor(item) {
  if (icons[item.name]) return icons[item.name]
  if (item.category === '低级内丹') return '/icons/neidan.png'
  return ''
}

function formatPrice(price) {
  return priceFormat.format(price)
}

function formatWan(price) {
  const wan = price / 10000
  const text = Number.isInteger(wan) ? String(wan) : String(Number(wan.toFixed(4)))
  return `${text}万`
}
</script>

<template>
  <el-container class="page">
    <el-header class="header" height="auto">
      <h1>梦幻西游道具价格</h1>
      <el-text v-if="status === 'ready'" class="meta" type="info">
        更新日期 {{ catalog.updatedAt }} · {{ catalog.currency }} · {{ filtered.length }} 条
      </el-text>
    </el-header>

    <el-main>
      <el-form class="filters" :inline="true" @submit.prevent>
        <el-form-item label="名称">
          <el-input
            v-model="keyword"
            clearable
            placeholder="输入名称或首字母，例如 jll"
            :disabled="status !== 'ready'"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="category"
            clearable
            placeholder="全部"
            :disabled="status !== 'ready'"
          >
            <el-option v-for="name in categories" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="onlyChanges" :disabled="status !== 'ready'">只看有更新记录</el-checkbox>
        </el-form-item>
      </el-form>

      <div v-loading="status === 'loading'" class="result">
        <el-alert
          v-if="status === 'error'"
          title="价格数据加载失败"
          type="error"
          show-icon
          :closable="false"
        />
        <el-empty v-else-if="status === 'ready' && filtered.length === 0" description="没有找到相关物品" />
        <div v-else class="list">
          <el-card
            v-for="item in filtered"
            :key="`${item.category}-${item.name}`"
            shadow="hover"
          >
            <div class="item">
              <el-image
                v-if="iconFor(item)"
                class="icon"
                :src="iconFor(item)"
                :alt="item.name"
                fit="contain"
              />
              <div class="item-body">
                <div class="name-row">
                  <span class="name">{{ item.name }}</span>
                  <el-tag type="warning" effect="plain">{{ item.category }}</el-tag>
                </div>
                <div class="price-row">
                  <span class="price">{{ formatPrice(item.price) }}</span>
                  <el-text type="info">约 {{ formatWan(item.price) }}</el-text>
                </div>
                <el-text class="updated" type="info" size="small">更新于 {{ item.updatedAt }}</el-text>
                <div v-if="hasHistory(item)" class="history">
                  <el-text type="info" size="small">更新记录</el-text>
                  <el-timeline>
                    <el-timeline-item
                      v-for="(record, index) in priceRecords(item)"
                      :key="index"
                      :timestamp="record.updatedAt"
                      placement="top"
                    >
                      <el-text v-if="record.source">{{ record.source }}</el-text>
                      {{ formatPrice(record.price) }}
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-main>
  </el-container>
</template>

<style scoped>
.page {
  max-width: 760px;
  margin: 0 auto;
  min-height: 100vh;
}

.header {
  padding: 28px 20px 0;
}

.header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.meta {
  display: block;
  margin-top: 8px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.filters :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 8px;
}

.filters :deep(.el-input) {
  width: 280px;
}

.filters :deep(.el-select) {
  width: 160px;
}

.result {
  min-height: 120px;
}

.list {
  display: grid;
  gap: 12px;
}

.item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.item-body {
  flex: 1;
  min-width: 0;
}

.icon {
  width: 44px;
  height: 44px;
  flex: none;
}

.icon :deep(img) {
  image-rendering: pixelated;
}

.name-row,
.price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.name {
  font-size: 18px;
  font-weight: 700;
}

.price {
  margin-top: 8px;
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.updated {
  display: block;
  margin-top: 6px;
}

.history {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.history :deep(.el-timeline) {
  margin-top: 12px;
  padding-left: 2px;
}

@media (max-width: 640px) {
  .filters :deep(.el-input),
  .filters :deep(.el-select) {
    width: 100%;
  }
}
</style>
