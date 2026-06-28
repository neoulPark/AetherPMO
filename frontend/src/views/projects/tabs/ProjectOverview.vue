<template>
  <div class="overview" v-if="project">
    <div class="grid-3">
      <div class="card">
        <h3 class="card-title">진행률</h3>
        <div class="big-progress">
          <div class="big-num">{{ project.progress }}%</div>
          <ProgressBar :value="project.progress" />
          <div class="progress-dates">
            <span>{{ project.startDate }}</span>
            <span>{{ project.endDate }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">산출물 현황</h3>
        <div class="stat-grid">
          <div class="stat-box">
            <div class="stat-num">{{ project.deliverableTotal }}</div>
            <div class="stat-label">전체</div>
          </div>
          <div class="stat-box green">
            <div class="stat-num">{{ project.deliverableSubmitted }}</div>
            <div class="stat-label">승인완료</div>
          </div>
          <div class="stat-box orange">
            <div class="stat-num">{{ project.deliverableReview }}</div>
            <div class="stat-label">검토중</div>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">이슈 / 리스크</h3>
        <div class="stat-grid">
          <div class="stat-box red">
            <div class="stat-num">{{ openIssues }}</div>
            <div class="stat-label">발생</div>
          </div>
          <div class="stat-box orange">
            <div class="stat-num">{{ inProgressIssues }}</div>
            <div class="stat-label">조치중</div>
          </div>
          <div class="stat-box green">
            <div class="stat-num">{{ doneIssues }}</div>
            <div class="stat-label">완료</div>
          </div>
        </div>
      </div>
    </div>

    <div class="grid-3 mt16">
      <div class="card">
        <h3 class="card-title">사업 정보</h3>
        <div class="info-list">
          <div class="info-row">
            <span class="info-key">사업명</span>
            <span class="info-val">{{ project.name }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">고객사</span>
            <span class="info-val">{{ project.client }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">PM</span>
            <span class="info-val">{{ project.pm }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">팀</span>
            <span class="info-val">{{ project.team }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">팀원 수</span>
            <span class="info-val">{{ project.memberCount }}명</span>
          </div>
          <div class="info-row">
            <span class="info-key">예산</span>
            <span class="info-val">{{ formatBudget(project.budget) }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">착수일</span>
            <span class="info-val">{{ project.startDate }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">종료일</span>
            <span class="info-val">{{ project.endDate }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">리스크 수준</span>
            <span class="info-val risk" :class="project.riskLevel === '높음' ? 'high' : project.riskLevel === '보통' ? 'med' : 'low'">{{ project.riskLevel }}</span>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">최근 액션 아이템</h3>
        <div class="action-list">
          <div v-for="item in projectActionItems" :key="item.id" class="action-row">
            <div class="action-info">
              <span class="action-title">{{ item.title }}</span>
              <div class="action-meta">
                <span>{{ item.assignee }}</span>
                <span class="dot">·</span>
                <span :class="isOverdue(item.dueDate) ? 'overdue' : ''">{{ item.dueDate }}</span>
              </div>
            </div>
            <StatusBadge :status="item.status" :label="item.statusLabel" />
          </div>
          <EmptyState v-if="projectActionItems.length === 0" message="액션 아이템이 없습니다." />
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">최근 이슈</h3>
        <div class="action-list">
          <div v-for="issue in projectIssues" :key="issue.id" class="action-row">
            <div class="action-info">
              <div class="issue-type-row">
                <StatusBadge :status="issue.type" :label="issue.type" />
                <span class="priority" :class="'p-' + issue.priority">우선순위: {{ issue.priority }}</span>
              </div>
              <span class="action-title">{{ issue.title }}</span>
              <div class="action-meta">
                <span>{{ issue.assignee }}</span>
                <span class="dot">·</span>
                <span>{{ issue.occurredDate }}</span>
              </div>
            </div>
            <StatusBadge :status="issue.status" :label="issue.statusLabel" />
          </div>
          <EmptyState v-if="projectIssues.length === 0" message="이슈가 없습니다." />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { mockActionItems, mockIssues } from '@/stores/mock'
import type { Project } from '@/types'
import ProgressBar from '@/components/common/ProgressBar.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const project = inject<Ref<Project | null>>('project')!

const projectActionItems = computed(() => mockActionItems.filter(a => a.projectId === projectId.value))
const projectIssues = computed(() => mockIssues.filter(i => i.projectId === projectId.value))

const openIssues = computed(() => projectIssues.value.filter(i => i.status === 'OPEN').length)
const inProgressIssues = computed(() => projectIssues.value.filter(i => i.status === 'IN_PROGRESS').length)
const doneIssues = computed(() => projectIssues.value.filter(i => i.status === 'DONE').length)

function formatBudget(v: number) {
  if (v >= 100000000) return `${(v / 100000000).toFixed(1)}억원`
  return `${v.toLocaleString()}원`
}

function isOverdue(date: string) {
  return new Date(date) < new Date('2026-06-15')
}
</script>

<style scoped>
.overview { }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.mt16 { margin-top: 16px; }

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 18px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.big-num {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 10px;
}

.progress-dates {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 6px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.stat-box {
  background: var(--bg-surface2);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.stat-box.green { background: rgba(0,184,148,0.1); }
.stat-box.orange { background: rgba(243,156,18,0.1); }
.stat-box.red { background: rgba(231,76,60,0.1); }

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label { font-size: 11px; color: var(--text-secondary); }

.info-list { display: flex; flex-direction: column; gap: 8px; }

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.info-row:last-child { border-bottom: none; padding-bottom: 0; }

.info-key { font-size: 12px; color: var(--text-muted); flex-shrink: 0; }
.info-val { font-size: 12px; color: var(--text-primary); text-align: right; }
.info-val.risk.high { color: var(--color-danger); }
.info-val.risk.med { color: var(--color-warning); }
.info-val.risk.low { color: var(--color-success); }

.action-list { display: flex; flex-direction: column; gap: 10px; }

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.action-row:last-child { border-bottom: none; }

.action-info { flex: 1; min-width: 0; }
.action-title { font-size: 13px; color: var(--text-primary); display: block; margin-bottom: 4px; line-height: 1.3; }
.action-meta { font-size: 11px; color: var(--text-muted); display: flex; gap: 4px; }
.dot { color: var(--border); }
.overdue { color: var(--color-danger); }

.issue-type-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.priority { font-size: 11px; }
.p-상 { color: var(--color-danger); }
.p-중 { color: var(--color-warning); }
.p-하 { color: var(--color-success); }
</style>
