<template>
  <div class="progress-track">
    <div class="progress-fill" :style="{ width: `${Math.min(100, Math.max(0, value))}%` }"></div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ value: number }>()
</script>

<style scoped>
.progress-track {
  width: 100%;
  height: 6px;
  background: var(--bg-surface2);
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary), var(--color-cyan));
  border-radius: 3px;
  transition: width 0.3s ease;
}
</style>
