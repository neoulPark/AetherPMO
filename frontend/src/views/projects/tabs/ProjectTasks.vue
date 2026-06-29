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
      <div v-else-if="view === 'tree'" class="tree-wrap">
        <div class="tree-head">
          <span class="col-name">업무명</span>
          <span class="col-status">상태</span>
          <span class="col-dates">일정</span>
          <span class="col-progress">진척률</span>
        </div>
        <el-tree
          class="wbs-tree"
          :data="tree"
          node-key="id"
          :props="{ label: 'taskName', children: 'children' }"
          :expand-on-click-node="false"
          default-expand-all
        >
          <template #default="{ data }">
            <div class="tree-row">
              <span class="col-name">
                <span
                  class="task-link"
                  :class="{ 'is-parent': hasChildren(data) }"
                  @click.stop="openDrawer(data)"
                >
                  {{ data.taskName }}
                </span>
              </span>
              <span class="col-status">
                <StatusBadge :status="data.status" :label="statusLabel(data.status)" />
              </span>
              <span class="col-dates" @click.stop>
                <div class="date-row">
                  <span class="date-tag plan">계획</span>
                  <el-date-picker
                    size="small"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="시작일"
                    :model-value="data.plannedStartDate"
                    :disabled="saving"
                    @update:model-value="(v: string | null) => onDate(data, 'plannedStartDate', v)"
                  />
                  <span class="date-sep">~</span>
                  <el-date-picker
                    size="small"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="종료일"
                    :model-value="data.plannedEndDate"
                    :disabled="saving"
                    @update:model-value="(v: string | null) => onDate(data, 'plannedEndDate', v)"
                  />
                </div>
                <div class="date-row">
                  <span class="date-tag actual">실제</span>
                  <el-date-picker
                    size="small"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="시작일"
                    :model-value="data.actualStartDate"
                    :disabled="saving"
                    @update:model-value="(v: string | null) => onDate(data, 'actualStartDate', v)"
                  />
                  <span class="date-sep">~</span>
                  <el-date-picker
                    size="small"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="종료일"
                    :model-value="data.actualEndDate"
                    :disabled="saving"
                    @update:model-value="(v: string | null) => onDate(data, 'actualEndDate', v)"
                  />
                </div>
              </span>
              <span class="col-progress" @click.stop>
                <div class="progress-cell">
                  <ProgressBar :value="data.progressRate" />
                  <template v-if="hasChildren(data)">
                    <span class="pct auto">{{ data.progressRate }}% <em>(자동)</em></span>
                  </template>
                  <template v-else>
                    <div class="progress-editor">
                      <button class="step" :disabled="data.progressRate <= 0 || saving" @click="bump(data, -10)">−</button>
                      <input
                        class="pct-input"
                        type="number"
                        min="0"
                        max="100"
                        :value="data.progressRate"
                        :disabled="saving"
                        @change="onInput(data, ($event.target as HTMLInputElement).value)"
                      />
                      <span class="pct-suffix">%</span>
                      <button class="step" :disabled="data.progressRate >= 100 || saving" @click="bump(data, 10)">+</button>
                    </div>
                  </template>
                </div>
              </span>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- GANTT VIEW -->
      <div v-else>
        <div v-if="!gantt" class="empty-hint">
          일정이 등록된 업무가 없습니다. 트리에서 시작일/종료일을 입력하세요.
        </div>
        <div v-else class="gantt">
          <!-- left column -->
          <div class="gantt-left">
            <div class="gantt-left-head">
              <span>업무명</span>
              <div class="gantt-legend">
                <span class="legend-item"><i class="legend-swatch planned"></i>계획</span>
                <span class="legend-item"><i class="legend-swatch actual"></i>실제</span>
              </div>
            </div>
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
                <!-- 계획(planned): outlined / translucent baseline bar -->
                <div
                  v-if="row.plannedBar"
                  class="gantt-bar planned"
                  @click="openDrawerById(row.id, !row.hasChildren)"
                  :style="{
                    left: `${row.plannedBar.left}px`,
                    width: `${row.plannedBar.width}px`,
                    borderColor: barColor(row.status),
                  }"
                  :title="`[계획] ${row.taskName} (${row.plannedBar.startLabel} ~ ${row.plannedBar.endLabel})`"
                ></div>

                <!-- 실제(actual): solid status-colored bar with progress fill -->
                <div
                  v-if="row.actualBar"
                  class="gantt-bar actual"
                  @click="openDrawerById(row.id, !row.hasChildren)"
                  :style="{
                    left: `${row.actualBar.left}px`,
                    width: `${row.actualBar.width}px`,
                    background: barColor(row.status),
                  }"
                  :title="`[실제] ${row.taskName} (${row.actualBar.startLabel} ~ ${row.actualBar.endLabel}, ${row.progressRate}%)`"
                >
                  <div
                    class="gantt-bar-fill"
                    :style="{ width: `${row.progressRate}%` }"
                  ></div>
                  <span class="gantt-bar-label">{{ row.progressRate }}%</span>
                </div>

                <!-- progress label on planned bar when no actual yet -->
                <span
                  v-else-if="row.plannedBar"
                  class="gantt-bar-label planned-only"
                  :style="{ left: `${row.plannedBar.left + 6}px` }"
                >{{ row.progressRate }}%</span>
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

