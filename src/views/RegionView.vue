<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { catalog, formatPrice, formatWan, iconFor, loadPrices, matchItems, status } from '../lib/prices.js'
import { serverGroup, serverName } from '../lib/session.js'

const route = useRoute()
const keyword = ref('')

onMounted(() => {
  loadPrices().catch(() => {})
})

const groups = computed(() => {
  const items = matchItems(catalog.value?.items ?? [], keyword.value)
  const map = new Map()
  for (const item of items) {
    const name = item.category || '未分类'
    if (!map.has(name)) map.set(name, [])
    map.get(name).push(item)
  }
  return [...map.entries()].map(([name, list]) => ({
    name,
    list: [...list].sort((a, b) => b.price - a.price),
  }))
})
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <el-text type="info">{{ route.meta.side }} · {{ serverGroup }} · {{ serverName }}</el-text>
        <h2>{{ route.meta.title }}</h2>
        <el-text type="info">同一分类放在一起，按单价从高到低看，少一次次搜索。</el-text>
      </div>
    </div>
    <el-input v-model="keyword" clearable placeholder="搜索分类里的物品，例如 内丹 或 nd" class="search" />
    <div v-loading="status === 'loading'">
      <el-alert v-if="status === 'error'" title="价格数据加载失败" type="error" show-icon :closable="false" />
      <el-empty v-else-if="status === 'ready' && groups.length === 0" description="没有找到相关物品" />
      <div v-else class="groups">
        <el-card v-for="group in groups" :key="group.name" shadow="never">
          <template #header>
            <div class="group-head">
              <strong>{{ group.name }}</strong>
              <el-tag effect="plain">{{ group.list.length }}</el-tag>
            </div>
          </template>
          <div class="grid">
            <div v-for="item in group.list" :key="item.name" class="cell">
              <el-image v-if="iconFor(item)" class="icon" :src="iconFor(item)" fit="contain" />
              <div>
                <div>{{ item.name }}</div>
                <el-text type="primary">{{ formatPrice(item.price) }}</el-text>
                <el-text type="info"> {{ formatWan(item.price) }}</el-text>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-head h2 { margin: 4px 0; }
.search { max-width: 360px; margin-bottom: 16px; }
.groups { display: grid; gap: 12px; }
.group-head, .cell { display: flex; align-items: center; gap: 8px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 10px; }
.icon { width: 32px; height: 32px; flex: none; }
.icon :deep(img) { image-rendering: pixelated; }
</style>
