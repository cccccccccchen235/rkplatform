<script setup lang="ts">
// 模拟考答题页（07 §7.5.1 专项，路由 `/exam/:sessionId`）。
//
// 用 `BlankLayout`：答题途中误点导航的代价太大。
//
// 三条来自设计文档的硬约束（07 §7.4.4）：
//   C4 计时以服务端为准 —— 本地只按 `displaySec` 视觉递减，写操作响应负责校准；
//   A5 场次不自动衔接 —— `PENDING` 场次显示「开始本场次」，午休不扣时；
//   B10 无变更时每 60s 心跳 —— store 内部完成，页面只在切后台/离开时补一次 flush。

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'

import ExamAnswerSheet from '@/components/exam/ExamAnswerSheet.vue'
import ExamQuestionCard from '@/components/exam/ExamQuestionCard.vue'
import ExamSectionTip from '@/components/exam/ExamSectionTip.vue'
import { useExamStore } from '@/stores/exam'
import type { ExamItemVO, ExamSectionVO } from '@/types/api'
import { SITTING_STATUS_LABEL, SUPPORT_STATUS_LABEL } from '@/types/labels'
import { formatDuration, formatUsedTime } from '@/utils/format'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const exam = useExamStore()

const loading = ref(true)
const loadError = ref('')
const loadErrorCode = ref<number | null>(null)
const abandoned = ref(false)

const submitVisible = ref(false)
const leavingVisible = ref(false)
const submitting = ref(false)
/** 交卷/跳转成绩页期间放行一次路由离开守卫。 */
let allowLeave = false

const sessionId = computed(() => String(route.params.sessionId ?? ''))
const session = computed(() => exam.session)
const currentSitting = computed(() => exam.currentSitting)
const sittings = computed(() => session.value?.sittings ?? [])

/** 当前显示的分节（可能是本场次里往前翻的已完成分节）。 */
const viewSectionNo = ref(0)

const currentSittingSections = computed<ExamSectionVO[]>(() => currentSitting.value?.sections ?? [])

const viewSection = computed<ExamSectionVO | null>(
  () => currentSittingSections.value.find((section) => section.sectionNo === viewSectionNo.value) ?? null,
)

const viewItems = computed<ExamItemVO[]>(() =>
  (session.value?.items ?? []).filter((item) => item.sectionNo === viewSectionNo.value),
)

const viewIndex = computed(() => viewItems.value.findIndex((item) => item.itemId === exam.currentItemId))
const currentItem = computed<ExamItemVO | null>(
  () => viewItems.value.find((item) => item.itemId === exam.currentItemId) ?? viewItems.value[0] ?? null,
)

const currentAnswer = computed(() => (currentItem.value ? (exam.answers[currentItem.value.itemId] ?? '') : ''))

/** 可进入的分节：本场次里已开始/已完成、且支持在线作答。 */
const enterableSectionNos = computed(() =>
  currentSittingSections.value
    .filter((section) => section.supportStatus === 'SUPPORTED' && (section.status === 'IN_PROGRESS' || section.status === 'FINISHED'))
    .map((section) => section.sectionNo),
)

const hasActiveSitting = computed(() => currentSitting.value?.status === 'IN_PROGRESS')
const hasPendingSitting = computed(() => currentSitting.value?.status === 'PENDING')
const allSittingsFinished = computed(() => sittings.value.length > 0 && sittings.value.every((s) => s.status === 'FINISHED'))

/** 当前分节是不是服务端的"当前分节"（只有它允许结束本节）。 */
const isServerCurrentSection = computed(() => viewSectionNo.value === session.value?.currentSectionNo)

const canFinishSection = computed(
  () =>
    hasActiveSitting.value &&
    isServerCurrentSection.value &&
    !!viewSection.value &&
    viewSection.value.supportStatus === 'SUPPORTED' &&
    viewSection.value.status === 'IN_PROGRESS' &&
    viewSection.value.allowEarlySubmit,
)

const countdownText = computed(() => formatDuration(exam.displaySec))
const countdownWarning = computed(() => hasActiveSitting.value && exam.displaySec <= 5 * 60)

const saveLabel = computed(() => {
  switch (exam.saveState) {
    case 'saving':
      return '保存中…'
    case 'saved':
      return exam.lastSavedAt ? `已保存 ${new Date(exam.lastSavedAt).toLocaleTimeString('zh-CN', { hour12: false })}` : '已保存'
    case 'failed':
      return '保存失败，点击重试'
    default:
      return '作答自动保存'
  }
})

