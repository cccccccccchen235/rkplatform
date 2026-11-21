<script setup lang="ts">
// 错题本（07 §7.5.1）。路由 `/wrong-book`，考生端布局。
//
// 三档状态用 `el-tabs` 切换，每次只发一个 `status`（互斥筛选，不是层级）：
// 「在册」是还要重做的，「已掌握」是连续答对后自动退出的，「已移出」是用户手动
// 移出后仍可恢复的。同一个疑问（"这道题现在还错吗"）在三个 tab 里是三种回答。
//
// `subjectId` 必填（05 §5.7 第 26 条）：没选科目时**不发请求**，页面直接空态，
// 而不是让服务端回一个 400 再翻译成空态。

import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchWrongQuestions, removeWrongQuestion, restoreWrongQuestion } from '@/api/wrong-book'
import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'
import type { WrongQuestionStatus, WrongQuestionVO } from '@/types/api'
import { WRONG_QUESTION_STATUS_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const subjectStore = useSubjectStore()
const user = useUserStore()

// 允许用 `?subjectId=` 直达（练习页的「错题重做」入口就带它）；没有时回落到
// 用户当前备考科目，再回落到列表第一个科目。少了这一条，本页只能被"点进来"，
// 不能被"链接进来"——而链接正是从练习页跳到错题本的自然方式。
const subjectId = ref(
  typeof route.query.subjectId === 'string' && route.query.subjectId !== ''
    ? route.query.subjectId
    : (user.currentSubjectId ?? ''),
)
const status = ref<WrongQuestionStatus>('ACTIVE')

const list = ref<WrongQuestionVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const loading = ref(true)
const loadError = ref('')

/** 正在执行行内操作的题目 ID，用于禁用那一行的按钮、防重复点击。 */
const acting = ref<string | null>(null)

const TAB_STATUSES: WrongQuestionStatus[] = ['ACTIVE', 'MASTERED', 'REMOVED']

/** 每个 tab 的空态文案不同："没有错题"与"没有已移出的记录"是两件事。 */
const EMPTY_TEXT: Record<WrongQuestionStatus, string> = {
  ACTIVE: '错题本里还没有题目，练习里答错的题会自动收进来。',
  MASTERED: '还没有已掌握的错题。',
  REMOVED: '没有已移出的错题。',
}

async function load(): Promise<void> {
  if (!subjectId.value) {
    list.value = []
    total.value = 0
    loading.value = false
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    // silent：失败由下面那块 alert 讲，不另弹 toast
    const result = await fetchWrongQuestions(
      {
        subjectId: subjectId.value,
        status: status.value,
        sortBy: 'lastAnsweredAt',
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      },
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
  // 顶栏已经预取过科目，这里再取一次会命中 5 分钟缓存（不会多打接口）
  try {
    await subjectStore.fetchGrouped({}, false, { silent: true })
  } catch {
    // 科目取不到不阻断本页：顶栏那块自己会报，这里最多是下拉为空
  }
  if (!subjectId.value) subjectId.value = subjectStore.flatList[0]?.id ?? ''
  await load()
})

/** 切换科目/状态/每页条数时回到第一页再拉，避免停在越界页码上。 */
function resetAndLoad(): void {
  pageNum.value = 1
  void load()
}

async function handleRemove(item: WrongQuestionVO): Promise<void> {
  acting.value = item.questionId
  try {
    await removeWrongQuestion(item.questionId)
    ElMessage.success('已移出错题本')
    // 移出后当前页可能空一格：重新拉当前页即可（数据量小，不做逐行占位）
    await load()
  } catch {
    // 失败提示由拦截器给出（1003 表示本就不在本子里）
  } finally {
    acting.value = null
  }
}

async function handleRestore(item: WrongQuestionVO): Promise<void> {
  acting.value = item.questionId
  try {
    await restoreWrongQuestion(item.questionId)
    ElMessage.success('已恢复到错题本')
    await load()
  } catch {
    // 同上
  } finally {
    acting.value = null
  }
}

function goRedo(): void {
  void router.push({ name: 'practice', params: { mode: 'WRONG_REDO' }, query: { subjectId: subjectId.value } })
}
</script>

<template>
  <div class="wrong-book rk-container">
    <header class="wrong-book__header">
      <h1 class="rk-page-title">错题本</h1>
      <p class="wrong-book__lead">
        答错的题会自动收进这里。连续答对后转入「已掌握」，手动移出的进「已移出」——两者都还能恢复。
      </p>
    </header>

    <div class="wrong-book__filters">
      <el-select v-model="subjectId" class="wrong-book__subject" placeholder="选择科目" @change="resetAndLoad">
        <el-option v-for="item in subjectStore.flatList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </div>

    <el-alert
      v-if="loadError"
      class="wrong-book__alert"
      type="error"
      :title="`错题本加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-tabs v-model="status" class="wrong-book__tabs" @tab-change="resetAndLoad">
      <el-tab-pane
        v-for="tab in TAB_STATUSES"
        :key="tab"
        :label="WRONG_QUESTION_STATUS_LABEL[tab]"
        :name="tab"
      />
    </el-tabs>

    <div class="wrong-book__toolbar">
      <span class="wrong-book__count">共 {{ total }} 题</span>
      <el-button class="wrong-book__redo" type="primary" :disabled="!subjectId" @click="goRedo">去重做</el-button>
    </div>

    <div v-if="loading" class="wrong-book__skeleton">
      <el-skeleton :rows="6" animated />
    </div>

    <el-empty v-else-if="!subjectId" description="先选择一个科目" />

    <el-empty v-else-if="list.length === 0" :description="EMPTY_TEXT[status]" />

    <ul v-else class="wrong-list">
      <li v-for="item in list" :key="item.questionId" class="wrong-item">
        <div class="wrong-item__main">
          <p class="wrong-item__stem">{{ item.stemSummary }}</p>
          <p v-if="item.outlinePath.length" class="wrong-item__path">{{ item.outlinePath.join(' / ') }}</p>
        </div>

        <dl class="wrong-item__stats">
          <div class="wrong-stat">
            <dt>错次</dt>
            <dd>{{ item.wrongCount }}</dd>
          </div>
          <div class="wrong-stat">
            <dt>对次</dt>
            <dd>{{ item.correctCount }}</dd>
          </div>
          <div class="wrong-stat">
            <dt>连对</dt>
            <dd>{{ item.consecutiveCorrect }}</dd>
          </div>
          <div class="wrong-stat">
            <dt>最近作答</dt>
            <dd>{{ item.lastAnsweredAt ?? '—' }}</dd>
          </div>
        </dl>

        <div class="wrong-item__actions">
          <el-button text type="primary" :disabled="!subjectId" @click="goRedo">去重做</el-button>
          <el-button
            v-if="status === 'REMOVED'"
            text
            :loading="acting === item.questionId"
            @click="handleRestore(item)"
          >
            恢复
          </el-button>
          <el-button v-else text :loading="acting === item.questionId" @click="handleRemove(item)">移出</el-button>
        </div>
      </li>
    </ul>

    <div v-if="!loading && total > pageSize" class="wrong-book__pagination">
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
  </div>
</template>

<style scoped lang="scss">
.wrong-book {
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

  &__tabs {
    margin-bottom: $rk-space-s;
  }

  &__toolbar {
    display: flex;
    align-items: center;
    gap: $rk-space-m;
    margin-bottom: $rk-space-l;
  }

  &__count {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__redo {
    margin-left: auto;
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

.wrong-list {
  display: grid;
  gap: $rk-space-m;
}

.wrong-item {
  display: flex;
  align-items: center;
  gap: $rk-space-l;
  padding: $rk-space-m $rk-space-l;
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;
  flex-wrap: wrap;
  transition:
    border-color 160ms ease-out,
    box-shadow $rk-soft-duration $rk-soft-ease;

  &:hover {
    border-color: rgba($rk-primary, 0.5);
    box-shadow: $rk-soft-shadow-hover;
  }

  &__main {
    flex: 1;
    min-width: 0;
  }

  &__stem {
    color: $rk-text-1;
    line-height: $rk-line-height;
    // 摘要是纯文本（05 §5.7 第 26 条给的是 stemSummary），两行封顶
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__path {
    margin-top: $rk-space-xs;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }

  &__stats {
    display: flex;
    gap: $rk-space-l;
    flex-shrink: 0;
    font-size: $rk-font-xs;
  }

  &__actions {
    display: flex;
    gap: $rk-space-xs;
    flex-shrink: 0;
  }
}

.wrong-stat {
  text-align: right;

  dt {
    color: $rk-text-3;
  }

  dd {
    margin-top: 2px;
    color: $rk-text-1;
    font-variant-numeric: tabular-nums;
  }
}

@media (max-width: 760px) {
  .wrong-item__stats {
    gap: $rk-space-m;
    flex-wrap: wrap;
  }
  .wrong-item__actions {
    width: 100%;
  }
}
</style>