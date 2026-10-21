// 枚举值 → 中文展示文案（前端内部概念，不属于接口契约）。
//
// 全部用 `Record<枚举, string>` 而不是 `Record<string, string>`：
// 后端新增一个枚举值时，这里会因为缺少键而**编译报错**。
// 用 string 索引签名的话，漏掉的值会在页面上显示成空白，没人会发现。

import type {
  EnabledStatus,
  ExamStatus,
  FeedbackType,
  OutlineOrigin,
  QuestionStatus,
  QuestionType,
  SectionType,
  SectionStatus,
  SittingStatus,
  SubjectLevel,
  SupportStatus,
  UserRole,
  WrongQuestionStatus,
} from './api'
import type { PracticeMode } from './domain'

/** 科目级别（01 §1.3）。 */
export const SUBJECT_LEVEL_LABEL: Record<SubjectLevel, string> = {
  JUNIOR: '初级',
  MIDDLE: '中级',
  SENIOR: '高级',
}

/** 页面渲染顺序：初 → 中 → 高。 */
export const SUBJECT_LEVEL_ORDER: SubjectLevel[] = ['JUNIOR', 'MIDDLE', 'SENIOR']

/** 考试周期。软考一年两考，部分科目只在上半年或下半年开考。 */
export const EXAM_PERIOD_LABEL: Record<'BOTH' | 'FIRST_HALF' | 'SECOND_HALF', string> = {
  BOTH: '上、下半年均开考',
  FIRST_HALF: '仅上半年开考',
  SECOND_HALF: '仅下半年开考',
}

/** 大纲完整度。SKELETON 不是故障，是数据现状，文案不得暗示"加载失败"。 */
export const OUTLINE_STATUS_LABEL: Record<'FULL' | 'SKELETON', string> = {
  FULL: '大纲完整',
  SKELETON: '大纲骨架',
}

/** 题库内容状态。 */
export const CONTENT_STATUS_LABEL: Record<'SAMPLE' | 'BULK', string> = {
  SAMPLE: '样例题',
  BULK: '题库已成规模',
}

export const SECTION_TYPE_LABEL: Record<SectionType, string> = {
  OBJECTIVE: '客观题',
  SUBJECTIVE: '主观题',
  ESSAY: '论文',
}

export const SUPPORT_STATUS_LABEL: Record<SupportStatus, string> = {
  SUPPORTED: '可在线作答',
  NOT_SUPPORTED: '需前往官方机考系统',
}

export const EXAM_STATUS_LABEL: Record<ExamStatus, string> = {
  CREATED: '待开始',
  IN_PROGRESS: '进行中',
  SUBMITTED: '已交卷',
  SCORED: '已判分',
  ABANDONED: '已放弃',
}

/** 场次状态（05 §5.6 的 `SittingVO.status`）。`PENDING` = 未开始计时（如论文场次）。 */
export const SITTING_STATUS_LABEL: Record<SittingStatus, string> = {
  PENDING: '未开始',
  IN_PROGRESS: '进行中',
  FINISHED: '已结束',
}

/** 分节状态（05 §5.6 的 `SectionVO.status`）。`SKIPPED` = `NOT_SUPPORTED`，仅展示提示。 */
export const SECTION_STATUS_LABEL: Record<SectionStatus, string> = {
  PENDING: '未开始',
  IN_PROGRESS: '进行中',
  FINISHED: '已结束',
  SKIPPED: '不可作答',
}

export const QUESTION_STATUS_LABEL: Record<QuestionStatus, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  PUBLISHED: '已发布',
  OFFLINE: '已下线',
}

/** 启用状态（科目的下架、知识点的停用共用）。 */
export const ENABLED_STATUS_LABEL: Record<EnabledStatus, string> = {
  ENABLED: '启用中',
  DISABLED: '已停用',
}

/** 账号角色（04 §4.1 的 `sys_user.role`）。 */
export const USER_ROLE_LABEL: Record<UserRole, string> = {
  USER: '考生',
  ADMIN: '管理员',
}

/**
 * 账号状态（04 §4.1 的 `sys_user.status`）。
 *
 * 不用 `EnabledStatus` 复用：库里的取值是 `ACTIVE / DISABLED`，
 * 与科目/知识点那套 `ENABLED / DISABLED` 不是同一个枚举——合成一个类型，
 * 会让"科目启用中"和"账号正常"共用同一个键，读代码时无法分辨。
 */
export const USER_STATUS_LABEL: Record<'ACTIVE' | 'DISABLED', string> = {
  ACTIVE: '正常',
  DISABLED: '已禁用',
}

/**
 * 章节出处（04 §4.5 的 `rk_outline_node.origin`）。
 *
 * 这个字段在界面上不是给考生看的，是给管理员做审计用的：它回答
 * "这一条是从官方大纲的标题抄来的，还是人工归纳的"——GR-1 要求章节名
 * **只取官方标题短语、不抄大纲正文**，所以来源必须能一眼看出来。
 */
export const OUTLINE_ORIGIN_LABEL: Record<OutlineOrigin, string> = {
  OFFICIAL_TITLE: '官方标题',
  MANUAL: '人工新增',
}

/** 卷面类型（02 §2.4）：初级与中级共用一套卷面，高级单独一套。 */
export const PAPER_TYPE_LABEL: Record<'JUNIOR_MID' | 'SENIOR', string> = {
  JUNIOR_MID: '初级/中级卷',
  SENIOR: '高级卷',
}

export const PRACTICE_MODE_LABEL: Record<PracticeMode, string> = {
  CHAPTER: '章节练习',
  KP: '知识点练习',
  SEQUENCE: '顺序练习',
  FAVORITE: '收藏练习',
  WRONG_REDO: '错题重做',
  SAMPLE: '样例题',
}

/** 题型（02 §2.2）。作答控件由它决定（07 §7.6 的 QuestionCard 分支表）。 */
export const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  SUBJECTIVE: '主观题',
  ESSAY: '论文题',
}

/** 题目反馈类型（02 §2.6.3）。 */
export const FEEDBACK_TYPE_LABEL: Record<FeedbackType, string> = {
  TYPO: '错别字',
  WRONG_ANSWER: '答案错误',
  UNCLEAR_ANALYSIS: '解析不清',
  COPYRIGHT: '版权问题',
  OTHER: '其他',
}

/**
 * 错题本条目状态（05 §5.7 第 26 条）。
 *
 * `MASTERED` 不是"用户手动标记"，而是连续答对到阈值后服务端自动置的；
 * 文案特意用「已掌握」而不是「已完成」，因为用户在错题本里看到的是一道题
 * 是否还会再错，不是一个待办是否被勾掉。
 */
export const WRONG_QUESTION_STATUS_LABEL: Record<WrongQuestionStatus, string> = {
  ACTIVE: '在册',
  MASTERED: '已掌握',
  REMOVED: '已移出',
}
