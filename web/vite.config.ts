import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'
import eslint from 'vite-plugin-eslint2'

const BACKEND_ORIGIN = 'http://localhost:8080'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入（07 §7.2.1）。整包 import ElementPlus 会让首屏多背
    // 一份用不到的组件；AutoImport 负责 ElMessage/ElMessageBox 这类"函数式"API，
    // Components 负责 <el-table> 这类"标签式"组件，两者缺一不可。
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/components.d.ts',
    }),
    // lint 在 dev server 里就地报错。用 vite-plugin-eslint2 而不是文档里的
    // vite-plugin-eslint：后者停在 ESLint 8 的配置格式上，本项目用 ESLint 9 的
    // flat config（eslint.config.js），原插件读不了。行为一致，只是换了维护中的实现。
    eslint({ lintOnStart: false, cache: true }),
  ],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  // 本机 WorkBuddy 对 node_modules/.vite/deps_temp_* 的批量删除有安全守卫：
  // 运行期按需发现新依赖会触发重优化并删临时目录，导致 dev 进程退出。
  // 因此关闭自动发现，启动时一次性预打包全部裸包；Element Plus 组件的子路径样式
  // 仍由 unplugin-vue-components 生成，直接走 node_modules 的 ESM，不参与预打包。
  optimizeDeps: {
    noDiscovery: true,
    include: [
      'vue',
      'vue-router',
      'pinia',
      'axios',
      'element-plus',
      'katex',
      'markdown-it',
      'echarts/core',
      'echarts/charts',
      'echarts/components',
      'echarts/renderers',
    ],
  },

  server: {
    port: 5173,
    proxy: {
      // changeOrigin 让后端看到的 Host 是它自己，避免将来加域名校验时本地联调失效
      '/api': { target: BACKEND_ORIGIN, changeOrigin: true },
      // 题干图片由后端静态托管（05 §5.10 第 51 项）。不代理这条，
      // 开发期 Markdown 里的 <img src="/uploads/..."> 会 404，而问题看起来像"后端没存图"。
      '/uploads': { target: BACKEND_ORIGIN, changeOrigin: true },
    },
  },

  css: {
    preprocessorOptions: {
      // 每个 .scss 编译单元自动拿到设计变量，组件里不必逐个 @use。
      // 副作用：变量文件里不能写任何会产出 CSS 的规则，否则每个组件都会重复一份。
      scss: {
        additionalData: '@use "@/styles/variables.scss" as *;',
      },
    },
  },

  build: {
    // KaTeX 的 woff2 字体一律输出成独立文件，不做 dataURL 内联（07 §7.2.1）：
    // 内联后字体会被塞进 CSS/JS，体积翻倍且无法被浏览器缓存复用。
    assetsInlineLimit: (filePath) => {
      if (/\.(woff2?|ttf|eot)$/i.test(filePath)) return false
      return undefined
    },
    rollupOptions: {
      output: {
        manualChunks(id) {
          // ⚠️ **这里曾经有一条 `if (id.includes('/src/views/admin/')) return 'admin'`，
          // 本轮删掉了。** 它不但没把管理端挡在首屏之外，反而把管理端**拽进了首屏**：
          // 被点名进 `admin` 这份手工 chunk 之后，rollup 把它依赖的公共模块
          // （`utils/request.ts`、`utils/storage.ts`——路由与 store 也都在用）
          // 一并并进了同一份产物，于是 `index-*.js` 必须**静态** import `admin-*.js`
          // 才拿得到那两个模块 → `index.html` 里出现 `admin` 的 modulepreload，
          // 首屏实测多下载 11.53 KB（JS+CSS），且 docs/07 §7.12「管理端代码不进首屏」
          // 这条验收项当场为假。
          //
          // 删掉之后交由 rollup 按动态 import 自然分包：三个管理页各自成 chunk
          // （`subject-manage` / `outline-manage` / `knowledge-point-manage`），
          // 只有真正进管理端时才下载。`AdminLayout.vue` 同理。第一屏的
          // modulepreload 只剩下面两条 vendor。
          //
          // 教训：手工 chunk 的语义是"这些模块必须在一起"，不是"这些模块要隔离"。
          // 想让一段代码不进首屏，正确做法是让它只被 `() => import()` 引用。
          if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts'
          if (id.includes('katex')) return 'vendor-katex'
          if (id.includes('element-plus')) return 'vendor-element'
          if (id.includes('markdown-it')) return 'vendor-markdown'
          if (id.includes('node_modules')) return 'vendor'
          return undefined
        },
      },
    },
  },
})