const remainingLabel = computed(() => (hasActiveSitting.value ? countdownText.value : '未计时'))

/** 交卷确认里按分节统计的已答/未答。 */
const sectionStats = computed(() =>
  (session.value?.sections ?? []).map((section) => {
    const items = (session.value?.items ?? []).filter((item) => item.sectionNo === section.sectionNo)
    const answered = items.filter((item) => (exam.answers[item.itemId] ?? '') !== '').length
    return { section, answered, total: items.length }
  }),
)

// ---------------- 加载 ----------------

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  loadErrorCode.value = null
  try {
    const vo = await exam.loadSession(sessionId.value, { silent: true })
    if (vo.status === 'SUBMITTED' || vo.status === 'SCORED') {
      allowLeave = true
      await router.replace({ name: 'exam-result', params: { sessionId: vo.sessionId } })
      return
    }
    if (vo.status === 'ABANDONED') {
      abandoned.value = true
      return
    }
    viewSectionNo.value = vo.currentSectionNo
  } catch (error) {
    const err = error as RequestError
    loadErrorCode.value = err.isBusiness ? err.code : null
    loadError.value = err.message
  } finally {
    loading.value = false
  }
}

// ---------------- 作答 ----------------

function handleSelect(value: string): void {
  const item = currentItem.value
  if (!item || !hasActiveSitting.value) return
  exam.setAnswer(item.itemId, value)
}

function jumpToItem(itemId: string): void {
  const item = session.value?.items.find((row) => row.itemId === itemId)
  if (!item) return
  if (!enterableSectionNos.value.includes(item.sectionNo)) return
  viewSectionNo.value = item.sectionNo
  exam.setCurrentItem(itemId)
}

function goPrev(): void {
  const prev = viewItems.value[viewIndex.value - 1]
  if (viewIndex.value > 0 && prev) exam.setCurrentItem(prev.itemId)
}

function goNext(): void {
  const next = viewItems.value[viewIndex.value + 1]
  if (viewIndex.value >= 0 && viewIndex.value < viewItems.value.length - 1 && next) exam.setCurrentItem(next.itemId)
}

function switchSection(sectionNo: number): void {
  if (!enterableSectionNos.value.includes(sectionNo)) return
  viewSectionNo.value = sectionNo
  const first = (session.value?.items ?? []).find((item) => item.sectionNo === sectionNo)
  if (first) exam.setCurrentItem(first.itemId)
}

// ---------------- 场次 / 分节操作 ----------------

async function startSitting(): Promise<void> {
  const sitting = currentSitting.value
  if (!sitting) return
  try {
    await exam.startSitting(sitting.sittingNo, { silent: true })
    viewSectionNo.value = session.value?.currentSectionNo ?? sitting.currentSectionNo
  } catch (error) {
    ElMessage.error((error as RequestError).message)
  }
}

async function finishSection(): Promise<void> {
  const section = viewSection.value
  if (!section || !canFinishSection.value) return
  try {
    await exam.advanceSection(section.sectionNo, { silent: true })
    viewSectionNo.value = session.value?.currentSectionNo ?? section.sectionNo
    ElMessage.success('已进入下一节')
  } catch (error) {
    ElMessage.warning((error as RequestError).message)
  }
}

// ---------------- 交卷 ----------------

function openSubmit(): void {
  submitVisible.value = true
}

async function confirmSubmit(): Promise<void> {
  if (submitting.value) return
  submitting.value = true
  try {
    await exam.submit({ silent: true })
    submitVisible.value = false
    allowLeave = true
    await router.replace({ name: 'exam-result', params: { sessionId: sessionId.value } })
  } catch (error) {
    ElMessage.error((error as RequestError).message)
  } finally {
    submitting.value = false
  }
}

// ---------------- 会话被超时结算 ----------------

watch(
  () => exam.ended,
  async (value) => {
    if (!value || allowLeave) return
    exam.forgetActive()
    allowLeave = true
    ElMessage.warning('本场考试时间已到，系统已自动交卷')
    await router.replace({ name: 'exam-result', params: { sessionId: sessionId.value } })
  },
)

// ---------------- 离开与后台校准 ----------------

function handleVisibility(): void {
  if (document.visibilityState === 'visible' && session.value) {
    void exam.flushSave()
  }
}

