import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Debtor, Group, Lead, LeadEvent, LeadInput, LeadStatus, LoginAttempt, LostReasonMode, Organization, Page,
  Pipeline, StaffItem, StaffRole, Task, TaskInput, TaskStatus, TaskSummary, User,
} from './models';

export interface LeadFilter {
  pipelineId?: string;
  status?: LeadStatus;
  mine?: boolean;
  archived?: boolean;
  query?: string;
}

export interface TaskFilter {
  all?: boolean;
  status?: TaskStatus;
  anyStatus?: boolean;
  leadId?: string;
}

/** Все обращения к backend API школы. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // ---------- Организация, сотрудники ----------

  organization(): Observable<Organization> {
    return this.http.get<Organization>('/api/organization');
  }

  staff(): Observable<StaffItem[]> {
    return this.http.get<StaffItem[]>('/api/staff');
  }

  users(): Observable<User[]> {
    return this.http.get<User[]>('/api/users');
  }

  createUser(body: { email: string; fullName: string; role: StaffRole; password: string }): Observable<User> {
    return this.http.post<User>('/api/users', body);
  }

  updateUser(id: string, body: { fullName: string; role: StaffRole }): Observable<User> {
    return this.http.put<User>(`/api/users/${id}`, body);
  }

  setUserBlocked(id: string, blocked: boolean): Observable<User> {
    return this.http.post<User>(`/api/users/${id}/${blocked ? 'block' : 'unblock'}`, {});
  }

  setUserPassword(id: string, password: string): Observable<void> {
    return this.http.post<void>(`/api/users/${id}/password`, { password });
  }

  loginJournal(page = 0, size = 50): Observable<Page<LoginAttempt>> {
    return this.http.get<Page<LoginAttempt>>('/api/users/login-journal', { params: { page, size } });
  }

  // ---------- Воронки ----------

  pipelines(): Observable<Pipeline[]> {
    return this.http.get<Pipeline[]>('/api/pipelines');
  }

  updatePipeline(id: string, body: { name: string; lostReasonMode: LostReasonMode }): Observable<Pipeline> {
    return this.http.put<Pipeline>(`/api/pipelines/${id}`, body);
  }

  addStage(pipelineId: string, name: string): Observable<Pipeline> {
    return this.http.post<Pipeline>(`/api/pipelines/${pipelineId}/stages`, { name });
  }

  renameStage(pipelineId: string, stageId: string, name: string): Observable<Pipeline> {
    return this.http.put<Pipeline>(`/api/pipelines/${pipelineId}/stages/${stageId}`, { name });
  }

  reorderStages(pipelineId: string, stageIds: string[]): Observable<Pipeline> {
    return this.http.put<Pipeline>(`/api/pipelines/${pipelineId}/stages/order`, { stageIds });
  }

  deleteStage(pipelineId: string, stageId: string, moveToStageId?: string): Observable<Pipeline> {
    const params = moveToStageId ? new HttpParams().set('moveToStageId', moveToStageId) : undefined;
    return this.http.delete<Pipeline>(`/api/pipelines/${pipelineId}/stages/${stageId}`, { params });
  }

  addLostReason(pipelineId: string, name: string): Observable<Pipeline> {
    return this.http.post<Pipeline>(`/api/pipelines/${pipelineId}/lost-reasons`, { name });
  }

  updateLostReason(pipelineId: string, reasonId: string, name: string, active: boolean): Observable<Pipeline> {
    return this.http.put<Pipeline>(`/api/pipelines/${pipelineId}/lost-reasons/${reasonId}`, { name, active });
  }

  // ---------- Лиды ----------

  leads(filter: LeadFilter = {}): Observable<Lead[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Lead[]>('/api/leads', { params });
  }

  lead(id: string): Observable<Lead> {
    return this.http.get<Lead>(`/api/leads/${id}`);
  }

  leadHistory(id: string): Observable<LeadEvent[]> {
    return this.http.get<LeadEvent[]>(`/api/leads/${id}/history`);
  }

  createLead(body: LeadInput): Observable<Lead> {
    return this.http.post<Lead>('/api/leads', body);
  }

  updateLead(id: string, body: LeadInput): Observable<Lead> {
    return this.http.put<Lead>(`/api/leads/${id}`, body);
  }

  moveLead(id: string, stageId: string): Observable<Lead> {
    return this.http.post<Lead>(`/api/leads/${id}/stage`, { stageId });
  }

  assignLeadManager(id: string, managerId: string | null): Observable<Lead> {
    return this.http.post<Lead>(`/api/leads/${id}/manager`, { managerId });
  }

  winLead(id: string): Observable<Lead> {
    return this.http.post<Lead>(`/api/leads/${id}/win`, {});
  }

  loseLead(id: string, lostReasonId: string | null, comment: string | null): Observable<Lead> {
    return this.http.post<Lead>(`/api/leads/${id}/lose`, { lostReasonId, comment });
  }

  reopenLead(id: string): Observable<Lead> {
    return this.http.post<Lead>(`/api/leads/${id}/reopen`, {});
  }

  setLeadArchived(id: string, archived: boolean): Observable<Lead> {
    return this.http.post<Lead>(`/api/leads/${id}/${archived ? 'archive' : 'unarchive'}`, {});
  }

  // ---------- Задачи ----------

  tasks(filter: TaskFilter = {}): Observable<Task[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value !== undefined && value !== null) {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Task[]>('/api/tasks', { params });
  }

  taskSummary(): Observable<TaskSummary> {
    return this.http.get<TaskSummary>('/api/tasks/summary');
  }

  createTask(body: TaskInput): Observable<Task> {
    return this.http.post<Task>('/api/tasks', body);
  }

  setTaskStatus(id: string, action: 'complete' | 'cancel' | 'reopen'): Observable<Task> {
    return this.http.post<Task>(`/api/tasks/${id}/${action}`, {});
  }

  // ---------- Обучение и финансы ----------

  groups(): Observable<Group[]> {
    return this.http.get<Group[]>('/api/groups');
  }

  debtors(): Observable<Debtor[]> {
    return this.http.get<Debtor[]>('/api/reports/debtors');
  }
}
