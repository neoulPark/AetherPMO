import api from '@/api/axios'
import type { ProjectDto } from '@/api/projects'

export interface DeliverableTemplate {
  deliverableTemplateId: number
  seqNo: number
  deliverableName: string
  isOptional: boolean
}

export interface TaskTemplate {
  taskTemplateId: number
  taskCode: string
  taskName: string
  isOptional: boolean
  deliverables: DeliverableTemplate[]
}

export interface Activity {
  activityCode: string
  activityName: string
  tasks: TaskTemplate[]
}

export interface Phase {
  phaseCode: string
  phaseName: string
  activities: Activity[]
}

export interface CreateProjectWithTailoringPayload {
  projectName: string
  description: string
  team: string
  pmId: number
  clientCompanyId: number
  status: string
  projectStage: string
  plannedStartDate: string | null
  plannedEndDate: string | null
  contractAmount: number | null
  riskLevel: string
  selectedTaskTemplateIds: number[]
  selectedDeliverableTemplateIds: number[]
}

export async function getCatalog(): Promise<Phase[]> {
  const res = await api.get('/methodology/catalog')
  return res.data.data
}

export async function createProjectWithTailoring(
  payload: CreateProjectWithTailoringPayload
): Promise<ProjectDto> {
  const res = await api.post('/projects/with-tailoring', payload)
  return res.data.data
}
