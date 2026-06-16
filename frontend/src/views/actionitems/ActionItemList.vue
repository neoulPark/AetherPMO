<template>
  <div class="action-list-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">액션 아이템</h1>
        <p class="page-sub">전체 프로젝트의 액션 아이템을 관리합니다.</p>
      </div>
      <button class="btn-primary">+ 액션 등록</button>
    </div>

    <div class="filters">
      <select class="filter-select" v-model="filterStatus">
        <option value="">전체 상태</option>
        <option value="PENDING">대기</option>
        <option value="IN_PROGRESS">진행중</option>
        <option value="DONE">완료</option>
      </select>
      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="search" type="text" placeholder="액션 아이템 검색..." />
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>프로젝트</th>
            <th>제목</th>
            <th>담당자</th>
            <th>기한</th>
            <th>완료일</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filtered" :key="item.id">
            <td class="project-cell">{{ item.projectName }}</td>
            <td class="title-cell">{{ item.title }}</td>
            <td>{{ item.assignee }}</td>
            <td :class="isOverdue(item.dueDate) && item.status !== 'DONE' ? 'overdue' : ''">{{ item.dueDate }}</td>
            <td>{{ item.completedDate ?? '-' }}</td>
            <td><StatusBadge :status="item.status" :label="item.statusLabel" /></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="filtered.length === 0" message="액션 아이템이 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockActionItems } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const filterStatus = ref('')
const search = ref('')

const filtered = computed(() => {
  let list = mockActionItems
  if (filterStatus.value) list = list.filter(i => i.status === filterStatus.value)
  if (search.value) list = list.filter(i => i.title.includes(search.value))
  return list
})

function isOverdue(date: string) {
  return new Date(date) < new Date('2026-06-15')
}
</script>

<style scoped>
.action-list-view { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.filters { display: flex; gap: 10px; margin-bottom: 20px; }
.filter-select { background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-primary); border-radius: 6px; padding: 7px 10px; font-size: 13px; outline: none; }
.search-box { display: flex; align-items: center; gap: 8px; background: var(--bg-surface2); border: 1px solid var(--border); border-radius: 6px; padding: 7px 12px; color: var(--text-muted); flex: 1; }
.search-box input { border: none; background: transparent; color: var(--text-primary); font-size: 13px; outline: none; width: 100%; }
.table-wrap { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
thead th { background: var(--bg-surface2); color: var(--text-secondary); font-size: 12px; text-transform: uppercase; padding: 10px 12px; text-align: left; font-weight: 600; }
tbody td { padding: 12px; border-bottom: 1px solid var(--border); color: var(--text-primary); font-size: 13px; }
tbody tr:last-child td { border-bottom: none; }
tbody tr:hover { background: var(--bg-surface2); }
.project-cell { font-size: 12px; color: var(--text-secondary); }
.title-cell { font-weight: 500; }
.overdue { color: var(--color-danger); }
</style>
