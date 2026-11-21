<script setup lang="ts">
// 练习答题页（07 §7.5.1 专项）。路由 `/practice/:mode`，用 `BlankLayout`：
// 答题过程中的误点导航代价太大，所以这一页没有顶栏。
//
// 数据流：出题（`POST /practice/papers`，一次性拿回含答案的题）→ 选中即本地判分
// → 入队 `pendingRecords` → 防抖/切题/离开时批量上报（`POST /practice/records`）。
// 顺序练习另外维护游标：进入时读 `GET /practice/progress` 作为 `fromSequence`，
// 推进时防抖 `PUT /practice/progress`。

import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import QuestionCard from '@/components/question/QuestionCard.vue'
import QuestionFeedbackDialog from '@/components/question/QuestionFeedbackDialog.vue'
import { usePracticeStore } from '@/stores/practice'
import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'
import type { PracticePaperPayload, PracticeProgress, QuestionDetailVO } from '@/types/api'
import type { PracticeMode } from '@/types/domain'
import { PRACTICE_MODE_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const practice = usePracticeStore()
const subjectStore = useSubjectStore()
const user = useUserStore()

const loading = ref(true)
const loadError = ref('')
const loadErrorCode = ref<number | null>(null)

const feedbackVisible = ref(false)

/** 顺序练习的续做起点（`fromSequence`），进入时由进度接口决定。 */
const fromSequence = ref(0)

/** 键盘可用字符：1~9 与 A~F 分别映射到选项下标与选项键（07 §7.5.1 专项）。 */
const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F']
const TRUE_FALSE_KEYS = ['A', 'B']

function queryString(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

const mode = computed(() => queryString(route.params.mode) as PracticeMode)
const modeLabel = computed(() => PRACTICE_MODE_LABEL[mode.value] ?? '练习')
const subjectId = computed(() => queryString(route.query.subjectId))
const refId = computed(() => queryString(route.query.refId))
const isSequence = computed(() => mode.value === 'SEQUENCE')
const isSample = computed(() => mode.value === 'SAMPLE')

/** 题量：样例题强制 ≤5（05 §5.6 第 12 条，`count > 5` 会返回 1001）。 */
const count = computed(() => {
  if (isSample.value) return 5
  const raw = Number(queryString(route.query.count))
  return Number.isFinite(raw) && raw > 0 ? raw : 10
})

const subjectName = computed(() => subjectStore.flatList.find((item) => item.id === subjectId.value)?.name ?? '练习')

const currentQuestion = computed(() => practice.currentQuestion)
const currentJudged = computed(() => practice.currentJudged)
const isEmpty = computed(() => !loading.value && !loadError.value && practice.total === 0)

/**
 * 按错误码给一句可操作的说明。
 *
 * 只用 `el-alert` 讲错误不够——用户知道失败了，但不知道下一步该做什么：
 * 2005 要登录、3001 要换科目、1001 要改范围。这三条都不是"再点一次重试"能解决的。
 */
const loadErrorHint = computed(() => {
  switch (loadErrorCode.value) {
    case 2005:
      return '该练习模式需要登录后使用，请先登录或注册。'
    case 3001:
      return '科目不存在或已停用，请返回科目页另选一门。'
    case 1001:
      return '请求参数有问题（例如样例题超过 5 题或题量越界），请调整后重试。'
    default:
      return ''
  }
})

// ---------------- 加载 ----------------

/** 从进度数组里挑顺序练习那条；没有记录时从 0 开始（05 §5.6 第 14 条）。 */
function pickSequenceCursor(progressList: PracticeProgress[]): number {
  return progressList.find((item) => item.mode === 'SEQUENCE')?.lastSequence ?? 0
}

async function load(): Promise<void> {
  if (!subjectId.value) {
    loading.value = false
    loadErrorCode.value = null
    loadError.value = '缺少科目参数，无法开始练习'
    return
  }

  loading.value = true
  loadError.value = ''
  loadErrorCode.value = null
  try {
    if (isSequence.value) {
      const progressList = await practice.fetchProgress(subjectId.value, { silent: true })
      fromSequence.value = pickSequenceCursor(progressList)
    } else {
      fromSequence.value = 0
    }

    const payload: PracticePaperPayload = {
      subjectId: subjectId.value,
      mode: mode.value,
      count: count.value,
    }
    if (refId.value) payload.refId = refId.value
    if (isSequence.value) payload.fromSequence = fromSequence.value

    // silent：失败由下面那块 alert + 重试呈现，不另弹 toast
    await practice.fetchPaper(payload, { silent: true })
  } catch (error) {
    const err = error as RequestError
    loadErrorCode.value = err.isBusiness ? err.code : null
    loadError.value = err.message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 顶栏不在这一页，科目名要自己取；失败静默，不阻断答题
  void subjectStore.fetchGrouped({}, false, { silent: true }).catch(() => undefined)
  void load()
  window.addEventListener('keydown', onKeydown)
})

// ---------------- 作答 ----------------

/** 上报防抖。选中即判分，但上报合并到 3 秒后一次发出（07 §7.5.1 专项）。 */
let flushTimer: ReturnType<typeof setTimeout> | null = null

function scheduleFlush(): void {
  if (flushTimer) clearTimeout(flushTimer)
  flushTimer = setTimeout(() => {
    flushTimer = null
    void practice.flushRecords()
  }, 3000)
}

function handleSelect(value: string): void {
  practice.answerCurrent(value)
  scheduleFlush()
}

/**
 * 顺序练习推进游标。
 *
 * 游标口径是"当前所处的题序"（`lastSequence` 回传给服务端时又作为 `fromSequence` 起点）：
 * `fromSequence + currentIndex`。切题后 `currentIndex` 已指向新题，所以这个值正好是
 * "已做完的题数"——离开后重新进来会停在下一道没做的题上。
 */
function saveSequenceCursor(): void {
  if (!isSequence.value) return
  practice.saveProgress(subjectId.value, fromSequence.value + practice.currentIndex)
}

async function goNext(): Promise<void> {
  if (!practice.next()) {
    ElMessage.info('已经是最后一题')
    return
  }
  // 离开题目即上报一次（07 §7.5.1 专项：每答满 10 题或离开题目调用）
  await practice.flushRecords()
  saveSequenceCursor()
}

function goPrev(): void {
  if (!practice.prev()) return
  saveSequenceCursor()
}

async function handleFavorite(): Promise<void> {
  try {
    await practice.toggleFavorite(practice.currentIndex)
  } catch {
    // toggleFavorite 已回滚本地状态；失败提示由拦截器给出
  }
}

function openFeedback(): void {
  if (!currentQuestion.value) return
  feedbackVisible.value = true
}

async function endPractice(): Promise<void> {
  await practice.flushRecords()
  await practice.flushProgress(subjectId.value)
  ElMessage.success('本次练习记录已提交')
  await router.push(subjectId.value ? { name: 'subject-detail', params: { id: subjectId.value } } : { name: 'subjects' })
}

/** 该题合法的选项键（判断题由前端生成 A/B，其余取 `options`）。 */
function validOptionKeys(question: QuestionDetailVO): string[] {
  if (question.questionType === 'TRUE_FALSE') return TRUE_FALSE_KEYS
  return (question.options ?? []).map((opt) => opt.key)
}

/** 键盘选择：多选是切换（加/减），单选是直接选中。 */
function selectByKey(key: string): void {
  const question = currentQuestion.value
  if (!question || !validOptionKeys(question).includes(key)) return

  if (question.questionType === 'MULTI_CHOICE') {
    const selected = new Set(practice.currentAnswer)
    if (selected.has(key)) selected.delete(key)
    else selected.add(key)
    handleSelect([...selected].sort().join(''))
    return
  }
  handleSelect(key)
}

/**
 * 文本输入控件会吞掉字母与数字，交还给它（07 §7.12：输入框内不触发快捷键）。
 *
 * **单选/复选的 input 不算**：焦点落在选项上时，Enter 仍应是"下一题"。
 */
function isTextEntry(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false
  if (target.isContentEditable) return true
  const tag = target.tagName
  if (tag === 'TEXTAREA' || tag === 'SELECT') return true
  if (tag === 'INPUT') {
    const type = (target as HTMLInputElement).type
    return type !== 'radio' && type !== 'checkbox'
  }
  return false
}

/** 按钮/链接自己会响应 Enter/Space；不排除就会"点一次、又前进一步"。 */
function isActivatable(target: EventTarget | null): boolean {
  return target instanceof HTMLElement && (target.tagName === 'BUTTON' || target.tagName === 'A')
}

function onKeydown(event: KeyboardEvent): void {
  // 反馈对话框开着时不响应全局快捷键（否则会在弹窗背后偷偷切题）
  if (feedbackVisible.value) return
  if (isTextEntry(event.target)) return
  if (event.metaKey || event.ctrlKey || event.altKey) return
  if (event.key === 'Enter' && isActivatable(event.target)) return

  switch (event.key) {
    case 'ArrowLeft':
      event.preventDefault()
      goPrev()
      return
    case 'ArrowRight':
    case 'Enter':
      event.preventDefault()
      void goNext()
      return
    default:
      break
  }

  // 1-9 映射到 A-I；A-F 直接选（07 §7.5.1 专项）
  if (/^[1-9]$/.test(event.key)) {
    event.preventDefault()
    selectByKey(OPTION_KEYS[Number(event.key) - 1] ?? '')
    return
  }
  if (/^[a-fA-F]$/.test(event.key)) {
    event.preventDefault()
    selectByKey(event.key.toUpperCase())
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  if (flushTimer) clearTimeout(flushTimer)
  // 离开页面兜底：冲掉待上报记录与游标（07 §7.5.1 专项）
  void practice.flushRecords()
  void practice.flushProgress(subjectId.value)
  practice.reset()
})
</script>

<template>
  <div class="practice-session">
    <header class="practice-session__topbar">
      <div class="practice-session__inner">
        <router-link
          :to="subjectId ? { name: 'subject-detail', params: { id: subjectId } } : { name: 'subjects' }"
          class="practice-session__back"
        >
          ← 退出
        </router-link>
        <span class="practice-session__subject">{{ subjectName }}</span>
        <span class="practice-session__mode">{{ modeLabel }}</span>
        <span v-if="!loading && !loadError" class="practice-session__progress">
          {{ practice.currentIndex + 1 }} / {{ practice.total }}
        </span>
      </div>
    </header>

    <!-- 游客样例题：说清题量与"注册后能继续"，而不是把 SAMPLE 当成残缺功能 -->
    <p v-if="isSample && !user.isLogin" class="practice-session__sample-hint">
      样例题共 5 题，注册后可继续练习。
    </p>

    <main class="practice-session__main">
      <div class="practice-session__inner">
        <div v-if="loading" class="practice-session__skeleton">
          <el-skeleton :rows="8" animated />
        </div>

        <el-alert
          v-else-if="loadError"
          type="error"
          :title="`练习加载失败：${loadError}`"
          :closable="false"
          show-icon
        >
          <template #default>
            <p v-if="loadErrorHint" class="practice-session__error-hint">{{ loadErrorHint }}</p>
            <el-button text type="primary" @click="load">重试</el-button>
          </template>
        </el-alert>

        <el-empty v-else-if="isEmpty" description="该范围暂无题目">
          <el-button
            type="primary"
            @click="router.push(subjectId ? { name: 'subject-detail', params: { id: subjectId } } : { name: 'subjects' })"
          >
            换范围
          </el-button>
        </el-empty>

        <QuestionCard
          v-else-if="currentQuestion"
          :question="currentQuestion"
          mode="practice"
          :judged="currentJudged"
          :model-value="practice.currentAnswer"
          @update:model-value="handleSelect"
          @favorite-change="handleFavorite"
          @feedback="openFeedback"
        />
      </div>
    </main>

    <footer v-if="!loading && !loadError && !isEmpty" class="practice-session__bottombar">
      <div class="practice-session__inner practice-session__actions">
        <el-button :disabled="practice.currentIndex === 0" @click="goPrev">上一题</el-button>
        <el-button :disabled="practice.currentIndex >= practice.total - 1" @click="goNext">下一题</el-button>
        <el-button text @click="handleFavorite">
          {{ currentQuestion?.isFavorited ? '★ 已收藏' : '☆ 收藏' }}
        </el-button>
        <el-button text @click="openFeedback">反馈</el-button>
        <el-button class="practice-session__end" type="primary" @click="endPractice">结束练习</el-button>
      </div>
      <!-- 快捷键说明放在操作条里：它是这一页唯一的隐藏操作，不写出来没人会去试 -->
      <p class="practice-session__keys">快捷键：← / → 切题，数字 1~9 或字母 A~F 选择，Enter 下一题。</p>
    </footer>

    <QuestionFeedbackDialog
      v-if="currentQuestion"
      :question-id="currentQuestion.id"
      v-model:visible="feedbackVisible"
    />
  </div>
</template>

<style scoped lang="scss">
.practice-session {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  // 透明，透出 #app 的蓝白渐变（docs/36）。
  background-color: transparent;

  &__inner {
    width: 100%;
    max-width: 900px;
    margin: 0 auto;
    padding: 0 $rk-space-xl;
  }

  &__topbar {
    position: sticky;
    top: 0;
    z-index: 10;
    background-color: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
    box-shadow: 0 1px 3px rgba(42, 114, 194, 0.08);
  }

  &__topbar &__inner {
    height: 56px;
    display: flex;
    align-items: center;
    gap: $rk-space-l;
  }

  &__back {
    color: $rk-text-2;
    font-size: $rk-font-xs;

    &:hover {
      color: $rk-primary;
    }
  }

  &__subject {
    font-weight: 600;
    color: $rk-text-1;
  }

  &__mode {
    padding: 0 $rk-space-s;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-radius-m;
    color: $rk-text-2;
    font-size: $rk-font-xs;
    line-height: 20px;
  }

  &__progress {
    margin-left: auto;
    color: $rk-text-2;
    font-variant-numeric: tabular-nums;
  }

  &__sample-hint {
    margin: $rk-space-m auto 0;
    max-width: 900px;
    padding: 0 $rk-space-xl;
    color: $rk-text-2;
    font-size: $rk-font-xs;
  }

  &__main {
    flex: 1;
    padding: $rk-space-xl 0;
    display: flex;
    flex-direction: column;
  }

  // 短题（截图里那种两行题干 + 四个选项）只占上方一小块，下面空一大片，
  // 视觉重心全压在顶栏。用 auto margin 让内容在纵向剩余空间里居中：
  // 有空间才居中，题目长到超出时 auto margin 自动归零、从顶部开始正常滚动
  // （用 justify-content:center 会在超高时把题干顶出可视区，不能用）。
  // 骨架屏 / 错误 / 空态也走同一条路径，加载前后位置不跳。
  &__main > &__inner {
    margin-top: auto;
    margin-bottom: auto;
  }

  &__skeleton {
    padding-top: $rk-space-l;
  }

  &__error-hint {
    margin-bottom: $rk-space-s;
    color: $rk-text-2;
    font-size: $rk-font-xs;
  }

  &__bottombar {
    position: sticky;
    bottom: 0;
    background-color: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
    box-shadow: 0 -1px 3px rgba(42, 114, 194, 0.08);
    padding: $rk-space-m 0 $rk-space-s;
  }

  &__actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: $rk-space-s;

    @media (max-width: 600px) {
      &__end {
        margin-left: 0;
      }
    }
  }

  // 结束练习是这一页的终点动作，推到最右，与"上一题/下一题"分开
  &__end {
    margin-left: auto;
  }

  &__keys {
    max-width: 900px;
    margin: $rk-space-xs auto 0;
    padding: 0 $rk-space-xl;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }
}
</style>