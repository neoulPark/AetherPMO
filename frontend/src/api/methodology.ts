import api from '@/api/axios'
import type { ProjectDto } from '@/api/projects'

export type NodeType = 'PHASE' | 'ACTIVITY' | 'TASK' | 'DELIVERABLE'

export interface CatalogNode {
  nodeId: number
  parentNodeId: number | null
  nodeType: NodeType
  code: string | null
  name: string
  isOptional: boolean
  seqNo: number | null
  sortOrder: number | null
  workflowId: number | null
  children: CatalogNode[]
}

export interface CreateNodePayload {
  parentNodeId: number | null
  nodeType: NodeType
  code?: string | null
  name: string
  isOptional?: boolean
  sortOrder?: number | null
  seqNo?: number | null
  deliverableCategory?: string | null
  stage?: string | null
}

export interface UpdateNodePayload {
  name?: string
  code?: string | null
  isOptional?: boolean
  sortOrder?: number | null
  description?: string | null
  workflowId?: number | null
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
  selectedNodeIds: number[]
}

export async function getCatalog(): Promise<CatalogNode[]> {
  const res = await api.get('/methodology/catalog')
  return res.data.data
}

export async function createNode(payload: CreateNodePayload): Promise<CatalogNode> {
  const res = await api.post('/methodology/nodes', payload)
  return res.data.data
}

export async function updateNode(
  id: number,
  payload: UpdateNodePayload
): Promise<CatalogNode> {
  const res = await api.put(`/methodology/nodes/${id}`, payload)
  return res.data.data
}

export async function deleteNode(id: number): Promise<void> {
  await api.delete(`/methodology/nodes/${id}`)
}

export async function createProjectWithTailoring(
  payload: CreateProjectWithTailoringPayload
): Promise<ProjectDto> {
  const res = await api.post('/projects/with-tailoring', payload)
  return res.data.data
}
