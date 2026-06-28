<template>
  <div class="methodology">
    <div class="page-header">
      <div>
        <h1 class="page-title">사업관리 표준 (OPMS)</h1>
        <p class="page-sub">공공 SI 사업관리 표준 방법론 — 단계 · 활동 · 업무 · 산출물 카탈로그</p>
      </div>
      <el-button type="primary" @click="openAdd(null, 'PHASE')">+ 단계 추가</el-button>
    </div>

    <div v-if="loading" class="loading-state">불러오는 중...</div>

    <template v-else>
      <div class="summary">{{ summary }}</div>

      <div class="card">
        <el-tree
          :data="catalog"
          node-key="nodeId"
          :props="{ label: 'name', children: 'children' }"
          :expand-on-click-node="false"
        >
          <template #default="{ data }">
            <div class="tree-row">
              <span class="type-badge" :class="'t-' + data.nodeType">{{ typeLabel(data.nodeType) }}</span>
              <span v-if="data.code" class="code">[{{ data.code }}]</span>
              <span class="name">{{ data.name }}</span>
              <span v-if="data.isOptional" class="badge">선택</span>
              <span class="actions">
                <el-button
                  v-if="childTypeOf(data.nodeType)"
                  link
                  size="small"
                  @click.stop="openAdd(data, childTypeOf(data.nodeType)!)"
                >+ 하위추가</el-button>
                <el-button link size="small" @click.stop="openEdit(data)">수정</el-button>
                <el-button link size="small" type="danger" @click.stop="onDelete(data)">삭제</el-button>
              </span>
            </div>
          </template>
        </el-tree>
      </div>
    </template>

    <!-- Add / Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px">
      <el-form label-position="top">
        <el-form-item label="이름" required>
          <el-input v-model="formName" placeholder="이름을 입력하세요" />
        </el-form-item>
        <el-form-item label="코드">
          <el-input v-model="formCode" placeholder="코드 (선택)" />
        </el-form-item>
        <el-form-item v-if="showOptional">
          <el-checkbox v-model="formOptional">선택 항목</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">취소</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCatalog,
  createNode,
  updateNode,
  deleteNode,
  type CatalogNode,
  type NodeType,
} from '@/api/methodology'

const catalog = ref<CatalogNode[]>([])
const loading = ref(false)

const TYPE_LABELS: Record<NodeType, string> = {
  PHASE: '단계',
  ACTIVITY: '활동',
  TASK: '업무',
  DELIVERABLE: '산출물',
}
function typeLabel(t: NodeType) {
  return TYPE_LABELS[t]
}

const CHILD_TYPE: Record<NodeType, NodeType | null> = {
  PHASE: 'ACTIVITY',
  ACTIVITY: 'TASK',
  TASK: 'DELIVERABLE',
  DELIVERABLE: null,
}
function childTypeOf(t: NodeType): NodeType | null {
  return CHILD_TYPE[t]
}

const summary = computed(() => {
  const counts: Record<NodeType, number> = { PHASE: 0, ACTIVITY: 0, TASK: 0, DELIVERABLE: 0 }
  const walk = (nodes: CatalogNode[]) => {
    for (const n of nodes) {
      counts[n.nodeType] = (counts[n.nodeType] || 0) + 1
      if (n.children?.length) walk(n.children)
    }
  }
  walk(catalog.value)
  return `${counts.PHASE}단계 · ${counts.ACTIVITY}활동 · ${counts.TASK}업무 · ${counts.DELIVERABLE}산출물`
})

async function load() {
  loading.value = true
  try {
    catalog.value = await getCatalog()
  } catch (e) {
    console.error('Failed to load catalog', e)
    catalog.value = []
  } finally {
    loading.value = false
  }
}

// Dialog state
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null) // null => add mode
const newNodeType = ref<NodeType>('PHASE')
const newParentId = ref<number | null>(null)
const formName = ref('')
const formCode = ref('')
const formOptional = ref(false)

const dialogTitle = computed(() =>
  editingId.value === null
    ? `${typeLabel(newNodeType.value)} 추가`
    : `${typeLabel(newNodeType.value)} 수정`
)
const showOptional = computed(
  () => newNodeType.value === 'TASK' || newNodeType.value === 'DELIVERABLE'
)

function openAdd(parent: CatalogNode | null, nodeType: NodeType) {
  editingId.value = null
  newNodeType.value = nodeType
  newParentId.value = parent ? parent.nodeId : null
  formName.value = ''
  formCode.value = ''
  formOptional.value = false
  dialogVisible.value = true
}

function openEdit(node: CatalogNode) {
  editingId.value = node.nodeId
  newNodeType.value = node.nodeType
  newParentId.value = node.parentNodeId
  formName.value = node.name
  formCode.value = node.code || ''
  formOptional.value = node.isOptional
  dialogVisible.value = true
}

async function submit() {
  if (!formName.value.trim()) {
    ElMessage.error('이름을 입력하세요.')
    return
  }
  submitting.value = true
  try {
    if (editingId.value === null) {
      await createNode({
        parentNodeId: newParentId.value,
        nodeType: newNodeType.value,
        name: formName.value.trim(),
        code: formCode.value.trim() || null,
        isOptional: showOptional.value ? formOptional.value : false,
      })
      ElMessage.success('추가되었습니다.')
    } else {
      await updateNode(editingId.value, {
        name: formName.value.trim(),
        code: formCode.value.trim() || null,
        isOptional: showOptional.value ? formOptional.value : undefined,
      })
      ElMessage.success('수정되었습니다.')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    console.error('Save failed', e)
    ElMessage.error('저장에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

async function onDelete(node: CatalogNode) {
  try {
    await ElMessageBox.confirm(
      `"${node.name}" 및 하위 항목이 모두 삭제됩니다. 계속하시겠습니까?`,
      '삭제 확인',
      { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }
    )
  } catch {
    return
  }
  try {
    await deleteNode(node.nodeId)
    ElMessage.success('삭제되었습니다.')
    await load()
  } catch (e) {
    console.error('Delete failed', e)
    ElMessage.error('삭제에 실패했습니다.')
  }
}

onMounted(load)
</script>

<style scoped>
.methodology { max-width: 1100px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.page-sub { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.loading-state { padding: 40px; text-align: center; color: var(--text-muted); font-size: 13px; }

.summary {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 14px;
  padding: 8px 12px;
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  display: inline-block;
}

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
}

.tree-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  font-size: 13px;
}
.type-badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  background: var(--bg-surface2);
  flex-shrink: 0;
}
.type-badge.t-PHASE { color: var(--color-primary); border-color: var(--color-primary); }
.type-badge.t-ACTIVITY { color: var(--color-cyan); border-color: var(--color-cyan); }
.type-badge.t-TASK { color: var(--color-info); }
.type-badge.t-DELIVERABLE { color: var(--text-muted); }
.code { color: var(--text-muted); font-size: 12px; flex-shrink: 0; }
.name { color: var(--text-primary); }
.badge {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-surface2);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.actions {
  margin-left: auto;
  display: none;
  gap: 2px;
  flex-shrink: 0;
}
.tree-row:hover .actions { display: inline-flex; }
</style>
