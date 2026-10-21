<script setup lang="ts">
// 学习统计（07 §7.5.1，路由 `/stats`）。四块：总览卡、趋势折线、章节掌握度树、薄弱知识点。
//
// 三个刻意的取舍：
//   ① `subjectId` 由本页自己选（并回落到列表第一个科目）：顶栏的科目切换调用的是
//      05 §5.3 第 7 条，那个接口尚未实现，`currentSubjectId` 通常是空的。
//   ② 章节掌握度用「缩进 + 横向条」表达，而不是纯柱状图：10 §10.8 的验收项要求
//      "树形展示、父节点为其子节点题量加权"，而 07 §7.5.1 写的是"柱状"。
//      缩进保留树形层级，条长表达数值，同时满足两处（取舍记在 docs/28）。
//   ③ 图表组件**动态 import**（07 §7.10）：`vendor-echarts` 只在 /stats 加载。

import { computed, defineAsyncComponent, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchStatOutline, fetchStatOverview, fetchStatTrend, fetchStatWeakKp } from '@/api/stats'
import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'
import type { StatOutlineNodeVO, StatOverviewVO, StatTrendPointVO, WeakKpVO } from '@/types/api'
import { formatPercent } from '@/utils/format'
import type { RequestError } from '@/utils/request'

const TrendChart = defineAsyncComponent(() => import('@/components/stats/TrendChart.vue'))

const route = useRoute()
const router = useRouter()
const subjectStore = useSubjectStore()
const user = useUserStore()

const subjectId = ref(
  typeof route.query.subjectId === 'string' && route.query.subjectId !== ''
    ? route.query.subjectId
    : (user.currentSubjectId ?? ''),
)

/** 趋势区间。10 §10.8 的验收项要求 7/30/90 切换正确。 */
const DAY_OPTIONS = [7, 30, 90]
const days = ref(30)

const overview = ref<StatOverviewVO | null>(null)
const trend = ref<StatTrendPointVO[]>([])
const outline = ref<StatOutlineNodeVO[]>([])
const weakKp = ref<WeakKpVO[]>([])

const loading = ref(true)
const loadError = ref('')
/** 5001（无科目/无统计）走空态，不当作"加载失败"。 */
const noStatistics = ref(false)

interface OutlineRow {
  node: StatOutlineNodeVO
  depth: number
}

/** 展平成带层级的行：缩进由 depth 决定，避免在模板里递归组件。 */
function flatten(nodes: StatOutlineNodeVO[], depth = 0): OutlineRow[] {
  return nodes.flatMap((node) => [{ node, depth }, ...flatten(node.children ?? [], depth + 1)])
}

const outlineRows = computed(() => flatten(outline.value))

/** 掌握度阈值：与 05 §5.5 第 12 条 `onlyUnmastered` 的 0.6 是同一个数。 */
const MASTERY_THRESHOLD = 0.6

function isWeak(mastery: number): boolean {
  return mastery < MASTERY_THRESHOLD
}

function resetStates(): void {
  overview.value = null
  trend.value = []
  outline.value = []
  weakKp.value = []
}

async function load(): Promise<void> {
  if (!subjectId.value) {
    resetStates()
    loading.value = false
    return
  }

  loading.value = true
  loadError.value = ''
  noStatistics.value = false
  try {
    const [ov, tr, ol, wk] = await Promise.all([
      fetchStatOverview({ subjectId: subjectId.value }, { silent: true }),
      fetchStatTrend({ subjectId: subjectId.value, days: days.value }, { silent: true }),
      fetchStatOutline({ subjectId: subjectId.value }, { silent: true }),
      fetchStatWeakKp({ subjectId: subjectId.value, limit: 10 }, { silent: true }),
    ])
    overview.value = ov
    trend.value = tr ?? []
    outline.value = ol ?? []
    weakKp.value = wk ?? []
  } catch (error) {
    const err = error as RequestError
    resetStates()
    if (err.code === 5001) {
      noStatistics.value = true
    } else {
      loadError.value = err.message
    }
  } finally {
    loading.value = false
  }
}

/** 只重拉趋势：切换区间不影响总览/章节/薄弱点，没必要整页转圈。 */
let trendSeq = 0
async function reloadTrend(): Promise<void> {
  if (!subjectId.value) return
  const seq = ++trendSeq
  try {
    const data = await fetchStatTrend({ subjectId: subjectId.value, days: days.value }, { silent: true })
    if (seq === trendSeq) trend.value = data ?? []
  } catch (error) {
    if (seq === trendSeq) loadError.value = (error as RequestError).message
  }
}

function goPracticeKp(kp: WeakKpVO): void {
  void router.push({
    name: 'practice',
    params: { mode: 'KP' },
    query: { subjectId: subjectId.value, refId: kp.kpId },
  })
}

