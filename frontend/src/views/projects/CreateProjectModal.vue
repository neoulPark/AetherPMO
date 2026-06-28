<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>새 프로젝트 등록</h2>
        <button class="modal-close" @click="$emit('close')">×</button>
      </div>

      <div class="modal-body">
        <!-- (A) 기본 정보 -->
        <section class="section">
          <h3 class="section-title">기본 정보</h3>
          <div class="form-group">
            <label>프로젝트 이름 *</label>
            <input v-model="form.projectName" type="text" placeholder="프로젝트 이름을 입력하세요" />
          </div>
          <div class="form-group">
            <label>프로젝트 설명</label>
            <textarea v-model="form.description" rows="2" placeholder="프로젝트 개요"></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>담당 부서/팀</label>
              <input v-model="form.team" type="text" placeholder="예) 개발팀" />
            </div>
            <div class="form-group">
              <label>프로젝트 매니저</label>
              <select v-model.number="form.pmId">
                <option :value="1">안유경</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>고객기관</label>
              <select v-model.number="form.clientCompanyId">
                <option :value="1">국립정보자원관리원</option>
                <option :value="2">국민건강보험공단</option>
                <option :value="3">오케스트로</option>
              </select>
            </div>
            <div class="form-group">
              <label>사업금액 (원)</label>
              <input v-model.number="form.contractAmount" type="number" placeholder="0" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>시작일</label>
              <input v-model="form.plannedStartDate" type="date" />
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input v-model="form.plannedEndDate" type="date" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>진행 상태</label>
              <select v-model="form.status">
                <option value="PLANNING">준비</option>
                <option value="IN_PROGRESS">수행 중</option>
                <option value="ON_HOLD">보류</option>
                <option value="COMPLETED">완료</option>
              </select>
            </div>
            <div class="form-group">
              <label>위험도</label>
              <select v-model="form.riskLevel">
                <option value="낮음">낮음</option>
                <option value="보통">보통</option>
                <option value="높음">높음</option>
              </select>
            </div>
          </div>
        </section>

        <!-- (B) 표준 방법론 테일러링 -->
        <section class="section">
          <div class="section-head">
            <h3 class="section-title">표준 방법론 테일러링</h3>
            <button class="toggle-all" @click="toggleAll">
              {{ allSelected ? '전체 해제' : '전체 선택' }}
            </button>
          </div>
          <p class="section-hint">선택한 업무·산출물로 프로젝트 WBS가 생성됩니다.</p>

          <div v-if="catalogLoading" class="loading-state">카탈로그 불러오는 중...</div>
          <div v-else class="tree-area">
            <CatalogTree
              :phases="phases"
              selectable
              :checked-tasks="checkedTasks"
              :checked-deliverables="checkedDeliverables"
              @toggle-task="onToggleTask"
              @toggle-deliverable="onToggleDeliverable"
              @toggle-activity="onToggleActivity"
              @toggle-phase="onTogglePhase"
            />
          </div>
          <div class="select-summary">
            선택: 업무 {{ checkedTasks.size }}개 · 산출물 {{ checkedDeliverables.size }}개
          </div>
        </section>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary" @click="$emit('close')">취소</button>
        <button class="btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '저장 중...' : '저장하기' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getCatalog,
  createProjectWithTailoring,
  type Phase,
  type Activity,
  type TaskTemplate,
  type DeliverableTemplate,
} from '@/api/methodology'
import CatalogTree from '@/components/methodology/CatalogTree.vue'

const emit = defineEmits<{ (e: 'close'): void; (e: 'created'): void }>()
const router = useRouter()

const phases = ref<Phase[]>([])
const catalogLoading = ref(false)
const saving = ref(false)

const checkedTasks = ref<Set<number>>(new Set())
const checkedDeliverables = ref<Set<number>>(new Set())

const form = ref({
  projectName: '',
  description: '',
  team: '',
  pmId: 1,
  clientCompanyId: 1,
  status: 'PLANNING',
  projectStage: 'EXECUTION',
  plannedStartDate: '',
  plannedEndDate: '',
  contractAmount: 0,
  riskLevel: '보통',
})

