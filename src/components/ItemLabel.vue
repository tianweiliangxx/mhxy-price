<script setup>
import { computed } from 'vue'
import { iconFor } from '../lib/prices.js'

const props = defineProps({
  name: { type: String, default: '' },
  item: { type: Object, default: null },
})

const label = computed(() => props.name || props.item?.name || '')
const src = computed(() => iconFor(props.item?.name ? props.item : label.value))
</script>

<template>
  <span class="item-label">
    <img v-if="src" class="icon" :src="src" alt="" />
    <span>{{ label }}</span>
  </span>
</template>

<style scoped>
.item-label { display: inline-flex; align-items: center; gap: 8px; min-width: 0; }
.icon { width: 28px; height: 28px; object-fit: contain; image-rendering: pixelated; flex: none; }
</style>
