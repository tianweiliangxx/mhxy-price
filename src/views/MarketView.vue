<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { catalog, formatPrice, formatWan, hasHistory, iconFor, loadPrices, matchItems, priceRecords, status } from '../lib/prices.js'
import { serverGroup, serverName } from '../lib/session.js'

const route = useRoute()
const keyword = ref('')
const category = ref('')
const onlyChanges = ref(false)

onMounted(() => {
  loadPrices().catch(() => {})
})

const categories = computed(() => [...new Set((catalog.value?.items ?? []).map((item) => item.category).filter(Boolean))])

const rows = computed(() => {
  let items = catalog.value?.items ?? []
  if (category.value) items = items.filter((item) => item.category === category.value)
  if (onlyChanges.value) items = items.filter(hasHistory)
  return matchItems(items, keyword.value)
})
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <el-text type="info">{{ route.meta.side }} · {{ serverGroup }} · {{ serverName }}</el-text>
        <h2>{{ route.meta.title }}</h2>
        <el-text type="info">{{ route.meta.hint }}</el-text>
      </div>
      <el-tag type="success" effect="plain">本地物价 {{ rows.length }} 条</el-tag>
    </div>

    <el-form :inline="true" class="filters" @submit.prevent>
      <el-form-item label="物品">
        <el-input v-model="keyword" clearable placeholder="名称或首字母，例如 jll" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="category" clearable placeholder="全部">
          <el-option v-for="name in categories" :key="name" :label="name" :value="name" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="onlyChanges">只看有更新记录</el-checkbox>
      </el-form-item>
    </el-form>

    <div v-loading="status === 'loading'">
      <el-alert v-if="status === 'error'" title="价格数据加载失败" type="error" show-icon :closable="false" />
      <el-empty v-else-if="status === 'ready' && rows.length === 0" description="没有找到相关物品" />
      <el-table v-else :data="rows" stripe>
        <el-table-column label="物品" min-width="220">
          <template #default="{ row }">
            <div class="item">
              <el-image v-if="iconFor(row)" class="icon" :src="iconFor(row)" fit="contain" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="单价" width="140">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="约合" width="120">
          <template #default="{ row }">{{ formatWan(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新日期" width="120" />
        <el-table-column label="来源" min-width="120">
          <template #default="{ row }">{{ row.source || '录入' }}</template>
        </el-table-column>
        <el-table-column type="expand" label="记录" width="70">
          <template #default="{ row }">
            <el-timeline v-if="hasHistory(row)">
              <el-timeline-item
                v-for="(record, index) in priceRecords(row)"
                :key="index"
                :timestamp="record.updatedAt"
              >
                {{ record.source || '当前' }} {{ formatPrice(record.price) }}
              </el-timeline-item>
            </el-timeline>
            <el-text v-else type="info">这条还没有更早的价格记录</el-text>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.page-head h2 {
  margin: 4px 0;
}

.filters {
  margin-bottom: 8px;
}

.filters :deep(.el-input),
.filters :deep(.el-select) {
  width: 220px;
}

.item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon {
  width: 32px;
  height: 32px;
  flex: none;
}

.icon :deep(img) {
  image-rendering: pixelated;
}
</style>