// Roll-up range for a node over a given start/end field pair:
// own dates if present, else min/max over descendants.
function rollupRange(
  t: TaskNode,
  startField: 'plannedStartDate' | 'actualStartDate',
  endField: 'plannedEndDate' | 'actualEndDate'
): { start: number | null; end: number | null } {
  let start = parseDate(t[startField])
  let end = parseDate(t[endField])
  if (start !== null && end !== null) return { start, end }
  for (const c of t.children || []) {
    const r = rollupRange(c, startField, endField)
    if (r.start !== null) start = start === null ? r.start : Math.min(start, r.start)
    if (r.end !== null) end = end === null ? r.end : Math.max(end, r.end)
  }
  return { start, end }
}

interface GanttBar {
  left: number
  width: number
  startLabel: string
  endLabel: string
}

interface GanttRow {
  id: number
  taskName: string
  depth: number
  status: string
  progressRate: number
  hasChildren: boolean
  plannedBar: GanttBar | null
  actualBar: GanttBar | null
}

const gantt = computed(() => {
  const plannedRanges = flatTasks.value.map((t) =>
    rollupRange(t, 'plannedStartDate', 'plannedEndDate')
  )
  const actualRanges = flatTasks.value.map((t) =>
    rollupRange(t, 'actualStartDate', 'actualEndDate')
  )
  let min = Infinity
  let max = -Infinity
  for (const r of [...plannedRanges, ...actualRanges]) {
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

  const toBar = (r: { start: number | null; end: number | null }): GanttBar | null => {
    if (r.start === null || r.end === null) return null
    const offset = Math.round((r.start - minDay) / DAY_MS)
    const dur = Math.round((r.end - r.start) / DAY_MS) + 1
    return {
      left: offset * DAY_W,
      width: Math.max(dur * DAY_W, 4),
      startLabel: fmt(r.start),
      endLabel: fmt(r.end),
    }
  }

  const rows: GanttRow[] = flatTasks.value.map((t, i) => {
    return {
      id: t.id,
      taskName: t.taskName,
      depth: t.depth,
      status: t.status,
      progressRate: t.progressRate,
      hasChildren: hasChildren(t),
      plannedBar: toBar(plannedRanges[i]),
      actualBar: toBar(actualRanges[i]),
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

type DateField = 'plannedStartDate' | 'plannedEndDate' | 'actualStartDate' | 'actualEndDate'

async function onDate(t: TaskNode, field: DateField, value: string | null) {
  if ((t[field] || null) === (value || null)) return
  saving.value = true
  try {
    await updateTask(t.id, { [field]: value })
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

/* ---- WBS tree ---- */
.tree-wrap { width: 100%; }

.tree-head {
  display: flex;
  align-items: center;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
}

.tree-row {
  display: flex;
  align-items: center;
  width: 100%;
  font-size: 13px;
  color: var(--text-primary);
  padding: 4px 0;
  gap: 0;
}

/* column widths shared by header + rows */
.col-name { flex: 1 1 auto; min-width: 0; }
.col-status { flex: 0 0 120px; width: 120px; }
.col-dates { flex: 0 0 320px; width: 320px; }
.col-progress { flex: 0 0 260px; width: 260px; }

/* el-tree node content height + alignment for multi-row date cells */
.wbs-tree :deep(.el-tree-node__content) {
  height: auto;
  align-items: center;
  padding-top: 4px;
  padding-bottom: 4px;
}

.is-parent { font-weight: 600; }

.task-link { cursor: pointer; }
.task-link:hover { color: var(--color-primary); text-decoration: underline; }

.date-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.date-row + .date-row { margin-top: 6px; }
.date-row :deep(.el-date-editor) { width: 120px; }
.date-sep { color: var(--text-muted); font-size: 12px; }
.date-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 3px;
  flex: 0 0 auto;
  width: 30px;
  text-align: center;
}
.date-tag.plan {
  color: var(--text-muted);
  border: 1px dashed var(--border);
}
.date-tag.actual {
  color: #fff;
  background: var(--color-primary);
}

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
  justify-content: space-between;
  gap: 10px;
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
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  cursor: pointer;
}
/* 계획(planned): outlined / translucent baseline bar, sits on top */
.gantt-bar.planned {
  top: 4px;
  height: 9px;
  background: transparent;
  border: 1px dashed var(--text-muted);
  opacity: 0.85;
  z-index: 2;
}
/* 실제(actual): solid status-colored bar, sits below planned */
.gantt-bar.actual {
  top: 16px;
  height: 14px;
  z-index: 3;
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
.gantt-bar-label.planned-only {
  position: absolute;
  top: 16px;
  color: var(--text-secondary);
  z-index: 2;
}

.gantt-legend {
  display: flex;
  gap: 12px;
  text-transform: none;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: var(--text-secondary);
  font-weight: 500;
  letter-spacing: 0;
}
.legend-swatch {
  display: inline-block;
  width: 16px;
  height: 9px;
  border-radius: 3px;
}
.legend-swatch.planned {
  background: transparent;
  border: 1px dashed var(--text-muted);
}
.legend-swatch.actual {
  background: var(--color-primary);
}
</style>
