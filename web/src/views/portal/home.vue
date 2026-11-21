<script setup lang="ts">
// 考生端首页：一面书架（07 §7.5.1 的首页，结构由设计决定锁定为「书架书脊」）。
//
// 页面回答的问题是「这个站有什么」。它的回答方式是把 20 个科目**当成 20 本书立在架上**：
//
//   · 蓝框的脊 = 大纲完整（outlineStatus=FULL）。今天只有 3 门（软件设计师、系统架构设计师、
//     信息系统项目管理师）；其余是虚线框。**不用"淡一档"表达未印**——降低对比度会让那 17 门
//     变成"看不清"，而它们的信息同样需要读（见 docs/18 的 F14）。
//   · 书脚一行写编号与卷面满分；考期在取下来的抽屉里以文字给出。
//
// 2026-10-02 移除两处图形编码（实测它们不挣得位置）：
//   · 脊高 = 卷面满分 —— 每层内部高度本已相同，脊高实际只重复了"层级"（层级已由层标签说出），
//     全页 20 根里唯一由它产生的独立信息是 IPT 矮一截，而那本的书名会被压成三列错行。
//   · 顶端色带 = 考期 —— 6px 细带上的三种形状读不出来，又与"已印"的蓝框同色同位相撞。
// 现在书脊统一高度，满分与考期改由文字承载（事实不丢，只是不再用冗余的图形重复表达）。
//
// 刻意不做成「功能卡片网格」：那种版式把每门课渲染成同样大小、同样可点的方块，
// 于是「这门课现在到底有没有东西可练」这个唯一要紧的问题被抹平了。书架把这件事
// 摆在明面上——20 根脊里只有 3 根是蓝框，用户不用点进去才发现是空的。

import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useSubjectStore } from '@/stores/subject'
import type { SubjectLevel, SubjectVO } from '@/types/api'
import { EXAM_PERIOD_LABEL, OUTLINE_STATUS_LABEL, SUBJECT_LEVEL_LABEL, SUBJECT_LEVEL_ORDER } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const router = useRouter()
const subjectStore = useSubjectStore()

const loadError = ref('')

/** 当前被「取下来」的那本。null = 都插在架上。 */
const activeId = ref<string | null>(null)

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
    // silent：失败由下面那块 alert 独家呈现（含重试按钮）。不传就变成
    // "先弹一条两秒消失的 toast，再出现一块内容相同的面板"，用户以为出了两个错。
    await subjectStore.fetchGrouped({}, false, { silent: true })
  } catch (error) {
    // 失败与空态是两件事：接口挂了要能重试，接口正常但没有科目才显示空态。
    loadError.value = (error as RequestError).message
  }
}

onMounted(load)

/**
 * 三层书架，高级在上、初级在下。
 *
 * 用 SUBJECT_LEVEL_ORDER 反序而不是写死 ['SENIOR','MIDDLE','JUNIOR']：
 * 级别顺序的唯一来源是 labels.ts 里那份数组，写死第二份就会在将来分叉。
 */
const tiers = computed(() =>
  [...SUBJECT_LEVEL_ORDER]
    .reverse()
    .map((level: SubjectLevel) => ({
      level,
      label: SUBJECT_LEVEL_LABEL[level],
      subjects: subjectStore.byLevel(level),
    })),
)

const printedCount = computed(() => subjectStore.printedSubjects.length)
const total = computed(() => subjectStore.flatList.length)

const activeSubject = computed<SubjectVO | null>(
  () => subjectStore.flatList.find((subject) => subject.id === activeId.value) ?? null,
)

/** 取下来的那本书的抽屉，用来在需要时把它滚进视口。 */
const drawerEl = ref<HTMLElement | null>(null)

/** 把一本书取下来 / 插回去。再点同一本就是插回去。 */
function toggle(subject: SubjectVO): void {
  activeId.value = activeId.value === subject.id ? null : subject.id
  if (activeId.value === null) return

  // 抽屉长在书架下面，而书架本身就占满一屏（实测 1280×800 时抽屉落在折线以下）。
  // 不滚的话，点一本书的**全部可见后果**就是它抬起来 14px——用户看不到
  // 考期、大纲、卷面，会以为这个交互没做完。
  //
  // 用 block: 'nearest'：只有它真的不在视口里才滚，已经在视野内时页面纹丝不动。
  void nextTick(() => {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    drawerEl.value?.scrollIntoView({ block: 'nearest', behavior: reduceMotion ? 'auto' : 'smooth' })
  })
}

