<script setup lang="ts">
// 管理端 · 知识点维护（07 §7.5.2 / 05 §5.10 第 46~50 条）。
//
// 合并（第 50 条）是这一页唯一有分量的操作：它**会删数据**。一次事务里
// 迁移题目关联 → 把来源的名字与同义词并进目标的 aliases → 删除来源标签。
// 所以它不能做成"多选后点一下就合并"：用户必须在最后一屏看见
// "哪个留下、哪几个消失、题会搬到哪里"。
//
// 于是合并用**独立对话框**而不是二次确认弹窗：选择谁当目标本身就是一次决策，
// 塞进 ElMessageBox 的"确定/取消"里就没地方放这个选择。
//
// aliases 全程是**逗号分隔的单个字符串**（05 §5.2），本页不拆成数组再拼回去——
// 库里就是一列，两边都不需要数组，多一次转换只会多一个说错话的地方。

import { computed, onMounted, reactive, ref } from 'vue'

import {
  createAdminKnowledgePoint,
  deleteAdminKnowledgePoint,
  fetchAdminKnowledgePoints,
  fetchAdminSubjects,
  mergeAdminKnowledgePoints,
  updateAdminKnowledgePoint,
} from '@/api/admin/content'
import type { AdminSubjectVO, KnowledgePointVO } from '@/types/api'
import { ENABLED_STATUS_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const subjects = ref<AdminSubjectVO[]>([])
const subjectId = ref('')
const rows = ref<KnowledgePointVO[]>([])
const selected = ref<KnowledgePointVO[]>([])

const loading = ref(false)
const loadError = ref('')
const tableRef = ref()

const dialogVisible = ref(false)
const saving = ref(false)
const formError = ref('')
const editingId = ref<string | null>(null)

const kpForm = reactive<{ name: string; aliases: string }>({ name: '', aliases: '' })
const formRef = ref()

const mergeVisible = ref(false)
const merging = ref(false)
const mergeError = ref('')
/** 合并后保留的那个标签。默认选题目最多的那个——留下的应该是内容最多的。 */
const mergeTargetId = ref('')

const isEditing = computed(() => editingId.value !== null)

const mergeCandidates = computed(() => selected.value)
const mergeTarget = computed(() => mergeCandidates.value.find((item) => item.id === mergeTargetId.value) ?? null)
const mergeSources = computed(() => mergeCandidates.value.filter((item) => item.id !== mergeTargetId.value))
/** 合并后目标上的题量：各被选中的题量之和（题目关联会全部迁过来）。 */
const mergeResultCount = computed(() => mergeCandidates.value.reduce((sum, item) => sum + item.questionCount, 0))
/** 目标上会新增的同义词：来源的名字 + 来源自己的别名，去重后剩下几段。 */
const mergeAliasCount = computed(() => {
  const parts = new Set<string>()
  mergeSources.value.forEach((item) => {
    parts.add(item.name)
    ;(item.aliases ?? '')
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean)
      .forEach((part) => parts.add(part))
  })
  return parts.size
})

const kpRules = {
  name: [
    { required: true, message: '知识点名称必填', trigger: 'blur' },
    { max: 64, message: '知识点名称不超过 64 字符', trigger: 'blur' },
  ],
  aliases: [{ max: 255, message: '同义词不超过 255 字符', trigger: 'blur' }],
}

async function loadSubjects(): Promise<void> {
  try {
    subjects.value = await fetchAdminSubjects({ silent: true })
    const first = subjects.value.find((item) => item.status === 'ENABLED') ?? subjects.value[0]
    if (first) {
      subjectId.value = first.id
      await load()
    }
  } catch (error) {
    loadError.value = (error as RequestError).message
  }
}

