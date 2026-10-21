// ===============================================================
// axios 封装（07 §7.7）。本文件是机制层，不认识任何业务概念：
// 不 import 任何 api/*，只认 05 §5.1 的响应体形状与错误码。
// 放在 utils/ 而不是 api/ 的理由见 07 §7.2 的决策段。
// ===============================================================

import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

import { STORAGE_KEYS, storage } from './storage'

// ElMessage 不在这里显式 import：由 unplugin-auto-import 的 ElementPlusResolver 注入
// （vite.config.ts）。显式 `import { ElMessage } from 'element-plus'` 不会带上它的样式，
// 结果是"提示能弹出来但没有样式"——一个看起来像 CSS 冲突、实际是引入方式不对的问题。
// 代价是 `pnpm type-check` 依赖 src/types/auto-imports.d.ts 已生成（首次 dev/build 时产出）。

declare module 'axios' {
  export interface AxiosRequestConfig {
    /**
     * 调用方自行处理错误：拦截器不弹任何提示，只把错误抛出去。
     * 用于"错误本身就是要展示给用户看的流程"（如 4003 有未完成考试时弹继续/放弃对话框），
     * 否则会出现「先弹一个通用错误提示、再弹一个业务对话框」的双重提示。
     */
    silent?: boolean
  }
}

/**
 * 调用方能传的请求选项。**故意比 `AxiosRequestConfig` 窄**：
 * 上面那个 `silent` 是页面唯一需要关心的开关，其余 axios 配置（headers、signal…）
 * 交给拦截器自己管——把它们暴露出去，api/ 就会开始 import axios，
 * 而 07 §7.11 要求 api/ 只做"路径 + 参数"的映射。
 */
export interface RequestOptions {
  /** 见 `AxiosRequestConfig.silent`（上面的模块声明）。 */
  silent?: boolean
}

/** 拦截器统一抛出的错误对象（07 §7.7）。页面用 `isBusiness` 区分业务失败与网络失败。 */
export interface RequestError extends Error {
  code: number
  message: string
  data: unknown
  traceId?: string
  isBusiness: boolean
}

/** 业务错误码（05 §5.1.2）。只列拦截器需要分流判断的，其余走兜底分支。 */
const CODE = {
  SUCCESS: 0,
  PARAM_INVALID: 1001,
  TOO_MANY_REQUESTS: 1002,
  RESOURCE_NOT_FOUND: 1003,
  FORBIDDEN: 1004,
  LOGIN_FAILED: 2001,
  NOT_LOGGED_IN: 2005,
  QUESTION_NOT_FOUND: 3002,
  QUESTION_DUPLICATE: 3005,
  SESSION_UNFINISHED_EXISTS: 4003,
  SESSION_NOT_FOUND: 4004,
  SESSION_ALREADY_ENDED: 4005,
} as const

/** 默认静默的业务码：由业务页面接管提示（07 §7.7 的 4003/4005 行）。 */
const SILENT_BY_DEFAULT = new Set<number>([CODE.SESSION_UNFINISHED_EXISTS, CODE.SESSION_ALREADY_ENDED])

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 15000,
})

// ---------------- 去重与取消 ----------------

/** 仅 GET 参与去重：写操作必须逐次到达服务端（作答保存、交卷）。 */
const pendingGetMap = new Map<string, Promise<unknown>>()

/** configKey → AbortController。路由切换时整体取消，避免旧页数据渲染到新页。 */
const aborters = new Map<string, AbortController>()

let seq = 0

function nextConfigKey(config: InternalAxiosRequestConfig): string {
  seq += 1
  return `${config.method ?? 'get'}:${config.url ?? ''}:${seq}`
}

function dedupeKey(config: InternalAxiosRequestConfig): string {
  const params = config.params ? JSON.stringify(config.params) : ''
  return `${config.method ?? 'get'}:${config.url ?? ''}:${params}`
}

/** 供路由切换 / 组件卸载时调用（07 §7.7）。 */
export function cancelAll(): void {
  aborters.forEach((controller) => controller.abort())
  aborters.clear()
  pendingGetMap.clear()
}

// ---------------- 未登录的处理权交给调用方 ----------------

/** 2005/HTTP 401 发生时传给处理方的上下文。 */
export interface UnauthorizedContext {
  /** 面向用户的文案。 */
  message: string
  /** 失败的请求本身是登录/注册：此时不该再跳转，用户已经在登录页上了。 */
  fromAuthEntry: boolean
}

export type UnauthorizedHandler = (context: UnauthorizedContext) => void | Promise<void>

