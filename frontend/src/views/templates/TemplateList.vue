<template>
  <div class="template-list">
    <div class="page-header">
      <div>
        <h1 class="page-title">산출물 템플릿</h1>
        <p class="page-sub">프로젝트 단계별 표준 산출물 템플릿을 관리합니다.</p>
      </div>
      <button class="btn-primary">+ 템플릿 등록</button>
    </div>

    <div class="stage-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="stage-tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >{{ tab.label }}</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>템플릿명</th>
            <th>구분</th>
            <th>버전</th>
            <th>최종 수정일</th>
            <th>파일</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="templates.length === 0">
            <td colspan="6">
              <EmptyState message="등록된 템플릿이 없습니다." />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'

const activeTab = ref('INCEPTION')
const tabs = [
  { key: 'INCEPTION', label: '착수 단계' },
  { key: 'EXECUTION', label: '수행 단계' },
  { key: 'CLOSURE', label: '종료 단계' },
]

const templates = computed(() => [])
</script>

<style scoped>
.template-list { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }

.stage-tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid var(--border); }
.stage-tab { padding: 8px 16px; font-size: 13px; color: var(--text-secondary); background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -1px; cursor: pointer; transition: color 0.15s; }
.stage-tab:hover { color: var(--text-primary); }
.stage-tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }

.table-wrap { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
thead th { background: var(--bg-surface2); color: var(--text-secondary); font-size: 12px; text-transform: uppercase; padding: 10px 12px; text-align: left; font-weight: 600; }
tbody td { padding: 12px; border-bottom: 1px solid var(--border); color: var(--text-primary); font-size: 13px; }
</style>
