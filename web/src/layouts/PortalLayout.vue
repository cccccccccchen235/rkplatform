<script setup lang="ts">
// 考生端布局（07 §7.2）：顶栏 + 科目切换。
//
// 顶栏只放"去哪儿"，不放"做什么"：具体动作（继续练习、开始模拟考）属于页面自己的
// 首屏内容。顶栏每多一个按钮，每个页面的可用宽度就少一点，而这个宽度正是
// 科目地图与题干最需要的。

import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import BrandMark from '@/components/common/BrandMark.vue'
import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const subjectStore = useSubjectStore()

// 科目切换下拉的数据源。未登录也会加载：科目列表是公开接口（05 §5.4）。
onMounted(() => {
  // 失败不在这里处理，也不该弹提示：这是一次**后台预取**，顶栏没有科目可选不该阻断整页。
  // `silent: true` 是必需的——不传的话，任何一次加载失败都会先由这里弹一条 toast，
  // 紧接着页面那块自己的错误面板再讲一遍同一件事。
  // 真正需要这份数据的页面会（用同一个去重后的请求）自己报错。
  subjectStore.fetchGrouped({}, false, { silent: true }).catch(() => undefined)
})

const currentSubjectName = computed(() => {
  const id = user.currentSubjectId
  if (!id) return '未选择科目'
  return subjectStore.flatList.find((s) => s.id === id)?.name ?? '未选择科目'
})

/** 当前高亮的导航项。`activeMenu` 让详情页也能高亮父级菜单（07 §7.3）。 */
const activeMenu = computed(() => (route.meta.activeMenu as string | undefined) ?? String(route.name ?? ''))

async function handleLogout(): Promise<void> {
  await user.logout()
  ElMessage.success('已退出登录')
  await router.push({ name: 'home' })
}

/**
 * 切换当前备考科目（05 §5.3 第 7 条）。
 *
 * 下拉是 `:model-value` 单向绑定（不是 v-model）：显示值始终来自 store，
 * 所以请求失败时它会自动弹回原来的科目，不会留下"界面上已经切了、库里没变"的假象。
 * 失败提示交给拦截器（不传 silent），这里只负责不让 rejected promise 逃逸。
 */
async function handleSubjectChange(subjectId: string): Promise<void> {
  try {
    await user.setCurrentSubject(subjectId)
    ElMessage.success('已切换当前备考科目')
  } catch {
    // 拦截器已就地提示（3001 科目不存在/已停用等）；显示值随 store 回弹。
  }
}
</script>

<template>
  <div class="portal-layout">
    <header class="portal-header">
      <div class="portal-header__inner rk-container">
        <router-link :to="{ name: 'home' }" class="portal-header__brand">
          <BrandMark :size="28" class="portal-header__brand-icon" />
          <span class="portal-header__brand-mark">软考</span>
          <span class="portal-header__brand-text">在线学习平台</span>
        </router-link>

        <nav class="portal-header__nav">
          <router-link :to="{ name: 'home' }" :class="{ 'is-active': activeMenu === 'home' }">首页</router-link>
          <router-link :to="{ name: 'subjects' }" :class="{ 'is-active': activeMenu === 'subjects' }">
            科目
          </router-link>
          <!--
            错题本与收藏只对已登录用户显示：它们读的是"我"的作答记录，未登录时
            点进去必然被守卫弹回登录页。渲染门槛与「退出」一致（都看 user.isLogin），
            不在模板里另造一套判断。
          -->
          <template v-if="user.isLogin">
            <router-link :to="{ name: 'wrong-book' }" :class="{ 'is-active': activeMenu === 'wrong-book' }">
              错题本
            </router-link>
            <router-link :to="{ name: 'favorites' }" :class="{ 'is-active': activeMenu === 'favorites' }">
              收藏
            </router-link>
            <router-link :to="{ name: 'exam-center' }" :class="{ 'is-active': activeMenu === 'exam' }">
              模拟考
            </router-link>
            <router-link :to="{ name: 'stats' }" :class="{ 'is-active': activeMenu === 'stats' }">
              统计
            </router-link>
            <router-link :to="{ name: 'profile' }" :class="{ 'is-active': activeMenu === 'profile' }">
              我的
            </router-link>
            <!--
              管理端入口只对管理员渲染（user.isAdmin 来自 /users/me 的 role）。
              前端隐藏只是体验层，不是安全边界：/admin/* 的路由守卫与后端接口
              已经各自校验 ADMIN，普通用户直接敲地址也进不去。
            -->
            <router-link v-if="user.isAdmin" :to="{ path: '/admin' }" class="portal-header__admin-link">
              管理端
            </router-link>
          </template>
        </nav>

        <div class="portal-header__actions">
          <el-select
            :model-value="user.currentSubjectId ?? ''"
            :placeholder="currentSubjectName"
            class="portal-header__subject"
            filterable
            :disabled="!user.isLogin"
            @change="handleSubjectChange"
          >
            <el-option
              v-for="item in subjectStore.flatList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>

          <template v-if="user.isLogin">
            <span class="portal-header__user">{{ user.userInfo?.nickname }}</span>
            <el-button text @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button text @click="router.push({ name: 'login' })">登录</el-button>
            <el-button type="primary" @click="router.push({ name: 'register' })">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped lang="scss">
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.portal-header {
  background-color: $rk-bg-card;
  border-bottom: 1px solid $rk-border;
  // 顶栏常驻：科目地图很长时，用户仍能随时切换科目或退出
  position: sticky;
  top: 0;
  z-index: 10;

  &__inner {
    height: 60px;
    display: flex;
    align-items: center;
    gap: $rk-space-xl;
  }

  &__brand {
    display: flex;
    align-items: baseline;
    gap: $rk-space-s;
    color: $rk-text-1;
    text-decoration: none;
    flex-shrink: 0;

    &:hover {
      text-decoration: none;
    }
  }

  &__brand-icon {
    align-self: center;
    flex-shrink: 0;
  }

  &__brand-mark {
    font-size: $rk-font-m;
    font-weight: 700;
    // 字标是浅底上的文字，走 4.5:1 底线。主色自 F14 起是 #2a72c2（4.90:1）。
    color: $rk-primary;
  }

  &__brand-text {
    font-size: $rk-font-m;
    font-weight: 600;
  }

  &__nav {
    display: flex;
    gap: $rk-space-xl;
    flex-shrink: 0;

    a {
      color: $rk-text-2;
      padding: $rk-space-xs 0;
      border-bottom: 2px solid transparent;

      // 导航是 14px 文字 + 2px 细线，两处都在页面底（#f5f7fa）上。
      // 主色自 F14 起是 #2a72c2（页面底 4.57:1），文字与细线都过线。
      &:hover {
        color: $rk-primary;
        text-decoration: none;
      }

      &.is-active {
        color: $rk-primary;
        border-bottom-color: $rk-primary;
      }
    }

    // 管理端入口用主色文字与考生端导航区分，但不加按钮框：
    // 顶栏只放「去哪儿」，主按钮留给页面自己的动作（DESIGN.md）。
    .portal-header__admin-link {
      color: $rk-primary;
    }
  }

  &__actions {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    flex-shrink: 0;
  }

  &__subject {
    width: 200px;
  }

  &__user {
    color: $rk-text-2;
  }
}

.portal-main {
  flex: 1;
}
</style>
