// ===============================================================
// 接口类型。手写维护，字段与 docs/05-api.md 完全一致（07 §7.11）。
//
// 05 是唯一契约来源：本文件与它不一致时，改的是本文件。
// 不自动生成类型（openapi-typescript 等）：生成链要求构建期后端可访问，
// 且 Java 视角的 Long 会映射成 number 而本项目 ID 一律走 string（05 §5.1）。
//
// 约定：所有 ID 是 string（雪花 ID 超出 JS 安全整数范围）；
// 所有时间是 string，格式 "yyyy-MM-dd HH:mm:ss"（05 §5.1）。
// ===============================================================

// 练习模式是前端枚举（domain.d.ts），但它是接口字段的取值域（05 §5.6 第 12/14 条），
// 所以这里引用它而不是再写一遍字符串联合——两处各写一份迟早会分叉。
import type { PracticeMode } from './domain'

/** 统一响应体（05 §5.1.1）。页面拿不到它——request.ts 已解包成 data。 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  traceId: string
}

/** 分页响应（05 §5.1.3）。所有列表接口都分页，pageSize 上限 100（AC-4）。 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// ---------------- 枚举（唯一来源：02 §2.2 业务规则） ----------------

export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'TRUE_FALSE' | 'SUBJECTIVE' | 'ESSAY'
export type QuestionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'PUBLISHED' | 'OFFLINE'
export type ExamStatus = 'CREATED' | 'IN_PROGRESS' | 'SUBMITTED' | 'SCORED' | 'ABANDONED'
export type SectionType = 'OBJECTIVE' | 'SUBJECTIVE' | 'ESSAY'
export type SupportStatus = 'SUPPORTED' | 'NOT_SUPPORTED'
export type SubjectLevel = 'JUNIOR' | 'MIDDLE' | 'SENIOR'
export type UserRole = 'USER' | 'ADMIN'

// ---------------- 认证与用户（05 §5.3） ----------------

/**
 * 05 §5.3 第 4 条的 UserVO 字段表。
 *
 * 注意 `mustChangePassword` 是**必填**而不是可选：它由 05 §5.3 明确定义，
 * 前端凭它强制跳转改密页（评审 C2）。07 §7.11 的示例片段漏了这个字段，
 * 以 05 为准（两处出入已记入 docs/18）。
 */
export interface UserVO {
  id: string
  username: string
  nickname: string
  role: UserRole
  status: string
  currentSubjectId?: string
  mustChangePassword: boolean
  lastLoginAt: string
  createdAt: string
}

/** 注册/登录的响应 data（05 §5.3 第 1、2 条）。 */
export interface AuthResultVO {
  token: string
  /** 会话到期时刻。服务端无法给出（会话永不过期）时为 null，前端不得据此假定失效。 */
  expireAt: string | null
  user: UserVO
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
}

export interface LoginPayload {
  username: string
  password: string
}

// ---------------- 个人中心（05 §5.3 第 5~7 条） ----------------
// 05 只给了这三条的路径与方法，没有给请求体字段表；字段名与 UserVO / 注册请求保持一致，
// 由 docs/33 登记这条偏离。

/** 改昵称（`PUT /users/me`）。上限 32 与注册时一致。 */
export interface NicknameUpdatePayload {
  nickname: string
}

/** 改密码（`PUT /users/me/password`）。新密码复杂度与注册口径相同（8~32 位含字母与数字）。 */
export interface PasswordUpdatePayload {
  oldPassword: string
  newPassword: string
}

/** 设置当前备考科目（`PUT /users/me/current-subject`）。`subjectId` 是字符串（05 §5.1 的 ID 约定）。 */
export interface CurrentSubjectPayload {
  subjectId: string
}

// ---------------- 科目与大纲（05 §5.4） ----------------

export interface PaperSectionSummary {
  sectionNo: number
  sectionName: string
  sectionType: SectionType
  questionCount: number
  supportStatus: SupportStatus
}

