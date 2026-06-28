<template>
  <div class="tasks">
    <div class="card">
      <div class="card-head">
        <h3 class="card-title">업무 (WBS)</h3>
        <div class="view-toggle">
          <button :class="{ active: view === 'tree' }" @click="view = 'tree'">트리</button>
          <button :class="{ active: view === 'gantt' }" @click="view = 'gantt'">간트</button>
        </div>
      </div>

      <div v-if="loading" class="loading-state">불러오는 중...</div>

      <div v-else-if="flatTasks.length === 0">
        <EmptyState message="등록된 업무가 없습니다." />
      </div>

      <!-- TREE VIEW -->
      <table v-else-if="view === 'tree'" class="task-table">
        <thead>
          <tr>
            <th class="col-name">업무명</th>
            <th class="col-status">상태</th>
            <th class="col-dates">일정</th>
            <th class="col-progress">진척률</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in flatTasks" :key="t.id">
            <td class="col-name">
              <span
                class="task-link"
                :style="{ paddingLeft: `${t.depth * 20}px` }"
                :class="{ 'is-parent': hasChildren(t) }"
                @click="openDrawer(t)"
              >
                {{ t.taskName }}
              </span>
            </td>
            <td class="col-status">
              <StatusBadge :status="t.status" :label="statusLabel(t.status)" />
            </td>
            <td class="col-dates">
              <div class="date-cell">
                <el-date-picker
                  size="small"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="시작일"
                  :model-value="t.plannedStartDate"
                  :disabled="saving"
                  @update:model-value="(v: string | null) => onDate(t, 'plannedStartDate', v)"
                />
                <span class="date-sep">~</span>
                <el-date-picker
                  size="small"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="종료일"
                  :model-value="t.plannedEndDate"
                  :disabled="saving"
                  @update:model-value="(v: string | null) => onDate(t, 'plannedEndDate', v)"
                />
              </div>
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

      <!-- GANTT VIEW -->
      <div v-else>
        <div v-if="!gantt" class="empty-hint">
          일정이 등록된 업무가 없습니다. 트리에서 시작일/종료일을 입력하세요.
        </div>
        <div v-else class="gantt">
          <!-- left column -->
          <div class="gantt-left">
            <div class="gantt-left-head">업무명</div>
            <div
              v-for="row in gantt.rows"
              :key="row.id"
              class="gantt-left-row"
            >
              <span
                class="gantt-name task-link"
                :style="{ paddingLeft: `${row.depth * 16}px` }"
                :class="{ 'is-parent': row.hasChildren }"
                :title="row.taskName"
                @click="openDrawerById(row.id, !row.hasChildren)"
              >{{ row.taskName }}</span>
            </div>
          </div>

          <!-- timeline -->
          <div class="gantt-right">
            <div class="gantt-track" :style="{ width: `${gantt.totalWidth}px` }">
              <!-- header: month labels -->
              <div class="gantt-head-row">
                <div
                  v-for="m in gantt.months"
                  :key="m.key"
                  class="gantt-month"
                  :style="{ left: `${m.left}px`, width: `${m.width}px` }"
                >{{ m.label }}</div>
              </div>

              <!-- today line -->
              <div
                v-if="gantt.todayLeft !== null"
                class="gantt-today"
                :style="{ left: `${gantt.todayLeft}px`, height: `${gantt.rows.length * ROW_H + HEAD_H}px` }"
              ></div>

              <!-- task rows -->
              <div
                v-for="row in gantt.rows"
                :key="row.id"
                class="gantt-row"
              >
                <div
                  v-if="row.bar"
                  class="gantt-bar"
                  @click="openDrawerById(row.id, !row.hasChildren)"
                  :style="{
                    left: `${row.bar.left}px`,
                    width: `${row.bar.width}px`,
                    background: barColor(row.status),
                  }"
                  :title="`${row.taskName} (${row.bar.startLabel} ~ ${row.bar.endLabel}, ${row.progressRate}%)`"
                >
                  <div
                    class="gantt-bar-fill"
                    :style="{ width: `${row.progressRate}%` }"
                  ></div>
                  <span class="gantt-bar-label">{{ row.progressRate }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <TaskDetailDrawer
      v-model="drawerOpen"
      :task-id="selectedTaskId"
      :project-id="projectId"
      :is-leaf="selectedIsLeaf"
      @updated="load"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getProjectTasks, updateTaskProgress, updateTask, type TaskNode } from '@/api/projects'
