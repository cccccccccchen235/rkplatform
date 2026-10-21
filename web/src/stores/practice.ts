// 练习会话（07 §7.4.3）。
//
// 这个 store 只服务"一次练习会话"：出题、作答、本地判分、批量上报、顺序练习游标。
// 它**不持久化**任何东西——刷新页面即重新出题。顺序练习的续做靠服务端的
// `user_practice_progress`（05 §5.6 第 14/15 条），不是靠本地缓存。

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { addFavorite, removeFavorite } from '@/api/favorite'
import {
  fetchPracticePaper,
  fetchPracticeProgress,
  savePracticeProgress,
  submitPracticeRecords,
} from '@/api/practice'
import type {
  PracticePaper,
  PracticePaperPayload,
  PracticeProgress,
  PracticeRecordItem,
  PracticeSource,
  QuestionDetailVO,
} from '@/types/api'
import type { PracticeMode } from '@/types/domain'
import type { RequestOptions } from '@/utils/request'

/**
 * 练习模式 → 上报来源（07 §7.4.3 的映射表）。
 *
 * `SAMPLE` 映射为 `null`：游客样例题**不上报**。写成一张 `Record` 而不是 if-else，
 * 是为了将来新增模式时这里会缺键、编译期就报错，而不是悄悄按 `WRONG_REDO` 上报。
 */
const SOURCE_BY_MODE: Record<PracticeMode, PracticeSource | null> = {
  CHAPTER: 'PRACTICE_CHAPTER',
  KP: 'PRACTICE_KP',
  SEQUENCE: 'PRACTICE_SEQUENCE',
  FAVORITE: 'PRACTICE_FAVORITE',
  WRONG_REDO: 'WRONG_REDO',
  SAMPLE: null,
}

/** 上报单次上限（05 §5.6 第 13 条）。 */
const RECORD_BATCH_SIZE = 50

/** 切题后游标落库的防抖时长（07 §7.5.1 专项：debounce 3s）。 */
const PROGRESS_DEBOUNCE_MS = 3000

