import 'vue-router'

/**
 * 路由 meta 的类型（07 §7.3 的 RouteMeta 约定）。
 *
 * 全部可选、未声明即按 false 处理：这样新增页面时不必逐条填写，
 * 而"忘记声明 requiresAuth"的后果是页面公开——所以凡是需要登录的页面，
 * 在 07 §7.3.1 的表里都是显式打勾的，评审时能逐行对照。
 */
declare module 'vue-router' {
  interface RouteMeta {
    /** document.title 的标题部分，最终渲染为 `${title} · 软考在线学习平台`。 */
    title?: string
    /** 需要登录。未登录时守卫重定向到 /login 并带 redirect。 */
    requiresAuth?: boolean
    /** 需要 ADMIN。**仅体验层拦截**，真正校验在服务端（07 §7.8）。 */
    requiresAdmin?: boolean
    /** 使用的布局。未声明时由路由表的嵌套结构决定。 */
    layout?: 'portal' | 'admin' | 'blank'
    /** 是否 keep-alive 缓存（练习、统计页开启）。 */
    keepAlive?: boolean
    /** 详情页高亮父菜单用（如科目详情高亮「科目」）。 */
    activeMenu?: string
    /**
     * 管理端菜单折叠（64px）时显示的 1~2 字缩写（07 §7.3）。
     *
     * 项目没有引入图标库（`@element-plus/icons-vue` 不在依赖表里），
     * 而 DESIGN.md 又明确"不引入装饰层"，所以折叠态用汉字缩写而不是
     * 现造一套图标——现造的图标没人认识，一个「科」字谁都认识。
     */
    menuShort?: string
    /** 管理端菜单排序（07 §7.3）。**不填则不进菜单**，`:id/edit` 这类页面靠这条隐藏。 */
    menuOrder?: number
  }
}
