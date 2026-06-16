<template>
  <div class="deliverables">
    <div class="tab-header">
      <h2 class="tab-title">산출물 관리</h2>
      <button class="btn-primary">+ 산출물 등록</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>산출물명</th>
            <th>구분</th>
            <th>버전</th>
            <th>작성자</th>
            <th>최종 수정일</th>
            <th>상태</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in deliverables" :key="item.id">
            <td class="name-cell">{{ item.name }}</td>
            <td><span class="category-tag">{{ item.category }}</span></td>
            <td class="version-cell">{{ item.version }}</td>
            <td>{{ item.author }}</td>
            <td>{{ item.updatedAt }}</td>
            <td><StatusBadge :status="item.status" :label="item.statusLabel" /></td>
            <td>
              <div class="action-btns">
                <button class="btn-icon">다운로드</button>
                <button class="btn-icon">검토 요청</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="deliverables.length === 0" message="등록된 산출물이 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockDeliverables } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const deliverables = computed(() => mockDeliverables.filter(d => d.projectId === projectId.value))
</script>

<style scoped>
.deliverables { }
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-title { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }

.table-wrap {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
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
</style>
