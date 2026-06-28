<template>
  <div class="catalog-tree">
    <div v-for="phase in phases" :key="phase.phaseCode" class="node phase-node">
      <div class="node-row level-0" @click="toggle(phase.phaseCode)">
        <span class="caret">{{ isOpen(phase.phaseCode) ? '▾' : '▸' }}</span>
        <label v-if="selectable" class="cbox" @click.stop>
          <input
            type="checkbox"
            :checked="phaseChecked(phase)"
            :indeterminate.prop="phaseIndeterminate(phase)"
            @change="togglePhase(phase, ($event.target as HTMLInputElement).checked)"
          />
        </label>
        <span class="code">[{{ phase.phaseCode }}]</span>
        <span class="name bold">{{ phase.phaseName }}</span>
        <span class="meta">{{ phase.activities.length }}활동</span>
      </div>

      <div v-if="isOpen(phase.phaseCode)" class="children">
        <div v-for="act in phase.activities" :key="act.activityCode" class="node activity-node">
          <div class="node-row level-1" @click="toggle(act.activityCode)">
            <span class="caret">{{ isOpen(act.activityCode) ? '▾' : '▸' }}</span>
            <label v-if="selectable" class="cbox" @click.stop>
              <input
                type="checkbox"
                :checked="activityChecked(act)"
                :indeterminate.prop="activityIndeterminate(act)"
                @change="toggleActivity(act, ($event.target as HTMLInputElement).checked)"
              />
            </label>
            <span class="code">[{{ act.activityCode }}]</span>
            <span class="name">{{ act.activityName }}</span>
            <span class="meta">{{ act.tasks.length }}업무</span>
          </div>

          <div v-if="isOpen(act.activityCode)" class="children">
            <div v-for="task in act.tasks" :key="task.taskTemplateId" class="node task-node">
              <div class="node-row level-2" @click="toggle('t' + task.taskTemplateId)">
                <span class="caret">{{ isOpen('t' + task.taskTemplateId) ? '▾' : '▸' }}</span>
                <label v-if="selectable" class="cbox" @click.stop>
                  <input
                    type="checkbox"
                    :checked="isTaskChecked(task)"
                    @change="toggleTask(task, ($event.target as HTMLInputElement).checked)"
                  />
                </label>
                <span class="code">[{{ task.taskCode }}]</span>
                <span class="name">{{ task.taskName }}</span>
                <span v-if="task.isOptional" class="badge">선택</span>
                <span class="meta">산출물 {{ task.deliverables.length }}개</span>
              </div>

              <div v-if="isOpen('t' + task.taskTemplateId)" class="children">
                <div
                  v-for="d in task.deliverables"
                  :key="d.deliverableTemplateId"
                  class="node-row level-3 deliverable-row"
                >
                  <span class="caret-spacer"></span>
                  <label v-if="selectable" class="cbox">
                    <input
                      type="checkbox"
                      :checked="isDeliverableChecked(d)"
                      @change="toggleDeliverable(d, ($event.target as HTMLInputElement).checked)"
                    />
                  </label>
                  <span class="dot">·</span>
                  <span class="name deliverable">{{ d.deliverableName }}</span>
                  <span v-if="d.isOptional" class="badge">선택</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Phase, Activity, TaskTemplate, DeliverableTemplate } from '@/api/methodology'

const props = defineProps<{
  phases: Phase[]
  selectable?: boolean
  checkedTasks?: Set<number>
  checkedDeliverables?: Set<number>
}>()

const emit = defineEmits<{
  (e: 'toggle-task', task: TaskTemplate, checked: boolean): void
  (e: 'toggle-deliverable', deliverable: DeliverableTemplate, checked: boolean): void
  (e: 'toggle-activity', activity: Activity, checked: boolean): void
  (e: 'toggle-phase', phase: Phase, checked: boolean): void
}>()

const open = ref<Set<string>>(new Set(props.phases.map((p) => p.phaseCode)))

function isOpen(key: string) {
  return open.value.has(key)
}
function toggle(key: string) {
  const s = new Set(open.value)
  if (s.has(key)) s.delete(key)
  else s.add(key)
  open.value = s
}

function isTaskChecked(t: TaskTemplate) {
  return props.checkedTasks?.has(t.taskTemplateId) ?? false
}
function isDeliverableChecked(d: DeliverableTemplate) {
  return props.checkedDeliverables?.has(d.deliverableTemplateId) ?? false
}

function activityChecked(a: Activity) {
  return a.tasks.length > 0 && a.tasks.every(isTaskChecked)
}
function activityIndeterminate(a: Activity) {
  const some = a.tasks.some(isTaskChecked)
  return some && !activityChecked(a)
}
function phaseChecked(p: Phase) {
  return p.activities.length > 0 && p.activities.every(activityChecked)
}
function phaseIndeterminate(p: Phase) {
  const some = p.activities.some((a) => a.tasks.some(isTaskChecked))
  return some && !phaseChecked(p)
}

function toggleTask(t: TaskTemplate, checked: boolean) {
  emit('toggle-task', t, checked)
}
function toggleDeliverable(d: DeliverableTemplate, checked: boolean) {
  emit('toggle-deliverable', d, checked)
}
function toggleActivity(a: Activity, checked: boolean) {
  emit('toggle-activity', a, checked)
}
function togglePhase(p: Phase, checked: boolean) {
  emit('toggle-phase', p, checked)
}
</script>

<style scoped>
.catalog-tree {
  font-size: 13px;
  color: var(--text-primary);
}

.node-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.node-row:hover {
  background: var(--bg-surface2);
}
.deliverable-row {
  cursor: default;
}
.deliverable-row:hover {
  background: transparent;
}

.level-0 { padding-left: 8px; }
.level-1 { padding-left: 28px; }
.level-2 { padding-left: 48px; }
.level-3 { padding-left: 72px; }

.caret {
  width: 14px;
  color: var(--text-muted);
  font-size: 11px;
  flex-shrink: 0;
}
.caret-spacer {
  width: 14px;
  flex-shrink: 0;
}

.cbox input {
  cursor: pointer;
  accent-color: var(--color-primary);
}

.code {
  color: var(--text-muted);
  font-size: 12px;
  flex-shrink: 0;
}
.name { color: var(--text-primary); }
.name.bold { font-weight: 700; }
.name.deliverable { color: var(--text-secondary); }
.dot { color: var(--text-muted); }

.badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-surface2);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}

.meta {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
}
</style>
