<script setup lang="ts">
// 注册（07 §7.5.1）。
//
// 校验规则与 05 §5.3 第 1 条逐条对应，并且**与后端同源**：
// 前端拦一道是为了即时反馈，不是为了安全——后端的 @Valid 才是裁决者。
// 两处规则不一致时会表现为"前端说可以、后端回 1001"，所以规则值写在这里也写在注释里，
// 改的时候两边一起改。

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
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{4,32}$/,
      message: '4~32 位，仅限字母、数字与下划线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '长度需为 8~32 位', trigger: 'blur' },
    {
      // 后端同一条正则（RegisterRequest.java）：
      // (?=.*[A-Za-z])(?=.*\d) —— 必须同时含字母与数字
      pattern: /^(?=.*[A-Za-z])(?=.*\d).*$/,
      message: '需同时包含字母与数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  nickname: [{ max: 32, message: '昵称最多 32 个字符', trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  errorMessage.value = ''

  try {
    // 昵称留空时**不传该字段**，由服务端取用户名（05 §5.3 第 1 条）。
    // 传空串会走服务端的 resolveNickname 分支得到同样结果，但那样前端就承担了
    // "空串等于没填"这个约定——约定属于契约，应该留在服务端。
    await user.register(
      {
        username: form.username,
        password: form.password,
        ...(form.nickname.trim() ? { nickname: form.nickname.trim() } : {}),
      },
      // silent：失败原因由这一页自己讲——1001 逐字段标到表单上，
      // 其余（2001 / 2003）交给上面那块 alert。拦截器再弹一遍就是重复报告。
      { silent: true },
    )
    ElMessage.success('注册成功，已自动登录')

    const redirect = (route.query.redirect as string) || '/'
    await router.replace(redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/')
  } catch (error) {
    const err = error as RequestError
    if (err.code === 1001) {
      // 1001 的 data 是字段级错误列表，两条出口各司其职：
      // 表单上按字段标注（"要改的是这个输入框"），alert 里放总述。
      // 这里**不再依赖拦截器的 toast**——它两秒就消失，而用户要照着它改输入框，
      // 所以请求带了 `silent: true`。
      applyFieldErrors(err.data)
      errorMessage.value = err.message
    } else {
      errorMessage.value = err.message
    }
  } finally {
    submitting.value = false
  }
}

/** 把 1001 的字段错误落到对应表单项上。形状不符时静默跳过，不阻断提交流程。 */
function applyFieldErrors(data: unknown): void {
  if (!Array.isArray(data)) return
  const fields = data
    .filter((item): item is { field: string; message: string } => {
      const candidate = item as { field?: unknown; message?: unknown }
      return typeof candidate?.field === 'string' && typeof candidate?.message === 'string'
    })
    .map((item) => ({ prop: item.field, message: item.message }))
  if (fields.length > 0) formRef.value?.validateField(fields as never).catch(() => undefined)
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-card__header">
        <h1 class="auth-card__title">注册</h1>
        <p class="auth-card__subtitle">注册后即可开始练习与模拟考</p>
      </header>

      <el-alert
        v-if="errorMessage"
        class="auth-card__alert"
        :title="errorMessage"
        type="error"
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
            autocomplete="new-password"
            placeholder="8~32 位，需同时含字母与数字"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请再次输入密码"
          />
        </el-form-item>

        <el-form-item label="昵称（可留空）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="留空则使用用户名" @keyup.enter="handleSubmit" />
        </el-form-item>

        <el-button type="primary" class="auth-card__submit" :loading="submitting" @click="handleSubmit">
          注册
        </el-button>
      </el-form>

      <p class="auth-card__foot">
        已有账号？
        <router-link :to="{ name: 'login', query: route.query }">去登录</router-link>
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
