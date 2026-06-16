<template>
  <div class="meeting-list-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">회의 관리</h1>
        <p class="page-sub">전체 프로젝트의 회의록을 관리합니다.</p>
      </div>
      <button class="btn-primary">+ 회의 등록</button>
    </div>

    <div class="filters">
      <select class="filter-select" v-model="filterProject">
        <option value="">전체 프로젝트</option>
        <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>
      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="search" type="text" placeholder="회의명 검색..." />
      </div>
    </div>

    <div class="meetings-grid">
      <div v-for="m in filtered" :key="m.id" class="meeting-card">
        <div class="card-top">
          <span class="meeting-type">{{ m.type }}</span>
          <span class="meeting-status" :class="m.status === 'COMPLETED' ? 'done' : 'upcoming'">
            {{ m.status === 'COMPLETED' ? '완료' : '예정' }}
          </span>
        </div>
        <h3 class="meeting-title">{{ m.title }}</h3>
        <div class="meeting-info">
          <div class="info-row">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            <span>{{ m.dateTime }}</span>
          </div>
          <div class="info-row">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
            </svg>
            <span>{{ m.location }}</span>
          </div>
          <div class="info-row attendees">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            <span>{{ m.attendees }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span class="project-label">{{ m.projectName }}</span>
          <button class="btn-view">회의록 보기</button>
        </div>
      </div>
    </div>

    <EmptyState v-if="filtered.length === 0" message="회의 데이터가 없습니다." />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockMeetings, mockProjects } from '@/stores/mock'
import EmptyState from '@/components/common/EmptyState.vue'

const filterProject = ref<number | ''>('')
const search = ref('')

const projects = mockProjects

const filtered = computed(() => {
  let list = mockMeetings
  if (filterProject.value !== '') list = list.filter(m => m.projectId === filterProject.value)
  if (search.value) list = list.filter(m => m.title.includes(search.value))
  return list
})
</script>

<style scoped>
.meeting-list-view { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.filters { display: flex; gap: 10px; margin-bottom: 20px; }
.filter-select { background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-primary); border-radius: 6px; padding: 7px 10px; font-size: 13px; outline: none; min-width: 160px; }
.search-box { display: flex; align-items: center; gap: 8px; background: var(--bg-surface2); border: 1px solid var(--border); border-radius: 6px; padding: 7px 12px; color: var(--text-muted); flex: 1; }
.search-box input { border: none; background: transparent; color: var(--text-primary); font-size: 13px; outline: none; width: 100%; }

.meetings-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }

.meeting-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; padding: 20px; transition: border-color 0.15s; }
.meeting-card:hover { border-color: var(--color-primary); }

.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }

.meeting-type { font-size: 11px; background: var(--bg-surface2); color: var(--text-secondary); padding: 2px 8px; border-radius: 4px; }
.meeting-status { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.meeting-status.done { background: rgba(0,184,148,0.15); color: var(--color-success); }
.meeting-status.upcoming { background: rgba(108,92,231,0.15); color: #a29bfe; }

.meeting-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 12px; line-height: 1.4; }

.meeting-info { display: flex; flex-direction: column; gap: 7px; margin-bottom: 14px; }
.info-row { display: flex; align-items: flex-start; gap: 8px; font-size: 12px; color: var(--text-secondary); }
.info-row svg { flex-shrink: 0; margin-top: 1px; color: var(--text-muted); }
.attendees span { line-height: 1.4; }

.card-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid var(--border); }
.project-label { font-size: 11px; color: var(--text-muted); }
.btn-view { background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-secondary); padding: 4px 10px; border-radius: 4px; cursor: pointer; font-size: 11px; }
.btn-view:hover { color: var(--text-primary); }
</style>