async function load(): Promise<void> {
  if (!subjectId.value) {
    rows.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    rows.value = await fetchAdminKnowledgePoints(subjectId.value, { silent: true })
    // 重载后旧的选中项在 DOM 里已经不存在，必须一起清掉：
    // 表格的 selection 是按行对象存的，不清就会留下指向已消失行的引用，
    // 合并时拿着它去提交一个已经不存在的 ID。
    selected.value = []
    tableRef.value?.clearSelection?.()
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
}

async function handleSubjectChange(): Promise<void> {
  await load()
}

function handleSelectionChange(rows_: KnowledgePointVO[]): void {
  selected.value = rows_
}

function openCreate(): void {
  editingId.value = null
  formError.value = ''
  kpForm.name = ''
  kpForm.aliases = ''
  dialogVisible.value = true
}

function openEdit(row: KnowledgePointVO): void {
  editingId.value = row.id
  formError.value = ''
  kpForm.name = row.name
  kpForm.aliases = row.aliases ?? ''
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate?.().catch(() => false)
  if (!valid) return

  saving.value = true
  formError.value = ''
  try {
    const payload = {
      subjectId: subjectId.value,
      name: kpForm.name,
      aliases: kpForm.aliases.trim(),
    }
    if (editingId.value) {
      await updateAdminKnowledgePoint(editingId.value, payload, { silent: true })
      ElMessage.success('知识点已保存')
    } else {
      await createAdminKnowledgePoint(payload, { silent: true })
      ElMessage.success('知识点已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    // 1001 在这里就是"同科目内重名"（uk_subject_name）。挂在表单顶部。
    formError.value = (error as RequestError).message
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: KnowledgePointVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」吗？仍被题目引用时服务端会拒绝（1005）——那种情况请改用"合并"。`,
      '删除知识点',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await deleteAdminKnowledgePoint(row.id)
    ElMessage.success('知识点已删除')
    await load()
  } catch (error) {
    // 1005 = 有题目引用。**这里必须把话说到"下一步做什么"**：
    // 删除被拒是这个页面的正常分支之一，不是异常，用户要的是出路而不是错误码。
    ElMessage.error((error as RequestError).message)
  }
}

function openMerge(): void {
  if (selected.value.length < 2) return
  mergeError.value = ''
  // 默认目标 = 题量最多的那个：合并的方向应当是"少的并进多的"。
  // 上面的 early return 已经保证至少两项，这里的可选链只是给
  // `noUncheckedIndexedAccess` 一个交代（它看不到那个早退）。
  const [best] = [...selected.value].sort((a, b) => b.questionCount - a.questionCount)
  mergeTargetId.value = best?.id ?? ''
  mergeVisible.value = true
}

async function handleMerge(): Promise<void> {
  const target = mergeTarget.value
  const sources = mergeSources.value
  if (!target || sources.length === 0) {
    mergeError.value = '请选择保留哪一个标签'
    return
  }

  merging.value = true
  mergeError.value = ''
  try {
    await mergeAdminKnowledgePoints({ sourceIds: sources.map((item) => item.id), targetId: target.id }, { silent: true })
    ElMessage.success(`已合并为「${target.name}」`)
    mergeVisible.value = false
    await load()
  } catch (error) {
    // 服务端会拒"来源含目标"和"跨科目"，正常路径下前端已经排掉了，
    // 所以这里能不吞就不吞：真出现说明前端的选项构造有问题，要看得见。
    mergeError.value = (error as RequestError).message
  } finally {
    merging.value = false
  }
}

onMounted(loadSubjects)
</script>

<template>
  <div class="kp-manage">
    <header class="kp-manage__header">
      <div>
        <h1 class="rk-page-title">知识点维护</h1>
        <p class="kp-manage__lead">
          列表含<strong>已停用</strong>标签（公开接口不再返回它们）。按题量降序，题量为 0 的就是还没有题目挂上去的标签。
        </p>
      </div>
    </header>

    <div class="kp-manage__toolbar">
      <el-select v-model="subjectId" placeholder="选择科目" class="kp-manage__subject" filterable @change="handleSubjectChange">
        <el-option v-for="item in subjects" :key="item.id" :label="`${item.name}（${item.code}）`" :value="item.id" />
      </el-select>

      <el-button type="primary" :disabled="!subjectId" @click="openCreate">新增知识点</el-button>

      <!--
        合并的入口按钮只在选中 ≥2 项时可用。禁用态而不是隐藏态：
        按钮突然出现会让用户不知道"这个功能一直在这里"。
      -->
      <el-button :disabled="selected.length < 2" @click="openMerge">
        合并所选{{ selected.length >= 2 ? `（${selected.length}）` : '' }}
      </el-button>

      <span class="kp-manage__hint">合并会删除被并掉的标签，并把它们的题目与同义词转到保留的那个上。</span>
    </div>

    <el-alert v-if="loadError" type="error" :title="`知识点加载失败：${loadError}`" :closable="false" show-icon>
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      class="kp-manage__table"
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="同义词" min-width="240">
        <template #default="{ row }">
          <span v-if="(row as KnowledgePointVO).aliases" class="kp-manage__aliases">
            {{ (row as KnowledgePointVO).aliases }}
          </span>
          <span v-else class="kp-manage__muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="题量" width="88" align="right">
        <template #default="{ row }">
          <span class="kp-manage__count">{{ (row as KnowledgePointVO).questionCount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="(row as KnowledgePointVO).status === 'ENABLED' ? 'success' : 'info'" disable-transitions>
            {{ ENABLED_STATUS_LABEL[(row as KnowledgePointVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="132" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row as KnowledgePointVO)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row as KnowledgePointVO)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="该科目还没有知识点标签" />
      </template>
    </el-table>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑知识点' : '新增知识点'" width="480px">
      <el-alert v-if="formError" type="error" :title="formError" :closable="false" show-icon class="kp-manage__form-error" />

      <el-form ref="formRef" :model="kpForm" :rules="kpRules" label-width="72px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="kpForm.name" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item label="同义词" prop="aliases">
          <el-input
            v-model="kpForm.aliases"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="用英文逗号分隔，如：需求分析,需求获取,需求工程"
          />
          <p class="kp-manage__hint">检索时命中同义词等于命中本标签。逗号必须是半角。</p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 合并：独立对话框，因为"谁留下"是一次必须显式做出的选择 -->
    <el-dialog v-model="mergeVisible" title="合并知识点" width="560px">
      <el-alert v-if="mergeError" type="error" :title="mergeError" :closable="false" show-icon class="kp-manage__form-error" />

      <p class="kp-manage__merge-intro">
        从选中的 {{ mergeCandidates.length }} 个标签中选一个<strong>保留</strong>，其余会被删除。题目关联与同义词都会转到保留的那个上。
      </p>

      <el-radio-group v-model="mergeTargetId" class="kp-manage__merge-group">
        <el-radio v-for="item in mergeCandidates" :key="item.id" :value="item.id" class="kp-manage__merge-option">
          <span class="kp-manage__merge-name">{{ item.name }}</span>
          <span class="kp-manage__muted">{{ item.questionCount }} 题</span>
        </el-radio>
      </el-radio-group>

      <div v-if="mergeTarget" class="kp-manage__merge-summary">
        <p>
          保留 <strong>{{ mergeTarget.name }}</strong>，其题量 {{ mergeTarget.questionCount }} → <strong>{{ mergeResultCount }}</strong>。
        </p>
        <p>
          删除 {{ mergeSources.length }} 个标签：
          <span v-for="(item, index) in mergeSources" :key="item.id" class="kp-manage__merge-removed">
            {{ item.name }}<span v-if="index < mergeSources.length - 1">、</span>
          </span>
        </p>
        <p>它们的名称与同义词会并入保留标签的同义词，去重后新增 {{ mergeAliasCount }} 段。</p>
        <p class="kp-manage__merge-warn">本操作会写操作日志，且不可撤销。</p>
      </div>

      <template #footer>
        <el-button @click="mergeVisible = false">取消</el-button>
        <el-button v-if="mergeTarget" type="primary" :loading="merging" @click="handleMerge">
          合并并删除 {{ mergeSources.length }} 个标签
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.kp-manage {
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
    width: 260px;
  }

  &__hint {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    line-height: $rk-line-height;
  }

  &__table {
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
  }

  &__aliases {
    color: $rk-text-2;
    word-break: break-all;
  }

  &__muted {
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__count {
    font-variant-numeric: tabular-nums;
  }

  &__form-error {
    margin-bottom: $rk-space-l;
  }

  &__merge-intro {
    color: $rk-text-2;
    margin-bottom: $rk-space-l;
  }

  &__merge-group {
    display: flex;
    flex-direction: column;
    gap: $rk-space-s;
    align-items: stretch;
    margin-bottom: $rk-space-l;
  }

  &__merge-option {
    height: auto;
    margin-right: 0;
    padding: $rk-space-s;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-m;
  }

  &__merge-name {
    margin-right: $rk-space-s;
  }

  &__merge-summary {
    padding: $rk-space-m;
    background-color: $rk-bg-page;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-m;
    color: $rk-text-2;
    line-height: $rk-line-height;
  }

  &__merge-removed {
    color: $rk-text-1;
  }

  &__merge-warn {
    color: $rk-danger;
  }
}
</style>
