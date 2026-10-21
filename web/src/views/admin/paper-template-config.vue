<script setup lang="ts">
// 管理端 · 卷面模板配置（07 §7.5.2「卷面模板配置（专项）」/ 05 §5.10 第 51~52 条）。
//
// 这一页与前面三个内容维护页的差别，是它编辑的是一份**有内部不变式的结构**：
// 序号要连续、场次总时长要与分节上下限自洽、模板/场次满分要与分节分值口径一致。
// 所以页面做了两件事把"人算"换成"机算"：
//   1. 序号（sittingNo/sectionNo/sortNo）全部由结构派生，任何增删移动后自动重排；
//   2. 场次与模板的满分**只读**，由分节现算，提交时写回——用户没有机会把总分算错。
// 服务端仍会按 05 §5.10 的五条规则再校验一次（10 §10.7 要求"不符时保存失败"），
// 前端算对不等于服务端放行：那两条是独立的防线。

import { computed, onMounted, reactive, ref } from 'vue'

import { fetchAdminSubjects } from '@/api/admin/content'
import { fetchAdminPaperTemplate, saveAdminPaperTemplate } from '@/api/admin/paper-template'
import type {
  AdminSubjectVO,
  InsufficientQuestionVO,
  PaperSectionConfigVO,
  PaperSittingConfigVO,
  PaperTemplateSavePayload,
  QuestionType,
  SectionType,
  SupportStatus,
} from '@/types/api'
import { PAPER_TYPE_LABEL, QUESTION_TYPE_LABEL, SECTION_TYPE_LABEL, SUPPORT_STATUS_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const SECTION_TYPE_OPTIONS: SectionType[] = ['OBJECTIVE', 'SUBJECTIVE', 'ESSAY']
const SUPPORT_STATUS_OPTIONS: SupportStatus[] = ['SUPPORTED', 'NOT_SUPPORTED']
const QUESTION_TYPE_OPTIONS = Object.keys(QUESTION_TYPE_LABEL) as QuestionType[]

/** 切换分节类型时的默认值（02 §2.4.2 的蓝图口径）。 */
const DEFAULT_QUESTION_TYPE: Record<SectionType, QuestionType> = {
  OBJECTIVE: 'SINGLE_CHOICE',
  SUBJECTIVE: 'SUBJECTIVE',
  ESSAY: 'ESSAY',
}
const DEFAULT_PER_QUESTION_SCORE: Record<SectionType, number> = { OBJECTIVE: 1, SUBJECTIVE: 15, ESSAY: 75 }
const DEFAULT_QUESTION_COUNT: Record<SectionType, number> = { OBJECTIVE: 75, SUBJECTIVE: 5, ESSAY: 1 }

const subjects = ref<AdminSubjectVO[]>([])
const subjectLoading = ref(false)
const selectedSubjectId = ref('')

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const formError = ref('')
/** true = 服务端返回 4001（该科目尚未配置卷面模板），此时引导管理员按蓝图新建。 */
const notConfigured = ref(false)
const warnings = ref<InsufficientQuestionVO[]>([])

const form = reactive<PaperTemplateSavePayload>({
  name: '',
  paperType: 'JUNIOR_MID',
  totalMaxScore: 0,
  autoScoredMaxScore: 0,
  sittings: [],
})

const selectedSubject = computed(
  () => subjects.value.find((subject) => subject.id === selectedSubjectId.value) ?? null,
)

/** 分节分值。NOT_SUPPORTED 不参与任何满分口径（02 §2.4.4），故恒按 0 计。 */
function sectionScore(section: PaperSectionConfigVO): number {
  if (section.supportStatus !== 'SUPPORTED') return 0
  return Number(section.perQuestionScore) * section.questionCount
}

function round1(value: number): number {
  return Math.round(value * 10) / 10
}

/** 场次的两个满分（只读派生）。 */
function sittingTotals(sitting: PaperSittingConfigVO): { total: number; auto: number } {
  let total = 0
  let auto = 0
  for (const section of sitting.sections) {
    const score = sectionScore(section)
    total += score
    if (section.sectionType === 'OBJECTIVE') auto += score
  }
  return { total: round1(total), auto: round1(auto) }
}

/** 模板级三个合计（只读派生）。totalDuration 是各场次之和，不是某一场的时长。 */
const totals = computed(() => {
  let total = 0
  let auto = 0
  let duration = 0
  for (const sitting of form.sittings) {
    const value = sittingTotals(sitting)
    total += value.total
    auto += value.auto
    duration += sitting.totalDuration
  }
  return { total: round1(total), auto: round1(auto), duration }
})

/**
 * 前端即时校验（07 §7.5.2 要求与 05 一致）。序号由 `renumber()` 保证连续，
 * 所以这里只查"人填的数字"：时长上下限与场次总时长的自洽。
 */
const validationErrors = computed<string[]>(() => {
  const errors: string[] = []
  if (!form.name.trim()) errors.push('模板名不能为空')
  if (form.sittings.length === 0) errors.push('至少需要一个场次')

  form.sittings.forEach((sitting, index) => {
    const label = `场次 ${index + 1}`
    if (sitting.totalDuration < 1) errors.push(`${label} 的总时长至少 1 分钟`)
    if (sitting.sections.length === 0) errors.push(`${label} 至少需要一个分节`)

    let sumMin = 0
    let sumMax = 0
    for (const section of sitting.sections) {
      if (section.durationMin > section.durationMax) {
        errors.push(`${label} 的分节「${section.sectionName || '未命名'}」最早离开时间不能晚于最晚离开时间`)
      }
      sumMin += section.durationMin
      sumMax += section.durationMax
    }
    if (sitting.sections.length > 0 && (sumMin > sitting.totalDuration || sitting.totalDuration > sumMax)) {
      errors.push(
        `${label} 的时长不自洽：需满足 Σ(最早) ≤ 总时长 ≤ Σ(最晚)，当前 ${sumMin} ≤ ${sitting.totalDuration} ≤ ${sumMax} 不成立`,
      )
    }
  })
  return errors
})

// ---------------- 结构编辑 ----------------

/** 重排序号：场次从 1、分节跨场次全局连续、sortNo 各层从 10 起。 */
function renumber(): void {
  let sectionNo = 1
  form.sittings.forEach((sitting, sittingIndex) => {
    sitting.sittingNo = sittingIndex + 1
    sitting.sortNo = (sittingIndex + 1) * 10
    sitting.sections.forEach((section, sectionIndex) => {
      section.sectionNo = sectionNo
      sectionNo += 1
      section.sortNo = (sectionIndex + 1) * 10
    })
  })
}

function newSection(sectionType: SectionType): PaperSectionConfigVO {
  return {
    sectionNo: 0,
    sectionName: '',
    sectionType,
    questionType: DEFAULT_QUESTION_TYPE[sectionType],
    questionCount: DEFAULT_QUESTION_COUNT[sectionType],
    perQuestionScore: DEFAULT_PER_QUESTION_SCORE[sectionType],
    durationMin: 0,
    durationMax: 0,
    allowEarlySubmit: sectionType !== 'ESSAY',
    supportStatus: 'SUPPORTED',
    sortNo: 0,
  }
}

function addSitting(): void {
  form.sittings.push({
    sittingNo: 0,
    sittingName: `场次 ${form.sittings.length + 1}`,
    totalDuration: 240,
    totalMaxScore: 0,
    autoScoredMaxScore: 0,
    sortNo: 0,
    sections: [newSection('OBJECTIVE')],
  })
  renumber()
}

function removeSitting(index: number): void {
  form.sittings.splice(index, 1)
  renumber()
}

function moveSitting(index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= form.sittings.length) return
  const [item] = form.sittings.splice(index, 1)
  if (!item) return
  form.sittings.splice(target, 0, item)
  renumber()
}

