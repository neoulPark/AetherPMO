<template>
  <div class="bidding-detail" v-if="project">
    <div class="breadcrumb">
      <router-link to="/projects/bidding">입찰 프로젝트</router-link>
      <span class="sep">›</span>
      <span>{{ project.name }}</span>
    </div>

    <div class="detail-top">
      <div class="detail-header">
        <div class="header-badges">
          <span class="bid-status" :class="project.bidStatus === 'SUBMITTED' ? 'submitted' : 'preparing'">
            {{ project.bidStatus === 'SUBMITTED' ? '제안 제출' : '제안 준비' }}
          </span>
          <span class="team-badge">{{ project.team }}</span>
        </div>
        <h1 class="project-title">{{ project.name }}</h1>
        <p class="project-desc" v-if="project.description">{{ project.description }}</p>
      </div>
      <div class="dday-large" :class="project.dday > 0 ? 'overdue' : ''">
        {{ project.dday > 0 ? `D+${project.dday}` : `D${project.dday}` }}
      </div>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >{{ tab.label }}</button>
    </div>

    <div class="tab-body">
      <!-- 사업개요 -->
      <div v-if="activeTab === 'overview'" class="grid-2">
        <div class="card">
          <h3 class="card-title">사업 기본 정보</h3>
          <div class="info-list">
            <div class="info-row"><span class="info-key">발주처</span><span class="info-val">{{ project.client }}</span></div>
            <div class="info-row"><span class="info-key">사업 규모</span><span class="info-val">{{ formatBudget(project.budget) }}</span></div>
            <div class="info-row"><span class="info-key">PM</span><span class="info-val">{{ project.pm }}</span></div>
            <div class="info-row"><span class="info-key">담당팀</span><span class="info-val">{{ project.team }}</span></div>
            <div class="info-row"><span class="info-key">참여인원</span><span class="info-val">{{ project.memberCount }}명</span></div>
          </div>
        </div>
        <div class="card">
          <h3 class="card-title">입찰 정보</h3>
          <div class="info-list">
            <div class="info-row"><span class="info-key">공고 번호</span><span class="info-val">{{ project.announcementNo ?? '미등록' }}</span></div>
            <div class="info-row"><span class="info-key">제안 마감일</span><span class="info-val">{{ project.proposalDeadline ?? '-' }}</span></div>
            <div class="info-row"><span class="info-key">컨소시엄 역할</span><span class="info-val">{{ project.consortiumRole }}</span></div>
            <div class="info-row"><span class="info-key">컨소시엄 지분</span><span class="info-val">{{ project.consortiumShare != null ? `${project.consortiumShare}%` : '미지정' }}</span></div>
            <div class="info-row"><span class="info-key">VRB 상태</span><span class="info-val">{{ project.vrb }}</span></div>
          </div>
        </div>
      </div>

      <!-- 제안준비서류 -->
      <div v-if="activeTab === 'proposal'">
        <EmptyState message="제안준비서류가 없습니다." />
      </div>

      <!-- 컨소시엄 -->
      <div v-if="activeTab === 'consortium'" class="card">
        <h3 class="card-title">컨소시엄 구성</h3>
        <div class="consortium-info">
          <div class="consortium-row">
            <span class="con-role">주관사</span>
            <span class="con-company">오케스트로</span>
            <span class="con-share">지분 미정</span>
          </div>
        </div>
        <p class="empty-note">컨소시엄 파트너사 정보가 없습니다.</p>
      </div>

      <!-- VRB -->
      <div v-if="activeTab === 'vrb'" class="card">
        <h3 class="card-title">VRB (사전 검토)</h3>
        <div class="vrb-status">
          <div class="vrb-badge not-submitted">미상신</div>
          <p class="vrb-desc">VRB 상신 전 단계입니다. VRB 문서를 준비 후 상신하세요.</p>
        </div>
      </div>
    </div>
  </div>
  <div v-else>
    <EmptyState message="입찰 프로젝트를 찾을 수 없습니다." />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockBiddingProjects } from '@/stores/mock'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const project = computed(() => mockBiddingProjects.find(p => p.id === projectId.value))

const activeTab = ref('overview')
const tabs = [
  { key: 'overview', label: '사업 개요' },
  { key: 'proposal', label: '제안 준비 서류' },
  { key: 'consortium', label: '컨소시엄' },
  { key: 'vrb', label: 'VRB' },
]

function formatBudget(v: number) {
  if (v >= 100000000) return `${(v / 100000000).toFixed(1)}억원`
  return `${v.toLocaleString()}원`
}
</script>

<style scoped>
.bidding-detail { max-width: 1200px; }
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; }
.breadcrumb a { color: var(--color-primary); text-decoration: none; }
.sep { color: var(--text-muted); }

.detail-top { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; padding: 20px; display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }

.header-badges { display: flex; gap: 8px; margin-bottom: 10px; }
.bid-status { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 10px; }
.bid-status.preparing { background: rgba(243,156,18,0.15); color: var(--color-warning); }
.bid-status.submitted { background: rgba(108,92,231,0.15); color: #a29bfe; }
.team-badge { background: var(--bg-surface2); color: var(--text-secondary); font-size: 11px; padding: 2px 8px; border-radius: 4px; }

.project-title { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.project-desc { font-size: 13px; color: var(--text-secondary); }

.dday-large { font-size: 28px; font-weight: 700; color: var(--color-success); }
.dday-large.overdue { color: var(--color-danger); }

.tabs { display: flex; border-bottom: 1px solid var(--border); margin-bottom: 20px; }
.tab-btn { padding: 10px 16px; font-size: 13px; color: var(--text-secondary); background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -1px; cursor: pointer; transition: color 0.15s; }
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }

.tab-body { }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; padding: 20px; }
.card-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 14px; text-transform: uppercase; }

.info-list { display: flex; flex-direction: column; gap: 8px; }
.info-row { display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid var(--border); }
.info-row:last-child { border-bottom: none; padding-bottom: 0; }
.info-key { font-size: 12px; color: var(--text-muted); }
.info-val { font-size: 12px; color: var(--text-primary); }

.consortium-row { display: flex; gap: 16px; align-items: center; padding: 10px; background: var(--bg-surface2); border-radius: 6px; }
.con-role { font-size: 11px; background: rgba(108,92,231,0.15); color: #a29bfe; padding: 2px 8px; border-radius: 4px; }
.con-company { font-size: 13px; font-weight: 600; color: var(--text-primary); flex: 1; }
.con-share { font-size: 12px; color: var(--text-muted); }
.empty-note { font-size: 12px; color: var(--text-muted); margin-top: 10px; }

.vrb-status { display: flex; align-items: center; gap: 14px; }
.vrb-badge { font-size: 12px; font-weight: 600; padding: 6px 14px; border-radius: 6px; }
.vrb-badge.not-submitted { background: rgba(74,85,104,0.3); color: var(--text-muted); }
.vrb-desc { font-size: 13px; color: var(--text-secondary); }
</style>
