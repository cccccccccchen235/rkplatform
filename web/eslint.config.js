import js from '@eslint/js'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'

export default defineConfigWithVueTs(
  {
    name: 'rk/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
  },
  {
    name: 'rk/files-to-ignore',
    ignores: [
      '**/dist/**',
      '**/node_modules/**',
      // 这两个文件由 unplugin-* 生成，手改会在下次 dev/build 时被覆盖
      'src/types/auto-imports.d.ts',
      'src/types/components.d.ts',
    ],
  },

  js.configs.recommended,
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  // 关掉与 prettier 冲突的格式类规则：格式交给 prettier，lint 只管对错，
  // 否则同一处缩进会被两个工具来回改。
  skipFormatting,

  {
    name: 'rk/axios-boundary',
    // 07 §7.11：api/ 只放「一函数一接口」，页面不得直接碰 axios 或拼 URL。
    // **唯一豁免是 utils/**：request.ts 存在的意义就是"项目里唯一一处 axios"，
    // 把它也拦掉等于让规则自相矛盾。这条约定一旦破例，接口路径就会散落到各页面，
    // 05 文档改路径时没人能找全。
    files: ['src/**/*.{ts,vue}'],
    ignores: ['src/utils/**'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: 'axios',
              message: '只有 utils/request.ts 可以直接使用 axios，其余请从 @/api/ 导入接口函数（07 §7.11）。',
            },
          ],
        },
      ],
    },
  },

  {
    name: 'rk/views',
    files: ['src/views/**/*.vue'],
    rules: {
      // 页面文件按 07 §7.11 就该是 kebab-case 的单段名（home.vue、login.vue），
      // 而这条规则是为"可复用组件"设计的：它要避免组件名与将来的 HTML 元素撞名。
      // 页面是路由的落点，不会被当成标签复用，所以在这里关掉，而不是把文件改名。
      'vue/multi-word-component-names': 'off',
    },
  },

  {
    name: 'rk/components',
    files: ['src/components/**/*.vue', 'src/layouts/**/*.vue'],
    rules: {
      // 这两类会当标签用，保留多词要求
      'vue/multi-word-component-names': 'error',
    },
  },
)
