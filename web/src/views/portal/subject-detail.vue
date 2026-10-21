<script setup lang="ts">
// 科目详情（07 §7.5.1）。与科目列表的分工：
// 列表页回答"有哪些科目、各自什么状态"，详情页回答"这门课考什么、怎么考"。
//
// 两处刻意的取舍：
//
// 1. **卷面结构按场次渲染**（05 §5.2 的 `PaperSummary.sittings`）。高级科目是
//    上午连考 + 下午论文两场，论文独立成场次、时长不与上午共享。所以时长与满分
//    必须落在场次上，不能把 sessions 拍平——拍平之后"总时长 210 分钟"这种数字
//    会让人以为是一场连着考 210 分钟。
// 2. **`paperSummary = null` 是数据现状，不是故障**。它表示这门课还没配卷面模板，
//    文案不能写成"加载失败"，否则用户会反复刷新一个永远不会有结果的页面。
//
// 这里展示 `contentStatus` 与 `questionCount` 是照 docs/23 §2.2 第 4 条执行的：
// 那一条把"库里 BULK 与 0 题并存"列为**页面前置**——页面如实展示，数据去改。
// 悄悄藏掉其中一个字段只会让矛盾不被发现。

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { useSubjectStore } from '@/stores/subject'
import type { PaperSittingSummary } from '@/types/api'
import {
  CONTENT_STATUS_LABEL,
  EXAM_PERIOD_LABEL,
  OUTLINE_STATUS_LABEL,
  SECTION_TYPE_LABEL,
  SUBJECT_LEVEL_LABEL,
  SUPPORT_STATUS_LABEL,
} from '@/types/labels'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const subjectStore = useSubjectStore()

const loading = ref(true)
const loadError = ref('')

const subjectId = computed(() => String(route.params.id ?? ''))
const subject = computed(() => subjectStore.currentSubject)

/** 只有场次，没有卷面的情况不该走到这里；取不到就给空数组。 */
const sittings = computed<PaperSittingSummary[]>(() => subject.value?.paperSummary?.sittings ?? [])

/**
 * 场次数 ≥ 2 时才在标题上写"共 N 场"：单场写出来是废话，
 * 而两场正是"高级科目与其他科目不一样"这件事本身。
 */
const showSittingCount = computed(() => sittings.value.length > 1)