onBeforeRouteLeave(() => {
  if (allowLeave) return true
  leavingVisible.value = true
  return false
})

async function confirmLeave(): Promise<void> {
  leavingVisible.value = false
  await exam.flushSave()
  allowLeave = true
  await router.push({ name: 'exam-center' })
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibility)
  void load()
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleVisibility)
  void exam.flushSave()
})
</script>

<template>
  <div class="exam-session">
    <header class="exam-session__topbar">
      <div class="exam-session__topbar-inner">
        <button type="button" class="exam-session__exit" @click="leavingVisible = true">← 退出</button>
        <span class="exam-session__subject">{{ session?.subjectName ?? '模拟考' }}</span>
        <span v-if="session" class="exam-session__no">{{ session.sessionNo }}</span>

        <span class="exam-session__spacer" />

        <span class="exam-session__save" :class="{ 'is-failed': exam.saveState === 'failed' }" @click="exam.retrySave">
          {{ saveLabel }}
        </span>
        <span class="exam-session__countdown" :class="{ 'is-warning': countdownWarning }">
          {{ hasActiveSitting || hasPendingSitting ? countdownText : '—' }}
        </span>
        <el-button type="primary" size="small" :loading="submitting" @click="openSubmit">交卷</el-button>
      </div>
    </header>

    <main class="exam-session__main">
      <div v-if="loading" class="exam-session__skeleton">
        <el-skeleton :rows="10" animated />
      </div>

      <el-result v-else-if="abandoned" icon="info" title="该模拟考已放弃" sub-title="放弃的会话不再计时，也不会计入模考统计。">
        <template #extra>
          <el-button type="primary" @click="router.push({ name: 'exam-center' })">返回模拟考中心</el-button>
        </template>
      </el-result>

      <el-result
        v-else-if="loadError"
        icon="warning"
        :title="loadErrorCode === 4004 ? '模拟考会话不存在' : '模拟考加载失败'"
        :sub-title="loadErrorCode === 4004 ? '链接可能已失效，或该会话不属于当前账号。' : loadError"
      >
        <template #extra>
          <el-button v-if="loadErrorCode !== 4004" type="primary" @click="load">重试</el-button>
          <el-button @click="router.push({ name: 'exam-center' })">返回模拟考中心</el-button>
        </template>
      </el-result>

      <template v-else-if="session">
        <!-- 场次 Tab：只有进行中/已结束的场次可进入（PENDING 由下面的按钮启动） -->
        <nav v-if="sittings.length > 1" class="exam-session__sitting-tabs">
          <span
            v-for="sitting in sittings"
            :key="sitting.sittingNo"
            class="exam-session__sitting-tab"
            :class="{
              'is-active': sitting.sittingNo === currentSitting?.sittingNo,
              'is-finished': sitting.status === 'FINISHED',
            }"
          >
            第 {{ sitting.sittingNo }} 场 · {{ sitting.sittingName }}
            <span class="exam-session__sitting-status">{{ SITTING_STATUS_LABEL[sitting.status] }}</span>
          </span>
        </nav>

        <!-- 未开始的场次（高级科目下午论文）：不自动计时，等用户点开始（评审 A5） -->
        <section v-if="hasPendingSitting" class="exam-session__pending rk-container">
          <h2 class="exam-session__pending-title">{{ currentSitting?.sittingName }}（未开始）</h2>
          <p class="exam-session__pending-text">
            本场次时长 {{ formatUsedTime(currentSitting?.totalDurationSec ?? 0) }}，需由你点击开始后才计时。
            场次之间不自动衔接（例如上午连考结束后，下午论文场次在午休期间不计时）。
          </p>
          <el-button type="primary" @click="startSitting">开始本场次</el-button>
        </section>

        <div v-else-if="allSittingsFinished" class="exam-session__finished rk-container">
          <el-result icon="success" title="全部场次已结束" sub-title="请点击右上角「交卷」查看成绩。">
            <template #extra>
              <el-button type="primary" @click="openSubmit">交卷</el-button>
            </template>
          </el-result>
        </div>

        <div v-else class="exam-session__body rk-container">
          <div class="exam-session__content">
            <nav class="exam-session__section-tabs">
              <button
                v-for="section in currentSittingSections"
                :key="section.sectionNo"
                type="button"
                class="exam-session__section-tab"
                :class="{
                  'is-active': section.sectionNo === viewSectionNo,
                  'is-disabled': !enterableSectionNos.includes(section.sectionNo),
                }"
                :disabled="!enterableSectionNos.includes(section.sectionNo)"
                @click="switchSection(section.sectionNo)"
              >
                {{ section.sectionNo }} · {{ section.sectionName }}
                <span v-if="section.supportStatus === 'NOT_SUPPORTED'" class="exam-session__section-note">
                  {{ SUPPORT_STATUS_LABEL[section.supportStatus] }}
                </span>
                <span v-else-if="section.status === 'FINISHED'" class="exam-session__section-note">已结束</span>
              </button>
            </nav>

            <ExamSectionTip
              v-if="viewSection && viewSection.supportStatus === 'NOT_SUPPORTED'"
              :section="viewSection"
              variant="not-supported"
            />

            <template v-else-if="viewSection">
              <p class="exam-session__section-meta">
                本节共 {{ viewSection.questionCount }} 题，每题 {{ viewSection.perQuestionScore }} 分；
                最早 {{ viewSection.durationMin }} 分钟、最晚 {{ viewSection.durationMax }} 分钟可离开。
              </p>

              <ExamSectionTip
                v-if="!viewSection.allowEarlySubmit"
                class="exam-session__section-tip"
                :section="viewSection"
                variant="no-early-submit"
              />

              <ExamQuestionCard
                v-if="currentItem"
                :item="currentItem"
                :model-value="currentAnswer"
                :disabled="viewSectionNo !== session.currentSectionNo || !hasActiveSitting"
                :remaining-label="remainingLabel"
                :session-id="sessionId"
                @update:model-value="handleSelect"
              />

              <p v-else class="exam-session__empty">本节暂无题目。</p>

              <footer class="exam-session__actions">
                <el-button :disabled="viewIndex <= 0" @click="goPrev">上一题</el-button>
                <span class="exam-session__position">
                  {{ viewIndex >= 0 ? viewIndex + 1 : 1 }} / {{ viewItems.length }}
                </span>
                <el-button :disabled="viewIndex < 0 || viewIndex >= viewItems.length - 1" @click="goNext">
                  下一题
                </el-button>
                <el-button
                  v-if="canFinishSection"
                  class="exam-session__finish-section"
                  type="primary"
                  plain
                  @click="finishSection"
                >
                  结束本节
                </el-button>
              </footer>
            </template>
          </div>

          <ExamAnswerSheet
            :items="session.items"
            :sections="currentSittingSections"
            :answers="exam.answers"
            :current-item-id="currentItem?.itemId ?? ''"
            :active-section-no="viewSectionNo"
            :enterable-section-nos="enterableSectionNos"
            @jump="jumpToItem"
          />
        </div>
      </template>
    </main>

    <!-- 交卷确认：先列出未答数，再二次确认（07 §7.5.1 专项） -->
    <el-dialog v-model="submitVisible" title="确认交卷" width="560px">
      <p class="exam-session__submit-lead">
        全卷共 {{ session?.questionCount ?? 0 }} 题，已答 {{ exam.answeredCount }} 题，
        <b>未答 {{ exam.unansweredCount }} 题</b>。交卷后不可再修改作答。
      </p>
      <table class="exam-session__stats">
        <thead>
          <tr>
            <th>分节</th>
            <th class="exam-session__stats-num">已答</th>
            <th class="exam-session__stats-num">未答</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in sectionStats" :key="row.section.sectionNo">
            <td>{{ row.section.sectionNo }} · {{ row.section.sectionName }}</td>
            <td class="exam-session__stats-num">{{ row.answered }} / {{ row.total }}</td>
            <td class="exam-session__stats-num">{{ Math.max(0, row.total - row.answered) }}</td>
          </tr>
        </tbody>
      </table>
      <template #footer>
        <el-button @click="submitVisible = false">继续作答</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">确认交卷</el-button>
      </template>
    </el-dialog>

    <!-- 离开拦截：计时不因离开而暂停（07 §7.5.1 专项） -->
    <el-dialog v-model="leavingVisible" title="离开答题页" width="420px">
      <p class="exam-session__submit-lead">离开后考试计时仍会继续，确定要离开吗？</p>
      <template #footer>
        <el-button @click="leavingVisible = false">继续答题</el-button>
        <el-button type="danger" @click="confirmLeave">确认离开</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.exam-session {
  width: 100%;
  min-height: 100vh;
  // 透明，透出 #app 的蓝白渐变（docs/36）。
  background-color: transparent;

  &__topbar {
    position: sticky;
    top: 0;
    z-index: 10;
    background-color: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
    box-shadow: 0 1px 3px rgba(42, 114, 194, 0.08);
  }

  &__topbar-inner {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    height: 56px;
    padding: 0 $rk-space-xl;
  }

  &__exit {
    padding: 0;
    border: none;
    background: none;
    color: $rk-text-2;
    font: inherit;
    font-size: $rk-font-xs;
    cursor: pointer;

    &:hover {
      color: $rk-primary;
    }
  }

  &__subject {
    font-weight: 600;
    color: $rk-text-1;
  }

  &__no {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__spacer {
    flex: 1;
  }

  &__save {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    cursor: pointer;

    &.is-failed {
      color: $rk-danger;
    }
  }

  &__countdown {
    min-width: 96px;
    text-align: right;
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
    font-variant-numeric: tabular-nums;

    // 最后 5 分钟用柔和的深琥珀提示，而非刺眼的警告红（舒缓备考压力，docs/36）。
    &.is-warning {
      color: $rk-warning;
    }
  }

  &__main {
    padding: $rk-space-l 0 $rk-space-xl;
  }

  &__skeleton {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 $rk-space-xl;
  }

  &__sitting-tabs {
    display: flex;
    gap: $rk-space-l;
    max-width: 1200px;
    margin: 0 auto $rk-space-m;
    padding: 0 $rk-space-xl;
  }

  &__sitting-tab {
    color: $rk-text-3;
    font-size: $rk-font-s;

    &.is-active {
      color: $rk-text-1;
      font-weight: 600;
    }

    &.is-finished {
      color: $rk-text-2;
    }
  }

  &__sitting-status {
    margin-left: $rk-space-xs;
    font-size: $rk-font-xs;
    color: $rk-text-3;
  }

  &__pending,
  &__finished {
    max-width: 900px;
    padding: $rk-space-xl;
    background-color: $rk-bg-card;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-soft-radius-card;
    box-shadow: $rk-soft-shadow;
  }

  &__pending-title {
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__pending-text {
    margin: $rk-space-m 0 $rk-space-l;
    max-width: 720px;
    color: $rk-text-2;
  }

  &__body {
    display: flex;
    align-items: flex-start;
    gap: $rk-space-l;

    // 响应式：窄屏把答题卡移到题目下方，而不是横向挤压题干。
    @media (max-width: 900px) {
      flex-direction: column;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__section-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: $rk-space-s;
    margin-bottom: $rk-space-m;
  }

  &__section-tab {
    padding: $rk-space-xs $rk-space-m;
    font: inherit;
    font-size: $rk-font-s;
    color: $rk-text-2;
    background-color: $rk-bg-card;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-soft-radius-control;
    cursor: pointer;

    &.is-active {
      color: $rk-primary;
      border-color: $rk-primary;
      background-color: rgba($rk-primary, 0.08);
    }

    &.is-disabled {
      color: $rk-text-3;
      background-color: $rk-bg-page;
      cursor: not-allowed;
    }
  }

  &__section-note {
    margin-left: $rk-space-xs;
    font-size: $rk-font-xs;
    color: $rk-text-3;
  }

  &__section-meta {
    margin-bottom: $rk-space-m;
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__section-tip {
    margin-bottom: $rk-space-m;
  }

  &__empty {
    color: $rk-text-3;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    margin-top: $rk-space-l;
  }

  &__position {
    color: $rk-text-3;
    font-size: $rk-font-s;
    font-variant-numeric: tabular-nums;
  }

  &__finish-section {
    margin-left: auto;
  }

  &__submit-lead {
    color: $rk-text-1;
  }

  &__stats {
    width: 100%;
    margin-top: $rk-space-m;
    border-collapse: collapse;
    font-size: $rk-font-s;

    th,
    td {
      padding: $rk-space-s $rk-space-m;
      text-align: left;
      border-bottom: 1px solid $rk-border;
    }

    th {
      color: $rk-text-3;
      font-size: $rk-font-xs;
      font-weight: 400;
    }

    &-num {
      text-align: right;
      font-variant-numeric: tabular-nums;
    }
  }
}
</style>
