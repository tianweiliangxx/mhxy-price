<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const tab = ref('features')

const features = [
  { eyebrow: '实时采集', title: '收购摊位识别', text: '打开收购摊位后，把摊主、物品和价格整理成当前区服的收购物价。', points: ['适合扫描收购摊位密集区域', '识别成功后会出现摊位预览', '被遮挡的内容会尽量跳过或标记异常'], to: '/buy/today' },
  { eyebrow: '优先查价', title: '今日摊位物价', text: '按已经录入的摊位单价查价，重点看现在卖什么、卖多少。', points: ['适合卖货前快速比价', '保留来源和更新记录', '比只看一个数字更接近这次录入'], to: '/buy/today' },
  { eyebrow: '价格参考', title: '行情列表', text: '把录入记录整理成参考行情，查看价格和近期变化。', points: ['适合看参考价', '可按物品和分类查询', '支持拼音首字母'], to: '/buy/market' },
  { eyebrow: '同系列对比', title: '全区物价', text: '同一分类的单价放在一起，从高到低看。', points: ['适合快速判断这一类物价', '高价排在前面', '没有录入的物品不占位置'], to: '/buy/region' },
  { eyebrow: '定位辅助', title: '找摊位', text: '原站用识别时的画面和地图坐标帮你回到摊位。本地没有这些坐标。', points: ['适合收购摊位密集的区', '查看说明和查价入口', '只辅助人工查找，不自动寻路'], to: '/buy/locate' },
  { eyebrow: '摆摊功能', title: '摆摊物价查看', text: '查看摆摊侧录入的出售价格，卖货前做参考。', points: ['适合观察当前摆摊价格', '摆摊和收购入口分开', '可继续看全区分类'], to: '/stall/today' },
]
</script>

<template>
  <section>
    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane label="功能玩法" name="features" />
      <el-tab-pane label="收购使用教程" name="guide" />
    </el-tabs>

    <div v-if="tab === 'features'">
      <div class="intro">
        <el-tag effect="plain">功能玩法</el-tag>
        <h2>梦幻全区物价助手能做什么</h2>
        <el-text type="info">收购侧看参考价和降价，摆摊侧看已录入的出售单价。具体查价从左侧菜单进入。</el-text>
      </div>
      <div class="grid">
        <el-card v-for="item in features" :key="item.title" class="feature" shadow="hover" @click="router.push(item.to)">
          <el-text type="primary">{{ item.eyebrow }}</el-text>
          <h3>{{ item.title }}</h3>
          <p>{{ item.text }}</p>
          <ul>
            <li v-for="point in item.points" :key="point">{{ point }}</li>
          </ul>
        </el-card>
      </div>
      <el-row :gutter="12" class="notes">
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>安全边界</template>
            <ul>
              <li>不会自动购买、自动交易、自动摆摊或自动寻路。</li>
              <li>不会注入游戏客户端，也不会读取游戏内存。</li>
              <li>这是本地物价本，数据来自已录入的 prices.json。</li>
            </ul>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never">
            <template #header>页面分工</template>
            <ul>
              <li>功能玩法负责说明每个入口做什么。</li>
              <li>使用教程负责查价时先选区服，再搜物品。</li>
              <li>更新记录展开后能看到旧价和来源。</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-card v-else shadow="never">
      <h2>收购使用教程</h2>
      <el-steps direction="vertical" :active="4">
        <el-step title="选择区服" description="右上角切换正式服或畅玩服，并选一个区服。本地物价不按区服拆开，区服只用来标记你正在看的服。" />
        <el-step title="打开今日摊位物价" description="左侧收购高卖里进入今日摊位物价或行情列表。" />
        <el-step title="搜索物品" description="输入中文名称，或每个字的拼音首字母，例如 jll 找金柳露。" />
        <el-step title="对照全区物价和捡漏" description="全区物价按分类比价。捡漏集合只列出比上一次更便宜的物品。" />
      </el-steps>
    </el-card>
  </section>
</template>

<style scoped>
.intro { margin: 8px 0 16px; }
.intro h2, h2 { margin: 8px 0; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.feature { cursor: pointer; }
.feature h3 { margin: 8px 0; }
.feature p { margin: 0 0 8px; color: var(--el-text-color-regular); }
.feature ul, .notes ul { margin: 0; padding-left: 18px; color: var(--el-text-color-secondary); }
.notes { margin-top: 12px; }
</style>
