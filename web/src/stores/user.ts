// 登录态与当前用户（07 §7.4.1）。

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import * as authApi from '@/api/auth'
import type { LoginPayload, RegisterPayload, UserVO } from '@/types/api'
import type { RequestOptions } from '@/utils/request'
import { STORAGE_KEYS, storage } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  // ---------------- state ----------------

  /**
   * 令牌。初始化自 localStorage，所以刷新页面后仍然是登录态（07 §7.8）。
   *
   * 它是本 store 唯一持久化的字段：`userInfo` 刻意不持久化——用户可能已经被禁用、
   * 改了角色、或被管理员改了昵称，把旧的 userInfo 从本地读回来会让这些变化看不见。
   * 刷新时统一走 `fetchMe()` 重新问服务端（见 router 守卫）。
   */
  const token = ref<string>(storage.get<string>(STORAGE_KEYS.token) ?? '')

  const userInfo = ref<UserVO | null>(null)

  /** 守卫取用户信息期间为 true，供顶部进度条与骨架屏使用。 */
  const permissionsLoading = ref(false)

  // ---------------- getters ----------------

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const currentSubjectId = computed(() => userInfo.value?.currentSubjectId ?? null)

  // ---------------- 内部工具 ----------------

  /** 写 token 时同步落盘，避免"内存里登录了、刷新就掉"两处不一致。 */
  function setToken(value: string): void {
    token.value = value
    storage.set(STORAGE_KEYS.token, value)
  }

  function applyAuthResult(result: { token: string; user: UserVO }): void {
    setToken(result.token)
    userInfo.value = result.user
  }

  // ---------------- actions ----------------

  async function register(payload: RegisterPayload, options?: RequestOptions): Promise<UserVO> {
    // 05 §5.3 第 1 条：注册成功即为登录态，响应与登录同构
    const result = await authApi.register(payload, options)
    applyAuthResult(result)
    return result.user
  }

  async function login(payload: LoginPayload, options?: RequestOptions): Promise<UserVO> {
    const result = await authApi.login(payload, options)
    applyAuthResult(result)
    return result.user
  }

  /** 刷新页面、或需要最新用户信息时调用（07 §7.3.3 守卫第 1 步）。 */
  async function fetchMe(): Promise<UserVO | null> {
    if (!token.value) return null
    permissionsLoading.value = true
    try {
      userInfo.value = await authApi.fetchMe()
      return userInfo.value
    } finally {
      permissionsLoading.value = false
    }
  }

  /** 只清本地状态，不发请求。2005 拦截器与守卫失败分支用它。 */
  function clearAuth(): void {
    token.value = ''
    userInfo.value = null
    permissionsLoading.value = false
    storage.remove(STORAGE_KEYS.token)
  }

  /**
   * 主动退出：先让服务端删会话，再清本地。
   *
   * 顺序不能反。先清本地的话，服务端那条会话会一直活到超时——用户以为"退出了"，
   * 但那个 token 在别处仍然可用。所以即便请求失败也要清本地（`finally`），
   * 只是这种失败要留下痕迹，不然"点了退出但没退"会变成一个无法复现的偶发现象。
   */
  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch (error) {
      // 服务端会话未能删除（网络断了、或已经超时）。本地仍然清掉，
      // 因为"用户想退出"这个意图必须被满足；同时把失败交给调用方决定是否提示。
      console.warn('[user] 服务端登出失败，已清除本地登录态', error)
    } finally {
      clearAuth()
    }
  }

  /**
   * 改昵称（05 §5.3 第 5 条）。
   *
   * 用服务端返回的 UserVO 回写，而不是本地改一个字段：昵称的裁剪与长度上限由服务端定，
   * 本地拼出一个"看起来一样"的值，迟早会与库里那行不一致（比如首尾空格）。
   */
  async function updateNickname(nickname: string, options?: RequestOptions): Promise<UserVO> {
    const updated = await authApi.updateMe({ nickname }, options)
    userInfo.value = updated
    return updated
  }

  /**
   * 改密码（05 §5.3 第 6 条）。
   *
   * **成功后强制登出**（07 §7.4.1）：服务端不踢会话，所以这里显式调一次 `logout()`
   * 让服务端删除会话、并清掉本地 token。顺序不能反——先清本地的话，那条会话会一直
   * 活到超时，用户以为"重新登录了"，其实旧 token 在别处仍然可用。
   */
  async function updatePassword(
    oldPassword: string,
    newPassword: string,
    options?: RequestOptions,
  ): Promise<void> {
    await authApi.updatePassword({ oldPassword, newPassword }, options)
    await logout()
  }

  /** 设置当前备考科目（05 §5.3 第 7 条）。成功后回写 `currentSubjectId`。 */
  async function setCurrentSubject(subjectId: string, options?: RequestOptions): Promise<UserVO> {
    const updated = await authApi.updateCurrentSubject({ subjectId }, options)
    userInfo.value = updated
    return updated
  }

  return {
    token,
    userInfo,
    permissionsLoading,
    isLogin,
    isAdmin,
    currentSubjectId,
    register,
    login,
    fetchMe,
    updateNickname,
    updatePassword,
    setCurrentSubject,
    clearAuth,
    logout,
  }
})