/**
 * 载入骨架的三层本数。
 *
 * 书脊现在是统一高度，骨架只按**每层本数**给个大概的横向占位。
 * 本数在响应到达前是未知的，所以只画一小排——到齐时会横向补满；
 * 不假装"完全不跳版"（真实是 5+11+4 根）。
 */
const SKELETON_TIERS = [4, 6, 4]

// ---------------- 键盘：像在架上找书一样移动 ----------------

const spineEls = new Map<string, HTMLButtonElement>()

function setSpineRef(id: string, el: unknown): void {
  if (el instanceof HTMLButtonElement) spineEls.set(id, el)
  else spineEls.delete(id)
}

/**
 * 书架的方向键导航。
 *
 * 这是渐进增强：每根书脊本身是可聚焦的 <button>，Tab 键一路走到底也能用；
 * 方向键只是让"在同一层里挪一格、换到上一层"不必按十几次 Tab。
 *
 * 不用 roving tabindex：那要求把 tabindex 全改成 -1 并手动维持，
 * 一旦哪次渲染没同步上，整排书脊会突然都不能 Tab 到。
 */
function onSpineKeydown(event: KeyboardEvent, tierIndex: number, index: number): void {
  const currentTier = tiers.value[tierIndex]
  if (!currentTier) return

  let target: { tier: number; index: number } | null = null

  switch (event.key) {
    case 'ArrowRight':
      if (index + 1 < currentTier.subjects.length) target = { tier: tierIndex, index: index + 1 }
      break
    case 'ArrowLeft':
      if (index > 0) target = { tier: tierIndex, index: index - 1 }
      break
    case 'ArrowDown': {
      const next = tiers.value[tierIndex + 1]
      if (next && next.subjects.length > 0) {
        target = { tier: tierIndex + 1, index: Math.min(index, next.subjects.length - 1) }
      }
      break
    }
    case 'ArrowUp': {
      const prev = tiers.value[tierIndex - 1]
      if (prev && prev.subjects.length > 0) {
        target = { tier: tierIndex - 1, index: Math.min(index, prev.subjects.length - 1) }
      }
      break
    }
    case 'Home':
      target = { tier: tierIndex, index: 0 }
      break
    case 'End':
      target = { tier: tierIndex, index: currentTier.subjects.length - 1 }
      break
    case 'Escape':
      // Esc 把书插回去，焦点留在原处：用户还在书架上，不该被弹到页面顶上。
      if (activeId.value !== null) {
        event.preventDefault()
        activeId.value = null
      }
      return
    default:
      return
  }

  if (!target) return
  event.preventDefault()
  const nextSubject = tiers.value[target.tier]?.subjects[target.index]
  if (nextSubject) spineEls.get(nextSubject.id)?.focus()
}

function openSubjects(): void {
  router.push({ name: 'subjects' })
}
</script>