onMounted(async () => {
  try {
    await subjectStore.fetchGrouped({}, false, { silent: true })
  } catch {
    // 科目取不到不阻断本页：下拉为空，下面的空态会说明"先选择科目"
  }
  if (!subjectId.value) {
    // 默认落到"第一个有题的科目"而不是列表第一个：列表首位是初级科目，
    // 目前多为空库，统计页会一屏全 0——看起来像页面坏了，而不是"这门课还没题"。
    const preferred =
      subjectStore.flatList.find((item) => (item.questionCount ?? 0) > 0) ?? subjectStore.flatList[0]
    subjectId.value = preferred?.id ?? ''
  }
  await load()
})
</script>

<template>
  <div class="stats rk-container">
    <header class="stats__header">
      <h1 class="rk-page-title">学习统计</h1>
      <p class="stats__lead">
        做题量、正确率与掌握度按科目汇总；学习天数与连续天数是跨科目的。
        新答的题会在下一次打开本页时计入。
      </p>
    </header>

    <div class="stats__filters">
      <el-select v-model="subjectId" class="stats__subject" placeholder="选择科目" @change="load">
        <el-option v-for="item in subjectStore.flatList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <span class="stats__range">
        <el-radio-group v-model="days" size="small" @change="reloadTrend">
          <el-radio-button v-for="option in DAY_OPTIONS" :key="option" :value="option">
            近 {{ option }} 天
          </el-radio-button>
        </el-radio-group>
      </span>
    </div>

    <el-alert
      v-if="loadError"
      class="stats__alert"
      type="error"
      :title="`统计加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <div v-if="loading" class="stats__skeleton">
      <el-skeleton :rows="8" animated />
    </div>

    <el-empty v-else-if="!subjectId" description="先选择一个科目" />

    <el-empty
      v-else-if="noStatistics"
      description="这个科目还没有统计数据，先去做几道题吧"
    >
      <el-button
        type="primary"
        @click="router.push({ name: 'practice', params: { mode: 'SEQUENCE' }, query: { subjectId } })"
      >
        去练习
      </el-button>
    </el-empty>

    <el-empty v-else-if="!overview" description="暂无统计数据" />

    <template v-else>
      <section class="stats__cards">
        <article class="stat-card">
          <p class="stat-card__label">做题量</p>
          <p class="stat-card__value">{{ overview.answeredCount }}</p>
          <p class="stat-card__hint">本科目累计作答（含重复作答）</p>
        </article>
        <article class="stat-card">
          <p class="stat-card__label">正确率</p>
          <p class="stat-card__value">{{ formatPercent(overview.accuracy) }}</p>
          <p class="stat-card__hint">对 {{ overview.correctCount }} / 共 {{ overview.answeredCount }}</p>
        </article>
        <article class="stat-card">
          <p class="stat-card__label">科目掌握度</p>
          <p class="stat-card__value">{{ formatPercent(overview.mastery) }}</p>
          <p class="stat-card__hint">
            覆盖度 × 时间加权正确率 + 未覆盖部分的先验 0.5；没练过的章节按 0.5 计入，
            所以只做一章不会立刻接近满分。
          </p>
        </article>
        <article class="stat-card">
          <p class="stat-card__label">章节覆盖</p>
          <p class="stat-card__value">{{ formatPercent(overview.outlineCoverage) }}</p>
          <p class="stat-card__hint">已练过的叶子章节 / 全部叶子章节</p>
        </article>
        <article class="stat-card">
          <p class="stat-card__label">连续学习</p>
          <p class="stat-card__value">{{ overview.continuousDays }} 天</p>
          <p class="stat-card__hint">累计学习 {{ overview.studyDays }} 天（今天没学按昨天起算）</p>
        </article>
        <article class="stat-card">
          <p class="stat-card__label">今日</p>
          <p class="stat-card__value">{{ overview.todayAnswered }}</p>
          <p class="stat-card__hint">今日正确率 {{ formatPercent(overview.todayAccuracy) }}</p>
        </article>
        <article class="stat-card">
          <p class="stat-card__label">模拟考</p>
          <p class="stat-card__value">{{ overview.examCount }}</p>
          <p class="stat-card__hint">
            最高分 {{ overview.bestScore == null ? '—' : overview.bestScore }}
          </p>
        </article>
      </section>

      <section class="stats__panel">
        <h2 class="stats__panel-title">练习趋势</h2>
        <TrendChart :points="trend" />
      </section>

      <section class="stats__panel">
        <h2 class="stats__panel-title">章节掌握度</h2>
        <p class="stats__panel-hint">
          父节点按其子节点的已发布题量加权。「未掌握」指<b>练过但</b>掌握度低于 0.6；
          没练过的章节按先验 0.5 显示，标为「未练习」。
        </p>
        <el-empty v-if="outlineRows.length === 0" description="该科目还没有章节树" />
        <ul v-else class="outline-list">
          <li v-for="row in outlineRows" :key="row.node.id" class="outline-row">
            <span class="outline-row__name" :style="{ paddingLeft: `${row.depth * 20}px` }">
              {{ row.node.name }}
            </span>
            <span class="outline-row__count">{{ row.node.answeredCount }} / {{ row.node.questionCount }} 题</span>
            <span class="outline-row__track">
              <span class="outline-row__bar" :style="{ width: formatPercent(row.node.mastery) }" />
            </span>
            <span class="outline-row__value">{{ formatPercent(row.node.mastery) }}</span>
            <el-tag
              v-if="row.node.answeredCount > 0 && isWeak(row.node.mastery)"
              size="small"
              type="warning"
              effect="plain"
            >
              未掌握
            </el-tag>
            <span v-else-if="row.node.answeredCount === 0" class="outline-row__idle">未练习</span>
            <span v-else class="outline-row__spacer" />
          </li>
        </ul>
      </section>

      <section class="stats__panel">
        <h2 class="stats__panel-title">薄弱知识点</h2>
        <p class="stats__panel-hint">掌握度最低的 10 个知识点，按升序排列。</p>
        <el-empty v-if="weakKp.length === 0" description="还没有薄弱知识点——已作答的知识点都在掌握中" />
        <ul v-else class="weak-list">
          <li v-for="kp in weakKp" :key="kp.kpId" class="weak-row">
            <span class="weak-row__name">{{ kp.kpName }}</span>
            <span class="weak-row__meta">
              作答 {{ kp.answeredCount }} 题 · 正确率 {{ formatPercent(kp.accuracy) }}
            </span>
            <span class="weak-row__mastery">{{ formatPercent(kp.mastery) }}</span>
            <el-button text type="primary" @click="goPracticeKp(kp)">去练习</el-button>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
