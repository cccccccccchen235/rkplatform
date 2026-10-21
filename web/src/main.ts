import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
// KaTeX 的样式必须在全局引入一次：题干与解析里的公式由 MarkdownRenderer 用
// `katex.renderToString` 生成 HTML，缺了这份 CSS 会渲染成裸露的排版标签。
// 字体文件由 vite.config.ts 的 assetsInlineLimit 拆成独立文件，可被浏览器缓存复用。
import 'katex/dist/katex.min.css'
import './styles/index.scss'

const app = createApp(App)

// pinia 必须在 router 之前装：全局守卫里会 useUserStore()，
// 顺序反了会在第一次导航时报 "getActivePinia() was called but there was no active Pinia"。
app.use(createPinia())
app.use(router)

app.mount('#app')