/**
 * 注册「登录已过期」的处理逻辑，由 router/index.ts 在启动时注入。
 *
 * 为什么用注册回调而不是在这里直接 import store 与 router：
 * 那样会形成 request → stores/user → api/auth → request 的循环 import。
 * 用动态 import 能绕开循环，但会留下"既静态又动态引入同一模块"的构建警告，
 * 而且让 utils/ 反向认识业务层——07 §7.2 明确要求 api/ 与 utils/ 的分工是
 * "机制不认识业务"。注册回调把这个方向掰回来了：谁有状态谁负责清，
 * 机制层只负责在自己的契约被违反时叫一声。
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

let unauthorizedHandler: UnauthorizedHandler | null = null

/** 没有任何处理方注册时的兜底：清掉本地令牌并整页跳登录。 */
function fallbackUnauthorized(context: UnauthorizedContext): void {
  storage.remove(STORAGE_KEYS.token)
  const { pathname, search } = window.location
  if (pathname === '/login' || pathname === '/register') return
  const redirect = encodeURIComponent(`${pathname}${search}`)
  window.location.assign(`${import.meta.env.BASE_URL}login?redirect=${redirect}`)
  void context
}

// ---------------- 请求拦截器 ----------------

service.interceptors.request.use((config) => {
  const configKey = nextConfigKey(config)
  ;(config as InternalAxiosRequestConfig & { configKey?: string }).configKey = configKey

  const controller = new AbortController()
  config.signal = controller.signal
  aborters.set(configKey, controller)

  // token 从 storage 读而不是从 store 读：utils/ 不依赖 stores/，
  // 否则会形成 request → stores/user → api/auth → request 的循环 import。
  // store 在登录/登出时会把 token 写进同一个 key，两者始终一致。
  const token = storage.get<string>(STORAGE_KEYS.token)
  if (token) {
    // Sa-Token 的 token-name = Authorization、token-prefix = Bearer（05 §5.1 / application.yml）
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// ---------------- 响应拦截器 ----------------

function cleanup(config: InternalAxiosRequestConfig | undefined): void {
  if (!config) return
  const key = (config as InternalAxiosRequestConfig & { configKey?: string }).configKey
  if (key) aborters.delete(key)
  if ((config.method ?? '').toLowerCase() === 'get') pendingGetMap.delete(dedupeKey(config))
}

function makeError(code: number, message: string, data: unknown, traceId: string | undefined, isBusiness: boolean): RequestError {
  const err = new Error(message) as RequestError
  err.code = code
  err.data = data
  err.traceId = traceId
  err.isBusiness = isBusiness
  return err
}

/** 登录/注册接口自身失败时不跳转：用户已经在登录页上了（07 §7.7 的 2005 行）。 */
function isAuthEntryRequest(config: InternalAxiosRequestConfig | undefined): boolean {
  const url = config?.url ?? ''
  return url.includes('/auth/login') || url.includes('/auth/register')
}

async function handleNotLoggedIn(config: InternalAxiosRequestConfig | undefined, message: string): Promise<never> {
  const context: UnauthorizedContext = { message, fromAuthEntry: isAuthEntryRequest(config) }
  if (unauthorizedHandler) await unauthorizedHandler(context)
  else fallbackUnauthorized(context)
  throw makeError(CODE.NOT_LOGGED_IN, message, null, undefined, true)
}

async function handleBusinessError(body: { code: number; message: string; data: unknown; traceId?: string }, config: InternalAxiosRequestConfig | undefined): Promise<never> {
  const { code, message, data, traceId } = body
  const silent = config?.silent === true || SILENT_BY_DEFAULT.has(code)

  switch (code) {
    case CODE.PARAM_INVALID:
      // data 是字段级错误列表。这里只弹第一条做即时反馈，
      // 表单页应 catch 后自行 setFields，不要依赖 toast 来告诉用户哪个字段错了。
      if (!silent) {
        ElMessage.error(extractFirstFieldMessage(data) ?? message)
      }
      break

    case CODE.TOO_MANY_REQUESTS:
      if (!silent) ElMessage.warning(message)
      break

    case CODE.RESOURCE_NOT_FOUND:
    case CODE.QUESTION_NOT_FOUND:
    case CODE.SESSION_NOT_FOUND:
      // 页面会据此进入空态；这里只提示，不做跳转
      if (!silent) ElMessage.warning(message)
      break

    case CODE.FORBIDDEN:
      // 不跳转、不清 token：越权可能只是局部操作，清 token 会把用户踢出整个应用
      if (!silent) ElMessage.error(message)
      break

    case CODE.NOT_LOGGED_IN:
      return handleNotLoggedIn(config, message)

    case CODE.QUESTION_DUPLICATE:
      // 3005 由管理端重复题对话框接管文案
      break

    default:
      if (!silent) {
        // 统一带 traceId：用户截图反馈时，这一串就能定位到日志行（05 §5.1）
        ElMessage.error(traceId ? `${message}（traceId: ${traceId}）` : message)
      }
  }

  throw makeError(code, message, data, traceId, true)
}

/**
 * 1001 的 data 形状是字段错误列表（`[{field, message}]`）；形状不符时返回 null。
 *
 * 只取 message 而不再拼 `field: ` 前缀：服务端给的文案已经自述了字段
 * （"用户名须为 4~32 位字母、数字或下划线"），加前缀会变成
 * "username: 用户名须为…"，中英混排且没有增加信息。
 */
function extractFirstFieldMessage(data: unknown): string | null {
  if (Array.isArray(data)) {
    const first = data[0] as { message?: string } | undefined
    if (first?.message) return first.message
  }
  if (data && typeof data === 'object') {
    const record = data as Record<string, string>
    const firstKey = Object.keys(record)[0]
    if (firstKey && typeof record[firstKey] === 'string') return record[firstKey]
  }
  return null
}

service.interceptors.response.use(
  (response: AxiosResponse) => {
    cleanup(response.config)
    const body = response.data as { code: number; message: string; data: unknown; traceId?: string } | undefined

    // 后端契约保证响应体永远是 R；不是 R 说明打到了非本项目接口（如反代把 /api 转到了别的服务）
    if (!body || typeof body.code !== 'number') {
      ElMessage.error('服务返回了无法识别的响应')
      throw makeError(-1, '服务返回了无法识别的响应', body, undefined, false)
    }

    if (body.code === CODE.SUCCESS) {
      // 解包：调用方直接拿业务数据，不写 res.data.data（07 §7.7）
      return body.data as never
    }
    return handleBusinessError(body, response.config) as never
  },

  async (error: AxiosError) => {
    cleanup(error.config as InternalAxiosRequestConfig | undefined)

    // HTTP 401/403 不是网络故障：它们是 2005/1004 的传输层表达（05 §5.1.1 规定
    // 只有这两个码值用非 200 状态码），响应体仍是标准 R，按业务错误分流。
    const body = error.response?.data as { code: number; message: string; data: unknown; traceId?: string } | undefined
    if (body && typeof body.code === 'number') {
      return handleBusinessError(body, error.config as InternalAxiosRequestConfig | undefined) as never
    }

    if (axios.isCancel(error)) {
      // 主动取消（路由切换）不是错误，页面不该弹提示。
      // 抛出的对象仍带 isBusiness=false，调用方按需忽略。
      throw makeError(-2, '请求已取消', null, undefined, false)
    }

    const message =
      error.code === 'ECONNABORTED' || error.message.includes('timeout')
        ? '请求超时，请稍后重试'
        : '网络异常，请稍后重试'

    if (error.config?.silent !== true) ElMessage.error(message)
    throw makeError(-1, message, null, undefined, false)
  },
)

/**
 * 统一出口。类型参数是**解包后的业务数据类型**，不是 R<T>：
 * 拦截器已经把 R 拆开，所以这里写 `request<SubjectVO[]>` 而不是 `request<ApiResult<...>>`。
 */
function request<T>(config: AxiosRequestConfig): Promise<T> {
  // 双断言在这里是必需的，不是图省事，两个原因叠在一起：
  // ① axios 1.20 的 `request<T, R, D, P>` 第二泛型是**条件类型**
  //    （`R extends AxiosResponseDefault ? AxiosResponse<...> : R`）。显式传一个
  //    泛型 T 进去，条件无法求值，返回类型就悬空成 `AxiosResponseResult<...>`
  //    而不是 T——`service.request<unknown, T>(config)` 写法过不了。
  // ② 本项目**已经不再使用** axios 的 `AxiosResponse`：响应拦截器把 `R<T>` 拆成
  //    `data` 之后直接 resolve（见上面的拦截器注释），运行时这里返回的就是 T。
  // 两边说的是同一件事，只有类型层对不上，所以在唯一这一个地方换算一次。
  // 全项目只有这一处 `as unknown as`。
  return service.request(config) as unknown as Promise<T>
}

/** GET，带去重（同一 url+params 的并发请求复用同一个 Promise）。 */
export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  const dedupe: AxiosRequestConfig = { ...config, method: 'get', url, params }
  const key = dedupeKey(dedupe as InternalAxiosRequestConfig)

  const existing = pendingGetMap.get(key)
  if (existing) return existing as Promise<T>

  const promise = request<T>(dedupe).finally(() => {
    pendingGetMap.delete(key)
  })
  pendingGetMap.set(key, promise)
  return promise
}

/** POST/PUT/PATCH 一律不去重：作答保存、交卷必须逐次到达服务端（07 §7.7）。 */
export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'post', url, data })
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'put', url, data })
}

/**
 * PATCH。
 *
 * 与 PUT 的区别是要表达的那件事：PUT 是"整体替换这份资源"，PATCH 是"只改这一处"。
 * 目前唯一的用处是科目的启停（05 §5.10 第 40 条）——它只动 `status` 一列，
 * 用 PUT 提交整份科目会让"这次改了什么"在审计日志里变成一个说不清的问题。
 *
 * 上面那行的注释早就写了 PATCH 不去重，但函数一直没落地；补上它而不是
 * 让调用方绕回 PUT。
 */
export function patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'patch', url, data })
}

export function del<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'delete', url, params })
}

export default service
