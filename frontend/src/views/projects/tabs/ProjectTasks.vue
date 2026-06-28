<template>
  <div class="tasks">
    <div class="card">
      <h3 class="card-title">업무 (WBS)</h3>

      <div v-if="loading" class="loading-state">불러오는 중...</div>

      <div v-else-if="flatTasks.length === 0">
        <EmptyState message="등록된 업무가 없습니다." />
      </div>

      <table v-else class="task-table">
        <thead>
          <tr>
            <th class="col-name">업무명</th>
            <th class="col-status">상태</th>
            <th class="col-progress">진척률</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in flatTasks" :key="t.id">
            <td class="col-name">
              <span :style="{ paddingLeft: `${t.depth * 20}px` }" :class="{ 'is-parent': hasChildren(t) }">
                {{ t.taskName }}
              </span>
            </td>
            <td class="col-status">
              <StatusBadge :status="t.status" :label="statusLabel(t.status)" />
            </td>
            <td class="col-progress">
              <div class="progress-cell">
                <ProgressBar :value="t.progressRate" />
                <template v-if="hasChildren(t)">
                  <span class="pct auto">{{ t.progressRate }}% <em>(자동)</em></span>
                </template>
                <template v-else>
                  <div class="progress-editor">
                    <button class="step" :disabled="t.progressRate <= 0 || saving" @click="bump(t, -10)">−</button>
                    <input
                      class="pct-input"
                      type="number"
                      min="0"
                      max="100"
                      :value="t.progressRate"
                      :disabled="saving"
                      @change="onInput(t, ($event.target as HTMLInputElement).value)"
                    />
                    <span class="pct-suffix">%</span>
                    <button class="step" :disabled="t.progressRate >= 100 || saving" @click="bump(t, 10)">+</button>
                  </div>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getProjectTasks, updateTaskProgress, type TaskNode } from '@/api/projects'
import ProgressBar from '@/components/common/ProgressBar.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const tree = ref<TaskNode[]>([])
const loading = ref(false)
const saving = ref(false)

const STATUS_LABELS: Record<string, string> = {
  TODO: '대기',
  IN_PROGRESS: '수행 중',
  REVIEW: '검토요청',
  DONE: '완료',
}
function statusLabel(s: string) {
  return STATUS_LABELS[s] || s
}

function hasChildren(t: TaskNode) {
  return Array.isArray(t.children) && t.children.length > 0
}

const flatTasks = computed(() => {
  const out: TaskNode[] = []
  const walk = (nodes: TaskNode[]) => {
    for (const n of nodes) {
      out.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(tree.value)
  return out
})

async function load() {
  loading.value = true
  try {
    tree.value = await getProjectTasks(projectId.value)
  } catch (e) {
    console.error('Failed to load tasks', e)
    tree.value = []
  } finally {
    loading.value = false
  }
}

async function save(taskId: number, progressRate: number) {
  const clamped = Math.min(100, Math.max(0, Math.round(progressRate)))
  saving.value = true
  try {
    await updateTaskProgress(taskId, clamped)
    await load()
  } catch (e) {
    console.error('Failed to update progress', e)
  } finally {
    saving.value = false
  }
}

function bump(t: TaskNode, delta: number) {
  save(t.id, t.progressRate + delta)
}

function onInput(t: TaskNode, value: string) {
  const n = Number(value)
  if (!Number.isNaN(n)) save(t.id, n)
}

onMounted(load)
watch(projectId, load)
</script>

<style scoped>
.card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 18px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.loading-state {
  padding: 30px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

.task-table {
  width: 100%;
  border-collapse: collapse;
}

.task-table th {
  text-align: left;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
  text-transform: uppercase;
}

.task-table td {
  padding: 10px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-primary);
  vertical-align: middle;
}

.col-status { width: 120px; }
.col-progress { width: 260px; }

.is-parent { font-weight: 600; }

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-cell :deep(.progress-track) { flex: 1; }

.pct {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  min-width: 80px;
  text-align: right;
}
.pct.auto em { color: var(--text-muted); font-style: normal; }

.progress-editor {
  display: flex;
  align-items: center;
  gap: 4px;
}

.step {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--bg-surface2);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
}
.step:disabled { opacity: 0.4; cursor: not-allowed; }

.pct-input {
  width: 48px;
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 12px;
  padding: 3px 5px;
  text-align: center;
  outline: none;
}

.pct-suffix { font-size: 12px; color: var(--text-muted); }
</style>
