<template>
  <section>
    <h2>采集记录</h2>
    <el-text type="info">每次录屏或 OBS 采集会形成一条会话。现在还没有接上识别，所以列表是空的。</el-text>
    <el-table class="table" :data="[]">
      <el-table-column prop="startedAt" label="开始时间" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="stalls" label="摊位" />
      <el-table-column prop="records" label="记录" />
      <el-table-column prop="confirmed" label="确认" />
      <el-table-column prop="id" label="会话ID" />
    </el-table>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; }
.table { margin-top: 12px; }
</style>
