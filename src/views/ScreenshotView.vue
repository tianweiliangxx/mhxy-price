<script setup>
import { ref } from 'vue'

const slots = ['戒指', '耳饰', '手镯', '佩饰', '头盔', '武器', '腰带', '项链', '衣服', '鞋子', '玉魄']
const placed = ref({})
const candidates = ref([])
const selected = ref(null)
const obs = ref({ host: '127.0.0.1', port: '4455', password: '' })

function addFiles(uploadFile) {
  const file = uploadFile.raw
  candidates.value.push({ id: `${Date.now()}-${file.name}`, name: file.name, url: URL.createObjectURL(file) })
}

function onDrop(slot) {
  if (selected.value == null) return
  placed.value = { ...placed.value, [slot]: candidates.value[selected.value] }
}

function exportList() {
  const manifest = {
    session: `local-seller-${Date.now()}`,
    slots: Object.fromEntries(slots.map((slot) => [slot, placed.value[slot]?.name || ''])),
    candidates: candidates.value.map((item) => item.name),
  }
  const blob = new Blob([JSON.stringify(manifest, null, 2)], { type: 'application/json' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '清单.json'
  link.click()
}
</script>

<template>
  <section>
    <div class="head">
      <div>
        <h2>卖号截图助手</h2>
        <el-text type="info">把详情图拖进槽位。打包先下载清单.json，图片仍留在本页。</el-text>
      </div>
      <el-space>
        <el-button type="primary">录屏采集</el-button>
        <el-button>OBS采集</el-button>
        <el-button>停止</el-button>
        <el-input v-model="obs.host" style="width: 110px" />
        <el-input v-model="obs.port" style="width: 80px" />
        <el-input v-model="obs.password" placeholder="OBS密码" style="width: 120px" />
      </el-space>
    </div>
    <el-space class="tools">
      <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="addFiles">
        <el-button>上传图片</el-button>
      </el-upload>
      <el-button @click="exportList">打包导出</el-button>
    </el-space>
    <div class="board">
      <div class="slots">
        <div v-for="slot in slots" :key="slot" class="slot" @dragover.prevent @drop="onDrop(slot)" @click="onDrop(slot)">
          <strong>{{ slot }}</strong>
          <img v-if="placed[slot]" :src="placed[slot].url" :alt="slot" />
          <span v-else>拖入详情图</span>
        </div>
      </div>
      <aside>
        <h3>候选详情</h3>
        <p>拖到槽位，或选中后点击槽位。</p>
        <div
          v-for="(item, index) in candidates"
          :key="item.id"
          class="candidate"
          draggable="true"
          :class="{ on: selected === index }"
          @dragstart="selected = index"
          @click="selected = index"
        >
          <img :src="item.url" :alt="item.name" />
          <span>{{ item.name }}</span>
        </div>
        <el-empty v-if="candidates.length === 0" description="开始上传装备详情图" :image-size="64" />
      </aside>
    </div>
  </section>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
h2 { margin: 0 0 4px; }
.tools { margin: 12px 0; }
.board { display: grid; grid-template-columns: 1fr 240px; gap: 12px; }
.slots { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 8px; }
.slot, .candidate { border: 1px dashed var(--el-border-color); border-radius: 8px; padding: 8px; background: #fff; }
.slot { min-height: 92px; }
.slot img, .candidate img { width: 100%; height: 64px; object-fit: contain; }
.candidate { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; cursor: grab; }
.candidate.on { border-color: var(--el-color-primary); }
.candidate img { width: 48px; }
@media (max-width: 800px) { .board { grid-template-columns: 1fr; } }
</style>
