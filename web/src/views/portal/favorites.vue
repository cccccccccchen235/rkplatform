<script setup lang="ts">
// 收藏（07 §7.5.1）。路由 `/favorites`，考生端布局。
//
// 列表用 `QuestionCard` 的 `mode='review'`：只读题目，不传 `judged`，所以不展示答案。
//
// 取消收藏是**乐观移除**（07 §7.5.1）：先从列表里去掉，再调 `DELETE`；失败回滚。
// 移除后页面顶部留一条「已取消收藏」条，提供「撤销」——撤销即重新 `POST /favorites`。
// 用常驻一条而不是弹 toast：这个撤销窗口有 8 秒，比 toast 的两秒长得多，
// 而"点了取消想反悔"恰恰是几秒之后才发生的动作。

import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import QuestionCard from '@/components/question/QuestionCard.vue'
import QuestionFeedbackDialog from '@/components/question/QuestionFeedbackDialog.vue'
import { addFavorite, fetchFavorites, removeFavorite } from '@/api/favorite'
import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'
import type { QuestionVO } from '@/types/api'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const subjectStore = useSubjectStore()
const user = useUserStore()

// 同错题本：允许 `?subjectId=` 直达，缺省才回落到用户的当前备考科目。
const subjectId = ref(
  typeof route.query.subjectId === 'string' && route.query.subjectId !== ''
    ? route.query.subjectId
    : (user.currentSubjectId ?? ''),
)

const list = ref<QuestionVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const loading = ref(true)
const loadError = ref('')

/** 最近一次被移除的收藏及其原位置，用于「撤销」。 */
const lastRemoved = ref<{ question: QuestionVO; index: number } | null>(null)

/** 反馈对话框的当前题目 ID 与显隐。 */
const feedbackQuestionId = ref('')
const feedbackVisible = ref(false)

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  lastRemoved.value = null
  try {
    // silent：失败由下面那块 alert 讲，不另弹 toast
    const result = await fetchFavorites(
      { subjectId: subjectId.value || undefined, pageNum: pageNum.value, pageSize: pageSize.value },
      { silent: true },
    )
    list.value = result.list ?? []
    total.value = result.total ?? 0
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await subjectStore.fetchGrouped({}, false, { silent: true })
  } catch {
    // 科目取不到不阻断本页
  }
  if (!subjectId.value) subjectId.value = subjectStore.flatList[0]?.id ?? ''
  await load()
})

function resetAndLoad(): void {
  pageNum.value = 1
  void load()
}

// ---------------- 取消收藏（乐观 + 撤销） ----------------

let undoTimer: ReturnType<typeof setTimeout> | null = null

function scheduleUndoClear(): void {
  if (undoTimer) clearTimeout(undoTimer)
  undoTimer = setTimeout(() => {
    lastRemoved.value = null
    undoTimer = null
  }, 8000)
}

async function handleCancelFavorite(index: number): Promise<void> {
  const question = list.value[index]
  if (!question) return

  const snapshot = { question, index }
  // 乐观移除
  list.value.splice(index, 1)
  total.value = Math.max(0, total.value - 1)
  lastRemoved.value = snapshot
  scheduleUndoClear()

  try {
    await removeFavorite(question.id)
  } catch {
    // 回滚：插回原位置，并撤掉撤销条（这次移除根本没成功）
    list.value.splice(Math.min(index, list.value.length), 0, question)
    total.value += 1
    if (lastRemoved.value === snapshot) lastRemoved.value = null
  }
}

async function undoRemove(): Promise<void> {
  const snapshot = lastRemoved.value
  if (!snapshot) return
  lastRemoved.value = null
  if (undoTimer) {
    clearTimeout(undoTimer)
    undoTimer = null
  }

  try {
    await addFavorite(snapshot.question.id)
    list.value.splice(Math.min(snapshot.index, list.value.length), 0, snapshot.question)
    total.value += 1
    ElMessage.success('已恢复收藏')
  } catch {
    // 恢复失败：题目仍在服务端收藏里（取消那次已成功），列表下次刷新会带上它
    ElMessage.error('恢复收藏失败，请刷新后重试')
  }
}

function openFeedback(question: QuestionVO): void {
  feedbackQuestionId.value = question.id
  feedbackVisible.value = true
}

onBeforeUnmount(() => {
  if (undoTimer) clearTimeout(undoTimer)
})
</script>

<template>
  <div class="favorites rk-container">
    <header class="favorites__header">
      <h1 class="rk-page-title">收藏</h1>
      <p class="favorites__lead">练习时点「☆ 收藏」的题会出现在这里，随时回来重看题干与解析。</p>
    </header>

    <div class="favorites__filters">
      <el-select v-model="subjectId" class="favorites__subject" placeholder="选择科目" @change="resetAndLoad">
        <el-option v-for="item in subjectStore.flatList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </div>

    <el-alert
      v-if="loadError"
      class="favorites__alert"
      type="error"
      :title="`收藏加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-alert v-if="lastRemoved" class="favorites__undo" type="info" :closable="false" show-icon title="已取消收藏">
      <template #default>
        <el-button text type="primary" @click="undoRemove">撤销</el-button>
      </template>
    </el-alert>

    <div v-if="loading" class="favorites__skeleton">
      <el-skeleton :rows="6" animated />
    </div>

    <el-empty v-else-if="list.length === 0" description="还没有收藏的题目。在练习页点「☆ 收藏」即可加入。" />

    <ul v-else class="favorite-list">
      <li v-for="(question, index) in list" :key="question.id">
        <QuestionCard
          :question="question"
          mode="review"
          @favorite-change="handleCancelFavorite(index)"
          @feedback="openFeedback(question)"
        />
      </li>
    </ul>

    <div v-if="!loading && total > pageSize" class="favorites__pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="resetAndLoad"
      />
    </div>

    <QuestionFeedbackDialog
      v-if="feedbackQuestionId"
      :question-id="feedbackQuestionId"
      v-model:visible="feedbackVisible"
    />
  </div>
</template>

<style scoped lang="scss">
.favorites {
  padding-top: $rk-space-l;
  padding-bottom: $rk-space-xl * 2;

  &__header {
    margin-bottom: $rk-space-l;
  }

  &__lead {
    color: $rk-text-2;
    max-width: 760px;
  }

  &__filters {
    margin-bottom: $rk-space-l;
  }

  &__subject {
    width: 240px;
  }

  &__alert {
    margin-bottom: $rk-space-l;
  }

  &__undo {
    margin-bottom: $rk-space-l;
  }

  &__skeleton {
    padding-top: $rk-space-l;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: $rk-space-l;
  }
}

.favorite-list {
  display: grid;
  gap: $rk-space-l;
}
</style>