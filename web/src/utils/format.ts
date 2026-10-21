// 展示格式化。放在 utils 而不是组件里：同一份"秒 → hh:mm:ss"将来在答题页、
// 成绩页、模拟考中心都要用，各写一遍必然会有一天三处不一致。

/**
 * 秒 → `hh:mm:ss`。
 *
 * 负数按 0 处理：倒计时与服务端时间对不齐时可能出现 -1 秒，
 * 显示 `-00:00:01` 会让人以为系统坏了，显示 `00:00:00` 才是它真正的含义（已到点）。
 */
export function formatDuration(totalSeconds: number | null | undefined): string {
  if (totalSeconds == null || !Number.isFinite(totalSeconds)) return '--:--:--'
  const s = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(s / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  const seconds = s % 60
  return [hours, minutes, seconds].map((n) => String(n).padStart(2, '0')).join(':')
}

/**
 * 秒 → `xh ym`（成绩页的用时口径，07 §7.5.1 专项说明）。
 *
 * 不足 1 分钟显示 `ym`，不足 1 小时显示 `ym`：成绩页关心的是"花了多久"，
 * 秒级精度在那里没有意义，反而让数字卡变长。
 */
export function formatUsedTime(totalSeconds: number | null | undefined): string {
  if (totalSeconds == null || !Number.isFinite(totalSeconds)) return '—'
  const s = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(s / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  if (hours > 0) return minutes > 0 ? `${hours}小时${minutes}分` : `${hours}小时`
  if (minutes > 0) return `${minutes}分`
  return `${s}秒`
}

/**
 * 0~1 的小数 → 百分比字符串。
 *
 * 入参可能是 null（未作答时服务端没有正确率可给），此时返回 `—` 而不是 `0%`：
 * 两者在成绩页上是"没做"与"全错"的区别，不能混。
 */
export function formatPercent(ratio: number | null | undefined, fractionDigits = 1): string {
  if (ratio == null || !Number.isFinite(ratio)) return '—'
  return `${(ratio * 100).toFixed(fractionDigits)}%`
}

/**
 * 服务端时间字符串 → `yyyy-MM-dd HH:mm`。
 *
 * 契约格式是 `yyyy-MM-dd HH:mm:ss`（05 §5.1），直接截断到分钟即可，
 * 不 new Date() 再格式化：那样会把服务端的 Asia/Shanghai 时间按浏览器本地时区重新解释，
 * 跨时区访问时显示的日期会整体偏移一天。
 */
export function formatDateTimeMinute(value: string | null | undefined): string {
  if (!value) return '—'
  return value.length >= 16 ? value.slice(0, 16) : value
}

/** 服务端时间字符串 → `yyyy-MM-dd`。 */
export function formatDate(value: string | null | undefined): string {
  if (!value) return '—'
  return value.length >= 10 ? value.slice(0, 10) : value
}
