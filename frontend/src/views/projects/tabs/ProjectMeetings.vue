<template>
  <div class="meetings">
    <div class="tab-header">
      <h2 class="tab-title">회의록</h2>
      <button class="btn-primary">+ 회의 등록</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>회의명</th>
            <th>유형</th>
            <th>일시</th>
            <th>장소</th>
            <th>참석자</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in meetings" :key="m.id">
            <td class="name-cell">{{ m.title }}</td>
            <td><span class="type-tag">{{ m.type }}</span></td>
            <td>{{ m.dateTime }}</td>
            <td>{{ m.location }}</td>
            <td class="attendees-cell">{{ m.attendees }}</td>
            <td><StatusBadge :status="m.status" :label="m.status === 'COMPLETED' ? '완료' : '예정'" /></td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="meetings.length === 0" message="등록된 회의가 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockMeetings } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const meetings = computed(() => mockMeetings.filter(m => m.projectId === projectId.value))
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
.name-cell { font-weight: 500; }
.attendees-cell { font-size: 12px; color: var(--text-secondary); max-width: 200px; }
.type-tag { background: var(--bg-surface2); color: var(--text-secondary); font-size: 11px; padding: 2px 8px; border-radius: 4px; }
</style>
