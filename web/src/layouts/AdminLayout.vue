<script setup lang="ts">
// 管理端布局（07 §7.2）：侧栏 + 面包屑。
//
// 它和 PortalLayout 是两种工作：考生端是"浏览与练习"，一屏一件事；
// 管理端是"维护数据"，一屏里要在三张表之间来回跳，所以左侧常驻一个菜单，
// 而不是把导航塞进顶栏。
//
// 视觉上仍是同一套系统（DESIGN.md）：白底、1px 线分层、零投影、一个蓝。
// 侧栏与内容区之间那条竖线就是"层板"，不需要再加阴影或卡片包裹。

import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const collapsed = ref(false)

/**
 * 菜单由**路由表**生成，不是另写一份常量（07 §7.8）。
 *
 * 判据是 `menuShort` 是否声明：`:id/edit` 那类页面不该出现在侧栏里，
 * 而"出现在侧栏"这件事需要在路由上有个开关，`menuShort` 顺便兼任了它。
 *
 * 排序走 `menuOrder` 而不是数组下标：`getRoutes()` 返回的是扁平化后的
 * 匹配器顺序，嵌套子路由的先后不保证与声明顺序一致。
 */
const menuItems = computed(() =>
  router
    .getRoutes()
    .filter((record) => record.path.startsWith('/admin/') && record.meta.menuShort)
    .map((record) => ({
      path: record.path,
      title: record.meta.title ?? record.path,
      short: record.meta.menuShort as string,
      order: record.meta.menuOrder ?? Number.MAX_SAFE_INTEGER,
    }))
    .sort((a, b) => a.order - b.order),
)

/** 当前高亮项。`el-menu` 的 `default-active` 要的是菜单项的 index，即完整路径。 */
const activeMenu = computed(() => route.path)

/** 面包屑：管理端 → 当前页。布局这一层是固定的，不假装它是可点的一级目录。 */
const currentTitle = computed(() => route.meta.title ?? '')

async function handleLogout(): Promise<void> {
  await user.logout()
  ElMessage.success('已退出登录')
  await router.push({ name: 'home' })
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sider" :class="{ 'is-collapsed': collapsed }">
      <router-link :to="{ name: 'home' }" class="admin-sider__brand">
        <span class="admin-sider__brand-mark">软考</span>
        <span v-show="!collapsed" class="admin-sider__brand-text">管理后台</span>
      </router-link>

      <el-menu :default-active="activeMenu" :collapse="collapsed" :collapse-transition="false" router class="admin-sider__menu">
        <!--
          默认插槽放缩写、`#title` 放全名：这是 el-menu 折叠态的既定用法
          （折叠时默认插槽仍显示、`#title` 变 tooltip）。缩写用 CSS 控制显隐，
          不做 v-if——`display: none` 已经把它从无障碍树里摘掉，不需要再包一层。
        -->
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span class="admin-sider__item-short">{{ item.short }}</span>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>

      <button type="button" class="admin-sider__toggle" @click="collapsed = !collapsed">
        {{ collapsed ? '展开菜单' : '收起菜单' }}
      </button>
    </aside>

    <div class="admin-body">
      <header class="admin-topbar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ name: 'home' }">考生端</el-breadcrumb-item>
          <el-breadcrumb-item>管理后台</el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="admin-topbar__actions">
          <span class="admin-topbar__user">{{ user.userInfo?.nickname }}</span>
          <el-button text @click="router.push({ name: 'subjects' })">返回考生端</el-button>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
  display: flex;
  background-color: $rk-bg-page;
}

.admin-sider {
  width: $rk-sider-width;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background-color: $rk-bg-card;
  // 侧栏与内容区之间只有这一条线。DESIGN.md 的 The Board-Not-Shadow Rule：
  // 需要分层时用线，不用投影。
  border-right: 1px solid $rk-border;
  position: sticky;
  top: 0;
  height: 100vh;

  &.is-collapsed {
    width: $rk-sider-collapsed-width;
  }

  &__brand {
    height: 60px;
    flex-shrink: 0;
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: $rk-space-s;
    border-bottom: 1px solid $rk-border;
    color: $rk-text-1;
    text-decoration: none;

    &:hover {
      text-decoration: none;
    }
  }

  &__brand-mark {
    font-size: $rk-font-m;
    font-weight: 700;
    color: $rk-primary;
  }

  &__brand-text {
    font-size: $rk-font-s;
    font-weight: 600;
  }

  &__menu {
    flex: 1;
    border-right: none;

    // 折叠态：菜单项只剩 menuShort 那个字，靠 flex 居中而不是靠 padding 猜位置
    :deep(.el-menu-item) {
      padding-left: $rk-space-l;
      justify-content: flex-start;
    }
  }

  &__item-short {
    display: none;
  }

  &.is-collapsed &__menu :deep(.el-menu-item) {
    justify-content: center;
    padding-left: 0;
  }

  &.is-collapsed &__item-short {
    display: inline;
  }

  &__toggle {
    height: 44px;
    flex-shrink: 0;
    border: none;
    border-top: 1px solid $rk-border;
    background-color: transparent;
    color: $rk-text-2;
    font-size: $rk-font-xs;
    cursor: pointer;

    &:hover {
      color: $rk-primary;
    }
  }
}

.admin-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-topbar {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $rk-space-l;
  padding: 0 $rk-space-xl;
  background-color: $rk-bg-card;
  border-bottom: 1px solid $rk-border;
  position: sticky;
  top: 0;
  z-index: 10;

  &__actions {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    flex-shrink: 0;
  }

  &__user {
    color: $rk-text-2;
  }
}

.admin-main {
  flex: 1;
  padding: $rk-space-xl;
  // 管理端表格很宽，刻意不套 .rk-container 的 1200px 上限：
  // 在 1280 基准宽下，内容区用满才不至于让表格横向滚动。
  min-width: 0;
}
</style>
