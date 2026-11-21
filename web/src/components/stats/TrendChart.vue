<script setup lang="ts">
// 练习趋势折线（05 §5.8 第 33 条、07 §7.10）。
//
// ECharts 按需注册（07 §7.10）：只 `use` 这个页面真正用到的图元，
// 不整包 import 'echarts'——那会把地图、关系图等一并打进 `vendor-echarts`。
// 本组件由 `stats-overview.vue` **动态 import**，所以 chart 代码只在 /stats 加载。

import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

import type { StatTrendPointVO } from '@/types/api'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps<{ points: StatTrendPointVO[] }>()

const container = ref<HTMLDivElement | null>(null)
let chart: ReturnType<typeof echarts.init> | null = null

/**
 * 从 CSS 变量取色（index.scss 的 :root 映射），而不是写死色值：
 * 主色在 F14 调整过一次，写死的十六进制会被静默落下（与 element-override 同一条教训）。
 */
function token(name: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

function render(): void {
  if (!chart) return
  const primary = token('--rk-primary', '#2a72c2')
  const success = token('--rk-success', '#2a6e2e')
  const softBorder = token('--rk-soft-border', '#dbe7f4')
  const text3 = token('--rk-text-3', '#6b6e76')

  chart.setOption(
    {
      // Soft Focus（docs/36）：tooltip 用柔和蓝调边框 + 微阴影，不再是硬线框。
      tooltip: {
        trigger: 'axis',
        extraCssText: `box-shadow: 0 8px 24px rgba(42,114,194,.12); border: 1px solid ${softBorder}; border-radius: 12px;`,
      },
      legend: { data: ['作答数', '正确数'], right: 0, top: 0, textStyle: { color: text3 } },
      grid: { left: 4, right: 4, top: 36, bottom: 0, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        // 只显示 MM-DD：横轴是密集的日历格子，年份每格重复一次没有信息量
        data: props.points.map((p) => p.date.slice(5)),
        axisLine: { lineStyle: { color: softBorder } },
        axisLabel: { color: text3 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: softBorder } },
        axisLabel: { color: text3 },
      },
      series: [
        {
          name: '作答数',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          data: props.points.map((p) => p.answeredCount),
          itemStyle: { color: primary },
          lineStyle: { color: primary },
        },
        {
          name: '正确数',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          data: props.points.map((p) => p.correctCount),
          itemStyle: { color: success },
          lineStyle: { color: success },
        },
      ],
    },
    { notMerge: true },
  )
}

function resize(): void {
  chart?.resize()
}

onMounted(() => {
  if (!container.value) return
  chart = echarts.init(container.value)
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})

watch(() => props.points, render, { deep: true })
</script>

<template>
  <div ref="container" class="trend-chart" />
</template>

<style scoped lang="scss">
.trend-chart {
  width: 100%;
  height: 260px;
}
</style>
