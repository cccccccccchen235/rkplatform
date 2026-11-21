<script setup lang="ts">
// 题目卡片（07 §7.6）。按题型分支渲染作答控件与判分展示。
//
// 三处刻意的取舍：
//
// 1. **对错不能只靠颜色**。已判分的选项同时给绿/红描边与「✓ 正确答案」「✗ 你的选择」
//    的文字标记——色觉障碍用户与黑白打印都读得出来（07 §7.6 分支表）。
// 2. **主观题/论文不出现分数**。只写「本节不判分，以下为参考答案」+ 参考答案 +
//    评分要点；这是 07 §7.12 的验收项，判分链压根不参与这两类题型。
// 3. **`mode='review'` 不展示答案**。收藏列表用它只读题目，判断展示由 `judged`
//    是否传入决定：review 模式不传 `judged`，因此选项不会被标对错。
// 4. **`mode='result'` 用于成绩回顾**：展示答案与解析、不做收藏/反馈动作，
//    且客观题的对错标记来自服务端 `isCorrect`（`judged`）。

import { computed } from 'vue'
import type { CheckboxValueType } from 'element-plus'

import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import type { QuestionDetailVO, QuestionOptionVO } from '@/types/api'
import { QUESTION_TYPE_LABEL } from '@/types/labels'

const props = withDefaults(
  defineProps<{
    question: QuestionDetailVO
    mode: 'practice' | 'exam' | 'review' | 'result'
    /** 本地判分结果；`null` = 不判分（主观题/论文），`undefined` = 还没判/不判（review）。 */
    judged?: boolean | null
    modelValue?: string
    disabled?: boolean
  }>(),
  { judged: undefined, modelValue: '', disabled: false },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'favorite-change': []
  feedback: []
}>()

// ---------------- 题型分支 ----------------

const isMulti = computed(() => props.question.questionType === 'MULTI_CHOICE')

const isSubjective = computed(
  () => props.question.questionType === 'SUBJECTIVE' || props.question.questionType === 'ESSAY',
)

const isObjective = computed(() => !isSubjective.value)

/**
 * 判断题的选项由前端生成，不依赖后端 `options`（07 §7.6 分支表）。
 *
 * 键仍是 `A/B`：02 §2.2.1 规定客观题的 `correct_answer` 存**选项 key**，
 * 所以服务端对判断题的答案是 `"A"` / `"B"`，这里不能另造「正确/错误」两个值去比对。
 */
const TRUE_FALSE_OPTIONS: QuestionOptionVO[] = [
  { key: 'A', content: '正确' },
  { key: 'B', content: '错误' },
]

const options = computed<QuestionOptionVO[]>(() => {
  if (props.question.questionType === 'TRUE_FALSE') return TRUE_FALSE_OPTIONS
  return props.question.options ?? []
})

// ---------------- 作答绑定 ----------------

const radioValue = computed<CheckboxValueType>({
  get: () => props.modelValue ?? '',
  set: (value) => emit('update:modelValue', value == null ? '' : String(value)),
})

// el-checkbox-group 的 modelValue 是 `(string | number)[]`（不含 boolean），
// 比 ElCheckbox 单值的 CheckboxValueType 窄一档，所以这里用它的实际类型而不是后者。
const checkboxValue = computed<(string | number)[]>({
  get: () => (props.modelValue ? props.modelValue.split('') : []),
  // 排序后拼接：多选答案顺序无关，统一成字典序，便于与服务端比对（02 §2.2.1）
  set: (value) => emit('update:modelValue', value.map((key) => String(key)).sort().join('')),
})

// ---------------- 判分展示 ----------------

const hasJudged = computed(() => props.judged === true || props.judged === false)

/** 与服务端 JudgeServiceImpl 同口径：归一化（trim + 大写）后比较；多选去空白再排序。 */
function normalize(value: string): string {
  return value.trim().toUpperCase()
}