/** 卷面摘要里的一场考试（`sittingNo` 从 1 连续递增）。 */
export interface PaperSittingSummary {
  sittingNo: number
  sittingName: string
  /** 本场次时长（分钟）。`PaperSummary.totalDuration` 是各场次之和。 */
  totalDuration: number
  totalMaxScore: number
  autoScoredMaxScore: number
  /** 本场次的分节。`sectionNo` **跨场次全局连续**（如高级为 1/2/3）。 */
  sections: PaperSectionSummary[]
}

/**
 * 卷面摘要。`null` 表示该科目尚未配置模板（05 §5.2 里它是可选字段，**不是空对象**）。
 *
 * **有 `sittings` 这一层**（05 §5.2）：高级科目是两场（上午连考 + 下午论文），
 * 论文独立成场次、时长不与上午共享，所以时长与满分都必须按场次给，
 * 模板级只放合计。07 §7.11 的示例片段写的是平铺 `sections`，以 05 为准。
 */
export interface PaperSummary {
  /** 各场次时长之和（分钟）。 */
  totalDuration: number
  totalMaxScore: number
  autoScoredMaxScore: number
  sittings: PaperSittingSummary[]
}

export interface SubjectVO {
  id: string
  level: SubjectLevel
  code: string
  name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'
  examPeriod: 'BOTH' | 'FIRST_HALF' | 'SECOND_HALF'
  /** FULL = 完整章节树；SKELETON = 仅骨架，页面需提示并只给模拟考/顺序练习入口。 */
  outlineStatus: 'FULL' | 'SKELETON'
  /** SAMPLE = 仅有样例题；BULK = 已成规模。 */
  contentStatus: 'SAMPLE' | 'BULK'
  questionCount: number
  /**
   * 卷面满分（225 / 150 / 75）。**列表接口就返回**（05 §5.2 的字段表在本次改动前
   * 没有这一项，见 docs/18 的 F3）。
   *
   * `null` = 该科目没有启用中的卷面模板。**不要用 `paperType` 换算它**：
   * `paperType` 只有 JUNIOR_MID / SENIOR 两值，而 IPT 的满分是 75 不是 150
   * （它的「应用技术」节不予机考，不计入满分）——那是数据事实，
   * 让前端写死 `code === 'IPT'` 是把库里的规则搬进视图层。
   */
  paperTotalScore?: number | null
  intro?: string
  paperSummary?: PaperSummary
}

/** GET /subjects 的 data：按级别分组（05 §5.4 第 8 条）。 */
export type GroupedSubjects = Record<SubjectLevel, SubjectVO[]>

export interface OutlineNodeVO {
  id: string
  parentId: string
  level: number
  name: string
  sortNo: number
  questionCount: number
  children: OutlineNodeVO[]
}

/** 知识点标签（05 §5.2）。被公开接口 11 与管理接口 46/47/48 复用。 */
export interface KnowledgePointVO {
  id: string
  subjectId: string
  name: string
  /**
   * 同义词，**逗号分隔的单个字符串，不是数组**（05 §5.2）。
   * 库里就是一列；前端把它拆成数组再拼回去，只会在两端各多一次转换，
   * 而唯一需要编辑它的地方（管理端）本来就是当字符串展示的。
   */
  aliases?: string
  questionCount: number
  status: EnabledStatus
}

// ---------------- 管理端 · 内容维护（05 §5.10，接口 37~50） ----------------

/** 启用/停用。科目与知识点共用（05 §5.2 的两个 VO 都叫 status）。 */
export type EnabledStatus = 'ENABLED' | 'DISABLED'

/** 章节出处。OFFICIAL_TITLE = 取自官方大纲的标题短语；MANUAL = 人工新增。 */
export type OutlineOrigin = 'OFFICIAL_TITLE' | 'MANUAL'

/**
 * 管理端科目列表行（05 §5.2 `AdminSubjectVO`）。
 *
 * 与 `SubjectVO` 的差别是**多一个 `status`、多一个 `sortNo`、没有 `paperSummary`**：
 * 考生端只见 `ENABLED`，那个字段放进 `SubjectVO` 是恒定值的噪音；
 * 而管理端必须能看出哪一行已停用——这是它存在的唯一理由。
 */
