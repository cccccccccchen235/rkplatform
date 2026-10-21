<script setup lang="ts">
// 空白布局（07 §7.2）：登录、注册、模拟考答题页使用。
// 它存在的理由是"没有导航"——表单填到一半误点菜单离开，或者答题时点到别的页面，
// 都是不可接受的。所以这不是一个视觉选择，而是行为约束。
</script>

<template>
  <div class="blank-layout">
    <router-view />
  </div>
</template>

<style scoped lang="scss">
.blank-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $rk-bg-page;
}
</style>
