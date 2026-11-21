<script setup lang="ts">
// 空白布局（07 §7.2）：登录、注册、模拟考答题页使用。
// 它存在的理由是"没有导航"——表单填到一半误点菜单离开，或者答题时点到别的页面，
// 都是不可接受的。所以这不是一个视觉选择，而是行为约束。
</script>

<template>
  <div class="blank-layout">
    <!-- 页面淡入：与 PortalLayout 同一套 rk-page 过渡（docs/36）。管理端不套用。 -->
    <router-view v-slot="{ Component }">
      <Transition name="rk-page" mode="out-in">
        <component :is="Component" />
      </Transition>
    </router-view>
  </div>
</template>

<style scoped lang="scss">
.blank-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  // 透明，透出 #app 的蓝白渐变；答题页自己铺满背景，登录/注册居中其上。
  background-color: transparent;
}
</style>