export interface AdminSubjectVO {
  id: string
  level: SubjectLevel
  code: string
  name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'
  examPeriod: 'BOTH' | 'FIRST_HALF' | 'SECOND_HALF'
  outlineStatus: 'FULL' | 'SKELETON'
  contentStatus: 'SAMPLE' | 'BULK'
  questionCount: number
  paperTotalScore?: number | null
  intro?: string
  sortNo: number
  status: EnabledStatus
}

/**
 * 新增/修改科目的请求体（05 §5.10 第 38/39 条）。
 *
 * `level` 在**修改时被忽略**（05 §5.10 明写"创建后不可改"）——级别决定卷面类型，
 * 改它会架空已有模板。所以它在 `PUT` 表单里是只读的。
 */
export interface SubjectSavePayload {
  level: SubjectLevel
  code: string
  name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'
  examPeriod: 'BOTH' | 'FIRST_HALF' | 'SECOND_HALF'
  outlineStatus: 'FULL' | 'SKELETON'
  contentStatus: 'SAMPLE' | 'BULK'
  intro?: string
}

/** 新增/修改章节的请求体（05 §5.10 第 42/43 条）。 */
export interface OutlineNodeSavePayload {
  subjectId: string
  /** 父章节；根传 `"0"`（04 §4.5：0 为根，不是 null）。 */
  parentId: string
  name: string
  sortNo?: number
  origin?: OutlineOrigin
  /** `level` 不在这里：它由 `parentId` 推导，提交它只会多一个可能说错话的输入。 */
}

/** 批量排序的一项（05 §5.10 第 45 条）。整棵树的新顺序是全量提交，不是增量。 */
export interface OutlineSortItem {
  id: string
  parentId: string
  sortNo: number
}

/** 新增/修改知识点的请求体（05 §5.10 第 47/48 条）。 */
export interface KnowledgePointSavePayload {
  subjectId: string
  name: string
  aliases?: string
}

/** 合并标签的请求体（05 §5.10 第 50 条）。 */
export interface KnowledgePointMergePayload {
  sourceIds: string[]
  targetId: string
}

// ---------------- 管理端 · 卷面模板配置（05 §5.10，接口 51~52） ----------------
// GET 的响应与 PUT 的请求体共用下面这组类型（GET 多出的 `subjectId` / `totalDuration`
// 是只读派生字段，PUT 时被服务端忽略），所以"回填即可保存"是类型层就成立的。

/**
 * 卷面分节配置（05 §5.10 第 51/52 条）。
 *
 * `durationMin` / `durationMax` **均必填**（对应评审 A6）：
 * 场次内总时长共享，分节只约束"最早/最晚可离开"的时间点。
 */
export interface PaperSectionConfigVO {
  /** 跨场次全局连续（如高级为 1/2/3）。 */
  sectionNo: number
  sectionName: string
  sectionType: SectionType
  questionType: QuestionType
  questionCount: number
  perQuestionScore: number
  durationMin: number
  durationMax: number
  /** 论文节为 `false`（05 §5.10 示例二）。 */
  allowEarlySubmit: boolean
  /** `NOT_SUPPORTED` 的分节不参与组卷、不计入任何满分口径（02 §2.4.4）。 */
  supportStatus: SupportStatus
  sortNo: number
}

/**
 * 卷面场次配置（05 §5.10 第 51/52 条）。
 *
 * `totalMaxScore` / `autoScoredMaxScore` 由服务端按分节重算并**比对**，
 * 对不上即 1001（10 §10.7 第一条验收项）——页面因此把这三个合计值做成只读派生值，
 * 不把"让用户自己算总分"这种易错事交给人。
 */
export interface PaperSittingConfigVO {
  /** 从 1 连续递增。 */
  sittingNo: number
  sittingName: string
  /** 本场次总时长（分钟）；场次内分节共享。 */
  totalDuration: number
  totalMaxScore: number
  autoScoredMaxScore: number
  sortNo: number
  sections: PaperSectionConfigVO[]
}

