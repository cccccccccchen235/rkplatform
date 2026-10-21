<script setup lang="ts">
// 模拟考中心（07 §7.5.1，路由 `/exam`）。
//
// 两件事：发起一门科目的模拟考，以及查看历史成绩。
//
// 一处必须解释的取舍 —— **4003 冲突对话框**（07 §7.4.4 / 评审 G1）：
// `POST /exams` 在存在未完成会话时返回 `4003`，但该响应**不带 `sessionId`**
// （05 §5.6 第 16 条只定义了错误码，服务端 `BizException` 无 data 载荷）。
// 而 `IN_PROGRESS` 会话又刻意不在历史列表里（05 §5.6 第 17 条）。因此前端只能在
// 发起成功时把 `sessionId` 记进 `localStorage`（`storage` 的 `exam:active`），
// 4003 时再读出来。这是同一浏览器内的正常找回路径；换设备时确实拿不到，
// 只能等 6 小时窗口期后由服务端自动结算——这一点在对话框文案里如实说明。

import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchExamHistory, type ExamHistoryQuery } from '@/api/exam'
import { useExamStore } from '@/stores/exam'
import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'
import type { ExamHistoryVO, ExamStatus } from '@/types/api'
import { EXAM_STATUS_LABEL } from '@/types/labels'
import { formatDateTimeMinute, formatPercent, formatUsedTime } from '@/utils/format'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const exam = useExamStore()
const subjectStore = useSubjectStore()
const user = useUserStore()

const subjectId = ref('')
const starting = ref(false)
const loadError = ref('')