.stats {
  padding-top: $rk-space-l;
  padding-bottom: $rk-space-xl * 2;

  &__header {
    margin-bottom: $rk-space-l;
  }

  &__lead {
    color: $rk-text-2;
    max-width: 760px;
  }

  &__filters {
    display: flex;
    align-items: center;
    gap: $rk-space-l;
    margin-bottom: $rk-space-l;
  }

  &__subject {
    width: 240px;
  }

  &__range {
    margin-left: auto;
  }

  &__alert {
    margin-bottom: $rk-space-l;
  }

  &__skeleton {
    padding-top: $rk-space-l;
  }

  &__cards {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: $rk-space-m;
    margin-bottom: $rk-space-l;
  }

  &__panel {
    padding: $rk-space-l;
    margin-bottom: $rk-space-l;
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
  }

  &__panel-title {
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
    margin-bottom: $rk-space-s;
  }

  &__panel-hint {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    margin-bottom: $rk-space-m;
  }
}

.stat-card {
  padding: $rk-space-l;
  background-color: $rk-bg-card;
  border: 1px solid $rk-border;
  border-radius: $rk-radius-l;

  &__label {
    color: $rk-text-2;
    font-size: $rk-font-xs;
  }

  &__value {
    margin-top: $rk-space-xs;
    font-size: $rk-font-xl;
    font-weight: 600;
    color: $rk-text-1;
    font-variant-numeric: tabular-nums;
  }

  &__hint {
    margin-top: $rk-space-xs;
    color: $rk-text-3;
    font-size: $rk-font-xs;
    line-height: $rk-line-height;
  }
}

.outline-list {
  display: grid;
  gap: $rk-space-xs;
}

.outline-row {
  display: grid;
  grid-template-columns: 1fr 140px 220px 60px 72px;
  align-items: center;
  gap: $rk-space-m;
  font-size: $rk-font-s;

  &__name {
    color: $rk-text-1;
    // 过长的章节名不换行：一行一条才看得出层级
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__count {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
    text-align: right;
  }

  &__track {
    height: 8px;
    background-color: $rk-bg-page;
    border-radius: $rk-radius-s;
    overflow: hidden;
  }

  &__bar {
    display: block;
    height: 100%;
    background-color: $rk-primary;
  }

  &__value {
    color: $rk-text-1;
    font-variant-numeric: tabular-nums;
    text-align: right;
  }

  &__spacer {
    display: inline-block;
    width: 72px;
  }

  &__idle {
    display: inline-block;
    width: 72px;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }
}

.weak-list {
  display: grid;
  gap: $rk-space-s;
}

.weak-row {
  display: grid;
  grid-template-columns: 1fr 260px 80px 80px;
  align-items: center;
  gap: $rk-space-m;
  padding: $rk-space-s 0;
  border-bottom: 1px solid $rk-border;

  &:last-child {
    border-bottom: none;
  }

  &__name {
    color: $rk-text-1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__mastery {
    color: $rk-text-1;
    font-variant-numeric: tabular-nums;
    text-align: right;
  }
}
</style>
