import api from '@/api/axios'
import type { Workflow } from '@/api/workflow'
import type { DeliverableDto, AttachmentDto } from '@/api/deliverables'

export interface TaskDto {
  id: number
  taskName: string
  status: string
  progressRate: number
  assigneeId: number | null
  plannedStartDate: string | null
  plannedEndDate: string | null
  plannedEffort: number | null
  actualEffort: number | null
  depth: number
  description: string | null
  catalogNodeId: number | null
  workflowId: number | null
}

export interface TemplateDeliverable {
  name: string
  templateFileRef: string
  seqNo: number
}

export interface UpdateTaskBody {
  taskName?: string
  status?: string
  assigneeId?: number | null
  plannedStartDate?: string | null
  plannedEndDate?: string | null
  plannedEffort?: number | null
  actualEffort?: number | null
  description?: string | null
}

export async function getTask(id: number): Promise<TaskDto> {
  const res = await api.get(`/tasks/${id}`)
  return res.data.data
}

export async function getTaskWorkflow(id: number): Promise<Workflow> {
  const res = await api.get(`/tasks/${id}/workflow`)
  return res.data.data
}

export async function updateTask(id: number, body: UpdateTaskBody) {
  const res = await api.put(`/tasks/${id}`, body)
  return res.data.data
}

export async function updateTaskProgress(id: number, progressRate: number) {
  const res = await api.put(`/tasks/${id}/progress`, { progressRate })
  return res.data.data
}

export async function getTaskDeliverables(id: number): Promise<DeliverableDto[]> {
  const res = await api.get(`/tasks/${id}/deliverables`)
  return res.data.data
}

export async function createTaskDeliverable(
  id: number,
  body: { deliverableName: string; deliverableType?: string }
): Promise<DeliverableDto> {
  const res = await api.post(`/tasks/${id}/deliverables`, body)
  return res.data.data
}

export async function getTemplateDeliverables(
  id: number
): Promise<TemplateDeliverable[]> {
  const res = await api.get(`/tasks/${id}/template-deliverables`)
  return res.data.data
}

export async function getAttachments(
  deliverableId: number
): Promise<AttachmentDto[]> {
  const res = await api.get(`/deliverables/${deliverableId}/attachments`)
  return res.data.data
}

export async function uploadAttachment(
  deliverableId: number,
  body: { fileName: string; contentType?: string; fileSize?: number }
): Promise<AttachmentDto> {
  const res = await api.post(`/deliverables/${deliverableId}/attachments`, body)
  return res.data.data
}
