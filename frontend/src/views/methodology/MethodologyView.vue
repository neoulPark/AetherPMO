<template>
  <div class="methodology">
    <div class="page-header">
      <div>
        <h1 class="page-title">사업관리 표준 (OPMS)</h1>
        <p class="page-sub">공공 SI 사업관리 표준 방법론 — 단계 · 활동 · 업무 · 산출물 카탈로그</p>
      </div>
      <el-button type="primary" @click="openAdd(null, 'PHASE')">+ 단계 추가</el-button>
    </div>

    <div v-if="loading" class="loading-state">불러오는 중...</div>

    <template v-else>
      <div class="summary">{{ summary }}</div>

      <div class="layout">
        <!-- LEFT: catalog tree -->
        <div class="card tree-card">
          <el-tree
            :data="catalog"
            node-key="nodeId"
            :props="{ label: 'name', children: 'children' }"
            :expand-on-click-node="false"
            @node-click="onNodeClick"
          >
            <template #default="{ data }">
              <div class="tree-row">
                <span class="type-badge" :class="'t-' + data.nodeType">{{ typeLabel(data.nodeType) }}</span>
                <span v-if="data.code" class="code">[{{ data.code }}]</span>
                <span class="name">{{ data.name }}</span>
                <span v-if="data.isOptional" class="badge">선택</span>
                <span v-if="data.nodeType === 'TASK' && data.workflowId" class="badge wf-badge">WF</span>
                <span class="actions">
                  <el-button
                    v-if="childTypeOf(data.nodeType)"
                    link
                    size="small"
                    @click.stop="openAdd(data, childTypeOf(data.nodeType)!)"
                  >+ 하위추가</el-button>
                  <el-button link size="small" @click.stop="openEdit(data)">수정</el-button>
                  <el-button link size="small" type="danger" @click.stop="onDelete(data)">삭제</el-button>
                </span>
              </div>
            </template>
          </el-tree>
        </div>

        <!-- RIGHT: workflow panel -->
        <div class="card wf-panel">
          <div class="wf-panel-head">
            <h2 class="wf-title">상태 관리 프로세스</h2>
            <span v-if="selectedTask" class="wf-task-name">
              <span v-if="selectedTask.code" class="code">[{{ selectedTask.code }}]</span>
              {{ selectedTask.name }}
            </span>
          </div>

          <div v-if="!selectedTask" class="wf-hint">
            업무(Task)를 선택하면 상태 관리 프로세스를 볼 수 있습니다.
          </div>

          <template v-else>
            <div class="wf-section">
              <label class="wf-label">워크플로 선택</label>
              <el-select
                v-model="selectedWorkflowId"
                placeholder="워크플로를 선택하세요"
                class="wf-select"
                @change="onAssignWorkflow"
              >
                <el-option
                  v-for="wf in workflows"
                  :key="wf.workflowId"
                  :label="wf.name"
                  :value="wf.workflowId"
                />
              </el-select>
            </div>

            <template v-if="activeWorkflow">
              <div class="wf-section">
                <label class="wf-label">상태</label>
                <div class="chip-row">
                  <span
                    v-for="s in sortedStatuses"
                    :key="s.statusId"
                    class="chip"
                    :style="chipStyle(s.color)"
                  >
                    {{ s.name }}
                    <span v-if="s.isInitial" class="chip-tag">시작</span>
                    <span v-if="s.isFinal" class="chip-tag">완료</span>
                  </span>
                </div>
              </div>

              <div class="wf-section">
                <label class="wf-label">워크플로 다이어그램</label>
                <WorkflowDiagram :workflow="activeWorkflow" />
              </div>

              <div class="wf-section">
                <el-button type="primary" plain @click="openWorkflowEdit">워크플로 편집</el-button>
              </div>
            </template>
            <div v-else class="wf-hint">이 업무에 지정된 워크플로가 없습니다. 위에서 선택하세요.</div>
          </template>
        </div>
      </div>
    </template>

    <!-- Add / Edit node dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px">
      <el-form label-position="top">
        <el-form-item label="이름" required>
          <el-input v-model="formName" placeholder="이름을 입력하세요" />
        </el-form-item>
        <el-form-item label="코드">
          <el-input v-model="formCode" placeholder="코드 (선택)" />
        </el-form-item>
        <el-form-item v-if="showOptional">
          <el-checkbox v-model="formRequired">필수 항목</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">취소</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">저장</el-button>
      </template>
    </el-dialog>

    <!-- Workflow edit dialog -->
    <el-dialog v-model="wfDialogVisible" title="워크플로 편집" width="640px">
      <template v-if="editWorkflow">
        <div class="wf-edit-section">
          <label class="wf-label">미리보기</label>
          <WorkflowDiagram :workflow="editWorkflow" />
        </div>

        <div class="wf-edit-section">
          <label class="wf-label">워크플로 이름</label>
          <div class="inline-row">
            <el-input v-model="wfNameDraft" placeholder="워크플로 이름" />
            <el-button type="primary" plain :loading="wfBusy" @click="saveWorkflowName">이름 저장</el-button>
          </div>
        </div>

        <div class="wf-edit-section">
          <label class="wf-label">상태 목록</label>
          <el-table :data="editWorkflow.statuses" size="small" class="wf-table">
            <el-table-column label="이름" min-width="130">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="색상" width="80">
              <template #default="{ row }">
                <el-color-picker v-model="row.color" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="분류" width="150">
              <template #default="{ row }">
                <el-select v-model="row.category" size="small">
                  <el-option label="TODO" value="TODO" />
                  <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
                  <el-option label="DONE" value="DONE" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="작업" width="120">
              <template #default="{ row }">
                <el-button link size="small" :loading="wfBusy" @click="saveStatus(row)">저장</el-button>
                <el-button link size="small" type="danger" @click="removeStatus(row)">삭제</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="add-status-row">
            <el-input v-model="newStatus.code" size="small" placeholder="코드" style="width:90px" />
            <el-input v-model="newStatus.name" size="small" placeholder="이름" style="width:120px" />
            <el-color-picker v-model="newStatus.color" size="small" />
            <el-select v-model="newStatus.category" size="small" style="width:140px">
              <el-option label="TODO" value="TODO" />
              <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
              <el-option label="DONE" value="DONE" />
            </el-select>
            <el-button size="small" :loading="wfBusy" @click="addNewStatus">+ 상태 추가</el-button>
          </div>
        </div>

        <div class="wf-edit-section">
          <label class="wf-label">전이 목록</label>
          <div class="trans-list">
            <div v-for="t in editWorkflow.transitions" :key="t.transitionId" class="trans-row edit">
              <span class="chip sm" :style="chipStyle(editStatusColor(t.fromStatusId))">{{ editStatusName(t.fromStatusId) }}</span>
              <span class="arrow">→</span>
              <span class="chip sm" :style="chipStyle(editStatusColor(t.toStatusId))">{{ editStatusName(t.toStatusId) }}</span>
              <el-button link size="small" type="danger" @click="removeTransition(t)">삭제</el-button>
            </div>
            <div v-if="!editWorkflow.transitions.length" class="wf-hint sm">등록된 전이가 없습니다.</div>
          </div>
          <div class="add-status-row">
            <el-select v-model="newTransition.fromStatusId" size="small" placeholder="시작 상태" style="width:160px">
              <el-option v-for="s in editWorkflow.statuses" :key="s.statusId" :label="s.name" :value="s.statusId" />
            </el-select>
            <span class="arrow">→</span>
            <el-select v-model="newTransition.toStatusId" size="small" placeholder="도착 상태" style="width:160px">
              <el-option v-for="s in editWorkflow.statuses" :key="s.statusId" :label="s.name" :value="s.statusId" />
            </el-select>
            <el-button size="small" :loading="wfBusy" @click="addNewTransition">+ 전이 추가</el-button>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="wfDialogVisible = false">닫기</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkflowDiagram from '@/components/workflow/WorkflowDiagram.vue'
