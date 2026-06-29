<template>
  <el-drawer
    :model-value="modelValue"
    direction="rtl"
    size="480px"
    :with-header="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @open="onOpen"
  >
    <div class="drawer-body">
      <div class="drawer-head">
        <div class="head-main">
          <h3 class="task-name">{{ task?.taskName || '업무 상세' }}</h3>
          <StatusBadge
            v-if="task"
            :status="task.status"
            :label="statusLabel(task.status)"
          />
        </div>
        <button class="close-btn" @click="close">✕</button>
      </div>

      <div v-if="loading" class="loading">불러오는 중...</div>

      <div v-else-if="task" class="sections">
        <!-- 상태 변경 -->
        <section class="sec">
          <h4 class="sec-title">상태 변경</h4>
          <div class="status-current">
            <span class="status-current-label">현재:</span>
            <StatusBadge :status="task.status" :label="statusLabel(task.status)" />
          </div>
          <!-- fallback: all statuses as buttons when transitions can't be resolved -->
          <div v-if="!transitionsResolved" class="status-row">
            <button
              v-for="s in statusButtons"
              :key="s.enum"
              class="status-btn"
              :class="{ active: task.status === s.enum }"
              :style="statusBtnStyle(s)"
              :disabled="saving"
              @click="changeStatus(s.enum)"
            >
              {{ s.label }}
            </button>
          </div>
          <!-- transition-limited select -->
          <el-select
            v-else
            class="status-select"
            placeholder="변경할 상태 선택"
            :disabled="saving || allowedNextStatuses.length === 0"
            :model-value="undefined"
            @update:model-value="(v: string) => changeStatus(v)"
          >
            <el-option
              v-for="s in allowedNextStatuses"
              :key="s.enum"
              :value="s.enum"
              :label="s.label"
            >
              <span class="opt-row">
                <span class="opt-dot" :style="{ background: s.color }"></span>
                {{ s.label }}
              </span>
            </el-option>
          </el-select>
          <div v-if="transitionsResolved && allowedNextStatuses.length === 0" class="status-empty">
            이동 가능한 다음 상태가 없습니다.
          </div>
        </section>

        <!-- 진척률 -->
        <section class="sec">
          <h4 class="sec-title">진척률</h4>
          <div v-if="isLeaf" class="progress-edit">
            <el-slider
              v-model="progressDraft"
              :max="100"
              :step="5"
              :disabled="saving"
              @change="saveProgress"
            />
            <span class="progress-val">{{ progressDraft }}%</span>
          </div>
          <div v-else class="progress-auto">
            <el-progress :percentage="task.progressRate" :stroke-width="14" />
            <span class="auto-tag">{{ task.progressRate }}% (자동)</span>
          </div>
        </section>

        <!-- 담당자 / 일정 -->
        <section class="sec">
          <h4 class="sec-title">담당자 / 일정</h4>
          <div class="field">
            <label>담당자 ID</label>
            <el-input-number
              v-model="assigneeDraft"
              :min="0"
              :controls="false"
              size="small"
              :disabled="saving"
              @change="saveAssignee"
            />
          </div>
          <div class="field">
            <label>계획 시작</label>
            <el-date-picker
              v-model="startDraft"
              type="date"
              value-format="YYYY-MM-DD"
              size="small"
              placeholder="계획 시작일"
              :disabled="saving"
              @change="saveDates"
            />
          </div>
          <div class="field">
            <label>계획 종료</label>
            <el-date-picker
              v-model="endDraft"
              type="date"
              value-format="YYYY-MM-DD"
              size="small"
              placeholder="계획 종료일"
              :disabled="saving"
              @change="saveDates"
            />
          </div>
          <div class="field">
            <label>실제 시작</label>
            <el-date-picker
              v-model="actualStartDraft"
              type="date"
              value-format="YYYY-MM-DD"
              size="small"
              placeholder="실제 시작일"
              :disabled="saving"
              @change="saveActualDates"
            />
          </div>
          <div class="field">
            <label>실제 종료</label>
            <el-date-picker
              v-model="actualEndDraft"
              type="date"
              value-format="YYYY-MM-DD"
              size="small"
              placeholder="실제 종료일"
              :disabled="saving"
              @change="saveActualDates"
            />
          </div>
          <div class="field">
            <label>계획 공수</label>
            <el-input-number
              v-model="effortDraft"
              :min="0"
              :controls="false"
              size="small"
              :disabled="saving"
              @change="saveEffort"
            />
          </div>
        </section>

        <!-- 산출물 -->
        <section class="sec">
          <h4 class="sec-title">산출물 (파일)</h4>

          <div class="sub">
            <div class="sub-title">표준 양식</div>
            <div v-if="templates.length === 0" class="empty-line">표준 양식이 없습니다.</div>
            <div v-for="tpl in templates" :key="tpl.seqNo" class="row-item">
              <span class="row-name">{{ tpl.name }}</span>
              <el-button size="small" @click="downloadTemplate(tpl)">다운로드</el-button>
            </div>
          </div>

          <div class="sub">
            <div class="sub-title">
              제출 산출물
              <el-button size="small" type="primary" plain @click="addDeliverable">
                + 산출물 추가
              </el-button>
            </div>
            <div v-if="deliverables.length === 0" class="empty-line">제출 산출물이 없습니다.</div>
            <div v-for="d in deliverables" :key="d.id" class="deliverable">
              <div class="deliverable-head">
                <span class="row-name">{{ d.deliverableName }}</span>
                <span class="ver">v{{ d.versionNo }}</span>
                <StatusBadge :status="d.status" :label="deliverableLabel(d.status)" />
              </div>
              <div class="deliverable-actions">
                <el-button size="small" @click="startUpload(d.id)">업로드</el-button>
                <el-button size="small" @click="downloadAttachments(d.id)">
                  다운로드 ({{ d.attachmentCount }})
                </el-button>
              </div>
              <div v-if="uploadFor === d.id" class="upload-form">
                <el-input v-model="uploadName" size="small" placeholder="파일명" />
                <el-button size="small" type="primary" @click="doUpload(d.id)">업로드</el-button>
                <el-button size="small" @click="uploadFor = null">취소</el-button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import StatusBadge from '@/components/common/StatusBadge.vue'
