<script setup lang="ts">
// Markdown + 公式渲染（07 §7.6）。
//
// 两道口子必须同时守住，缺一不可：
//   ① **不开启 `html: true`**。题干/解析来自离线流水线，但把它们当成可信 HTML 渲染是
//      一处没有收益的风险面：题目内容最终会进入 `v-html`，一旦中间环节被污染，
//      就等价于在页面里执行任意脚本（07 §7.8 的 XSS 缓解手段之一）。
//   ② 公式由**自己的小插件**渲染，不引入 `markdown-it-katex`（不在依赖表里）。
//      实现方式是接管 `text` token：先按 `$$...$$` / `$...$` 切开，数学段交
//      `katex.renderToString`，其余段走 markdown-it 自带的 `escapeHtml`。
//
// 渲染结果是模块级单例：markdown-it 实例与规则只建一次，组件每次渲染只跑 `render`。

import katex from 'katex'
import MarkdownIt from 'markdown-it'
import { computed } from 'vue'

/** 行内 `$...$`（不允许跨行）与块级 `$$...$$`（允许跨行）。块级分支写在前面，先匹配更长的定界符。 */
const MATH_PATTERN = /\$\$([\s\S]+?)\$\$|\$([^$\n]+?)\$/g

/**
 * 把一段纯文本里的公式替换成 KaTeX 的 HTML，其余文本转义。
 *
 * 公式语法错误时**原样输出源码**（`throwOnError: false` + try/catch 双保险）：
 * 一处坏公式不该让整段题干变成空白。
 */
function renderTextWithMath(raw: string, escapeHtml: (text: string) => string): string {
  let html = ''
  let lastIndex = 0
  MATH_PATTERN.lastIndex = 0

  let match = MATH_PATTERN.exec(raw)
  while (match !== null) {
    html += escapeHtml(raw.slice(lastIndex, match.index))
    const displayMode = match[1] !== undefined
    const tex = displayMode ? match[1] : match[2]
    try {
      html += katex.renderToString(tex ?? '', { displayMode, throwOnError: false })
    } catch {
      html += escapeHtml(match[0])
    }
    lastIndex = match.index + match[0].length
    match = MATH_PATTERN.exec(raw)
  }

  html += escapeHtml(raw.slice(lastIndex))
  return html
}

const md = new MarkdownIt({
  html: false,
  linkify: false,
  // 题干里的换行按 Markdown 语义处理（单个换行折叠成空格）；选项与分段都靠空行表达
  breaks: false,
})

// 接管文本 token。放在这里而不是每次渲染都改：规则是实例级的一次性配置。
md.renderer.rules.text = (tokens, idx) => {
  const token = tokens[idx]
  if (!token) return ''
  return renderTextWithMath(token.content, md.utils.escapeHtml)
}

const props = withDefaults(defineProps<{ content: string; inline?: boolean }>(), {
  inline: false,
})

/**
 * 两种渲染模式。
 *
 * {@code inline} 是给**选项**用的：选项内容在库里同样带 `$...$` 定界符
 * （如 `$169$`），不渲染就会把定界符原样显示出来。而 `md.render()` 会把内容包成
 * `<p>` 并带上段落外边距——放进 `el-radio` 的标签里会把单行选项撑成两行高。
 * `renderInline` 走同一套 inline 规则（含上面接管的 `text` 规则），因此公式照渲染，
 * 只是不产生块级包裹。
 */
const html = computed(() =>
  props.inline ? md.renderInline(props.content ?? '') : md.render(props.content ?? ''),
)
</script>

<template>
  <span v-if="inline" class="markdown-renderer markdown-renderer--inline" v-html="html" />
  <div v-else class="markdown-renderer" v-html="html" />
</template>

<style scoped lang="scss">
// v-html 生成的内容没有 scoped 属性，所有对它的样式都要走 :deep()。
.markdown-renderer {
  color: $rk-text-1;
  line-height: $rk-line-height;
  // 长公式/长英文串不能把卡片撑破
  word-break: break-word;

  :deep(p) {
    margin: 0 0 $rk-space-m;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(img) {
    max-width: 100%;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-m;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0 0 $rk-space-m;
    padding-left: 1.5em;
  }

  :deep(pre) {
    margin: 0 0 $rk-space-m;
    padding: $rk-space-m;
    background-color: $rk-bg-page;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-m;
    overflow-x: auto;
  }

  :deep(code) {
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
    font-size: $rk-font-xs;
  }

  // 块级公式横向超宽时允许滚动，而不是溢出到卡片外
  :deep(.katex-display) {
    overflow-x: auto;
    overflow-y: hidden;
  }

  // 行内模式（选项）：不产生块级包裹，段落外边距一并去掉
  &--inline {
    display: inline;

    :deep(p) {
      display: inline;
      margin: 0;
    }
  }
}
</style>