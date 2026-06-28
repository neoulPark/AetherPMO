import api from '@/api/axios'

export type DeliverableStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'

export interface DeliverableDto {
  id: number
  projectId: number
  taskId: number | null
  deliverableName: string
  deliverableType: string
  status: DeliverableStatus
  versionNo: number | string
  submittedBy: number | null
  submittedAt: string | null
  reviewedAt: string | null
  approvedAt: string | null
  authorName: string | null
  attachmentCount: number
  updatedAt: string | null
}

export interface AttachmentDto {
  id: number
  fileName: string
  fileSize: number
  uploadedAt: string
}

export async function listDeliverables(
  projectId: number | string
): Promise<DeliverableDto[]> {
  const res = await api.get(`/projects/${projectId}/deliverables`)
  return res.data.data
}

export async function getDeliverable(id: number): Promise<DeliverableDto> {
  const res = await api.get(`/deliverables/${id}`)
  return res.data.data
}

export async function submitDeliverable(id: number) {
  const res = await api.post(`/deliverables/${id}/submit`)
  return res.data.data
}

export async function startReview(id: number) {
  const res = await api.post(`/deliverables/${id}/review`, {})
  return res.data.data
}

export async function approveDeliverable(id: number, comment: string) {
  const res = await api.post(`/deliverables/${id}/approve`, { comment })
  return res.data.data
}

export async function rejectDeliverable(id: number, comment: string) {
  const res = await api.post(`/deliverables/${id}/review`, {
    decision: 'REJECT',
    comment,
  })
  return res.data.data
}

export async function getAttachments(id: number): Promise<AttachmentDto[]> {
  const res = await api.get(`/deliverables/${id}/attachments`)
  return res.data.data
}

export interface StatusMeta {
  label: string
  color: string
}

export const DELIVERABLE_STATUS_META: Record<DeliverableStatus, StatusMeta> = {
  DRAFT: { label: '초안', color: 'gray' },
  SUBMITTED: { label: '검토 요청', color: 'orange' },
  UNDER_REVIEW: { label: '검토중', color: 'blue' },
  APPROVED: { label: '승인 완료', color: 'green' },
  REJECTED: { label: '반려', color: 'red' },
}

export function statusMeta(status: string): StatusMeta {
  return (
    DELIVERABLE_STATUS_META[status as DeliverableStatus] || {
      label: status,
      color: 'gray',
    }
  )
}