/** 管理端卷面模板详情（05 §5.10 第 51 条）。 */
export interface PaperTemplateDetailVO {
  subjectId: string
  name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'
  /** 各场次时长之和（分钟），只读派生。 */
  totalDuration: number
  totalMaxScore: number
  autoScoredMaxScore: number
  sittings: PaperSittingConfigVO[]
}

/** 整体覆盖式保存请求体（05 §5.10 第 52 条）。模板级没有 `totalDuration`。 */
export interface PaperTemplateSavePayload {
  name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'
  totalMaxScore: number
  autoScoredMaxScore: number
  sittings: PaperSittingConfigVO[]
}

/** 题量不足警告（05 §5.10 第 52 条，`insufficientQuestions` 的单项）。 */
export interface InsufficientQuestionVO {
  sectionNo: number
  sectionName: string
  requiredCount: number
  availableCount: number
}

/**
 * 保存结果。保存成功（`code = 0`）也可能带警告——
 * 题量不足是"先配卷面、后补题库"的正常流程，不阻断保存。
 */
export interface PaperTemplateSaveResultVO {
  insufficientQuestions: InsufficientQuestionVO[]
}

// ---------------- 练习 / 错题本 / 收藏 / 反馈（05 §5.6、§5.7、§5.9） ----------------
// 这一组接口的后端已实现并验收（docs/25）。其余里程碑（模拟考、批次、审核、
// 用户管理、卷面模板配置）仍未实现：这里不写"看起来合理"的占位声明——手写类型的
// 价值在于逐字段对照 05，凭空补出来的类型会让人以为接口已经可用。

/** 题干选项（05 §5.2 的 `options`）。只有客观题有；主观题/论文为 `null`。 */
export interface QuestionOptionVO {
  key: string
  content: string
}

/** 题干图片（由后端静态托管，见 05 §5.10 第 51 项）。 */
export interface QuestionImageVO {
  url: string
  alt: string
}

/** 题目的知识点标签（只带 id 与 name，不含停用状态）。 */
export interface QuestionKpVO {
  id: string
  name: string
}

/**
 * 题目（05 §5.2）。**不含答案字段**——考试答题与收藏列表都用它。
 *
 * `outlinePath` 是从根到叶的章节名数组，页面直接 `join(' / ')` 展示：
 * 自己拿 `outlineId` 去大纲树里找会要求先加载整棵树，而列表页并不需要它。
 */
export interface QuestionVO {
  id: string
  subjectId: string
  outlineId: string
  outlinePath: string[]
  questionType: QuestionType
  stem: string
  images: QuestionImageVO[]
  difficulty: number
  /** 客观题的选项；主观题/论文为 `null`（不是空数组，见 05 §5.2）。 */
  options: QuestionOptionVO[] | null
  knowledgePoints: QuestionKpVO[]
  isFavorited: boolean
}

/**
 * 含答案与解析的题目（05 §5.2）。练习出题与成绩回顾用它。
 *
 * `isCorrect` / `userAnswer` 在出题响应里为 `null`（用户还没作答）：
 * 判分由前端 `judgeLocally` 给出（07 §7.4.3），上报后以服务端复核为准。
 */
export interface QuestionDetailVO extends QuestionVO {
  correctAnswer?: string | null
  referenceAnswer?: string | null
  scorePoints?: string[] | null
  analysis?: string | null
  isCorrect?: boolean | null
  userAnswer?: string | null
}

/** 练习出题的请求体（05 §5.6 第 12 条）。 */
export interface PracticePaperPayload {
  subjectId: string
  mode: PracticeMode
  /** `CHAPTER` 传章节 ID；`KP` 传知识点 ID。 */
  refId?: string
  count: number
  difficulty?: number
  onlyUnmastered?: boolean
  /** `SEQUENCE` 模式的续做起点（从 0 开始）。 */
  fromSequence?: number
}

