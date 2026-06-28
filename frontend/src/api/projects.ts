import api from '@/api/axios'
import type { Project } from '@/types'

export interface ProjectDto {
  id: number
  projectName: string
  projectCode: string
  description: string
  pmId: number
  clientCompanyId: number
  status: string
  projectStage: 'BIDDING' | 'EXECUTION' | 'COMPLETED'
  plannedStartDate: string
  plannedEndDate: string
  actualStartDate: string | null
  actualEndDate: string | null
  contractAmount: number | string | null
  progressRate: number | null
  riskLevel: string | null
  team: string | null
  location: string | null
  businessType: string | null
  pmName: string | null
  clientName: string | null
  bidStatus?: string | null
  consortiumRole?: string | null
  consortiumShare?: number | null
  vrbStatus?: string | null
  announcementNo?: string | null
  proposalDeadline?: string | null
}

export interface TaskNode {
  id: number
  parentTaskId: number | null
  projectId: number
  taskName: string
  status: string
  progressRate: number
  assigneeId: number | null
  plannedStartDate: string | null
  plannedEndDate: string | null
  plannedEffort: number | null
  actualEffort: number | null
  depth: number
  sortOrder: number
  description: string | null
  children: TaskNode[]
}

export async function listProjects(stage: string): Promise<ProjectDto[]> {
  const res = await api.get(`/projects?stage=${stage}`)
  return res.data.data
}

export async function getProject(id: number | string): Promise<ProjectDto> {
  const res = await api.get(`/projects/${id}`)
  return res.data.data
}

export async function getProjectTasks(projectId: number | string): Promise<TaskNode[]> {
  const res = await api.get(`/projects/${projectId}/tasks`)
  return res.data.data
}

export async function updateTaskProgress(taskId: number, progressRate: number) {
  const res = await api.put(`/tasks/${taskId}/progress`, { progressRate })
  return res.data.data
}

export async function updateTask(id: number, payload: Record<string, unknown>) {
  const res = await api.put(`/tasks/${id}`, payload)
  return res.data.data
}

const STATUS_LABELS: Record<string, string> = {
  IN_PROGRESS: '수행 중',
  PLANNING: '준비',
  ON_HOLD: '보류',
  COMPLETED: '완료',
  CANCELLED: '취소',
}

function computeDday(endDate: string | null): number {
  if (!endDate) return 0
  const today = new Date('2026-06-16')
  const end = new Date(endDate)
  const diff = Math.round((today.getTime() - end.getTime()) / 86400000)
  // positive => past due, negative => days remaining
  return diff
}

const BID_STATUS_LABELS: Record<string, string> = {
  PREPARING: '제안 준비',
  SUBMITTED: '제안 제출',
  WAITING: '결과 대기',
  WON: '수주',
  LOST: '실패',
}

export function bidStatusLabel(status?: string | null): string {
  if (!status) return '제안 준비'
  return BID_STATUS_LABELS[status] || status
}

export function mapBiddingProject(dto: ProjectDto): Project {
  const base = mapProject(dto)
  return {
    ...base,
    bidStatus: dto.bidStatus || undefined,
    consortiumRole: dto.consortiumRole || undefined,
    consortiumShare: dto.consortiumShare ?? null,
    vrb: dto.vrbStatus || undefined,
    announcementNo: dto.announcementNo ?? null,
    proposalDeadline: dto.proposalDeadline || undefined,
    dday: computeDday(dto.proposalDeadline || dto.plannedEndDate),
  }
}

export function mapProject(dto: ProjectDto): Project {
  const risk = (dto.riskLevel || '보통') as Project['riskLevel']
  return {
    id: dto.id,
    name: dto.projectName,
    description: dto.description,
    team: dto.team || '',
    stage: dto.projectStage,
    status: dto.status,
    statusLabel: STATUS_LABELS[dto.status] || dto.status,
    client: dto.clientName || '',
    pm: dto.pmName || '',
    pmId: dto.pmId,
    startDate: dto.plannedStartDate,
    endDate: dto.plannedEndDate,
    budget: Number(dto.contractAmount || 0),
    progress: dto.progressRate || 0,
    memberCount: 0,
    deliverableTotal: 0,
    deliverableSubmitted: 0,
    deliverableReview: 0,
    riskLevel: risk,
    dday: computeDday(dto.plannedEndDate),
  }
}