import {
  getCatalog,
  createNode,
  updateNode,
  deleteNode,
  type CatalogNode,
  type NodeType,
} from '@/api/methodology'
import {
  getWorkflows,
  getWorkflow,
  updateWorkflow,
  addStatus,
  updateStatus,
  deleteStatus,
  addTransition,
  deleteTransition,
  assignNodeWorkflow,
  type Workflow,
  type StatusCategory,
} from '@/api/workflow'

const catalog = ref<CatalogNode[]>([])
const loading = ref(false)

const TYPE_LABELS: Record<NodeType, string> = {
  PHASE: '단계',
  ACTIVITY: '활동',
  TASK: '업무',
  DELIVERABLE: '산출물',
}
function typeLabel(t: NodeType) {
  return TYPE_LABELS[t]
}

const CHILD_TYPE: Record<NodeType, NodeType | null> = {
  PHASE: 'ACTIVITY',
  ACTIVITY: 'TASK',
  TASK: 'DELIVERABLE',
  DELIVERABLE: null,
}
function childTypeOf(t: NodeType): NodeType | null {
  return CHILD_TYPE[t]
}

const summary = computed(() => {
  const counts: Record<NodeType, number> = { PHASE: 0, ACTIVITY: 0, TASK: 0, DELIVERABLE: 0 }
  const walk = (nodes: CatalogNode[]) => {
    for (const n of nodes) {
      counts[n.nodeType] = (counts[n.nodeType] || 0) + 1
      if (n.children?.length) walk(n.children)
    }
  }
  walk(catalog.value)
  return `${counts.PHASE}단계 · ${counts.ACTIVITY}활동 · ${counts.TASK}업무 · ${counts.DELIVERABLE}산출물`
})

