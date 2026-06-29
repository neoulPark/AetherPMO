import api from '@/api/axios'

export type ContactType = 'INTERNAL' | 'EXTERNAL'

export interface ContactPoint {
  contactId: number
  projectId: number
  field: string
  contactType: ContactType
  userId: number | null
  name: string | null
  company: string | null
  department: string | null
  title: string | null
  phone: string | null
  email: string | null
  note: string | null
  sortOrder: number | null
}

export interface ContactUser {
  id: number
  username: string
  email: string
  fullName: string
  role: string
  active: boolean
}

export interface ContactBody {
  field: string
  contactType: ContactType
  userId?: number | null
  name?: string | null
  company?: string | null
  department?: string | null
  title?: string | null
  phone?: string | null
  email?: string | null
  note?: string | null
  sortOrder?: number | null
}

export async function getContacts(projectId: number | string): Promise<ContactPoint[]> {
  const res = await api.get(`/projects/${projectId}/contacts`)
  return res.data.data
}

export async function createContact(projectId: number | string, body: ContactBody): Promise<ContactPoint> {
  const res = await api.post(`/projects/${projectId}/contacts`, body)
  return res.data.data
}

export async function updateContact(id: number, body: ContactBody): Promise<ContactPoint> {
  const res = await api.put(`/contacts/${id}`, body)
  return res.data.data
}

export async function deleteContact(id: number): Promise<void> {
  await api.delete(`/contacts/${id}`)
}

export async function searchUsers(q: string): Promise<ContactUser[]> {
  const res = await api.get(`/users/search`, { params: { q } })
  return res.data.data
}
