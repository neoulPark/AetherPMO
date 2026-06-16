<template>
  <div class="action-items">
    <div class="tab-header">
      <h2 class="tab-title">액션 아이템</h2>
      <button class="btn-primary">+ 액션 등록</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>제목</th>
            <th>담당자</th>
            <th>기한</th>
            <th>완료일</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td class="title-cell">{{ item.title }}</td>
            <td>{{ item.assignee }}</td>
            <td :class="isOverdue(item.dueDate) && item.status !== 'DONE' ? 'overdue' : ''">{{ item.dueDate }}</td>
            <td>{{ item.completedDate ?? '-' }}</td>
            <td><StatusBadge :status="item.status" :label="item.statusLabel" /></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="items.length === 0" message="등록된 액션 아이템이 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockActionItems } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const items = computed(() => mockActionItems.filter(a => a.projectId === projectId.value))

function isOverdue(date: string) {
  return new Date(date) < new Date('2026-06-15')
}
</script>

<style scoped>
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tab-title { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.table-wrap { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
thead th { background: var(--bg-surface2); color: var(--text-secondary); font-size: 12px; text-transform: uppercase; padding: 10px 12px; text-align: left; font-weight: 600; }
tbody td { padding: 12px; border-bottom: 1px solid var(--border); color: var(--text-primary); font-size: 13px; }
tbody tr:last-child td { border-bottom: none; }
tbody tr:hover { background: var(--bg-surface2); }
.title-cell { font-weight: 500; }
.overdue { color: var(--color-danger); }
</style>