import ProgressBar from '@/components/common/ProgressBar.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import TaskDetailDrawer from '@/components/projects/TaskDetailDrawer.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const tree = ref<TaskNode[]>([])
const loading = ref(false)
const saving = ref(false)
const view = ref<'tree' | 'gantt'>('tree')

const drawerOpen = ref(false)
const selectedTaskId = ref<number | null>(null)
const selectedIsLeaf = ref(true)

function openDrawer(t: TaskNode) {
  openDrawerById(t.id, !hasChildren(t))
}
function openDrawerById(id: number, isLeaf: boolean) {
  selectedTaskId.value = id
  selectedIsLeaf.value = isLeaf
  drawerOpen.value = true
}

const DAY_W = 16
const ROW_H = 34
const HEAD_H = 28
const DAY_MS = 86400000

const STATUS_LABELS: Record<string, string> = {
  TODO: '대기',
  IN_PROGRESS: '수행 중',
  REVIEW: '검토요청',
  DONE: '완료',
}
function statusLabel(s: string) {
  return STATUS_LABELS[s] || s
}

const STATUS_COLORS: Record<string, string> = {
  TODO: '#4a5568',
  대기: '#4a5568',
  IN_PROGRESS: '#0984e3',
  진행중: '#0984e3',
  REVIEW: '#f39c12',
  검토중: '#f39c12',
  DONE: '#00b894',
  완료: '#00b894',
  REJECTED: '#e74c3c',
  반려: '#e74c3c',
}
function barColor(status: string) {
  return STATUS_COLORS[status] || '#6c5ce7'
}

function hasChildren(t: TaskNode) {
  return Array.isArray(t.children) && t.children.length > 0
}

function parseDate(s: string | null): number | null {
  if (!s) return null
  const t = new Date(s + 'T00:00:00').getTime()
  return Number.isNaN(t) ? null : t
}