async function load() {
  loading.value = true
  try {
    catalog.value = await getCatalog()
  } catch (e) {
    console.error('Failed to load catalog', e)
    catalog.value = []
  } finally {
    loading.value = false
  }
}

/* ------------- Workflow panel ------------- */
const workflows = ref<Workflow[]>([])
let workflowsLoaded = false
const selectedTask = ref<CatalogNode | null>(null)
const selectedWorkflowId = ref<number | null>(null)
const activeWorkflow = ref<Workflow | null>(null)

async function ensureWorkflows() {
  if (workflowsLoaded) return
  try {
    workflows.value = await getWorkflows()
    workflowsLoaded = true
  } catch (e) {
    console.error('Failed to load workflows', e)
  }
}

async function onNodeClick(data: CatalogNode) {
  if (data.nodeType !== 'TASK') {
    selectedTask.value = null
    activeWorkflow.value = null
    return
  }
  selectedTask.value = data
  selectedWorkflowId.value = data.workflowId
  activeWorkflow.value = null
  await ensureWorkflows()
  if (data.workflowId != null) {
    await loadActiveWorkflow(data.workflowId)
  }
}

async function loadActiveWorkflow(id: number) {
  try {
    activeWorkflow.value = await getWorkflow(id)
  } catch (e) {
    console.error('Failed to load workflow', e)
    activeWorkflow.value = null
  }
}

async function onAssignWorkflow(workflowId: number) {
  if (!selectedTask.value) return
  try {
    await assignNodeWorkflow(selectedTask.value.nodeId, workflowId)
    selectedTask.value.workflowId = workflowId
    ElMessage.success('워크플로가 지정되었습니다.')
    await loadActiveWorkflow(workflowId)
  } catch (e) {
    console.error('Assign failed', e)
    ElMessage.error('워크플로 지정에 실패했습니다.')
    selectedWorkflowId.value = selectedTask.value.workflowId
  }
}

const sortedStatuses = computed(() =>
  activeWorkflow.value
    ? [...activeWorkflow.value.statuses].sort((a, b) => a.sortOrder - b.sortOrder)
    : []
)

function chipStyle(color: string) {
  return { background: color, color: readableText(color) }
}
function readableText(hex: string): string {
  const c = hex.replace('#', '')
  if (c.length < 6) return '#fff'
  const r = parseInt(c.slice(0, 2), 16)
  const g = parseInt(c.slice(2, 4), 16)
  const b = parseInt(c.slice(4, 6), 16)
  const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return lum > 0.6 ? '#1a1a1a' : '#ffffff'
}

/* ------------- Workflow edit dialog ------------- */
const wfDialogVisible = ref(false)
const wfBusy = ref(false)
const editWorkflow = ref<Workflow | null>(null)
const wfNameDraft = ref('')
const newStatus = reactive<{ code: string; name: string; color: string; category: StatusCategory }>({
  code: '',
  name: '',
  color: '#0984e3',
  category: 'IN_PROGRESS',
})
const newTransition = reactive<{ fromStatusId: number | null; toStatusId: number | null }>({
  fromStatusId: null,
  toStatusId: null,
})

async function refreshEditWorkflow() {
  if (!editWorkflow.value) return
  const id = editWorkflow.value.workflowId
  editWorkflow.value = await getWorkflow(id)
  if (activeWorkflow.value?.workflowId === id) {
    activeWorkflow.value = editWorkflow.value
  }
  const idx = workflows.value.findIndex((w) => w.workflowId === id)
  if (idx >= 0) workflows.value[idx] = editWorkflow.value
}

async function openWorkflowEdit() {
  if (!activeWorkflow.value) return
  editWorkflow.value = await getWorkflow(activeWorkflow.value.workflowId)
  wfNameDraft.value = editWorkflow.value.name
  wfDialogVisible.value = true
}

