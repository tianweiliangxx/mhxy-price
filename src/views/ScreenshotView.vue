<script setup>
import { ref } from 'vue'

const preview = ref('')
const note = ref('')
const saved = ref(false)

function onFile(file) {
  preview.value = URL.createObjectURL(file.raw)
  saved.value = false
  return false
}

function save() {
  saved.value = true
}
</script>

<template>
  <section>
    <h2>卖号截图助手</h2>
    <el-text type="info">把截图放进来做对照，方便整理角色、装备和召唤兽。图片只留在这台浏览器里。</el-text>
    <el-row :gutter="16" class="body">
      <el-col :xs="24" :md="12">
        <el-upload drag :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onFile">
          <div class="drop">把截图拖到这里，或点击选择</div>
        </el-upload>
        <el-image v-if="preview" class="preview" :src="preview" fit="contain" />
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>核对清单</template>
          <el-checkbox>角色面板</el-checkbox>
          <el-checkbox>装备</el-checkbox>
          <el-checkbox>召唤兽</el-checkbox>
          <el-checkbox>道具栏</el-checkbox>
          <el-input v-model="note" class="note" type="textarea" :rows="4" placeholder="补充说明" />
          <el-button type="primary" @click="save">保存备注</el-button>
          <el-text v-if="saved" type="success">备注已留在本页</el-text>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; }
.body { margin-top: 16px; }
.drop { padding: 28px 0; color: var(--el-text-color-secondary); }
.preview { width: 100%; max-height: 360px; margin-top: 12px; }
.note { margin: 12px 0; }
.el-checkbox { display: flex; margin-bottom: 8px; }
</style>