function reassign() {
  // trigger reactivity on Set mutation
  checkedTasks.value = new Set(checkedTasks.value)
  checkedDeliverables.value = new Set(checkedDeliverables.value)
}

function setTask(t: TaskTemplate, checked: boolean) {
  if (checked) {
    checkedTasks.value.add(t.taskTemplateId)
    t.deliverables.forEach((d) => checkedDeliverables.value.add(d.deliverableTemplateId))
  } else {
    checkedTasks.value.delete(t.taskTemplateId)
    t.deliverables.forEach((d) => checkedDeliverables.value.delete(d.deliverableTemplateId))
  }
}

function onToggleTask(t: TaskTemplate, checked: boolean) {
  setTask(t, checked)
  reassign()
}

function onToggleDeliverable(d: DeliverableTemplate, checked: boolean) {
  if (checked) checkedDeliverables.value.add(d.deliverableTemplateId)
  else checkedDeliverables.value.delete(d.deliverableTemplateId)
  reassign()
}

function onToggleActivity(a: Activity, checked: boolean) {
  a.tasks.forEach((t) => setTask(t, checked))
  reassign()
}

function onTogglePhase(p: Phase, checked: boolean) {
  p.activities.forEach((a) => a.tasks.forEach((t) => setTask(t, checked)))
  reassign()
}

const totalTasks = computed(() =>
  phases.value.reduce((s, p) => s + p.activities.reduce((s2, a) => s2 + a.tasks.length, 0), 0)
)
const allSelected = computed(
  () => totalTasks.value > 0 && checkedTasks.value.size === totalTasks.value
)

function selectAll(checked: boolean) {
  for (const p of phases.value)
    for (const a of p.activities) for (const t of a.tasks) setTask(t, checked)
  reassign()
}

function toggleAll() {
  selectAll(!allSelected.value)
}

async function loadCatalog() {
  catalogLoading.value = true
  try {
    phases.value = await getCatalog()
    selectAll(true) // default: all checked
  } catch (e) {
    console.error('Failed to load catalog', e)
    phases.value = []
  } finally {
    catalogLoading.value = false
  }
}

async function handleSave() {
  if (!form.value.projectName.trim()) {
    alert('프로젝트 이름을 입력하세요.')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form.value,
      plannedStartDate: form.value.plannedStartDate || null,
      plannedEndDate: form.value.plannedEndDate || null,
      contractAmount: form.value.contractAmount || null,
      selectedTaskTemplateIds: Array.from(checkedTasks.value),
      selectedDeliverableTemplateIds: Array.from(checkedDeliverables.value),
    }
    const created = await createProjectWithTailoring(payload)
    emit('created')
    emit('close')
    if (created?.id) router.push(`/projects/${created.id}/tasks`)
  } catch (e) {
    console.error('Failed to create project', e)
    alert('프로젝트 생성에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

onMounted(loadCatalog)
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  width: 640px;
  max-width: 92vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border);
}
.modal-header h2 { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.modal-close { background: none; border: none; color: var(--text-muted); font-size: 20px; cursor: pointer; }

.modal-body { padding: 20px 24px; overflow-y: auto; }

.section { margin-bottom: 24px; }
.section-head { display: flex; justify-content: space-between; align-items: center; }
.section-title { font-size: 13px; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; }
.section-hint { font-size: 12px; color: var(--text-muted); margin: -6px 0 10px; }

.toggle-all {
  background: var(--bg-surface2);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.form-group label { font-size: 12px; color: var(--text-secondary); }
.form-group input, .form-group select, .form-group textarea {
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 8px 10px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}
.form-group textarea { resize: vertical; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.tree-area {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px;
  background: var(--bg-surface2);
}

.loading-state { padding: 24px; text-align: center; color: var(--text-muted); font-size: 13px; }

.select-summary { margin-top: 10px; font-size: 12px; color: var(--text-secondary); }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}

.btn-primary {
  background: var(--color-primary);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn-primary:hover { background: var(--color-primary-hover); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-secondary {
  background: var(--bg-surface2);
  color: var(--text-primary);
  border: 1px solid var(--border);
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
</style>