<template>
  <div class="home">
    <header class="home__head rk-container">
      <h1 class="home__title">软考科目</h1>
      <p class="home__lead">
        共 {{ total }} 门，分三级。蓝框的 {{ printedCount }} 门大纲已完整——其余的立在架上，
        等着印出来。取下一本，看它的考期、大纲与卷面。
      </p>
    </header>

    <el-alert
      v-if="loadError"
      class="home__alert rk-container"
      type="error"
      :title="`科目加载失败：${loadError}`"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <!-- 载入中：先按书架的骨架占位（三层、书脊形状，见 SKELETON_TIERS）。
         不做闪烁动画——这一页只有一处动效，留给"把书取下来"。 -->
    <div v-if="subjectStore.loading && !subjectStore.loaded" class="shelf rk-container" aria-hidden="true">
      <div v-for="(ghosts, tierIndex) in SKELETON_TIERS" :key="tierIndex" class="shelf__tier">
        <div class="shelf__books">
          <span v-for="m in ghosts" :key="m" class="spine spine--ghost" />
        </div>
        <div class="shelf__board" />
      </div>
    </div>

    <el-empty
      v-else-if="!loadError && total === 0"
      class="home__empty rk-container"
      description="暂无科目数据"
    />

    <template v-else>
      <div class="shelf rk-container">
        <section v-for="(tier, tierIndex) in tiers" :key="tier.level" class="shelf__tier">
          <div class="shelf__books">
            <button
              v-for="(subject, index) in tier.subjects"
              :key="subject.id"
              :ref="(el) => setSpineRef(subject.id, el)"
              type="button"
              class="spine"
              :class="{
                'spine--printed': subject.outlineStatus === 'FULL',
                'spine--out': activeId === subject.id,
              }"
              :aria-pressed="activeId === subject.id"
              :aria-label="`${subject.name}，${OUTLINE_STATUS_LABEL[subject.outlineStatus]}，卷面满分 ${subject.paperTotalScore ?? '未知'} 分，${EXAM_PERIOD_LABEL[subject.examPeriod]}`"
              @click="toggle(subject)"
              @keydown="onSpineKeydown($event, tierIndex, index)"
            >
              <span class="spine__name"><span class="spine__name-text">{{ subject.name }}</span></span>
              <span class="spine__foot">
                <span class="spine__code">{{ subject.code }}</span>
                <span class="spine__score">{{ subject.paperTotalScore ?? '—' }}</span>
              </span>
            </button>
          </div>

          <div class="shelf__board">
            <span class="shelf__tier-label">{{ tier.label }}</span>
            <span class="shelf__tier-count">
              {{ tier.subjects.filter((s) => s.outlineStatus === 'FULL').length }} / {{ tier.subjects.length }}
            </span>
          </div>
        </section>
      </div>

      <!-- 取下来的那本。高度不预留，避免首屏被一块空白占掉；
           它长在书架下方，书架本身不动（toggle 里负责在需要时把它滚进视口）。 -->
      <div ref="drawerEl" class="drawer rk-container">
        <div v-if="activeSubject" class="drawer__body">
          <div class="drawer__head">
            <h2 class="drawer__name">{{ activeSubject.name }}</h2>
            <span class="drawer__code">{{ activeSubject.code }}</span>
          </div>

          <dl class="drawer__facts">
            <div>
              <dt>考期</dt>
              <dd>{{ EXAM_PERIOD_LABEL[activeSubject.examPeriod] }}</dd>
            </div>
            <div>
              <dt>大纲</dt>
              <dd>{{ OUTLINE_STATUS_LABEL[activeSubject.outlineStatus] }}</dd>
            </div>
            <div>
              <dt>题库</dt>
              <dd>{{ activeSubject.questionCount }} 题</dd>
            </div>
            <div>
              <dt>卷面</dt>
              <dd>{{ activeSubject.paperTotalScore ?? '—' }} 分</dd>
            </div>
          </dl>

          <el-button class="drawer__action" type="primary" @click="openSubjects">在科目页查看</el-button>
        </div>

        <p v-else class="drawer__hint">选一根书脊，把它取下来看考期、大纲与卷面。</p>
      </div>

      <footer class="legend rk-container">
        <ul class="legend__items">
          <li class="legend__item">
            <span class="legend__swatch legend__swatch--printed" aria-hidden="true" />
            <span>蓝框 = 大纲完整（{{ printedCount }} 门）</span>
          </li>
          <li class="legend__item">
            <span class="legend__swatch legend__swatch--unprinted" aria-hidden="true" />
            <span>虚线框 = 大纲仍是骨架（{{ total - printedCount }} 门）</span>
          </li>
        </ul>

        <p class="legend__note">
          题库仍在建设，架上只写结构事实：大纲是否完整；书脚是科目编号与卷面满分，
          考期在取下来的抽屉里。
        </p>
      </footer>
    </template>
  </div>
</template>

