import api from '@/api/axios'
import { updateNode } from '@/api/methodology'

export type StatusCategory = 'TODO' | 'IN_PROGRESS' | 'DONE'

export interface WorkflowStatus {
  statusId: number
  code: string
  name: string
  color: string
  category: StatusCategory
  isInitial: boolean
  isFinal: boolean
  sortOrder: number
}

export interface WorkflowTransition {
  transitionId: number
  fromStatusId: number
  toStatusId: number
  name: string | null
}

export interface Workflow {
  workflowId: number
  name: string
  description: string | null
  isDefault: boolean
  statuses: WorkflowStatus[]
  transitions: WorkflowTransition[]
}

export interface CreateWorkflowPayload {
  name: string
  description?: string | null
}

export interface UpdateWorkflowPayload {
  name?: string
  description?: string | null
}

export interface AddStatusPayload {
  code: string
  name: string
  color: string
  category: StatusCategory
  isInitial?: boolean
  isFinal?: boolean
  sortOrder?: number
}

export interface UpdateStatusPayload {
  name?: string
  color?: string
  category?: StatusCategory
  isInitial?: boolean
  isFinal?: boolean
  sortOrder?: number
}

export interface AddTransitionPayload {
  fromStatusId: number
  toStatusId: number
  name?: string | null
}

export async function getWorkflows(): Promise<Workflow[]> {
  const res = await api.get('/workflows')
  return res.data.data
}

export async function getWorkflow(id: number): Promise<Workflow> {
  const res = await api.get(`/workflows/${id}`)
  return res.data.data
}

export async function createWorkflow(body: CreateWorkflowPayload): Promise<Workflow> {
  const res = await api.post('/workflows', body)
  return res.data.data
}

export async function updateWorkflow(
  id: number,
  body: UpdateWorkflowPayload
): Promise<Workflow> {
  const res = await api.put(`/workflows/${id}`, body)
  return res.data.data
}

export async function deleteWorkflow(id: number): Promise<void> {
  await api.delete(`/workflows/${id}`)
}

export async function addStatus(
  workflowId: number,
  body: AddStatusPayload
): Promise<WorkflowStatus> {
  const res = await api.post(`/workflows/${workflowId}/statuses`, body)
  return res.data.data
}

export async function updateStatus(
  statusId: number,
  body: UpdateStatusPayload
): Promise<WorkflowStatus> {
  const res = await api.put(`/workflow-statuses/${statusId}`, body)
  return res.data.data
}

export async function deleteStatus(statusId: number): Promise<void> {
  await api.delete(`/workflow-statuses/${statusId}`)
}

export async function addTransition(
  workflowId: number,
  body: AddTransitionPayload
): Promise<WorkflowTransition> {
  const res = await api.post(`/workflows/${workflowId}/transitions`, body)
  return res.data.data
}

export async function deleteTransition(transitionId: number): Promise<void> {
  await api.delete(`/workflow-transitions/${transitionId}`)
}

export async function assignNodeWorkflow(
  nodeId: number,
  workflowId: number | null
): Promise<void> {
  await updateNode(nodeId, { workflowId })
}
