<template>
  <div class="methodology">
    <div class="page-header">
      <div>
        <h1 class="page-title">사업관리 표준 (OPMS)</h1>
        <p class="page-sub">공공 SI 사업관리 표준 방법론 — 단계 · 활동 · 업무 · 산출물 카탈로그</p>
      </div>
    </div>

    <div v-if="loading" class="loading-state">불러오는 중...</div>

    <template v-else>
      <div class="summary">{{ summary }}</div>

      <div class="card">
        <CatalogTree :phases="phases" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCatalog, type Phase } from '@/api/methodology'
import CatalogTree from '@/components/methodology/CatalogTree.vue'

const phases = ref<Phase[]>([])
const loading = ref(false)

const summary = computed(() => {
  let acts = 0
  let tasks = 0
  let delivs = 0
  for (const p of phases.value) {
    acts += p.activities.length
    for (const a of p.activities) {
      tasks += a.tasks.length
      for (const t of a.tasks) delivs += t.deliverables.length
    }
  }
  return `${phases.value.length}단계 · ${acts}활동 · ${tasks}업무 · ${delivs}산출물`
})

async function load() {
  loading.value = true
  try {
    phases.value = await getCatalog()
  } catch (e) {
    console.error('Failed to load catalog', e)
    phases.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.methodology { max-width: 1100px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.loading-state { padding: 40px; text-align: center; color: var(--text-muted); font-size: 13px; }

.summary {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 14px;
  padding: 8px 12px;
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  display: inline-block;
}

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}
</style>
