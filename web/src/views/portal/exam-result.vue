<script setup lang="ts">
// 成绩详情页（07 §7.5.1 专项，路由 `/exam/:sessionId/result`）。
//
// 展示口径严格照 05 §5.6 第 24 条：总分卡只认**客观题得分**（唯一参与判定的分数），
// 主观题/论文分节标题旁固定标注「本节不判分，仅提供参考答案」且**不出现任何分数**。
//
// `GET /exams/{id}/result` 在 `SUBMITTED` 但未 `SCORED` 时按后端约定重试（1s × 3），
// 仍拿不到 `scoredAt` 就展示已有数据并提示稍后刷新——判分与交卷在同一事务里，
// 正常路径不会走到这里，但网络抖动造成的中间态值得兜一下。

import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import QuestionCard from '@/components/question/QuestionCard.vue'
import { useExamStore } from '@/stores/exam'
import type { ExamResultSectionVO, ExamResultSittingVO, ExamResultVO } from '@/types/api'
import { SECTION_TYPE_LABEL, SUPPORT_STATUS_LABEL } from '@/types/labels'
import { formatDateTimeMinute, formatPercent, formatUsedTime } from '@/utils/format'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const exam = useExamStore()

const result = ref<ExamResultVO | null>(null)
const loading = ref(true)
const loadError = ref('')
const loadErrorCode = ref<number | null>(null)
const scoring = ref(false)

const sessionId = computed(() => String(route.params.sessionId ?? ''))

function sectionsOf(sitting: ExamResultSittingVO): ExamResultSectionVO[] {
  return (result.value?.sectionResults ?? []).filter((section) => section.sittingNo === sitting.sittingNo)
}

function isUnscored(section: ExamResultSectionVO): boolean {
  return section.sectionType !== 'OBJECTIVE'
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  loadErrorCode.value = null
  try {
    let vo = await exam.fetchResult(sessionId.value, { silent: true })
    // 已交卷但尚未判分：轮询最多 3 次（每次 1s）
    for (let attempt = 0; attempt < 3 && !vo.scoredAt; attempt += 1) {
      scoring.value = true
      await sleep(1000)
      vo = await exam.fetchResult(sessionId.value, { silent: true })
    }
    result.value = vo
    exam.forgetActive()
  } catch (error) {
    const err = error as RequestError
    loadErrorCode.value = err.isBusiness ? err.code : null
    loadError.value = err.message
  } finally {
    scoring.value = false
    loading.value = false
  }
}

function goSession(): void {
  void router.push({ name: 'exam-session', params: { sessionId: sessionId.value } })
}

onMounted(load)
</script>

<template>
  <div class="exam-result rk-container">
    <router-link :to="{ name: 'exam-center' }" class="exam-result__back">← 模拟考中心</router-link>

    <div v-if="loading" class="exam-result__skeleton">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 未交卷（1005）：不是错误，是"还没到看成绩的时候"，直接引导回答题页 -->
    <el-result
      v-else-if="loadErrorCode === 1005"
      icon="info"
      title="尚未交卷"
      sub-title="本场模拟考还未交卷，交卷后才能查看成绩。"
    >
      <template #extra>
        <el-button type="primary" @click="goSession">回到答题页</el-button>
      </template>
    </el-result>

    <el-result
      v-else-if="loadError"
      :icon="loadErrorCode === 4004 ? 'warning' : 'error'"
      :title="loadErrorCode === 4004 ? '模拟考会话不存在' : '成绩加载失败'"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button v-if="loadErrorCode !== 4004" type="primary" @click="load">重试</el-button>
        <el-button @click="router.push({ name: 'exam-center' })">返回模拟考中心</el-button>
      </template>
    </el-result>

    <template v-else-if="result">
      <header class="exam-result__header">
        <h1 class="rk-page-title">模拟考成绩</h1>
        <p class="exam-result__meta">
          {{ result.subjectName }} · 会话 {{ result.sessionNo }} · 交卷于
          {{ formatDateTimeMinute(result.submittedAt) }}
        </p>
      </header>

      <el-alert
        v-if="!result.scoredAt"
        class="exam-result__alert"
        type="warning"
        :closable="false"
        show-icon
        title="成绩仍在结算中，稍后刷新即可看到完整结果"
      />

      <section class="score-cards">
        <article class="score-card score-card--primary">
          <p class="score-card__label">客观题得分</p>
          <p class="score-card__value">{{ result.objectiveScore }}</p>
          <p class="score-card__hint">唯一参与判定的分数</p>
        </article>
        <article class="score-card">
          <p class="score-card__label">可判分满分</p>
          <p class="score-card__value">{{ result.autoScoredMaxScore }}</p>
          <p class="score-card__hint">仅客观题分节</p>
        </article>
        <article class="score-card">
          <p class="score-card__label">卷面满分</p>
          <p class="score-card__value">{{ result.totalMaxScore }}</p>
          <p class="score-card__hint">含主观题/论文节，不计入判定</p>
        </article>
        <article class="score-card">
          <p class="score-card__label">客观题正确率</p>
          <p class="score-card__value">{{ formatPercent(result.accuracy) }}</p>
          <p class="score-card__hint">用时 {{ formatUsedTime(result.usedSec) }}</p>
        </article>
      </section>

      <section class="exam-result__panel">
        <h2 class="exam-result__panel-title">分场次成绩</h2>

        <article v-for="sitting in result.sittingResults" :key="sitting.sittingNo" class="sitting-result">
          <header class="sitting-result__head">
            <h3 class="sitting-result__name">第 {{ sitting.sittingNo }} 场 · {{ sitting.sittingName }}</h3>
            <span v-if="!sitting.answered" class="sitting-result__flag">本场次未作答</span>
            <span class="sitting-result__meta">
              用时 {{ formatUsedTime(sitting.usedSec) }} / 客观题 {{ sitting.objectiveScore }}（可判分
              {{ sitting.autoScoredMaxScore }}，卷面 {{ sitting.totalMaxScore }}）
            </span>
          </header>

          <table class="section-result-table">
            <thead>
              <tr>
                <th class="section-result-table__no">节</th>
                <th>分节</th>
                <th>类型</th>
                <th>作答方式</th>
                <th class="section-result-table__num">客观题得分</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="section in sectionsOf(sitting)" :key="section.sectionNo">
                <td class="section-result-table__no">{{ section.sectionNo }}</td>
                <td>
                  {{ section.sectionName }}
                  <span v-if="isUnscored(section)" class="section-result-table__note">本节不判分，仅提供参考答案</span>
                </td>
                <td>{{ SECTION_TYPE_LABEL[section.sectionType] }}</td>
                <td>{{ SUPPORT_STATUS_LABEL[section.supportStatus] }}</td>
                <!-- scored = false 的分节连"得分"列都不出现（07 §7.5.1 专项） -->
                <td class="section-result-table__num">
                  <template v-if="section.scored">{{ section.objectiveScore }} / {{ section.maxScore }}</template>
                  <span v-else class="section-result-table__muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section class="exam-result__panel">
        <h2 class="exam-result__panel-title">逐题回顾</h2>
        <p class="exam-result__panel-hint">
          客观题按对错标记，主观题与论文只显示「已作答/未作答」并给出参考答案与评分要点。
        </p>

        <el-empty v-if="result.items.length === 0" description="没有可回顾的题目数据" />
        <ol v-else class="review-list">
          <li v-for="(item, index) in result.items" :key="`${item.id}-${index}`" class="review-item">
            <p class="review-item__no">
              第 {{ index + 1 }} 题
              <span class="review-item__state">
                {{
                  item.userAnswer
                    ? item.isCorrect === true
                      ? '答对'
                      : item.isCorrect === false
                        ? '答错'
                        : '已作答'
                    : '未作答'
                }}
              </span>
            </p>
            <QuestionCard :question="item" mode="result" :judged="item.isCorrect ?? null" :model-value="item.userAnswer ?? ''" />
          </li>
        </ol>
      </section>

      <footer class="exam-result__footer">
        本次为模拟，官方考试须一次性通过全部科目。
      </footer>
    </template>
  </div>