import {
  getTask,
  getTaskWorkflow,
  updateTask,
  updateTaskProgress,
  getTaskDeliverables,
  createTaskDeliverable,
  getTemplateDeliverables,
  getAttachments,
  uploadAttachment,
  type TaskDto,
  type TemplateDeliverable,
} from '@/api/tasks'
import type { Workflow } from '@/api/workflow'
import {
  type DeliverableDto,
  statusMeta as deliverableStatusMeta,
} from '@/api/deliverables'

const props = defineProps<{
  modelValue: boolean
  taskId: number | null
  projectId: number
  isLeaf?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'updated'): void
}>()

const loading = ref(false)
const saving = ref(false)
const task = ref<TaskDto | null>(null)
const workflow = ref<Workflow | null>(null)
const deliverables = ref<DeliverableDto[]>([])
const templates = ref<TemplateDeliverable[]>([])

const progressDraft = ref(0)
const assigneeDraft = ref<number | undefined>(undefined)
const startDraft = ref<string | null>(null)
const endDraft = ref<string | null>(null)
const actualStartDraft = ref<string | null>(null)
const actualEndDraft = ref<string | null>(null)
const effortDraft = ref<number | undefined>(undefined)

const uploadFor = ref<number | null>(null)
const uploadName = ref('')

const isLeaf = computed(() => props.isLeaf !== false)

// name(workflow) <-> task enum mapping
const STATUS_DEFS: { enum: string; label: string }[] = [
  { enum: 'TODO', label: '대기' },
  { enum: 'IN_PROGRESS', label: '진행중' },
  { enum: 'REVIEW', label: '검토중' },
  { enum: 'DONE', label: '완료' },
  { enum: 'REJECTED', label: '반려' },
]
const FALLBACK_COLORS: Record<string, string> = {
  TODO: '#4a5568',
  IN_PROGRESS: '#0984e3',
  REVIEW: '#f39c12',
  DONE: '#00b894',
  REJECTED: '#e74c3c',
}

