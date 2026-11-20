/**
 * 全站统一的时间展示格式化：把后端 ISO 字符串（2026-11-12T09:30:00）渲染为 "YYYY-MM-DD HH:mm:ss"
 */
export function formatDateTime(val?: string | null): string {
  if (!val) return '-'
  return val.replace('T', ' ').substring(0, 19)
}