function fmt(ms: number): string {
  const d = new Date(ms)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
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

// Roll-up range for a node: own dates if present, else min/max over descendants.
function rollupRange(t: TaskNode): { start: number | null; end: number | null } {
  let start = parseDate(t.plannedStartDate)
  let end = parseDate(t.plannedEndDate)
  if (start !== null && end !== null) return { start, end }
  for (const c of t.children || []) {
    const r = rollupRange(c)
    if (r.start !== null) start = start === null ? r.start : Math.min(start, r.start)
    if (r.end !== null) end = end === null ? r.end : Math.max(end, r.end)
  }
  return { start, end }
}

interface GanttRow {
  id: number
  taskName: string
  depth: number
  status: string
  progressRate: number
  hasChildren: boolean
  bar: { left: number; width: number; startLabel: string; endLabel: string } | null
}

const gantt = computed(() => {
  const ranges = flatTasks.value.map((t) => rollupRange(t))
  let min = Infinity
  let max = -Infinity
  for (const r of ranges) {
    if (r.start !== null) min = Math.min(min, r.start)
    if (r.end !== null) max = Math.max(max, r.end)
  }
  if (!Number.isFinite(min) || !Number.isFinite(max)) return null

  // pad to whole months
  const start = new Date(min)
  const minDay = new Date(start.getFullYear(), start.getMonth(), 1).getTime()
  const end = new Date(max)
  const maxDay = new Date(end.getFullYear(), end.getMonth() + 1, 0).getTime()
  const totalDays = Math.round((maxDay - minDay) / DAY_MS) + 1
  const totalWidth = totalDays * DAY_W

  const rows: GanttRow[] = flatTasks.value.map((t, i) => {
    const r = ranges[i]
    let bar: GanttRow['bar'] = null
    if (r.start !== null && r.end !== null) {
      const offset = Math.round((r.start - minDay) / DAY_MS)
      const dur = Math.round((r.end - r.start) / DAY_MS) + 1
      bar = {
        left: offset * DAY_W,
        width: Math.max(dur * DAY_W, 4),
        startLabel: fmt(r.start),
        endLabel: fmt(r.end),
      }
    }
    return {
      id: t.id,
      taskName: t.taskName,
      depth: t.depth,
      status: t.status,
      progressRate: t.progressRate,
      hasChildren: hasChildren(t),
      bar,
    }
  })

  // month columns
  const months: { key: string; label: string; left: number; width: number }[] = []
  let cur = new Date(minDay)
  while (cur.getTime() <= maxDay) {
    const mStart = new Date(cur.getFullYear(), cur.getMonth(), 1).getTime()
    const mEnd = new Date(cur.getFullYear(), cur.getMonth() + 1, 0).getTime()
    const left = Math.round((mStart - minDay) / DAY_MS) * DAY_W
    const days = Math.round((mEnd - mStart) / DAY_MS) + 1
    months.push({
      key: `${cur.getFullYear()}-${cur.getMonth()}`,
      label: `${cur.getFullYear()}.${String(cur.getMonth() + 1).padStart(2, '0')}`,
      left,
      width: days * DAY_W,
    })
    cur = new Date(cur.getFullYear(), cur.getMonth() + 1, 1)
  }

  // today line
  const now = new Date()
  const todayMs = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  let todayLeft: number | null = null
  if (todayMs >= minDay && todayMs <= maxDay) {
    todayLeft = Math.round((todayMs - minDay) / DAY_MS) * DAY_W
  }

  return { rows, months, totalWidth, todayLeft }
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

async function onDate(t: TaskNode, field: 'plannedStartDate' | 'plannedEndDate', value: string | null) {
  if ((t[field] || null) === (value || null)) return
  saving.value = true
  try {
    await updateTask(t.id, {
      plannedStartDate: field === 'plannedStartDate' ? value : t.plannedStartDate,
      plannedEndDate: field === 'plannedEndDate' ? value : t.plannedEndDate,
    })
    await load()
  } catch (e) {
    console.error('Failed to update task dates', e)
  } finally {
    saving.value = false
  }
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

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.view-toggle {
  display: flex;
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
}
.view-toggle button {
  background: var(--bg-surface2);
  color: var(--text-secondary);
  border: none;
  padding: 5px 14px;
  font-size: 12px;
  cursor: pointer;
}
.view-toggle button.active {
  background: var(--color-primary);
  color: #fff;
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
.col-dates { width: 300px; }
.col-progress { width: 260px; }

.is-parent { font-weight: 600; }

.task-link { cursor: pointer; }
.task-link:hover { color: var(--color-primary); text-decoration: underline; }

.date-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.date-cell :deep(.el-date-editor) { width: 130px; }
.date-sep { color: var(--text-muted); font-size: 12px; }

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

/* ---- Gantt ---- */
.empty-hint {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

.gantt {
  display: flex;
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
}

.gantt-left {
  flex: 0 0 260px;
  width: 260px;
  border-right: 1px solid var(--border);
  background: var(--bg-surface2);
}
.gantt-left-head {
  height: 28px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  border-bottom: 1px solid var(--border);
}
.gantt-left-row {
  height: 34px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid var(--border);
}
.gantt-name {
  font-size: 12px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gantt-name.is-parent { font-weight: 600; }

.gantt-right {
  flex: 1;
  overflow-x: auto;
}
.gantt-track {
  position: relative;
}
.gantt-head-row {
  position: relative;
  height: 28px;
  border-bottom: 1px solid var(--border);
}
.gantt-month {
  position: absolute;
  top: 0;
  height: 28px;
  display: flex;
  align-items: center;
  padding-left: 6px;
  font-size: 11px;
  color: var(--text-secondary);
  border-left: 1px solid var(--border);
  box-sizing: border-box;
}
.gantt-today {
  position: absolute;
  top: 0;
  width: 2px;
  background: var(--color-danger);
  opacity: 0.6;
  z-index: 3;
}
.gantt-row {
  position: relative;
  height: 34px;
  border-bottom: 1px solid var(--border);
}
.gantt-bar {
  position: absolute;
  top: 7px;
  height: 20px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  z-index: 2;
  cursor: pointer;
}
.gantt-bar-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: rgba(255, 255, 255, 0.28);
}
.gantt-bar-label {
  position: relative;
  font-size: 10px;
  color: #fff;
  padding: 0 6px;
  white-space: nowrap;
  z-index: 1;
}
</style>
