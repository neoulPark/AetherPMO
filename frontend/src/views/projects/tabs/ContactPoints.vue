<template>
  <div class="contact-points">
    <div class="tab-header">
      <div>
        <h2 class="tab-title">컨택포인트</h2>
        <p class="tab-subtitle">프로젝트 분야별 담당자 연락처</p>
      </div>
      <button class="btn-primary" @click="openCreate">+ 컨택포인트 추가</button>
    </div>

    <div class="table-wrap">
      <div v-if="loading" class="loading-state">불러오는 중...</div>

      <template v-else>
        <el-table v-if="contacts.length > 0" :data="contacts" class="dark-table" style="width: 100%">
          <el-table-column prop="field" label="분야" min-width="100" />
          <el-table-column label="구분" width="80">
            <template #default="{ row }">
              <span class="type-badge" :class="row.contactType === 'INTERNAL' ? 'internal' : 'external'">
                {{ row.contactType === 'INTERNAL' ? '내부' : '외부' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="이름" min-width="100">
            <template #default="{ row }">{{ row.name || '-' }}</template>
          </el-table-column>
          <el-table-column label="소속" min-width="120">
            <template #default="{ row }">
              {{ row.contactType === 'INTERNAL' ? (row.company || '내부') : (row.company || '-') }}
            </template>
          </el-table-column>
          <el-table-column label="직책" min-width="90">
            <template #default="{ row }">{{ row.title || '-' }}</template>
          </el-table-column>
          <el-table-column label="연락처" min-width="120">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="이메일" min-width="160">
            <template #default="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="비고" min-width="120">
            <template #default="{ row }">{{ row.note || '-' }}</template>
          </el-table-column>
          <el-table-column label="관리" width="120" fixed="right">
            <template #default="{ row }">
              <div class="action-btns">
                <button class="btn-icon" @click="openEdit(row)">수정</button>
                <button class="btn-icon" @click="onDelete(row)">삭제</button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState v-else message="등록된 컨택포인트가 없습니다." />
      </template>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '컨택포인트 수정' : '컨택포인트 추가'"
      width="520px"
      class="contact-dialog"
    >
      <el-form label-position="top" class="contact-form">
        <el-form-item label="분야">
          <el-select
            v-model="form.field"
            filterable
            allow-create
            default-first-option
            placeholder="분야 선택 또는 입력"
            style="width: 100%"
          >
            <el-option v-for="opt in fieldSuggestions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>

        <el-form-item label="구분">
          <el-radio-group v-model="form.contactType">
            <el-radio label="INTERNAL">내부</el-radio>
            <el-radio label="EXTERNAL">외부</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.contactType === 'INTERNAL'">
          <el-form-item label="담당자 (Amaranth 사용자)">
            <el-select
              v-model="form.userId"
              filterable
              remote
              reserve-keyword
              placeholder="이름으로 검색"
              :remote-method="onUserSearch"
              :loading="userLoading"
              style="width: 100%"
            >
              <el-option
                v-for="u in userOptions"
                :key="u.id"
                :label="`${u.fullName} (${u.role})`"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="pickedUserName" label="선택된 담당자">
            <el-input :model-value="pickedUserName" readonly />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="이름" required>
            <el-input v-model="form.name" placeholder="이름" />
          </el-form-item>
          <el-form-item label="소속/회사">
            <el-input v-model="form.company" placeholder="회사명" />
          </el-form-item>
          <el-form-item label="부서">
            <el-input v-model="form.department" placeholder="부서" />
          </el-form-item>
          <el-form-item label="직책">
            <el-input v-model="form.title" placeholder="직책" />
          </el-form-item>
          <el-form-item label="전화">
            <el-input v-model="form.phone" placeholder="전화번호" />
          </el-form-item>
          <el-form-item label="이메일">
            <el-input v-model="form.email" placeholder="이메일" />
          </el-form-item>
        </template>

        <el-form-item label="비고">
          <el-input v-model="form.note" type="textarea" :rows="3" placeholder="비고" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getContacts,
  createContact,
  updateContact,
  deleteContact,
  searchUsers,
  type ContactPoint,
  type ContactType,
  type ContactBody,
  type ContactUser,
} from '@/api/contacts'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const contacts = ref<ContactPoint[]>([])
const loading = ref(false)

const fieldSuggestions = ['영업', '기술', '계약', '법무', '품질', '고객사 PM', '사업관리', '보안']

const dialogVisible = ref(false)
const editing = ref<ContactPoint | null>(null)
const saving = ref(false)

interface FormState {
  field: string
  contactType: ContactType
  userId: number | null
  name: string
  company: string
  department: string
  title: string
  phone: string
  email: string
  note: string
}

function emptyForm(): FormState {
  return {
    field: '',
    contactType: 'INTERNAL',
    userId: null,
    name: '',
    company: '',
    department: '',
    title: '',
    phone: '',
    email: '',
    note: '',
  }
}

const form = reactive<FormState>(emptyForm())

const userOptions = ref<ContactUser[]>([])
const userLoading = ref(false)
const pickedUserName = ref('')

watch(
  () => form.userId,
  (id) => {
    if (id == null) {
      pickedUserName.value = ''
      return
    }
    const u = userOptions.value.find((x) => x.id === id)
    if (u) pickedUserName.value = u.fullName
  }
)

async function onUserSearch(q: string) {
  if (!q) {
    userOptions.value = []
    return
  }
  userLoading.value = true
  try {
    userOptions.value = await searchUsers(q)
  } catch (e) {
    console.error('Failed to search users', e)
    userOptions.value = []
  } finally {
    userLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    contacts.value = await getContacts(projectId.value)
  } catch (e) {
    console.error('Failed to load contacts', e)
    contacts.value = []
  } finally {
    loading.value = false
  }
}

function resetForm(src?: ContactPoint) {
  Object.assign(form, emptyForm())
  userOptions.value = []
  pickedUserName.value = ''
  if (src) {
    form.field = src.field || ''
    form.contactType = src.contactType
    form.userId = src.userId
    form.name = src.name || ''
    form.company = src.company || ''
    form.department = src.department || ''
    form.title = src.title || ''
    form.phone = src.phone || ''
    form.email = src.email || ''
    form.note = src.note || ''
    if (src.contactType === 'INTERNAL' && src.name) {
      pickedUserName.value = src.name
    }
  }
}

function openCreate() {
  editing.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ContactPoint) {
  editing.value = row
  resetForm(row)
  dialogVisible.value = true
}

async function onSave() {
  if (!form.field.trim()) {
    ElMessage.warning('분야를 입력하세요.')
    return
  }
  let body: ContactBody
  if (form.contactType === 'INTERNAL') {
    if (form.userId == null) {
      ElMessage.warning('담당자를 선택하세요.')
      return
    }
    body = {
      field: form.field,
      contactType: 'INTERNAL',
      userId: form.userId,
      note: form.note || null,
    }
  } else {
    if (!form.name.trim()) {
      ElMessage.warning('이름을 입력하세요.')
      return
    }
    body = {
      field: form.field,
      contactType: 'EXTERNAL',
      name: form.name,
      company: form.company || null,
      department: form.department || null,
      title: form.title || null,
      phone: form.phone || null,
      email: form.email || null,
      note: form.note || null,
    }
  }

  saving.value = true
  try {
    if (editing.value) {
      await updateContact(editing.value.contactId, body)
      ElMessage.success('수정되었습니다.')
    } else {
      await createContact(projectId.value, body)
      ElMessage.success('추가되었습니다.')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    console.error('Failed to save contact', e)
    ElMessage.error('저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function onDelete(row: ContactPoint) {
  try {
    await ElMessageBox.confirm('이 컨택포인트를 삭제하시겠습니까?', '삭제 확인', {
      confirmButtonText: '삭제',
      cancelButtonText: '취소',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteContact(row.contactId)
    ElMessage.success('삭제되었습니다.')
    await load()
  } catch (e) {
    console.error('Failed to delete contact', e)
    ElMessage.error('삭제에 실패했습니다.')
  }
}

onMounted(load)
watch(projectId, load)
</script>

<style scoped>
.tab-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.tab-title { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.tab-subtitle { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.btn-primary { background: var(--color-primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; }

.table-wrap {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}

.loading-state { padding: 30px; text-align: center; color: var(--text-muted); font-size: 13px; }

.type-badge { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.type-badge.internal { background: rgba(0,184,148,0.15); color: #4fd1c5; }
.type-badge.external { background: var(--bg-surface2); color: var(--text-secondary); }

.action-btns { display: flex; gap: 6px; }
.btn-icon {
  background: var(--bg-surface2);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
}
.btn-icon:hover { color: var(--text-primary); }

.dark-table {
  --el-table-bg-color: var(--bg-surface);
  --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface2);
  --el-table-border-color: var(--border);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-row-hover-bg-color: var(--bg-surface2);
  --el-bg-color: var(--bg-surface);
}
</style>
