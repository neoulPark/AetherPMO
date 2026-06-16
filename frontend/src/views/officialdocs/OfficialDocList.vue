<template>
  <div class="official-docs-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">공문 관리</h1>
        <p class="page-sub">전체 프로젝트의 공문을 관리합니다.</p>
      </div>
      <button class="btn-primary">+ 공문 등록</button>
    </div>

    <div class="filters">
      <select class="filter-select" v-model="filterDirection">
        <option value="">전체 방향</option>
        <option value="INBOUND">수신</option>
        <option value="OUTBOUND">발신</option>
      </select>
      <select class="filter-select" v-model="filterApproval">
        <option value="">전체 결재</option>
        <option value="APPROVED">승인</option>
        <option value="PENDING">대기</option>
      </select>
      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="search" type="text" placeholder="공문 제목, 문서번호 검색..." />
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>문서번호</th>
            <th>프로젝트</th>
            <th>제목</th>
            <th>방향</th>
            <th>수신/발신처</th>
            <th>기안자</th>
            <th>발송일</th>
            <th>결재</th>
            <th>첨부</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="doc in filtered" :key="doc.id">
            <td><span class="doc-no">{{ doc.docNo }}</span></td>
            <td class="project-cell">{{ doc.projectName }}</td>
            <td class="title-cell">{{ doc.title }}</td>
            <td><StatusBadge :status="doc.direction" :label="doc.direction === 'INBOUND' ? '수신' : '발신'" /></td>
            <td>{{ doc.direction === 'INBOUND' ? doc.senderOrg : doc.receiverOrg }}</td>
            <td>{{ doc.drafter }}</td>
            <td :class="!doc.sentDate ? 'pending-date' : ''">{{ doc.sentDate ?? '미발송' }}</td>
            <td><StatusBadge :status="doc.approvalStatus" :label="doc.approvalStatus === 'APPROVED' ? '승인' : '대기'" /></td>
            <td>{{ doc.attachmentCount > 0 ? `${doc.attachmentCount}개` : '-' }}</td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="filtered.length === 0" message="공문이 없습니다." />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockOfficialDocs } from '@/stores/mock'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const filterDirection = ref('')
const filterApproval = ref('')
const search = ref('')

const filtered = computed(() => {
  let list = mockOfficialDocs
  if (filterDirection.value) list = list.filter(d => d.direction === filterDirection.value)
  if (filterApproval.value) list = list.filter(d => d.approvalStatus === filterApproval.value)
  if (search.value) list = list.filter(d => d.title.includes(search.value) || d.docNo.includes(search.value))
  return list
})
</script>

<style scoped>
.official-docs-view { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.filters { display: flex; gap: 10px; margin-bottom: 20px; }
.filter-select { background: var(--bg-surface2); border: 1px solid var(--border); color: var(--text-primary); border-radius: 6px; padding: 7px 10px; font-size: 13px; outline: none; }
.search-box { display: flex; align-items: center; gap: 8px; background: var(--bg-surface2); border: 1px solid var(--border); border-radius: 6px; padding: 7px 12px; color: var(--text-muted); flex: 1; }
.search-box input { border: none; background: transparent; color: var(--text-primary); font-size: 13px; outline: none; width: 100%; }
.table-wrap { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
thead th { background: var(--bg-surface2); color: var(--text-secondary); font-size: 12px; text-transform: uppercase; padding: 10px 12px; text-align: left; font-weight: 600; }
tbody td { padding: 12px; border-bottom: 1px solid var(--border); color: var(--text-primary); font-size: 13px; }
tbody tr:last-child td { border-bottom: none; }
tbody tr:hover { background: var(--bg-surface2); }
.doc-no { color: var(--color-info); cursor: pointer; font-family: monospace; font-size: 12px; }
.project-cell { font-size: 12px; color: var(--text-secondary); }
.title-cell { font-weight: 500; max-width: 280px; font-size: 12px; }
.pending-date { color: var(--text-muted); }
</style>