function editStatusName(id: number): string {
  return editWorkflow.value?.statuses.find((s) => s.statusId === id)?.name ?? '?'
}
function editStatusColor(id: number): string {
  return editWorkflow.value?.statuses.find((s) => s.statusId === id)?.color ?? '#4a5568'
}

async function saveWorkflowName() {
  if (!editWorkflow.value || !wfNameDraft.value.trim()) return
  wfBusy.value = true
  try {
    await updateWorkflow(editWorkflow.value.workflowId, { name: wfNameDraft.value.trim() })
    await refreshEditWorkflow()
    ElMessage.success('저장되었습니다.')
  } catch (e) {
    console.error(e)
    ElMessage.error('저장에 실패했습니다.')
  } finally {
    wfBusy.value = false
  }
}

async function saveStatus(row: Workflow['statuses'][number]) {
  wfBusy.value = true
  try {
    await updateStatus(row.statusId, {
      name: row.name,
      color: row.color,
      category: row.category,
      isInitial: row.isInitial,
      isFinal: row.isFinal,
      sortOrder: row.sortOrder,
    })
    await refreshEditWorkflow()
    ElMessage.success('상태가 저장되었습니다.')
  } catch (e) {
    console.error(e)
    ElMessage.error('저장에 실패했습니다.')
  } finally {
    wfBusy.value = false
  }
}

async function removeStatus(row: Workflow['statuses'][number]) {
  try {
    await ElMessageBox.confirm(`"${row.name}" 상태를 삭제하시겠습니까?`, '삭제 확인', {
      type: 'warning',
      confirmButtonText: '삭제',
      cancelButtonText: '취소',
    })
  } catch {
    return
  }
  try {
    await deleteStatus(row.statusId)
    await refreshEditWorkflow()
    ElMessage.success('삭제되었습니다.')
  } catch (e) {
    console.error(e)
    ElMessage.error('삭제에 실패했습니다.')
  }
}

async function addNewStatus() {
  if (!editWorkflow.value) return
  if (!newStatus.code.trim() || !newStatus.name.trim()) {
    ElMessage.error('코드와 이름을 입력하세요.')
    return
  }
  wfBusy.value = true
  try {
    await addStatus(editWorkflow.value.workflowId, {
      code: newStatus.code.trim(),
      name: newStatus.name.trim(),
      color: newStatus.color,
      category: newStatus.category,
      sortOrder: editWorkflow.value.statuses.length,
    })
    newStatus.code = ''
    newStatus.name = ''
    await refreshEditWorkflow()
    ElMessage.success('상태가 추가되었습니다.')
  } catch (e) {
    console.error(e)
    ElMessage.error('추가에 실패했습니다.')
  } finally {
    wfBusy.value = false
  }
}

async function removeTransition(t: Workflow['transitions'][number]) {
  try {
    await deleteTransition(t.transitionId)
    await refreshEditWorkflow()
    ElMessage.success('전이가 삭제되었습니다.')
  } catch (e) {
    console.error(e)
    ElMessage.error('삭제에 실패했습니다.')
  }
}

async function addNewTransition() {
  if (!editWorkflow.value) return
  if (newTransition.fromStatusId == null || newTransition.toStatusId == null) {
    ElMessage.error('시작/도착 상태를 선택하세요.')
    return
  }
  wfBusy.value = true
  try {
    await addTransition(editWorkflow.value.workflowId, {
      fromStatusId: newTransition.fromStatusId,
      toStatusId: newTransition.toStatusId,
    })
    newTransition.fromStatusId = null
    newTransition.toStatusId = null
    await refreshEditWorkflow()
    ElMessage.success('전이가 추가되었습니다.')
  } catch (e) {
    console.error(e)
    ElMessage.error('추가에 실패했습니다.')
  } finally {
    wfBusy.value = false
  }
}