onMounted(async () => {
  // 先清空再加载：直接进详情页时 store 里可能还留着上一次的科目，
  // 不清就会在数据到达前先渲染出**另一门课**的卷面结构。
  subjectStore.clearDetail()
  loading.value = true
  loadError.value = ''
  try {
    // silent：两个请求的失败都归下面那块 alert 讲（`Promise.all` 只抛出先失败的那个，
    // 但另一条被静音了，所以不会出现"面板讲一个、toast 讲另一个"）。
    await Promise.all([
      subjectStore.fetchSubjectDetailById(subjectId.value, { silent: true }),
      // 知识点与详情互不依赖，并发取。任一失败都算失败：半屏数据比整屏错误更难判断。
      subjectStore.fetchKnowledgePoints(subjectId.value, { silent: true }),
    ])
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
})

async function reload(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    await Promise.all([
      subjectStore.fetchSubjectDetailById(subjectId.value, { silent: true }),
      subjectStore.fetchKnowledgePoints(subjectId.value, { silent: true }),
    ])
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="subject-detail rk-container">
    <router-link :to="{ name: 'subjects' }" class="subject-detail__back">← 全部科目</router-link>

    <div v-if="loading" class="subject-detail__skeleton">
      <el-skeleton :rows="8" animated />
    </div>

    <el-alert
      v-else-if="loadError"
      type="error"
      :title="`科目详情加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="reload">重试</el-button>
      </template>
    </el-alert>

    <template v-else-if="subject">
      <header class="subject-detail__header">
        <div class="subject-detail__title-row">
          <h1 class="rk-page-title subject-detail__title">{{ subject.name }}</h1>
          <span class="subject-detail__code">{{ subject.code }}</span>
          <span class="subject-detail__level">{{ SUBJECT_LEVEL_LABEL[subject.level] }}</span>
        </div>
        <p v-if="subject.intro" class="subject-detail__intro">{{ subject.intro }}</p>
      </header>

      <dl class="subject-facts">
        <div class="subject-fact">
          <dt>开考</dt>
          <dd>{{ EXAM_PERIOD_LABEL[subject.examPeriod] }}</dd>
        </div>
        <div class="subject-fact">
          <dt>大纲</dt>
          <dd>
            {{ OUTLINE_STATUS_LABEL[subject.outlineStatus] }}
            <router-link :to="{ name: 'subject-outline', params: { id: subject.id } }" class="subject-fact__link">
              查看章节
            </router-link>
          </dd>
        </div>
        <div class="subject-fact">
          <dt>题库</dt>
          <dd>
            {{ CONTENT_STATUS_LABEL[subject.contentStatus] }}
            <span class="subject-fact__num">{{ subject.questionCount }} 题</span>
          </dd>
        </div>
        <div class="subject-fact">
          <dt>卷面</dt>
          <dd>
            <template v-if="subject.paperTotalScore != null">满分 {{ subject.paperTotalScore }} 分</template>
            <template v-else>尚未配置卷面模板</template>
          </dd>
        </div>
      </dl>

      <section class="subject-section">
        <h2 class="subject-section__title">
          卷面结构
          <span v-if="showSittingCount" class="subject-section__note">共 {{ sittings.length }} 场</span>
          <router-link
            v-if="sittings.length > 0"
            class="subject-section__action"
            :to="{ name: 'exam-center', query: { subjectId: subject.id } }"
          >
            发起模拟考
          </router-link>
        </h2>

        <el-empty v-if="sittings.length === 0" description="该科目尚未配置卷面模板，暂不能发起模拟考" />

        <div v-else class="sitting-list">
          <article v-for="sitting in sittings" :key="sitting.sittingNo" class="sitting">
            <header class="sitting__head">
              <h3 class="sitting__name">{{ sitting.sittingName }}</h3>
              <div class="sitting__metrics">
                <span class="sitting__metric">时长 {{ sitting.totalDuration }} 分钟</span>
                <span class="sitting__metric">满分 {{ sitting.totalMaxScore }} 分</span>
                <!--
                  只有两个数不一样时才写机考满分。相等时写出来是一句废话
                  （"满分 75 / 其中机考 75"），而不同时它恰恰是最该看见的那个数：
                  差额就是需要人工阅卷、考生在平台上拿不到即时反馈的部分。
                -->
                <span v-if="sitting.autoScoredMaxScore !== sitting.totalMaxScore" class="sitting__metric">
                  其中机考 {{ sitting.autoScoredMaxScore }} 分
                </span>
              </div>
            </header>

            <table class="section-table">
              <thead>
                <tr>
                  <th class="section-table__no">节</th>
                  <th>分节</th>
                  <th>题型</th>
                  <th class="section-table__num">题量</th>
                  <th>作答方式</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="section in sitting.sections" :key="section.sectionNo">
                  <td class="section-table__no">{{ section.sectionNo }}</td>
                  <td>{{ section.sectionName }}</td>
                  <td>{{ SECTION_TYPE_LABEL[section.sectionType] }}</td>
                  <td class="section-table__num">{{ section.questionCount }}</td>
                  <td>
                    <span
                      class="section-table__support"
                      :class="{ 'is-offline': section.supportStatus === 'NOT_SUPPORTED' }"
                    >
                      {{ SUPPORT_STATUS_LABEL[section.supportStatus] }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </article>
        </div>
      </section>

      <section class="subject-section">
        <h2 class="subject-section__title">
          知识点
          <span v-if="subjectStore.knowledgePoints.length" class="subject-section__note">
            {{ subjectStore.knowledgePoints.length }} 个
          </span>
        </h2>

        <el-empty v-if="subjectStore.knowledgePoints.length === 0" description="该科目暂无知识点标签" />

        <ul v-else class="kp-list">
          <li v-for="kp in subjectStore.knowledgePoints" :key="kp.id" class="kp-item">
            <span class="kp-item__name">{{ kp.name }}</span>
            <!-- 同义词是"检索时会命中这里"的说明，不是装饰；有才显示 -->
            <span v-if="kp.aliases" class="kp-item__aliases">{{ kp.aliases }}</span>
            <span class="kp-item__count">{{ kp.questionCount }}</span>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
.subject-detail {
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

  &__header {
    margin-bottom: $rk-space-l;
  }

  &__title-row {
    display: flex;
    align-items: baseline;
    gap: $rk-space-m;
  }

  &__title {
    margin-bottom: 0;
  }

  &__code {
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__level {
    padding: 0 $rk-space-s;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-s;
    color: $rk-text-2;
    font-size: $rk-font-xs;
    line-height: 20px;
  }

  &__intro {
    margin-top: $rk-space-m;
    max-width: 760px;
    color: $rk-text-2;
  }
}

.subject-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: $rk-space-l;
  padding: $rk-space-l;
  margin-bottom: $rk-space-xl;
  background-color: $rk-bg-card;
  border: 1px solid $rk-border;
  border-radius: $rk-radius-l;
}

.subject-fact {
  display: flex;
  flex-direction: column;
  gap: $rk-space-xs;

  dt {
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  dd {
    color: $rk-text-1;
  }

  &__num {
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
    margin-left: $rk-space-xs;
  }

  &__link {
    margin-left: $rk-space-s;
    font-size: $rk-font-xs;
    color: $rk-primary;
  }
}

.subject-section {
  margin-bottom: $rk-space-xl * 1.5;

  &__title {
    display: flex;
    align-items: baseline;
    gap: $rk-space-s;
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
    margin-bottom: $rk-space-l;
    padding-bottom: $rk-space-s;
    border-bottom: 1px solid $rk-border;
  }

  &__note {
    font-size: $rk-font-xs;
    font-weight: 400;
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__action {
    margin-left: auto;
    font-size: $rk-font-xs;
    font-weight: 400;
    color: $rk-primary;
  }
}

.sitting-list {
  display: grid;
  gap: $rk-space-l;
}

.sitting {
  background-color: $rk-bg-card;
  border: 1px solid $rk-border;
  border-radius: $rk-radius-l;
  overflow: hidden;

  &__head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: $rk-space-l;
    padding: $rk-space-m $rk-space-l;
    border-bottom: 1px solid $rk-border;
    background-color: $rk-bg-page;
  }

  &__name {
    font-size: $rk-font-s;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__metrics {
    display: flex;
    gap: $rk-space-l;
  }

  &__metric {
    font-size: $rk-font-xs;
    color: $rk-text-2;
    font-variant-numeric: tabular-nums;
  }
}

.section-table {
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
    font-size: $rk-font-xs;
    font-weight: 400;
    color: $rk-text-3;
  }

  td {
    color: $rk-text-1;
  }

  tbody tr:last-child td {
    border-bottom: none;
  }

  &__no,
  &__num {
    font-variant-numeric: tabular-nums;
  }

  &__no {
    width: 48px;
    color: $rk-text-3;
  }

  &__num {
    width: 72px;
    text-align: right;
  }

  &__support {
    color: $rk-text-2;

    &.is-offline {
      color: $rk-warning;
    }
  }
}

.kp-list {
  display: flex;
  flex-wrap: wrap;
  gap: $rk-space-s;
}

.kp-item {
  display: flex;
  align-items: baseline;
  gap: $rk-space-s;
  padding: $rk-space-xs $rk-space-m;
  background-color: $rk-bg-card;
  border: 1px solid $rk-border;
  border-radius: $rk-radius-s;

  &__name {
    color: $rk-text-1;
  }

  &__aliases {
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__count {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }
}
</style>
