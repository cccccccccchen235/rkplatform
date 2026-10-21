// 管理端内容维护（05 §5.10 第 37~50 条）：科目 / 章节树 / 知识点。
//
// 一个业务域一个文件、一函数一接口（07 §7.11）：本文件只做"路径 + 参数"的映射，
// 不放机制代码，也不做数据整形——页面拿到什么就是服务端返回的什么。
//
// 卷面模板（第 51/52 条）不在这里：它属于 paper-template-config 页，
// 单独放在同目录的 paper-template.ts（见该文件顶部说明）。
//
// 关于 `options`：**只有"调用方自己会渲染这条错误"的接口才带它**，其余不带。
// 带它的分两类——
//   ① 页面级读取（三条 fetch*）：页面上有一块常驻的错误面板 + 重试按钮；
//   ② 对话框内的写操作（新增/编辑/合并）：错误挂在对话框顶部。
// 两类都传 `{ silent: true }`，否则同一次失败会被讲两遍（toast 一遍、面板一遍）。
// 删除那三条不带：它们的失败**只**由 toast 讲（列表行上一个按钮，没有可挂的地方）。

import type {
  AdminSubjectVO,
  EnabledStatus,
  KnowledgePointMergePayload,
  KnowledgePointSavePayload,
  KnowledgePointVO,
  OutlineNodeSavePayload,
  OutlineNodeVO,
  OutlineSortItem,
  SubjectSavePayload,
} from '@/types/api'
import { del, get, patch, post, put, type RequestOptions } from '@/utils/request'

// ---------------- 科目（37~40） ----------------

/**
 * 05 §5.10 第 37 条。管理端列表**含 `DISABLED`**——这正是它区别于考生端列表的地方。
 *
 * 三条 `fetch*` 都接 `options` 并一律由页面传 `{ silent: true }`：三个管理页各有一块
 * 自己的错误面板（含重试按钮），页面独家提示，避免同一次失败既弹 toast 又出面板。
 */
export function fetchAdminSubjects(options?: RequestOptions): Promise<AdminSubjectVO[]> {
  return get<AdminSubjectVO[]>('/admin/subjects', undefined, options)
}

/** 05 §5.10 第 38 条。返回新科目 ID。`code` 重复时服务端返回 `1001`。 */
export function createAdminSubject(payload: SubjectSavePayload, options?: RequestOptions): Promise<string> {
  return post<string>('/admin/subjects', payload, options)
}

/** 05 §5.10 第 39 条。请求体里的 `level` 会被服务端忽略（创建后不可改）。 */
export function updateAdminSubject(id: string, payload: SubjectSavePayload, options?: RequestOptions): Promise<void> {
  return put<void>(`/admin/subjects/${id}`, payload, options)
}

/**
 * 05 §5.10 第 40 条 —— **P2 引入的第一个 AC-2 高危操作**。
 *
 * `DISABLED` 即下架：考生端列表与详情随即不可见，服务端会写
 * `sys_operation_log`（`action = DISABLE`/`ENABLE`）。所以这个函数在页面上
 * 必须挂在二次确认之后，不能出现在列表行的直接点击上。
 */
export function changeAdminSubjectStatus(id: string, status: EnabledStatus): Promise<void> {
  return patch<void>(`/admin/subjects/${id}/status`, { status })
}

// ---------------- 章节树（41~45） ----------------

/** 05 §5.10 第 41 条。管理端树**含已停用节点**，故不带 `includeDisabled` 参数。 */
export function fetchAdminOutline(subjectId: string, options?: RequestOptions): Promise<OutlineNodeVO[]> {
  return get<OutlineNodeVO[]>('/admin/outline', { subjectId }, options)
}

/** 05 §5.10 第 42 条。层级由 `parentId` 推导；深于 3 层服务端返回 `1001`。 */
export function createAdminOutlineNode(payload: OutlineNodeSavePayload, options?: RequestOptions): Promise<string> {
  return post<string>('/admin/outline', payload, options)
}

/** 05 §5.10 第 43 条。改父会让整棵子树平移层级，成环或超 3 层返回 `1001`。 */
export function updateAdminOutlineNode(id: string, payload: OutlineNodeSavePayload, options?: RequestOptions): Promise<void> {
  return put<void>(`/admin/outline/${id}`, payload, options)
}

/**
 * 05 §5.10 第 44 条。有题目引用返回 `1005`。
 *
 * 服务端对"仍有子章节"的节点也返回 `1005`（05 只写了前一条），
 * 所以页面提示不能只写"有题目引用"——那句话在子章节场景下是错的。
 */
export function deleteAdminOutlineNode(id: string): Promise<void> {
  return del<void>(`/admin/outline/${id}`)
}

/**
 * 05 §5.10 第 45 条。请求体是**裸数组**。
 *
 * 语义是"整棵树的新顺序"的全量提交，不是增量：未提到的节点保持原位。
 * 跨层拖拽（改 `parentId`）也在同一次提交里表达。
 */
export function sortAdminOutline(items: OutlineSortItem[]): Promise<void> {
  return put<void>('/admin/outline/sort', items)
}

// ---------------- 知识点（46~50） ----------------

/** 05 §5.10 第 46 条。含 `DISABLED` 项，按题量降序。 */
export function fetchAdminKnowledgePoints(subjectId: string, options?: RequestOptions): Promise<KnowledgePointVO[]> {
  return get<KnowledgePointVO[]>('/admin/knowledge-points', { subjectId }, options)
}

/** 05 §5.10 第 47 条。同科目内重名返回 `1001`。 */
export function createAdminKnowledgePoint(payload: KnowledgePointSavePayload, options?: RequestOptions): Promise<string> {
  return post<string>('/admin/knowledge-points', payload, options)
}

/** 05 §5.10 第 48 条。`aliases` 是逗号分隔的整串，提交时原样传出。 */
export function updateAdminKnowledgePoint(id: string, payload: KnowledgePointSavePayload, options?: RequestOptions): Promise<void> {
  return put<void>(`/admin/knowledge-points/${id}`, payload, options)
}

/** 05 §5.10 第 49 条。仍被题目引用时返回 `1005`。 */
export function deleteAdminKnowledgePoint(id: string): Promise<void> {
  return del<void>(`/admin/knowledge-points/${id}`)
}

/**
 * 05 §5.10 第 50 条。一次事务内：迁移题目关联 → 并入 `aliases` → 删来源标签。
 *
 * `sourceIds` 中不得包含 `targetId`，也不得跨科目——这两条由服务端强制，
 * 前端只负责别把它们拼出来（选择器的候选项里要排掉目标自己）。
 */
export function mergeAdminKnowledgePoints(payload: KnowledgePointMergePayload, options?: RequestOptions): Promise<void> {
  return post<void>('/admin/knowledge-points/merge', payload, options)
}
