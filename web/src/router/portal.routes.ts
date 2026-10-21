// 考生端路由（07 §7.3.1）。
//
// 只登记**页面文件已经存在**的路由。07 §7.3.1 表里还有模拟考中心、统计、个人中心等，
// 它们各自依赖尚未实现的后端接口，随页面开工时逐条加进来——提前登记会让
// `pnpm build` 因为找不到组件文件而失败，或者更糟：为了让它不失败而建一个空组件，
// 于是路由表看起来"已经支持"了。

import type { RouteRecordRaw } from 'vue-router'

import BlankLayout from '@/layouts/BlankLayout.vue'
import PortalLayout from '@/layouts/PortalLayout.vue'

export const portalRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: PortalLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/portal/home.vue'),
        meta: { title: '首页', layout: 'portal' },
      },
      {
        path: 'subjects',
        name: 'subjects',
        component: () => import('@/views/portal/subject-list.vue'),
        meta: { title: '科目', layout: 'portal' },
      },
      {
        // 详情与大纲拆成两条路由、两个页面（07 §7.3.1）：详情回答"考什么"，
        // 大纲回答"学什么"。合成一条会让 /subjects/2 一次拉两份数据，
        // 而用户在列表页点进去时十有八九只想看卷面结构。
        path: 'subjects/:id',
        name: 'subject-detail',
        component: () => import('@/views/portal/subject-detail.vue'),
        // activeMenu 让详情页高亮顶栏的「科目」（07 §7.3 的 meta 约定）
        meta: { title: '科目详情', layout: 'portal', activeMenu: 'subjects' },
      },
      {
        path: 'subjects/:id/outline',
        name: 'subject-outline',
        component: () => import('@/views/portal/subject-outline.vue'),
        meta: { title: '章节大纲', layout: 'portal', activeMenu: 'subjects' },
      },
      {
        path: 'wrong-book',
        name: 'wrong-book',
        component: () => import('@/views/portal/wrong-book.vue'),
        // 错题本与收藏都必须登录：它们读的是"我"的作答记录（05 §5.7 权限列 USER）
        meta: { title: '错题本', requiresAuth: true, layout: 'portal', activeMenu: 'wrong-book' },
      },
      {
        path: 'favorites',
        name: 'favorites',
        component: () => import('@/views/portal/favorites.vue'),
        meta: { title: '收藏', requiresAuth: true, layout: 'portal', activeMenu: 'favorites' },
      },
      {
        // 学习统计（07 §7.3.1）。必须登录：四个接口都是 USER 权限（05 §5.8）。
        // keepAlive：区间切换与科目切换都在页内完成，缓存页面状态省一次重排；
        // 数据新鲜度由页面自己在 onMounted 拉取保证，不依赖 keepAlive 的缓存。
        path: 'stats',
        name: 'stats',
        component: () => import('@/views/portal/stats-overview.vue'),
        meta: { title: '学习统计', requiresAuth: true, layout: 'portal', activeMenu: 'stats', keepAlive: true },
      },
      {
        // 模拟考中心（07 §7.3.1）。十个接口都是 USER 权限（05 §5.6），必须登录。
        path: 'exam',
        name: 'exam-center',
        component: () => import('@/views/portal/exam-center.vue'),
        meta: { title: '模拟考', requiresAuth: true, layout: 'portal', activeMenu: 'exam' },
      },
      {
        // 成绩详情（07 §7.3.1）。放在中心页之后，`/exam/:sessionId/result` 的静态段
        // 评分高于 `/exam/:sessionId`，两条不会互相遮蔽。
        path: 'exam/:sessionId/result',
        name: 'exam-result',
        component: () => import('@/views/portal/exam-result.vue'),
        meta: { title: '模拟考成绩', requiresAuth: true, layout: 'portal', activeMenu: 'exam' },
      },
      {
        // 个人中心（07 §7.3.1 的 M1）：昵称 / 密码 / 当前科目 / 退出。
        // 必须登录——四个接口都是 USER 权限（05 §5.3 第 4~7 条）。
        // 它也是 `mustChangePassword = true` 时守卫强制跳转的落点（06 §6.5）。
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/portal/profile.vue'),
        meta: { title: '个人中心', requiresAuth: true, layout: 'portal', activeMenu: 'profile' },
      },
    ],
  },
  {
    path: '/',
    component: BlankLayout,
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/portal/login.vue'),
        // 登录/注册用空白布局：没有导航就没有"误点走开、表单白填"（07 §7.2）
        meta: { title: '登录', layout: 'blank' },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/portal/register.vue'),
        meta: { title: '注册', layout: 'blank' },
      },
      {
        // 练习答题页用空白布局：答题途中误点导航的代价太大（07 §7.2）。
        //
        // **刻意不写 requiresAuth**：仅 `mode = SAMPLE`（游客样例题）免登录，其余模式
        // 需登录，这是一条按 `:mode` 动态判定的规则，静态 meta 表达不了。保守做法是
        // 交给服务端——非 SAMPLE 模式在未登录时返回 `2005`，拦截器随即清登录态并跳转
        // 登录页（07 §5.1/§7.7），页面再把 `2005` 就地讲成"该模式需要登录"。
        path: 'practice/:mode',
        name: 'practice',
        component: () => import('@/views/portal/practice-session.vue'),
        meta: { title: '练习', layout: 'blank', keepAlive: true },
      },
      {
        // 模拟考答题页（07 §7.3.1）。空白布局 + 必须登录；答题页自己有离开拦截，
        // 计时不因页面切换而暂停（07 §7.5.1 专项）。
        path: 'exam/:sessionId',
        name: 'exam-session',
        component: () => import('@/views/portal/exam-session.vue'),
        meta: { title: '模拟考答题', requiresAuth: true, layout: 'blank' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/portal/not-found.vue'),
    meta: { title: '页面不存在', layout: 'blank' },
  },
]
