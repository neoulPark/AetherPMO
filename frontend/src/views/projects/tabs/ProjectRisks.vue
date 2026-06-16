<template>
  <div class="risks">
    <div class="tab-header">
      <h2 class="tab-title">이슈 / 리스크</h2>
      <button class="btn-primary">+ 이슈 등록</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>유형</th>
            <th>제목</th>
            <th>우선순위</th>
            <th>담당자</th>
            <th>발생일</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="issue in issues" :key="issue.id">
            <td><StatusBadge :status="issue.type" :label="issue.type === 'ISSUE' ? '이슈' : '리스크'" /></td>
            <td class="title-cell">{{ issue.title }}</td>
            <td><span class="priority" :class="'p-' + issue.priority">{{ issue.priority }}</span></td>
            <td>{{ issue.assignee }}</td>
            <td>{{ issue.occurredDate }}</td>
            <td><StatusBadge :status="issue.status" :label="issue.statusLabel" /></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="issues.length === 0" message="등록된 이슈 / 리스크가 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockIssues } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const issues = computed(() => mockIssues.filter(i => i.projectId === projectId.value))
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
.priority { font-size: 12px; font-weight: 700; padding: 2px 8px; border-radius: 4px; }
.p-상 { color: var(--color-danger); background: rgba(231,76,60,0.1); }
.p-중 { color: var(--color-warning); background: rgba(243,156,18,0.1); }
.p-하 { color: var(--color-success); background: rgba(0,184,148,0.1); }
</style>