/* ------------- Node CRUD dialog ------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const newNodeType = ref<NodeType>('PHASE')
const newParentId = ref<number | null>(null)
const formName = ref('')
const formCode = ref('')
const formRequired = ref(true)

const dialogTitle = computed(() =>
  editingId.value === null
    ? `${typeLabel(newNodeType.value)} 추가`
    : `${typeLabel(newNodeType.value)} 수정`
)
const showOptional = computed(
  () => newNodeType.value === 'TASK' || newNodeType.value === 'DELIVERABLE'
)

function openAdd(parent: CatalogNode | null, nodeType: NodeType) {
  editingId.value = null
  newNodeType.value = nodeType
  newParentId.value = parent ? parent.nodeId : null
  formName.value = ''
  formCode.value = ''
  formRequired.value = true
  dialogVisible.value = true
}

function openEdit(node: CatalogNode) {
  editingId.value = node.nodeId
  newNodeType.value = node.nodeType
  newParentId.value = node.parentNodeId
  formName.value = node.name
  formCode.value = node.code || ''
  formRequired.value = !node.isOptional
  dialogVisible.value = true
}

async function submit() {
  if (!formName.value.trim()) {
    ElMessage.error('이름을 입력하세요.')
    return
  }
  submitting.value = true
  try {
    if (editingId.value === null) {
      await createNode({
        parentNodeId: newParentId.value,
        nodeType: newNodeType.value,
        name: formName.value.trim(),
        code: formCode.value.trim() || null,
        isOptional: showOptional.value ? !formRequired.value : false,
      })
      ElMessage.success('추가되었습니다.')
    } else {
      await updateNode(editingId.value, {
        name: formName.value.trim(),
        code: formCode.value.trim() || null,
        isOptional: showOptional.value ? !formRequired.value : undefined,
      })
      ElMessage.success('수정되었습니다.')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    console.error('Save failed', e)
    ElMessage.error('저장에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

async function onDelete(node: CatalogNode) {
  try {
    await ElMessageBox.confirm(
      `"${node.name}" 및 하위 항목이 모두 삭제됩니다. 계속하시겠습니까?`,
      '삭제 확인',
      { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }
    )
  } catch {
    return
  }
  try {
    await deleteNode(node.nodeId)
    ElMessage.success('삭제되었습니다.')
    if (selectedTask.value?.nodeId === node.nodeId) {
      selectedTask.value = null
      activeWorkflow.value = null
    }
    await load()
  } catch (e) {
    console.error('Delete failed', e)
    ElMessage.error('삭제에 실패했습니다.')
  }
}

onMounted(load)
</script>

<style scoped>
.methodology { max-width: 1200px; }
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

.layout { display: flex; gap: 16px; align-items: flex-start; }
.tree-card { flex: 1 1 60%; min-width: 0; }
.wf-panel { flex: 1 1 40%; min-width: 0; position: sticky; top: 16px; }

@media (max-width: 900px) {
  .layout { flex-direction: column; }
  .tree-card, .wf-panel { width: 100%; flex: 1 1 auto; position: static; }
}

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}

.tree-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  font-size: 13px;
}
.type-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  background: var(--bg-surface2);
  flex-shrink: 0;
}
.type-badge.t-PHASE { color: var(--color-primary); border-color: var(--color-primary); }
.type-badge.t-ACTIVITY { color: var(--color-cyan); border-color: var(--color-cyan); }
.type-badge.t-TASK { color: var(--color-info); }
.type-badge.t-DELIVERABLE { color: var(--text-muted); }
.code { color: var(--text-muted); font-size: 12px; flex-shrink: 0; }
.name { color: var(--text-primary); }
.badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-surface2);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.wf-badge { color: var(--color-success); border-color: var(--color-success); }
.actions {
  margin-left: auto;
  display: none;
  gap: 2px;
  flex-shrink: 0;
}
.tree-row:hover .actions { display: inline-flex; }

/* workflow panel */
.wf-panel-head { display: flex; flex-direction: column; gap: 2px; margin-bottom: 12px; }
.wf-title { font-size: 15px; font-weight: 700; color: var(--text-primary); }
.wf-task-name { font-size: 13px; color: var(--text-secondary); }
.wf-hint { font-size: 13px; color: var(--text-muted); padding: 12px 0; }
.wf-hint.sm { padding: 4px 0; font-size: 12px; }
.wf-section { margin-bottom: 16px; }
.wf-label { display: block; font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; font-weight: 600; }
.wf-select { width: 100%; }

.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
}
.chip.sm { font-size: 11px; padding: 2px 8px; }
.chip-tag { font-size: 9px; opacity: 0.85; border: 1px solid currentColor; border-radius: 4px; padding: 0 3px; }

.trans-list { display: flex; flex-direction: column; gap: 6px; }
.trans-row { display: flex; align-items: center; gap: 8px; }
.arrow { color: var(--text-secondary); font-size: 13px; }

/* edit dialog */
.wf-edit-section { margin-bottom: 18px; }
.inline-row { display: flex; gap: 8px; align-items: center; }
.wf-table { margin-bottom: 10px; }
.add-status-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.trans-row.edit { padding: 2px 0; }
</style>
