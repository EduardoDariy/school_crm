/** Типы ответов backend API (см. Swagger: /swagger-ui.html). */

export type StaffRole = 'ADMIN' | 'MANAGER' | 'TEACHER' | 'FINANCE';
export type LeadStatus = 'OPEN' | 'WON' | 'LOST';
export type LostReasonMode = 'DISABLED' | 'OPTIONAL' | 'REQUIRED';
export type TaskStatus = 'OPEN' | 'DONE' | 'CANCELLED';
export type LeadEventType =
  | 'CREATED' | 'UPDATED' | 'STAGE_CHANGED' | 'MANAGER_CHANGED'
  | 'WON' | 'LOST' | 'REOPENED' | 'ARCHIVED' | 'UNARCHIVED';

export interface CurrentUser {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: StaffRole;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: CurrentUser;
}

export interface Organization {
  id: string;
  name: string;
  subdomain: string;
  timezone: string;
}

export interface Stage {
  id: string;
  name: string;
  position: number;
}

export interface LostReason {
  id: string;
  name: string;
  active: boolean;
}

export interface Pipeline {
  id: string;
  name: string;
  defaultPipeline: boolean;
  lostReasonMode: LostReasonMode;
  stages: Stage[];
  lostReasons: LostReason[];
}

export interface Lead {
  id: string;
  name: string;
  phoneNumber: string | null;
  messengerId: string | null;
  source: string | null;
  pipelineId: string;
  stageId: string;
  status: LeadStatus;
  managerId: string | null;
  lostReasonId: string | null;
  lostComment: string | null;
  personId: string | null;
  archived: boolean;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface LeadEvent {
  id: string;
  type: LeadEventType;
  fromStageId: string | null;
  toStageId: string | null;
  managerId: string | null;
  lostReasonId: string | null;
  comment: string | null;
  actorId: string | null;
  createdAt: string;
}

export interface LeadInput {
  name: string;
  phoneNumber?: string | null;
  messengerId?: string | null;
  source?: string | null;
  managerId?: string | null;
}

export interface Task {
  id: string;
  title: string;
  description: string | null;
  dueAt: string;
  status: TaskStatus;
  overdue: boolean;
  assigneeId: string;
  createdBy: string;
  leadId: string | null;
  personId: string | null;
  completedAt: string | null;
  completedBy: string | null;
  createdAt: string;
}

export interface TaskInput {
  title: string;
  description?: string | null;
  dueAt: string;
  assigneeId?: string | null;
  leadId?: string | null;
}

export interface TaskSummary {
  open: number;
  overdue: number;
  dueToday: number;
}

export interface StaffItem {
  id: string;
  fullName: string;
  role: StaffRole;
  blocked: boolean;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: StaffRole;
  blocked: boolean;
  lastLoginAt: string | null;
  createdAt: string | null;
}

export interface LoginAttempt {
  id: string;
  email: string;
  success: boolean;
  failureReason: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Person {
  id: string;
  firstName: string;
  lastName: string | null;
  phoneNumber: string | null;
  student: boolean;
}

export interface Group {
  id: string;
  name: string;
  teacherId: string | null;
  active: boolean;
  students: Person[] | null;
}

export interface Debtor {
  studentId: string;
  firstName: string;
  lastName: string | null;
  phoneNumber: string | null;
  lessonsRemaining: number;
}

/** Ошибка API в формате Problem Details. */
export interface Problem {
  status: number;
  detail?: string;
  existingId?: string;
}
