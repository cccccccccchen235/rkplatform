<script setup lang="ts">
/**
 * 顶栏品牌图标：透明底的书脊标记，与 public/favicon.svg 的蓝底版本分开。
 *
 * 设计口径（DESIGN.md）：
 * - 全站只有一个蓝 #2a72c2；
 * - 三根书脊等高、底部对齐在一条 1px 层板线上，高度不编码任何数值；
 * - 一根实心蓝 = 已印（大纲完整），两根灰虚线 = 未印；这个「实线/虚线」
 *   是首页书脊上唯一的图形编码，此处沿用同一语义。
 *
 * 不做 favicon 的简化版复用：16px 下虚线会糊，顶栏 28px 才有足够像素表达。
 */
const props = withDefaults(defineProps<{ size?: number }>(), { size: 28 })
</script>

<template>
  <svg
    :width="props.size"
    :height="props.size"
    viewBox="0 0 32 32"
    aria-hidden="true"
    xmlns="http://www.w3.org/2000/svg"
  >
    <rect x="7" y="10" width="5" height="15" fill="#2a72c2" />
    <rect
      x="13.5"
      y="10"
      width="5"
      height="15"
      fill="#ffffff"
      stroke="#dcdfe6"
      stroke-width="1"
      stroke-dasharray="2 2"
    />
    <rect
      x="20"
      y="10"
      width="5"
      height="15"
      fill="#ffffff"
      stroke="#dcdfe6"
      stroke-width="1"
      stroke-dasharray="2 2"
    />
    <path d="M5 26H27" fill="none" stroke="#6b6e76" stroke-width="1" />
  </svg>
</template>
