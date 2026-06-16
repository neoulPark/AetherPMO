<template>
  <div class="official-docs">
    <div class="tab-header">
      <h2 class="tab-title">공문 관리</h2>
      <button class="btn-primary">+ 공문 등록</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>문서번호</th>
            <th>제목</th>
            <th>방향</th>
            <th>수신처</th>
            <th>기안자</th>
            <th>발송일</th>
            <th>결재</th>
            <th>첨부</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="doc in docs" :key="doc.id">
            <td><span class="doc-no">{{ doc.docNo }}</span></td>
            <td class="title-cell">{{ doc.title }}</td>
            <td><StatusBadge :status="doc.direction" :label="doc.direction === 'INBOUND' ? '수신' : '발신'" /></td>
            <td>{{ doc.receiverOrg }}</td>
            <td>{{ doc.drafter }}</td>
            <td :class="!doc.sentDate ? 'pending-date' : ''">{{ doc.sentDate ?? '미발송' }}</td>
            <td><StatusBadge :status="doc.approvalStatus" :label="doc.approvalStatus === 'APPROVED' ? '승인' : '대기'" /></td>
            <td>{{ doc.attachmentCount > 0 ? `${doc.attachmentCount}개` : '-' }}</td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="docs.length === 0" message="등록된 공문이 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { mockOfficialDocs } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const docs = computed(() => mockOfficialDocs.filter(d => d.projectId === projectId.value))
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
.doc-no { color: var(--color-info); cursor: pointer; font-family: monospace; font-size: 12px; }
.title-cell { font-weight: 500; max-width: 300px; }
.pending-date { color: var(--text-muted); }
</style>
