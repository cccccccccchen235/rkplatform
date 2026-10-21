// 科目（07 §7.4.2）。

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  fetchSubjectDetail,
  fetchSubjectKnowledgePoints,
  fetchSubjectOutline,
  fetchSubjects,
  type SubjectListQuery,
} from '@/api/subject'
import type { GroupedSubjects, KnowledgePointVO, OutlineNodeVO, SubjectLevel, SubjectVO } from '@/types/api'
import { SUBJECT_LEVEL_ORDER } from '@/types/labels'
import type { RequestOptions } from '@/utils/request'

/**
 * 缓存时长。科目、大纲、知识点几乎不变（07 §7.10），
 * 5 分钟内重复进入首页/科目列表不再打接口。
 */
const CACHE_TTL_MS = 5 * 60 * 1000

const EMPTY_GROUPED: GroupedSubjects = { JUNIOR: [], MIDDLE: [], SENIOR: [] }

export const useSubjectStore = defineStore('subject', () => {
  // ---------------- state ----------------

  const grouped = ref<GroupedSubjects>({ ...EMPTY_GROUPED })

  const loading = ref(false)

  /** 本次数据是否来自服务端而非缓存。空态文案需要区分"还没有数据"与"筛完为空"。 */
  const loaded = ref(false)

  /**
   * 详情页当前科目，**含 `paperSummary`**（只有详情接口填充它）。
   *
   * 与 `flatList` 里那一份是两条来路：列表里那份没有卷面结构，
   * 而详情页要渲染场次与分节。所以不能"从列表里挑一个"来代替它。
   */
  const currentSubject = ref<SubjectVO | null>(null)

  /** 当前科目的章节树（根节点数组）。空数组 = 该科目还没有章节，不是"取不到"。 */
  const outlineTree = ref<OutlineNodeVO[]>([])

  const knowledgePoints = ref<KnowledgePointVO[]>([])

  let cachedAt = 0

  // ---------------- getters ----------------

  /** 展平成一维，供下拉框与"全部科目"入口使用。顺序固定为 初→中→高。 */
  const flatList = computed<SubjectVO[]>(() =>
    SUBJECT_LEVEL_ORDER.flatMap((level) => grouped.value[level] ?? []),
  )

  /**
   * 已挂载启用中卷面模板的科目 = 能发起模拟考的科目。
   *
   * 07 §7.4.2 把它写成 `flatList.filter(s => s.paperSummary != null)`，
   * 那个写法**恒等于空数组**：列表接口对 paperSummary 一律返回 null
   * （SubjectVO 的 javadoc 写明"列表接口返回 null，仅 GET /subjects/{id} 填充"），
   * 而空数组在模拟考页上会被渲染成"没有任何科目可以考"——一个听起来像事实的错误结论。
   *
   * 现在按 `paperTotalScore != null` 判断（docs/18 的 F3：后端给列表接口加了那个字段）。
   * 它和 `paperSummary != null` 表达同一件事，但那个 null 是"接口没带"，
   * 这个 null 是"库里没有"——只有后者能拿来判断资格。
   *
   * 注意它今天等于**全部 20 个科目**（init-data.sql 给每个科目都配了卷面模板），
   * 所以它不是"内容完备度"的指标；那是 outlineStatus / contentStatus 的事。
   * 它的用途是：将来某个科目下架了模板，模拟考入口要跟着消失。
   */
  const configuredSubjects = computed<SubjectVO[]>(() =>
    flatList.value.filter((subject) => subject.paperTotalScore != null),
  )

  /**
   * 大纲完整的科目。
   *
   * 首页书架用它决定哪些书脊是"已印"的蓝脊。**刻意用 outlineStatus 而不是
   * contentStatus**：题库状态字段写着 BULK（已成规模），而同一行返回的
   * questionCount 是 0——两个数字放在一起会自相矛盾。大纲状态没有这个冲突，
   * 而且它正是"这门课现在能不能真的学"的第一道门。
   */
  const printedSubjects = computed<SubjectVO[]>(() =>
    flatList.value.filter((subject) => subject.outlineStatus === 'FULL'),
  )

  /** 三个级别里一条科目都没有。整页空态据此判断。 */
  const isEmpty = computed(() => flatList.value.length === 0)

  // ---------------- actions ----------------

  /**
   * 拉取科目列表。
   *
   * `force = true` 用于"我要看最新数据"的场景（下拉刷新、发布后的管理端回看）；
   * 默认走 5 分钟缓存，因为这是首页与科目列表页的公共数据，
   * 每次路由切换都重拉会让首屏多一个无谓的等待。
   *
   * `options` 只往下透传（当前唯一的用处是 `silent`）：缓存命中时**不发请求**，
   * 所以传了也不会有什么被"记住"。
   */
  async function fetchGrouped(query: SubjectListQuery = {}, force = false, options?: RequestOptions): Promise<GroupedSubjects> {
    const fresh = Date.now() - cachedAt < CACHE_TTL_MS
    if (!force && fresh && loaded.value) {
      return grouped.value
    }

    loading.value = true
    try {
      const data = await fetchSubjects(query, options)
      // 接口保证三个键都存在，但契约之外的情况（例如后端将来少发一个键）
      // 不该让页面在 `grouped[level].length` 上崩掉，这里补一次归一化。
      grouped.value = {
        JUNIOR: data.JUNIOR ?? [],
        MIDDLE: data.MIDDLE ?? [],
        SENIOR: data.SENIOR ?? [],
      }
      loaded.value = true
      cachedAt = Date.now()
      return grouped.value
    } finally {
      loading.value = false
    }
  }

  /** 清缓存。当前科目变更、或管理端改过科目后调用。 */
  function invalidate(): void {
    cachedAt = 0
  }

  /** 按级别取一组，供列表页的三级分组渲染（不依赖对象键序）。 */
  function byLevel(level: SubjectLevel): SubjectVO[] {
    return grouped.value[level] ?? []
  }

  /**
   * 拉取科目详情（05 §5.4 第 9 条）。**不走缓存**：详情页是"我要看这门课"，
   * 5 分钟的旧数据在这里没有收益，反而会在管理端改完科目后让人以为改动没生效。
   */
  async function fetchSubjectDetailById(id: string, options?: RequestOptions): Promise<SubjectVO> {
    const detail = await fetchSubjectDetail(id, options)
    currentSubject.value = detail
    return detail
  }

  async function fetchOutline(id: string, options?: RequestOptions): Promise<OutlineNodeVO[]> {
    const tree = await fetchSubjectOutline(id, options)
    outlineTree.value = tree ?? []
    return outlineTree.value
  }

  async function fetchKnowledgePoints(id: string, options?: RequestOptions): Promise<KnowledgePointVO[]> {
    const list = await fetchSubjectKnowledgePoints(id, options)
    knowledgePoints.value = list ?? []
    return knowledgePoints.value
  }

  /** 离开详情页时清掉，避免"上一门科目的树闪现在下一门课页面上"。 */
  function clearDetail(): void {
    currentSubject.value = null
    outlineTree.value = []
    knowledgePoints.value = []
  }

  return {
    grouped,
    loading,
    loaded,
    currentSubject,
    outlineTree,
    knowledgePoints,
    flatList,
    configuredSubjects,
    printedSubjects,
    isEmpty,
    fetchGrouped,
    fetchSubjectDetailById,
    fetchOutline,
    fetchKnowledgePoints,
    clearDetail,
    invalidate,
    byLevel,
  }
})
