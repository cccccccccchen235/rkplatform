<script setup lang="ts">
// 根组件只负责挂载路由出口。布局不在这一层决定——每个路由组自带布局组件
// （portal.routes.ts 的嵌套结构），这样"登录页没有导航"是路由表的结构事实，
// 而不是 App.vue 里的一句条件判断。
</script>

<template>
  <router-view />
</template>