function asKeySet(value: string): string {
  return normalize(value)
    .replace(/\s+/g, '')
    .split('')
    .sort()
    .join('')
}

function isAnswerKey(answer: string, key: string): boolean {
  if (isMulti.value) return asKeySet(answer).includes(key)
  return normalize(answer) === key
}

/** 某个选项在已判分状态下的呈现：正确项 / 用户错选 / 中性。 */
function optionState(key: string): 'correct' | 'wrong' | 'neutral' {
  if (!hasJudged.value) return 'neutral'
  if (isAnswerKey(props.question.correctAnswer ?? '', key)) return 'correct'
  if (isAnswerKey(props.modelValue ?? '', key)) return 'wrong'
  return 'neutral'
}

/** 文字/图标标记，与颜色一起表达同一个事实（07 §7.6）。 */
function optionMark(key: string): string {
  const state = optionState(key)
  if (state === 'correct') return '✓ 正确答案'
  if (state === 'wrong') return '✗ 你的选择'
  return ''
}

/**
 * 解析的展开条件：
 * · `practice`：已判分才展开（答题中途不剧透）；
 * · `result`：交卷后的回顾，有解析就展开（`judged` 可能为 null，如未作答的主观题）。
 */
const showAnalysis = computed(() => {
  if (!props.question.analysis) return false
  if (props.mode === 'result') return true
  return props.mode === 'practice' && hasJudged.value
})

/** 收藏/反馈是"练习"与"收藏列表"的动作；考试与成绩回顾里没有它们的位置。 */
const showActions = computed(() => props.mode === 'practice' || props.mode === 'review')

const hasScorePoints = computed(() => (props.question.scorePoints?.length ?? 0) > 0)
</script>

