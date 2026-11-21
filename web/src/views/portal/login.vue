<script setup lang="ts">
// 登录（07 §7.5.1）。
//
// 错误处理的口径：2001 / 2002 / 2004 都由拦截器抛出，这里**就地**用 el-alert 展示，
// 不用 toast——登录失败是用户要盯着看、要据此决定下一步的信息
// （是记错了密码？还是账号被锁了要等 15 分钟？），toast 两秒就消失了。

import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'

import { useUserStore } from '@/stores/user'
import type { RequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
/** 登录失败的完整文案，就地展示。 */
const errorMessage = ref('')
/** 锁定态单独标记：它不该和"密码错了"长得一样。 */
const locked = ref(false)

const form = reactive({
  username: '',
  password: '',
})

// 登录页只校验"必填"。刻意不在这里复刻注册时的复杂度规则（8~32 位含字母数字）：
// 老用户的密码可能是在规则收紧之前设的，前端先拦下来他就永远登不进来了。
// 密码格式的裁决权在服务端（05 §5.3 第 2 条只要求 @NotBlank）。
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  errorMessage.value = ''
  locked.value = false

  try {
    // silent：本页开头那段注释就是为这个参数写的——失败原因留在页面上（alert），
    // 不弹 toast。少了它，用户会看到一条两秒消失的 toast 加一条一模一样的常驻提示。
    await user.login({ username: form.username, password: form.password }, { silent: true })
    ElMessage.success('登录成功')

    // 只接受站内相对路径，挡掉 `?redirect=//evil.com` 这类开放重定向（同 router/index.ts）
    const redirect = (route.query.redirect as string) || '/'
    await router.replace(redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/')
  } catch (error) {
    const err = error as RequestError
    if (err.isBusiness) {
      errorMessage.value = err.message
      // 2004 是"15 分钟后再试"，与 2001 的处置方式完全不同，视觉上要能区分
      locked.value = err.code === 2004
    } else {
      errorMessage.value = err.message
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-card__header">
        <h1 class="auth-card__title">登录</h1>
        <p class="auth-card__subtitle">继续你的备考进度</p>
      </header>

      <el-alert
        v-if="errorMessage"
        class="auth-card__alert"
        :title="errorMessage"
        :type="locked ? 'warning' : 'error'"
        :closable="false"
        show-icon
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" autocomplete="username" placeholder="4~32 位字母、数字或下划线" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入密码"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-button type="primary" class="auth-card__submit" :loading="submitting" @click="handleSubmit">
          登录
        </el-button>
      </el-form>

      <p class="auth-card__foot">
        还没有账号？
        <router-link :to="{ name: 'register', query: route.query }">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.auth-page {
  width: 400px;
  max-width: calc(100vw - 32px);
}

.auth-card {
  background-color: $rk-bg-card;
  border: 1px solid $rk-soft-border;
  border-radius: $rk-soft-radius-card;
  box-shadow: $rk-soft-shadow;
  padding: $rk-space-xl * 1.5;

  &__header {
    margin-bottom: $rk-space-xl;
  }

  &__title {
    font-size: $rk-font-xl;
    font-weight: 600;
    color: $rk-text-1;
  }

  &__subtitle {
    margin-top: $rk-space-xs;
    color: $rk-text-3;
  }

  &__alert {
    margin-bottom: $rk-space-l;
  }

  &__submit {
    width: 100%;
    margin-top: $rk-space-s;
  }

  &__foot {
    margin-top: $rk-space-xl;
    text-align: center;
    color: $rk-text-3;
  }
}
</style>