function addSection(sitting: PaperSittingConfigVO): void {
  sitting.sections.push(newSection('SUBJECTIVE'))
  renumber()
}

function removeSection(sitting: PaperSittingConfigVO, index: number): void {
  sitting.sections.splice(index, 1)
  renumber()
}

function moveSection(sitting: PaperSittingConfigVO, index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= sitting.sections.length) return
  const [item] = sitting.sections.splice(index, 1)
  if (!item) return
  sitting.sections.splice(target, 0, item)
  renumber()
}

/** 改分节类型时把题型/分值/题数拉到该类型的常规口径，避免留下"论文节却是单选题"的组合。 */
function onSectionTypeChange(section: PaperSectionConfigVO): void {
  section.questionType = DEFAULT_QUESTION_TYPE[section.sectionType]
  section.perQuestionScore = DEFAULT_PER_QUESTION_SCORE[section.sectionType]
  section.questionCount = DEFAULT_QUESTION_COUNT[section.sectionType]
  // 论文节不可提前交卷（02 §2.4.2）。
  section.allowEarlySubmit = section.sectionType !== 'ESSAY'
}

/**
 * 置为 NOT_SUPPORTED 时分值归零（与 04 §4.8.2 的 IPT 种子数据一致）；
 * 改回 SUPPORTED 时按分节类型给回一档默认值，不让它停在 0 分 0 题的"看起来配了"状态。
 */
