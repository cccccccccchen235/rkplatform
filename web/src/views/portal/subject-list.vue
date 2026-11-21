<script setup lang="ts">
// 科目列表（07 §7.5.1）。本轮唯一"跑真数据"的页面：数据来自 GET /api/v1/subjects。
//
// 页面呈现的是**数据现状**而不是宣传口径：大纲是骨架就写骨架，题库只有样例题就写样例题，
// 没有配置卷面模板就没有模拟考入口。这些字段本来就是为这件事存在的（05 §5.4 的
// outlineStatus / contentStatus / paperSummary），把它们如实显示出来，
// 比统一渲染成"敬请期待"更有用——用户能据此决定现在要不要选这个科目。

import { computed, onMounted, ref } from 'vue'

import { useSubjectStore } from '@/stores/subject'
import type { SubjectVO } from '@/types/api'
import {
  CONTENT_STATUS_LABEL,
  EXAM_PERIOD_LABEL,
  OUTLINE_STATUS_LABEL,
  SUBJECT_LEVEL_LABEL,
  SUBJECT_LEVEL_ORDER,
} from '@/types/labels'
import type { RequestError } from '@/utils/request'

const subjectStore = useSubjectStore()

const loadError = ref('')

/**
 * 首屏与"重试"共用同一条路径。
 *
 * 之前的重试按钮直接调 `fetchGrouped({}, true)`：失败了不更新 `loadError`，
 * 于是面板上留着**上一次**的错误文案，同时那次 rejection 没人接——控制台多一条
 * 未处理的 Promise 拒绝。走同一个 `load()` 两个问题一起消失。
 */
async function load(): Promise<void> {
  loadError.value = ''
  try {
    // silent：失败由下面那块 alert 独家呈现（它带重试按钮）。
    // 不传的话会先弹一条两秒就消失的 toast，再出现一块内容相同的面板。
    await subjectStore.fetchGrouped({}, false, { silent: true })
  } catch (error) {
    // 空态与失败是两件事：接口挂了要能重试，接口正常但没有数据显示"暂无科目"。
    // 混在一起会让"服务没起来"看起来像"数据是空的"。
    loadError.value = (error as RequestError).message
  }
}

onMounted(load)

/** 三个级别的分组，顺序固定为 初→中→高（不依赖接口返回的键序）。 */
const levelGroups = computed(() =>
  SUBJECT_LEVEL_ORDER.map((level) => ({
    level,
    label: SUBJECT_LEVEL_LABEL[level],
    subjects: subjectStore.byLevel(level),
  })).filter((group) => group.subjects.length > 0),
)

const total = computed(() => subjectStore.flatList.length)

/**
 * 这门课能不能发起模拟考 = 它有没有启用中的卷面模板。
 *
 * 判断依据是 `paperTotalScore != null`（docs/18 的 F3：后端给列表接口加了这个字段）。
 * 不能用 `paperSummary != null`——列表接口对那个字段一律返回 null，
 * 照那样写会得出"没有任何科目可以考"，一个听起来像事实的错误结论。
 */
function canStartExam(subject: SubjectVO): boolean {
  return subject.paperTotalScore != null
}
</script>

<template>
  <div class="subject-list rk-container">
    <header class="subject-list__header">
      <h1 class="rk-page-title">科目</h1>
      <p class="subject-list__lead">
        软考共 {{ total }} 个科目。每个科目下面写的是它在平台上的真实状态：开考时间、大纲完整度、
        题库规模——按这些决定现在练哪个。
      </p>
    </header>

    <el-alert
      v-if="loadError"
      class="subject-list__alert"
      type="error"
      :title="`科目列表加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <!-- 骨架屏：三级分组卡片的首屏形状是已知的，用形状占位比转圈更能减少跳动 -->
    <div v-if="subjectStore.loading && !subjectStore.loaded" class="subject-list__skeleton">
      <el-skeleton v-for="n in 3" :key="n" :rows="4" animated />
    </div>

    <el-empty
      v-else-if="!loadError && subjectStore.isEmpty"
      description="暂无科目数据"
    />

    <template v-else>
      <section v-for="group in levelGroups" :key="group.level" class="subject-group">
        <h2 class="subject-group__title">
          {{ group.label }}
          <span class="subject-group__count">{{ group.subjects.length }}</span>
        </h2>

        <ul class="subject-grid">
          <!--
            整张卡片是一个链接而不是卡片里的一个「详情」小按钮：卡片上没有任何
            第二个可点的地方，把命中区做成整块既省一次瞄准，也省掉一行按钮。
            代价是卡片自身不再能放别的交互元素——目前没有。
          -->
          <li v-for="subject in group.subjects" :key="subject.id">
            <router-link class="subject-card" :to="{ name: 'subject-detail', params: { id: subject.id } }">
            <div class="subject-card__head">
              <h3 class="subject-card__name">{{ subject.name }}</h3>
              <span class="subject-card__code">{{ subject.code }}</span>
            </div>

            <p v-if="subject.intro" class="subject-card__intro">{{ subject.intro }}</p>
            <p v-else class="subject-card__intro subject-card__intro--empty">暂无简介</p>

            <dl class="subject-card__facts">
              <div class="subject-fact">
                <dt>开考</dt>
                <dd>{{ EXAM_PERIOD_LABEL[subject.examPeriod] }}</dd>
              </div>
              <div class="subject-fact">
                <dt>大纲</dt>
                <dd>{{ OUTLINE_STATUS_LABEL[subject.outlineStatus] }}</dd>
              </div>
              <div class="subject-fact">
                <dt>题库</dt>
                <dd>
                  {{ CONTENT_STATUS_LABEL[subject.contentStatus] }}
                  <span class="subject-fact__sub">{{ subject.questionCount }} 题</span>
                </dd>
              </div>
            </dl>

            <!--
              卷面满分与"能不能考"。
              判据是列表接口返回的 paperTotalScore，不是 paperSummary——后者按设计
              在列表里恒为 null（05 §5.4 把"含 paperSummary"列为详情接口的特性，
              避免为 20 个科目各带一棵场次树），拿它判断资格会得出反的结论。
              分节数与时长仍然只有详情接口有，所以这里只给满分。
            -->
            <div class="subject-card__exam">
              <template v-if="canStartExam(subject)">
                <span class="subject-card__exam-ready">卷面 {{ subject.paperTotalScore }} 分</span>
                <span class="subject-card__exam-meta">已配置卷面，可发起模拟考</span>
              </template>
              <span v-else class="subject-card__exam-none">尚未配置卷面模板，暂不能模拟考</span>
            </div>

              <span class="subject-card__go">查看科目详情 →</span>
            </router-link>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
