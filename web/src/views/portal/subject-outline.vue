<script setup lang="ts">
// 科目大纲（07 §7.5.1）。与详情页分成两页，而不是把树塞进详情页底部：
//
// 详情页回答"这门课考什么"（卷面结构、题型），大纲页回答"这门课学什么"（章节）。
// 树可能有上百个节点，两件事叠在一页，卷面结构会被推到首屏之外——
// 而卷面结构恰恰是用户决定"要不要现在就开始学"的依据。
//
// 数据来自公开接口 10（`GET /subjects/{id}/outline`），只返回 `ENABLED` 节点。
//
// 一个必须说清的口径：**父节点的题量含其子章节**（04 §4.6：`question_count`
// 是"含子节点的已发布题量"的冗余列）。所以"某章 12 题"不等于"这一章自己挂了 12 道"，
// 页面必须把这句话写出来，否则用户会拿父子两级的数字去对账，永远对不上。

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { useSubjectStore } from '@/stores/subject'
import type { OutlineNodeVO } from '@/types/api'
import { OUTLINE_STATUS_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const subjectStore = useSubjectStore()

const loading = ref(true)
const loadError = ref('')

const subjectId = computed(() => String(route.params.id ?? ''))
const subject = computed(() => subjectStore.currentSubject)
const tree = computed(() => subjectStore.outlineTree)

/** 该科目是不是还没被灌过章节。空数组 = 还没有，不是"取不到"。 */
const isEmpty = computed(() => !loading.value && !loadError.value && tree.value.length === 0)

/** 章节总数（含各级）。 */
const nodeCount = computed(() => countNodes(tree.value))

/**
 * 覆盖题量 = **根节点题量之和**。
 *
 * 不是把所有节点的题量加起来：父节点那一列已经含了子节点（04 §4.6），
 * 全加起来会重复计数——一个"看起来像真的"的错数字。
 */
const questionCount = computed(() => tree.value.reduce((sum, node) => sum + node.questionCount, 0))

function countNodes(nodes: OutlineNodeVO[]): number {
  return nodes.reduce((sum, node) => sum + 1 + countNodes(node.children ?? []), 0)
}

onMounted(async () => {
  loading.value = true
  loadError.value = ''
  try {
    // 详情可能还没取过（直接输网址进来、或从别处跳来）。
    // 只有"同一个科目"才复用，否则会拿着上一门课的标题渲染这一页。
    // silent：失败统一由下面那块 alert + 重试按钮呈现，不另弹 toast。
    if (!subjectStore.currentSubject || subjectStore.currentSubject.id !== subjectId.value) {
      subjectStore.clearDetail()
      await subjectStore.fetchSubjectDetailById(subjectId.value, { silent: true })
    }
    await subjectStore.fetchOutline(subjectId.value, { silent: true })
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
})

async function reload(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    await subjectStore.fetchOutline(subjectId.value, { silent: true })
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="subject-outline rk-container">
    <router-link
      v-if="subject"
      :to="{ name: 'subject-detail', params: { id: subject.id } }"
      class="subject-outline__back"
    >
      ← 返回「{{ subject.name }}」
    </router-link>
    <router-link v-else :to="{ name: 'subjects' }" class="subject-outline__back">← 全部科目</router-link>

    <header class="subject-outline__header">
      <h1 class="rk-page-title">章节大纲</h1>
      <!-- 三段之间靠 flex 的 gap 分隔，不写全角空格：全角空格宽度随字体变，
           而且它把一个排版间距写进了文本内容里（复制这一行会连空格一起带走）。 -->
      <p v-if="subject" class="subject-outline__lead">
        <span>{{ subject.name }}</span>
        <span>{{ OUTLINE_STATUS_LABEL[subject.outlineStatus] }}</span>
        <span v-if="!loading && !loadError" class="subject-outline__tally">
          共 {{ nodeCount }} 个章节，覆盖 {{ questionCount }} 题
        </span>
      </p>
      <!--
        父节点题量的口径写在标题下而不是做成 tooltip：它会影响用户怎么读整棵树，
        藏进悬浮提示里等于默认没人需要知道。
      -->
      <p class="subject-outline__note">父章节的题量含其子章节，所以同级之间可以直接比较，不要与子级相加。</p>
    </header>

    <div v-if="loading" class="subject-outline__skeleton">
      <el-skeleton :rows="10" animated />
    </div>

    <el-alert
      v-else-if="loadError"
      type="error"
      :title="`章节大纲加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="reload">重试</el-button>
      </template>
    </el-alert>

    <!--
      「骨架」与「还没有」是两件事：SKELETON 是数据现状（大纲只录到一级），
      要用一句话说明它为什么会这么浅；空数组才是"这门课还没录章节"。
    -->
    <template v-else-if="!isEmpty">
      <el-alert
        v-if="subject?.outlineStatus === 'SKELETON'"
        class="subject-outline__alert"
        type="info"
        :closable="false"
        show-icon
        title="该科目目前是骨架大纲，只录入了一级章节，后续会补齐。"
      />

      <div class="outline-panel">
        <el-tree
          :data="tree"
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          default-expand-all
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <div class="outline-row">
              <span class="outline-row__name">{{ (data as OutlineNodeVO).name }}</span>
              <span class="outline-row__count">{{ (data as OutlineNodeVO).questionCount }} 题</span>
              <!-- 三级节点右侧留一个"叶"的标记，让"这层到头了"这件事不用靠缩进去猜 -->
              <span v-if="node.isLeaf" class="outline-row__leaf">末级</span>
            </div>
          </template>
        </el-tree>
      </div>
    </template>

    <el-empty v-else description="该科目还没有录入章节" />
  </div>
</template>

<style scoped lang="scss">
.subject-outline {
  padding-top: $rk-space-l;
  padding-bottom: $rk-space-xl * 2;

  &__back {
    display: inline-block;
    margin-bottom: $rk-space-m;
    color: $rk-text-2;
    font-size: $rk-font-xs;

    &:hover {
      color: $rk-primary;
    }
  }

  &__header {
    margin-bottom: $rk-space-l;
  }

  &__lead {
    display: flex;
    gap: $rk-space-s;
    color: $rk-text-2;
    margin-bottom: $rk-space-xs;

  }

  // 计数与前面两段的信息类型不同，用竖线隔开而不是只靠间距（也不用 :last-child：
  // 加载中时计数那一项不存在，选择器会落到状态那一段上）
  &__tally {
    padding-left: $rk-space-s;
    border-left: 1px solid $rk-border;
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__note {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    max-width: 760px;
  }

  &__skeleton {
    padding-top: $rk-space-l;
  }

  &__alert {
    margin-bottom: $rk-space-l;
  }
}

.outline-panel {
  padding: $rk-space-l;
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;
}

.outline-row {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: $rk-space-m;
  padding-right: $rk-space-s;

  &__name {
    color: $rk-text-1;
  }

  &__count {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    font-variant-numeric: tabular-nums;
  }

  &__leaf {
    padding: 0 $rk-space-xs;
    border: 1px solid $rk-soft-border;
    border-radius: $rk-radius-m;
    color: $rk-text-3;
    font-size: $rk-font-xs;
  }
}

:deep(.el-tree-node__content) {
  height: 36px;
  border-radius: $rk-radius-m;
}
</style>
