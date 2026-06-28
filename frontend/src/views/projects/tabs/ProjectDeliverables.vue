<template>
  <div class="deliverables">
    <div class="tab-header">
      <h2 class="tab-title">제출된 산출물 목록</h2>
      <button class="btn-primary" disabled>+ 산출물 등록</button>
    </div>

    <div class="table-wrap">
      <div v-if="loading" class="loading-state">불러오는 중...</div>

      <template v-else>
        <table v-if="deliverables.length > 0">
          <thead>
            <tr>
              <th>산출물명</th>
              <th>분류</th>
              <th>버전</th>
              <th>작성자</th>
              <th>최종 업데이트</th>
              <th>첨부</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in deliverables" :key="item.id">
              <td class="name-cell">{{ item.deliverableName }}</td>
              <td><span class="category-tag">{{ item.deliverableType }}</span></td>
              <td class="version-cell">{{ item.versionNo }}</td>
              <td>{{ item.authorName || '-' }}</td>
              <td>{{ formatDate(item.updatedAt) }}</td>
              <td>
                <span v-if="item.attachmentCount > 0">📎 {{ item.attachmentCount }}</span>
                <span v-else class="muted">-</span>
              </td>
              <td>
                <StatusBadge :status="item.status" :label="statusMeta(item.status).label" />
              </td>
              <td>
                <div class="action-btns">
                  <template v-if="item.status === 'DRAFT'">
                    <button class="btn-icon" :disabled="busy" @click="onSubmit(item)">제출</button>
                  </template>
                  <template v-else-if="item.status === 'SUBMITTED'">
                    <button class="btn-icon" :disabled="busy" @click="onStartReview(item)">검토</button>
                    <button class="btn-icon" :disabled="busy" @click="onApprove(item)">승인</button>
                    <button class="btn-icon" :disabled="busy" @click="onReject(item)">반려</button>
                  </template>
                  <template v-else-if="item.status === 'UNDER_REVIEW'">
                    <button class="btn-icon" :disabled="busy" @click="onApprove(item)">승인</button>
                    <button class="btn-icon" :disabled="busy" @click="onReject(item)">반려</button>
                  </template>
                  <template v-else>
                    <span class="muted">—</span>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <EmptyState v-else message="등록된 산출물이 없습니다." />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  listDeliverables,
  submitDeliverable,
  startReview,
  approveDeliverable,
  rejectDeliverable,
  statusMeta,
  type DeliverableDto,
} from '@/api/deliverables'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const deliverables = ref<DeliverableDto[]>([])
const loading = ref(false)
const busy = ref(false)

function formatDate(value: string | null): string {
  if (!value) return '-'
  return value.slice(0, 10)
}

async function load() {
  loading.value = true
  try {
    deliverables.value = await listDeliverables(projectId.value)
  } catch (e) {
    console.error('Failed to load deliverables', e)
    deliverables.value = []
  } finally {
    loading.value = false
  }
}

async function runAction(fn: () => Promise<unknown>) {
  busy.value = true
  try {
    await fn()
    await load()
  } catch (e) {
    console.error('Deliverable action failed', e)
  } finally {
    busy.value = false
  }
}

function onSubmit(item: DeliverableDto) {
  runAction(() => submitDeliverable(item.id))
}

function onStartReview(item: DeliverableDto) {
  runAction(() => startReview(item.id))
}

function onApprove(item: DeliverableDto) {
  runAction(() => approveDeliverable(item.id, ''))
}

function onReject(item: DeliverableDto) {
  const reason = window.prompt('반려 사유')
  if (reason === null) return
  runAction(() => rejectDeliverable(item.id, reason))
}

onMounted(load)
watch(projectId, load)
</script>

<style scoped>
.deliverables { }
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-title { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.table-wrap {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

.loading-state {
  padding: 30px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

table { width: 100%; border-collapse: collapse; }

thead th {
  background: var(--bg-surface2);
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
}

tbody td {
  padding: 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
  font-size: 13px;
}

tbody tr:last-child td { border-bottom: none; }
tbody tr:hover { background: var(--bg-surface2); }

.name-cell { font-weight: 500; }
.version-cell { font-family: monospace; color: var(--text-secondary); }
.muted { color: var(--text-muted); }

.category-tag {
  background: var(--bg-surface2);
  color: var(--text-secondary);
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.action-btns { display: flex; gap: 6px; }
.btn-icon {
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
}
.btn-icon:hover { color: var(--text-primary); }
.btn-icon:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
