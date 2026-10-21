// 模拟考会话（07 §7.4.4）。
//
// 这个 store 只服务"一次模拟考会话"：发起、断点续答、作答保存、场次/分节切换、交卷。
// 它的核心约束是 **C4：计时以服务端为准**——本地 `setInterval(1000)` 只推进展示用的
// `tickAt`，`displaySec` 由 getter 现算；任何写回 `remainingSec` 的地方都来自服务端响应
// （`GET` / `PUT answers` / `start` / `finish` / `advance`）。
//
// 第二个约束是 **PENDING 场次不倒计时**：高级科目下午论文场次需用户主动 start，
// 中间隔的午休不扣时（评审 A5）。所以 `startTick()` 只在当前场次 `IN_PROGRESS` 时启用。

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  abandonExam,
  advanceExamSection,
  createExam,
  fetchExamResult,
  fetchExamSession,
  finishExamSitting,
  saveExamAnswers,
  startExamSitting,
  submitExam,
} from '@/api/exam'
import type {
  ExamAnswerPayloadItem,
  ExamItemVO,
  ExamResultVO,
  ExamSectionVO,
  ExamSessionVO,
  ExamSittingVO,
} from '@/types/api'
import type { RequestError, RequestOptions } from '@/utils/request'
import { STORAGE_KEYS, storage } from '@/utils/storage'

/** 保存作答的单次上限（05 §5.6 第 19 条）；超出时前端分批。 */
const ANSWER_BATCH_SIZE = 100
/** 作答变更后合并上报的防抖时长（07 §7.5.1 专项：debounce 5s）。 */
const SAVE_DEBOUNCE_MS = 5000
/** 无变更时的心跳间隔，用于刷新 `remainingSec` 并给服务端一次超时结算机会。 */
const HEARTBEAT_MS = 60000
/** 连续保存失败达到该次数后提示"网络异常"，但不阻断本地作答。 */
const MAX_SAVE_FAILURES = 3

/** 服务端已知的"会话已结束"错误码（05 §5.1.2）。 */
const CODE_SESSION_ALREADY_ENDED = 4005

export type ExamSaveState = 'idle' | 'saving' | 'saved' | 'failed'

interface ActiveExamRecord {
  sessionId: string
  subjectId: string
  savedAt: number
}

