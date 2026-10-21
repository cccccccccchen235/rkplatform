<script setup lang="ts">
// 个人中心（07 §7.3.1 / §7.5.1 的 M1）。路由 `/profile`，考生端布局。
//
// 四件事：基本信息、改昵称、改密码、当前备考科目，外加退出登录。
//
// 两条口径写死在实现里，不是可以商量的细节：
//   1. **改密成功后强制重新登录**（07 §7.5.1）。服务端改密不踢会话，所以成功后必须
//      显式调一次登出（store 的 updatePassword 内部已做），再跳登录页。
//   2. **退出登录二次确认**（07 §7.5.1）。退出本身不可撤销，点错一次要重登一遍。
//
// `mustChangePassword = true` 时本页是守卫强制跳转的落点（06 §6.5），
// 所以页面对这种情况要给出一条明确的说明，而不是让用户猜"为什么老被弹到这里"。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'

import { useSubjectStore } from '@/stores/subject'
import { useUserStore } from '@/stores/user'
import { USER_ROLE_LABEL, USER_STATUS_LABEL } from '@/types/labels'
import type { RequestError } from '@/utils/request'

const router = useRouter()
const user = useUserStore()
const subjectStore = useSubjectStore()

const loading = ref(false)
const loadError = ref('')

const nicknameForm = reactive({ nickname: '' })
const nicknameFormRef = ref<FormInstance>()
const nicknameError = ref('')
const savingNickname = ref(false)

const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordError = ref('')
const savingPassword = ref(false)

const subjectId = ref('')
const subjectError = ref('')
const savingSubject = ref(false)

const userInfo = computed(() => user.userInfo)

/** 表单里的昵称与当前科目跟着 store 走：刷新用户信息后表单不该停在旧值上。 */
watch(
  () => user.userInfo,
  (value) => {
    nicknameForm.nickname = value?.nickname ?? ''
    subjectId.value = value?.currentSubjectId ?? ''
  },
  { immediate: true },
)

const nicknameRules: FormRules = {
  nickname: [
    { required: true, message: '昵称不能为空', trigger: 'blur' },
    { max: 32, message: '昵称最长 32 位', trigger: 'blur' },
  ],
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '新密码长度须为 8~32 位', trigger: 'blur' },
    {
      // 与 05 §5.3 第 1 条的注册口径一致（服务端也会再校验一次）
      pattern: /^(?=.*[A-Za-z])(?=.*\d).*$/,
      message: '新密码须同时包含字母与数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function load(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    // silent：本页顶部有自己的错误面板；再弹一条 toast 就是同一次失败讲两遍
    await user.fetchMe()
  } catch (error) {
    loadError.value = (error as RequestError).message
  } finally {
    loading.value = false
  }
  // 科目下拉是公开接口，失败不该让整页报错（只是这一项没得选）
  await subjectStore.fetchGrouped({}, false, { silent: true }).catch(() => undefined)
}

async function handleNickname(): Promise<void> {
  nicknameError.value = ''
  const valid = await nicknameFormRef.value?.validate?.().catch(() => false)
  if (!valid) return

  savingNickname.value = true
  try {
    await user.updateNickname(nicknameForm.nickname.trim(), { silent: true })
    ElMessage.success('昵称已更新')
  } catch (error) {
    nicknameError.value = (error as RequestError).message
  } finally {
    savingNickname.value = false
  }
}

async function handlePassword(): Promise<void> {
  passwordError.value = ''
  const valid = await passwordFormRef.value?.validate?.().catch(() => false)
  if (!valid) return

  savingPassword.value = true
  try {
    // store 的 updatePassword 在成功后**已经登出**（服务端删会话 + 清本地 token），
    // 所以这里只剩"告诉用户并把他送到登录页"。
    await user.updatePassword(passwordForm.oldPassword, passwordForm.newPassword, { silent: true })
    ElMessage.success('密码已修改，请重新登录')
    await router.replace({ name: 'login' })
  } catch (error) {
    // 2006 原密码错误就走这一条：就地提示，不弹 toast（用户要盯着这句话决定重试）
    passwordError.value = (error as RequestError).message
  } finally {
    savingPassword.value = false
  }
}

async function handleSubject(value: string): Promise<void> {
  subjectError.value = ''
  savingSubject.value = true
  try {
    await user.setCurrentSubject(value, { silent: true })
    ElMessage.success('已切换当前备考科目')
  } catch (error) {
    subjectError.value = (error as RequestError).message
    // 失败时把下拉弹回 store 里的值（`:model-value` 单向绑定，本行只是立刻同步）
    subjectId.value = user.currentSubjectId ?? ''
  } finally {
    savingSubject.value = false
  }
}

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('退出后需要重新登录才能继续做题与查看统计。', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await user.logout()
  ElMessage.success('已退出登录')
  await router.push({ name: 'home' })
}

onMounted(load)
</script>

