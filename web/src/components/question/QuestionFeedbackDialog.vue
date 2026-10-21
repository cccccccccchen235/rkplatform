<script setup lang="ts">
// 题目反馈对话框（07 §7.6，接口 05 §5.9 第 36 条）。
//
// 失败提示留在对话框内部（el-alert），不靠 toast：`1002`「24 小时内最多 3 次」
// 是一条需要用户读懂才会改行为的提示，两秒就消失的 toast 做不到这件事。

import { computed, ref } from 'vue'

import { submitQuestionFeedback } from '@/api/question'
import type { FeedbackType } from '@/types/api'
import { FEEDBACK_TYPE_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const props = defineProps<{
  questionId: string
  visible: boolean
}>()

const emit = defineEmits<{ 'update:visible': [value: boolean] }>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

/** 类型下拉的候选。键取自映射表，保证与后端的枚举取值域一致（多一个少一个都会编译报错）。 */
const typeOptions = (Object.keys(FEEDBACK_TYPE_LABEL) as FeedbackType[]).map((value) => ({
  value,
  label: FEEDBACK_TYPE_LABEL[value],
}))

const type = ref<FeedbackType>('TYPO')
const content = ref('')
const submitting = ref(false)
const errorMessage = ref('')

/** 5~1000 字，与 05 §5.9 第 36 条一致。按去首尾空白后的长度算。 */
const trimmedLength = computed(() => content.value.trim().length)

function reset(): void {
  type.value = 'TYPO'
  content.value = ''
  errorMessage.value = ''
  submitting.value = false
}

async function handleSubmit(): Promise<void> {
  if (trimmedLength.value < 5) {
    errorMessage.value = '反馈内容至少 5 个字'
    return
  }
  if (trimmedLength.value > 1000) {
    errorMessage.value = '反馈内容最多 1000 个字'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    // silent：失败原因留在本对话框的 alert 上，不另弹 toast
    await submitQuestionFeedback(props.questionId, { type: type.value, content: content.value.trim() }, { silent: true })
    ElMessage.success('反馈已提交，感谢你的帮助')
    dialogVisible.value = false
  } catch (error) {
    const err = error as RequestError
    errorMessage.value = err.code === 1002 ? '同一题目 24 小时内最多提交 3 次反馈' : err.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" title="题目反馈" width="480px" @closed="reset">
    <el-alert
      v-if="errorMessage"
      class="feedback__alert"
      type="error"
      :title="errorMessage"
      :closable="false"
      show-icon
    />

    <el-form label-position="top">
      <el-form-item label="问题类型">
        <el-select v-model="type" class="feedback__type">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="反馈内容">
        <el-input
          v-model="content"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="请描述题目存在的问题（至少 5 个字）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.feedback__alert {
  margin-bottom: $rk-space-l;
}

.feedback__type {
  width: 100%;
}
</style>