function onSupportStatusChange(section: PaperSectionConfigVO): void {
  if (section.supportStatus === 'NOT_SUPPORTED') {
    section.questionCount = 0
    section.perQuestionScore = 0
    return
  }
  section.perQuestionScore = DEFAULT_PER_QUESTION_SCORE[section.sectionType]
  section.questionCount = DEFAULT_QUESTION_COUNT[section.sectionType]
}

// ---------------- 蓝图 ----------------

/** 02 §2.4.2 的两种卷面蓝图。取默认值而不是硬编码 IPT 之类的科目规则。 */
function buildBlueprint(paperType: 'JUNIOR_MID' | 'SENIOR', subjectName: string): PaperSittingConfigVO[] {
  const objective = (name: string, durationMin: number, durationMax: number): PaperSectionConfigVO => ({
    ...newSection('OBJECTIVE'),
    sectionName: name,
    durationMin,
    durationMax,
  })
  const subjective = (name: string, count: number, perScore: number, durationMin: number, durationMax: number): PaperSectionConfigVO => ({
    ...newSection('SUBJECTIVE'),
    sectionName: name,
    questionCount: count,
    perQuestionScore: perScore,
    durationMin,
    durationMax,
  })

  void subjectName
  if (paperType === 'SENIOR') {
    return [
      {
        sittingNo: 1,
        sittingName: '连考场次',
        totalDuration: 240,
        totalMaxScore: 0,
        autoScoredMaxScore: 0,
        sortNo: 10,
        sections: [objective('综合知识', 120, 150), subjective('案例分析', 3, 25, 90, 120)],
      },
      {
        sittingNo: 2,
        sittingName: '论文章次',
        totalDuration: 120,
        totalMaxScore: 0,
        autoScoredMaxScore: 0,
        sortNo: 20,
        sections: [
          {
            ...newSection('ESSAY'),
            sectionName: '论文',
            questionCount: 1,
            perQuestionScore: 75,
            durationMin: 120,
            durationMax: 120,
            allowEarlySubmit: false,
          },
        ],
      },
    ]
  }
  return [
    {
      sittingNo: 1,
      sittingName: '连考场次',
      totalDuration: 240,
      totalMaxScore: 0,
      autoScoredMaxScore: 0,
      sortNo: 10,
      sections: [objective('基础知识', 90, 120), subjective('应用技术', 5, 15, 120, 150)],
    },
  ]
}