<template>
  <div class="profile rk-container">
    <header class="profile__header">
      <h1 class="rk-page-title">个人中心</h1>
      <p class="profile__lead">昵称、密码与当前备考科目。修改密码后需要重新登录。</p>
    </header>

    <el-alert v-if="loadError" type="error" :title="`用户信息加载失败：${loadError}`" :closable="false" show-icon class="profile__alert">
      <template #default>
        <el-button text type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-alert
      v-if="userInfo?.mustChangePassword"
      type="warning"
      :closable="false"
      show-icon
      class="profile__alert"
      title="管理员已重置你的密码"
    >
      请先在下方修改密码，改完再使用其它功能。
    </el-alert>

    <section v-if="userInfo" class="profile__card">
      <h2 class="profile__card-title">基本信息</h2>
      <dl class="profile__facts">
        <div class="profile__fact">
          <dt>用户名</dt>
          <dd>{{ userInfo.username }}</dd>
        </div>
        <div class="profile__fact">
          <dt>昵称</dt>
          <dd>{{ userInfo.nickname }}</dd>
        </div>
        <div class="profile__fact">
          <dt>角色</dt>
          <dd>{{ USER_ROLE_LABEL[userInfo.role] }}</dd>
        </div>
        <div class="profile__fact">
          <dt>账号状态</dt>
          <dd>{{ USER_STATUS_LABEL[userInfo.status as 'ACTIVE' | 'DISABLED'] ?? userInfo.status }}</dd>
        </div>
        <div class="profile__fact">
          <dt>最近登录</dt>
          <dd>{{ userInfo.lastLoginAt ?? '—' }}</dd>
        </div>
        <div class="profile__fact">
          <dt>注册时间</dt>
          <dd>{{ userInfo.createdAt ?? '—' }}</dd>
        </div>
      </dl>
    </section>

    <section class="profile__card">
      <h2 class="profile__card-title">修改昵称</h2>
      <el-alert v-if="nicknameError" type="error" :title="nicknameError" :closable="false" show-icon class="profile__alert" />
      <el-form ref="nicknameFormRef" :model="nicknameForm" :rules="nicknameRules" label-width="72px" class="profile__form">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="nicknameForm.nickname" maxlength="32" show-word-limit />
        </el-form-item>
        <el-button type="primary" :loading="savingNickname" @click="handleNickname">保存昵称</el-button>
      </el-form>
    </section>

    <section class="profile__card">
      <h2 class="profile__card-title">修改密码</h2>
      <el-alert v-if="passwordError" type="error" :title="passwordError" :closable="false" show-icon class="profile__alert" />
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="96px"
        class="profile__form"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <p class="profile__hint">新密码 8~32 位，须同时包含字母与数字；修改成功后需要重新登录。</p>
        <el-button type="primary" :loading="savingPassword" @click="handlePassword">修改密码</el-button>
      </el-form>
    </section>

    <section class="profile__card">
      <h2 class="profile__card-title">当前备考科目</h2>
      <el-alert v-if="subjectError" type="error" :title="subjectError" :closable="false" show-icon class="profile__alert" />
      <el-select
        v-model="subjectId"
        class="profile__subject"
        filterable
        :loading="subjectStore.loading"
        :disabled="savingSubject"
        placeholder="选择备考科目"
        @change="handleSubject"
      >
        <el-option v-for="item in subjectStore.flatList" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <p class="profile__hint">当前科目决定首页、错题本与统计的默认口径。顶栏的下拉与这里改的是同一件事。</p>
    </section>

    <section class="profile__card">
      <h2 class="profile__card-title">退出登录</h2>
      <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      <p class="profile__hint">退出后需要重新输入用户名与密码。服务端会同时让当前令牌失效。</p>
    </section>
  </div>
</template>

<style scoped lang="scss">
.profile {
  padding-top: $rk-space-l;
  padding-bottom: $rk-space-xl * 2;

  &__header {
    margin-bottom: $rk-space-l;
  }

  &__lead {
    color: $rk-text-2;
    max-width: 760px;
  }

  &__alert {
    margin-bottom: $rk-space-l;
  }

  &__card {
    background-color: $rk-bg-card;
    border: 1px solid $rk-border;
    border-radius: $rk-radius-l;
    padding: $rk-space-l;
    margin-bottom: $rk-space-l;
  }

  &__card-title {
    font-size: $rk-font-m;
    font-weight: 600;
    margin-bottom: $rk-space-m;
  }

  &__facts {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: $rk-space-m $rk-space-l;
    margin: 0;
  }

  &__fact {
    dt {
      color: $rk-text-3;
      font-size: $rk-font-xs;
    }

    dd {
      margin: 2px 0 0;
      color: $rk-text-1;
    }
  }

  &__form {
    max-width: 480px;
  }

  &__subject {
    width: 320px;
  }

  &__hint {
    color: $rk-text-3;
    font-size: $rk-font-xs;
    line-height: $rk-line-height;
    margin: $rk-space-s 0;
  }
}
</style>
