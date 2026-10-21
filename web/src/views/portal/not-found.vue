<script setup lang="ts">
// 404（07 §7.3.1 最后一行）。用空白布局：此时页面已经不在用户的预期路径上，
// 再顶一条导航会让人以为"我还在站内某个地方"。
</script>

<template>
  <el-result icon="warning" title="页面不存在" sub-title="地址可能输错了，或者这个页面还没做。">
    <template #extra>
      <el-button type="primary" @click="$router.replace({ name: 'home' })">回到首页</el-button>
    </template>
  </el-result>
</template>
