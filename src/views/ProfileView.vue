<script setup>
import { ref } from 'vue'
import { canRebind, profile, todayText } from '../lib/session.js'

const nickname = ref(profile.value.nickname)
const serverDraft = ref(profile.value.boundServer)
const characterName = ref('')

function saveNickname() {
  profile.value.nickname = nickname.value.trim()
}

function bindServer() {
  if (!canRebind()) return
  profile.value.boundServer = serverDraft.value.trim() || profile.value.boundServer
  profile.value.boundOn = todayText()
}

function addCharacter() {
  const name = characterName.value.trim()
  if (!name || profile.value.characters.length >= 10) return
  profile.value.characters.push({ name, server: profile.value.boundServer })
  characterName.value = ''
}
</script>

<template>
  <section>
    <h2>我的账号</h2>
    <el-card shadow="never">
      <el-form label-width="120px">
        <el-form-item label="昵称">
          <el-input v-model="nickname" placeholder="设置昵称" style="width: 240px" />
          <el-button class="gap" @click="saveNickname">保存</el-button>
        </el-form-item>
        <el-form-item label="当前绑定区服">{{ profile.boundServer }}</el-form-item>
        <el-form-item label="修改绑定">
          <el-input v-model="serverDraft" :disabled="!canRebind()" style="width: 240px" />
          <el-button class="gap" type="primary" :disabled="!canRebind()" @click="bindServer">修改绑定</el-button>
        </el-form-item>
        <el-form-item label="今日是否可修改">{{ canRebind() ? '今天还可以改一次' : '今天已修改过' }}</el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>会员与积分</template>
      <p>普通会员 · 积分余额 {{ profile.points }}</p>
      <el-space>
        <el-tag>一星会员 1个月 · 300 积分</el-tag>
        <el-tag>二星会员 1个月 · 800 积分</el-tag>
        <el-tag>三星会员 1个月 · 1500 积分</el-tag>
      </el-space>
    </el-card>
    <el-card shadow="never" class="block">
      <template #header>游戏角色 {{ profile.characters.length }}/10</template>
      <el-form :inline="true" @submit.prevent>
        <el-input v-model="characterName" placeholder="输入角色名" style="width: 220px" />
        <el-button type="primary" @click="addCharacter">创建</el-button>
      </el-form>
      <el-empty v-if="profile.characters.length === 0" description="当前区服还没有角色" />
      <el-table v-else :data="profile.characters">
        <el-table-column prop="name" label="角色" />
        <el-table-column prop="server" label="区服" />
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 12px; }
.block { margin-top: 12px; }
.gap { margin-left: 8px; }
</style>