async function applyBlueprint(): Promise<void> {
  const subject = selectedSubject.value
  if (subject && form.sittings.length > 0) {
    try {
      await ElMessageBox.confirm(
        '一键填充会用所在卷面类型的标准蓝图覆盖当前的全部场次与分节，未保存的改动会丢失。'
          + '高级卷的论文为独立场次；信息处理技术员的「应用技术」需在填充后手动置为「不可在线作答」。',
        '按蓝图一键填充',
        { confirmButtonText: '覆盖填充', cancelButtonText: '取消', type: 'warning' },
      )
    } catch {
      return
    }
  }
  form.paperType = subject?.paperType ?? form.paperType
  form.name = subject ? `${subject.name} 模拟卷` : form.name
  form.sittings = buildBlueprint(form.paperType, subject?.name ?? '')
  renumber()
  notConfigured.value = false
}

// ---------------- 载入与保存 ----------------

function applyDetail(detail: {
  name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'
  sittings: PaperSittingConfigVO[]
}): void {
  form.name = detail.name
  form.paperType = detail.paperType
  form.sittings = detail.sittings.map((sitting) => ({
    ...sitting,
    sections: sitting.sections.map((section) => ({ ...section })),
  }))
  renumber()
}

async function loadSubjects(): Promise<void> {
  subjectLoading.value = true
  try {
    subjects.value = await fetchAdminSubjects({ silent: true })
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    subjectLoading.value = false
  }
}

async function loadTemplate(): Promise<void> {
  if (!selectedSubjectId.value) return
  loading.value = true
  loadError.value = ''
  formError.value = ''
  warnings.value = []
  try {
    const detail = await fetchAdminPaperTemplate(selectedSubjectId.value, { silent: true })
    applyDetail(detail)
    notConfigured.value = false
  } catch (error) {
    const err = error as RequestError
    if (err.code === 4001) {
      // 尚未配置是这一页要解决的起点，不是错误：给一份该卷面类型的空蓝图。
      notConfigured.value = true
      form.paperType = selectedSubject.value?.paperType ?? 'JUNIOR_MID'
      form.name = selectedSubject.value ? `${selectedSubject.value.name} 模拟卷` : ''
      form.sittings = buildBlueprint(form.paperType, selectedSubject.value?.name ?? '')
      renumber()
      return
    }
    loadError.value = err.message
  } finally {
    loading.value = false
  }
}

/** 提交前把只读派生的序号与满分写回，保证与服务端口径一致。 */
function buildPayload(): PaperTemplateSavePayload {
  let sectionNo = 1
  const sittings = form.sittings.map((sitting, sittingIndex): PaperSittingConfigVO => {
    const value = sittingTotals(sitting)
    return {
      sittingNo: sittingIndex + 1,
      sittingName: sitting.sittingName.trim() || `场次 ${sittingIndex + 1}`,
      totalDuration: sitting.totalDuration,
      totalMaxScore: value.total,
      autoScoredMaxScore: value.auto,
      sortNo: (sittingIndex + 1) * 10,
      sections: sitting.sections.map((section, sectionIndex): PaperSectionConfigVO => {
        const row = {
          ...section,
          sectionNo,
          sectionName: section.sectionName.trim() || `分节 ${sectionIndex + 1}`,
          sortNo: (sectionIndex + 1) * 10,
        }
        sectionNo += 1
        return row
      }),
    }
  })
  const total = round1(sittings.reduce((sum, sitting) => sum + sitting.totalMaxScore, 0))
  const auto = round1(sittings.reduce((sum, sitting) => sum + sitting.autoScoredMaxScore, 0))
  return {
    name: form.name.trim(),
    paperType: form.paperType,
    totalMaxScore: total,
    autoScoredMaxScore: auto,
    sittings,
  }
}