export const useExamStore = defineStore('exam', () => {
  // ---------------- state ----------------

  const sessionId = ref('')
  const session = ref<ExamSessionVO | null>(null)
  /** itemId → 用户作答。多选存成排序后的字符串（如 "ABD"）。 */
  const answers = ref<Record<string, string>>({})
  /** 待上报的 itemId。 */
  const dirty = ref<Set<string>>(new Set())
  const currentItemId = ref('')

  const loading = ref(false)
  const submitting = ref(false)

  /** 当前场次的剩余秒数（服务端最近一次下发的值）。 */
  const remainingSec = ref(0)
  /** 收到上面那个值的本地时刻。 */
  const syncedAt = ref(0)
  /** 1 秒定时器推进的时间戳，仅驱动 `displaySec` 重算。 */
  const tickAt = ref(0)

  const saveState = ref<ExamSaveState>('idle')
  const lastSavedAt = ref<number | null>(null)

  /**
   * 会话被服务端判为结束（多为超时自动交卷，`4005`）。
   * 答题页 watch 它，跳成绩页——store 不 import router，避免机制层认识路由。
   */
  const ended = ref(false)

  // 非响应式：定时器句柄与"当前题进入时刻"。
  let tickTimer: ReturnType<typeof setInterval> | null = null
  let saveTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let itemEnteredAt = Date.now()
  let saveFailures = 0
  /** 剩余归零后只触发一次结算，避免每秒都打一次心跳。 */
  let zeroFlushDone = false

  // ---------------- getters ----------------

  const currentSitting = computed<ExamSittingVO | null>(() => {
    const vo = session.value
    if (!vo) return null
    return vo.sittings.find((sitting) => sitting.sittingNo === vo.currentSittingNo) ?? null
  })

  const currentSection = computed<ExamSectionVO | null>(() => {
    const vo = session.value
    if (!vo) return null
    return vo.sections.find((section) => section.sectionNo === vo.currentSectionNo) ?? null
  })

  const isTicking = computed(() => currentSitting.value?.status === 'IN_PROGRESS')

  /**
   * 展示用倒计时。
   *
   * `PENDING` / `FINISHED` 场次**冻结**（PENDING 恒为总时长，FINISHED 为 0），
   * 只有 `IN_PROGRESS` 才按本地流逝推进——本地时钟偏快偏慢都不参与判定。
   */
  const displaySec = computed(() => {
    if (!isTicking.value) return Math.max(0, remainingSec.value)
    const elapsed = Math.floor((tickAt.value - syncedAt.value) / 1000)
    return Math.max(0, remainingSec.value - Math.max(0, elapsed))
  })

  const answeredItemIds = computed(() => {
    const ids = new Set<string>()
    for (const [itemId, value] of Object.entries(answers.value)) {
      if (value !== '' && value != null) ids.add(itemId)
    }
    return ids
  })

  const answeredCount = computed(() => answeredItemIds.value.size)

  const unansweredCount = computed(() => {
    const total = session.value?.questionCount ?? 0
    return Math.max(0, total - answeredCount.value)
  })

  // ---------------- 本地计时 ----------------

  function startTick(): void {
    stopTick()
    tickAt.value = Date.now()
    tickTimer = setInterval(() => {
      tickAt.value = Date.now()
      // 展示到点：不本地强制交卷，只向服务端确认一次（07 §7.4.4 第 5 条）。
      if (displaySec.value <= 0 && !zeroFlushDone) {
        zeroFlushDone = true
        void flushSave()
      }
    }, 1000)
  }

  function stopTick(): void {
    if (tickTimer) {
      clearInterval(tickTimer)
      tickTimer = null
    }
  }

  function restartHeartbeat(): void {
    stopHeartbeat()
    // 无变更时每 60s 打一次空 items 的心跳，刷新 remainingSec（评审 B10）
    heartbeatTimer = setInterval(() => {
      if (isTicking.value && dirty.value.size === 0) void flushSave()
    }, HEARTBEAT_MS)
  }

  function stopHeartbeat(): void {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  // ---------------- 状态同步 ----------------

  /** 重建整个会话状态（进入页面 / 断点续答 / 切换分节后的重取）。 */
  function applySession(vo: ExamSessionVO): void {
    session.value = vo
    sessionId.value = vo.sessionId

    const restored: Record<string, string> = {}
    for (const item of vo.items) {
      if (item.userAnswer) restored[item.itemId] = item.userAnswer
    }
    answers.value = restored
    dirty.value = new Set()

    const sitting = vo.sittings.find((s) => s.sittingNo === vo.currentSittingNo) ?? null
    remainingSec.value = sitting ? sitting.remainingSec : 0
    syncedAt.value = Date.now()
    tickAt.value = syncedAt.value
    zeroFlushDone = false
    saveFailures = 0
    saveState.value = 'idle'

    if (sitting?.status === 'IN_PROGRESS') {
      startTick()
      restartHeartbeat()
    } else {
      stopTick()
      stopHeartbeat()
    }

    // 当前题：当前分节里第一道未作答的题；全答完则回到本节第一题
    const sectionItems = itemsOfSection(vo.currentSectionNo)
    currentItemId.value =
      sectionItems.find((item) => !restored[item.itemId])?.itemId ?? sectionItems[0]?.itemId ?? ''
    itemEnteredAt = Date.now()

    rememberActive(vo)
  }

  function itemsOfSection(sectionNo: number): ExamItemVO[] {
    return (session.value?.items ?? []).filter((item) => item.sectionNo === sectionNo)
  }

  /** 服务端响应回来后校准计时（不改作答与当前题）。 */
  function applyTiming(nextRemainingSec: number, nextSittingNo?: number, nextSectionNo?: number): void {
    remainingSec.value = nextRemainingSec
    syncedAt.value = Date.now()
    tickAt.value = syncedAt.value
    zeroFlushDone = false
    const vo = session.value
    if (!vo) return
    if (typeof nextSittingNo === 'number') vo.currentSittingNo = nextSittingNo
    if (typeof nextSectionNo === 'number') vo.currentSectionNo = nextSectionNo
    const sitting = vo.sittings.find((s) => s.sittingNo === vo.currentSittingNo)
    if (sitting) {
      sitting.remainingSec = nextRemainingSec
      if (typeof nextSectionNo === 'number') sitting.currentSectionNo = nextSectionNo
    }
  }

  /** 用最新 `sittings` 覆盖会话的场次与分节（finish / start 的响应都带完整场次）。 */
  function replaceSittings(sittings: ExamSittingVO[], currentSittingNo: number): void {
    const vo = session.value
    if (!vo) return
    vo.sittings = sittings
    vo.sections = sittings.flatMap((s) => s.sections).sort((a, b) => a.sectionNo - b.sectionNo)
    vo.currentSittingNo = currentSittingNo
    const sitting = sittings.find((s) => s.sittingNo === currentSittingNo) ?? null
    vo.currentSectionNo = sitting ? sitting.currentSectionNo : vo.currentSectionNo
    remainingSec.value = sitting ? sitting.remainingSec : 0
    syncedAt.value = Date.now()
    tickAt.value = syncedAt.value
    if (sitting?.status === 'IN_PROGRESS') {
      startTick()
      restartHeartbeat()
    } else {
      stopTick()
      stopHeartbeat()
    }
  }

  // ---------------- 断点续答凭据 ----------------

  function rememberActive(vo: ExamSessionVO): void {
    if (vo.status !== 'CREATED' && vo.status !== 'IN_PROGRESS') return
    const record: ActiveExamRecord = { sessionId: vo.sessionId, subjectId: vo.subjectId, savedAt: Date.now() }
    storage.set(STORAGE_KEYS.examActive, record)
  }

  function forgetActive(): void {
    storage.remove(STORAGE_KEYS.examActive)
  }

  function readActive(): ActiveExamRecord | null {
    return storage.get<ActiveExamRecord>(STORAGE_KEYS.examActive)
  }

  // ---------------- 发起 / 载入 ----------------

  /** 05 §5.6 第 16 条。发起成功即记住会话，供 4003 冲突对话框找回。 */
  async function create(subjectId: string, options?: RequestOptions): Promise<ExamSessionVO> {
    const vo = await createExam(subjectId, options)
    applySession(vo)
    return vo
  }

  /** 05 §5.6 第 18 条。断点续答入口。 */
  async function loadSession(id: string, options?: RequestOptions): Promise<ExamSessionVO> {
    loading.value = true
    try {
      const vo = await fetchExamSession(id, options)
      applySession(vo)
      if (vo.status === 'SUBMITTED' || vo.status === 'SCORED' || vo.status === 'ABANDONED') {
        forgetActive()
        stopTick()
        stopHeartbeat()
      }
      return vo
    } finally {
      loading.value = false
    }
  }

  // ---------------- 作答 ----------------

  function setCurrentItem(itemId: string): void {
    currentItemId.value = itemId
    itemEnteredAt = Date.now()
  }

  function setAnswer(itemId: string, value: string): void {
    answers.value = { ...answers.value, [itemId]: value }
    const next = new Set(dirty.value)
    next.add(itemId)
    dirty.value = next
    saveState.value = 'idle'
    scheduleSave()
  }

  function scheduleSave(): void {
    if (saveTimer) clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      saveTimer = null
      void flushSave()
    }, SAVE_DEBOUNCE_MS)
  }

  /**
   * 取消防抖、立刻上报全部 `dirty` 项；无 `dirty` 时按空 `items` 心跳刷新 `remainingSec`。
   *
   * 失败时不抛给调用方（页面在离开/交卷路径上 `void` 调用它）；连续失败达 3 次
   * 才把 `saveState` 置为 `failed`，由页面提示"网络异常"，本地作答不受阻断。
   */
  async function flushSave(): Promise<void> {
    const id = sessionId.value
    if (!id || !session.value) return
    if (session.value.status === 'SUBMITTED' || session.value.status === 'SCORED' || session.value.status === 'ABANDONED') {
      return
    }
    if (saveTimer) {
      clearTimeout(saveTimer)
      saveTimer = null
    }

    const ids = [...dirty.value]
    const snapshot: ExamAnswerPayloadItem[] = ids.map((itemId) => ({
      itemId,
      userAnswer: answers.value[itemId] ?? '',
      durationMs: itemId === currentItemId.value ? Math.max(0, Date.now() - itemEnteredAt) : undefined,
    }))

    saveState.value = 'saving'
    try {
      let latest = { remainingSec: remainingSec.value, currentSittingNo: session.value.currentSittingNo, currentSectionNo: session.value.currentSectionNo }
      for (let start = 0; start < Math.max(snapshot.length, 1); start += ANSWER_BATCH_SIZE) {
        const batch = snapshot.slice(start, start + ANSWER_BATCH_SIZE)
        latest = await saveExamAnswers(id, { items: batch })
        // 只有"这一批确实包含当前正在编辑的题"才把它的用时清零，避免心跳把用时抹掉
        if (batch.length > 0) itemEnteredAt = Date.now()
      }
      // 上报成功：只把"提交值仍是当前值"的 itemId 从 dirty 移除。
      // 请求飞行期间用户又改了同一题时，answers 里已是新值，这里保留它的 dirty，
      // 否则新值会被这次旧请求的成功回调吞掉（一道很窄但真实的竞态）。
      const submitted = new Map(snapshot.map((item) => [item.itemId, item.userAnswer]))
      const next = new Set(dirty.value)
      submitted.forEach((sentValue, itemId) => {
        if ((answers.value[itemId] ?? '') === sentValue) next.delete(itemId)
      })
      dirty.value = next
      saveFailures = 0
      saveState.value = 'saved'
      lastSavedAt.value = Date.now()
      applyTiming(latest.remainingSec, latest.currentSittingNo, latest.currentSectionNo)
    } catch (error) {
      await handleSaveError(error as RequestError)
    }
  }

  async function handleSaveError(error: RequestError): Promise<void> {
    if (error.code === CODE_SESSION_ALREADY_ENDED) {
      // 超时已被服务端自动交卷（或本场次结束）：重取会话，让页面按终态跳转
      stopTick()
      stopHeartbeat()
      ended.value = true
      return
    }
    saveFailures += 1
    saveState.value = 'failed'
    if (saveFailures >= MAX_SAVE_FAILURES) {
      console.warn('[exam] 作答保存连续失败，保留待重试', error)
    } else {
      console.warn('[exam] 作答保存失败，将重试', error)
      // 保留 dirty，稍后再试一次
      scheduleSave()
    }
  }

  /** 供页面在剩余归零 / 保存失败提示上手动重试。 */
  function retrySave(): void {
    saveFailures = 0
    void flushSave()
  }

  // ---------------- 场次与分节 ----------------

  /** 05 §5.6 第 20 条。`PENDING → IN_PROGRESS`；成功后才启动本地倒计时（评审 A5）。 */
  async function startSitting(sittingNo: number, options?: RequestOptions): Promise<void> {
    const vo = session.value
    if (!vo) return
    await flushSave()
    const result = await startExamSitting(vo.sessionId, sittingNo, options)
    vo.sittings = vo.sittings.map((sitting) => (sitting.sittingNo === sittingNo ? result.sitting : sitting))
    vo.sections = vo.sittings.flatMap((s) => s.sections).sort((a, b) => a.sectionNo - b.sectionNo)
    vo.currentSittingNo = sittingNo
    vo.currentSectionNo = result.currentSectionNo
    vo.status = 'IN_PROGRESS'
    applyTiming(result.remainingSec, sittingNo, result.currentSectionNo)
    startTick()
    restartHeartbeat()
    const sectionItems = itemsOfSection(result.currentSectionNo)
    currentItemId.value =
      sectionItems.find((item) => !answers.value[item.itemId])?.itemId ?? sectionItems[0]?.itemId ?? ''
    itemEnteredAt = Date.now()
  }

  /** 05 §5.6 第 21 条。离开当前分节；响应缺 `currentSittingNo`，故重取一次会话。 */
  async function advanceSection(sectionNo: number, options?: RequestOptions): Promise<void> {
    const vo = session.value
    if (!vo) return
    await flushSave()
    await advanceExamSection(vo.sessionId, sectionNo, options)
    await loadSession(vo.sessionId, { silent: true })
  }

  /** 05 §5.6 第 22 条。提前结束整个场次。 */
  async function finishSitting(sittingNo: number, options?: RequestOptions): Promise<void> {
    const vo = session.value
    if (!vo) return
    await flushSave()
    const result = await finishExamSitting(vo.sessionId, sittingNo, options)
    replaceSittings(result.sittings, result.currentSittingNo)
    // 下一场次为 PENDING：把当前题指向它第一节的第一题，等用户点「开始本场次」
    const nextSitting = session.value?.sittings.find((s) => s.sittingNo === result.currentSittingNo)
    if (nextSitting) {
      const sectionItems = itemsOfSection(nextSitting.currentSectionNo)
      currentItemId.value =
        sectionItems.find((item) => !answers.value[item.itemId])?.itemId ?? sectionItems[0]?.itemId ?? ''
    }
    if (session.value) rememberActive(session.value)
  }

  // ---------------- 交卷 / 放弃 ----------------

  /** 05 §5.6 第 23 条。先保存再交卷；服务端幂等，`submitting` 只防连点。 */
  async function submit(options?: RequestOptions): Promise<ExamResultVO> {
    const vo = session.value
    if (!vo) throw new Error('没有进行中的模拟考会话')
    if (submitting.value) throw new Error('正在交卷')
    submitting.value = true
    try {
      await flushSave()
      const result = await submitExam(vo.sessionId, options)
      vo.status = 'SCORED'
      stopTick()
      stopHeartbeat()
      forgetActive()
      return result
    } finally {
      submitting.value = false
    }
  }

  /** 05 §5.6 第 24 条。成绩详情（只读，不进主状态）。 */
  function fetchResult(id: string, options?: RequestOptions): Promise<ExamResultVO> {
    return fetchExamResult(id, options)
  }

  /** 05 §5.6 第 25 条。放弃；不计入模考统计。 */
  async function abandon(id: string = sessionId.value, options?: RequestOptions): Promise<void> {
    await abandonExam(id, options)
    stopTick()
    stopHeartbeat()
    forgetActive()
    if (session.value?.sessionId === id) session.value.status = 'ABANDONED'
  }

  /** 清空会话状态。页面卸载与重新发起前调用。 */
  function reset(): void {
    stopTick()
    stopHeartbeat()
    if (saveTimer) {
      clearTimeout(saveTimer)
      saveTimer = null
    }
    sessionId.value = ''
    session.value = null
    answers.value = {}
    dirty.value = new Set()
    currentItemId.value = ''
    remainingSec.value = 0
    syncedAt.value = 0
    tickAt.value = 0
    saveState.value = 'idle'
    lastSavedAt.value = null
    ended.value = false
    submitting.value = false
    zeroFlushDone = false
    saveFailures = 0
  }

  return {
    // state
    sessionId,
    session,
    answers,
    dirty,
    currentItemId,
    loading,
    submitting,
    remainingSec,
    syncedAt,
    tickAt,
    saveState,
    lastSavedAt,
    ended,
    // getters
    currentSitting,
    currentSection,
    isTicking,
    displaySec,
    answeredItemIds,
    answeredCount,
    unansweredCount,
    // actions
    create,
    loadSession,
    setCurrentItem,
    setAnswer,
    scheduleSave,
    flushSave,
    retrySave,
    startSitting,
    advanceSection,
    finishSitting,
    submit,
    fetchResult,
    abandon,
    readActive,
    forgetActive,
    reset,
  }
})
