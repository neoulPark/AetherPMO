export interface User {
  id: number
  name: string
  email: string
  role: string
  initials: string
  color: string
}

export interface Project {
  id: number
  name: string
  description: string
  team: string
  stage: 'BIDDING' | 'EXECUTION' | 'COMPLETED'
  status: string
  statusLabel: string
  client: string
  pm: string
  pmId: number
  startDate: string
  endDate: string
  budget: number
  progress: number
  memberCount: number
  deliverableTotal: number
  deliverableSubmitted: number
  deliverableReview: number
  riskLevel: '높음' | '보통' | '낮음'
  dday: number
  bidStatus?: string
  consortiumRole?: string
  consortiumShare?: number | null
  vrb?: string
  announcementNo?: string | null
  proposalDeadline?: string
}

export interface Issue {
  id: number
  projectId: number
  projectName: string
  type: 'ISSUE' | 'RISK'
  title: string
  priority: '상' | '중' | '하'
  assignee: string
  occurredDate: string
  status: string
  statusLabel: string
}

export interface ActionItem {
  id: number
  projectId: number
  projectName: string
  title: string
  assignee: string
  dueDate: string
  completedDate?: string | null
  status: string
  statusLabel: string
}

export interface Deliverable {
  id: number
  projectId: number
  name: string
  category: string
  version: string
  author: string
  updatedAt: string
  status: string
  statusLabel: string
}

export interface Meeting {
  id: number
  projectId: number
  projectName: string
  title: string
  type: string
  dateTime: string
  location: string
  attendees: string
  status: string
}

export interface OfficialDoc {
  id: number
  projectId: number
  projectName: string
  docNo: string
  title: string
  direction: 'INBOUND' | 'OUTBOUND'
  senderOrg: string
  receiverOrg: string
  drafter: string
  draftDept: string
  sentDate: string | null
  approvalStatus: string
  reviewStatus: string
  attachmentCount: number
}

export interface Template {
  id: number
  name: string
  category: string
  version: string
  updatedAt: string
  fileName: string
  stage: 'INCEPTION' | 'EXECUTION' | 'CLOSURE'
}