function statusLabel(enumVal: string) {
  return STATUS_DEFS.find((s) => s.enum === enumVal)?.label || enumVal
}
function deliverableLabel(status: string) {
  return deliverableStatusMeta(status).label
}

const statusButtons = computed(() =>
  STATUS_DEFS.map((s) => {
    const wf = workflow.value?.statuses.find((ws) => ws.name === s.label)
    return { ...s, color: wf?.color || FALLBACK_COLORS[s.enum] }
  })
)

function statusBtnStyle(s: { enum: string; color?: string }) {
  if (task.value?.status === s.enum) {
    return { background: s.color, borderColor: s.color, color: '#fff' }
  }
  return { borderColor: s.color, color: s.color }
}

// Map a workflow status to the task enum: by name first, then by category.
const NAME_TO_ENUM: Record<string, string> = {
  대기: 'TODO',
  진행중: 'IN_PROGRESS',
  검토중: 'REVIEW',
  완료: 'DONE',
  반려: 'REJECTED',
}
const CATEGORY_TO_ENUM: Record<string, string> = {
  TODO: 'TODO',
  IN_PROGRESS: 'IN_PROGRESS',
  DONE: 'DONE',
}
function wfStatusToEnum(ws: { name: string; category: string }): string | null {
  return NAME_TO_ENUM[ws.name] ?? CATEGORY_TO_ENUM[ws.category] ?? null
}

// The workflow status object matching the task's current enum status.
const currentWfStatus = computed(() => {
  const wf = workflow.value
  if (!wf || !task.value) return null
  return wf.statuses.find((ws) => wfStatusToEnum(ws) === task.value!.status) ?? null
})

// Allowed next statuses, derived from outgoing transitions of the current status.
const allowedNextStatuses = computed(() => {
  const wf = workflow.value
  const cur = currentWfStatus.value
  if (!wf || !cur) return []
  const toIds = wf.transitions
    .filter((tr) => tr.fromStatusId === cur.statusId)
    .map((tr) => tr.toStatusId)
  const seen = new Set<string>()
  const out: { enum: string; label: string; color: string }[] = []
  for (const id of toIds) {
    const ws = wf.statuses.find((s) => s.statusId === id)
    if (!ws) continue
    const enumVal = wfStatusToEnum(ws)
    if (!enumVal || seen.has(enumVal)) continue
    seen.add(enumVal)
    out.push({ enum: enumVal, label: statusLabel(enumVal), color: ws.color })
  }
  return out
})

// Whether we could resolve the workflow + current status to limit transitions.
const transitionsResolved = computed(
  () => !!workflow.value && !!currentWfStatus.value
)