export const usePracticeStore = defineStore('practice', () => {
  // ---------------- state ----------------

  const subjectId = ref('')
  const mode = ref<PracticeMode>('SEQUENCE')
  /** `CHAPTER` 传章节 ID；`KP` 传知识点 ID；其余为 `null`。 */
  const refId = ref<string | null>(null)

  /** 本卷题目，**含答案与解析**（05 §5.6 第 12 条）。 */
  const questions = ref<QuestionDetailVO[]>([])
  /** questionId → 用户作答。多选存成排序后的字符串（如 "ABD"）。 */
  const answers = ref<Record<string, string>>({})
  /** questionId → 本地判分；主观题/论文为 `null`（07 §7.4.3）。 */
  const judged = ref<Record<string, boolean | null>>({})

  const currentIndex = ref(0)
  const total = ref(0)
  /** 顺序练习的下一个题序（服务端给，出题时透传回来）。 */
  const nextSequence = ref(0)
  const loading = ref(false)

  /** 尚未上报的答题记录。同一题重复作答覆盖旧记录，不上报中途的快照。 */
  const pendingRecords = ref<PracticeRecordItem[]>([])

  /**
   * 本地正确数。上报成功后由服务端的 `correctCount` 覆盖（07 §7.4.3）：
   * 客户端判分只是即时反馈，权威结果以服务端复核为准。
   */
  const localCorrectCount = ref(0)

  /** 当前题进入视野的时刻，用于计算 `durationMs`。非响应式，不需要触发渲染。 */
  let questionEnteredAt = Date.now()

  // ---------------- getters ----------------

  const currentQuestion = computed<QuestionDetailVO | null>(() => questions.value[currentIndex.value] ?? null)

  const currentAnswer = computed<string>(() => {
    const question = currentQuestion.value
    return question ? (answers.value[question.id] ?? '') : ''
  })

  const currentJudged = computed<boolean | null>(() => {
    const question = currentQuestion.value
    if (!question) return null
    return judged.value[question.id] ?? null
  })

  const answeredCount = computed(() => Object.keys(answers.value).length)

  const hasPendingRecords = computed(() => pendingRecords.value.length > 0)

  // ---------------- 本地判分（07 §7.4.3，严格对齐 02 §2.2.1） ----------------

  /** 大写并去空白：服务端存的是规范化后的大写字母，用户输入/展示可能带空格。 */
  function normalize(value: string): string {
    return value.trim().toUpperCase()
  }

  /**
   * 多选比对：**字符集合完全相等、顺序无关，不给部分分**（07 §7.4.3）。
   *
   * 与 06 §6.7 / JudgeServiceImpl 逐字对齐：只去掉内部空白再排序
   * （服务端就是这么做的），不要顺手把逗号也当分隔符——那样前端判对、
   * 服务端判错，同一道题会出现两个结论。
   */
  function asKeySet(value: string): string {
    return normalize(value)
      .replace(/\s+/g, '')
      .split('')
      .sort()
      .join('')
  }

  /**
   * 本地判分。返回 `true/false`；主观题、论文，或缺少答案/作答时返回 `null`。
   *
   * 返回 `null` 而不是 `false`：`false` 在 UI 上会被读成"你答错了"，
   * 而"不判分"与"判错"是两件事。
   */
  function judgeLocally(index: number): boolean | null {
    const question = questions.value[index]
    if (!question) return null

    if (question.questionType === 'SUBJECTIVE' || question.questionType === 'ESSAY') return null

    const correct = question.correctAnswer
    const user = answers.value[question.id]
    if (!correct || !user) return null

    if (question.questionType === 'MULTI_CHOICE') return asKeySet(user) === asKeySet(correct)
    return normalize(user) === normalize(correct)
  }

  // ---------------- actions ----------------

  /** 记录"进入当前题"的时刻，作为下一题作答用时的起点。 */
  function markQuestionEntered(): void {
    questionEnteredAt = Date.now()
  }

  /** 入队一条答题记录；同一题重复作答覆盖旧记录（避免同题多条）。 */
  function queueRecord(questionIdValue: string, userAnswer: string): void {
    const durationMs = Math.max(0, Date.now() - questionEnteredAt)
    const record: PracticeRecordItem = { questionId: questionIdValue, userAnswer, durationMs }
    const existing = pendingRecords.value.findIndex((item) => item.questionId === questionIdValue)
    if (existing >= 0) pendingRecords.value.splice(existing, 1, record)
    else pendingRecords.value.push(record)
  }

  /**
   * 提交当前题的作答：写 `answers`、就地判分、入队待上报。
   *
   * 顺手重算 `localCorrectCount`（而不是自增）：同一题改答案时自增会重复计数。
   */
  function answerCurrent(key: string): void {
    const question = currentQuestion.value
    if (!question) return

    answers.value = { ...answers.value, [question.id]: key }
    judged.value = { ...judged.value, [question.id]: judgeLocally(currentIndex.value) }
    localCorrectCount.value = Object.values(judged.value).filter((value) => value === true).length
    queueRecord(question.id, key)
  }

  /** 下一题。返回是否真的移动了（末题返回 false，页面据此提示"已是最后一题"）。 */
  function next(): boolean {
    if (currentIndex.value >= questions.value.length - 1) return false
    currentIndex.value += 1
    markQuestionEntered()
    return true
  }

  function prev(): boolean {
    if (currentIndex.value <= 0) return false
    currentIndex.value -= 1
    markQuestionEntered()
    return true
  }

  /**
   * 出题。整个会话状态在这里重置，避免上一卷的作答/判分残留到新卷。
   *
   * `options` 由页面传 `{ silent: true }`：失败归页面那块 alert 讲，
   * 不在 api 层弹 toast。
   */
  async function fetchPaper(payload: PracticePaperPayload, options?: RequestOptions): Promise<PracticePaper> {
    subjectId.value = payload.subjectId
    mode.value = payload.mode
    refId.value = payload.refId ?? null

    loading.value = true
    try {
      const paper = await fetchPracticePaper(payload, options)
      questions.value = paper.questions ?? []
      total.value = paper.total ?? 0
      nextSequence.value = paper.nextSequence ?? 0
      currentIndex.value = 0
      answers.value = {}
      judged.value = {}
      pendingRecords.value = []
      localCorrectCount.value = 0
      markQuestionEntered()
      return paper
    } finally {
      loading.value = false
    }
  }

  /**
   * 批量上报 `pendingRecords`（单次 ≤50 条），成功后清空并覆盖本地正确数。
   *
   * 失败（含 `1002` 限流）**保留** `pendingRecords` 等下一次防抖/离开页面时重试
   * （07 §7.5.1 专项）。所以这里自己吞掉错误，调用方可以 `void flushRecords()`。
   */
  async function flushRecords(source?: PracticeSource): Promise<void> {
    const resolvedSource = source ?? SOURCE_BY_MODE[mode.value]
    // SAMPLE（游客）不上报；没有待上报项时也不发请求
    if (!resolvedSource || pendingRecords.value.length === 0) return

    // 提前取快照：请求飞行期间 reset() 可能把 state 清掉
    const subjectIdValue = subjectId.value
    const items = [...pendingRecords.value]

    try {
      let correctCount = localCorrectCount.value
      for (let start = 0; start < items.length; start += RECORD_BATCH_SIZE) {
        const result = await submitPracticeRecords({
          subjectId: subjectIdValue,
          source: resolvedSource,
          items: items.slice(start, start + RECORD_BATCH_SIZE),
        })
        correctCount = result.correctCount
      }
      pendingRecords.value = []
      localCorrectCount.value = correctCount
    } catch (error) {
      console.warn('[practice] 答题记录上报失败，已保留待重试', error)
    }
  }

  /**
   * 05 §5.6 第 14 条。返回**数组**（缺省 `mode` 时是该科目全部模式的游标）。
   * 顺序练习页自己在返回里挑 `mode === 'SEQUENCE'` 的那条。
   */
  function fetchProgress(subjectIdValue: string, options?: RequestOptions): Promise<PracticeProgress[]> {
    return fetchPracticeProgress(subjectIdValue, undefined, options)
  }

  // 游标防抖：timer 与"待落库的题序"都放在闭包里，不暴露给页面。
  let progressTimer: ReturnType<typeof setTimeout> | null = null
  let pendingSequence = 0

  /** 记下要落库的题序；3 秒内的连续切题只发一次 PUT。 */
  function saveProgress(subjectIdValue: string, seq: number): void {
    // 只有顺序练习维护游标（05 §5.6 第 15 条）
    if (mode.value !== 'SEQUENCE') return
    pendingSequence = seq
    if (progressTimer) clearTimeout(progressTimer)
    progressTimer = setTimeout(() => {
      progressTimer = null
      void flushProgress(subjectIdValue)
    }, PROGRESS_DEBOUNCE_MS)
  }

  /** 取消防抖并立即落库（离开页面 / 结束练习时调用）。失败只记日志，不阻断离开。 */
  async function flushProgress(subjectIdValue: string = subjectId.value): Promise<void> {
    if (progressTimer) {
      clearTimeout(progressTimer)
      progressTimer = null
    }
    if (mode.value !== 'SEQUENCE' || !subjectIdValue) return

    try {
      await savePracticeProgress({
        subjectId: subjectIdValue,
        mode: 'SEQUENCE',
        refId: refId.value ?? undefined,
        lastSequence: pendingSequence,
        lastQuestionId: currentQuestion.value?.id,
      })
    } catch (error) {
      console.warn('[practice] 练习进度保存失败', error)
    }
  }

  /**
   * 收藏/取消收藏当前题（乐观更新）。
   *
   * 两个接口都是幂等的（05 §5.7），所以可以先改本地、失败再回滚；
   * 回滚后把错误抛给页面，由拦截器的 toast 讲（这一处是行内动作，没有可挂的面板）。
   */
  async function toggleFavorite(index: number): Promise<void> {
    const question = questions.value[index]
    if (!question) return

    const nextFavorited = !question.isFavorited
    question.isFavorited = nextFavorited
    try {
      if (nextFavorited) await addFavorite(question.id)
      else await removeFavorite(question.id)
    } catch (error) {
      question.isFavorited = !nextFavorited
      throw error
    }
  }

  /** 清空会话状态。页面卸载与重新出题前调用。 */
  function reset(): void {
    if (progressTimer) {
      clearTimeout(progressTimer)
      progressTimer = null
    }
    pendingSequence = 0
    subjectId.value = ''
    refId.value = null
    questions.value = []
    answers.value = {}
    judged.value = {}
    currentIndex.value = 0
    total.value = 0
    nextSequence.value = 0
    pendingRecords.value = []
    localCorrectCount.value = 0
    loading.value = false
  }

  return {
    // state
    subjectId,
    mode,
    refId,
    questions,
    answers,
    judged,
    currentIndex,
    total,
    nextSequence,
    loading,
    pendingRecords,
    localCorrectCount,
    // getters
    currentQuestion,
    currentAnswer,
    currentJudged,
    answeredCount,
    hasPendingRecords,
    // actions
    judgeLocally,
    answerCurrent,
    next,
    prev,
    fetchPaper,
    flushRecords,
    fetchProgress,
    saveProgress,
    flushProgress,
    toggleFavorite,
    reset,
  }
})