<script setup lang="ts">
// 模拟考答题卡上的题目（07 §7.5.1 模拟考答题页专项）。
//
// 与练习页 `QuestionCard` 的**关键区别**：考试期间题目**永不含答案**——
// 不渲染 `correctAnswer` / `referenceAnswer` / `analysis`，也不做任何判分。
// 所以这里不复用 `QuestionCard`，而是一个只认 `ExamItemVO` 的窄组件。
//
// 客观题（单选/多选/判断）用选项控件；主观题/论文交给 `EssayEditor`（带草稿与字数统计）。
// 判断题的 A/B 由前端生成：服务端存的答案就是选项 key，不能另造"正确/错误"两个值。

import { computed } from 'vue'
import type { CheckboxValueType } from 'element-plus'

import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import EssayEditor from '@/components/exam/EssayEditor.vue'
import type { ExamItemVO, QuestionOptionVO } from '@/types/api'
import { QUESTION_TYPE_LABEL } from '@/types/labels'

const props = withDefaults(
  defineProps<{
    item: ExamItemVO
    modelValue: string
    disabled?: boolean
    /** 论文/主观题写作框顶部的剩余时长文案（由 store 提供，组件不自行计时）。 */
    remainingLabel?: string
    sessionId: string
  }>(),
  { disabled: false, remainingLabel: '' },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'draft-saved': [savedAt: number]
}>()

const isMulti = computed(() => props.item.questionType === 'MULTI_CHOICE')

const isSubjective = computed(
  () => props.item.questionType === 'SUBJECTIVE' || props.item.questionType === 'ESSAY',
)

const TRUE_FALSE_OPTIONS: QuestionOptionVO[] = [
  { key: 'A', content: '正确' },
  { key: 'B', content: '错误' },
]

const options = computed<QuestionOptionVO[]>(() => {
  if (props.item.questionType === 'TRUE_FALSE') return TRUE_FALSE_OPTIONS
  return props.item.options ?? []
})

const radioValue = computed<CheckboxValueType>({
  get: () => props.modelValue ?? '',
  set: (value) => emit('update:modelValue', value == null ? '' : String(value)),
})

const checkboxValue = computed<(string | number)[]>({
  get: () => (props.modelValue ? props.modelValue.split('') : []),
  // 排序后拼接：多选答案顺序无关，与服务端 JudgeService 的规范化口径一致
  set: (value) => emit('update:modelValue', value.map((key) => String(key)).sort().join('')),
})
</script>

<template>
  <article class="exam-question">
    <header class="exam-question__head">
      <span class="exam-question__type">{{ QUESTION_TYPE_LABEL[item.questionType] }}</span>
      <span class="exam-question__score">{{ item.score }} 分</span>
    </header>

    <div class="exam-question__stem">
      <MarkdownRenderer :content="item.stem" />
    </div>

    <div v-if="item.images.length" class="exam-question__images">
      <img
        v-for="(image, index) in item.images"
        :key="index"
        class="exam-question__image"
        :src="image.url"
        :alt="image.alt"
        loading="lazy"
      />
    </div>

    <div v-if="isSubjective" class="exam-question__essay">
      <EssayEditor
        :model-value="modelValue"
        :item-id="item.itemId"
        :session-id="sessionId"
        :remaining-label="remainingLabel"
        :readonly="disabled"
        @update:model-value="emit('update:modelValue', $event)"
        @draft-saved="emit('draft-saved', $event)"
      />
    </div>

    <div v-else class="exam-question__options">
      <el-checkbox-group v-if="isMulti" v-model="checkboxValue" :disabled="disabled">
        <div v-for="opt in options" :key="opt.key" class="exam-option">
          <el-checkbox :value="opt.key">
            <span class="exam-option__key">{{ opt.key }}</span>
            <MarkdownRenderer class="exam-option__content" inline :content="opt.content" />
          </el-checkbox>
        </div>
      </el-checkbox-group>

      <el-radio-group v-else v-model="radioValue" :disabled="disabled">
        <div v-for="opt in options" :key="opt.key" class="exam-option">
          <el-radio :value="opt.key">
            <span class="exam-option__key">{{ opt.key }}</span>
            <MarkdownRenderer class="exam-option__content" inline :content="opt.content" />
          </el-radio>
        </div>
      </el-radio-group>

      <p v-if="isMulti" class="exam-question__hint">多选题须全部选对才得分，少选、多选均不得分。</p>
    </div>
  </article>
</template>

<style scoped lang="scss">
.exam-question {
  padding: $rk-space-l;
  background-color: $rk-bg-card;
  border: 1px solid $rk-border;
  border-radius: $rk-radius-l;

  &__head {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    padding-bottom: $rk-space-m;
    margin-bottom: $rk-space-l;
    border-bottom: 1px solid $rk-border;
  }

  &__type {
    padding: 0 $rk-space-s;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-s;
    color: $rk-text-1;
    font-size: $rk-font-xs;
    line-height: 20px;
  }

  &__score {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__stem {
    margin-bottom: $rk-space-l;
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
}

.exam-option {
  padding: $rk-space-s $rk-space-m;
  border: 1px solid $rk-border;
  border-radius: $rk-radius-m;

  &__key {
    display: inline-block;
    min-width: 1.2em;
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__content {
    color: $rk-text-1;
  }
}
</style>