/** 练习出题的响应（05 §5.6 第 12 条）。 */
export interface PracticePaper {
  questions: QuestionDetailVO[]
  total: number
  nextSequence: number
}

/** 答题上报的来源（05 §5.6 第 13 条）。不含 `SAMPLE`——游客样例题不上报（07 §7.4.3）。 */
export type PracticeSource =
  | 'PRACTICE_CHAPTER'
  | 'PRACTICE_KP'
  | 'PRACTICE_SEQUENCE'
  | 'PRACTICE_FAVORITE'
  | 'WRONG_REDO'

/** 单条答题记录。**不含 `isCorrect`**：服务端会重新判分（05 §5.6 第 13 条）。 */
export interface PracticeRecordItem {
  questionId: string
  userAnswer: string
  durationMs: number
}

/** 答题上报的请求体（单次上限 50 条）。 */
export interface PracticeRecordPayload {
  subjectId: string
  source: PracticeSource
  items: PracticeRecordItem[]
}

/** 答题上报的响应。`correctCount` 是服务端复核后的正确数。 */
export interface PracticeRecordResult {
  accepted: number
  correctCount: number
}

/** 练习进度游标（05 §5.6 第 14 条）。`GET /practice/progress` 返回的是**数组**。 */
export interface PracticeProgress {
  subjectId: string
  mode: PracticeMode
  refId?: string | null
  lastSequence: number
  lastQuestionId?: string | null
  totalCount: number
  answeredCount: number
  updatedAt: string
}

/** 更新顺序练习游标的请求体（05 §5.6 第 15 条）。 */
export interface PracticeProgressPayload {
  subjectId: string
  mode: PracticeMode
  refId?: string
  lastSequence: number
  lastQuestionId?: string
}

/** 错题本条目的状态（05 §5.7 第 26 条）。 */
export type WrongQuestionStatus = 'ACTIVE' | 'MASTERED' | 'REMOVED'

/** 错题本条目（05 §5.7 第 26 条）。 */
export interface WrongQuestionVO {
  questionId: string
  stemSummary: string
  outlinePath: string[]
  wrongCount: number
  correctCount: number
  consecutiveCorrect: number
  lastAnsweredAt?: string | null
  status: WrongQuestionStatus
}

/** 题目反馈类型（05 §5.9、02 §2.6.3）。 */
export type FeedbackType = 'TYPO' | 'WRONG_ANSWER' | 'UNCLEAR_ANALYSIS' | 'COPYRIGHT' | 'OTHER'

/** 题目反馈请求体。`content` 5~1000 字（05 §5.9 第 36 条）。 */
export interface QuestionFeedbackPayload {
  type: FeedbackType
  content: string
}

// ---------------- 学习统计（05 §5.8，接口 32~35） ----------------
// 后端已实现（docs/28），故这些类型是对着 05 §5.8 的字段表逐字写出的。
// `accuracy` / `mastery` 是 0~1 的小数（服务端 BigDecimal），**不是百分比字符串**：
// 展示时用 `formatPercent()` 换算，别在类型上做手脚。

/**
 * 统计总览（05 §5.8 第 32 条）。
 *
 * 字段是**两组口径**：科目维度（做题量/正确率/掌握度/章节覆盖/模考）与用户维度
 * （学习天数/连续天数/今日）。连续天数是"我这个人"的属性，不随科目切换而变。
 */
export interface StatOverviewVO {
  answeredCount: number
  correctCount: number
  accuracy: number
  studyDays: number
  continuousDays: number
  /** 科目掌握度；无作答时是**先验 0.5**而不是 0。 */
  mastery: number
  examCount: number
  /** 最高客观题得分；未考过模拟考为 `null`。 */
  bestScore?: number | null
  /** 已练习叶子章节数 / 总叶子章节数。 */
  outlineCoverage: number
  todayAnswered: number
  todayAccuracy: number
}

