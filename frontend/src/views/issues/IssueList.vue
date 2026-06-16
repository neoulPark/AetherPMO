<template>
  <div class="issue-list">
    <div class="page-header">
      <div>
        <h1 class="page-title">이슈 / 리스크 관리</h1>
        <p class="page-sub">전체 프로젝트의 이슈 및 리스크를 관리합니다.</p>
      </div>
      <button class="btn-primary">+ 이슈 등록</button>
    </div>

    <div class="filters">
      <select class="filter-select" v-model="filterType">
        <option value="">전체 유형</option>
        <option value="ISSUE">이슈</option>
        <option value="RISK">리스크</option>
      </select>
      <select class="filter-select" v-model="filterPriority">
        <option value="">전체 우선순위</option>
        <option value="상">상</option>
        <option value="중">중</option>
        <option value="하">하</option>
      </select>
      <select class="filter-select" v-model="filterStatus">
        <option value="">전체 상태</option>
        <option value="OPEN">발생</option>
        <option value="IN_PROGRESS">조치중</option>
        <option value="DONE">완료</option>
      </select>
      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="search" type="text" placeholder="이슈 제목 검색..." />
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num">{{ mockIssues.length }}</div>
        <div class="stat-label">전체</div>
      </div>
      <div class="stat-card red">
        <div class="stat-num">{{ mockIssues.filter(i => i.status === 'OPEN').length }}</div>
        <div class="stat-label">발생</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-num">{{ mockIssues.filter(i => i.status === 'IN_PROGRESS').length }}</div>
        <div class="stat-label">조치중</div>
      </div>
      <div class="stat-card green">
        <div class="stat-num">{{ mockIssues.filter(i => i.status === 'DONE').length }}</div>
        <div class="stat-label">완료</div>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>유형</th>
            <th>프로젝트</th>
            <th>제목</th>
            <th>우선순위</th>
            <th>담당자</th>
            <th>발생일</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="issue in filtered" :key="issue.id">
            <td><StatusBadge :status="issue.type" :label="issue.type === 'ISSUE' ? '이슈' : '리스크'" /></td>
            <td class="project-cell">{{ issue.projectName }}</td>
            <td class="title-cell">{{ issue.title }}</td>
            <td><span class="priority" :class="'p-' + issue.priority">{{ issue.priority }}</span></td>
            <td>{{ issue.assignee }}</td>
            <td>{{ issue.occurredDate }}</td>
            <td><StatusBadge :status="issue.status" :label="issue.statusLabel" /></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="filtered.length === 0" message="조건에 맞는 이슈가 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockIssues } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const filterType = ref('')
const filterPriority = ref('')
const filterStatus = ref('')
const search = ref('')

const filtered = computed(() => {
  let list = mockIssues
  if (filterType.value) list = list.filter(i => i.type === filterType.value)
  if (filterPriority.value) list = list.filter(i => i.priority === filterPriority.value)
  if (filterStatus.value) list = list.filter(i => i.status === filterStatus.value)
  if (search.value) list = list.filter(i => i.title.includes(search.value))
  return list
})
</script>

<style scoped>
.issue-list { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.filters { display: flex; gap: 10px; margin-bottom: 20px; }
.filter-select { background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-primary); border-radius: 6px; padding: 7px 10px; font-size: 13px; outline: none; }
.search-box { display: flex; align-items: center; gap: 8px; background: var(--bg-surface2); border: 1px solid var(--border); border-radius: 6px; padding: 7px 12px; color: var(--text-muted); flex: 1; }
.search-box input { border: none; background: transparent; color: var(--text-primary); font-size: 13px; outline: none; width: 100%; }

.stats-row { display: flex; gap: 12px; margin-bottom: 20px; }
.stat-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 8px; padding: 14px 20px; min-width: 80px; }
.stat-card.red { border-color: rgba(231,76,60,0.3); }
.stat-card.orange { border-color: rgba(243,156,18,0.3); }
.stat-card.green { border-color: rgba(0,184,148,0.3); }
.stat-num { font-size: 24px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 11px; color: var(--text-muted); }
.stat-card.red .stat-num { color: var(--color-danger); }
.stat-card.orange .stat-num { color: var(--color-warning); }
.stat-card.green .stat-num { color: var(--color-success); }

.table-wrap { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
thead th { background: var(--bg-surface2); color: var(--text-secondary); font-size: 12px; text-transform: uppercase; padding: 10px 12px; text-align: left; font-weight: 600; }
tbody td { padding: 12px; border-bottom: 1px solid var(--border); color: var(--text-primary); font-size: 13px; }
tbody tr:last-child td { border-bottom: none; }
tbody tr:hover { background: var(--bg-surface2); }
.project-cell { font-size: 12px; color: var(--text-secondary); }
.title-cell { font-weight: 500; }
.priority { font-size: 12px; font-weight: 700; padding: 2px 8px; border-radius: 4px; }
.p-상 { color: var(--color-danger); background: rgba(231,76,60,0.1); }
.p-중 { color: var(--color-warning); background: rgba(243,156,18,0.1); }
.p-하 { color: var(--color-success); background: rgba(0,184,148,0.1); }
</style>
