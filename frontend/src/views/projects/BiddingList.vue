<template>
  <div class="bidding-list">
    <div class="page-header">
      <div>
        <h1 class="page-title">입찰 단계 프로젝트</h1>
        <p class="page-sub">입찰 및 제안 준비 중인 사업을 관리합니다.</p>
      </div>
      <button class="btn-primary">+ 입찰 사업 등록</button>
    </div>

    <div class="stage-tabs">
      <router-link to="/projects/bidding" class="stage-tab active">
        입찰 단계 <span class="count">{{ biddingProjects.length }}</span>
      </router-link>
      <router-link to="/projects/execution" class="stage-tab">
        수행 중 <span class="count">0</span>
      </router-link>
      <router-link to="/projects/completed" class="stage-tab">
        종료 <span class="count">0</span>
      </router-link>
    </div>

    <div class="sub-tabs">
      <button
        v-for="t in subTabs"
        :key="t.key"
        class="sub-tab"
        :class="{ active: activeSubTab === t.key }"
        @click="activeSubTab = t.key"
      >{{ t.label }} <span class="count">{{ countFor(t.key) }}</span></button>
    </div>

    <div class="content-layout">
      <div class="projects-col">
        <div v-if="loading" class="loading-state">불러오는 중...</div>
        <div v-else-if="filteredProjects.length === 0">
          <EmptyState message="해당 조건에 맞는 입찰 프로젝트가 없습니다." />
        </div>
        <div
          v-for="project in filteredProjects"
          :key="project.id"
          class="bid-card"
          @click="router.push(`/projects/bidding/${project.id}`)"
        >
          <div class="bid-card-header">
            <div class="bid-header-left">
              <span class="bid-status" :class="project.bidStatus === 'SUBMITTED' ? 'submitted' : 'preparing'">
                {{ bidStatusLabel(project.bidStatus) }}
              </span>
              <span class="team-badge">{{ project.team }}</span>
            </div>
            <div class="dday" :class="project.dday > 0 ? 'overdue' : ''">
              {{ project.dday > 0 ? `D+${project.dday}` : `D${project.dday}` }}
            </div>
          </div>

          <h3 class="bid-title">{{ project.name }}</h3>
          <p class="bid-desc" v-if="project.description">{{ project.description }}</p>

          <div class="bid-meta">
            <div class="meta-row">
              <span class="meta-key">발주처</span>
              <span class="meta-val">{{ project.client }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">제안 마감</span>
              <span class="meta-val" :class="isDeadlineNear(project.proposalDeadline) ? 'warn' : ''">{{ project.proposalDeadline ?? '-' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">사업 규모</span>
              <span class="meta-val">{{ formatBudget(project.budget) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">PM</span>
              <span class="meta-val">{{ project.pm }}</span>
            </div>
          </div>

          <div class="bid-footer">
            <div class="footer-chip">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
              </svg>
              {{ project.consortiumRole }}
            </div>
            <div class="footer-chip vrb">{{ project.vrb }}</div>
          </div>
        </div>
      </div>

      <div class="narara-panel">
        <div class="panel-header">
          <h3>나라장터 공고 검색</h3>
          <span class="panel-badge">BETA</span>
        </div>
        <div class="panel-search">
          <div class="search-input-wrap">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input v-model="naraSearch" type="text" placeholder="공고명, 발주처 검색..." />
          </div>
          <button class="btn-primary" style="white-space:nowrap">검색</button>
        </div>

        <div class="panel-filters">
          <select class="filter-select" v-model="naraType">
            <option value="">입찰 유형</option>
            <option value="general">일반경쟁</option>
            <option value="limited">제한경쟁</option>
          </select>
          <select class="filter-select" v-model="naraBudget">
            <option value="">금액 범위</option>
            <option value="1">10억 미만</option>
            <option value="2">10억~50억</option>
            <option value="3">50억 이상</option>
          </select>
        </div>

        <div class="panel-results">
          <div class="panel-result-item" v-for="n in 3" :key="n">
            <div class="result-badge">SW개발</div>
            <p class="result-title">{{ ['행정안전부 디지털 전환 기반 데이터 플랫폼 구축', '국토교통부 스마트시티 통합관제 시스템 구축', '국방부 사이버보안 취약점 점검 자동화 시스템'][n-1] }}</p>
            <div class="result-meta">
              <span>{{ ['조달청', '국토교통부', '방위사업청'][n-1] }}</span>
              <span>{{ ['120억', '85억', '47억'][n-1] }}</span>
              <span class="deadline">마감 2026-06-{{ [20, 25, 28][n-1] }}</span>
            </div>
            <button class="btn-import">사업 등록</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listProjects, mapBiddingProject, bidStatusLabel } from '@/api/projects'
import EmptyState from '@/components/common/EmptyState.vue'
import type { Project } from '@/types'

const router = useRouter()
const naraSearch = ref('')
const naraType = ref('')
const naraBudget = ref('')

const loading = ref(false)
const biddingProjects = ref<Project[]>([])

const subTabs = [
  { key: 'all', label: '전체' },
  { key: 'PREPARING', label: '제안 준비중' },
  { key: 'SUBMITTED', label: '제안 제출' },
  { key: 'WAITING', label: '결과 대기' },
  { key: 'WON', label: '수주' },
  { key: 'LOST', label: '실패' },
]
const activeSubTab = ref('all')

function countFor(key: string) {
  if (key === 'all') return biddingProjects.value.length
  return biddingProjects.value.filter(p => p.bidStatus === key).length
}

const filteredProjects = computed(() => {
  if (activeSubTab.value === 'all') return biddingProjects.value
  return biddingProjects.value.filter(p => p.bidStatus === activeSubTab.value)
})

async function loadProjects() {
  loading.value = true
  try {
    const dtos = await listProjects('BIDDING')
    biddingProjects.value = dtos.map(mapBiddingProject)
  } catch (e) {
    console.error('Failed to load bidding projects', e)
    biddingProjects.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadProjects)

function formatBudget(v: number) {
  if (v >= 100000000) return `${(v / 100000000).toFixed(1)}억원`
  return `${v.toLocaleString()}원`
}

function isDeadlineNear(date?: string) {
  if (!date) return false
  const d = new Date(date)
  const now = new Date('2026-06-15')
  const diff = (d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)
  return diff >= 0 && diff <= 7
}
</script>

<style scoped>
.bidding-list { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }

.stage-tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid var(--border); }
.stage-tab { padding: 8px 16px; font-size: 13px; color: var(--text-secondary); text-decoration: none; border-bottom: 2px solid transparent; margin-bottom: -1px; display: flex; align-items: center; gap: 6px; transition: color 0.15s; }
.stage-tab:hover, .stage-tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }
.count { background: var(--bg-surface2); color: var(--text-muted); font-size: 11px; padding: 1px 6px; border-radius: 10px; }

.sub-tabs { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }
.sub-tab { background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-secondary); border-radius: 16px; padding: 5px 12px; font-size: 12px; cursor: pointer; display: flex; align-items: center; gap: 6px; }
.sub-tab.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.sub-tab .count { background: rgba(0,0,0,0.2); color: inherit; }
.loading-state { padding: 40px; text-align: center; color: var(--text-muted); font-size: 13px; }

.content-layout { display: grid; grid-template-columns: 1fr 360px; gap: 20px; }

.projects-col { display: flex; flex-direction: column; gap: 14px; }

.bid-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; padding: 18px; cursor: pointer; transition: border-color 0.15s; }
.bid-card:hover { border-color: var(--color-primary); }

.bid-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.bid-header-left { display: flex; gap: 8px; align-items: center; }

.bid-status { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.bid-status.preparing { background: rgba(243,156,18,0.15); color: var(--color-warning); }
.bid-status.submitted { background: rgba(108,92,231,0.15); color: #a29bfe; }

.team-badge { background: var(--bg-surface2); color: var(--text-secondary); font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.dday { font-size: 12px; font-weight: 600; color: var(--color-success); }
.dday.overdue { color: var(--color-danger); }

.bid-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 6px; line-height: 1.4; }
.bid-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin-bottom: 12px; }

.bid-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }
.meta-row { display: flex; flex-direction: column; gap: 2px; }
.meta-key { font-size: 10px; color: var(--text-muted); }
.meta-val { font-size: 13px; color: var(--text-primary); }
.meta-val.warn { color: var(--color-warning); }

.bid-footer { display: flex; gap: 10px; padding-top: 12px; border-top: 1px solid var(--border); }
.footer-chip { display: flex; align-items: center; gap: 5px; font-size: 11px; color: var(--text-secondary); background: var(--bg-surface2); padding: 4px 10px; border-radius: 6px; }
.footer-chip.vrb { color: var(--text-muted); }

.narara-panel { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; padding: 18px; height: fit-content; position: sticky; top: 0; }
.panel-header { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.panel-header h3 { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.panel-badge { background: rgba(108,92,231,0.2); color: #a29bfe; font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: 600; }

.panel-search { display: flex; gap: 8px; margin-bottom: 10px; }
.search-input-wrap { flex: 1; display: flex; align-items: center; gap: 8px; background: var(--bg-surface2); border: 1px solid var(--border); border-radius: 6px; padding: 7px 10px; color: var(--text-muted); }
.search-input-wrap input { border: none; background: transparent; color: var(--text-primary); font-size: 13px; outline: none; width: 100%; }

.panel-filters { display: flex; gap: 8px; margin-bottom: 14px; }
.filter-select { flex: 1; background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-secondary); border-radius: 6px; padding: 6px 8px; font-size: 12px; outline: none; }

.panel-results { display: flex; flex-direction: column; gap: 10px; }
.panel-result-item { background: var(--bg-surface2); border: 1px solid var(--border); border-radius: 8px; padding: 12px; }
.result-badge { display: inline-block; font-size: 10px; padding: 1px 6px; border-radius: 4px; background: rgba(0,206,201,0.15); color: var(--color-cyan); margin-bottom: 6px; }
.result-title { font-size: 13px; font-weight: 500; color: var(--text-primary); line-height: 1.4; margin-bottom: 6px; }
.result-meta { display: flex; gap: 10px; font-size: 11px; color: var(--text-secondary); margin-bottom: 8px; }
.deadline { color: var(--color-warning); }
.btn-import { background: rgba(108,92,231,0.15); border: 1px solid rgba(108,92,231,0.3); color: #a29bfe; padding: 4px 10px; border-radius: 4px; cursor: pointer; font-size: 11px; }
</style>
