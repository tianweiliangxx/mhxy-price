<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { serverGroup, serverName, servers } from './lib/session.js'

const route = useRoute()
const router = useRouter()
const serverQuery = ref('')
const noticeOpen = ref(false)
const feedbackOpen = ref(false)
const notifyOpen = ref(false)
const loginOpen = ref(false)
const feedback = ref('')
const feedbackSent = ref(false)

const active = computed(() => route.path)

const matchedServers = computed(() => {
  const text = serverQuery.value.trim()
  if (!text) return servers
  return servers.filter((name) => name.includes(text))
})

function pickServer(name) {
  serverName.value = name
  serverQuery.value = ''
}

function sendFeedback() {
  feedbackSent.value = true
}
</script>

<template>
  <el-container class="shell">
    <el-aside width="248px" class="sider">
      <div class="brand">
        <div class="logo">价</div>
        <div>
          <strong>梦幻全区物价助手</strong>
          <p>收购高卖 / 摊位捡漏 / 仓库管理</p>
        </div>
      </div>
      <el-menu
        :default-active="active"
        :default-openeds="['buy', 'stall']"
        background-color="#0f172a"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-sub-menu index="buy">
          <template #title>收购高卖</template>
          <el-menu-item index="/buy/today">今日摊位物价</el-menu-item>
          <el-menu-item index="/buy/region">全区物价</el-menu-item>
          <el-menu-item index="/buy/market">行情列表</el-menu-item>
          <el-menu-item index="/buy/bargains">捡漏集合</el-menu-item>
          <el-menu-item index="/buy/trends">趋势报表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="stall">
          <template #title>摆摊</template>
          <el-menu-item index="/stall/data">摊位数据</el-menu-item>
          <el-menu-item index="/stall/today">今天摊位物价</el-menu-item>
          <el-menu-item index="/stall/region">全区物价</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/screenshot">卖号截图助手</el-menu-item>
        <el-menu-item index="/warehouse">仓库管理</el-menu-item>
      </el-menu>
      <div class="sider-foot">
        <el-text class="who">当前用户</el-text>
        <el-button type="primary" class="login" @click="loginOpen = true">微信小程序扫码登录</el-button>
        <p>本地物价可以直接查。扫码登录是原站采集用的，这里不会跳走。</p>
      </div>
    </el-aside>

    <el-container>
      <el-header class="top" height="64px">
        <div class="top-links">
          <el-button link @click="router.push('/')">梦幻工具箱</el-button>
          <el-button link @click="noticeOpen = true">更新公告</el-button>
          <el-button link @click="feedbackOpen = true">反馈</el-button>
          <el-button link @click="notifyOpen = true">通知</el-button>
        </div>
        <div class="top-tools">
          <el-radio-group v-model="serverGroup" size="small">
            <el-radio-button value="正式服">正式服</el-radio-button>
            <el-radio-button value="畅玩服">畅玩服</el-radio-button>
          </el-radio-group>
          <el-dropdown trigger="click" @command="pickServer">
            <el-input v-model="serverQuery" :placeholder="`搜索区服，例如：${serverName}`" :prefix-icon="Search" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="name in matchedServers" :key="name" :command="name">{{ name }}</el-dropdown-item>
                <el-dropdown-item v-if="matchedServers.length === 0" disabled>没有这个区服</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="noticeOpen" title="更新公告" width="520px">
      <el-tag type="success" effect="plain">正式发布</el-tag>
      <h3>本地物价助手</h3>
      <p>查价、全区分类、捡漏和更新记录使用已经录入的 prices.json。原站的微信扫码采集没有接进来。</p>
      <template #footer>
        <el-button type="primary" @click="noticeOpen = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="feedbackOpen" title="反馈" width="480px">
      <el-input v-model="feedback" type="textarea" :rows="4" placeholder="价格不对、缺图标，或页面哪里别扭" />
      <el-text v-if="feedbackSent" type="success">已留在本页，不会发送到原站。</el-text>
      <template #footer>
        <el-button @click="feedbackOpen = false">取消</el-button>
        <el-button type="primary" @click="sendFeedback">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="notifyOpen" title="通知" width="480px">
      <el-empty v-if="false" />
      <el-text>当前区服：{{ serverGroup }} · {{ serverName }}。物价来自本地录入。</el-text>
    </el-dialog>

    <el-dialog v-model="loginOpen" title="微信小程序扫码登录" width="420px">
      <p>原站登录后才能看它自己的采集数据。这个副本直接使用本地物价，不需要扫码。</p>
      <template #footer>
        <el-button type="primary" @click="loginOpen = false">知道了</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  background: #f3f6fb;
}

.sider {
  background: #0f172a;
  color: #cbd5e1;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 16px 14px 8px;
}

.brand strong {
  display: block;
  color: #fff;
  font-size: 14px;
}

.brand p {
  margin: 2px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #1677ff;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
}

.sider :deep(.el-menu) {
  border-right: 0;
  flex: 1;
}

.sider :deep(.el-menu-item.is-active) {
  background: #1677ff;
}

.sider-foot {
  padding: 12px 14px 16px;
}

.who {
  color: #94a3b8;
}

.login {
  width: 100%;
  margin: 8px 0;
}

.sider-foot p {
  margin: 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.top-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-tools :deep(.el-input) {
  width: 240px;
}

.main {
  padding: 20px;
}

@media (max-width: 800px) {
  .sider {
    display: none;
  }
}
</style>
