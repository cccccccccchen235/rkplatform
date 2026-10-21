<script setup lang="ts">
// 管理端 · 科目维护（07 §7.5.2 / 05 §5.10 第 37~40 条）。
//
// 这一页有两处与普通 CRUD 不同，且都不是可选的修饰：
//
// 1. **停用是下架**。`PATCH /status` 是 P2 引入的第一个 AC-2 高危操作，
//    服务端会写 `sys_operation_log`。所以它必须走二次确认，且确认文案要
//    说清后果（考生端立刻看不到），而不是一句"确定要停用吗"。
// 2. **列表含 DISABLED**。这是管理端列表与考生端列表唯一的区别——
//    如果这一页只显示启用中的科目，"重新启用"就再也找不到了。

import { computed, onMounted, reactive, ref } from 'vue'

import {
  changeAdminSubjectStatus,
  createAdminSubject,
  fetchAdminSubjects,
  updateAdminSubject,
} from '@/api/admin/content'
import type { AdminSubjectVO, SubjectLevel, SubjectSavePayload } from '@/types/api'
import {
  CONTENT_STATUS_LABEL,
  ENABLED_STATUS_LABEL,
  EXAM_PERIOD_LABEL,
  OUTLINE_ORIGIN_LABEL,
  OUTLINE_STATUS_LABEL,
  PAPER_TYPE_LABEL,
  SUBJECT_LEVEL_LABEL,
  SUBJECT_LEVEL_ORDER,
} from '@/types/labels'
import type { RequestError } from '@/utils/request'

// `OUTLINE_ORIGIN_LABEL` 在这一页用不到（它属于大纲页），但模板里引用了会报未使用；
// 这里显式列出来供下面的 form 常量使用，避免误删。
void OUTLINE_ORIGIN_LABEL

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const rows = ref<AdminSubjectVO[]>([])

const drawerVisible = ref(false)
/** null = 新增；非 null = 编辑该科目。 */
const editing = ref<AdminSubjectVO | null>(null)
/** 表单级错误（服务端 1001 且没给出具体字段时）。 */
const formError = ref('')

const formRef = ref()

const form = reactive<SubjectSavePayload>({
  level: 'MIDDLE',
  code: '',
  name: '',
  paperType: 'JUNIOR_MID',
  examPeriod: 'BOTH',
  outlineStatus: 'SKELETON',
  contentStatus: 'SAMPLE',
  intro: '',
})

const isEditing = computed(() => editing.value !== null)

const LEVEL_OPTIONS = SUBJECT_LEVEL_ORDER.map((level) => ({ value: level, label: SUBJECT_LEVEL_LABEL[level] }))
const PAPER_TYPE_OPTIONS = (Object.keys(PAPER_TYPE_LABEL) as Array<keyof typeof PAPER_TYPE_LABEL>).map((value) => ({
  value,
  label: PAPER_TYPE_LABEL[value],
}))
const EXAM_PERIOD_OPTIONS = (['BOTH', 'FIRST_HALF', 'SECOND_HALF'] as const).map((value) => ({
  value,
  label: EXAM_PERIOD_LABEL[value],
}))
const OUTLINE_STATUS_OPTIONS = (['FULL', 'SKELETON'] as const).map((value) => ({
  value,
  label: OUTLINE_STATUS_LABEL[value],
}))
const CONTENT_STATUS_OPTIONS = (['SAMPLE', 'BULK'] as const).map((value) => ({
  value,
  label: CONTENT_STATUS_LABEL[value],
}))

