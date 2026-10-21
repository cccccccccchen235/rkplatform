<script setup lang="ts">
// 管理端 · 大纲维护（07 §7.5.2 / 05 §5.10 第 41~45 条）。
//
// 这一页的难点不在增删改，在**拖拽**：一次拖拽同时表达了"换顺序"和"换父"，
// 而服务端对层级有硬约束（最多 3 层、不许成环）。约束必须在这里就挡住——
// 让用户拖完再弹一个 1001，等于让他白拖一次。
//
// 所以 `allow-drop` 是这一页的核心逻辑，不是可选的润色：
// ① 目标层级 = inner 时 dropNode.level + 1，prev/next 时 dropNode.level；
// ② 光看被拖节点的层级不够，要看**整棵子树**——把一个 L2 节点拖成 L3 的父节点，
//    它自己的 L2 子节点就变成 L4 了。所以判据是 `目标层级 + 子树深度 - 1 ≤ 3`。
// ③ 不能拖进自己的后代（成环）。
//
// 排序提交走 `PUT /admin/outline/sort`，请求体是**整棵树的裸数组全量提交**
// （05 §5.10 第 45 条），不是"移动了哪个节点"的增量。

import { computed, onMounted, reactive, ref } from 'vue'

import {
  createAdminOutlineNode,
  deleteAdminOutlineNode,
  fetchAdminOutline,
  fetchAdminSubjects,
  sortAdminOutline,
  updateAdminOutlineNode,
} from '@/api/admin/content'
import type { AdminSubjectVO, OutlineNodeSavePayload, OutlineNodeVO, OutlineSortItem, OutlineOrigin } from '@/types/api'
import { OUTLINE_ORIGIN_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

/** 大纲最大层级（02 §2.2 / 05 §5.10）。写在这里是为了让"3"只出现一次。 */
const MAX_LEVEL = 3

/**
 * el-tree 回调里拿到的是它内部的 `Node` 对象，不是我们的 VO。
 *
 * 这里只声明本页真正用到的三个字段，并且 `data` 取 `unknown` 而不是 `OutlineNodeVO`：
 * `Node.data` 的类型是 `TreeNodeData`（`Record<string, any>`），把它当作
 * `OutlineNodeVO` 会让整个回调签名与 `AllowDropFunction` 不兼容——而如果引
 * element-plus 的深层类型（`element-plus/es/components/tree/src/...`）来对齐，
 * 又会被它的小版本升级打断。取 `unknown` 两边都不得罪，代价是取用处要断言一次。
 */
interface ElTreeNode {
  level: number
  data: unknown
  childNodes?: ElTreeNode[]
}

/** 把 el-tree 的节点数据还原成我们的 VO。只在这一页内部用。 */
function vo(node: ElTreeNode): OutlineNodeVO {
  return node.data as OutlineNodeVO
}

const subjects = ref<AdminSubjectVO[]>([])
const subjectId = ref('')
const treeData = ref<OutlineNodeVO[]>([])
const treeRef = ref()

const loading = ref(false)
const loadError = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const formError = ref('')
/** null = 新增 */
const editingId = ref<string | null>(null)
/** 新增时的父节点；`'0'` 表示根。 */
const editingParentId = ref('0')
const editingParentName = ref('')
const editingLevel = ref(1)

const nodeForm = reactive<{ name: string; origin: OutlineOrigin }>({
  name: '',
  origin: 'MANUAL',
})

const formRef = ref()

const ORIGIN_OPTIONS: Array<{ value: OutlineOrigin; label: string }> = (
  ['OFFICIAL_TITLE', 'MANUAL'] as OutlineOrigin[]
).map((value) => ({ value, label: OUTLINE_ORIGIN_LABEL[value] }))

const isEditing = computed(() => editingId.value !== null)

const currentSubject = computed(() => subjects.value.find((item) => item.id === subjectId.value) ?? null)

/** 空树时才提示"还没有章节"；"取不到"是另一回事，由 loadError 表达。 */
const isEmpty = computed(() => !loading.value && !loadError.value && treeData.value.length === 0)

const nodeRules = {
  name: [
    { required: true, message: '章节标题必填', trigger: 'blur' },
    { max: 64, message: '章节标题不超过 64 字符', trigger: 'blur' },
  ],
}

async function loadSubjects(): Promise<void> {
  try {
    subjects.value = await fetchAdminSubjects({ silent: true })
    // 默认停在第一个启用中的科目上：管理端列表含已停用项，
    // 直接取第一条可能落到一个已经下架的科目上。
    const first = subjects.value.find((item) => item.status === 'ENABLED') ?? subjects.value[0]
    if (first) {
      subjectId.value = first.id
      await loadTree()
    }
  } catch (error) {
    loadError.value = (error as RequestError).message
  }
}

async function loadTree(): Promise<void> {
  if (!subjectId.value) {
    treeData.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    treeData.value = await fetchAdminOutline(subjectId.value, { silent: true })
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
}

async function handleSubjectChange(): Promise<void> {
  await loadTree()
}

// ---------------- 拖拽约束 ----------------

/** 子树深度：自身算 1。用来判断"拖过去之后整棵子树最深会到第几层"。 */
function subtreeDepth(node: ElTreeNode): number {
  const children = node.childNodes ?? []
  if (children.length === 0) return 1
  return 1 + Math.max(...children.map((child) => subtreeDepth(child)))
}

/** `candidate` 是否是 `ancestor` 的后代（含自身）。 */
function isSelfOrDescendant(ancestor: ElTreeNode, candidate: ElTreeNode): boolean {
  if (vo(ancestor).id === vo(candidate).id) return true
  return (ancestor.childNodes ?? []).some((child) => isSelfOrDescendant(child, candidate))
}

/**
 * el-tree 的 `allow-drop`。返回 false 时节点直接弹回原位，不给服务端添麻烦。
 *
 * 注意这里**没有**校验"父节点是否属于同一科目"：跨科目拖拽在本页不可能发生，
 * 树上只有当前科目的节点。
 */
function allowDrop(dragging: ElTreeNode, drop: ElTreeNode, type: 'prev' | 'inner' | 'next'): boolean {
  // 成环：拖到自己或自己的后代上
  if (isSelfOrDescendant(dragging, drop)) return false

  // inner = 成为 drop 的子节点；prev/next = 成为 drop 的同级
  const targetLevel = type === 'inner' ? drop.level + 1 : drop.level

  // 光看被拖节点的层级不够：它带着自己的整棵子树一起走
  return targetLevel + subtreeDepth(dragging) - 1 <= MAX_LEVEL
}

/**
 * 拖拽完成。el-tree 已经改完本地结构，这里做的是**把它读出来交给服务端**。
 *
 * 不改回"重载"：重载会把整棵树重建，展开状态与滚动位置一起丢；
 * 而这次改动的内容就是我们手上这棵树，来回一趟只会让界面闪一下。
 * 代价是本地与服务端短暂不一致——所以失败时必须重载，见下面的 catch。
 */
async function handleDrop(): Promise<void> {
  const items: OutlineSortItem[] = []
  collect(treeData.value, '0', items)

  try {
    await sortAdminOutline(items)
    ElMessage.success('顺序已保存')
  } catch (error) {
    ElMessage.error((error as RequestError).message)
    // 本地结构已经改了，但服务端没接受——只有重载才能让两边重新一致
    await loadTree()
  }
}

/**
 * 把树压平成 `{id, parentId, sortNo}` 列表，并**就地同步本地模型**。
 *
 * 同步 `parentId` 不是顺手做的事：跨层拖拽后节点在本地仍然记着旧父节点，
 * 下一次拖拽或"新增子章节"就会拿着错的父亲去提交。
 */
function collect(nodes: OutlineNodeVO[], parentId: string, out: OutlineSortItem[]): void {
  nodes.forEach((node, index) => {
    node.parentId = parentId
    node.sortNo = index + 1
    out.push({ id: node.id, parentId, sortNo: index + 1 })
    collect(node.children ?? [], node.id, out)
  })
}

// ---------------- 增删改 ----------------

function openCreateRoot(): void {
  editingId.value = null
  editingParentId.value = '0'
  editingParentName.value = ''
  editingLevel.value = 1
  formError.value = ''
  nodeForm.name = ''
  nodeForm.origin = 'MANUAL'
  dialogVisible.value = true
}

function openCreateChild(parent: OutlineNodeVO): void {
  editingId.value = null
  editingParentId.value = parent.id
  editingParentName.value = parent.name
  editingLevel.value = parent.level + 1
  formError.value = ''
  nodeForm.name = ''
  nodeForm.origin = 'MANUAL'
  dialogVisible.value = true
}

function openEdit(node: OutlineNodeVO): void {
  editingId.value = node.id
  editingParentId.value = node.parentId
  editingParentName.value = ''
  editingLevel.value = node.level
  formError.value = ''
  nodeForm.name = node.name
  // `OutlineNodeVO` 上没有 `origin`（05 §5.2 的字段表里它只出现在保存入参上），
  // 所以编辑时无从得知原出处——要么改 05 让读接口也返回它，要么接受
  // "编辑一次就把出处重置为人工"。这里选后者，但**不静默**：
  // 编辑对话框里明写了这一句，用户看得见自己正在改变什么。
  nodeForm.origin = 'MANUAL'
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate?.().catch(() => false)
  if (!valid) return

  saving.value = true
  formError.value = ''
  try {
    const payload: OutlineNodeSavePayload = {
      subjectId: subjectId.value,
      parentId: editingParentId.value,
      name: nodeForm.name,
      origin: nodeForm.origin,
    }
    if (editingId.value) {
      await updateAdminOutlineNode(editingId.value, payload, { silent: true })
      ElMessage.success('章节已保存')
    } else {
      await createAdminOutlineNode(payload, { silent: true })
      ElMessage.success('章节已新增')
    }
    dialogVisible.value = false
    await loadTree()
  } catch (error) {
    // 1001 在这里几乎总是"层级超限"或"成环"（05 §5.10 第 42/43 条），
    // 挂在对话框里而不是飘一条 toast：用户要改的是这屏表单里的父节点。
    formError.value = (error as RequestError).message
  } finally {
    saving.value = false
  }
}

async function handleDelete(node: OutlineNodeVO): Promise<void> {
  const childCount = node.children?.length ?? 0
  const detail =
    childCount > 0
      ? `该章节下还有 ${childCount} 个直接子章节，删除会一并移走整棵子树。`
      : node.questionCount > 0
        ? `该章节下挂着 ${node.questionCount} 道题。`
        : ''

  try {
    await ElMessageBox.confirm(
      `确定删除「${node.name}」吗？${detail}服务端在有题目引用或仍有子章节时会拒绝本次删除。`,
      '删除章节',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await deleteAdminOutlineNode(node.id)
    ElMessage.success('章节已删除')
    await loadTree()
  } catch (error) {
    // 1005 = 有题目引用，或仍有子章节（后者是服务端额外加的规则，
    // 05 §5.10 第 44 条只写了前者）。直接把服务端的话原样给用户。
    ElMessage.error((error as RequestError).message)
  }
}

onMounted(loadSubjects)
</script>

<template>
  <div class="outline-manage">
    <header class="outline-manage__header">
      <div>
        <h1 class="rk-page-title">大纲维护</h1>
        <p class="outline-manage__lead">
          最多 <strong>{{ MAX_LEVEL }}</strong> 层。拖拽同时完成排序与改父级——把节点拖到某个章节<em>内部</em>是成为它的子章节，
          拖到<em>两行之间</em>是成为同级。不合法的落点会直接弹回，不到服务端。
        </p>
      </div>
    </header>

    <div class="outline-manage__toolbar">
      <el-select
        v-model="subjectId"
        placeholder="选择科目"
        class="outline-manage__subject"
        filterable
        @change="handleSubjectChange"
      >
        <el-option v-for="item in subjects" :key="item.id" :label="`${item.name}（${item.code}）`" :value="item.id">
          <span>{{ item.name }}（{{ item.code }}）</span>
          <!-- 已停用科目也会出现在这里：它的章节树仍要能维护，否则停用即等于弃养 -->
          <span v-if="item.status === 'DISABLED'" class="outline-manage__disabled-mark">已停用</span>
        </el-option>
      </el-select>

      <el-button type="primary" :disabled="!subjectId" @click="openCreateRoot">新增根章节</el-button>

      <span v-if="currentSubject" class="outline-manage__hint">
        大纲状态：{{ currentSubject.outlineStatus === 'FULL' ? '完整章节树' : '仅骨架' }}
      </span>
    </div>

    <el-alert v-if="loadError" type="error" :title="`章节树加载失败：${loadError}`" :closable="false" show-icon>
      <template #default>
        <el-button text type="primary" @click="loadTree">重试</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="outline-manage__panel">
      <el-empty v-if="isEmpty" description="该科目还没有章节，先新增一个根章节" />

      <el-tree
        v-else
        ref="treeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        draggable
        default-expand-all
        :expand-on-click-node="false"
        :allow-drop="allowDrop"
        @node-drop="handleDrop"
      >
        <template #default="{ node, data }">
          <div class="outline-node">
            <span class="outline-node__name">{{ (data as OutlineNodeVO).name }}</span>

            <span class="outline-node__meta">
              <span class="outline-node__level">L{{ node.level }}</span>
              <span class="outline-node__count">{{ (data as OutlineNodeVO).questionCount }} 题</span>
            </span>

            <!--
              操作按钮常显，不做 hover 才出现：`visibility: hidden` 会让按钮
              连键盘也聚焦不到，而纯 `opacity` 又留下一个看不见却点得到的靶子。
              DESIGN.md 的选择是"宁可多三个字，也不要藏起来的功能"。
            -->
            <span class="outline-node__actions">
              <el-button
                v-if="node.level < MAX_LEVEL"
                text
                size="small"
                type="primary"
                @click.stop="openCreateChild(data as OutlineNodeVO)"
              >
                加子章节
              </el-button>
              <el-button text size="small" @click.stop="openEdit(data as OutlineNodeVO)">编辑</el-button>
              <el-button text size="small" type="danger" @click.stop="handleDelete(data as OutlineNodeVO)">
                删除
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑章节' : '新增章节'" width="480px">
      <el-alert v-if="formError" type="error" :title="formError" :closable="false" show-icon class="outline-manage__form-error" />

      <el-form ref="formRef" :model="nodeForm" :rules="nodeRules" label-width="88px">
        <el-form-item label="层级">
          <span class="outline-manage__static">
            L{{ editingLevel }}
            <template v-if="editingParentName">，位于「{{ editingParentName }}」之下</template>
            <template v-else-if="editingParentId === '0'">（根章节）</template>
          </span>
          <p class="outline-manage__hint">层级由父节点决定，改父级请在树上拖拽。</p>
        </el-form-item>

        <el-form-item label="章节标题" prop="name">
          <el-input v-model="nodeForm.name" maxlength="64" show-word-limit />
          <!--
            GR-1：这里放的是官方大纲的**标题短语**，不是大纲正文。
            提示写在输入框下而不是只在文档里，因为写这一行的人才是会犯错的人。
          -->
          <p class="outline-manage__hint">只填标题短语（如「需求分析」）。大纲正文内容不录入本系统。</p>
        </el-form-item>

        <el-form-item label="出处" prop="origin">
          <el-select v-model="nodeForm.origin" class="outline-manage__field">
            <el-option v-for="opt in ORIGIN_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <p v-if="isEditing" class="outline-manage__hint">
            读接口不返回出处（05 §5.2），所以每次编辑都会按这里的选择重写它。
          </p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.outline-manage {
  &__lead {
    color: $rk-text-2;
    max-width: 860px;
  }

  &__toolbar {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    margin-bottom: $rk-space-l;
  }

  &__subject {
    width: 320px;
  }

  &__disabled-mark {
    float: right;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__hint {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    line-height: $rk-line-height;
  }

  &__panel {
    min-height: 240px;
    padding: $rk-space-m;
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
  }

  &__static {
    color: $rk-text-2;
  }

  &__field {
    width: 100%;
  }

  &__form-error {
    margin-bottom: $rk-space-l;
  }
}

.outline-node {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: $rk-space-m;
  padding-right: $rk-space-s;

  &__name {
    color: $rk-text-1;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: $rk-space-s;
    flex-shrink: 0;
  }

  &__level,
  &__count {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__actions {
    margin-left: auto;
    flex-shrink: 0;
  }
}

// el-tree 的行高与 hover 底色：默认的 26px 太挤，本页每行有三个按钮。
:deep(.el-tree-node__content) {
  height: 40px;
  border-radius: $rk-radius-s;
}
</style>