.subject-list {
  padding-top: $rk-space-xl;
  padding-bottom: $rk-space-xl * 2;

  &__header {
    margin-bottom: $rk-space-xl;
  }

  &__lead {
    color: $rk-text-2;
    max-width: 720px;
  }

  &__alert {
    margin-bottom: $rk-space-l;
  }

  &__skeleton {
    display: grid;
    gap: $rk-space-l;
  }
}

.subject-group {
  margin-bottom: $rk-space-xl * 1.5;

  &__title {
    display: flex;
    align-items: baseline;
    gap: $rk-space-s;
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
    margin-bottom: $rk-space-l;
    padding-bottom: $rk-space-s;
    border-bottom: 1px solid $rk-border;
  }

  &__count {
    font-size: $rk-font-xs;
    font-weight: 400;
    color: $rk-text-3;
  }
}

.subject-grid {
  display: grid;
  // 1280 宽度下三列：卡片内容（科目名 + 三条事实 + 卷面行）在 360px 左右刚好不换行
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: $rk-space-l;

  // li 只是网格单元，卡片本体是里面的 <a>——这样整张卡都是链接的命中区，
  // 又不用把 <a> 直接塞进 <ul>（那是不合法的子元素）。
  > li {
    display: flex;
  }

  // 响应式（docs/36）：宽屏三列 → 中屏两列 → 窄屏单列。
  @media (max-width: 1080px) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  @media (max-width: 700px) {
    grid-template-columns: 1fr;
  }
}

.subject-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $rk-space-m;
  padding: $rk-space-xl;
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;
  // 链接默认的蓝字下划线会把整张卡片变成"一大片链接"，这里只保留它可点的事实：
  // 颜色继承，下划线去掉，可点性由 hover 时的边框与末行文案表达。
  color: inherit;
  text-decoration: none;
  // 边框色与末行文案的变化走同一条 160ms 指数缓出，和设计系统里书脊的
  // spine-border 动效同一口径；鼠标滑过时不再"啪"地跳色。
  transition:
    border-color 160ms ease-out,
    box-shadow $rk-soft-duration $rk-soft-ease,
    transform $rk-soft-duration $rk-soft-ease,
    color 160ms ease-out;

  &:hover {
    border-color: rgba($rk-primary, 0.5);
    transform: translateY(-3px);
    box-shadow: $rk-soft-shadow-hover;

    .subject-card__go {
      color: $rk-primary;
    }
  }

  &__go {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    transition: color 160ms ease-out;
  }

  &__head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: $rk-space-s;
  }

  &__name {
    font-size: $rk-font-m;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__code {
    font-size: $rk-font-xs;
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }

  &__intro {
    color: $rk-text-2;
    // 三行封顶：简介长短不一时卡片高度会参差，网格行高被拉齐后仍会留下大片空白
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: calc(#{$rk-font-s} * #{$rk-line-height} * 3);

    &--empty {
      color: $rk-text-3;
      // "暂无简介"是一行事实，不是三行 placebo。留三行的高度会在
      // 标题与事实表之间撑出一块空白，让人以为简介加载失败了。
      // 卡片整体仍由网格同行等高，卷面行照旧 margin-top:auto 沉底。
      min-height: auto;
    }
  }

  &__facts {
    display: grid;
    gap: $rk-space-xs;
    font-size: $rk-font-xs;
  }

  &__exam {
    margin-top: auto;
    padding-top: $rk-space-m;
    border-top: 1px dashed $rk-border;
    display: flex;
    flex-direction: column;
    gap: $rk-space-xs;
    font-size: $rk-font-xs;
  }

  &__exam-ready {
    // “卷面 225 分”是 12px 小字，落在卡片白底上。主色自 F14 起是 #2a72c2（4.90:1），
    // 这一处直接用主色即达标，不需要另设一个"文字档"。
    color: $rk-primary;
  }

  &__exam-meta {
    color: $rk-text-3;
  }

  &__exam-none {
    color: $rk-text-3;
  }
}

.subject-fact {
  display: flex;
  gap: $rk-space-s;

  dt {
    color: $rk-text-3;
    flex-shrink: 0;
    width: 2.5em;
  }

  dd {
    color: $rk-text-2;
  }

  &__sub {
    color: $rk-text-3;
    margin-left: $rk-space-xs;
  }
}
</style>
