// 认证与当前用户（05 §5.3）。一个函数一个接口，不含任何机制代码（07 §7.2）。

import type {
  AuthResultVO,
  CurrentSubjectPayload,
  LoginPayload,
  NicknameUpdatePayload,
  PasswordUpdatePayload,
  RegisterPayload,
  UserVO,
} from '@/types/api'
import { get, post, put, type RequestOptions } from '@/utils/request'

// 登录与注册都接 `options`：两页各自用 el-alert 就地展示失败原因
// （登录页顶部的注释写明了"不用 toast"的理由），所以调用方一律传 `{ silent: true }`，
// 否则用户会先看到一条两秒就消失的 toast、再看到下面那块一直留着的提示。

/** 05 §5.3 第 1 条。成功后即为登录态，响应体与登录一致。 */
export function register(payload: RegisterPayload, options?: RequestOptions): Promise<AuthResultVO> {
  return post<AuthResultVO>('/auth/register', payload, options)
}

/**
 * 05 §5.3 第 2 条。失败 2001 / 禁用 2002 / 锁定 2004。
 *
 * 注释里说的"就地提示"是**页面**的事，不是拦截器的事：拦截器只负责把码与话抛出来
 * （`silent: true` 时连 toast 都不弹），由登录页决定怎么讲给用户听。
 */
export function login(payload: LoginPayload, options?: RequestOptions): Promise<AuthResultVO> {
  return post<AuthResultVO>('/auth/login', payload, options)
}

/** 05 §5.3 第 3 条。服务端删除会话；前端清本地是另一件事（见 stores/user.ts）。 */
export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}

/** 05 §5.3 第 4 条。刷新页面时恢复登录态与角色。 */
export function fetchMe(): Promise<UserVO> {
  return get<UserVO>('/users/me')
}

/** 05 §5.3 第 5 条。返回更新后的用户，供 store 直接回写。 */
export function updateMe(payload: NicknameUpdatePayload, options?: RequestOptions): Promise<UserVO> {
  return put<UserVO>('/users/me', payload, options)
}

/**
 * 05 §5.3 第 6 条。原密码错误返回 `2006`，由个人中心页就地提示。
 *
 * 成功后服务端会把 `mustChangePassword` 置 false；**服务端不踢会话**，
 * 所以"改完必须重新登录"这件事由调用方显式调 {@link logout} 完成（07 §7.5.1）。
 */
export function updatePassword(payload: PasswordUpdatePayload, options?: RequestOptions): Promise<void> {
  return put<void>('/users/me/password', payload, options)
}

/** 05 §5.3 第 7 条。科目不存在或已停用返回 `3001`。 */
export function updateCurrentSubject(
  payload: CurrentSubjectPayload,
  options?: RequestOptions,
): Promise<UserVO> {
  return put<UserVO>('/users/me/current-subject', payload, options)
}
