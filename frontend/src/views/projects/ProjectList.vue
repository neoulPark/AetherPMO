<template>
  <div class="project-list">
    <div class="page-header">
      <div>
        <h1 class="page-title">프로젝트 관리</h1>
        <p class="page-sub">수행 중인 프로젝트를 관리합니다.</p>
      </div>
      <button class="btn-primary" @click="showModal = true">+ 새 프로젝트 등록</button>
    </div>

    <div class="stage-tabs">
      <router-link to="/projects/bidding" class="stage-tab">
        입찰 단계 <span class="count">{{ biddingCount }}</span>
      </router-link>
      <router-link to="/projects/execution" class="stage-tab" active-class="active">
        수행 중 <span class="count">{{ executionCount }}</span>
      </router-link>
      <router-link to="/projects/completed" class="stage-tab" active-class="active">
        종료 <span class="count">{{ completedCount }}</span>
      </router-link>
    </div>

    <div class="filters">
      <select class="filter-select" v-model="filterTeam">
        <option value="">전체 부서</option>
        <option value="개발팀">개발팀</option>
        <option value="기획팀">기획팀</option>
      </select>
      <select class="filter-select" v-model="filterStatus">
        <option value="">전체 상태</option>
        <option value="IN_PROGRESS">수행 중</option>
        <option value="DELAYED">지연</option>
        <option value="COMPLETED">완료</option>
      </select>
      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="searchQuery" type="text" placeholder="프로젝트명 검색..." />
      </div>
    </div>

    <div v-if="loading" class="loading-state">불러오는 중...</div>

    <div class="projects-grid" v-else>
      <div
        v-for="project in filteredProjects"
        :key="project.id"
        class="project-card"
        @click="router.push(`/projects/${project.id}`)"
      >
        <div class="card-header">
          <div class="card-header-left">
            <StatusBadge :status="project.status" :label="project.statusLabel" />
            <span class="team-badge">{{ project.team }}</span>
          </div>
          <div class="dday" :class="project.dday > 0 ? 'overdue' : ''">
            {{ project.dday > 0 ? `D+${project.dday}` : `D${project.dday}` }}
          </div>
        </div>

        <h3 class="card-title">{{ project.name }}</h3>
        <p class="card-desc">{{ project.description }}</p>

        <div class="card-meta">
          <div class="meta-item">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
            <span>{{ project.client }}</span>
          </div>
          <div class="meta-item">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
            <span>{{ project.memberCount }}명</span>
          </div>
        </div>

        <div class="progress-section">
          <div class="progress-header">
            <span class="progress-label">진행률</span>
            <span class="progress-value">{{ project.progress }}%</span>
          </div>
          <ProgressBar :value="project.progress" />
        </div>

        <div class="card-footer">
          <div class="footer-item">
            <span class="footer-label">PM</span>
            <span class="footer-value">{{ project.pm }}</span>
          </div>
          <div class="footer-item">
            <span class="footer-label">기간</span>
            <span class="footer-value">{{ project.startDate }} ~ {{ project.endDate }}</span>
          </div>
          <div class="footer-item">
            <span class="footer-label">예산</span>
            <span class="footer-value">{{ formatBudget(project.budget) }}</span>
          </div>
        </div>

        <div class="card-stats">
          <div class="stat-item">
            <span class="stat-label">산출물</span>
            <span class="stat-val">{{ project.deliverableSubmitted }}/{{ project.deliverableTotal }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">검토중</span>
            <span class="stat-val">{{ project.deliverableReview }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">리스크</span>
            <span class="stat-val risk" :class="project.riskLevel === '높음' ? 'high' : project.riskLevel === '보통' ? 'med' : 'low'">{{ project.riskLevel }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && filteredProjects.length === 0">
      <EmptyState message="해당 조건에 맞는 프로젝트가 없습니다." />
    </div>

    <!-- Create Project Modal -->
    <CreateProjectModal
      v-if="showModal"
      @close="showModal = false"
      @created="onCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listProjects, mapProject } from '@/api/projects'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ProgressBar from '@/components/common/ProgressBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import CreateProjectModal from '@/views/projects/CreateProjectModal.vue'
import type { Project } from '@/types'

const route = useRoute()
const router = useRouter()

const filterTeam = ref('')
const filterStatus = ref('')
const searchQuery = ref('')
const showModal = ref(false)

const currentStage = computed(() => route.params.stage as string)
const loading = ref(false)
const apiProjects = ref<Project[]>([])

const biddingCount = ref(0)
const executionCount = ref(0)
const completedCount = ref(0)

async function loadCounts() {
  try {
    const [bidding, execution, completed] = await Promise.all([
      listProjects('BIDDING'),
      listProjects('EXECUTION'),
      listProjects('COMPLETED'),
    ])
    biddingCount.value = bidding.length
    executionCount.value = execution.length
    completedCount.value = completed.length
  } catch (e) {
    console.error('Failed to load stage counts', e)
  }
}

async function loadProjects() {
  const backendStage = currentStage.value === 'completed' ? 'COMPLETED' : 'EXECUTION'
  loading.value = true
  try {
    const dtos = await listProjects(backendStage)
    apiProjects.value = dtos.map(mapProject)
  } catch (e) {
    console.error('Failed to load projects', e)
    apiProjects.value = []
  } finally {
    loading.value = false
  }
}

function onCreated() {
  loadCounts()
  loadProjects()
}

onMounted(() => {
  loadCounts()
  loadProjects()
})
watch(currentStage, loadProjects)

const filteredProjects = computed(() => {
  let list: Project[] = apiProjects.value

  if (filterTeam.value) list = list.filter(p => p.team === filterTeam.value)
  if (filterStatus.value) list = list.filter(p => p.status === filterStatus.value)
  if (searchQuery.value) list = list.filter(p => p.name.includes(searchQuery.value))

  return list
})

function formatBudget(v: number) {
  if (v >= 100000000) return `${(v / 100000000).toFixed(1)}억`
  if (v >= 10000) return `${(v / 10000).toFixed(0)}만`
  return v.toLocaleString()
}
</script>

<style scoped>
.project-list { max-width: 1200px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.btn-primary {
  background: var(--color-primary);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}
.btn-primary:hover { background: var(--color-primary-hover); }

.btn-secondary {
  background: var(--bg-surface2);
  color: var(--text-primary);
  border: 1px solid var(--border);
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.stage-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 0;
}

.stage-tab {
  padding: 8px 16px;
  font-size: 13px;
  color: var(--text-secondary);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.15s;
}

.stage-tab:hover { color: var(--text-primary); }
.stage-tab.active, .stage-tab.router-link-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.count {
  background: var(--bg-surface2);
  color: var(--text-muted);
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filter-select {
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  color: var(--text-primary);
  border-radius: 6px;
  padding: 7px 10px;
  font-size: 13px;
  outline: none;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 7px 12px;
  color: var(--text-muted);
  flex: 1;
}

.search-box input {
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  width: 100%;
}

.loading-state {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

.projects-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.project-card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
}

.project-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.team-badge {
  background: var(--bg-surface2);
  color: var(--text-secondary);
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.dday {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-success);
}

.dday.overdue { color: var(--color-danger); }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  gap: 14px;
  margin-bottom: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-secondary);
}

.progress-section { margin-bottom: 14px; }

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.progress-label { font-size: 12px; color: var(--text-secondary); }
.progress-value { font-size: 12px; font-weight: 600; color: var(--color-primary); }

.card-footer {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
  margin-bottom: 12px;
}

.footer-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.footer-label { font-size: 10px; color: var(--text-muted); }
.footer-value { font-size: 12px; color: var(--text-secondary); }

.card-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label { font-size: 10px; color: var(--text-muted); }
.stat-val { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.stat-val.risk.high { color: var(--color-danger); }
.stat-val.risk.med { color: var(--color-warning); }
.stat-val.risk.low { color: var(--color-success); }

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.modal {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  width: 520px;
  max-height: 90vh;
  overflow-y: auto;
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

.modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 14px; }

.form-group { display: flex; flex-direction: column; gap: 6px; }
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

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}
</style>
