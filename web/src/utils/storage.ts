// 本地存储的唯一入口（07 §7.2）。
//
// 为什么要包一层而不是随手 localStorage.setItem：
// ① 全部 key 带 `rk:` 前缀，同域下如果有别的页面就不会互相覆盖；
// ② 统一 try/catch —— 隐私模式、配额写满时 setItem 会抛异常，
//    散落各处的裸调用会以"某个按钮点了没反应"的形式暴露，很难定位；
// ③ 序列化/反序列化只在一处，避免有人存了对象却按字符串读回来。

const PREFIX = 'rk:'

function fullKey(key: string): string {
  return `${PREFIX}${key}`
}

export const storage = {
  get<T>(key: string): T | null {
    try {
      const raw = window.localStorage.getItem(fullKey(key))
      if (raw === null) return null
      return JSON.parse(raw) as T
    } catch {
      // 值不是合法 JSON（被别的代码写坏了、或旧版本存的格式）时按"没有"处理。
      // 抛出去会让整个应用在启动阶段白屏，而正确做法是当作未登录/无草稿。
      return null
    }
  },

  set(key: string, value: unknown): void {
    try {
      window.localStorage.setItem(fullKey(key), JSON.stringify(value))
    } catch {
      // 写不进去（配额满 / 隐私模式）不阻断业务：内存里的状态仍然是对的，
      // 代价只是刷新后丢一次登录态，用户重新登录即可。
    }
  },

  remove(key: string): void {
    try {
      window.localStorage.removeItem(fullKey(key))
    } catch {
      // 同上
    }
  },

  clearAll(): void {
    try {
      // 只清本项目的 key，不动同域下其他人的数据
      const keys: string[] = []
      for (let i = 0; i < window.localStorage.length; i += 1) {
        const k = window.localStorage.key(i)
        if (k !== null && k.startsWith(PREFIX)) keys.push(k)
      }
      keys.forEach((k) => window.localStorage.removeItem(k))
    } catch {
      // 同上
    }
  },
}

/** 存储 key 常量。集中在这里，避免 'rk:token' 这个字符串在项目里出现第二次。 */
export const STORAGE_KEYS = {
  /** 登录令牌（07 §7.8：localStorage 而非 Cookie）。 */
  token: 'token',
  /**
   * 最近一次发起、尚未交卷的模拟考会话（`{ sessionId, subjectId, savedAt }`）。
   *
   * 存在的唯一理由：`POST /exams` 在存在未完成会话时返回 `4003`，而该响应**不带
   * `sessionId`**（05 §5.6 第 16 条只定义了错误码，`BizException` 无 data 载荷）。
   * 07 §7.4 要求 4003 对话框显示 `sessionId` 并支持「继续 / 放弃」，因此前端只能在
   * 发起成功时把它记下来。清除时机：交卷成功、放弃成功、或读到终态会话。
   */
  examActive: 'exam:active',
} as const