</template>

<style scoped lang="scss">
.exam-result {
  padding-top: $rk-space-l;
  padding-bottom: $rk-space-xl * 2;

  &__back {
    display: inline-block;
    margin-bottom: $rk-space-m;
    color: $rk-text-2;
    font-size: $rk-font-xs;

    &:hover {
      color: $rk-primary;
    }
  }

  &__skeleton {
    padding-top: $rk-space-l;
  }

  &__meta {
    margin-top: $rk-space-xs;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__alert {
    margin-top: $rk-space-m;
  }

  &__panel {
    margin-top: $rk-space-xl;
    padding: $rk-space-xl;
    background-color: $rk-bg-card;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-soft-radius-card;
    box-shadow: $rk-soft-shadow;
  }

  &__panel-title {
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
    margin-bottom: $rk-space-s;
  }

  &__panel-hint {
    margin-bottom: $rk-space-m;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__footer {
    margin-top: $rk-space-xl;
    padding-top: $rk-space-l;
    border-top: 1px solid $rk-border;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }
}

.score-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: $rk-space-m;
  margin-top: $rk-space-l;

  @media (max-width: 1080px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  @media (max-width: 560px) {
    grid-template-columns: 1fr;
  }
}

.score-card {
  padding: $rk-space-l;
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;

  &--primary {
    border-color: $rk-primary;
    box-shadow: $rk-soft-shadow-hover;
  }

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
  }
}

.sitting-result + .sitting-result {
  margin-top: $rk-space-l;
}

.sitting-result {
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;
  overflow: hidden;

  &__head {
    display: flex;
    align-items: baseline;
    gap: $rk-space-m;
    flex-wrap: wrap;
    padding: $rk-space-m $rk-space-l;
    background-color: $rk-soft-spine-unprinted-1;
    border-bottom: 1px solid $rk-soft-border;
  }

  &__name {
    font-size: $rk-font-s;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__flag {
    color: $rk-warning;
    font-size: $rk-font-xs;
  }

  &__meta {
    margin-left: auto;
    color: $rk-text-2;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }
}

.section-result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: $rk-font-s;

  th,
  td {
    padding: $rk-space-s $rk-space-l;
    text-align: left;
    border-bottom: 1px solid $rk-border;
  }

  th {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-weight: 400;
  }

  tbody tr:last-child td {
    border-bottom: none;
  }

  &__no {
    width: 48px;
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__num {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  &__note {
    margin-left: $rk-space-m;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__muted {
    color: $rk-text-3;
  }
}

.review-list {
  display: grid;
  gap: $rk-space-l;
  padding: 0;
  list-style: none;
}

.review-item {
  &__no {
    margin-bottom: $rk-space-s;
    color: $rk-text-2;
    font-size: $rk-font-s;
    font-weight: 600;
  }

  &__state {
    margin-left: $rk-space-s;
    font-weight: 400;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }
}
</style>