/** 趋势的一个点（05 §5.8 第 33 条）。缺失日期由服务端补 0，前端直接渲染。 */
export interface StatTrendPointVO {
  /** `yyyy-MM-dd`。 */
  date: string
  answeredCount: number
  correctCount: number
  accuracy: number
  durationSec: number
}

/**
 * 章节掌握度节点（05 §5.8 第 34 条）。
 *
 * 与公开大纲的 `OutlineNodeVO` 形状相同但多三个统计字段，故独立成形（见服务端
 * `StatOutlineNodeVO` 的说明）。`questionCount` 是**本节点含子节点**的已发布题量。
 */
export interface StatOutlineNodeVO {
  id: string
  parentId: string
  level: number
  name: string
  sortNo: number
  questionCount: number
  answeredCount: number
  accuracy: number
  mastery: number
  children: StatOutlineNodeVO[]
}

/** 薄弱知识点（05 §5.8 第 35 条）。按 `mastery` 升序。 */
export interface WeakKpVO {
  kpId: string
  kpName: string
  answeredCount: number
  accuracy: number
  mastery: number
}

// ---------------- 模拟考（05 §5.6，接口 16~25） ----------------
// 后端已实现并验收（docs/30），故这些类型是对着 05 §5.6 的字段表与 rk-domain 的 VO
// 逐字写出的。两条口径写进注释而不是类型里：
//   ① 会话里的 `items`（`ExamItemVO`）**不含答案**，答案只在 `/exams/{id}/result` 返回；
//   ② 会话级 `remainingSec` / `currentSectionNo` 是**当前场次**的值，
//      真值以 `sittings[]` 为准（`PENDING` 场次不倒计时）。

/** 场次状态（02 §2.5.5）。`PENDING` 不跑计时，须用户主动 start。 */
export type SittingStatus = 'PENDING' | 'IN_PROGRESS' | 'FINISHED'

/** 分节状态（04 §4.7）。`SKIPPED` = `NOT_SUPPORTED`，仅展示提示。 */
export type SectionStatus = 'PENDING' | 'IN_PROGRESS' | 'FINISHED' | 'SKIPPED'

/**
 * 会话分节（05 §5.6 的 `SectionVO`）。
 *
 * `durationMin` / `durationMax` 是"最早/最晚可离开本节"的分钟数，**均必填**；
 * `perQuestionScore` 由服务端按 `maxScore / questionCount` 派生（会话表只快照了满分）。
 * 已无结转字段（对应评审 B11）。
 */
export interface ExamSectionVO {
  sittingNo: number
  sectionNo: number
  sectionName: string
  sectionType: SectionType
  questionCount: number
  maxScore: number
  perQuestionScore: number
  durationMin: number
  durationMax: number
  allowEarlySubmit: boolean
  usedSec: number
  supportStatus: SupportStatus
  status: SectionStatus
}

/**
 * 会话场次（05 §5.6 的 `SittingVO`）。
 *
 * `PENDING` 时 `remainingSec === totalDurationSec` 且 `startedAt === null`——前端据此
 * 显示「开始本场次」按钮而不是直接倒计时（场次之间不自动衔接，评审 A5）。
 */
export interface ExamSittingVO {
  sittingNo: number
  sittingName: string
  totalDurationSec: number
  remainingSec: number
  status: SittingStatus
  currentSectionNo: number
  totalMaxScore: number
  autoScoredMaxScore: number
  objectiveScore: number
  startedAt: string | null
  finishedAt: string | null
  sections: ExamSectionVO[]
}

/** 会话题目（05 §5.6 的 `ExamItemVO`）。**不含 `correctAnswer`/`referenceAnswer`/`analysis`**。 */
export interface ExamItemVO {
  itemId: string
  questionId: string
  sequence: number
  sectionNo: number
  questionType: QuestionType
  stem: string
  images: QuestionImageVO[]
  /** 主观题/论文为 `[]`（快照为空），前端按题型决定是否渲染选项。 */
  options: QuestionOptionVO[]
  score: number
  userAnswer?: string | null
  answeredAt?: string | null
  durationMs?: number | null
}