async function handleSave(): Promise<void> {
  if (validationErrors.value.length > 0) {
    ElMessage.error(validationErrors.value[0])
    return
  }
  saving.value = true
  formError.value = ''
  warnings.value = []
  try {
    const result = await saveAdminPaperTemplate(selectedSubjectId.value, buildPayload(), { silent: true })
    const warningItems = result?.insufficientQuestions ?? []
    ElMessage.success('卷面模板已保存')
    // 回读服务端权威值：保存是覆盖式的，回读一次能立刻确认"写进去的与页面上的"一致。
    await loadTemplate()
    warnings.value = warningItems
  } catch (error) {
    formError.value = (error as RequestError).message
  } finally {
    saving.value = false
  }
}

async function handleSubjectChange(): Promise<void> {
  await loadTemplate()
}

onMounted(async () => {
  await loadSubjects()
  const first = subjects.value[0]
  if (first) {
    selectedSubjectId.value = first.id
    await loadTemplate()
  }
})
</script>

<template>
  <div class="paper-template">
    <header class="paper-template__header">
      <div>
        <h1 class="rk-page-title">卷面模板</h1>
        <p class="paper-template__lead">
          为科目配置场次与分节。序号与满分由结构自动派生；「时长要自洽」「满分要与分节分值一致」两条在不满足时无法保存。
        </p>
      </div>
    </header>

    <div class="paper-template__toolbar">
      <el-select
        v-model="selectedSubjectId"
        class="paper-template__subject"
        filterable
        :loading="subjectLoading"
        placeholder="选择科目"
        @change="handleSubjectChange"
      >
        <el-option
          v-for="subject in subjects"
          :key="subject.id"
          :label="`${subject.code} · ${subject.name}`"
          :value="subject.id"
        />
      </el-select>
      <el-button :disabled="!selectedSubjectId" @click="applyBlueprint">按蓝图一键填充</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selectedSubjectId" @click="handleSave">保存</el-button>
    </div>

    <el-alert v-if="loadError" type="error" :title="`卷面模板加载失败：${loadError}`" :closable="false" show-icon>
      <template #default>
        <el-button text type="primary" @click="loadTemplate">重试</el-button>
      </template>
    </el-alert>

    <el-alert
      v-if="notConfigured"
      type="info"
      :closable="false"
      show-icon
      title="该科目尚未配置卷面模板"
      class="paper-template__notice"
    >
      可直接「按蓝图一键填充」获得一份标准卷面，或手动添加场次与分节后保存。
    </el-alert>

    <el-alert v-if="formError" type="error" :title="formError" :closable="false" show-icon class="paper-template__notice" />

    <el-alert
      v-if="warnings.length > 0"
      type="warning"
      :closable="false"
      show-icon
      class="paper-template__notice"
      title="题量不足，保存后按现有题量组卷"
    >
      <ul class="paper-template__warning-list">
        <li v-for="warning in warnings" :key="warning.sectionNo">
          分节 {{ warning.sectionNo }}「{{ warning.sectionName }}」配置 {{ warning.requiredCount }} 题，
          该科目已发布单选题仅 {{ warning.availableCount }} 题——组卷时会按 L4 规则缩减并提示。
        </li>
      </ul>
    </el-alert>

    <el-alert
      v-if="validationErrors.length > 0"
      type="error"
      :closable="false"
      show-icon
      class="paper-template__notice"
      title="还有规则不满足，修正后才能保存"
    >
      <ul class="paper-template__warning-list">
        <li v-for="(error, index) in validationErrors" :key="index">{{ error }}</li>
      </ul>
    </el-alert>

    <section v-loading="loading" class="paper-template__body">
      <div class="paper-template__card">
        <div class="paper-template__card-head">
          <h2 class="paper-template__card-title">模板</h2>
          <span class="paper-template__muted">{{ PAPER_TYPE_LABEL[form.paperType] }}</span>
        </div>
        <div class="paper-template__grid">
          <label class="paper-template__field">
            <span class="paper-template__label">模板名</span>
            <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="如 系统架构设计师 模拟卷" />
          </label>
          <div class="paper-template__field">
            <span class="paper-template__label">总时长</span>
            <span class="paper-template__value">{{ totals.duration }} 分钟<span class="paper-template__muted">（各场次之和）</span></span>
          </div>
          <div class="paper-template__field">
            <span class="paper-template__label">卷面满分</span>
            <span class="paper-template__value">{{ totals.total }}</span>
          </div>
          <div class="paper-template__field">
            <span class="paper-template__label">可判分满分</span>
            <span class="paper-template__value">{{ totals.auto }}</span>
          </div>
        </div>
      </div>

      <div v-for="(sitting, sittingIndex) in form.sittings" :key="sittingIndex" class="paper-template__card">
        <div class="paper-template__card-head">
          <h2 class="paper-template__card-title">场次 {{ sitting.sittingNo }}</h2>
          <div class="paper-template__sitting-fields">
            <el-input v-model="sitting.sittingName" class="paper-template__sitting-name" maxlength="64" placeholder="场次名" />
            <label class="paper-template__inline">
              <span class="paper-template__label">总时长</span>
              <el-input-number v-model="sitting.totalDuration" :min="1" :max="600" size="small" controls-position="right" />
              <span class="paper-template__muted">分钟</span>
            </label>
            <span class="paper-template__muted">卷面分 {{ sittingTotals(sitting).total }} / 可判分 {{ sittingTotals(sitting).auto }}</span>
          </div>
          <div class="paper-template__row-actions">
            <el-button text :disabled="sittingIndex === 0" @click="moveSitting(sittingIndex, -1)">上移</el-button>
            <el-button text :disabled="sittingIndex === form.sittings.length - 1" @click="moveSitting(sittingIndex, 1)">下移</el-button>
            <el-button text type="danger" :disabled="form.sittings.length <= 1" @click="removeSitting(sittingIndex)">删除场次</el-button>
          </div>
        </div>

        <div class="paper-template__table-wrap">
          <table class="paper-template__table">
            <thead>
              <tr>
                <th class="paper-template__no">节号</th>
                <th>分节名</th>
                <th>类型</th>
                <th>题型</th>
                <th>题数</th>
                <th>每题分</th>
                <th>最早(分)</th>
                <th>最晚(分)</th>
                <th>提前交卷</th>
                <th>支持状态</th>
                <th>排序</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(section, sectionIndex) in sitting.sections" :key="sectionIndex">
                <td class="paper-template__no">{{ section.sectionNo }}</td>
                <td><el-input v-model="section.sectionName" size="small" maxlength="64" placeholder="分节名" /></td>
                <td>
                  <el-select v-model="section.sectionType" size="small" @change="onSectionTypeChange(section)">
                    <el-option v-for="type in SECTION_TYPE_OPTIONS" :key="type" :label="SECTION_TYPE_LABEL[type]" :value="type" />
                  </el-select>
                </td>
                <td>
                  <el-select v-model="section.questionType" size="small">
                    <el-option
                      v-for="type in QUESTION_TYPE_OPTIONS"
                      :key="type"
                      :label="QUESTION_TYPE_LABEL[type]"
                      :value="type"
                    />
                  </el-select>
                </td>
                <td>
                  <el-input-number
                    v-model="section.questionCount"
                    :min="0"
                    :max="999"
                    size="small"
                    controls-position="right"
                    :disabled="section.supportStatus === 'NOT_SUPPORTED'"
                  />
                </td>
                <td>
                  <el-input-number
                    v-model="section.perQuestionScore"
                    :min="0"
                    :max="999"
                    :step="0.5"
                    size="small"
                    controls-position="right"
                    :disabled="section.supportStatus === 'NOT_SUPPORTED'"
                  />
                </td>
                <td><el-input-number v-model="section.durationMin" :min="0" :max="600" size="small" controls-position="right" /></td>
                <td><el-input-number v-model="section.durationMax" :min="0" :max="600" size="small" controls-position="right" /></td>
                <td>
                  <el-switch v-model="section.allowEarlySubmit" :disabled="section.sectionType === 'ESSAY'" />
                </td>
                <td>
                  <el-select v-model="section.supportStatus" size="small" @change="onSupportStatusChange(section)">
                    <el-option
                      v-for="status in SUPPORT_STATUS_OPTIONS"
                      :key="status"
                      :label="SUPPORT_STATUS_LABEL[status]"
                      :value="status"
                    />
                  </el-select>
                </td>
                <td class="paper-template__no">{{ section.sortNo }}</td>
                <td>
                  <div class="paper-template__row-actions">
                    <el-button text :disabled="sectionIndex === 0" @click="moveSection(sitting, sectionIndex, -1)">上移</el-button>
                    <el-button text :disabled="sectionIndex === sitting.sections.length - 1" @click="moveSection(sitting, sectionIndex, 1)">下移</el-button>
                    <el-button text type="danger" :disabled="sitting.sections.length <= 1" @click="removeSection(sitting, sectionIndex)">删除</el-button>
                  </div>
                </td>
              </tr>
              <tr v-if="sitting.sections.length === 0">
                <td colspan="12" class="paper-template__empty">该场次还没有分节</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="paper-template__card-foot">
          <el-button text type="primary" :disabled="sitting.sections.length >= 3" @click="addSection(sitting)">
            新增分节（每场次最多 3 个）
          </el-button>
          <span class="paper-template__muted">「不可在线作答」的分节不参与组卷、不计入任何满分口径。</span>
        </div>
      </div>

      <el-button class="paper-template__add-sitting" :disabled="form.sittings.length >= 2" @click="addSitting">
        新增场次（最多 2 个）
      </el-button>
    </section>
  </div>
