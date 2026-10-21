<script setup lang="ts">
// 分节提示条（07 §7.6 的 `SectionTip`）。
//
// 两种变体各自对应一条"不能做某件事"的业务规则，文案本身就是规则说明：
//   · `not-supported`：本平台不模拟 Office 操作题（如 IPT「应用技术」），只能去官方机考系统；
//   · `no-early-submit`：论文节必须待本场次时长达标后整体交卷，不能"结束本节"。
// 它是**空态**而不是错误态——这两种情况都不是故障，所以用 el-alert 的 info 档。

import type { ExamSectionVO } from '@/types/api'
import { SECTION_TYPE_LABEL } from '@/types/labels'

withDefaults(
  defineProps<{
    section: ExamSectionVO
    variant?: 'not-supported' | 'no-early-submit'
  }>(),
  { variant: 'not-supported' },
)
</script>

<template>
  <el-alert class="section-tip" :closable="false" show-icon type="info">
    <template #title>
      {{ section.sectionName }}（{{ SECTION_TYPE_LABEL[section.sectionType] }}）
    </template>
    <p v-if="variant === 'not-supported'" class="section-tip__text">
      本平台无法模拟 Office 操作题，本节请前往官方机考模拟练习平台练习。
    </p>
    <p v-else class="section-tip__text">
      本节不判分，须待本场次时长达标后整体交卷，不支持提前结束本节。
    </p>
  </el-alert>
</template>

<style scoped lang="scss">
.section-tip {
  &__text {
    margin-top: $rk-space-xs;
    color: $rk-text-2;
    font-size: $rk-font-s;
    line-height: $rk-line-height;
  }
}
</style>