/** 会话详情（05 §5.6 第 16/18 条）。断点续答靠它恢复全部已答内容与剩余时间。 */
export interface ExamSessionVO {
  sessionId: string
  sessionNo: string
  subjectId: string
  subjectName: string
  status: ExamStatus
  /** 当前场次；无 `IN_PROGRESS` 场次时为下一个待开始的 `PENDING` 场次。 */
  currentSittingNo: number
  /** **全场次**总时长之和（展示用）。 */
  totalDurationSec: number
  /** 当前 `IN_PROGRESS` 场次的剩余秒数；无进行中场次时为 `0`（须先 start）。 */
  remainingSec: number
  currentSectionNo: number
  questionCount: number
  autoScoredMaxScore: number
  totalMaxScore: number
  sittings: ExamSittingVO[]
  /** 全场次分节的**扁平列表**（含 `sittingNo`）。 */
  sections: ExamSectionVO[]
  items: ExamItemVO[]
}

/** 保存作答的一条（05 §5.6 第 19 条）。`durationMs` 可选，服务端会把单题封顶到 10 分钟。 */
export interface ExamAnswerPayloadItem {
  itemId: string
  userAnswer: string
  durationMs?: number
}

/** 保存作答的请求体；`items` 允许为空数组（心跳与计时校准）。 */
export interface ExamAnswerPayload {
  items: ExamAnswerPayloadItem[]
}

/** 保存作答的响应：服务端口径的剩余时长，供前端校准倒计时。 */
export interface ExamAnswerResultVO {
  remainingSec: number
  currentSittingNo: number
  currentSectionNo: number
}

/** 启动场次的响应（05 §5.6 第 20 条）。 */
export interface ExamSittingStartVO {
  sitting: ExamSittingVO
  remainingSec: number
  currentSectionNo: number
}

/** 提前结束场次的响应（05 §5.6 第 22 条）。 */
export interface ExamSittingFinishVO {
  currentSittingNo: number
  remainingSec: number
  sittings: ExamSittingVO[]
}

/** 离开分节的响应（05 §5.6 第 21 条）。 */
export interface ExamSectionAdvanceVO {
  currentSectionNo: number
  remainingSec: number
  sections: ExamSectionVO[]
}

/** 历史成绩列表项（05 §5.6 第 17 条）。`ABANDONED` 的 `submittedAt` 为 `null`。 */
export interface ExamHistoryVO {
  sessionId: string
  sessionNo: string
  subjectId: string
  subjectName: string
  status: ExamStatus
  objectiveScore: number
  autoScoredMaxScore: number
  accuracy: number
  usedSec: number
  submittedAt: string | null
}

/** 成绩详情的场次汇总（05 §5.6 第 24 条）。`answered = false` 表示该场次未作答。 */
export interface ExamResultSittingVO {
  sittingNo: number
  sittingName: string
  totalDurationSec: number
  usedSec: number
  objectiveScore: number
  autoScoredMaxScore: number
  totalMaxScore: number
  status: SittingStatus
  answered: boolean
}

/** 成绩详情的分节明细（05 §5.6 第 24 条）。`scored = false` 时**不渲染任何分数**。 */
export interface ExamResultSectionVO {
  sittingNo: number
  sectionNo: number
  sectionName: string
  sectionType: SectionType
  maxScore: number
  objectiveScore: number
  supportStatus: SupportStatus
  scored: boolean
}

/** 成绩详情（05 §5.6 第 24 条）。`items` 才是含答案与解析的 `QuestionDetailVO[]`。 */
export interface ExamResultVO {
  sessionId: string
  sessionNo: string
  subjectName: string
  /** 客观题得分（**唯一参与判定的分数**）。 */
  objectiveScore: number
  autoScoredMaxScore: number
  totalMaxScore: number
  accuracy: number
  usedSec: number
  submittedAt: string | null
  scoredAt: string | null
  sittingResults: ExamResultSittingVO[]
  sectionResults: ExamResultSectionVO[]
  items: QuestionDetailVO[]
}