async function onOpen() {
  if (!props.taskId) return
  loading.value = true
  uploadFor.value = null
  try {
    const [t, wf, dels, tpls] = await Promise.all([
      getTask(props.taskId),
      getTaskWorkflow(props.taskId).catch(() => null),
      getTaskDeliverables(props.taskId).catch(() => []),
      getTemplateDeliverables(props.taskId).catch(() => []),
    ])
    task.value = t
    workflow.value = wf
    deliverables.value = dels
    templates.value = tpls
    progressDraft.value = t.progressRate
    assigneeDraft.value = t.assigneeId ?? undefined
    startDraft.value = t.plannedStartDate
    endDraft.value = t.plannedEndDate
    actualStartDraft.value = t.actualStartDate
    actualEndDraft.value = t.actualEndDate
    effortDraft.value = t.plannedEffort ?? undefined
  } catch (e) {
    console.error('Failed to load task detail', e)
    ElMessage.error('업무 정보를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}

async function changeStatus(enumVal: string) {
  if (!props.taskId || !task.value || task.value.status === enumVal) return
  saving.value = true
  try {
    await updateTask(props.taskId, { status: enumVal })
    task.value.status = enumVal
    ElMessage.success('상태가 변경되었습니다.')
    emit('updated')
  } catch (e) {
    console.error(e)
    ElMessage.error('상태 변경에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function saveProgress(val: number) {
  if (!props.taskId) return
  saving.value = true
  try {
    await updateTaskProgress(props.taskId, val)
    ElMessage.success('진척률이 저장되었습니다.')
    emit('updated')
  } catch (e) {
    console.error(e)
    ElMessage.error('진척률 저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function patch(body: Record<string, unknown>, msg: string) {
  if (!props.taskId) return
  saving.value = true
  try {
    await updateTask(props.taskId, body)
    ElMessage.success(msg)
    emit('updated')
  } catch (e) {
    console.error(e)
    ElMessage.error('저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

function saveAssignee() {
  patch({ assigneeId: assigneeDraft.value ?? null }, '담당자가 저장되었습니다.')
}
function saveDates() {
  patch(
    { plannedStartDate: startDraft.value, plannedEndDate: endDraft.value },
    '일정이 저장되었습니다.'
  )
}
function saveActualDates() {
  patch(
    { actualStartDate: actualStartDraft.value, actualEndDate: actualEndDraft.value },
    '실제 일정이 저장되었습니다.'
  )
}
function saveEffort() {
  patch({ plannedEffort: effortDraft.value ?? null }, '공수가 저장되었습니다.')
}

function downloadTemplate(tpl: TemplateDeliverable) {
  ElMessage.info(`표준 양식 다운로드 (mock): ${tpl.name}`)
}

async function downloadAttachments(deliverableId: number) {
  try {
    const atts = await getAttachments(deliverableId)
    if (atts.length === 0) {
      ElMessage.warning('첨부 파일이 없습니다.')
      return
    }
    ElMessage.info(`다운로드 (mock): ${atts.map((a) => a.fileName).join(', ')}`)
  } catch (e) {
    console.error(e)
    ElMessage.error('첨부를 불러오지 못했습니다.')
  }
}

function startUpload(deliverableId: number) {
  uploadFor.value = deliverableId
  uploadName.value = ''
}

async function doUpload(deliverableId: number) {
  const name = uploadName.value.trim()
  if (!name) {
    ElMessage.warning('파일명을 입력하세요.')
    return
  }
  try {
    await uploadAttachment(deliverableId, { fileName: name })
    ElMessage.success(`업로드 완료 (mock): ${name}`)
    uploadFor.value = null
    if (props.taskId) deliverables.value = await getTaskDeliverables(props.taskId)
    emit('updated')
  } catch (e) {
    console.error(e)
    ElMessage.error('업로드에 실패했습니다.')
  }
}

async function addDeliverable() {
  if (!props.taskId) return
  const name = `산출물 ${deliverables.value.length + 1}`
  try {
    await createTaskDeliverable(props.taskId, { deliverableName: name })
    deliverables.value = await getTaskDeliverables(props.taskId)
    ElMessage.success('산출물이 추가되었습니다.')
    emit('updated')
  } catch (e) {
    console.error(e)
    ElMessage.error('산출물 추가에 실패했습니다.')
  }
}
</script>

<style scoped>
.drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-surface);
  color: var(--text-primary);
}
.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
}
.head-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.task-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.close-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 16px;
  cursor: pointer;
}
.loading {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
}
.sections {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
}
.sec {
  margin-bottom: 26px;
}
.sec-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}
.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.status-btn {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid;
  background: transparent;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.status-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.status-current {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.status-current-label {
  font-size: 12px;
  color: var(--text-muted);
}
.status-select {
  width: 100%;
}
.status-empty {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}
.opt-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.opt-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.progress-edit {
  display: flex;
  align-items: center;
  gap: 14px;
}
.progress-edit :deep(.el-slider) {
  flex: 1;
}
.progress-val {
  font-size: 13px;
  color: var(--text-secondary);
  min-width: 42px;
  text-align: right;
}
.progress-auto {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.auto-tag {
  font-size: 12px;
  color: var(--text-muted);
}
.field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.field label {
  width: 80px;
  font-size: 12px;
  color: var(--text-muted);
}
.sub {
  margin-bottom: 16px;
}
.sub-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.empty-line {
  font-size: 12px;
  color: var(--text-muted);
  padding: 4px 0;
}
.row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
}
.row-name {
  font-size: 13px;
  color: var(--text-primary);
}
.deliverable {
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.deliverable-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.ver {
  font-size: 11px;
  color: var(--text-muted);
}
.deliverable-actions {
  display: flex;
  gap: 8px;
}
.upload-form {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
</style>