<template>
  <article class="question-card">
    <header class="question-card__head">
      <span class="question-card__type">{{ QUESTION_TYPE_LABEL[question.questionType] }}</span>
      <span v-if="question.difficulty != null" class="question-card__difficulty">难度 {{ question.difficulty }}</span>
      <span v-if="question.outlinePath.length" class="question-card__path">
        {{ question.outlinePath.join(' / ') }}
      </span>

      <div v-if="showActions" class="question-card__actions">
        <el-button text class="question-card__action" @click="emit('favorite-change')">
          {{ question.isFavorited ? '★ 已收藏' : '☆ 收藏' }}
        </el-button>
        <el-button text class="question-card__action" @click="emit('feedback')">反馈</el-button>
      </div>
    </header>

    <div class="question-card__stem">
      <MarkdownRenderer :content="question.stem" />
    </div>

    <div v-if="question.images.length" class="question-card__images">
      <img
        v-for="(image, index) in question.images"
        :key="index"
        class="question-card__image"
        :src="image.url"
        :alt="image.alt"
        loading="lazy"
      />
    </div>

    <!-- 客观题：单选用单选组，多选用复选组，判断题用前端生成的 A/B 两选项 -->
    <div v-if="isObjective" class="question-card__options">
      <el-checkbox-group v-if="isMulti" v-model="checkboxValue" :disabled="disabled">
        <div
          v-for="opt in options"
          :key="opt.key"
          class="qoption"
          :class="`qoption--${optionState(opt.key)}`"
        >
          <el-checkbox :value="opt.key">
            <span class="qoption__key">{{ opt.key }}</span>
            <MarkdownRenderer class="qoption__content" inline :content="opt.content" />
            <span v-if="optionMark(opt.key)" class="qoption__mark">{{ optionMark(opt.key) }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>

      <el-radio-group v-else v-model="radioValue" :disabled="disabled">
        <div
          v-for="opt in options"
          :key="opt.key"
          class="qoption"
          :class="`qoption--${optionState(opt.key)}`"
        >
          <el-radio :value="opt.key">
            <span class="qoption__key">{{ opt.key }}</span>
            <MarkdownRenderer class="qoption__content" inline :content="opt.content" />
            <span v-if="optionMark(opt.key)" class="qoption__mark">{{ optionMark(opt.key) }}</span>
          </el-radio>
        </div>
      </el-radio-group>

      <p v-if="isMulti" class="question-card__hint">多选题须全部选对才得分，少选、多选均不得分。</p>
    </div>

    <!-- 主观题/论文：不判分，只给参考答案与评分要点，且不出现任何分数 -->
    <section v-if="isSubjective" class="question-card__reference">
      <p class="question-card__reference-title">本节不判分，以下为参考答案</p>
      <MarkdownRenderer v-if="question.referenceAnswer" :content="question.referenceAnswer" />
      <p v-else class="question-card__hint">本题暂无参考答案。</p>

      <div v-if="hasScorePoints" class="question-card__points">
        <p class="question-card__points-title">评分要点</p>
        <ul>
          <li v-for="(point, index) in question.scorePoints ?? []" :key="index">{{ point }}</li>
        </ul>
      </div>
    </section>

    <section v-if="showAnalysis" class="question-card__analysis">
      <p class="question-card__analysis-title">解析</p>
      <MarkdownRenderer :content="question.analysis ?? ''" />
    </section>
  </article>
</template>

<style scoped lang="scss">
.question-card {
  padding: $rk-space-xl;
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;

  &__head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: $rk-space-m;
    padding-bottom: $rk-space-m;
    margin-bottom: $rk-space-l;
    border-bottom: 1px solid $rk-soft-border;
  }

  &__type {
    padding: 0 $rk-space-s;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-radius-m;
    color: $rk-text-1;
    font-size: $rk-font-xs;
    line-height: 20px;
  }

  &__difficulty,
  &__path {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__path {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  // 收藏/反馈推到行末，与左侧的题型元数据分开
  &__actions {
    margin-left: auto;
    display: flex;
    gap: $rk-space-xs;
  }

  &__action {
    font-size: $rk-font-xs;
  }

  // 题干行高放宽到 1.75：长文本更易读、减少屏上压迫感（docs/36）。
  &__stem {
    margin-bottom: $rk-space-l;
    line-height: $rk-soft-line-height-body;
  }

  &__images {
    display: flex;
    flex-direction: column;
    gap: $rk-space-m;
    margin-bottom: $rk-space-l;
  }

  &__image {
    max-width: 100%;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-m;
  }

  &__options {
    display: flex;
    flex-direction: column;
    gap: $rk-space-s;
  }

  &__hint {
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__reference,
  &__analysis {
    margin-top: $rk-space-l;
    padding-top: $rk-space-l;
    border-top: 1px solid $rk-border;
  }

  &__reference-title {
    margin-bottom: $rk-space-m;
    color: $rk-text-2;
    font-size: $rk-font-s;
    font-weight: 600;
  }

  &__points {
    margin-top: $rk-space-m;
  }

  &__points-title {
    margin-bottom: $rk-space-xs;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__analysis-title {
    margin-bottom: $rk-space-m;
    color: $rk-text-2;
    font-size: $rk-font-s;
    font-weight: 600;
  }

  ul {
    padding-left: 1.5em;
    color: $rk-text-2;
  }
}

// 选项行：默认中性，已判分时用描边 + 文字共同表达对错（不靠颜色单独承载信息）
.qoption {
  padding: $rk-space-m;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-control;
  transition:
    border-color 160ms ease-out,
    background-color 160ms ease-out;

  &--correct {
    border-color: $rk-success;
    background-color: rgba($rk-success, 0.06);
  }

  &--wrong {
    border-color: $rk-danger;
    background-color: rgba($rk-danger, 0.06);
  }

  &__key {
    display: inline-block;
    min-width: 1.2em;
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__content {
    color: $rk-text-1;
  }

  &__mark {
    margin-left: $rk-space-m;
    font-size: $rk-font-xs;
  }

  &--correct &__mark {
    color: $rk-success;
  }

  &--wrong &__mark {
    color: $rk-danger;
  }
}
</style>
