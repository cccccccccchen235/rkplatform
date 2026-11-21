<script setup lang="ts">
// 答题卡（07 §7.6 的 `AnswerSheet`）。
//
// 一次只渲染**当前场次**的分节（页面传进来的 `sections` 已经过滤过），按 `sectionNo` 分组；
// 每格的编号是"本节内的题序"，不是全卷题序——考场上找题看的就是"本节第几题"。
//
// 三种格状态用**形状 + 文字**共同表达，不单靠颜色：当前题是实心描边，已答打勾，
// 未答留白。`NOT_SUPPORTED` / 未开始的分节整组禁用（不可点击），对应 07 §7.12 的验收项。

import type { ExamItemVO, ExamSectionVO } from '@/types/api'

const props = defineProps<{
  items: ExamItemVO[]
  sections: ExamSectionVO[]
  answers: Record<string, string>
  currentItemId: string
  /** 当前正在显示的分节；点击其它可进入分节的题会由页面切换过去。 */
  activeSectionNo: number
  /** 允许进入的分节号（`IN_PROGRESS` / `FINISHED`，且非 `NOT_SUPPORTED`）。 */
  enterableSectionNos: number[]
}>()

const emit = defineEmits<{ jump: [itemId: string] }>()

function itemsOf(sectionNo: number): ExamItemVO[] {
  return props.items.filter((item) => item.sectionNo === sectionNo)
}

function isAnswered(itemId: string): boolean {
  const value = props.answers[itemId]
  return value != null && value !== ''
}

function answeredCountOf(sectionNo: number): number {
  return itemsOf(sectionNo).filter((item) => isAnswered(item.itemId)).length
}

function isEnterable(section: ExamSectionVO): boolean {
  return section.supportStatus === 'SUPPORTED' && props.enterableSectionNos.includes(section.sectionNo)
}

function handleClick(item: ExamItemVO, section: ExamSectionVO): void {
  if (!isEnterable(section)) return
  emit('jump', item.itemId)
}
</script>

<template>
  <aside class="answer-sheet">
    <h2 class="answer-sheet__title">答题卡</h2>

    <p class="answer-sheet__legend">
      <span class="answer-sheet__legend-item"><span class="answer-sheet__dot answer-sheet__dot--done" />已答</span>
      <span class="answer-sheet__legend-item"><span class="answer-sheet__dot" />未答</span>
      <span class="answer-sheet__legend-item"><span class="answer-sheet__dot answer-sheet__dot--current" />当前</span>
    </p>

    <section v-for="section in sections" :key="section.sectionNo" class="answer-sheet__group">
      <header class="answer-sheet__group-head">
        <span class="answer-sheet__group-name">{{ section.sectionName }}</span>
        <span class="answer-sheet__group-count">
          {{ answeredCountOf(section.sectionNo) }} / {{ section.questionCount }}
        </span>
      </header>

      <p v-if="itemsOf(section.sectionNo).length === 0" class="answer-sheet__empty">本节暂无题目</p>

      <div v-else class="answer-sheet__grid">
        <button
          v-for="(item, index) in itemsOf(section.sectionNo)"
          :key="item.itemId"
          type="button"
          class="answer-sheet__cell"
          :class="{
            'is-answered': isAnswered(item.itemId),
            'is-current': item.itemId === currentItemId,
            'is-active-section': section.sectionNo === activeSectionNo,
            'is-disabled': !isEnterable(section),
          }"
          :disabled="!isEnterable(section)"
          @click="handleClick(item, section)"
        >
          {{ index + 1 }}
        </button>
      </div>
    </section>
  </aside>
</template>

<style scoped lang="scss">
.answer-sheet {
  width: $rk-exam-aside-width;
  flex: 0 0 $rk-exam-aside-width;
  align-self: flex-start;
  padding: $rk-space-l;
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;

  // 窄屏时答题卡不再固定 280px，铺满下方。
  @media (max-width: 900px) {
    width: 100%;
    flex-basis: auto;
  }

  &__title {
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
    margin-bottom: $rk-space-s;
  }

  &__legend {
    display: flex;
    gap: $rk-space-m;
    margin-bottom: $rk-space-m;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__legend-item {
    display: inline-flex;
    align-items: center;
    gap: $rk-space-xs;
  }

  &__dot {
    width: 10px;
    height: 10px;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-s;
    background-color: $rk-bg-card;

    &--done {
      background-color: $rk-primary;
      border-color: $rk-primary;
    }

    &--current {
      border-color: $rk-text-1;
      border-width: 2px;
    }
  }

  &__group + &__group {
    margin-top: $rk-space-l;
  }

  &__group-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: $rk-space-s;
    padding-bottom: $rk-space-xs;
    margin-bottom: $rk-space-s;
    border-bottom: 1px solid $rk-soft-border;
  }

  &__group-name {
    color: $rk-text-1;
    font-size: $rk-font-s;
  }

  &__group-count {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__empty {
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: $rk-space-s;
  }

  &__cell {
    height: 28px;
    padding: 0;
    font: inherit;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
    color: $rk-text-2;
    background-color: $rk-bg-card;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-radius-m;
    cursor: pointer;

    &.is-answered {
      color: $rk-bg-card;
      background-color: $rk-primary;
      border-color: $rk-primary;
    }

    &.is-current {
      border-color: $rk-text-1;
      border-width: 2px;
      font-weight: 600;
    }

    &:disabled {
      color: $rk-text-3;
      background-color: $rk-bg-page;
      cursor: not-allowed;
    }

    &:not(:disabled):hover {
      border-color: $rk-primary;
    }
  }
}
</style>
