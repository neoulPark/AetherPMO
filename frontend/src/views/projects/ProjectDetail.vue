<template>
  <div class="project-detail" v-if="project">
    <div class="detail-top">
      <div class="breadcrumb">
        <router-link to="/projects/execution">프로젝트</router-link>
        <span class="sep">›</span>
        <span>{{ project.name }}</span>
      </div>

      <div class="detail-header">
        <div class="header-left">
          <div class="header-badges">
            <StatusBadge :status="project.status" :label="project.statusLabel" />
            <span class="team-badge">{{ project.team }}</span>
            <span class="risk-badge" :class="project.riskLevel === '높음' ? 'high' : project.riskLevel === '보통' ? 'med' : 'low'">
              리스크: {{ project.riskLevel }}
            </span>
          </div>
          <h1 class="project-title">{{ project.name }}</h1>
          <p class="project-desc">{{ project.description }}</p>
        </div>
        <div class="header-right">
          <div class="dday-large" :class="project.dday > 0 ? 'overdue' : ''">
            {{ project.dday > 0 ? `D+${project.dday}` : `D${project.dday}` }}
          </div>
        </div>
      </div>

      <div class="meta-bar">
        <div class="meta-item">
          <span class="meta-label">고객사</span>
          <span class="meta-val">{{ project.client }}</span>
        </div>
        <div class="meta-divider"></div>
        <div class="meta-item">
          <span class="meta-label">PM</span>
          <span class="meta-val">{{ project.pm }}</span>
        </div>
        <div class="meta-divider"></div>
        <div class="meta-item">
          <span class="meta-label">사업 기간</span>
          <span class="meta-val">{{ project.startDate }} ~ {{ project.endDate }}</span>
        </div>
        <div class="meta-divider"></div>
        <div class="meta-item">
          <span class="meta-label">예산</span>
          <span class="meta-val">{{ formatBudget(project.budget) }}</span>
        </div>
        <div class="meta-divider"></div>
        <div class="meta-item">
          <span class="meta-label">팀원</span>
          <span class="meta-val">{{ project.memberCount }}명</span>
        </div>
        <div class="meta-divider"></div>
        <div class="meta-item">
          <span class="meta-label">진행률</span>
          <span class="meta-val progress-val">{{ project.progress }}%</span>
        </div>
      </div>
    </div>

    <div class="detail-tabs">
      <router-link :to="`/projects/${project.id}/overview`" class="tab-link" active-class="active">사업 개요</router-link>
      <router-link :to="`/projects/${project.id}/deliverables`" class="tab-link" active-class="active">산출물</router-link>
      <router-link :to="`/projects/${project.id}/meetings`" class="tab-link" active-class="active">회의록</router-link>
      <router-link :to="`/projects/${project.id}/risks`" class="tab-link" active-class="active">이슈/리스크</router-link>
      <router-link :to="`/projects/${project.id}/action-items`" class="tab-link" active-class="active">액션 아이템</router-link>
      <router-link :to="`/projects/${project.id}/official-docs`" class="tab-link" active-class="active">공문</router-link>
    </div>

    <div class="tab-content">
      <router-view />
    </div>
  </div>
  <div v-else class="not-found">
    <EmptyState message="프로젝트를 찾을 수 없습니다." />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockProjects } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const project = computed(() => mockProjects.find(p => p.id === projectId.value))

function formatBudget(v: number) {
  if (v >= 100000000) return `${(v / 100000000).toFixed(1)}억원`
  if (v >= 10000) return `${(v / 10000).toFixed(0)}만원`
  return `${v.toLocaleString()}원`
}
</script>

<style scoped>
.project-detail { max-width: 1200px; }

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.breadcrumb a { color: var(--color-primary); text-decoration: none; }
.sep { color: var(--text-muted); }

.detail-top {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.header-badges {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.team-badge {
  background: var(--bg-surface2);
  color: var(--text-secondary);
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.risk-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.risk-badge.high { background: rgba(231,76,60,0.15); color: var(--color-danger); }
.risk-badge.med { background: rgba(243,156,18,0.15); color: var(--color-warning); }
.risk-badge.low { background: rgba(0,184,148,0.15); color: var(--color-success); }

.project-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
  line-height: 1.3;
}

.project-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.dday-large {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-success);
  white-space: nowrap;
}

.dday-large.overdue { color: var(--color-danger); }

.meta-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding-top: 14px;
  border-top: 1px solid var(--border);
  flex-wrap: wrap;
  gap: 0;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 0 16px;
}

.meta-item:first-child { padding-left: 0; }

.meta-label { font-size: 10px; color: var(--text-muted); }
.meta-val { font-size: 13px; color: var(--text-primary); font-weight: 500; }
.meta-val.progress-val { color: var(--color-primary); font-weight: 700; }

.meta-divider {
  width: 1px;
  height: 30px;
  background: var(--border);
}

.detail-tabs {
  display: flex;
  gap: 0;
  margin-top: 16px;
  border-bottom: 1px solid var(--border);
}

.tab-link {
  padding: 10px 16px;
  font-size: 13px;
  color: var(--text-secondary);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.15s;
}

.tab-link:hover { color: var(--text-primary); }
.tab-link.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }

.tab-content { margin-top: 20px; }

.not-found { padding: 60px; }
</style>