<style scoped lang="scss">
// 书脊统一高度。
//
// 2026-10-02 之前这里是"三档脊高，按 --spine-ratio 线性换算"，现已移除：
// 每层内部高度本已相同，脊高只重复了层级（层级由层标签说了）；唯一被它压矮的
// IPT（75 分）反而把书名挤成三列错行。
//
// 160px 是这么定的：三层等高之后，三层书脊要能在参考视口 1280×800 里**同时看到**
// （实测 200px 会把初级层挤到折线以下），且总高与改动前的 200+133+133 基本持平。
// 这个高度下只有最长的 11 字名（系统集成项目管理工程师）折成两列，其余 19 本单列。
$spine-height: 160px;
$spine-width: 88px;
$spine-gap: 8px;

.home {
  padding-bottom: $rk-space-xl * 2;

  &__head {
    padding-top: $rk-space-xl;
  }

  &__title {
    font-size: $rk-font-xl;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__lead {
    margin-top: $rk-space-s;
    color: $rk-text-2;
    max-width: 720px;
  }

  &__alert {
    margin-top: $rk-space-l;
  }

  &__empty {
    margin-top: $rk-space-xl * 2;
  }
}

.shelf {
  margin-top: $rk-space-xl;

  &__tier + &__tier {
    margin-top: $rk-space-l;
  }

  // 书立在层板上：align-items 靠底对齐，高度差就自然变成"高矮"
  &__books {
    display: flex;
    align-items: flex-end;
    gap: $spine-gap;
    min-height: $spine-height;
  }

  // 层板是这一页唯一的"实体感"，来自一条实线，不来自阴影或渐变。
  // 用 $rk-text-3 而不是 $rk-border：后者是和书脊描边同一档的细线，
  // 层板要能一眼分辨出"书站在什么东西上"。
  &__board {
    display: flex;
    align-items: baseline;
    gap: $rk-space-s;
    border-bottom: 1px solid $rk-text-3;
    padding-bottom: $rk-space-xs;
  }

  &__tier-label {
    font-size: $rk-font-s;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__tier-count {
    font-size: $rk-font-xs;
    color: $rk-text-3;
    font-variant-numeric: tabular-nums;
  }
}

.spine {
  // 书脊形状：定宽、等高、内容自下而上排
  flex: 0 0 $spine-width;
  width: $spine-width;
  height: $spine-height;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 0;
  font: inherit;
  text-align: center;
  cursor: pointer;
  background-color: $rk-bg-page;
  border: 1px dashed $rk-border;
  border-radius: $rk-radius-s $rk-radius-s 0 0;
  position: relative;
  overflow: hidden;
  // 唯一的一处动效：把书从架上取下来。指数缓出，从"已经看得见"的状态出发。
  transition:
    transform 220ms cubic-bezier(0.16, 1, 0.3, 1),
    border-color 160ms ease-out;

  &:hover:not(.spine--out) {
    // 悬停只是"手指搭上去"：抬起 4px，不改变任何信息
    transform: translateY(-4px);
    border-color: $rk-text-3;
  }

  // 取下来了：整本书离开层板。不用投影——那是拟物；
  // 靠"底部离开层板线"这件事本身说明它被拿起来了。
  &--out {
    transform: translateY(-14px);
    z-index: 1;
  }

  &--printed {
    background-color: $rk-bg-card;
    // 描边用全站唯一的那个蓝。1px 描边是**图形**不是文字，craft floor 对图形的
    // 要求是 ≥3:1，主色（白底 4.90:1）过线；「已印」的含义仍由实线/虚线这组
    // **形状**承担，颜色只是加强——所以它不受文字底线约束。见 docs/18 的 F14。
    border: 1px solid $rk-primary;
  }

  // 载入骨架：与真书同形，但不可交互、不参与朗读
  &--ghost {
    cursor: default;
    border-style: solid;
    pointer-events: none;
  }

  &:focus-visible {
    // 焦点环是图形指示，需要 3:1；主色本身在页面底只有 2.59:1，用文字档
    outline: 2px solid $rk-primary;
    outline-offset: 2px;
  }

  &__name {
    flex: 1;
    min-height: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    padding: 2px;
  }

  // 竖排书名。名字长的（如「系统集成项目管理工程师」）会自动折成第二列，
  // 所以不需要为长名字单独缩字号——那会让同一层书架上的字号参差不齐。
  &__name-text {
    writing-mode: vertical-rl;
    max-height: 100%;
    font-size: $rk-font-s;
    line-height: 1.15;
    letter-spacing: 0.06em;
    font-weight: 600;
    color: $rk-text-1;
    // 11 字长名会折成两列。让两列字数尽量均衡（6+5 而不是 8+3），
    // 视觉上是一块整齐的书名，而不是一列长一列短。纯渐进增强：
    // 不支持 text-wrap: balance 的浏览器忽略这一行，退回现有自动折行。
    // 不缩字号、不改脊宽——那两条是设计系统的硬规则（见文件头注释）。
    text-wrap: balance;
  }

  // 书脚是**一行**（编号 + 满分并排），不是两行。
  // 等高之后这一条不再是"必须"（200px 的脊容得下两行书脚），
  // 但一行仍是更好的读法：编号与满分并排，一眼对得齐。
  &__foot {
    flex: 0 0 auto;
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 3px;
    padding-bottom: 4px;
    font-size: $rk-font-xs;
    line-height: 1;
    font-variant-numeric: tabular-nums;
  }

  &__code {
    color: $rk-text-3;
    letter-spacing: 0.02em;
  }

  &__score {
    color: $rk-text-3;
  }

  // 已印书脊的编号与满分是 12px 小字，落在白底卡片上，走 4.5:1 的文字底线。
  // 主色自 F14 起是 #2a72c2（白底 4.90:1），这一处直接用主色就够——不再需要
  // "填充档 / 文字档"两个蓝。未印的那一档仍用 $rk-text-3，靠明度而非色相区分。
  &--printed &__code,
  &--printed &__score {
    color: $rk-primary;
  }
}

.drawer {
  margin-top: $rk-space-l;

  &__body {
    display: flex;
    align-items: center;
    gap: $rk-space-xl;
    flex-wrap: wrap;
    padding: $rk-space-l;
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
  }

  &__head {
    display: flex;
    align-items: baseline;
    gap: $rk-space-s;
    min-width: 220px;
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

  // 规格条：紧跟在科目名后面，不甩到最右边。
  // 甩过去会在中间留出一大段空白，看上去是两个断开的簇而不是一行信息。
  // 头部固定的 min-width 让不同长度的科目名后面，这几项的起点仍然对齐。
  &__facts {
    display: flex;
    gap: $rk-space-xl;
    font-size: $rk-font-xs;

    dt {
      color: $rk-text-3;
    }

    dd {
      color: $rk-text-1;
      margin-top: 2px;
      font-variant-numeric: tabular-nums;
    }
  }

  // 入口推到最右：规格条读完之后，动作落在行的末端。
  &__action {
    margin-left: auto;
  }

  &__hint {
    color: $rk-text-3;
    // 未选中时占一行的高度：有提示、但不留一块空白把首屏吃掉
    padding: $rk-space-s 0;
  }
}

.legend {
  margin-top: $rk-space-xl;
  padding-top: $rk-space-l;
  border-top: 1px solid $rk-border;

  &__items {
    display: flex;
    flex-wrap: wrap;
    gap: $rk-space-l $rk-space-xl;
    font-size: $rk-font-xs;
    color: $rk-text-2;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: $rk-space-s;
  }

  &__swatch {
    width: 12px;
    height: 18px;
    border-radius: $rk-radius-s $rk-radius-s 0 0;

    &--printed {
      background-color: $rk-bg-card;
      // 必须与 .spine--printed 的描边逐字相同——图例的唯一职责就是和它描述的对象一致
      border: 1px solid $rk-primary;
    }

    &--unprinted {
      background-color: $rk-bg-page;
      border: 1px dashed $rk-border;
    }
  }

  &__note {
    margin-top: $rk-space-m;
    font-size: $rk-font-xs;
    color: $rk-text-3;
  }
}

// 尊重系统的"减少动态效果"。取下来的结果仍然成立（只是没有过渡），
// 不是把功能关掉。
@media (prefers-reduced-motion: reduce) {
  .spine {
    transition: none;
  }
}
</style>