</template>

<style scoped lang="scss">
.paper-template {
  &__header {
    margin-bottom: $rk-space-l;
  }

  &__lead {
    color: $rk-text-2;
    max-width: 820px;
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

  &__notice {
    margin-bottom: $rk-space-m;
  }

  &__warning-list {
    margin: 0;
    padding-left: 1.2em;
    line-height: 1.7;
  }

  &__body {
    min-height: 120px;
  }

  &__card {
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
    padding: $rk-space-l;
    margin-bottom: $rk-space-l;
  }

  &__card-head {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    flex-wrap: wrap;
    margin-bottom: $rk-space-m;
  }

  &__card-title {
    font-size: $rk-font-m;
    font-weight: 600;
    margin: 0;
  }

  &__card-foot {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    margin-top: $rk-space-s;
  }

  &__grid {
    display: flex;
    flex-wrap: wrap;
    gap: $rk-space-l;
    align-items: flex-end;
  }

  &__field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 220px;
  }

  &__label {
    font-size: $rk-font-xs;
    color: $rk-text-3;
  }

  &__value {
    font-variant-numeric: tabular-nums;
    color: $rk-text-1;
  }

  &__sitting-fields {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    flex: 1;
  }

  &__sitting-name {
    width: 200px;
  }

  &__inline {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__muted {
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__row-actions {
    display: flex;
    align-items: center;
    white-space: nowrap;
  }

  &__table-wrap {
    overflow-x: auto;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    min-width: 1100px;

    th,
    td {
      border: 1px solid $rk-border;
      padding: 6px 8px;
      text-align: left;
      vertical-align: middle;
    }

    th {
      background-color: $rk-bg-page;
      font-weight: 600;
      font-size: $rk-font-xs;
      color: $rk-text-2;
      white-space: nowrap;
    }
  }

  &__no {
    text-align: right;
    font-variant-numeric: tabular-nums;
    color: $rk-text-2;
  }

  &__empty {
    text-align: center;
    color: $rk-text-3;
    padding: $rk-space-l;
  }

  &__add-sitting {
    width: 100%;
  }
}
</style>