const history = ref<ExamHistoryVO[]>([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historyPageSize = 10
const includeAbandoned = ref(false)
const historyLoading = ref(false)
const historyError = ref('')

/** 4003 冲突对话框；null = 未打开。 */
const conflict = ref<{ sessionId: string; sittingLabel: string } | null>(null)
/** 本地没有未完成会话记录而服务端却报 4003 时的说明性对话框。 */
const conflictUnknown = ref(false)

const configuredSubjects = computed(() => subjectStore.configuredSubjects)

/** 已选科目是否配置了卷面模板。模板缺失时禁用发起并就地说明（4001 的前置提示）。 */
const selectedSubject = computed(() => configuredSubjects.value.find((item) => item.id === subjectId.value) ?? null)

const canStart = computed(() => !!selectedSubject.value && !starting.value)

const historyTagType = (status: ExamHistoryVO['status']): 'success' | 'info' | 'warning' =>
  status === 'SCORED' ? 'success' : status === 'ABANDONED' ? 'info' : 'warning'

function statusLabel(status: string): string {
  return EXAM_STATUS_LABEL[status as ExamStatus] ?? status
}

// ---------------- 科目 ----------------

async function loadSubjects(): Promise<void> {
  try {
    await subjectStore.fetchGrouped({}, false, { silent: true })
  } catch {
    // 科目取不到不阻断整页：下面的空态会说明"没有可考科目"
  }
  const preferred =
    configuredSubjects.value.find((item) => item.id === user.currentSubjectId) ??
    configuredSubjects.value[0] ??
    null
  subjectId.value = preferred?.id ?? ''
}

// ---------------- 历史 ----------------

async function loadHistory(): Promise<void> {
  historyLoading.value = true
  historyError.value = ''
  try {
    const query: ExamHistoryQuery = {
      includeAbandoned: includeAbandoned.value,
      pageNum: historyPage.value,
      pageSize: historyPageSize,
    }
    const page = await fetchExamHistory(query, { silent: true })
    history.value = page.list ?? []
    historyTotal.value = page.total ?? 0
  } catch (error) {
    history.value = []
    historyTotal.value = 0
    historyError.value = (error as RequestError).message
  } finally {
    historyLoading.value = false
  }
}

function changePage(page: number): void {
  historyPage.value = page
  void loadHistory()
}

function toggleAbandoned(): void {
  historyPage.value = 1
  void loadHistory()
}

// ---------------- 发起 ----------------

async function startExam(): Promise<void> {
  if (!subjectId.value || starting.value) return
  starting.value = true
  try {
    const vo = await exam.create(subjectId.value, { silent: true })
    await router.push({ name: 'exam-session', params: { sessionId: vo.sessionId } })
  } catch (error) {
    await handleStartError(error as RequestError)
  } finally {
    starting.value = false
  }
}

async function handleStartError(error: RequestError): Promise<void> {
  switch (error.code) {
    case 4001:
      loadError.value = '该科目尚未配置卷面模板，暂不能发起模拟考。'
      return
    case 4002:
      loadError.value = '该科目卷面没有可作答的分节（当前题库缺少对应题型），请换一门科目。'
      return
    case 4003:
      await openConflictDialog()
      return
    default:
      loadError.value = error.message
  }
}

/**
 * 打开 4003 冲突对话框。
 *
 * 先读本地记录的 `sessionId`，再 `GET /exams/{id}` 确认它仍是未完成态
 * （可能已被超时结算或已在别处交卷）。取不到/已结束时清理本地记录并提示重试。
 */
async function openConflictDialog(): Promise<void> {
  const active = exam.readActive()
  if (!active) {
    conflictUnknown.value = true
    return
  }
  try {
    const vo = await exam.loadSession(active.sessionId, { silent: true })
    if (vo.status !== 'CREATED' && vo.status !== 'IN_PROGRESS') {
      exam.forgetActive()
      conflictUnknown.value = true
      return
    }
    const sitting = vo.sittings.find((item) => item.sittingNo === vo.currentSittingNo)
    conflict.value = {
      sessionId: vo.sessionId,
      sittingLabel: sitting ? `第 ${sitting.sittingNo} 场 · ${sitting.sittingName}` : `第 ${vo.currentSittingNo} 场`,
    }
  } catch {
    exam.forgetActive()
    conflictUnknown.value = true
  }
}

function continueExam(): void {
  const sessionIdValue = conflict.value?.sessionId ?? ''
  conflict.value = null
  if (sessionIdValue) void router.push({ name: 'exam-session', params: { sessionId: sessionIdValue } })
}

/** 放弃未完成会话后立即重试发起。 */
async function abandonAndRestart(): Promise<void> {
  const sessionIdValue = conflict.value?.sessionId ?? ''
  conflict.value = null
  if (!sessionIdValue) return
  starting.value = true
  try {
    await exam.abandon(sessionIdValue, { silent: true })
    ElMessage.success('已放弃未完成的模拟考')
    await startExam()
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    starting.value = false
  }
}

function openResult(row: ExamHistoryVO): void {
  if (row.status === 'ABANDONED') return
  void router.push({ name: 'exam-result', params: { sessionId: row.sessionId } })
}

onMounted(async () => {
  await loadSubjects()
  // 从科目详情页「发起模拟考」带过来的 subjectId 优先（且必须是配置了模板的科目）
  const fromQuery = typeof route.query.subjectId === 'string' ? route.query.subjectId : ''
  if (fromQuery && configuredSubjects.value.some((item) => item.id === fromQuery)) {
    subjectId.value = fromQuery
  }
  await loadHistory()
})
</script>

<template>
  <div class="exam-center rk-container">
    <header class="exam-center__header">
      <h1 class="rk-page-title">模拟考</h1>
      <p class="exam-center__lead">
        按卷面模板随机组卷，全程服务端计时；交卷后立即判客观题并给出成绩与逐题回顾。
        主观题与论文节只提供参考答案，不计入得分。
      </p>
    </header>

    <section class="exam-center__start">
      <h2 class="exam-center__section-title">发起模拟考</h2>
      <div class="exam-center__start-row">
        <el-select v-model="subjectId" class="exam-center__subject" filterable placeholder="选择科目">
          <el-option
            v-for="item in configuredSubjects"
            :key="item.id"
            :label="`${item.name}（${item.code}）`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" :loading="starting" :disabled="!canStart" @click="startExam">
          开始模拟考
        </el-button>
        <span v-if="selectedSubject" class="exam-center__summary">
          满分 {{ selectedSubject.paperTotalScore ?? '—' }} 分
        </span>
      </div>

      <el-alert
        v-if="loadError"
        class="exam-center__alert"
        type="error"
        :title="loadError"
        :closable="true"
        show-icon
        @close="loadError = ''"
      />

      <el-empty
        v-if="configuredSubjects.length === 0"
        class="exam-center__empty"
        description="暂无配置了卷面模板的科目，暂时无法发起模拟考"
      />
    </section>

    <section class="exam-center__history">
      <header class="exam-center__history-head">
        <h2 class="exam-center__section-title">历史成绩</h2>
        <el-checkbox v-model="includeAbandoned" @change="toggleAbandoned">显示已放弃的考试</el-checkbox>
      </header>

      <el-alert
        v-if="historyError"
        class="exam-center__alert"
        type="error"
        :title="`历史成绩加载失败：${historyError}`"
        :closable="false"
        show-icon
      >
        <template #default>
          <el-button text type="primary" @click="loadHistory">重试</el-button>
        </template>
      </el-alert>

      <el-table v-loading="historyLoading" :data="history" class="exam-center__table" empty-text="还没有模拟考记录">
        <el-table-column prop="subjectName" label="科目" min-width="160" />
        <el-table-column prop="sessionNo" label="会话编号" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="historyTagType(row.status)" effect="plain" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="客观题得分" width="130" align="right">
          <template #default="{ row }">
            {{ row.objectiveScore }} / {{ row.autoScoredMaxScore }}
          </template>
        </el-table-column>
        <el-table-column label="正确率" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.accuracy) }}</template>
        </el-table-column>
        <el-table-column label="用时" width="110" align="right">
          <template #default="{ row }">{{ formatUsedTime(row.usedSec) }}</template>
        </el-table-column>
        <el-table-column label="交卷时间" width="160">
          <template #default="{ row }">{{ formatDateTimeMinute(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'ABANDONED'"
              text
              type="primary"
              @click="openResult(row as ExamHistoryVO)"
            >
              查看成绩
            </el-button>
            <span v-else class="exam-center__muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="historyTotal > historyPageSize" class="exam-center__pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="historyTotal"
          :page-size="historyPageSize"
          :current-page="historyPage"
          @current-change="changePage"
        />
      </div>
    </section>

    <el-dialog :model-value="conflict !== null" title="存在未完成的模拟考" width="460px" @close="conflict = null">
      <p class="exam-center__dialog-text">
        你有一场未完成的模拟考：<b>{{ conflict?.sittingLabel }}</b>。
      </p>
      <p class="exam-center__dialog-sub">会话编号：{{ conflict?.sessionId }}</p>
      <template #footer>
        <el-button @click="abandonAndRestart">放弃并重新开始</el-button>
        <el-button type="primary" @click="continueExam">继续未完成的考试</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="conflictUnknown" title="存在未完成的模拟考" width="460px" @close="conflictUnknown = false">
      <p class="exam-center__dialog-text">
        服务端提示你还有一场未完成的模拟考，但本机没有它的记录（可能是在其它设备上发起的）。
      </p>
      <p class="exam-center__dialog-sub">
        未完成的会话会在窗口期结束后被服务端自动交卷，随后出现在历史成绩中；也可以稍后回到本页重试。
      </p>
      <template #footer>
        <el-button type="primary" @click="conflictUnknown = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.exam-center {
  padding-top: $rk-space-l;
  padding-bottom: $rk-space-xl * 2;

  &__lead {
    margin-top: $rk-space-s;
    max-width: 760px;
    color: $rk-text-2;
  }

  &__section-title {
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__start {
    margin-top: $rk-space-xl;
  }

  &__start-row {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    margin-top: $rk-space-m;
  }

  &__subject {
    width: 320px;
  }

  &__summary {
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__alert {
    margin-top: $rk-space-m;
  }

  &__empty {
    margin-top: $rk-space-m;
  }

  &__history {
    margin-top: $rk-space-xl * 1.5;
  }

  &__history-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: $rk-space-l;
    margin-bottom: $rk-space-m;
  }

  &__table {
    width: 100%;
  }

  &__pager {
    display: flex;
    justify-content: flex-end;
    margin-top: $rk-space-m;
  }

  &__muted {
    color: $rk-text-3;
  }

  &__dialog-text {
    color: $rk-text-1;
  }

  &__dialog-sub {
    margin-top: $rk-space-m;
    color: $rk-text-2;
    font-size: $rk-font-s;
  }
}
</style>
