// 管理端路由（07 §7.3.2）。本轮是 views/admin/ 的**首次落地**，只登记三个页面。
//
// 07 §7.3.2 的表里还有 9 个管理端页面（数据概览、题目、审核、批次、卷面模板、
// 反馈、用户、配置）。这里不预写它们：每个都依赖尚未实现的后端接口，
// 提前登记要么让 `pnpm build` 因为找不到组件文件而失败，要么催生一批空组件，
// 于是路由表看起来"已经支持"了——那正是 portal.routes.ts 顶部同一条理由。
//
// **AdminLayout 用动态 import**：静默地把管理端布局塞进主包，会让
// 07 §7.12 的"未登录访问 /admin/* 时 Network 里没有 admin chunk 请求"变成假话。
// 布局文件在 layouts/ 下，不匹配 vite.config.ts 里 `/src/views/admin/` 那条
// manualChunks 规则，所以它自成一个按需加载的小 chunk，这正好是我们要的。

import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      {
        path: '',
        redirect: { name: 'admin-subjects' },
      },
      {
        path: 'subjects',
        name: 'admin-subjects',
        component: () => import('@/views/admin/subject-manage.vue'),
        meta: {
          title: '科目维护',
          requiresAuth: true,
          requiresAdmin: true,
          layout: 'admin',
          menuShort: '科目',
          menuOrder: 1,
        },
      },
      {
        path: 'outline',
        name: 'admin-outline',
        component: () => import('@/views/admin/outline-manage.vue'),
        meta: {
          title: '大纲维护',
          requiresAuth: true,
          requiresAdmin: true,
          layout: 'admin',
          menuShort: '大纲',
          menuOrder: 2,
        },
      },
      {
        path: 'knowledge-points',
        name: 'admin-kps',
        component: () => import('@/views/admin/knowledge-point-manage.vue'),
        meta: {
          title: '知识点维护',
          requiresAuth: true,
          requiresAdmin: true,
          layout: 'admin',
          menuShort: '标签',
          menuOrder: 3,
        },
      },
      {
        // 卷面模板配置（07 §7.3.2 的 /admin/paper-templates，UC-A06）。
        // 它排在内容维护三件套之后：先有科目/大纲/知识点，才有"给这个科目配一份卷面"。
        path: 'paper-templates',
        name: 'admin-paper-templates',
        component: () => import('@/views/admin/paper-template-config.vue'),
        meta: {
          title: '卷面模板',
          requiresAuth: true,
          requiresAdmin: true,
          layout: 'admin',
          menuShort: '卷面',
          menuOrder: 4,
        },
      },
    ],
  },
]
