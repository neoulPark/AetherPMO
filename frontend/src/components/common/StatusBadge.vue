<template>
  <span class="badge" :class="badgeClass">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  label: string
}>()

const badgeClass = computed(() => {
  const s = props.status.toUpperCase()
  if (s === 'IN_PROGRESS' || s === '수행중') return 'badge-purple'
  if (s === 'DELAYED' || s === '지연') return 'badge-red'
  if (s === 'APPROVED' || s === '승인완료') return 'badge-green'
  if (s === 'REVIEW' || s === '검토요청') return 'badge-orange'
  if (s === 'DONE' || s === '완료') return 'badge-green'
  if (s === 'PENDING' || s === '대기') return 'badge-gray'
  if (s === 'OPEN' || s === '발생') return 'badge-red'
  if (s === 'ISSUE') return 'badge-dark'
  if (s === 'RISK') return 'badge-slate'
  if (s === 'INBOUND' || s === '수신') return 'badge-blue'
  if (s === 'OUTBOUND' || s === '발신') return 'badge-cyan'
  if (s === 'BIDDING') return 'badge-orange'
  if (s === 'COMPLETED') return 'badge-green'
  return 'badge-gray'
})
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.badge-purple { background: rgba(108, 92, 231, 0.2); color: #a29bfe; }
.badge-red { background: rgba(231, 76, 60, 0.2); color: #ff7675; }
.badge-green { background: rgba(0, 184, 148, 0.2); color: #00b894; }
.badge-orange { background: rgba(243, 156, 18, 0.2); color: #f39c12; }
.badge-gray { background: rgba(74, 85, 104, 0.4); color: #8892a4; }
.badge-dark { background: rgba(44, 48, 68, 0.8); color: #8892a4; }
.badge-slate { background: rgba(30, 33, 48, 0.9); color: #636e82; border: 1px solid #2a2d3e; }
.badge-blue { background: rgba(9, 132, 227, 0.2); color: #74b9ff; }
.badge-cyan { background: rgba(0, 206, 201, 0.2); color: #00cec9; }
</style>
