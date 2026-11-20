import type { Directive } from 'vue'

/**
 * v-reveal —— 元素进入视口时上浮淡入
 * 用法：v-reveal 或 v-reveal="120"（延迟毫秒）
 */
const observer =
  typeof window !== 'undefined' && 'IntersectionObserver' in window
    ? new IntersectionObserver(
        (entries, obs) => {
          for (const entry of entries) {
            if (entry.isIntersecting) {
              entry.target.classList.add('is-in')
              obs.unobserve(entry.target)
            }
          }
        },
        { rootMargin: '0px 0px -8% 0px', threshold: 0.08 }
      )
    : null

const reducedMotion =
  typeof window !== 'undefined' &&
  window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

export const vReveal: Directive<HTMLElement, number | undefined> = {
  mounted(el, binding) {
    if (!observer || reducedMotion) {
      el.classList.add('reveal', 'is-in')
      return
    }
    el.classList.add('reveal')
    if (typeof binding.value === 'number' && binding.value > 0) {
      el.style.transitionDelay = `${binding.value}ms`
    }
    observer.observe(el)
  },
  unmounted(el) {
    observer?.unobserve(el)
  }
}
