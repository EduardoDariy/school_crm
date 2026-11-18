import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Debtor, Group, Lead, Task, TaskSummary } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { TaskDialogComponent } from '../../shared/task-dialog.component';
import { TaskListComponent } from '../../shared/task-list.component';

/** Рабочий стол: набор блоков зависит от роли сотрудника. */
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, TaskListComponent, TaskDialogComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Здравствуйте, {{ firstName() }}!</h1>
          <div class="muted">{{ today }}</div>
        </div>
      </div>

      @if (isSales) {
        <div class="tiles">
          <a class="card tile" routerLink="/tasks">
            <div class="value">{{ summary()?.open ?? '—' }}</div><div class="muted">Мои открытые задачи</div>
          </a>
          <a class="card tile" [class.alert]="(summary()?.overdue ?? 0) > 0" routerLink="/tasks">
            <div class="value">{{ summary()?.overdue ?? '—' }}</div><div class="muted">Просрочено</div>
          </a>
          <a class="card tile" routerLink="/tasks">
            <div class="value">{{ summary()?.dueToday ?? '—' }}</div><div class="muted">Ещё на сегодня</div>
          </a>
          <a class="card tile" routerLink="/leads">
            <div class="value">{{ myLeads().length }}</div><div class="muted">Мои лиды в работе</div>
          </a>
        </div>

        <div class="grid-2">
          <section class="card">
            <div class="row section-head">
              <h2>Мои задачи</h2>
              <span class="spacer"></span>
              <button class="btn btn-sm" (click)="taskDialog.set(true)">+ Задача</button>
            </div>
            <app-task-list [tasks]="tasks()" emptyText="Открытых задач нет — отличная работа!" (changed)="load()" />
          </section>
          <section class="card">
            <div class="row section-head">
              <h2>Новые лиды без ответственного</h2>
              <span class="spacer"></span>
              <a routerLink="/leads">Все лиды →</a>
            </div>
            @if (unassigned().length === 0) { <div class="empty">Все лиды распределены</div> }
            <table class="table">
              @for (lead of unassigned(); track lead.id) {
                <tr class="clickable" [routerLink]="['/leads', lead.id]">
                  <td>{{ lead.name }}</td>
                  <td class="muted">{{ lead.phoneNumber || lead.messengerId || '' }}</td>
                  <td>@if (lead.source) { <span class="badge">{{ lead.source }}</span> }</td>
                </tr>
              }
            </table>
          </section>
        </div>
      }

      @if (auth.hasRole('TEACHER')) {
        <section class="card">
          <div class="row section-head"><h2>Мои группы</h2><span class="spacer"></span><a routerLink="/groups">Подробнее →</a></div>
          @if (groups().length === 0) { <div class="empty">За вами пока не закреплены группы</div> }
          <table class="table">
            @for (group of groups(); track group.id) {
              <tr><td>{{ group.name }}</td><td class="muted">{{ group.students?.length ?? 0 }} учеников</td></tr>
            }
          </table>
        </section>
      }

      @if (auth.hasRole('FINANCE', 'ADMIN')) {
        <section class="card debtors">
          <h2>Закончились занятия по абонементу</h2>
          @if (debtors().length === 0) { <div class="empty">Должников нет</div> }
          <table class="table">
            @for (d of debtors(); track d.studentId) {
              <tr><td>{{ d.firstName }} {{ d.lastName ?? '' }}</td><td class="muted">{{ d.phoneNumber ?? '' }}</td>
                <td><span class="badge badge-danger">{{ d.lessonsRemaining }} занятий</span></td></tr>
            }
          </table>
        </section>
      }
    </div>

    @if (taskDialog()) {
      <app-task-dialog (closed)="taskDialog.set(false)" (created)="taskDialog.set(false); load()" />
    }
  `,
  styles: `
    .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 16px; }
    .tile { color: var(--text); }
    .tile:hover { text-decoration: none; border-color: var(--accent); }
    .tile .value { font-size: 28px; font-weight: 600; }
    .tile.alert .value { color: var(--danger); }
    .section-head { margin-bottom: 4px; }
    .section-head h2 { margin: 0; }
    .spacer { flex: 1; }
    .debtors { margin-top: 16px; }
  `,
})
export class DashboardComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly staff = inject(StaffDirectoryService);

  protected readonly isSales = this.auth.hasRole('ADMIN', 'MANAGER');
  protected readonly summary = signal<TaskSummary | null>(null);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly myLeads = signal<Lead[]>([]);
  protected readonly unassigned = signal<Lead[]>([]);
  protected readonly groups = signal<Group[]>([]);
  protected readonly debtors = signal<Debtor[]>([]);
  protected readonly taskDialog = signal(false);

  protected readonly today = new Date().toLocaleDateString('ru-RU', { weekday: 'long', day: 'numeric', month: 'long' });

  firstName(): string {
    const name = this.auth.user()?.fullName ?? '';
    return name.split(' ')[0] || name;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    if (this.isSales) {
      this.staff.load();
      this.api.taskSummary().subscribe(s => this.summary.set(s));
      this.api.tasks().subscribe(t => this.tasks.set(t.slice(0, 8)));
      this.api.leads({ mine: true, status: 'OPEN' }).subscribe(l => this.myLeads.set(l));
      this.api.leads({ status: 'OPEN' }).subscribe(l => this.unassigned.set(l.filter(x => !x.managerId).slice(0, 8)));
    }
    if (this.auth.hasRole('TEACHER')) {
      this.api.groups().subscribe(g => this.groups.set(g));
    }
    if (this.auth.hasRole('FINANCE', 'ADMIN')) {
      this.api.debtors().subscribe(d => this.debtors.set(d));
    }
  }
}
