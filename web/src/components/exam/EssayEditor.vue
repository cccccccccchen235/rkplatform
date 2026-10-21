<script setup lang="ts">
// 论文 / 主观题写作框（07 §7.6 的 `EssayEditor`）。
//
// 它承担三件事：① 字数统计（去掉空白后计，与"不少于 N 字"的提示联动）；
// ② localStorage 草稿（防刷新/崩溃丢内容，**不参与**服务端作答保存）；
// ③ Markdown 预览与纯文本导出。
//
// 草稿键是 `essay:{sessionId}:{itemId}`（`storage` 统一加 `rk:` 前缀后即 07 §7.6 的
// `rk:essay:{sessionId}:{itemId}`）。恢复规则：服务端 `userAnswer` 为空且本地草稿非空时，
// 弹一次确认——**不静默覆盖**，否则用户会以为"我明明没写"。

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import { storage } from '@/utils/storage'

const props = withDefaults(
  defineProps<{
    modelValue: string
    itemId: string
    sessionId?: string
    remainingLabel?: string
    readonly?: boolean
  }>(),
  { sessionId: '', remainingLabel: '', readonly: false },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'draft-saved': [savedAt: number]
}>()

const preview = ref(false)

const draftKey = computed(() => `essay:${props.sessionId}:${props.itemId}`)

/** 去空白后的字数：论文要求的是"实际写下的字"，空行与缩进不该凑数。 */
const wordCount = computed(() => (props.modelValue ?? '').replace(/\s/g, '').length)

function onInput(value: string): void {
  emit('update:modelValue', value)
}

let draftTimer: ReturnType<typeof setTimeout> | null = null

function saveDraft(): void {
  if (props.readonly || !props.sessionId) return
  storage.set(draftKey.value, { content: props.modelValue, savedAt: Date.now() })
  emit('draft-saved', Date.now())
}

function scheduleDraftSave(): void {
  if (props.readonly || !props.sessionId) return
  if (draftTimer) clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    draftTimer = null
    saveDraft()
  }, 2000)
}

watch(() => props.modelValue, scheduleDraftSave)

function exportText(): void {
  const blob = new Blob([props.modelValue ?? ''], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `论文-${props.itemId}.txt`
  anchor.click()
  URL.revokeObjectURL(url)
}

function handleBeforeUnload(): void {
  saveDraft()
}

onMounted(async () => {
  if (props.readonly || !props.sessionId) return
  window.addEventListener('beforeunload', handleBeforeUnload)

  const draft = storage.get<{ content?: string; savedAt?: number }>(draftKey.value)
  const draftContent = draft?.content ?? ''
  // 服务端已有作答时不提示恢复：以服务端为准，本地草稿只做"没提交过"的兜底
  if (draftContent && !(props.modelValue ?? '')) {
    try {
      await ElMessageBox.confirm('检测到未提交的草稿，是否恢复？', '恢复草稿', {
        confirmButtonText: '恢复',
        cancelButtonText: '丢弃',
        type: 'info',
      })
      emit('update:modelValue', draftContent)
    } catch {
      storage.remove(draftKey.value)
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (draftTimer) {
    clearTimeout(draftTimer)
    draftTimer = null
  }
  saveDraft()
})
</script>

<template>
  <section class="essay-editor">
    <header class="essay-editor__head">
      <span class="essay-editor__count">已写 {{ wordCount }} 字</span>
      <span v-if="remainingLabel" class="essay-editor__timer">剩余 {{ remainingLabel }}</span>
      <span class="essay-editor__spacer" />
      <el-button text type="primary" @click="preview = !preview">
        {{ preview ? '继续编辑' : '预览' }}
      </el-button>
      <el-button text @click="exportText">导出为文本</el-button>
    </header>

    <MarkdownRenderer v-if="preview" class="essay-editor__preview" :content="modelValue || '*（还没有内容）*'" />
    <el-input
      v-else
      :model-value="modelValue"
      type="textarea"
      :rows="16"
      :disabled="readonly"
      resize="vertical"
      placeholder="在此作答（支持 Markdown）。内容每 2 秒自动存为本地草稿。"
      @update:model-value="onInput"
      @blur="saveDraft"
    />
  </section>
</template>

<style scoped lang="scss">
.essay-editor {
  &__head {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    margin-bottom: $rk-space-s;
    font-size: $rk-font-xs;
    color: $rk-text-3;
  }

  &__count,
  &__timer {
    font-variant-numeric: tabular-nums;
  }

  &__spacer {
    flex: 1;
  }

  &__preview {
    min-height: 200px;
    padding: $rk-space-m;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-m;
  }
}
</style>
