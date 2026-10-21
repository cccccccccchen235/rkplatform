// 路由与全局守卫（07 §7.3.3）。

import { createRouter, createWebHistory } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { cancelAll, setUnauthorizedHandler } from '@/utils/request'

import { adminRoutes } from './admin.routes'
import { portalRoutes } from './portal.routes'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // 管理端接在考生端之后。顺序本身不影响匹配——vue-router 4 按路径得分排序，
  // 静态段 `/admin/subjects` 的分高于 `/:pathMatch(.*)*` 的兜底，两者谁先声明都一样。
  // 这样排只是让"考生端 + 兜底"仍是一个连续的块，读起来是两个应用而不是四段。
  routes: [...portalRoutes, ...adminRoutes],
  // 切换路由回到顶部；浏览器的"后退"仍走原生行为（vue-router 会自动处理 popstate）
  scrollBehavior: () => ({ top: 0 }),
})

// 把「登录已过期」的处理逻辑注册给 axios 拦截器（见 utils/request.ts 的注释）。
// 放在路由模块里，是因为这段逻辑需要同时用到 store（清登录态）与 router（跳转），
// 而这两者都在这里，且 request.ts 依赖 router 而不是反过来。
setUnauthorizedHandler(async ({ message, fromAuthEntry }) => {
  useUserStore().clearAuth()
  ElMessage.warning(message)

  // 登录/注册接口自身返回 2005 时不再跳转：用户已经在登录页上，
  // 再 push 一次同名路由只会让表单看起来"闪了一下"。
  if (fromAuthEntry) return

  const current = router.currentRoute.value
  if (current.name !== 'login') {
    await router.push({ name: 'login', query: { redirect: current.fullPath } })
  }
})

/** 首次登录强制改密的提示只弹一次：否则用户每次点导航都会再挨一条同样的 toast。 */
let mustChangePasswordWarned = false

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 软考在线学习平台` : '软考在线学习平台'

  const user = useUserStore()

  // 1) 有 token 但没有用户信息（刷新页面、或刚在别的标签页登录）→ 向服务端确认
  //    这一步不能省：token 可能已过期、账号可能已被禁用，本地无从判断。
  //    失败即视为未登录，且必须清掉 token——否则会进入"有 token 但永远取不到用户信息"
  //    的状态，守卫每次导航都要打一次必然失败的 /users/me。
  if (user.token && !user.userInfo) {
    try {
      await user.fetchMe()
    } catch {
      user.clearAuth()
    }
  }

  // 2) 需要登录但未登录 → 跳登录并带上来源，登录后回到原处
  if (to.meta.requiresAuth && !user.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 3) 已登录还去登录/注册页 → 回 redirect 或首页。
  //    只接受站内相对路径：`redirect` 来自 URL 查询串，用户可以随便改，
  //    放行 `//evil.com` 会变成一个开放重定向。
  if (user.isLogin && (to.name === 'login' || to.name === 'register')) {
    const redirect = (to.query.redirect as string) || '/'
    return redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
  }

  // 3.5) 管理员重置过密码的账号必须先改密（06 §6.5：`mustChangePassword = true` 时
  //      前端凭该字段强制跳改密页——本项目的改密页就是个人中心）。
  //      放在第 3 步之后：那一支处理的是"已登录还去登录页"，语义不同，不能被这条抢走。
  if (user.isLogin && user.userInfo?.mustChangePassword && to.name !== 'profile') {
    if (!mustChangePasswordWarned) {
      mustChangePasswordWarned = true
      ElMessage.warning('管理员已重置你的密码，请先修改密码')
    }
    return { name: 'profile' }
  }

  // 4) 角色不足。**仅体验层拦截**，真正的校验在服务端（07 §7.8）：
  //    这里放行也不会让普通用户拿到管理端数据，只会让他看到一个空页面。
  if (to.meta.requiresAdmin && !user.isAdmin) {
    ElMessage.error('无权限访问该页面')
    return { name: 'home' }
  }

  return true
})

router.afterEach(() => {
  // 取消上一页仍在飞行的请求，避免"旧页数据渲染到新页"（07 §7.7）。
  // 放在 afterEach 而不是 beforeEach：beforeEach 可能被守卫重定向打断，
  // 在那种情况下取消请求会让新目标页自己的请求也被误杀。
  cancelAll()
})

router.onError((error) => {
  // 部署后旧 chunk 失效：用户停留在上一个版本打开的页面上，点链接时浏览器仍按
  // 老文件名去取，取到 404。这种情况只能刷新，且必须让用户知道发生了什么，
  // 否则表现是"点了没反应"。
  const message = String(error?.message ?? '')
  if (/Failed to fetch dynamically imported module|Importing a module script failed/i.test(message)) {
    ElMessage.warning('页面资源已更新，正在重新加载')
    window.location.reload()
  }
})

export default router
