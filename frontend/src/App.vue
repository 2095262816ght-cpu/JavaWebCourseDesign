<template>
  <div class="app-shell">
    <!-- 氛围层：光晕 / 网格 / 噪点 / 鼠标光斑 -->
    <div class="ambient" aria-hidden="true">
      <span class="ambient-orb orb-violet"></span>
      <span class="ambient-orb orb-cyan"></span>
      <span class="ambient-orb orb-pink"></span>
      <span class="ambient-grid"></span>
      <span class="ambient-noise"></span>
      <span class="ambient-vignette"></span>
      <span
        class="ambient-spotlight"
        :style="{ transform: `translate3d(${spot.x}px, ${spot.y}px, 0)` }"
      ></span>
    </div>

    <el-config-provider>
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-config-provider>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onUnmounted } from 'vue'
import { ElConfigProvider } from 'element-plus'

const spot = reactive({ x: -600, y: -600 })
let frame = 0

function handlePointerMove(event: PointerEvent) {
  if (frame) return
  frame = requestAnimationFrame(() => {
    spot.x = event.clientX - 320
    spot.y = event.clientY - 320
    frame = 0
  })
}

onMounted(() => {
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('pointermove', handlePointerMove)
  if (frame) cancelAnimationFrame(frame)
})
</script>

<style>
/* ---------- 页面切换 ---------- */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.32s var(--ease), transform 0.32s var(--ease);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ---------- 外壳 ---------- */
.app-shell {
  position: relative;
  min-height: 100vh;
  isolation: isolate;
}

.app-shell > :not(.ambient) {
  position: relative;
  z-index: 2;
}

/* ---------- 氛围层 ---------- */
.ambient {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(900px 520px at 12% -8%, rgba(124, 92, 255, 0.24) 0%, transparent 62%),
    radial-gradient(760px 460px at 88% 4%, rgba(34, 211, 238, 0.16) 0%, transparent 64%),
    radial-gradient(900px 620px at 50% 108%, rgba(255, 77, 141, 0.12) 0%, transparent 66%),
    linear-gradient(180deg, #05060a 0%, #070910 48%, #05060a 100%);
}

.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(110px);
  will-change: transform;
}

.orb-violet {
  top: -240px;
  left: -180px;
  width: 640px;
  height: 640px;
  background: radial-gradient(circle at 40% 40%, rgba(124, 92, 255, 0.75), transparent 68%);
  animation: orb-drift-a 26s ease-in-out infinite alternate;
}

.orb-cyan {
  top: -160px;
  right: -200px;
  width: 560px;
  height: 560px;
  background: radial-gradient(circle at 55% 45%, rgba(34, 211, 238, 0.55), transparent 68%);
  animation: orb-drift-b 32s ease-in-out infinite alternate;
}

.orb-pink {
  bottom: -320px;
  left: 34%;
  width: 700px;
  height: 700px;
  background: radial-gradient(circle at 50% 50%, rgba(255, 77, 141, 0.34), transparent 70%);
  animation: orb-drift-c 38s ease-in-out infinite alternate;
}

/* 透视网格 */
.ambient-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: radial-gradient(ellipse 90% 60% at 50% 0%, #000 0%, transparent 78%);
  -webkit-mask-image: radial-gradient(ellipse 90% 60% at 50% 0%, #000 0%, transparent 78%);
  opacity: 0.55;
}

/* 噪点 */
.ambient-noise {
  position: absolute;
  inset: 0;
  opacity: 0.035;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='180' height='180' filter='url(%23n)'/%3E%3C/svg%3E");
}

/* 暗角 */
.ambient-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 90% at 50% 40%, transparent 40%, rgba(0, 0, 0, 0.55) 100%);
}

/* 鼠标光斑 */
.ambient-spotlight {
  position: absolute;
  top: 0;
  left: 0;
  width: 640px;
  height: 640px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.16) 0%, rgba(34, 211, 238, 0.05) 42%, transparent 68%);
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

@keyframes orb-drift-a {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(120px, 90px, 0) scale(1.18);
  }
}

@keyframes orb-drift-b {
  from {
    transform: translate3d(0, 0, 0) scale(1.06);
  }
  to {
    transform: translate3d(-140px, 70px, 0) scale(0.92);
  }
}

@keyframes orb-drift-c {
  from {
    transform: translate3d(0, 0, 0) scale(0.94);
  }
  to {
    transform: translate3d(90px, -110px, 0) scale(1.12);
  }
}

@media (max-width: 768px) {
  .ambient-orb {
    filter: blur(80px);
  }

  .ambient-spotlight {
    display: none;
  }

  .ambient-grid {
    background-size: 48px 48px;
  }
}
</style>
