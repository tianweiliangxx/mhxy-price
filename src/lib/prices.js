import { ref } from 'vue'
import { pinyin } from 'pinyin-pro'

export const catalog = ref(null)
export const status = ref('idle')

let pending = null

export function loadPrices() {
  if (catalog.value || pending) return pending
  status.value = 'loading'
  pending = fetch('/api/items')
    .then((response) => {
      if (!response.ok) throw new Error(String(response.status))
      return response.json()
    })
    .then((data) => {
      if (!data || !Array.isArray(data.items)) throw new Error('invalid prices')
      catalog.value = data
      status.value = 'ready'
      return data
    })
    .catch((error) => {
      status.value = 'error'
      pending = null
      throw error
    })
  return pending
}

const icons = {
  '炼妖石·105': '/icons/lianyaoshi.png',
  '炼妖石·115': '/icons/lianyaoshi.png',
  '炼妖石·125': '/icons/lianyaoshi.png',
  顺逆神针: '/icons/shunnishenzhen.png',
  储灵袋: '/icons/chulingdai.png',
  玉灵果: '/icons/yulingguo.png',
}

export function iconFor(item) {
  if (icons[item.name]) return icons[item.name]
  if ((item.category || '').includes('召唤兽内丹') || (item.name || '').endsWith('内丹')) return '/icons/neidan.png'
  return ''
}

export function initialsOf(name) {
  const letters = pinyin(name || '', {
    pattern: 'first',
    toneType: 'none',
    type: 'array',
    nonZh: 'consecutive',
  })
  return letters.join('').toLowerCase().replace(/[^a-z0-9]/g, '')
}

const priceFormat = new Intl.NumberFormat('zh-CN')

export function formatPrice(price) {
  return priceFormat.format(price)
}

export function formatWan(price) {
  const wan = price / 10000
  const text = Number.isInteger(wan) ? String(wan) : String(Number(wan.toFixed(4)))
  return `${text}万`
}

export function hasHistory(item) {
  return Array.isArray(item.history) && item.history.length > 0
}

export function priceRecords(item) {
  return [
    ...(item.history ?? []),
    { price: item.price, updatedAt: item.updatedAt, source: item.source },
  ]
}

export function matchItems(items, query) {
  const text = query.trim()
  if (!text) return items
  const key = text.toLowerCase()
  const ranked = [[], [], [], []]
  for (const item of items) {
    const name = item.name ?? ''
    const initials = initialsOf(name)
    if (name.startsWith(text)) ranked[0].push(item)
    else if (initials.startsWith(key)) ranked[1].push(item)
    else if (name.includes(text)) ranked[2].push(item)
    else if (initials.includes(key)) ranked[3].push(item)
  }
  return ranked.flat()
}

export async function addRecognizedItems(rows, side) {
  const today = new Date().toISOString().slice(0, 10)
  const incoming = rows.map((row) => ({
    name: row.name,
    side: side === '摆摊' ? '摆摊' : '收购',
    category: row.category || '',
    categoryId: row.categoryId ?? null,
    price: row.price,
    updatedAt: today,
    source: '画面识别',
  }))
  const response = await fetch('/api/items', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(incoming),
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.error || '没有写入数据库')
  }
  catalog.value = null
  pending = null
  return loadPrices()
}

export function priceDrop(item) {
  if (!hasHistory(item)) return 0
  const previous = item.history[item.history.length - 1].price
  return previous - item.price
}

export const GOLD_YUAN = 219
export const GOLD_BASE = 30000000

export function formatRmb(price) {
  const yuan = (price * GOLD_YUAN) / GOLD_BASE
  return `¥${yuan.toFixed(2)}`
}

export function specOf(name) {
  const text = name || ''
  const mark = text.lastIndexOf('·')
  if (mark < 0) return { series: text, level: null, spec: text }
  const series = text.slice(0, mark)
  const tail = text.slice(mark + 1)
  const matched = tail.match(/\d+/)
  const level = matched ? Number(matched[0]) : null
  return { series, level, spec: text }
}

export function quoteOf(item) {
  const prices = priceRecords(item).map((record) => record.price)
  const sum = prices.reduce((total, price) => total + price, 0)
  const avg = Math.round(sum / prices.length)
  return {
    high: Math.max(...prices),
    low: Math.min(...prices),
    avg,
    week: avg,
    samples: prices.length,
    stalls: 1,
  }
}
