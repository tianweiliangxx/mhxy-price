<script setup>
import { computed, onMounted, ref } from 'vue'
import { pinyin } from 'pinyin-pro'

const keyword = ref('')
const category = ref('')
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
  const query = keyword.value.trim()
  if (!query) return scoped
  const queryKey = query.toLowerCase()

  const ranked = [[], [], [], []]
  for (const item of scoped) {
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
  <main class="page">
    <header class="header">
      <h1>梦幻西游道具价格</h1>
      <p v-if="status === 'ready'" class="meta">
        更新日期 {{ catalog.updatedAt }} · {{ catalog.currency }} · {{ filtered.length }} 条
      </p>
    </header>

    <section class="filters" aria-label="筛选">
      <label class="field">
        <span>名称</span>
        <input
          v-model="keyword"
          type="search"
          placeholder="输入名称或首字母，例如 jll"
          :disabled="status !== 'ready'"
        />
      </label>
      <label class="field">
        <span>分类</span>
        <select v-model="category" :disabled="status !== 'ready'">
          <option value="">全部</option>
          <option v-for="name in categories" :key="name" :value="name">{{ name }}</option>
        </select>
      </label>
    </section>

    <p v-if="status === 'loading'" class="state">正在加载价格…</p>
    <p v-else-if="status === 'error'" class="state state-error" role="alert">价格数据加载失败</p>
    <p v-else-if="filtered.length === 0" class="state">没有找到相关物品</p>
    <ul v-else class="list">
      <li v-for="item in filtered" :key="`${item.category}-${item.name}`">
        <div class="name-row">
          <strong>{{ item.name }}</strong>
          <span class="tag">{{ item.category }}</span>
        </div>
        <div class="price-row">
          <span class="price">{{ formatPrice(item.price) }}</span>
          <span class="wan">约 {{ formatWan(item.price) }}</span>
        </div>
        <p class="updated">更新于 {{ item.updatedAt }}</p>
      </li>
    </ul>
  </main>
</template>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 20px 48px;
}

.header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.meta {
  margin: 8px 0 0;
  color: #57534e;
}

.filters {
  display: grid;
  grid-template-columns: 1fr 180px;
  gap: 12px;
  margin: 24px 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #44403c;
  font-size: 14px;
}

.field input,
.field select {
  height: 42px;
  padding: 0 12px;
  border: 1px solid #d6d3d1;
  border-radius: 8px;
  background: #fff;
  color: inherit;
}

.field input:focus,
.field select:focus {
  outline: 2px solid #b45309;
  border-color: #b45309;
}

.state {
  margin: 0;
  padding: 28px 16px;
  text-align: center;
  color: #57534e;
  background: #fff;
  border-radius: 12px;
}

.state-error {
  color: #9f1239;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.list li {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgb(28 25 23 / 6%);
}

.name-row,
.price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.name-row strong {
  font-size: 18px;
}

.tag {
  color: #9a3412;
  background: #ffedd5;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
}

.price {
  margin-top: 8px;
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.wan,
.updated {
  color: #78716c;
}

.updated {
  margin: 6px 0 0;
  font-size: 13px;
}

@media (max-width: 640px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