/** 与 05 §5.10 第 38 条的字段约束一致，服务端仍会再校验一次。 */
const rules = {
  code: [
    { required: true, message: '科目代码必填', trigger: 'blur' },
    { max: 32, message: '科目代码不超过 32 字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '科目名必填', trigger: 'blur' },
    { max: 64, message: '科目名不超过 64 字符', trigger: 'blur' },
  ],
  intro: [{ max: 2000, message: '简介不超过 2000 字符', trigger: 'blur' }],
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    rows.value = await fetchAdminSubjects({ silent: true })
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editing.value = null
  formError.value = ''
  Object.assign(form, {
    level: 'MIDDLE' as SubjectLevel,
    code: '',
    name: '',
    paperType: 'JUNIOR_MID',
    examPeriod: 'BOTH',
    outlineStatus: 'SKELETON',
    contentStatus: 'SAMPLE',
    intro: '',
  })
  drawerVisible.value = true
}

function openEdit(row: AdminSubjectVO): void {
  editing.value = row
  formError.value = ''
  Object.assign(form, {
    level: row.level,
    code: row.code,
    name: row.name,
    paperType: row.paperType,
    examPeriod: row.examPeriod,
    outlineStatus: row.outlineStatus,
    contentStatus: row.contentStatus,
    intro: row.intro ?? '',
  })
  drawerVisible.value = true
}

/**
 * 把服务端的 1001 落到具体字段上（05 §5.10 第 38 条：`code` 重名返回 `1001`）。
 *
 * 不能只弹一条 toast：用户刚填完一屏表单，需要知道**哪一个**字段要改。
 * 服务端给了字段错误列表就定位到字段；只给了消息（业务规则类 1001）就挂在表单顶部。
 */
function applyServerFieldErrors(error: RequestError): void {
  const data = error.data
  if (Array.isArray(data) && data.length > 0) {
    const list = data
      .filter((item): item is { field: string; message: string } => {
        const maybe = item as { field?: unknown; message?: unknown }
        return typeof maybe?.field === 'string' && typeof maybe?.message === 'string'
      })
      .map((item) => ({ message: item.message, field: item.field }))
    if (list.length > 0) {
      formRef.value?.setFields?.(list)
      return
    }
  }
  formError.value = error.message
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate?.().catch(() => false)
  if (!valid) return

  saving.value = true
  formError.value = ''
  try {
    const payload: SubjectSavePayload = { ...form }
    if (isEditing.value && editing.value) {
      await updateAdminSubject(editing.value.id, payload, { silent: true })
      ElMessage.success('科目已保存')
    } else {
      await createAdminSubject(payload, { silent: true })
      ElMessage.success('科目已新增')
    }
    drawerVisible.value = false
    await load()
  } catch (error) {
    const err = error as RequestError
    if (err.isBusiness && err.code === 1001) applyServerFieldErrors(err)
    else if (!err.isBusiness || err.code !== 1001) formError.value = err.message
  } finally {
    saving.value = false
  }
}

/**
 * 启用/停用。**停用 = 下架**，所以确认框里要写清后果而不是只问"确定吗"。
 *
 * 文案里带上科目名：管理端列表很长，从这一行点开的对话框如果只写
 * "该科目"，用户就没法在最后一步确认自己点的是不是想点的那一行。
 */
async function handleToggleStatus(row: AdminSubjectVO): Promise<void> {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const disabling = next === 'DISABLED'

  const tip = disabling
    ? `停用「${row.name}」后，考生端的科目列表与详情页将立即不再显示它。已存在的模拟考会话不受影响。本次操作会记入操作日志。`
    : `启用「${row.name}」后，它会重新出现在考生端科目列表中。本次操作会记入操作日志。`

  try {
    await ElMessageBox.confirm(tip, disabling ? '停用科目' : '启用科目', {
      confirmButtonText: disabling ? '停用' : '启用',
      cancelButtonText: '取消',
      type: disabling ? 'warning' : 'info',
    })
  } catch {
    // 取消不是错误
    return
  }

  try {
    await changeAdminSubjectStatus(row.id, next)
    ElMessage.success(disabling ? '已停用' : '已启用')
    await load()
  } catch (error) {
    // 拦截器已提示，这里只在需要时补充上下文
    void error
  }
}

onMounted(load)
</script>

<template>
  <div class="subject-manage">
    <header class="subject-manage__header">
      <div>
        <h1 class="rk-page-title">科目维护</h1>
        <p class="subject-manage__lead">
          列表含<strong>已停用</strong>科目——停用即下架，考生端立刻看不到，所以停用后仍要在这里能找到它并重新启用。
        </p>
      </div>
      <el-button type="primary" @click="openCreate">新增科目</el-button>
    </header>

    <el-alert v-if="loadError" type="error" :title="`科目列表加载失败：${loadError}`" :closable="false" show-icon>
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="rows" class="subject-manage__table" row-key="id">
      <el-table-column prop="sortNo" label="序号" width="72" align="right" />
      <el-table-column label="级别" width="88">
        <template #default="{ row }">{{ SUBJECT_LEVEL_LABEL[(row as AdminSubjectVO).level] }}</template>
      </el-table-column>
      <el-table-column prop="code" label="代码" width="88" />
      <el-table-column prop="name" label="科目名" min-width="180" />
      <el-table-column label="卷面类型" width="112">
        <template #default="{ row }">{{ PAPER_TYPE_LABEL[(row as AdminSubjectVO).paperType] }}</template>
      </el-table-column>
      <el-table-column label="开考" min-width="140">
        <template #default="{ row }">{{ EXAM_PERIOD_LABEL[(row as AdminSubjectVO).examPeriod] }}</template>
      </el-table-column>
      <el-table-column label="大纲" width="96">
        <template #default="{ row }">{{ OUTLINE_STATUS_LABEL[(row as AdminSubjectVO).outlineStatus] }}</template>
      </el-table-column>
      <el-table-column label="题库" width="120">
        <template #default="{ row }">
          {{ CONTENT_STATUS_LABEL[(row as AdminSubjectVO).contentStatus] }}
          <span class="subject-manage__muted">{{ (row as AdminSubjectVO).questionCount }} 题</span>
        </template>
      </el-table-column>
      <el-table-column label="卷面满分" width="96" align="right">
        <template #default="{ row }">
          <span v-if="(row as AdminSubjectVO).paperTotalScore != null">{{ (row as AdminSubjectVO).paperTotalScore }}</span>
          <!-- null 是"库里没有可用模板"，与 0 分是两件事，不能用 0 或 — 含糊过去 -->
          <span v-else class="subject-manage__muted">未配置</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <!--
            停用状态不只靠颜色：Tag 文字本身就是"已停用"。DESIGN.md 的
            The Contrast Floor Rule 之外还有一条更基本的——仅依赖颜色的信息必须
            同时有文字，所以这里不写"红点 = 停用"。
          -->
          <el-tag :type="(row as AdminSubjectVO).status === 'ENABLED' ? 'success' : 'info'" disable-transitions>
            {{ ENABLED_STATUS_LABEL[(row as AdminSubjectVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row as AdminSubjectVO)">编辑</el-button>
          <el-button
            text
            :type="(row as AdminSubjectVO).status === 'ENABLED' ? 'danger' : 'primary'"
            @click="handleToggleStatus(row as AdminSubjectVO)"
          >
            {{ (row as AdminSubjectVO).status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无科目数据" />
      </template>
    </el-table>

    <el-drawer v-model="drawerVisible" :title="isEditing ? '编辑科目' : '新增科目'" size="480px">
      <el-alert v-if="formError" type="error" :title="formError" :closable="false" show-icon class="subject-manage__form-error" />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" :disabled="isEditing" class="subject-manage__field">
            <el-option v-for="opt in LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <!--
            级别创建后不可改（05 §5.10）：级别决定卷面类型，改它会架空已有模板。
            接口虽然会忽略这个字段，但界面必须说清"这不是没生效，是不能改"，
            否则用户会以为保存失败而反复重试。
          -->
          <p v-if="isEditing" class="subject-manage__hint">级别创建后不可修改：它决定卷面类型，改动会架空已有模板。</p>
        </el-form-item>

        <el-form-item label="科目代码" prop="code">
          <el-input v-model="form.code" maxlength="32" show-word-limit placeholder="如 SAD" />
        </el-form-item>

        <el-form-item label="科目名" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item label="卷面类型" prop="paperType">
          <el-select v-model="form.paperType" class="subject-manage__field">
            <el-option v-for="opt in PAPER_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="开考周期" prop="examPeriod">
          <el-select v-model="form.examPeriod" class="subject-manage__field">
            <el-option v-for="opt in EXAM_PERIOD_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="大纲状态" prop="outlineStatus">
          <el-select v-model="form.outlineStatus" class="subject-manage__field">
            <el-option v-for="opt in OUTLINE_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="题库状态" prop="contentStatus">
          <el-select v-model="form.contentStatus" class="subject-manage__field">
            <el-option v-for="opt in CONTENT_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="简介" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.subject-manage {
  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: $rk-space-l;
    margin-bottom: $rk-space-l;
  }

  &__lead {
    color: $rk-text-2;
    max-width: 760px;
  }

  &__table {
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
  }

  &__muted {
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__field {
    width: 100%;
  }

  &__hint {
    font-size: $rk-font-xs;
    color: $rk-text-3;
    line-height: $rk-line-height;
  }

  &__form-error {
    margin-bottom: $rk-space-l;
  }
}
</style>
